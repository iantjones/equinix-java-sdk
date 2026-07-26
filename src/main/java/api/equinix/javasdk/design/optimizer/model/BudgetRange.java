package api.equinix.javasdk.design.optimizer.model;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

/**
 * Monthly budget range for cost-constrained optimization. {@code maxMonthly} is the ceiling the
 * cost estimate is reported against (never an exclusion filter); {@code minMonthly} is currently
 * informational only — the engine does not read it. See
 * {@code OptimizationConstraints.budget} for the full reporting contract.
 */
@Value
public class BudgetRange {

    /** The minimum monthly spend — informational; not checked or reported against. */
    BigDecimal minMonthly;

    /** The maximum monthly spend the estimate is reported against. */
    BigDecimal maxMonthly;

    /** ISO 4217 currency code of both bounds (e.g. {@code "USD"}). */
    String currency;

    /**
     * Convenience constructor in USD.
     *
     * @param minMonthly the minimum monthly spend (informational)
     * @param maxMonthly the maximum monthly spend, reported against
     */
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
