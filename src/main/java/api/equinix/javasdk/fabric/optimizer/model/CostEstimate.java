package api.equinix.javasdk.fabric.optimizer.model;

import lombok.Value;

import java.math.BigDecimal;
import java.util.List;

/**
 * Aggregated cost estimate across all recommended metros.
 */
@Value
public class CostEstimate {

    BigDecimal monthlyTotal;
    BigDecimal setupTotal;
    String currency;
    List<MetroCostBreakdown> perMetro;
    boolean withinBudget;
    String costDisclaimer;
}
