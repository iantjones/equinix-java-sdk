package api.equinix.javasdk.design.optimizer.model;

import api.equinix.javasdk.core.model.MetroId;
import api.equinix.javasdk.design.value.ratecard.PriceSource;
import lombok.Value;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Estimated cost breakdown for a single metro.
 */
@Value
public class MetroCostBreakdown {

    MetroId metroId;
    BigDecimal monthlyRecurring;
    BigDecimal nonRecurring;
    Map<String, BigDecimal> lineItems;

    /**
     * ISO 4217 code of the currency this metro's figures are quoted in (e.g. {@code "USD"},
     * {@code "EUR"}). Live Fabric pricing genuinely quotes different currencies per region, so
     * per-metro rows in one estimate can legitimately differ; renderers must print this code or
     * its symbol rather than assuming a dollar sign. May be {@code null} when the pricing source
     * did not state one.
     */
    String currency;

    /**
     * Provenance of this metro's figures: {@link PriceSource#EQUINIX_LIVE} (or
     * {@code CUSTOM}/{@code REFERENCE}) when priced from the rate card, or
     * {@link PriceSource#ESTIMATE} when the regional pricing heuristic was used.
     */
    PriceSource source;

    /**
     * The metro's first-month outlay: monthly recurring plus one-time setup, in this row's
     * {@code currency}.
     *
     * @return {@code monthlyRecurring + nonRecurring}
     */
    public BigDecimal total() {
        return monthlyRecurring.add(nonRecurring);
    }
}
