package com.eqixiac.equinix.design.value.tco;

import com.eqixiac.equinix.design.value.CurrencyReconciler;
import com.eqixiac.equinix.design.value.ratecard.ColocationItem;
import com.eqixiac.equinix.design.value.ratecard.EgressPath;
import com.eqixiac.equinix.design.value.ratecard.EgressRate;
import com.eqixiac.equinix.design.value.ratecard.PriceQuote;
import com.eqixiac.equinix.design.value.ratecard.RateCard;
import com.eqixiac.equinix.design.value.ratecard.ReferenceRateCard;
import com.eqixiac.equinix.design.value.ratecard.Term;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Internal engine that turns a {@link TcoCalculator.Builder} configuration into a
 * {@link TcoComparison}. Stateless. Cloud egress and Equinix interconnect costs come
 * from the resolved rate card (the caller's, or the standard live-then-reference
 * chain); on-prem inputs come from the caller's overrides, falling back to the
 * bundled {@link ReferenceRateCard} midpoints. Every cross-component sum runs
 * through a {@code CurrencyReconciler}, so mixed-currency figures are reported as
 * unpriced/partial with a reason, never silently combined.
 */
final class TcoEngine {

    private TcoEngine() {}

    static TcoComparison compute(TcoCalculator.Builder b) {
        RateCard rateCard = b.getRateCard() != null ? b.getRateCard() : RateCard.standardChain(b.getFabric());
        ReferenceRateCard reference = ReferenceRateCard.standard();
        Term term = b.getTerm();

        BigDecimal gb = b.getEgressUnit().toGigabytes(BigDecimal.valueOf(b.getEgressAmount()));

        Optional<EgressRate> internet = rateCard.egress(b.getProvider(), b.getRegion(), EgressPath.INTERNET, term);
        Optional<EgressRate> privateEgress = rateCard.egress(b.getProvider(), b.getRegion(), EgressPath.PRIVATE, term);
        String currency = resolveCurrency(internet, privateEgress);

        List<CostBreakdown> breakdowns = new ArrayList<>();
        if (b.getArchetypes().contains(DeploymentArchetype.PUBLIC_CLOUD_INTERNET)) {
            breakdowns.add(publicCloudInternet(internet, gb, currency));
        }
        if (b.getArchetypes().contains(DeploymentArchetype.ON_PREM)) {
            breakdowns.add(onPrem(b, reference, currency));
        }
        if (b.getArchetypes().contains(DeploymentArchetype.EQUINIX_INTERCONNECT)) {
            breakdowns.add(equinixInterconnect(b, rateCard, reference, privateEgress, gb, term, currency));
        }

        // Fold any caller-supplied line items (e.g. compute/storage) into every priced archetype,
        // so absolute totals reflect the full deployment (applied uniformly — the comparison is
        // unchanged).
        Map<String, BigDecimal> additional = b.getAdditionalLineItems();
        if (additional != null && !additional.isEmpty()) {
            List<CostBreakdown> withExtra = new ArrayList<>(breakdowns.size());
            for (CostBreakdown cb : breakdowns) {
                withExtra.add(withAdditional(cb, additional));
            }
            breakdowns = withExtra;
        }

        // Stamp every breakdown with its cost over the full commitment term (MRC × months + NRC)
        // so the recommendation and savings account for one-time setup charges, not just the
        // monthly rate.
        List<CostBreakdown> withTermTotals = new ArrayList<>(breakdowns.size());
        for (CostBreakdown cb : breakdowns) {
            withTermTotals.add(withTermTotal(cb, term));
        }
        breakdowns = withTermTotals;

        // Recommend the priced archetype that is cheapest over the term, not by MRC alone —
        // a low monthly rate with a heavy setup charge must not beat a slightly higher
        // monthly rate with none.
        CostBreakdown recommended = breakdowns.stream()
                .filter(CostBreakdown::isPriced)
                .min((x, y) -> x.getTotalOverTerm().compareTo(y.getTotalOverTerm()))
                .orElse(null);

        Optional<CostBreakdown> baseline = breakdowns.stream()
                .filter(c -> c.getArchetype() == DeploymentArchetype.PUBLIC_CLOUD_INTERNET && c.isPriced())
                .findFirst();

        // Baseline-minus-recommended is a subtraction, so it is only valid when both breakdowns are
        // in the same currency. Each breakdown is stamped with the currency its own components
        // reconciled to (which the layered default chain can genuinely make differ from the
        // comparison-wide egress currency), so this guard does real work: when the two disagree,
        // the saving is left null (not a fabricated cross-currency figure).
        BigDecimal monthlySavings = null;
        BigDecimal annualSavings = null;
        BigDecimal termSavings = null;
        String currencyNote = null;
        if (recommended != null && baseline.isPresent()) {
            if (CurrencyReconciler.knownDifferent(baseline.get().getCurrency(), recommended.getCurrency())) {
                currencyNote = " The saving versus the baseline was not computed: the baseline ("
                        + baseline.get().getCurrency() + ") and recommended (" + recommended.getCurrency()
                        + ") archetypes are priced in different currencies and cannot be subtracted without an "
                        + "FX rate.";
            } else {
                monthlySavings = baseline.get().getMonthlyTotal().subtract(recommended.getMonthlyTotal());
                annualSavings = monthlySavings.multiply(BigDecimal.valueOf(12));
                termSavings = baseline.get().getTotalOverTerm().subtract(recommended.getTotalOverTerm());
            }
        }

        String disclaimer = "Design-time TCO estimate, not a quote. Equinix Fabric connection costs use live "
                + "pricing where available; cloud-egress, cloud-provider interconnect-port, cross-connect, and "
                + "on-prem figures are indicative reference midpoints (the on-prem inputs are coarse and "
                + "overridable). Compute, storage, software, staffing, and per-provider free-tier egress allowances "
                + "are out of scope. Actual costs depend on region, volume, tiering, and contract terms.";
        if (currencyNote != null) {
            disclaimer = disclaimer + currencyNote;
        }

        return TcoComparison.builder()
                .breakdowns(breakdowns)
                .recommended(recommended == null ? null : recommended.getArchetype())
                .baseline(DeploymentArchetype.PUBLIC_CLOUD_INTERNET)
                .monthlySavingsVsBaseline(monthlySavings)
                .annualSavingsVsBaseline(annualSavings)
                .savingsOverTermVsBaseline(termSavings)
                .term(term)
                .currency(currency)
                .disclaimer(disclaimer)
                .asOf(reference.asOf())
                .build();
    }

