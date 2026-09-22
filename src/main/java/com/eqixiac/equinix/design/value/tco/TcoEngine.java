package com.eqixiac.equinix.design.value.tco;

import com.eqixiac.equinix.design.value.CurrencyReconciler;
import com.eqixiac.equinix.design.value.ratecard.ColocationItem;
import com.eqixiac.equinix.design.value.ratecard.EgressPath;
import com.eqixiac.equinix.design.value.ratecard.EgressRate;
import com.eqixiac.equinix.design.value.ratecard.MulticloudLinkQuote;
import com.eqixiac.equinix.design.value.ratecard.MulticloudLinkRequest;
import com.eqixiac.equinix.design.value.ratecard.PriceQuote;
import com.eqixiac.equinix.design.value.ratecard.RateCard;
import com.eqixiac.equinix.design.value.ratecard.ReferenceRateCard;
import com.eqixiac.equinix.design.value.ratecard.Term;
import com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
 *
 * <p>Two modes. With no peer cloud the single-cloud methods run exactly as before. With a peer
 * cloud ({@code toCloud}) the {@code *TwoSided} methods run instead: internet and private egress
 * are priced in both directions, the Equinix archetype carries two virtual connections and both
 * clouds' CSP port figures, and the native multicloud archetype is priced from
 * {@code RateCard.multicloudLink}. Pricing one archetype two-sided and another one-sided would
 * bias the ranking, so the mode applies to all of them together; the on-prem archetype, whose
 * inputs carry no cloud egress, is reported unpriced in this mode.</p>
 */
final class TcoEngine {

    private TcoEngine() {}

