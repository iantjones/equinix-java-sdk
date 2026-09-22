package com.eqixiac.equinix.design.value.savings;

import com.eqixiac.equinix.design.value.CurrencyReconciler;
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
import java.util.Currency;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Internal engine that turns a {@link SavingsCalculator.Builder} configuration
 * into a {@link SavingsEstimate}. Stateless.
 *
 * <p>The single-cloud figures are computed the same way whether or not a peer cloud is set. A
 * peer cloud adds {@link #compareMulticloudPaths}, which prices the traffic between the two
 * clouds over three paths and derives the break-even sustained rate.</p>
 */
final class SavingsCalculatorEngine {

    private static final Currency USD = Currency.getInstance("USD");

    private SavingsCalculatorEngine() {}

    static SavingsEstimate compute(SavingsCalculator.Builder b) {
        // Default: live Equinix interconnect pricing, then bundled reference figures
        // (which also supply the cloud egress rates the savings calculation needs).
        RateCard rateCard = b.getRateCard() != null ? b.getRateCard() : RateCard.standardChain(b.getFabric());
        Term term = b.getTerm();

        BigDecimal gb = b.getEgressUnit().toGigabytes(BigDecimal.valueOf(b.getEgressAmount()));

        // ── Egress rates (internet vs private) ──
        Optional<EgressRate> internet = rateCard.egress(b.getProvider(), b.getRegion(), EgressPath.INTERNET, term);
        Optional<EgressRate> priv = rateCard.egress(b.getProvider(), b.getRegion(), EgressPath.PRIVATE, term);
        Currency internetCur = internet.map(EgressRate::getCurrency).orElse(null);
        Currency privCur = priv.map(EgressRate::getCurrency).orElse(null);
        // internet − private is a subtraction, so the two rates must be in one currency. If they are
        // known to differ, egress savings cannot be computed (rather than fabricated).
        boolean egressCurrencyMismatch = CurrencyReconciler.knownDifferent(internetCur, privCur);
        boolean egressPriced = internet.isPresent() && priv.isPresent() && !egressCurrencyMismatch;

        BigDecimal internetRate = internet.map(EgressRate::getPricePerGb).orElse(BigDecimal.ZERO);
        BigDecimal privateRate = priv.map(EgressRate::getPricePerGb).orElse(BigDecimal.ZERO);
        BigDecimal internetCost = internetRate.multiply(gb);
        BigDecimal privateCost = privateRate.multiply(gb);
        BigDecimal egressSavings = egressPriced ? internetCost.subtract(privateCost) : BigDecimal.ZERO;
        if (!egressPriced) {
            // Only one (or neither) egress rate resolved, or the two are in different currencies —
            // don't surface a half-populated or cross-currency comparison; zero the per-line figures
            // so the report is internally consistent.
            internetRate = BigDecimal.ZERO;
            privateRate = BigDecimal.ZERO;
            internetCost = BigDecimal.ZERO;
            privateCost = BigDecimal.ZERO;
        }
        // The currency egress figures (and thus egressSavings) are expressed in.
        Currency egressCurrency = internetCur != null ? internetCur : privCur;

        // ── Equinix interconnect cost ──
        Optional<PriceQuote> connection = rateCard.connection(
                b.getConnectionType(), b.getBandwidthMbps(), b.getMetro(), term);
        boolean connectionPriced = connection.isPresent();
        Currency equinixCurrency = connection.map(PriceQuote::getCurrency).orElse(null);
        BigDecimal equinixMonthly = BigDecimal.ZERO;
        BigDecimal equinixSetup = BigDecimal.ZERO;
        if (connection.isPresent()) {
            equinixMonthly = connection.get().getMonthlyRecurring();
            equinixSetup = connection.get().getNonRecurring();
        }
        // A requested Cloud Router that cannot be folded in leaves the interconnect figures
        // PARTIAL (connection only) — tracked with the exact reason so the disclaimer can name
        // the component rather than misstate the whole interconnect cost as unavailable.
        String routerExclusionReason = null;
        if (b.isIncludeRouter()) {
            Optional<PriceQuote> router = rateCard.cloudRouter(b.getRouterPackage(), b.getMetro(), term);
            if (router.isEmpty()) {
                routerExclusionReason = " The Fabric Cloud Router (" + b.getRouterPackage()
                        + ") was requested but could not be priced by the rate card; the Equinix interconnect "
                        + "figures are partial and exclude the Cloud Router.";
            } else if (CurrencyReconciler.knownDifferent(equinixCurrency, router.get().getCurrency())) {
                // Connection + router are summed, so they must share a currency.
                routerExclusionReason = " The Fabric Cloud Router (" + b.getRouterPackage() + ") is priced in "
                        + router.get().getCurrency() + " but the Fabric connection is priced in "
                        + equinixCurrency + "; they cannot be summed without an FX rate, so the Equinix "
                        + "interconnect figures are partial and exclude the Cloud Router.";
            } else {
                equinixMonthly = equinixMonthly.add(router.get().getMonthlyRecurring());
                equinixSetup = equinixSetup.add(router.get().getNonRecurring());
            }
        }
        boolean equinixPriced = connectionPriced && routerExclusionReason == null;

        // ── Cross-currency reconciliation (egress vs interconnect) ──
        // The estimate is stamped with a single currency; figures in another currency must never
        // be rendered under that label. When the interconnect cost is in a different currency from
        // the egress figures, it is excluded (zeroed) and reported via the disclaimer instead —
        // the mixed/unpriced convention — and everything derived from it is omitted.
        boolean crossCurrencyMismatch = CurrencyReconciler.knownDifferent(egressCurrency, equinixCurrency);
        String excludedEquinixFigures = null;
        if (crossCurrencyMismatch) {
            excludedEquinixFigures = equinixCurrency + " "
                    + equinixMonthly.setScale(2, RoundingMode.HALF_UP).toPlainString() + "/mo"
                    + (equinixSetup.signum() != 0
                        ? " plus " + equinixCurrency + " "
                            + equinixSetup.setScale(2, RoundingMode.HALF_UP).toPlainString() + " one-time"
                        : "");
            equinixMonthly = BigDecimal.ZERO;
            equinixSetup = BigDecimal.ZERO;
            equinixPriced = false;
        }

        // ── Net savings & break-even ──
        // netMonthly nets the egress saving against the interconnect cost, so it is only meaningful
        // when the two are in the same currency. When they are known to differ, the net (and the
        // figures derived from it) are omitted rather than reported as a cross-currency subtraction.
        BigDecimal netMonthly = null;
        BigDecimal annualNet = null;
        BigDecimal firstYearNet = null;
        if (!crossCurrencyMismatch) {
            netMonthly = egressSavings.subtract(equinixMonthly);
            annualNet = netMonthly.multiply(BigDecimal.valueOf(12));
            firstYearNet = annualNet.subtract(equinixSetup);
        }

        BigDecimal perGbDelta = internetRate.subtract(privateRate);
        // break-even divides the interconnect monthly (equinix currency) by the per-GB delta (egress
        // currency), so it too needs a single currency.
        BigDecimal breakEvenGb = (egressPriced && !crossCurrencyMismatch && perGbDelta.signum() > 0)
                ? equinixMonthly.divide(perGbDelta, 2, RoundingMode.HALF_UP)
                : null;
        BigDecimal paybackMonths = (netMonthly != null && netMonthly.signum() > 0 && equinixSetup.signum() > 0)
                ? equinixSetup.divide(netMonthly, 1, RoundingMode.HALF_UP)
                : null;

        Currency currency = resolveCurrency(internet, priv, connection);
        boolean complete = egressPriced && equinixPriced && !crossCurrencyMismatch;

        StringBuilder disclaimer = new StringBuilder(
                "Design-time estimate, not a quote. Equinix interconnect costs use live Fabric pricing where "
                        + "available; egress rates are indicative reference or caller-supplied figures. Actual costs "
                        + "depend on region, tiering, volume, and contract terms. Excludes per-provider free-tier "
                        + "egress allowances and compute/storage costs.");
        if (egressCurrencyMismatch) {
            disclaimer.append(" The internet (").append(internetCur).append(") and private (").append(privCur)
                    .append(") egress rates are in different currencies, so egress savings could not be computed "
                            + "without an FX rate.");
        } else if (!egressPriced) {
            disclaimer.append(" Egress rates were unavailable from the rate card, so egress savings could not be computed.");
        }
        // Name exactly which interconnect component (if any) could not be priced, so the
        // disclaimer never misstates a partially priced interconnect as wholly unavailable.
        if (!connectionPriced) {
            disclaimer.append(" The Equinix Fabric connection cost was unavailable from the rate card.");
        }
        if (routerExclusionReason != null) {
            disclaimer.append(routerExclusionReason);
        }
        if (b.getPeerProvider() != null) {
            disclaimer.append(" The cloud-to-cloud section prices traffic in both directions; its native "
                    + "multicloud link figures are the providers' published hourly list rates converted at ")
                    .append(MulticloudLinkQuote.HOURS_PER_MONTH)
                    .append(" h/month, and a side with no published rate is reported unpriced, not estimated.");
        }
        if (crossCurrencyMismatch) {
            disclaimer.append(" Egress figures are in ").append(egressCurrency)
                    .append(" but the Equinix interconnect cost (").append(excludedEquinixFigures)
                    .append(") is in ").append(equinixCurrency)
                    .append("; without an FX rate it cannot be restated in ").append(egressCurrency)
                    .append(", so the interconnect figures are excluded (reported as zero) and the net, "
                            + "annual, first-year, break-even, and payback figures are omitted.");
        }

        return SavingsEstimate.builder()
                .monthlyEgressGb(gb)
                .provider(b.getProvider())
                .region(b.getRegion())
                .metro(b.getMetro())
                .internetRatePerGb(internetRate)
                .privateRatePerGb(privateRate)
                .internetEgressMonthlyCost(internetCost)
                .privateEgressMonthlyCost(privateCost)
                .monthlyEgressSavings(egressSavings)
                .equinixMonthlyCost(equinixMonthly)
                .equinixSetupCost(equinixSetup)
                .netMonthlySavings(netMonthly)
                .annualNetSavings(annualNet)
                .firstYearNetSavings(firstYearNet)
                .breakEvenGbPerMonth(breakEvenGb)
                .paybackMonths(paybackMonths)
                .currency(currency.getCurrencyCode())
                .egressPriced(egressPriced)
                .equinixPriced(equinixPriced)
                .complete(complete)
                .disclaimer(disclaimer.toString())
                .multicloudComparison(b.getPeerProvider() == null ? null
                        : compareMulticloudPaths(b, rateCard, term, gb, internet, priv))
                .build();
    }

    /**
     * Prices the traffic between {@code fromCloud} and the peer cloud over the public internet,
     * the two-sided Equinix path and the native multicloud link, then derives the break-even
     * sustained rate and the lowest-cost path. Every path is priced on the same two volumes.
     * A path with any unpriced input, or whose inputs span currencies, has a {@code null} total
     * and a note naming the cause.
     */
    private static MulticloudPathComparison compareMulticloudPaths(SavingsCalculator.Builder b, RateCard rateCard,
                                                                   Term term, BigDecimal gb,
                                                                   Optional<EgressRate> internetA,
                                                                   Optional<EgressRate> privateA) {
        CloudProviderType a = b.getProvider();
        CloudProviderType z = b.getPeerProvider();
        BigDecimal reverseGb = b.isReverseEgressSet()
                ? b.getReverseEgressUnit().toGigabytes(BigDecimal.valueOf(b.getReverseEgressAmount()))
                : gb;
        List<String> notes = new ArrayList<>();
        ReferenceRateCard reference = ReferenceRateCard.standard();

        // ── Public internet, both directions ──
        Optional<EgressRate> internetZ = rateCard.egress(z, b.getPeerRegion(), EgressPath.INTERNET, term);
        PathSum internet = new PathSum("Public internet path");
        internet.addEgress(a, internetA, gb, "internet", notes);
        internet.addEgress(z, internetZ, reverseGb, "internet", notes);

        // ── Equinix Fabric, both clouds ──
        Optional<EgressRate> privateZ = rateCard.egress(z, b.getPeerRegion(), EgressPath.PRIVATE, term);
        Optional<PriceQuote> connection = rateCard.connection(
                b.getConnectionType(), b.getBandwidthMbps(), b.getMetro(), term);
        PathSum equinix = new PathSum("Equinix path");
        if (connection.isPresent()) {
            BigDecimal two = BigDecimal.valueOf(2);
            equinix.addFixed(connection.get().getCurrency(),
                    connection.get().getMonthlyRecurring().multiply(two),
                    connection.get().getNonRecurring().multiply(two));
            notes.add("Equinix path: two Fabric virtual connections (one per cloud) at " + b.getBandwidthMbps()
                    + " Mbps, " + money(connection.get().getMonthlyRecurring(), connection.get().getCurrency())
                    + "/month each, " + connection.get().getSource() + noteSuffix(connection.get().getNote()) + ".");
        } else {
            equinix.missing("the Fabric connection price", notes);
        }
        if (b.isIncludeRouter()) {
            Optional<PriceQuote> router = rateCard.cloudRouter(b.getRouterPackage(), b.getMetro(), term);
            if (router.isPresent()) {
                equinix.addFixed(router.get().getCurrency(),
                        router.get().getMonthlyRecurring(), router.get().getNonRecurring());
            } else {
                equinix.missing("the Fabric Cloud Router (" + b.getRouterPackage() + ") price", notes);
            }
        } else {
            // Two virtual connections terminate on an Equinix A-side. Without a router in the
            // figures, the fixed cost is low and the break-even is high; say so.
            notes.add("Equinix path: no A-side is priced (no Fabric Cloud Router was requested with "
                    + "includeCloudRouter(...), and this calculator has no colocation lever). The two virtual "
                    + "connections terminate on a Cloud Router, a Network Edge device or a colocation port; its "
                    + "price is missing from the Equinix fixed cost, so the break-even rate is overstated by "
                    + "that amount divided by the per-GB difference.");
        }
        for (CloudProviderType cloud : List.of(a, z)) {
            Optional<PriceQuote> port = reference.cspInterconnectPortMonthlyQuote(cloud, b.getBandwidthMbps());
            if (port.isPresent()) {
                equinix.addFixed(port.get().getCurrency(), port.get().getMonthlyRecurring(), BigDecimal.ZERO);
                notes.add("Equinix path: " + cloud + " interconnect port "
                        + money(port.get().getMonthlyRecurring(), port.get().getCurrency()) + "/month, "
                        + port.get().getSource() + noteSuffix(port.get().getNote()) + ", reference data as of "
                        + reference.asOf() + ".");
            } else {
                equinix.missing("a reference CSP interconnect-port figure for " + cloud, notes);
            }
        }
        equinix.addEgress(a, privateA, gb, "private", notes);
        equinix.addEgress(z, privateZ, reverseGb, "private", notes);

        // ── Native multicloud link ──
        MulticloudLinkRequest request = MulticloudLinkRequest.builder()
                .providerA(a).regionA(b.getRegion())
                .providerZ(z).regionZ(b.getPeerRegion())
                .bandwidthMbps(b.getBandwidthMbps())
                .pathTier(b.getPathTier())
                .term(term)
                .useAwsFreeTier(b.isUseAwsFreeTier())
                .build();
        Optional<MulticloudLinkQuote> linkQuote = rateCard.multicloudLink(request);
        Optional<EgressRate> nativeRateA = rateCard.egress(a, b.getRegion(), EgressPath.MULTICLOUD_INTERCONNECT, term);
        Optional<EgressRate> nativeRateZ = rateCard.egress(
                z, b.getPeerRegion(), EgressPath.MULTICLOUD_INTERCONNECT, term);
        PathSum nativePath = new PathSum("Native multicloud path");
        if (linkQuote.isEmpty()) {
            nativePath.missing("any native multicloud link price for " + a + " <-> " + z
                    + " (no rate card in the chain holds one)", notes);
        } else {
            MulticloudLinkQuote quote = linkQuote.get();
            for (CloudProviderType cloud : List.of(a, z)) {
                Optional<PriceQuote> side = quote.side(cloud);
                if (side.isPresent()) {
                    nativePath.addFixed(side.get().getCurrency(),
                            side.get().getMonthlyRecurring(), side.get().getNonRecurring());
                    notes.add("Native path, " + cloud + " side: " + side.get().getSource()
                            + noteSuffix(side.get().getNote()) + ".");
                }
            }
            quote.unpricedSummary().ifPresent(summary -> {
                nativePath.incomplete = true;
                notes.add("Native multicloud path unpriced. " + summary);
            });
            notes.addAll(quote.getNotes());
        }
        nativePath.addEgress(a, nativeRateA, gb, "native link", notes);
        nativePath.addEgress(z, nativeRateZ, reverseGb, "native link", notes);

        // ── One comparison currency; a path in another currency is withheld, never converted ──
        String currency = comparisonCurrency(internet, equinix, nativePath);
        for (PathSum path : List.of(internet, equinix, nativePath)) {
            path.settle(currency, notes);
        }

        // ── Break-even (a property of the prices, not of the declared volumes) ──
        BigDecimal breakEven = null;
        boolean nativeCheaperAbove = false;
        Map<EgressPath, BigDecimal> totals = new EnumMap<>(EgressPath.class);
        if (internet.total() != null) {
            totals.put(EgressPath.INTERNET, internet.total());
        }
        if (equinix.total() != null) {
            totals.put(EgressPath.PRIVATE, equinix.total());
        }
        if (nativePath.total() != null) {
            totals.put(EgressPath.MULTICLOUD_INTERCONNECT, nativePath.total());
        }
        if (equinix.total() != null && nativePath.total() != null) {
            // Both paths are fully priced, so all four per-GB rates are present. The path totals
            // are in the comparison currency, but a per-GB RATE only enters a path's currency
            // check through a non-zero cost: at a declared volume of zero the rates were never
            // checked, so they are checked here before they are summed and divided.
            List<String> offCurrency = new ArrayList<>();
            for (Map.Entry<String, EgressRate> rate : Map.of(
                    a + " private", privateA.get(), z + " private", privateZ.get(),
                    a + " native link", nativeRateA.get(), z + " native link", nativeRateZ.get()).entrySet()) {
                Currency rateCurrency = rate.getValue().getCurrency();
                if (rateCurrency == null || !rateCurrency.getCurrencyCode().equals(currency)) {
                    offCurrency.add(rate.getKey() + " (" + rateCurrency + ")");
                }
            }
            if (!offCurrency.isEmpty()) {
                notes.add("Break-even not computed: the per-GB rates must all be in " + currency
                        + " and these are not: " + String.join(", ", new java.util.TreeSet<>(offCurrency))
                        + ". Rates in different currencies are not summed and no FX rate is applied.");
            } else {
                BigDecimal perGbEquinix = privateA.get().getPricePerGb().add(privateZ.get().getPricePerGb());
                BigDecimal perGbNative = nativeRateA.get().getPricePerGb().add(nativeRateZ.get().getPricePerGb());
                Optional<BigDecimal> computed = MulticloudPathComparison.breakEvenSustainedMbps(
                        nativePath.fixed, equinix.fixed, perGbEquinix, perGbNative);
                if (computed.isPresent()) {
                    breakEven = computed.get();
                    nativeCheaperAbove = MulticloudPathComparison.nativeCheaperAboveBreakEven(nativePath.fixed, equinix.fixed);
                    BigDecimal capacity = BigDecimal.valueOf(2L * b.getBandwidthMbps());
                    notes.add("Break-even = 2 x ((native fixed " + money(nativePath.fixed, currency) + " - Equinix fixed "
                            + money(equinix.fixed, currency) + ") / (" + perGbEquinix.toPlainString() + " - "
                            + perGbNative.toPlainString() + " " + currency + "/GB)) GB each way x "
                            + MulticloudPathComparison.MEGABITS_PER_GB + " Mb/GB / ("
                            + MulticloudLinkQuote.HOURS_PER_MONTH + " h x 3600 s), symmetric traffic assumed. "
                            + (nativeCheaperAbove
                                ? "Below the break-even rate the Equinix path costs less per month; above it the native path does."
                                : "The native link's fixed fee is below the Equinix path's fixed cost and its per-GB rate is "
                                    + "above the Equinix path's, so the sense is reversed: below the break-even rate the "
                                    + "native path costs less per month; above it the Equinix path does."));
                    if (breakEven.compareTo(capacity) > 0) {
                        notes.add("The break-even rate exceeds the link's capacity (" + capacity.toPlainString()
                                + " Mbps summed over both directions at " + b.getBandwidthMbps()
                                + " Mbps), so at this link size the "
                                + (nativeCheaperAbove ? "Equinix" : "native")
                                + " path costs less at every achievable sustained rate.");
                    }
                } else {
                    EgressPath dominant = MulticloudPathComparison.dominatesAtEveryVolume(
                            nativePath.fixed, equinix.fixed, perGbEquinix, perGbNative).orElseThrow();
                    notes.add(dominant == EgressPath.MULTICLOUD_INTERCONNECT
                            ? "No break-even: the native link's fixed fee does not exceed the Equinix path's fixed "
                                + "cost and its per-GB rate does not exceed the Equinix path's, so the native path "
                                + "costs the same or less at every volume."
                            : "No break-even: the Equinix path's fixed cost does not exceed the native link's fee "
                                + "and its per-GB rate does not exceed the native link's, so the Equinix path costs "
                                + "the same or less at every volume.");
                }
            }
        } else {
            notes.add("Break-even not computed: it needs both the Equinix path and the native path fully priced "
                    + "in one currency.");
        }

        EgressPath lowest = null;
        if (totals.size() >= 2) {
            for (Map.Entry<EgressPath, BigDecimal> e : totals.entrySet()) {
                if (lowest == null || e.getValue().compareTo(totals.get(lowest)) < 0) {
                    lowest = e.getKey();
                }
            }
        }

        return MulticloudPathComparison.builder()
                .providerA(a).regionA(b.getRegion())
                .providerZ(z).regionZ(b.getPeerRegion())
                .bandwidthMbps(b.getBandwidthMbps())
                .pathTier(b.getPathTier())
                .forwardEgressGb(gb)
                .reverseEgressGb(reverseGb)
                .reverseEgressAssumedSymmetric(!b.isReverseEgressSet())
                .internetMonthlyCost(internet.total())
                .equinixFixedMonthlyCost(equinix.settledFixed())
                .equinixEgressMonthlyCost(equinix.settledTransfer())
                .equinixMonthlyCost(equinix.total())
                .equinixSetupCost(equinix.settledSetup())
                .nativeFixedMonthlyCost(nativePath.settledFixed())
                .nativeDataTransferMonthlyCost(nativePath.settledTransfer())
                .nativeMonthlyCost(nativePath.total())
                .nativeLinkQuote(linkQuote.orElse(null))
                .breakEvenSustainedMbps(breakEven)
                .nativeCheaperAboveBreakEven(nativeCheaperAbove)
                .lowestCostPath(lowest)
                .currency(currency)
                .notes(notes)
                .build();
    }

    /**
     * The currency the comparison is stated in. The break-even is a relation between the Equinix
     * path and the native path only, so when both are complete and priced in one currency that
     * currency wins, whatever the internet path is priced in (the internet path is then withheld
     * if it differs). Otherwise the first complete single-currency path in list order supplies
     * it, then the first single-currency path, then USD.
     */
    private static String comparisonCurrency(PathSum internet, PathSum equinix, PathSum nativePath) {
        String equinixCurrency = equinix.recon.soleCurrency();
        String nativeCurrency = nativePath.recon.soleCurrency();
        if (!equinix.incomplete && !nativePath.incomplete
                && CurrencyReconciler.sameKnownCurrency(equinixCurrency, nativeCurrency)) {
            return equinixCurrency;
        }
        PathSum[] paths = {internet, equinix, nativePath};
        for (PathSum path : paths) {
            String sole = path.recon.soleCurrency();
            if (!path.incomplete && sole != null) {
                return sole;
            }
        }
        for (PathSum path : paths) {
            String sole = path.recon.soleCurrency();
            if (sole != null) {
                return sole;
            }
        }
        return USD.getCurrencyCode();
    }

    private static String money(BigDecimal amount, Object currency) {
        return currency + " " + amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String noteSuffix(String note) {
        return note == null || note.isBlank() ? "" : " (" + note + ")";
    }

    /**
     * Accumulates one path's fixed and per-GB components. The path has a total only when every
     * component was present and all of them share the comparison currency. A zero amount is
     * currency-invariant and never creates a currency mix.
     */
    private static final class PathSum {
        private final String label;
        private final CurrencyReconciler recon = CurrencyReconciler.create();
        private BigDecimal fixed = BigDecimal.ZERO;
        private BigDecimal setup = BigDecimal.ZERO;
        private BigDecimal transfer = BigDecimal.ZERO;
        private boolean incomplete;
        private boolean settled;

        PathSum(String label) {
            this.label = label;
        }

        void addFixed(Currency currency, BigDecimal monthly, BigDecimal oneTime) {
            fixed = fixed.add(monthly);
            setup = setup.add(oneTime);
            if (monthly.signum() != 0 || oneTime.signum() != 0) {
                recon.add(currency, monthly, oneTime);
            }
        }

        void addEgress(CloudProviderType cloud, Optional<EgressRate> rate, BigDecimal volumeGb, String pathName,
                       List<String> notes) {
            if (rate.isEmpty()) {
                missing("the " + pathName + " per-GB rate for " + cloud, notes);
                return;
            }
            BigDecimal cost = rate.get().costFor(volumeGb);
            transfer = transfer.add(cost);
            if (cost.signum() != 0) {
                recon.add(rate.get().getCurrency(), cost, BigDecimal.ZERO);
            }
            notes.add(label + ": " + cloud + " " + pathName + " rate " + rate.get().getPricePerGb().toPlainString()
                    + " " + rate.get().getCurrency() + "/GB, " + rate.get().getSource()
                    + noteSuffix(rate.get().getNote()) + ".");
        }

        void missing(String what, List<String> notes) {
            incomplete = true;
            notes.add(label + " unpriced: the rate card could not supply " + what + ".");
        }

        /** Fixes the path against the comparison currency; afterwards the totals are final. */
        void settle(String comparisonCurrency, List<String> notes) {
            settled = true;
            if (recon.isMixed() || recon.sawUnknownCurrency()) {
                incomplete = true;
                notes.add(label + " unpriced: its components are in more than one currency ("
                        + recon.describeMonthlySubtotals() + " per month) and cannot be summed without an FX rate.");
            } else if (CurrencyReconciler.knownDifferent(recon.soleCurrency(), comparisonCurrency)) {
                incomplete = true;
                notes.add(label + " withheld: it is priced in " + recon.soleCurrency() + " ("
                        + recon.describeMonthlySubtotals() + " per month) and the comparison is in "
                        + comparisonCurrency + "; it cannot be compared without an FX rate.");
            }
        }

        BigDecimal total() {
            return !settled || incomplete ? null : fixed.add(transfer);
        }

        BigDecimal settledFixed() {
            return incomplete ? null : fixed;
        }

        BigDecimal settledTransfer() {
            return incomplete ? null : transfer;
        }

        BigDecimal settledSetup() {
            return incomplete ? null : setup;
        }
    }

    private static Currency resolveCurrency(Optional<EgressRate> internet, Optional<EgressRate> priv,
                                            Optional<PriceQuote> connection) {
        if (internet.isPresent() && internet.get().getCurrency() != null) {
            return internet.get().getCurrency();
        }
        if (priv.isPresent() && priv.get().getCurrency() != null) {
            return priv.get().getCurrency();
        }
        if (connection.isPresent() && connection.get().getCurrency() != null) {
            return connection.get().getCurrency();
        }
        return USD;
    }
}