    private static CostBreakdown withTermTotal(CostBreakdown cb, Term term) {
        BigDecimal total = cb.getMonthlyTotal()
                .multiply(BigDecimal.valueOf(term.months()))
                .add(cb.getSetupTotal());
        return cb.toBuilder().totalOverTerm(total).build();
    }

    private static CostBreakdown publicCloudInternet(Optional<EgressRate> internet, BigDecimal gb, String currency) {
        Map<String, BigDecimal> items = new LinkedHashMap<>();
        if (internet.isEmpty()) {
            return CostBreakdown.builder()
                    .archetype(DeploymentArchetype.PUBLIC_CLOUD_INTERNET)
                    .monthlyTotal(BigDecimal.ZERO).setupTotal(BigDecimal.ZERO)
                    .currency(currency).lineItems(items).priced(false)
                    .note("Public-internet egress rate unavailable for this provider/region.")
                    .build();
        }
        BigDecimal egressCost = internet.get().costFor(gb);
        items.put("Cloud egress (public internet)", egressCost);
        return CostBreakdown.builder()
                .archetype(DeploymentArchetype.PUBLIC_CLOUD_INTERNET)
                .monthlyTotal(egressCost).setupTotal(BigDecimal.ZERO)
                .currency(currency).lineItems(items).priced(true)
                .build();
    }

