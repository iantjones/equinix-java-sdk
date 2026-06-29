package api.equinix.javasdk.design.optimizer.model;

import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.design.value.ratecard.PriceSource;
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

    /**
     * Provenance of this metro's figures: {@link PriceSource#EQUINIX_LIVE} (or
     * {@code CUSTOM}/{@code REFERENCE}) when priced from the rate card, or
     * {@link PriceSource#ESTIMATE} when the regional pricing heuristic was used.
     */
    PriceSource source;

    public BigDecimal total() {
        return monthlyRecurring.add(nonRecurring);
    }
}
