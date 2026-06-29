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

    /** Total estimated monthly recurring cost for the full deployment. */
    BigDecimal monthlyTotal;

    /** Total estimated one-time setup cost. */
    BigDecimal setupTotal;

    /** Currency code (default: USD). */
    @Builder.Default
    String currency = "USD";

    /** Monthly cost for all Cloud Routers. */
    BigDecimal routerMonthlyCost;

    /** Monthly cost for all provider connections. */
    BigDecimal providerConnectionMonthlyCost;

    /** Monthly cost for all backbone links. */
    BigDecimal backboneMonthlyCost;

    /** Per-connection cost breakdown: connection name to monthly cost. */
    Map<String, BigDecimal> perConnectionCost;

    /**
     * Dominant provenance of these figures: {@link PriceSource#EQUINIX_LIVE} when every line
     * item was live-priced, {@link PriceSource#ESTIMATE} when the heuristic fallback was used,
     * or {@link PriceSource#COMPOSITE} when mixed. Read alongside {@link #disclaimer}.
     */
    @Builder.Default
    PriceSource source = PriceSource.ESTIMATE;

    /** Pricing disclaimer, reflecting the actual {@link #source}. */
    @Builder.Default
    String disclaimer = "Estimates based on published Fabric pricing. Actual costs may vary based on contract terms, volume discounts, and promotional offers.";
}