    private static CostBreakdown onPrem(TcoCalculator.Builder b, ReferenceRateCard reference, String currency) {
        BigDecimal transitPerMbps = orReference(b.getOnPremTransitPerMbpsMonth(), reference, "transitPerMbpsMonth");
        BigDecimal hardware = orReference(b.getOnPremHardwareMonthly(), reference, "hardwareMonthly");
        BigDecimal crossConnect = orReference(b.getOnPremCrossConnectMonthly(), reference, "crossConnectMonthly");
        BigDecimal powerPerKw = orReference(b.getOnPremPowerPerKwMonth(), reference, "powerPerKwMonth");

        Map<String, BigDecimal> items = new LinkedHashMap<>();
        if (transitPerMbps == null || hardware == null || crossConnect == null || powerPerKw == null) {
            return CostBreakdown.builder()
                    .archetype(DeploymentArchetype.ON_PREM)
                    .monthlyTotal(BigDecimal.ZERO).setupTotal(BigDecimal.ZERO)
                    .currency(currency).lineItems(items).priced(false)
                    .note("On-prem reference figures unavailable.")
                    .build();
        }

        // The bundled midpoints are published in the reference card's own currency (USD). When the
        // comparison currency (resolved from egress) is known to differ, stamping those figures with
        // it would mislabel them — the same guard the reference fold-ins in equinixInterconnect(...)
        // apply. Per the CurrencyReconciler policy the archetype is reported unpriced with the
        // reason, never as a relabelled cross-currency number. Caller-supplied overrides are taken
        // to be in the comparison currency, so a fully overridden archetype still prices.
        boolean usesReferenceMidpoints = b.getOnPremTransitPerMbpsMonth() == null
                || b.getOnPremHardwareMonthly() == null
                || b.getOnPremCrossConnectMonthly() == null
                || b.getOnPremPowerPerKwMonth() == null;
        if (usesReferenceMidpoints && CurrencyReconciler.knownDifferent(reference.currencyCode(), currency)) {
            return CostBreakdown.builder()
                    .archetype(DeploymentArchetype.ON_PREM)
                    .monthlyTotal(BigDecimal.ZERO).setupTotal(BigDecimal.ZERO)
                    .currency(currency).lineItems(items).priced(false)
                    .note("On-prem reference midpoints are " + reference.currencyCode()
                            + " but this comparison is priced in " + currency
                            + "; they cannot be relabelled without an FX rate, so this archetype is reported "
                            + "as unpriced. Supply all four on-prem overrides in " + currency
                            + " to price it.")
                    .build();
        }
        BigDecimal transit = transitPerMbps.multiply(BigDecimal.valueOf(b.getBandwidthMbps()));
        BigDecimal power = powerPerKw.multiply(BigDecimal.valueOf(b.getPowerKw()));
        items.put("Carrier IP transit (" + b.getBandwidthMbps() + " Mbps)", transit);
        items.put("Amortized hardware", hardware);
        items.put("Cross-connect", crossConnect);
        items.put("Power/space (" + b.getPowerKw() + " kW)", power);
        BigDecimal monthly = transit.add(hardware).add(crossConnect).add(power);
        return CostBreakdown.builder()
                .archetype(DeploymentArchetype.ON_PREM)
                .monthlyTotal(monthly).setupTotal(BigDecimal.ZERO)
                .currency(currency).lineItems(items).priced(true)
                .note("Indicative on-prem midpoints; excludes staffing, software, and compute.")
                .build();
    }

