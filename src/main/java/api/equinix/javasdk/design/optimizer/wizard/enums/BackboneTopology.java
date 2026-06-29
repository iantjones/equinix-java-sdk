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

    FULL_MESH,

    HUB_SPOKE,

    RING
}
