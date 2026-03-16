package api.equinix.javasdk.fabric.optimizer.wizard.model;

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

    /** Pricing disclaimer. */
    @Builder.Default
    String disclaimer = "Estimates based on published Fabric pricing. Actual costs may vary based on contract terms, volume discounts, and promotional offers.";
}
