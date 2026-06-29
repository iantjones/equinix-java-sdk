package api.equinix.javasdk.design.value.ratecard;

/**
 * Commitment term for a priced resource. Equinix and cloud-provider pricing
 * both vary by term length — longer commitments typically reduce the monthly
 * recurring charge — so the term is an input to every rate-card lookup and a
 * driver of savings calculations.
 */
public enum Term {

    MONTH_1(1),

    MONTH_12(12),

    MONTH_24(24),

    MONTH_36(36);

    private final int months;

    Term(int months) {
        this.months = months;
    }

    public int months() {
        return months;
    }
}
