package com.eqixiac.equinix.design.value.tco;

import com.eqixiac.equinix.design.cost.ConvertingRateCard;
import com.eqixiac.equinix.design.cost.CurrencyConverters;
import com.eqixiac.equinix.design.spec.CommercialTerms;
import com.eqixiac.equinix.design.value.CurrencyReconciler;
import com.eqixiac.equinix.design.value.ratecard.ColocationItem;
import com.eqixiac.equinix.design.value.ratecard.EgressPath;
import com.eqixiac.equinix.design.value.ratecard.EgressRate;
import com.eqixiac.equinix.design.value.ratecard.MulticloudLinkQuote;
import com.eqixiac.equinix.design.value.ratecard.MulticloudLinkRequest;
import com.eqixiac.equinix.design.value.ratecard.ConnectionModel;
import com.eqixiac.equinix.design.value.ratecard.PriceLookup;
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
        RateCard base = b.getRateCard() != null ? b.getRateCard() : RateCard.standardChain(b.getFabric());
        // One comparison currency for every figure: anything quoted in another currency is converted (pinned
        // rates, a supplied converter, live ECB rates, the bundled table), exactly as RateCardCostModel does.
        ConvertingRateCard rateCard = ConvertingRateCard.of(base, b.getCurrency(), CurrencyConverters.forTerms(
                CommercialTerms.builder().currency(b.getCurrency()).exchangeRates(b.getExchangeRates()).build(),
                b.getCurrencyConverter(), b.getDefaultRates()));
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

        PriceLookup<EgressRate> internetLookup = rateCard.egressLookup(b.getProvider(), b.getRegion(), EgressPath.INTERNET,
                term);
        PriceLookup<EgressRate> privateLookup = rateCard.egressLookup(b.getProvider(), b.getRegion(), EgressPath.PRIVATE,
                term);
        Optional<EgressRate> internet = internetLookup.toOptional();
        Optional<EgressRate> privateEgress = privateLookup.toOptional();
        String currency = b.getCurrency();

        List<CostBreakdown> breakdowns = new ArrayList<>();
        if (b.getArchetypes().contains(DeploymentArchetype.PUBLIC_CLOUD_INTERNET)) {
            breakdowns.add(twoSided
                    ? publicCloudInternetTwoSided(b, rateCard, internet, gb, reverseGb, term, currency)
                    : publicCloudInternet(internetLookup, gb, currency));
        }
        if (b.getArchetypes().contains(DeploymentArchetype.ON_PREM)) {
            breakdowns.add(twoSided ? onPremNotApplicable(b, currency) : onPrem(b, reference, rateCard, currency));
        }
        if (b.getArchetypes().contains(DeploymentArchetype.EQUINIX_INTERCONNECT)) {
            breakdowns.add(twoSided
                    ? equinixInterconnectTwoSided(b, rateCard, reference, privateEgress, gb, reverseGb, term, currency)
                    : equinixInterconnect(b, rateCard, reference, privateLookup, gb, term, currency));
        }
        if (twoSided && b.getArchetypes().contains(DeploymentArchetype.NATIVE_MULTICLOUD_INTERCONNECT)) {
            breakdowns.add(nativeMulticloudInterconnect(b, rateCard, gb, reverseGb, term, currency));
        }

        // Fold any caller-supplied line items (e.g. compute/storage) into every priced archetype,
        // so absolute totals reflect the full deployment (applied uniformly - the comparison is
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

        // Recommend the priced archetype that is cheapest over the term, not by MRC alone - 
        // a low monthly rate with a heavy setup charge must not beat a slightly higher
        // monthly rate with none.
        CostBreakdown recommended = breakdowns.stream()
                .filter(CostBreakdown::isPriced)
                .min((x, y) -> x.getTotalOverTerm().compareTo(y.getTotalOverTerm()))
                .orElse(null);

        String recommendationNote = recommendationNote(breakdowns, recommended);

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
        for (String fxNote : rateCard.converter().notes()) {
            disclaimer = disclaimer + " " + fxNote + ".";
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
                .recommendationNote(recommendationNote)
                .specFingerprint(b.getSpecFingerprint())
                .assumptions(b.getSpecAssumptions())
                .build();
    }

    /**
     * States how the recommendation relates to the incomplete archetypes. "Priced" means complete, so the
     * recommendation is always among complete archetypes; an incomplete archetype whose priced part over the
     * term is already at or above the recommendation's total cannot win once its missing lines are added,
     * otherwise the recommendation is provisional and the note says so.
     */
    static String recommendationNote(List<CostBreakdown> breakdowns, CostBreakdown recommended) {
        List<String> parts = new ArrayList<>();
        for (CostBreakdown cb : breakdowns) {
            if (cb.isPriced()) {
                continue;
            }
            String name = cb.getArchetype().getDisplayName();
            boolean sameCurrency = recommended != null && recommended.getCurrency() != null
                    && recommended.getCurrency().equals(cb.getCurrency());
            boolean hasPartial = cb.getLineItems() != null && !cb.getLineItems().isEmpty()
                    && cb.getMonthlyTotal() != null && cb.getMonthlyTotal().signum() > 0;
            if (recommended == null) {
                parts.add(name + " is incomplete (" + cb.getUnpriced().size() + " unpriced component(s))");
            } else if (hasPartial && sameCurrency
                    && cb.getTotalOverTerm().compareTo(recommended.getTotalOverTerm()) >= 0) {
                parts.add(name + " is incomplete, but its priced part alone (" + cb.getCurrency() + " "
                        + cb.getTotalOverTerm().setScale(2, RoundingMode.HALF_UP).toPlainString()
                        + " over the term) already exceeds the recommendation, so it cannot be cheaper");
            } else {
                parts.add(name + " is incomplete and could not be compared (" + cb.getUnpriced().size()
                        + " unpriced component(s)); it might be cheaper once priced, so the recommendation is "
                        + "provisional");
            }
        }
        if (parts.isEmpty()) {
            return null;
        }
        return (recommended == null ? "No archetype is complete, so none is recommended. "
                : "The recommendation is chosen among complete archetypes only. ") + String.join("; ", parts) + ".";
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

    private static CostBreakdown publicCloudInternet(PriceLookup<EgressRate> internetLookup, BigDecimal gb,
                                                     String currency) {
        Map<String, BigDecimal> items = new LinkedHashMap<>();
        Optional<EgressRate> internet = internetLookup.toOptional();
        if (internet.isEmpty()) {
            String reason = "Cloud egress (public internet): " + internetLookup.unpricedReason();
            return CostBreakdown.builder()
                    .archetype(DeploymentArchetype.PUBLIC_CLOUD_INTERNET)
                    .monthlyTotal(BigDecimal.ZERO).setupTotal(BigDecimal.ZERO)
                    .currency(currency).lineItems(items).priced(false)
                    .note("Public-internet egress rate unavailable for this provider/region.")
                    .unpriced(List.of(reason))
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

    private static CostBreakdown onPrem(TcoCalculator.Builder b, ReferenceRateCard reference,
                                        ConvertingRateCard rateCard, String currency) {
        BigDecimal transitPerMbps = orReference(b.getOnPremTransitPerMbpsMonth(), reference, "transitPerMbpsMonth", rateCard);
        BigDecimal hardware = orReference(b.getOnPremHardwareMonthly(), reference, "hardwareMonthly", rateCard);
        BigDecimal crossConnect = orReference(b.getOnPremCrossConnectMonthly(), reference, "crossConnectMonthly", rateCard);
        BigDecimal powerPerKw = orReference(b.getOnPremPowerPerKwMonth(), reference, "powerPerKwMonth", rateCard);

        Map<String, BigDecimal> items = new LinkedHashMap<>();
        if (transitPerMbps == null || hardware == null || crossConnect == null || powerPerKw == null) {
            return CostBreakdown.builder()
                    .archetype(DeploymentArchetype.ON_PREM)
                    .monthlyTotal(BigDecimal.ZERO).setupTotal(BigDecimal.ZERO)
                    .currency(currency).lineItems(items).priced(false)
                    .note("On-prem reference figures unavailable.")
                    .unpriced(List.of("On-prem inputs: a reference midpoint is missing from the bundle, or has no "
                            + "exchange rate from " + reference.currencyCode() + " to " + currency + ", and no override "
                            + "was supplied"))
                    .build();
        }

        // The bundled midpoints are published in the reference card's own currency (USD). When the
        // comparison currency (resolved from egress) is known to differ, stamping those figures with
        // it would mislabel them - the same guard the reference fold-ins in equinixInterconnect(...)
        // apply. Per the CurrencyReconciler policy the archetype is reported unpriced with the
        // reason, never as a relabelled cross-currency number. Caller-supplied overrides are taken
        // to be in the comparison currency, so a fully overridden archetype still prices.
        boolean usesReferenceMidpoints = b.getOnPremTransitPerMbpsMonth() == null
                || b.getOnPremHardwareMonthly() == null
                || b.getOnPremCrossConnectMonthly() == null
                || b.getOnPremPowerPerKwMonth() == null;
        // The midpoints were converted to the comparison currency above (orReference); this guard only fires for a
        // caller-built comparison whose currency the reference figures could not reach.
        if (usesReferenceMidpoints && transitPerMbps == null) {
            return CostBreakdown.builder()
                    .archetype(DeploymentArchetype.ON_PREM)
                    .monthlyTotal(BigDecimal.ZERO).setupTotal(BigDecimal.ZERO)
                    .currency(currency).lineItems(items).priced(false)
                    .note("On-prem reference midpoints are " + reference.currencyCode()
                            + " but this comparison is priced in " + currency
                            + "; they cannot be relabelled without an FX rate, so this archetype is reported "
                            + "as unpriced. Supply all four on-prem overrides in " + currency
                            + " to price it.")
                    .unpriced(List.of("On-prem midpoints: " + reference.currencyCode() + " reference figures cannot be "
                            + "combined with " + currency + " without an exchange rate"))
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
                .unpriced(List.of("On-prem: not applicable to a cloud-to-cloud comparison"))
                .build();
    }

    private static CostBreakdown equinixInterconnect(TcoCalculator.Builder b, ConvertingRateCard rateCard,
                                                     ReferenceRateCard reference, PriceLookup<EgressRate> privateLookup,
                                                     BigDecimal gb, Term term, String currency) {
        Map<String, BigDecimal> items = new LinkedHashMap<>();
        // Every component this archetype should contain is either a line item or an entry here: nothing is
        // silently dropped, and the archetype is "priced" only when this list is empty.
        List<String> unpriced = new ArrayList<>();

        // Every priced component flows through one reconciler. A cross-currency sum (e.g. USD private egress
        // + a GBP live Fabric connection, which the default chain produces for London) would be a fabricated
        // figure, so when the components disagree the archetype is reported as incomplete rather than totalled.
        CurrencyReconciler recon = CurrencyReconciler.create();

        if (privateLookup.isPriced()) {
            BigDecimal egressCost = privateLookup.value().costFor(gb);
            items.put("Cloud egress (private interconnect)", egressCost);
            recon.add(privateLookup.value().getCurrency(), egressCost, BigDecimal.ZERO);
        } else {
            unpriced.add("Cloud egress (private interconnect): " + privateLookup.unpricedReason());
        }
        PriceLookup<PriceQuote> connection = rateCard.connectionLookup(
                b.getConnectionType(), b.getBandwidthMbps(), b.getMetro(), term);
        if (connection.isPriced()) {
            items.put("Equinix Fabric connection", connection.value().getMonthlyRecurring());
            recon.add(connection.value().getCurrency(),
                    connection.value().getMonthlyRecurring(), connection.value().getNonRecurring());
        } else {
            unpriced.add("Equinix Fabric connection: " + connection.unpricedReason());
        }
        if (b.isIncludeRouter()) {
            PriceLookup<PriceQuote> router = rateCard.cloudRouterLookup(b.getRouterPackage(), b.getMetro(), term);
            if (router.isPriced()) {
                items.put("Fabric Cloud Router", router.value().getMonthlyRecurring());
                recon.add(router.value().getCurrency(),
                        router.value().getMonthlyRecurring(), router.value().getNonRecurring());
            } else {
                unpriced.add("Fabric Cloud Router (" + b.getRouterPackage() + "): " + router.unpricedReason());
            }
        }

        int crossConnectCount = b.getCrossConnects();
        boolean coloCrossConnectPriced = foldColocationPrimitives(b, rateCard, term, items, recon);

        if (recon.isMixed()) {
            String mixNote = "Equinix interconnect components are priced in multiple currencies ("
                    + recon.describeCurrencies() + "): " + recon.describeMonthlySubtotals() + " per month. A "
                    + "single-currency total cannot be formed without an FX rate, so this archetype is reported "
                    + "as incomplete rather than as a fabricated cross-currency sum.";
            unpriced.add("Total: " + mixNote);
            return CostBreakdown.builder()
                    .archetype(DeploymentArchetype.EQUINIX_INTERCONNECT)
                    .monthlyTotal(BigDecimal.ZERO).setupTotal(BigDecimal.ZERO)
                    .currency(currency).lineItems(items).priced(false)
                    .note(String.join(" ", unpriced)).unpriced(unpriced)
                    .build();
        }

        BigDecimal monthly = recon.monthlyTotal().orElse(BigDecimal.ZERO);
        BigDecimal setup = recon.setupTotal().orElse(BigDecimal.ZERO);
        String coreCurrency = recon.soleCurrencyOr(currency);

        // The provider's fee for the connection a Fabric cloud connection is (a hosted/partner connection),
        // priced exactly as the optimiser's RateCardCostModel prices it.
        PriceLookup<PriceQuote> cspPort = rateCard.toTarget(reference.providerConnection(b.getProvider(),
                ConnectionModel.HOSTED, b.getBandwidthMbps()));
        if (!cspPort.isPriced()) {
            unpriced.add("Cloud provider hosted-connection fee: " + cspPort.unpricedReason());
        } else if (!coreCurrency.equals(cspPort.value().getCurrency().getCurrencyCode())) {
            unpriced.add("Cloud provider hosted-connection fee: " + cspPort.value().getCurrency() + " "
                    + cspPort.value().getMonthlyRecurring().toPlainString() + "/month cannot be combined with "
                    + coreCurrency + " components without an exchange rate");
        } else {
            items.put("Cloud provider interconnect port", cspPort.value().getMonthlyRecurring());
            monthly = monthly.add(cspPort.value().getMonthlyRecurring());
        }
        if (!coloCrossConnectPriced && crossConnectCount > 0) {
            Optional<BigDecimal> referenceCrossConnect = reference.equinixCrossConnectMonthly();
            Optional<BigDecimal> crossConnect = referenceCrossConnect.flatMap(v -> rateCard.amount(v,
                    reference.currencyCode()));
            if (referenceCrossConnect.isEmpty()) {
                unpriced.add("Equinix cross-connect: no rate supplied and no reference figure");
            } else if (crossConnect.isEmpty() || !coreCurrency.equals(rateCard.target())) {
                unpriced.add("Equinix cross-connect: the reference figure is " + reference.currencyCode()
                        + " and no exchange rate to " + coreCurrency + " is available");
            } else {
                BigDecimal crossConnectMonthly = crossConnect.get().multiply(BigDecimal.valueOf(crossConnectCount));
                items.put(countedLabel("Equinix cross-connect", crossConnectCount, crossConnect.get()),
                        crossConnectMonthly);
                monthly = monthly.add(crossConnectMonthly);
            }
        }

        return CostBreakdown.builder()
                .archetype(DeploymentArchetype.EQUINIX_INTERCONNECT)
                .monthlyTotal(monthly).setupTotal(setup)
                .currency(coreCurrency).lineItems(items).priced(unpriced.isEmpty())
                .note(unpriced.isEmpty() ? null : String.join(" ", unpriced))
                .unpriced(unpriced)
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
        // configured power draw before being folded in - previously it was never consumed at all.
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

    // Cloud-to-cloud (two-sided) forms

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
                .note(note).provenance(provenance).unpriced(note == null ? List.of() : List.of(note))
                .build();
    }

    /**
     * Equinix interconnect, both clouds: private egress in both directions, one Fabric virtual
     * connection per cloud, the optional Cloud Router (one serves both connections), the
     * colocation primitives, and the CSP interconnect-port reference figure for BOTH clouds. A
     * missing component on either side leaves the archetype partially priced, so it is never
     * ranked on an incomplete total against the native path.
     */
    private static CostBreakdown equinixInterconnectTwoSided(TcoCalculator.Builder b, ConvertingRateCard rateCard,
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
                ? Optional.empty() : reference.equinixCrossConnectMonthly()
                        .flatMap(v -> rateCard.amount(v, reference.currencyCode()));
        if (coreCurrency.equals(rateCard.target())) {
            for (CloudProviderType cloud : List.of(b.getProvider(), b.getPeerProvider())) {
                PriceLookup<PriceQuote> cspPort = rateCard.toTarget(reference.providerConnection(cloud,
                        ConnectionModel.HOSTED, b.getBandwidthMbps()));
                if (cspPort.isPriced()) {
                    items.put("Cloud provider interconnect port (" + cloud + ")", cspPort.value().getMonthlyRecurring());
                    monthly = monthly.add(cspPort.value().getMonthlyRecurring());
                    provenance.add("Cloud provider interconnect port (" + cloud + "): " + cspPort.value().getNote()
                            + ", reference data as of " + reference.asOf() + ".");
                } else {
                    partial.add("Cloud provider hosted-connection fee (" + cloud + "): " + cspPort.unpricedReason()
                            + "; this archetype's totals are partial and exclude it.");
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
                .provenance(provenance).unpriced(partial)
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
                    .note(none).provenance(provenance).unpriced(List.of(none))
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
                .note(note).provenance(provenance).unpriced(partial)
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
     * the per-currency subtotals - the same convention the single-cloud Equinix archetype uses.
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
                .unpriced(priorNote == null ? List.of(mixNote) : List.of(priorNote, mixNote))
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

    private static BigDecimal orReference(BigDecimal override, ReferenceRateCard reference, String key,
                                          ConvertingRateCard rateCard) {
        if (override != null) {
            return override;
        }
        return reference.onPrem(key).flatMap(v -> rateCard.amount(v, reference.currencyCode())).orElse(null);
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