    private static CostBreakdown equinixInterconnect(TcoCalculator.Builder b, RateCard rateCard,
                                                     ReferenceRateCard reference, Optional<EgressRate> privateEgress,
                                                     BigDecimal gb, Term term, String currency) {
        Map<String, BigDecimal> items = new LinkedHashMap<>();

        Optional<PriceQuote> connection = rateCard.connection(
                b.getConnectionType(), b.getBandwidthMbps(), b.getMetro(), term);
        boolean priced = privateEgress.isPresent() && connection.isPresent();
        String note = priced ? null : "Private egress rate or Equinix connection price unavailable.";

        // Every priced component of this archetype flows through one reconciler. A cross-currency
        // sum (e.g. USD private egress + a EUR live Fabric connection, which the default chain
        // genuinely produces for an EMEA metro) would be a fabricated figure, so when the components
        // disagree the archetype is reported as unpriced/mixed rather than totalled.
        CurrencyReconciler recon = CurrencyReconciler.create();

        BigDecimal egressCost = privateEgress.map(r -> r.costFor(gb)).orElse(BigDecimal.ZERO);
        if (privateEgress.isPresent()) {
            items.put("Cloud egress (private interconnect)", egressCost);
            recon.add(privateEgress.get().getCurrency(), egressCost, BigDecimal.ZERO);
        }
        if (connection.isPresent()) {
            items.put("Equinix Fabric connection", connection.get().getMonthlyRecurring());
            recon.add(connection.get().getCurrency(),
                    connection.get().getMonthlyRecurring(), connection.get().getNonRecurring());
        }
        if (b.isIncludeRouter()) {
            Optional<PriceQuote> router = rateCard.cloudRouter(b.getRouterPackage(), b.getMetro(), term);
            if (router.isPresent()) {
                items.put("Fabric Cloud Router", router.get().getMonthlyRecurring());
                recon.add(router.get().getCurrency(),
                        router.get().getMonthlyRecurring(), router.get().getNonRecurring());
            } else {
                // A requested component that cannot be priced must not silently vanish from the
                // total. Follow the unpriced-component convention: flag the archetype as not fully
                // priced and name the missing component, leaving the partial figures visible.
                priced = false;
                String routerNote = "Fabric Cloud Router (" + b.getRouterPackage() + ") was requested but "
                        + "could not be priced by the rate card; this archetype's totals are partial and "
                        + "exclude the Cloud Router.";
                note = note == null ? routerNote : note + " " + routerNote;
            }
        }

        // Caller-supplied colocation primitives (cabinet, cross-connect, and per-kW power) take
        // precedence and make the physical-infrastructure side of the comparison reflect real
        // figures. Cabinet and cross-connect quotes are per unit, so they scale by the configured
        // counts (default 1), mirroring how POWER_PER_KW scales by the configured kW below.
        int crossConnectCount = b.getCrossConnects();
        Optional<PriceQuote> coloCrossConnect = crossConnectCount == 0 ? Optional.empty()
                : rateCard.colocation(ColocationItem.CROSS_CONNECT, b.getMetro(), term);
        if (coloCrossConnect.isPresent()) {
            BigDecimal qty = BigDecimal.valueOf(crossConnectCount);
            BigDecimal crossConnectMonthly = coloCrossConnect.get().getMonthlyRecurring().multiply(qty);
            BigDecimal crossConnectSetup = coloCrossConnect.get().getNonRecurring().multiply(qty);
            items.put(countedLabel("Equinix cross-connect", crossConnectCount,
                    coloCrossConnect.get().getMonthlyRecurring()), crossConnectMonthly);
            recon.add(coloCrossConnect.get().getCurrency(), crossConnectMonthly, crossConnectSetup);
        }
        int cabinetCount = b.getCabinets();
        Optional<PriceQuote> coloCabinet = cabinetCount == 0 ? Optional.empty()
                : rateCard.colocation(ColocationItem.CABINET, b.getMetro(), term);
        if (coloCabinet.isPresent()) {
            BigDecimal qty = BigDecimal.valueOf(cabinetCount);
            BigDecimal cabinetMonthly = coloCabinet.get().getMonthlyRecurring().multiply(qty);
            BigDecimal cabinetSetup = coloCabinet.get().getNonRecurring().multiply(qty);
            items.put(countedLabel("Colocation cabinet", cabinetCount,
                    coloCabinet.get().getMonthlyRecurring()), cabinetMonthly);
            recon.add(coloCabinet.get().getCurrency(), cabinetMonthly, cabinetSetup);
        }
        // POWER_PER_KW is priced per kW per month (see ColocationItem), so it is multiplied by the
        // configured power draw before being folded in — previously it was never consumed at all.
        Optional<PriceQuote> coloPower = rateCard.colocation(ColocationItem.POWER_PER_KW, b.getMetro(), term);
        if (coloPower.isPresent() && b.getPowerKw() > 0) {
            BigDecimal kw = BigDecimal.valueOf(b.getPowerKw());
            BigDecimal powerMonthly = coloPower.get().getMonthlyRecurring().multiply(kw);
            BigDecimal powerSetup = coloPower.get().getNonRecurring().multiply(kw);
            items.put("Colocation power (" + b.getPowerKw() + " kW)", powerMonthly);
            recon.add(coloPower.get().getCurrency(), powerMonthly, powerSetup);
        }

        // Mixed currencies among the components above: do not fabricate a combined total. Surface the
        // per-currency subtotals and mark the archetype unpriced, so it is treated as unavailable
        // rather than presented as a single (wrong) number.
        if (recon.isMixed()) {
            String mixNote = "Equinix interconnect components are priced in multiple currencies ("
                    + recon.describeCurrencies() + "): " + recon.describeMonthlySubtotals() + " per month. A "
                    + "single-currency total cannot be formed without an FX rate, so this archetype is reported "
                    + "as unpriced rather than as a fabricated cross-currency sum.";
            return CostBreakdown.builder()
                    .archetype(DeploymentArchetype.EQUINIX_INTERCONNECT)
                    .monthlyTotal(BigDecimal.ZERO).setupTotal(BigDecimal.ZERO)
                    .currency(currency).lineItems(items).priced(false)
                    .note(note == null ? mixNote : note + " " + mixNote)
                    .build();
        }

        BigDecimal monthly = recon.monthlyTotal().orElse(BigDecimal.ZERO);
        BigDecimal setup = recon.setupTotal().orElse(BigDecimal.ZERO);
        String coreCurrency = recon.soleCurrencyOr(currency);

        // The CSP interconnect port and (unless a colocation cross-connect was supplied above, or the
        // caller set crossConnects(0)) the Equinix cross-connect come from the bundled reference card
        // (USD). Only fold them in when the reconciled currency of the components above matches, so
        // reference figures never silently mix currencies into the total. The reference cross-connect
        // is per unit, so it scales by the configured count too.
        Optional<BigDecimal> cspPort = reference.cspInterconnectPortMonthly(b.getProvider(), b.getBandwidthMbps());
        Optional<BigDecimal> crossConnect = (coloCrossConnect.isPresent() || crossConnectCount == 0)
                ? Optional.empty() : reference.equinixCrossConnectMonthly();
        if (coreCurrency.equals(reference.currencyCode())) {
            if (cspPort.isPresent()) {
                items.put("Cloud provider interconnect port", cspPort.get());
                monthly = monthly.add(cspPort.get());
            }
            if (crossConnect.isPresent()) {
                BigDecimal crossConnectMonthly = crossConnect.get().multiply(BigDecimal.valueOf(crossConnectCount));
                items.put(countedLabel("Equinix cross-connect", crossConnectCount, crossConnect.get()),
                        crossConnectMonthly);
                monthly = monthly.add(crossConnectMonthly);
            }
        } else if (cspPort.isPresent() || crossConnect.isPresent()) {
            String skip = "CSP interconnect port and Equinix cross-connect omitted: reference figures are "
                    + reference.currencyCode() + " and cannot be mixed with " + coreCurrency + ".";
            note = note == null ? skip : note + " " + skip;
        }

        // Stamp the breakdown with the currency its own components reconciled to — not the
        // comparison-wide egress currency — so the baseline-vs-recommended currency guard in
        // compute() compares labels that actually describe these figures.
        return CostBreakdown.builder()
                .archetype(DeploymentArchetype.EQUINIX_INTERCONNECT)
                .monthlyTotal(monthly).setupTotal(setup)
                .currency(coreCurrency).lineItems(items).priced(priced)
                .note(note)
                .build();
    }

