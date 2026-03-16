package api.equinix.javasdk.fabric.optimizer.wizard.model;

import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.fabric.enums.ConnectionType;
import api.equinix.javasdk.fabric.optimizer.wizard.enums.ConnectionPurpose;
import lombok.Builder;
import lombok.Value;

/**
 * A Fabric connection to be created as part of a deployment plan. Represents
 * either a provider connection (Cloud Router to service profile) or a backbone
 * link (Cloud Router to Cloud Router).
 */
@Value
@Builder
public class PlannedConnection {

    /** Display name for this connection. */
    String name;

    /** The Fabric connection type (e.g., EVPL_VC). */
    ConnectionType connectionType;

    /** The purpose of this connection within the deployment. */
    ConnectionPurpose purpose;

    /** Allocated bandwidth in Mbps. */
    int bandwidthMbps;

    /** Bandwidth sizing breakdown. */
    BandwidthAllocation bandwidthAllocation;

    /** Metro where the A-side Cloud Router resides. */
    MetroCode aSideMetro;

    /** Name of the A-side Cloud Router. */
    String aSideRouterName;

    /** For provider connections: the Z-side service profile UUID. */
    String zSideServiceProfileUuid;

    /** For provider connections: the provider display label. */
    String zSideProviderLabel;

    /** For provider connections: the seller region. */
    String zSideSellerRegion;

    /** For backbone connections: the Z-side metro. */
    MetroCode zSideMetro;

    /** For backbone connections: the Z-side Cloud Router name. */
    String zSideRouterName;

    /** Notification email for provisioning updates. */
    String notificationEmail;

    /** Whether this is a provider or backbone connection. */
    public boolean isProviderConnection() {
        return purpose == ConnectionPurpose.PROVIDER;
    }

    /** Whether this is an inter-metro backbone link. */
    public boolean isBackboneLink() {
        return purpose == ConnectionPurpose.BACKBONE;
    }
}
