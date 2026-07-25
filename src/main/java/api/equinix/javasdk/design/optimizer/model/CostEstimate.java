package api.equinix.javasdk.design.optimizer.model;

import api.equinix.javasdk.design.value.ratecard.PriceSource;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Aggregated cost estimate across all recommended metros.
 */
@Value
@Builder
public class CostEstimate {

    /**
     * The aggregate monthly cost across all recommended metros, in {@link #currency}. This is
     * {@code null} when the selected metros are priced in more than one currency: summing across
     * currencies without an FX rate would be a fabricated figure, so no single total is produced and
     * the per-currency subtotals are exposed via {@link #monthlyByCurrency} instead (see also
     * {@link #costDisclaimer}).
     */
    BigDecimal monthlyTotal;
    BigDecimal setupTotal;

    /**
     * The single currency of {@link #monthlyTotal}/{@link #setupTotal}, or {@code null} when the
     * metros span multiple currencies (see {@link #monthlyByCurrency}).
     */
    String currency;

    /**
     * Monthly subtotal per currency code, always populated. When the estimate is single-currency it
     * holds one entry equal to {@link #monthlyTotal}; when the metros span currencies it is the only
     * honest breakdown of the monthly cost, since {@link #monthlyTotal} is then {@code null}.
     */
    Map<String, BigDecimal> monthlyByCurrency;

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