    /**
     * A per-unit line-item label carrying the count and unit rate when more than one unit is
     * folded in, e.g. {@code "Colocation cabinet (2x @ 500.00/mo)"}.
     */
    private static String countedLabel(String base, int count, BigDecimal unitMonthly) {
        if (count == 1) {
            return base;
        }
        return base + " (" + count + "x @ "
                + unitMonthly.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString() + "/mo)";
    }

    private static CostBreakdown withAdditional(CostBreakdown cb, Map<String, BigDecimal> extra) {
        if (!cb.isPriced()) {
            return cb;
        }
        Map<String, BigDecimal> items = new LinkedHashMap<>(cb.getLineItems());
        BigDecimal monthly = cb.getMonthlyTotal();
        for (Map.Entry<String, BigDecimal> e : extra.entrySet()) {
            items.put(e.getKey(), e.getValue());
            monthly = monthly.add(e.getValue());
        }
        return cb.toBuilder()
                .monthlyTotal(monthly)
                .lineItems(items)
                .build();
    }

    private static BigDecimal orReference(BigDecimal override, ReferenceRateCard reference, String key) {
        return override != null ? override : reference.onPrem(key).orElse(null);
    }

    private static String resolveCurrency(Optional<EgressRate> internet, Optional<EgressRate> privateEgress) {
        if (internet.isPresent() && internet.get().getCurrency() != null) {
            return internet.get().getCurrency().getCurrencyCode();
        }
        if (privateEgress.isPresent() && privateEgress.get().getCurrency() != null) {
            return privateEgress.get().getCurrency().getCurrencyCode();
        }
        return "USD";
    }
}
