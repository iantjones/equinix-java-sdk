package api.equinix.javasdk.fabric.optimizer.model;

import api.equinix.javasdk.core.enums.MetroCode;
import lombok.Value;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Estimated cost breakdown for a single metro.
 */
@Value
public class MetroCostBreakdown {

    MetroCode metroCode;
    BigDecimal monthlyRecurring;
    BigDecimal nonRecurring;
    Map<String, BigDecimal> lineItems;

    public BigDecimal total() {
        return monthlyRecurring.add(nonRecurring);
    }
}
