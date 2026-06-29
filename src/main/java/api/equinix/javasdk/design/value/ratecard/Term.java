package api.equinix.javasdk.design.value.ratecard;

/**
 * Commitment term for a priced resource. Equinix and cloud-provider pricing
 * both vary by term length — longer commitments typically reduce the monthly
 * recurring charge — so the term is an input to every rate-card lookup and a
 * driver of savings calculations.
 */
public enum Term {

    /** Month-to-month, no commitment. */
    MONTH_1(1),

    /** 12-month commitment. */
    MONTH_12(12),

    /** 24-month commitment. */
    MONTH_24(24),

    /** 36-month commitment. */
    MONTH_36(36);

    private final int months;

    Term(int months) {
        this.months = months;
    }

    /** Returns the term length in months. */
    public int months() {
        return months;
    }
}
