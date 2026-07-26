package api.equinix.javasdk.design.value.ratecard;

/**
 * An Equinix colocation cost primitive that a {@link RateCard} can price — alongside the
 * interconnection rates (connection / cloud router / egress). These let a caller supply their own
 * colocation rates (e.g. a negotiated cabinet or cross-connect rate), so a colocation-vs-cloud
 * comparison covers the physical-infrastructure costs Equinix sells, not just the interconnect.
 *
 * <p>Each item's {@link PriceQuote} is expressed per the unit named below, per month; the cost
 * model multiplies it by the relevant quantity (cabinets, kW, cross-connects).</p>
 */
public enum ColocationItem {

    /**
     * A colocation cabinet, priced <em>per cabinet per month</em>. The TCO model multiplies
     * the per-cabinet quote by the configured cabinet count
     * ({@code TcoCalculator.Builder.cabinets(int)}, default 1).
     */
    CABINET,

    /**
     * Power (and the space it implies), priced <em>per kW of draw per month</em>. The TCO
     * model multiplies the per-kW quote by the configured draw
     * ({@code TcoCalculator.Builder.powerKw(double)}, default 5.0).
     */
    POWER_PER_KW,

    /**
     * A physical cross-connect, priced <em>per cross-connect per month</em>. The TCO model
     * multiplies the per-unit quote by the configured count
     * ({@code TcoCalculator.Builder.crossConnects(int)}, default 1).
     */
    CROSS_CONNECT
}
