package api.equinix.javasdk.design.value.ratecard;

/**
 * Commitment term for a priced resource. Equinix and cloud-provider pricing
 * both vary by term length — longer commitments typically reduce the monthly
 * recurring charge — so the term is an input to every rate-card lookup and a
 * driver of savings calculations.
 */
public enum Term {

    /** Month-to-month — no commitment; typically the highest monthly rate. */
    MONTH_1(1),

    /** 12-month commitment — the default term across the value-realization builders. */
    MONTH_12(12),

    /** 24-month commitment. */
    MONTH_24(24),

    /** 36-month commitment — typically the lowest monthly rate. */
    MONTH_36(36);

    private final int months;

    Term(int months) {
        this.months = months;
    }

    /**
     * The commitment length in months. This is the multiplier
     * {@link PriceQuote#totalOverTerm(Term)} applies to the monthly-recurring
     * charge ({@code MRC × months() + NRC}), and the value term-aware rate
     * cards compare against a price row's own term length.
     *
     * @return the number of months in this commitment term
     */
    public int months() {
        return months;
    }
}
