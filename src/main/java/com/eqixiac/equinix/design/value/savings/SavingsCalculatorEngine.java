package com.eqixiac.equinix.design.value.savings;

import com.eqixiac.equinix.design.value.CurrencyReconciler;
import com.eqixiac.equinix.design.value.ratecard.EgressPath;
import com.eqixiac.equinix.design.value.ratecard.EgressRate;
import com.eqixiac.equinix.design.value.ratecard.PriceQuote;
import com.eqixiac.equinix.design.value.ratecard.RateCard;
import com.eqixiac.equinix.design.value.ratecard.Term;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Optional;

/**
 * Internal engine that turns a {@link SavingsCalculator.Builder} configuration
 * into a {@link SavingsEstimate}. Stateless.
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
                .build();
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
