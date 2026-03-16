package api.equinix.javasdk.fabric.optimizer.model;

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

    public BudgetRange(BigDecimal minMonthly, BigDecimal maxMonthly, String currency) {
        this.minMonthly = minMonthly;
        this.maxMonthly = maxMonthly;
        this.currency = currency;
    }
}
