package api.equinix.javasdk.design.value.tco;

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

        // Recommend the cheapest priced archetype.
        CostBreakdown recommended = breakdowns.stream()
                .filter(CostBreakdown::isPriced)
                .min((x, y) -> x.getMonthlyTotal().compareTo(y.getMonthlyTotal()))
                .orElse(null);

        Optional<CostBreakdown> baseline = breakdowns.stream()
                .filter(c -> c.getArchetype() == DeploymentArchetype.PUBLIC_CLOUD_INTERNET && c.isPriced())
                .findFirst();

        BigDecimal monthlySavings = null;
        BigDecimal annualSavings = null;
        if (recommended != null && baseline.isPresent()) {
            monthlySavings = baseline.get().getMonthlyTotal().subtract(recommended.getMonthlyTotal());
            annualSavings = monthlySavings.multiply(BigDecimal.valueOf(12));
        }

        String disclaimer = "Design-time TCO estimate, not a quote. Equinix Fabric connection costs use live "
                + "pricing where available; cloud-egress, cloud-provider interconnect-port, cross-connect, and "
                + "on-prem figures are indicative reference midpoints (the on-prem inputs are coarse and "
                + "overridable). Compute, storage, software, staffing, and per-provider free-tier egress allowances "
                + "are out of scope. Actual costs depend on region, volume, tiering, and contract terms.";

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

        BigDecimal egressCost = privateEgress.map(r -> r.costFor(gb)).orElse(BigDecimal.ZERO);
        BigDecimal monthly = BigDecimal.ZERO;
        BigDecimal setup = BigDecimal.ZERO;
        String note = priced ? null : "Private egress rate or Equinix connection price unavailable.";

        if (privateEgress.isPresent()) {
            items.put("Cloud egress (private interconnect)", egressCost);
            monthly = monthly.add(egressCost);
        }
        if (connection.isPresent()) {
            items.put("Equinix Fabric connection", connection.get().getMonthlyRecurring());
            monthly = monthly.add(connection.get().getMonthlyRecurring());
            setup = setup.add(connection.get().getNonRecurring());
        }
        if (b.isIncludeRouter()) {
            Optional<PriceQuote> router = rateCard.cloudRouter(b.getRouterPackage(), b.getMetro(), term);
            if (router.isPresent()) {
                items.put("Fabric Cloud Router", router.get().getMonthlyRecurring());
                monthly = monthly.add(router.get().getMonthlyRecurring());
                setup = setup.add(router.get().getNonRecurring());
            }
        }

        // The CSP interconnect port and Equinix cross-connect come from the bundled reference
        // card (USD). Only fold them in when the comparison currency matches, so a non-USD
        // custom rate card never silently mixes currencies into the total.
        Optional<BigDecimal> cspPort = reference.cspInterconnectPortMonthly(b.getProvider(), b.getBandwidthMbps());
        Optional<BigDecimal> crossConnect = reference.equinixCrossConnectMonthly();
        if (currency.equals(reference.currencyCode())) {
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
                    + reference.currencyCode() + " and cannot be mixed with " + currency + ".";
            note = note == null ? skip : note + " " + skip;
        }

        return CostBreakdown.builder()
                .archetype(DeploymentArchetype.EQUINIX_INTERCONNECT)
                .monthlyTotal(monthly).setupTotal(setup)
                .currency(currency).lineItems(items).priced(priced)
                .note(note)
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
