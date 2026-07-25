package api.equinix.javasdk.design.value.tco;

import api.equinix.javasdk.design.value.CurrencyReconciler;
import api.equinix.javasdk.design.value.ratecard.ColocationItem;
import api.equinix.javasdk.design.value.ratecard.EgressPath;
import api.equinix.javasdk.design.value.ratecard.EgressRate;
import api.equinix.javasdk.design.value.ratecard.PriceQuote;
import api.equinix.javasdk.design.value.ratecard.RateCard;
import api.equinix.javasdk.design.value.ratecard.ReferenceRateCard;
import api.equinix.javasdk.design.value.ratecard.Term;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Internal engine that turns a {@link TcoCalculator.Builder} configuration into a
 * {@link TcoComparison}. Stateless. Cloud egress and on-prem inputs come from the
 * bundled {@link ReferenceRateCard}; Equinix interconnect costs use the resolved
 * (live-then-reference) rate card.
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

        // Recommend the cheapest priced archetype.
        CostBreakdown recommended = breakdowns.stream()
                .filter(CostBreakdown::isPriced)
                .min((x, y) -> x.getMonthlyTotal().compareTo(y.getMonthlyTotal()))
                .orElse(null);

        Optional<CostBreakdown> baseline = breakdowns.stream()
                .filter(c -> c.getArchetype() == DeploymentArchetype.PUBLIC_CLOUD_INTERNET && c.isPriced())
                .findFirst();

        // Baseline-minus-recommended is a subtraction, so it is only valid when both breakdowns are
        // in the same currency. Every breakdown here is stamped with the comparison-wide `currency`
        // (resolved from egress), and a breakdown whose own components mixed currencies is marked
        // unpriced by equinixInterconnect(...) and thus never chosen as recommended/baseline — so this
        // guard normally holds trivially. It is kept explicit rather than assumed: on the off chance
        // the two disagree, the saving is left null (not a fabricated cross-currency figure).
        BigDecimal monthlySavings = null;
        BigDecimal annualSavings = null;
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
                .currency(currency)
                .disclaimer(disclaimer)
                .asOf(reference.asOf())
                .build();
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
            }
        }

        // Caller-supplied colocation primitives (cabinet, cross-connect, and per-kW power) take
        // precedence and make the physical-infrastructure side of the comparison reflect real figures.
        Optional<PriceQuote> coloCrossConnect = rateCard.colocation(ColocationItem.CROSS_CONNECT, b.getMetro(), term);
        if (coloCrossConnect.isPresent()) {
            items.put("Equinix cross-connect", coloCrossConnect.get().getMonthlyRecurring());
            recon.add(coloCrossConnect.get().getCurrency(),
                    coloCrossConnect.get().getMonthlyRecurring(), coloCrossConnect.get().getNonRecurring());
        }
        Optional<PriceQuote> coloCabinet = rateCard.colocation(ColocationItem.CABINET, b.getMetro(), term);
        if (coloCabinet.isPresent()) {
            items.put("Colocation cabinet", coloCabinet.get().getMonthlyRecurring());
            recon.add(coloCabinet.get().getCurrency(),
                    coloCabinet.get().getMonthlyRecurring(), coloCabinet.get().getNonRecurring());
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

        // The CSP interconnect port and (unless a colocation cross-connect was supplied above) the
        // Equinix cross-connect come from the bundled reference card (USD). Only fold them in when the
        // reconciled currency of the components above matches, so reference figures never silently mix
        // currencies into the total.
        Optional<BigDecimal> cspPort = reference.cspInterconnectPortMonthly(b.getProvider(), b.getBandwidthMbps());
        Optional<BigDecimal> crossConnect = coloCrossConnect.isPresent()
                ? Optional.empty() : reference.equinixCrossConnectMonthly();
        if (coreCurrency.equals(reference.currencyCode())) {
            if (cspPort.isPresent()) {
                items.put("Cloud provider interconnect port", cspPort.get());
                monthly = monthly.add(cspPort.get());
            }
            if (crossConnect.isPresent()) {
                items.put("Equinix cross-connect", crossConnect.get());
                monthly = monthly.add(crossConnect.get());
            }
        } else if (cspPort.isPresent() || crossConnect.isPresent()) {
            String skip = "CSP interconnect port and Equinix cross-connect omitted: reference figures are "
                    + reference.currencyCode() + " and cannot be mixed with " + coreCurrency + ".";
            note = note == null ? skip : note + " " + skip;
        }

        return CostBreakdown.builder()
                .archetype(DeploymentArchetype.EQUINIX_INTERCONNECT)
                .monthlyTotal(monthly).setupTotal(setup)
                .currency(currency).lineItems(items).priced(priced)
                .note(note)
                .build();
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
        return CostBreakdown.builder()
                .archetype(cb.getArchetype())
                .monthlyTotal(monthly)
                .setupTotal(cb.getSetupTotal())
                .currency(cb.getCurrency())
                .lineItems(items)
                .priced(true)
                .note(cb.getNote())
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
