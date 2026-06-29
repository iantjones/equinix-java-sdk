package api.equinix.javasdk.design.value.ratecard;

/**
 * An Equinix colocation cost primitive that a {@link RateCard} can price — alongside the
 * interconnection rates (connection / cloud router / egress). These let a caller feed the cost
 * models their <em>real</em> colocation economics (e.g. a negotiated cabinet or cross-connect
 * rate), so a colocation-vs-cloud comparison reflects what Equinix actually sells rather than only
 * the interconnect.
 *
 * <p>Each item's {@link PriceQuote} is expressed per the unit named below, per month; the cost
 * model multiplies it by the relevant quantity (cabinets, kW, cross-connects).</p>
 */
public enum ColocationItem {

    /** A cabinet / rack — priced per cabinet, per month. */
    CABINET,

    /** Power — priced per kW, per month. */
    POWER_PER_KW,

    /** A cross-connect — priced per cross-connect, per month. */
    CROSS_CONNECT
}
