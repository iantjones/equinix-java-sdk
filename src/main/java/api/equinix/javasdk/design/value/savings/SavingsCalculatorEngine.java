package api.equinix.javasdk.design.value.savings;

import api.equinix.javasdk.design.value.ratecard.EgressPath;
import api.equinix.javasdk.design.value.ratecard.EgressRate;
import api.equinix.javasdk.design.value.ratecard.EquinixRateCard;
import api.equinix.javasdk.design.value.ratecard.PriceQuote;
import api.equinix.javasdk.design.value.ratecard.RateCard;
import api.equinix.javasdk.design.value.ratecard.ReferenceRateCard;
import api.equinix.javasdk.design.value.ratecard.Term;

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
        RateCard rateCard = b.getRateCard() != null
                ? b.getRateCard()
                : RateCard.layered(EquinixRateCard.of(b.getFabric()), ReferenceRateCard.standard());
        Term term = b.getTerm();

        BigDecimal gb = b.getEgressUnit().toGigabytes(BigDecimal.valueOf(b.getEgressAmount()));

        // ── Egress rates (internet vs private) ──
        Optional<EgressRate> internet = rateCard.egress(b.getProvider(), b.getRegion(), EgressPath.INTERNET, term);
        Optional<EgressRate> priv = rateCard.egress(b.getProvider(), b.getRegion(), EgressPath.PRIVATE, term);
        boolean egressPriced = internet.isPresent() && priv.isPresent();

        BigDecimal internetRate = internet.map(EgressRate::getPricePerGb).orElse(BigDecimal.ZERO);
        BigDecimal privateRate = priv.map(EgressRate::getPricePerGb).orElse(BigDecimal.ZERO);
        BigDecimal internetCost = internetRate.multiply(gb);
        BigDecimal privateCost = privateRate.multiply(gb);
        BigDecimal egressSavings = egressPriced ? internetCost.subtract(privateCost) : BigDecimal.ZERO;

        // ── Equinix interconnect cost ──
        Optional<PriceQuote> connection = rateCard.connection(
                b.getConnectionType(), b.getBandwidthMbps(), b.getMetro(), term);
        boolean equinixPriced = connection.isPresent();
        BigDecimal equinixMonthly = BigDecimal.ZERO;
        BigDecimal equinixSetup = BigDecimal.ZERO;
        if (connection.isPresent()) {
            equinixMonthly = connection.get().getMonthlyRecurring();
            equinixSetup = connection.get().getNonRecurring();
        }
        if (b.isIncludeRouter()) {
            Optional<PriceQuote> router = rateCard.cloudRouter(b.getRouterPackage(), b.getMetro(), term);
            if (router.isPresent()) {
                equinixMonthly = equinixMonthly.add(router.get().getMonthlyRecurring());
                equinixSetup = equinixSetup.add(router.get().getNonRecurring());
            } else {
                equinixPriced = false;
            }
        }

        // ── Net savings & break-even ──
        BigDecimal netMonthly = egressSavings.subtract(equinixMonthly);
        BigDecimal annualNet = netMonthly.multiply(BigDecimal.valueOf(12));
        BigDecimal firstYearNet = annualNet.subtract(equinixSetup);

        BigDecimal perGbDelta = internetRate.subtract(privateRate);
        BigDecimal breakEvenGb = (egressPriced && perGbDelta.signum() > 0)
                ? equinixMonthly.divide(perGbDelta, 2, RoundingMode.HALF_UP)
                : null;
        BigDecimal paybackMonths = (netMonthly.signum() > 0 && equinixSetup.signum() > 0)
                ? equinixSetup.divide(netMonthly, 1, RoundingMode.HALF_UP)
                : null;

        Currency currency = resolveCurrency(internet, priv, connection);
        boolean complete = egressPriced && equinixPriced;

        StringBuilder disclaimer = new StringBuilder(
                "Design-time estimate, not a quote. Equinix interconnect costs use live Fabric pricing where "
                        + "available; egress rates are indicative reference or caller-supplied figures. Actual costs "
                        + "depend on region, tiering, volume, and contract terms.");
        if (!egressPriced) {
            disclaimer.append(" Egress rates were unavailable from the rate card, so egress savings could not be computed.");
        }
        if (!equinixPriced) {
            disclaimer.append(" The Equinix interconnect cost was unavailable from the rate card.");
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
