package api.equinix.javasdk.design.optimizer.wizard.enums;

/**
 * Defines the inter-metro backbone link topology for connecting Cloud Routers
 * across multiple Equinix metros.
 *
 * <ul>
 *   <li>{@link #FULL_MESH} — Every metro connects to every other metro: N*(N-1)/2 links</li>
 *   <li>{@link #HUB_SPOKE} — Primary metro connects to all others: N-1 links</li>
 *   <li>{@link #RING} — Each metro connects to the next in sequence: N links</li>
 * </ul>
 */
public enum BackboneTopology {

    /** Every metro pair connected directly. Highest redundancy, highest cost. */
    FULL_MESH,

    /** Primary (top-ranked) metro acts as hub; all others connect to it. */
    HUB_SPOKE,

    /** Metros connected in a ring. Balanced cost and redundancy. */
    RING
}
