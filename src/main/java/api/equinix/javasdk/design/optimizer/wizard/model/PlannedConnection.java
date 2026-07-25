package api.equinix.javasdk.design.optimizer.wizard.model;

import api.equinix.javasdk.core.model.MetroId;
import api.equinix.javasdk.fabric.enums.ConnectionType;
import api.equinix.javasdk.fabric.enums.PeeringType;
import api.equinix.javasdk.fabric.model.implementation.cloud.CloudProviderType;
import api.equinix.javasdk.design.optimizer.wizard.enums.ConnectionPurpose;
import lombok.Builder;
import lombok.Value;

/**
 * A Fabric connection to be created as part of a deployment plan. Represents
 * either a provider connection (Cloud Router to service profile) or a backbone
 * link (Cloud Router to Cloud Router).
 *
 * <p>The {@code aSideRouterName}/{@code zSideRouterName} are NAME references resolved to real Cloud
 * Router uuids at execution time. The {@code zSide*} provider fields carry everything a real cloud VC
 * needs <em>except</em> the customer's cloud authorization key ({@code zSideAuthenticationKey}), the
 * VLAN tag ({@code zSideVlanTag}) and (for Azure) the peering type ({@code zSidePeeringType}) — the
 * inputs a brand-new customer must gather, which are null at plan time and enumerated separately as
 * {@link ConnectionInputRequirement}s rather than fabricated. The optional
 * {@code aSideExistingRouterUuid}/{@code aSidePortUuid} name a caller-supplied pre-existing endpoint
 * (lens 3b), which lets the connection be dry-run at PLAN time because the endpoint is already real.</p>
 */
@Value
@Builder(toBuilder = true)
public class PlannedConnection {

    String name;

    ConnectionType connectionType;

    ConnectionPurpose purpose;

    int bandwidthMbps;

    BandwidthAllocation bandwidthAllocation;

    MetroId aSideMetro;

    String aSideRouterName;

    String zSideServiceProfileUuid;

    String zSideProviderLabel;

    String zSideSellerRegion;

    MetroId zSideMetro;

    String zSideRouterName;

    String notificationEmail;

    /**
     * The resolved cloud provider type for a provider connection's Z-side (for example
     * {@link CloudProviderType#AWS}), or {@link CloudProviderType#OTHER} for a third-party profile.
     * Drives the required-authorization enumeration and the Z-side adapter selection.
     */
    CloudProviderType zSideCloudType;

    /**
     * The cloud-specific authorization key for the Z-side (AWS Account ID, Azure ExpressRoute service
     * key, GCP pairing key, ...). A customer input — {@code null} at plan time, supplied before the
     * connection is provisioned. Never fabricated by the wizard.
     */
    String zSideAuthenticationKey;

    /** The Z-side peering type ({@code PRIVATE}/{@code MICROSOFT}); relevant for Azure ExpressRoute. */
    PeeringType zSidePeeringType;

    /** The DOT1Q VLAN tag for the Z-side. A customer input; {@code null} at plan time. */
    Integer zSideVlanTag;

    /**
     * An existing Fabric Cloud Router uuid supplied by the caller to serve as the A-side (lens 3b).
     * When set, the connection has a real A-side endpoint and can be dry-run at PLAN time.
     */
    String aSideExistingRouterUuid;

    /**
     * An existing customer port uuid supplied by the caller to serve as the A-side (lens 3b).
     * When set, the connection has a real A-side endpoint and can be dry-run at PLAN time.
     */
    String aSidePortUuid;

    /** Optional order term length in months (for example 12, 24 or 36). */
    Integer termLength;

    public boolean isProviderConnection() {
        return purpose == ConnectionPurpose.PROVIDER;
    }

    public boolean isBackboneLink() {
        return purpose == ConnectionPurpose.BACKBONE;
    }

    /**
     * Whether the caller supplied a real, pre-existing A-side endpoint (an existing Cloud Router or
     * port). When {@code true} the connection's live endpoint dry-run can run at PLAN time (lens 3b);
     * when {@code false} the A-side Cloud Router does not exist yet, so the live dry-run is deferred
     * to provisioning (lens 3a).
     *
     * @return {@code true} if a pre-existing A-side endpoint uuid is set
     */
    public boolean hasPreExistingEndpoint() {
        return aSideExistingRouterUuid != null || aSidePortUuid != null;
    }
}