    static TcoComparison compute(TcoCalculator.Builder b) {
        RateCard rateCard = b.getRateCard() != null ? b.getRateCard() : RateCard.standardChain(b.getFabric());
        ReferenceRateCard reference = ReferenceRateCard.standard();
        Term term = b.getTerm();

        BigDecimal gb = b.getEgressUnit().toGigabytes(BigDecimal.valueOf(b.getEgressAmount()));
        boolean twoSided = b.getPeerProvider() != null;
        // Reverse-direction volume (peer cloud -> fromCloud). Not set = assumed symmetric; the
        // assumption is stated in the comparison's traffic note.
        BigDecimal reverseGb = !twoSided ? null
                : b.isReverseEgressSet()
                    ? b.getReverseEgressUnit().toGigabytes(BigDecimal.valueOf(b.getReverseEgressAmount()))
                    : gb;

        Optional<EgressRate> internet = rateCard.egress(b.getProvider(), b.getRegion(), EgressPath.INTERNET, term);
        Optional<EgressRate> privateEgress = rateCard.egress(b.getProvider(), b.getRegion(), EgressPath.PRIVATE, term);
        String currency = resolveCurrency(internet, privateEgress);

        List<CostBreakdown> breakdowns = new ArrayList<>();
        if (b.getArchetypes().contains(DeploymentArchetype.PUBLIC_CLOUD_INTERNET)) {
            breakdowns.add(twoSided
                    ? publicCloudInternetTwoSided(b, rateCard, internet, gb, reverseGb, term, currency)
                    : publicCloudInternet(internet, gb, currency));
        }
        if (b.getArchetypes().contains(DeploymentArchetype.ON_PREM)) {
            breakdowns.add(twoSided ? onPremNotApplicable(b, currency) : onPrem(b, reference, currency));
        }
        if (b.getArchetypes().contains(DeploymentArchetype.EQUINIX_INTERCONNECT)) {
            breakdowns.add(twoSided
                    ? equinixInterconnectTwoSided(b, rateCard, reference, privateEgress, gb, reverseGb, term, currency)
                    : equinixInterconnect(b, rateCard, reference, privateEgress, gb, term, currency));
        }
        if (twoSided && b.getArchetypes().contains(DeploymentArchetype.NATIVE_MULTICLOUD_INTERCONNECT)) {
            breakdowns.add(nativeMulticloudInterconnect(b, rateCard, gb, reverseGb, term, currency));
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
        if (twoSided) {
            disclaimer = disclaimer + " Cloud-to-cloud comparison: every archetype is priced on traffic in both "
                    + "directions; on-prem is not priced (its inputs carry no cloud egress). Native multicloud "
                    + "link figures are the providers' published "
                    + "hourly list rates converted at " + MulticloudLinkQuote.HOURS_PER_MONTH + " h/month (reference "
                    + "data as of " + reference.multicloudAsOf() + ", retrieved " + reference.multicloudRetrieved()
                    + "); a side with no published rate is reported unpriced, not estimated. The AWS "
                    + "Interconnect - multicloud free tier is applied only when useAwsFreeTier(true) is set.";
        }
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
                .trafficNote(twoSided ? trafficNote(b, gb, reverseGb) : null)
                .build();
    }

    private static String trafficNote(TcoCalculator.Builder b, BigDecimal gb, BigDecimal reverseGb) {
        String from = describe(b.getProvider(), b.getRegion());
        String to = describe(b.getPeerProvider(), b.getPeerRegion());
        return from + " -> " + to + ": " + wholeGb(gb) + " GB/mo; " + to + " -> " + from + ": "
                + wholeGb(reverseGb) + " GB/mo"
                + (b.isReverseEgressSet() ? ""
                    : " (reverse volume not supplied; assumed equal to the forward volume)")
                + ". Link size " + b.getBandwidthMbps() + " Mbps, path tier " + b.getPathTier()
                + (b.isUseAwsFreeTier() ? ", AWS free tier requested" : "") + ".";
    }

    private static String describe(CloudProviderType provider, String region) {
        return region == null || region.isBlank() ? String.valueOf(provider) : provider + " (" + region + ")";
    }

    private static String wholeGb(BigDecimal gb) {
        return gb.setScale(0, RoundingMode.HALF_UP).toPlainString();
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

    /**
     * The on-prem archetype in a cloud-to-cloud comparison: never priced. The on-prem inputs
     * describe one site (transit, hardware, one cross-connect, power) and contain no egress from
     * either cloud, while every other archetype is priced on the traffic in both directions. A
     * total formed from those inputs would be ranked on less traffic than its peers, so the
     * archetype is reported unpriced, which also excludes it from the recommendation.
     */
    private static CostBreakdown onPremNotApplicable(TcoCalculator.Builder b, String currency) {
        return CostBreakdown.builder()
                .archetype(DeploymentArchetype.ON_PREM)
                .monthlyTotal(BigDecimal.ZERO).setupTotal(BigDecimal.ZERO)
                .currency(currency).lineItems(new LinkedHashMap<>()).priced(false)
                .note("On-prem is not priced in a cloud-to-cloud comparison: its inputs describe one site "
                        + "and contain no egress from " + b.getProvider() + " or " + b.getPeerProvider()
                        + ", while the other archetypes are priced on the traffic in both directions. "
                        + "Remove toCloud(...) for the single-cloud comparison that prices it.")
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

        int crossConnectCount = b.getCrossConnects();
        boolean coloCrossConnectPriced = foldColocationPrimitives(b, rateCard, term, items, recon);

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
        Optional<BigDecimal> crossConnect = (coloCrossConnectPriced || crossConnectCount == 0)
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
     * Folds the caller-supplied colocation primitives (cross-connect, cabinet, per-kW power) into
     * an Equinix archetype's line items and reconciler. Shared by the single-cloud and two-sided
     * forms so both price them identically.
     *
     * @return whether the rate card priced the cross-connect (the reference cross-connect
     *         fallback is then skipped)
     */
    private static boolean foldColocationPrimitives(TcoCalculator.Builder b, RateCard rateCard, Term term,
                                                    Map<String, BigDecimal> items, CurrencyReconciler recon) {
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
        return coloCrossConnect.isPresent();
    }

    // ── Cloud-to-cloud (two-sided) forms ──

    /**
     * Public internet, both directions: {@code fromCloud}'s internet egress on the forward volume
     * plus the peer cloud's on the reverse volume. A missing rate leaves the archetype partially
     * priced with the priced direction still visible.
     */
    private static CostBreakdown publicCloudInternetTwoSided(TcoCalculator.Builder b, RateCard rateCard,
                                                             Optional<EgressRate> internet, BigDecimal gb,
                                                             BigDecimal reverseGb, Term term, String currency) {
        Optional<EgressRate> peerInternet = rateCard.egress(
                b.getPeerProvider(), b.getPeerRegion(), EgressPath.INTERNET, term);
        Map<String, BigDecimal> items = new LinkedHashMap<>();
        List<String> provenance = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        CurrencyReconciler recon = CurrencyReconciler.create();

        foldEgress(b.getProvider(), "public internet", internet, gb, items, recon, provenance, missing);
        foldEgress(b.getPeerProvider(), "public internet", peerInternet, reverseGb, items, recon, provenance, missing);

        String note = missing.isEmpty() ? null
                : "Public-internet egress rate unavailable for " + String.join(" and ", missing)
                + "; this archetype's totals are partial and exclude that direction.";
        if (recon.isMixed()) {
            return mixedCurrencyBreakdown(DeploymentArchetype.PUBLIC_CLOUD_INTERNET,
                    "Public-internet egress rates", recon, items, note, provenance, currency);
        }
        return CostBreakdown.builder()
                .archetype(DeploymentArchetype.PUBLIC_CLOUD_INTERNET)
                .monthlyTotal(recon.monthlyTotal().orElse(BigDecimal.ZERO)).setupTotal(BigDecimal.ZERO)
                .currency(recon.soleCurrencyOr(currency)).lineItems(items).priced(missing.isEmpty())
                .note(note).provenance(provenance)
                .build();
    }

    /**
     * Equinix interconnect, both clouds: private egress in both directions, one Fabric virtual
     * connection per cloud, the optional Cloud Router (one serves both connections), the
     * colocation primitives, and the CSP interconnect-port reference figure for BOTH clouds. A
     * missing component on either side leaves the archetype partially priced, so it is never
     * ranked on an incomplete total against the native path.
     */
    private static CostBreakdown equinixInterconnectTwoSided(TcoCalculator.Builder b, RateCard rateCard,
                                                             ReferenceRateCard reference,
                                                             Optional<EgressRate> privateEgress, BigDecimal gb,
                                                             BigDecimal reverseGb, Term term, String currency) {
        Optional<EgressRate> peerPrivate = rateCard.egress(
                b.getPeerProvider(), b.getPeerRegion(), EgressPath.PRIVATE, term);
        Optional<PriceQuote> connection = rateCard.connection(
                b.getConnectionType(), b.getBandwidthMbps(), b.getMetro(), term);

        Map<String, BigDecimal> items = new LinkedHashMap<>();
        List<String> provenance = new ArrayList<>();
        List<String> missingEgress = new ArrayList<>();
        List<String> partial = new ArrayList<>();
        CurrencyReconciler recon = CurrencyReconciler.create();

        foldEgress(b.getProvider(), "private interconnect", privateEgress, gb, items, recon, provenance, missingEgress);
        foldEgress(b.getPeerProvider(), "private interconnect", peerPrivate, reverseGb, items, recon, provenance,
                missingEgress);
        if (!missingEgress.isEmpty()) {
            partial.add("Private egress rate unavailable for " + String.join(" and ", missingEgress) + ".");
        }

        if (connection.isPresent()) {
            // One virtual connection per cloud, each at the configured bandwidth.
            for (CloudProviderType cloud : List.of(b.getProvider(), b.getPeerProvider())) {
                items.put("Equinix Fabric connection to " + cloud, connection.get().getMonthlyRecurring());
                recon.add(connection.get().getCurrency(),
                        connection.get().getMonthlyRecurring(), connection.get().getNonRecurring());
            }
            provenance.add("Equinix Fabric connections: two virtual connections (one per cloud) at "
                    + b.getBandwidthMbps() + " Mbps, " + connection.get().getSource()
                    + (connection.get().getNote() == null ? "" : " (" + connection.get().getNote() + ")") + ".");
        } else {
            partial.add("Equinix connection price unavailable.");
        }

        if (b.isIncludeRouter()) {
            Optional<PriceQuote> router = rateCard.cloudRouter(b.getRouterPackage(), b.getMetro(), term);
            if (router.isPresent()) {
                items.put("Fabric Cloud Router", router.get().getMonthlyRecurring());
                recon.add(router.get().getCurrency(),
                        router.get().getMonthlyRecurring(), router.get().getNonRecurring());
            } else {
                partial.add("Fabric Cloud Router (" + b.getRouterPackage() + ") was requested but could not be "
                        + "priced by the rate card; this archetype's totals are partial and exclude the Cloud Router.");
            }
        }

        int crossConnectCount = b.getCrossConnects();
        boolean coloCrossConnectPriced = foldColocationPrimitives(b, rateCard, term, items, recon);
        if (!b.isIncludeRouter()) {
            // Two virtual connections terminate on an Equinix A-side. Say which one the figures
            // contain, so an A-side-free total is never read as the full cost of the path.
            provenance.add(crossConnectCount > 0
                    ? "Equinix A-side: no Fabric Cloud Router was requested (includeCloudRouter(...)); the two "
                        + "virtual connections are taken to terminate on a colocation port, represented by the "
                        + "cross-connect line. The port itself is not priced."
                    : "Equinix A-side: no Fabric Cloud Router and no cross-connect are included, so the A-side the "
                        + "two virtual connections terminate on is not priced and this total is understated.");
        }

        if (recon.isMixed()) {
            return mixedCurrencyBreakdown(DeploymentArchetype.EQUINIX_INTERCONNECT,
                    "Equinix interconnect components", recon, items,
                    partial.isEmpty() ? null : String.join(" ", partial), provenance, currency);
        }

        BigDecimal monthly = recon.monthlyTotal().orElse(BigDecimal.ZERO);
        BigDecimal setup = recon.setupTotal().orElse(BigDecimal.ZERO);
        String coreCurrency = recon.soleCurrencyOr(currency);

        Optional<BigDecimal> crossConnect = (coloCrossConnectPriced || crossConnectCount == 0)
                ? Optional.empty() : reference.equinixCrossConnectMonthly();
        if (coreCurrency.equals(reference.currencyCode())) {
            for (CloudProviderType cloud : List.of(b.getProvider(), b.getPeerProvider())) {
                Optional<PriceQuote> cspPort = reference.cspInterconnectPortMonthlyQuote(cloud, b.getBandwidthMbps());
                if (cspPort.isPresent()) {
                    items.put("Cloud provider interconnect port (" + cloud + ")", cspPort.get().getMonthlyRecurring());
                    monthly = monthly.add(cspPort.get().getMonthlyRecurring());
                    provenance.add("Cloud provider interconnect port (" + cloud + "): " + cspPort.get().getNote()
                            + ", reference data as of " + reference.asOf() + ".");
                } else {
                    partial.add("No reference CSP interconnect-port figure for " + cloud
                            + "; this archetype's totals are partial and exclude that port.");
                }
            }
            if (crossConnect.isPresent()) {
                BigDecimal crossConnectMonthly = crossConnect.get().multiply(BigDecimal.valueOf(crossConnectCount));
                items.put(countedLabel("Equinix cross-connect", crossConnectCount, crossConnect.get()),
                        crossConnectMonthly);
                monthly = monthly.add(crossConnectMonthly);
            }
        } else {
            partial.add("CSP interconnect ports and Equinix cross-connect omitted: reference figures are "
                    + reference.currencyCode() + " and cannot be mixed with " + coreCurrency
                    + "; this archetype's totals are partial.");
        }

        return CostBreakdown.builder()
                .archetype(DeploymentArchetype.EQUINIX_INTERCONNECT)
                .monthlyTotal(monthly).setupTotal(setup)
                .currency(coreCurrency).lineItems(items).priced(partial.isEmpty())
                .note(partial.isEmpty() ? null : String.join(" ", partial))
                .provenance(provenance)
                .build();
    }

    /**
     * The providers' native multicloud link: one flat fee per provider plus the link's per-GB
     * charge on each direction's volume. The data-transfer line is added only when both sides
     * are priced; it reads "(none charged)" when the resolved per-GB rates make it zero.
     */
    private static CostBreakdown nativeMulticloudInterconnect(TcoCalculator.Builder b, RateCard rateCard,
                                                              BigDecimal gb, BigDecimal reverseGb, Term term,
                                                              String currency) {
        Map<String, BigDecimal> items = new LinkedHashMap<>();
        List<String> provenance = new ArrayList<>();
        DeploymentArchetype archetype = DeploymentArchetype.NATIVE_MULTICLOUD_INTERCONNECT;

        MulticloudLinkRequest request = MulticloudLinkRequest.builder()
                .providerA(b.getProvider()).regionA(b.getRegion())
                .providerZ(b.getPeerProvider()).regionZ(b.getPeerRegion())
                .bandwidthMbps(b.getBandwidthMbps())
                .pathTier(b.getPathTier())
                .term(term)
                .useAwsFreeTier(b.isUseAwsFreeTier())
                .build();
        Optional<MulticloudLinkQuote> resolved = rateCard.multicloudLink(request);
        if (resolved.isEmpty()) {
            String none = "No rate card in the chain holds native multicloud link prices for "
                    + b.getProvider() + " <-> " + b.getPeerProvider() + ".";
            provenance.add(none);
            return CostBreakdown.builder()
                    .archetype(archetype)
                    .monthlyTotal(BigDecimal.ZERO).setupTotal(BigDecimal.ZERO)
                    .currency(currency).lineItems(items).priced(false)
                    .note(none).provenance(provenance)
                    .build();
        }
        MulticloudLinkQuote quote = resolved.get();
        CurrencyReconciler recon = CurrencyReconciler.create();
        List<String> partial = new ArrayList<>();

        foldLinkSide(b.getProvider(), quote, b.getBandwidthMbps(), items, recon, provenance);
        foldLinkSide(b.getPeerProvider(), quote, b.getBandwidthMbps(), items, recon, provenance);
        quote.unpricedSummary().ifPresent(summary ->
                partial.add(summary + " This archetype's totals are partial."));

        if (quote.isFullyPriced()) {
            // The per-GB charge on the link is its own lookup. It is folded in (as an explicit
            // line, zero or not) only when both flat fees are priced and both rates resolve.
            Optional<EgressRate> rateA = rateCard.egress(
                    b.getProvider(), b.getRegion(), EgressPath.MULTICLOUD_INTERCONNECT, term);
            Optional<EgressRate> rateZ = rateCard.egress(
                    b.getPeerProvider(), b.getPeerRegion(), EgressPath.MULTICLOUD_INTERCONNECT, term);
            List<String> missingRate = new ArrayList<>();
            if (rateA.isEmpty()) {
                missingRate.add(String.valueOf(b.getProvider()));
            }
            if (rateZ.isEmpty()) {
                missingRate.add(String.valueOf(b.getPeerProvider()));
            }
            if (missingRate.isEmpty()) {
                BigDecimal costA = rateA.get().costFor(gb);
                BigDecimal costZ = rateZ.get().costFor(reverseGb);
                BigDecimal transfer = costA.add(costZ);
                // A zero amount is the same in every currency, so it never creates a mix.
                if (costA.signum() != 0) {
                    recon.add(rateA.get().getCurrency(), costA, BigDecimal.ZERO);
                }
                if (costZ.signum() != 0) {
                    recon.add(rateZ.get().getCurrency(), costZ, BigDecimal.ZERO);
                }
                items.put(transfer.signum() == 0
                        ? "Data transfer over the native link (none charged)"
                        : "Data transfer over the native link", transfer);
                provenance.add("Data transfer, " + b.getProvider() + ": " + perGbProvenance(rateA.get()));
                provenance.add("Data transfer, " + b.getPeerProvider() + ": " + perGbProvenance(rateZ.get()));
            } else {
                partial.add("Per-GB rate on the native link unavailable for " + String.join(" and ", missingRate)
                        + " (EgressPath.MULTICLOUD_INTERCONNECT); the data-transfer cost is unknown and this "
                        + "archetype's totals are partial.");
            }
        }
        provenance.addAll(quote.getNotes());

        String note = partial.isEmpty() ? null : String.join(" ", partial);
        if (recon.isMixed()) {
            return mixedCurrencyBreakdown(archetype, "Native multicloud link sides", recon, items, note,
                    provenance, currency);
        }
        return CostBreakdown.builder()
                .archetype(archetype)
                .monthlyTotal(recon.monthlyTotal().orElse(BigDecimal.ZERO))
                .setupTotal(recon.setupTotal().orElse(BigDecimal.ZERO))
                .currency(recon.soleCurrencyOr(currency)).lineItems(items).priced(partial.isEmpty())
                .note(note).provenance(provenance)
                .build();
    }

    private static void foldLinkSide(CloudProviderType provider, MulticloudLinkQuote quote, int bandwidthMbps,
                                     Map<String, BigDecimal> items, CurrencyReconciler recon,
                                     List<String> provenance) {
        Optional<PriceQuote> side = quote.side(provider);
        if (side.isEmpty()) {
            return;
        }
        items.put("Native link, " + provider + " side (" + bandwidthMbps + " Mbps)", side.get().getMonthlyRecurring());
        // A zero-priced side (the AWS free tier) is the same amount in every currency, so it never
        // creates a currency mix with the other side.
        if (side.get().getMonthlyRecurring().signum() != 0 || side.get().getNonRecurring().signum() != 0) {
            recon.add(side.get().getCurrency(), side.get().getMonthlyRecurring(), side.get().getNonRecurring());
        }
        provenance.add(provider + " side: " + side.get().getSource()
                + (side.get().getNote() == null ? "" : ", " + side.get().getNote()) + ".");
    }

    private static void foldEgress(CloudProviderType provider, String pathLabel, Optional<EgressRate> rate,
                                   BigDecimal volumeGb, Map<String, BigDecimal> items, CurrencyReconciler recon,
                                   List<String> provenance, List<String> missing) {
        if (rate.isEmpty()) {
            missing.add(String.valueOf(provider));
            return;
        }
        BigDecimal cost = rate.get().costFor(volumeGb);
        items.put("Cloud egress from " + provider + " (" + pathLabel + ")", cost);
        recon.add(rate.get().getCurrency(), cost, BigDecimal.ZERO);
        provenance.add("Cloud egress from " + provider + " (" + pathLabel + "): " + wholeGb(volumeGb) + " GB x "
                + perGbProvenance(rate.get()));
    }

    private static String perGbProvenance(EgressRate rate) {
        return rate.getPricePerGb().toPlainString() + " " + rate.getCurrency() + "/GB, " + rate.getSource()
                + (rate.getNote() == null ? "" : " (" + rate.getNote() + ")") + ".";
    }

    /**
     * The mixed-currency form of a two-sided breakdown: zero totals, unpriced, and a note giving
     * the per-currency subtotals — the same convention the single-cloud Equinix archetype uses.
     */
    private static CostBreakdown mixedCurrencyBreakdown(DeploymentArchetype archetype, String what,
                                                        CurrencyReconciler recon, Map<String, BigDecimal> items,
                                                        String priorNote, List<String> provenance,
                                                        String currency) {
        String mixNote = what + " are priced in multiple currencies (" + recon.describeCurrencies() + "): "
                + recon.describeMonthlySubtotals() + " per month. A single-currency total cannot be formed "
                + "without an FX rate, so this archetype is reported as unpriced rather than as a fabricated "
                + "cross-currency sum.";
        return CostBreakdown.builder()
                .archetype(archetype)
                .monthlyTotal(BigDecimal.ZERO).setupTotal(BigDecimal.ZERO)
                .currency(currency).lineItems(items).priced(false)
                .note(priorNote == null ? mixNote : priorNote + " " + mixNote)
                .provenance(provenance)
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
