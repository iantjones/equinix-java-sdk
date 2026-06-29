package api.equinix.javasdk.design.optimizer.wizard.model;

import api.equinix.javasdk.design.value.ratecard.PriceSource;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Aggregated pricing breakdown for a complete deployment plan, including
 * Cloud Router costs, provider connection costs, and backbone link costs.
 */
@Value
@Builder
public class PlanPricing {

    BigDecimal monthlyTotal;

    BigDecimal setupTotal;

    @Builder.Default
    String currency = "USD";

    BigDecimal routerMonthlyCost;

    BigDecimal providerConnectionMonthlyCost;

    BigDecimal backboneMonthlyCost;

    Map<String, BigDecimal> perConnectionCost;

    /**
     * Dominant provenance of these figures: {@link PriceSource#EQUINIX_LIVE} when every line
     * item was live-priced, {@link PriceSource#ESTIMATE} when the heuristic fallback was used,
     * or {@link PriceSource#COMPOSITE} when mixed. Read alongside {@link #disclaimer}.
     */
    @Builder.Default
    PriceSource source = PriceSource.ESTIMATE;

    @Builder.Default
    String disclaimer = "Estimates based on published Fabric pricing. Actual costs may vary based on contract terms, volume discounts, and promotional offers.";
}
