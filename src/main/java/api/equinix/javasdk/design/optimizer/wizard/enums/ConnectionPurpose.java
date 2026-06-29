package api.equinix.javasdk.design.optimizer.wizard.enums;

/**
 * Classifies the purpose of a planned connection within a deployment plan.
 *
 * <ul>
 *   <li>{@link #PROVIDER} — Connection from a Cloud Router to a cloud/service provider</li>
 *   <li>{@link #BACKBONE} — Inter-metro connection between two Cloud Routers</li>
 *   <li>{@link #DEVICE} — Connection from a Cloud Router to an on-premises or network edge device</li>
 * </ul>
 */
public enum ConnectionPurpose {

    PROVIDER,

    BACKBONE,

    DEVICE
}
