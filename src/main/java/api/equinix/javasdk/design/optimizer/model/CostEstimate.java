package api.equinix.javasdk.design.optimizer.model;

import api.equinix.javasdk.design.value.ratecard.PriceSource;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.List;

/**
 * Aggregated cost estimate across all recommended metros.
 */
@Value
@Builder
public class CostEstimate {

    BigDecimal monthlyTotal;
    BigDecimal setupTotal;
    String currency;
    List<MetroCostBreakdown> perMetro;
    boolean withinBudget;
    String costDisclaimer;

    /**
     * Aggregate provenance across the per-metro figures: a single
     * {@link PriceSource} when every metro shares it (e.g. all
     * {@link PriceSource#EQUINIX_LIVE} or all {@link PriceSource#ESTIMATE}),
     * otherwise {@link PriceSource#COMPOSITE} for a mix of live and heuristic.
     */
    PriceSource source;
}
