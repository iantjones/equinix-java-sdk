package api.equinix.javasdk.design.optimizer.model;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

/**
 * Monthly budget range for cost-constrained optimization.
 */
@Value
public class BudgetRange {

    BigDecimal minMonthly;
    BigDecimal maxMonthly;
    String currency;

    public BudgetRange(double minMonthly, double maxMonthly) {
        this(BigDecimal.valueOf(minMonthly), BigDecimal.valueOf(maxMonthly), "USD");
    }

    /**
     * Canonical constructor. Prefer the generated {@code builder()} — the two
     * adjacent {@code BigDecimal} parameters make positional calls swap-prone.
     *
     * @param minMonthly the minimum monthly budget
     * @param maxMonthly the maximum monthly budget
     * @param currency   the ISO currency code (e.g. {@code "USD"})
     */
    @Builder
    public BudgetRange(BigDecimal minMonthly, BigDecimal maxMonthly, String currency) {
        this.minMonthly = minMonthly;
        this.maxMonthly = maxMonthly;
        this.currency = currency;
    }
}
