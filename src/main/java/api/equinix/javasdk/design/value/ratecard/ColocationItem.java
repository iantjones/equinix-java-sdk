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

    CABINET,

    POWER_PER_KW,

    CROSS_CONNECT
}
