package com.eqixiac.equinix.design.optimizer.wizard.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.eqixiac.equinix.core.model.MetroId;
import com.eqixiac.equinix.fabric.enums.ConnectionType;
import com.eqixiac.equinix.fabric.enums.PeeringType;
import com.eqixiac.equinix.fabric.enums.RedundancyPriority;
import com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType;
import com.eqixiac.equinix.design.optimizer.wizard.enums.ConnectionPurpose;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;
import lombok.Value;

import java.util.List;

/**
 * A Fabric connection to be created as part of a deployment plan. Represents
 * either a provider connection (Cloud Router to service profile) or a backbone
 * link (Cloud Router to Cloud Router).
 *
 * <p>The {@code aSideRouterName}/{@code zSideRouterName} are NAME references resolved to real Cloud
 * Router uuids at execution time. The {@code zSide*} provider fields carry everything a real cloud VC
 * needs <em>except</em> the customer's cloud authorization key ({@code zSideAuthenticationKey}), the
 * VLAN tag ({@code zSideVlanTag}) and (for Azure) the peering type ({@code zSidePeeringType}) - the
 * inputs a brand-new customer must gather, which are null at plan time and enumerated separately as
 * {@link ConnectionInputRequirement}s rather than fabricated. The optional
 * {@code aSideExistingRouterUuid}/{@code aSidePortUuid} name a caller-supplied pre-existing endpoint
 * (lens 3b), which lets the connection be dry-run at PLAN time because the endpoint is already real.</p>
 */
@Value
@Builder(toBuilder = true)
@Jacksonized
public class PlannedConnection {

    /**
     * The resource's stable logical key within its plan (for example {@code connection/DC/aws}). Derived from the
     * design (metro codes, provider and role), never from the generated name or the name prefix, so
     * the same design always yields the same keys. Drift detection, reconcile and checkpoints
     * identify the resource by this key.
     */
    String key;

    String name;

    ConnectionType connectionType;

    ConnectionPurpose purpose;

    int bandwidthMbps;

    BandwidthAllocation bandwidthAllocation;

    /**
     * How the wizard selected this connection's Z-side service profile and billable bandwidth - the raw
     * requirement, the tier it was stamped at (possibly rounded up), and every covering candidate. Set
     * for a provider connection built from bandwidth-aware capability data; {@code null} for a backbone
     * link or a legacy availability entry that carried no candidate profiles. Exposes the choice (and
     * any round-up) so an interactive layer can confirm rather than the wizard picking silently.
     */
    ProfileSelection profileSelection;

    @JsonProperty("aSideMetro")
    MetroId aSideMetro;

    @JsonProperty("aSideRouterName")
    String aSideRouterName;

    @JsonProperty("zSideServiceProfileUuid")
    String zSideServiceProfileUuid;

    @JsonProperty("zSideProviderLabel")
    String zSideProviderLabel;

    @JsonProperty("zSideSellerRegion")
    String zSideSellerRegion;

    @JsonProperty("zSideMetro")
    MetroId zSideMetro;

    @JsonProperty("zSideRouterName")
    String zSideRouterName;

    /**
     * Every notification recipient configured on the wizard ({@code deployment.notifications}).
     * All of them are stamped onto the connection wire body ({@code ConnectionBodies}) and rendered
     * into exported HCL - never just the first address.
     */
    List<String> notificationEmails;

    /**
     * The resolved cloud provider type for a provider connection's Z-side (for example
     * {@link CloudProviderType#AWS}), or {@link CloudProviderType#OTHER} for a third-party profile.
     * Drives the required-authorization enumeration and the Z-side adapter selection.
     */
    @JsonProperty("zSideCloudType")
    CloudProviderType zSideCloudType;

    /**
     * The cloud-specific authorization key for the Z-side (AWS Account ID, Azure ExpressRoute service
     * key, GCP pairing key, ...). A customer input - {@code null} at plan time, supplied before the
     * connection is provisioned. Never fabricated by the wizard. Never serialized: a stored plan document
     * (and any JSON rendering of the plan) omits it, so a secret is never written to disk; supply it
     * through {@code ExecutionInputs} when executing a loaded plan.
     */
    @JsonIgnore
    String zSideAuthenticationKey;

    /** The Z-side peering type ({@code PRIVATE}/{@code MICROSOFT}); relevant for Azure ExpressRoute. */
    @JsonProperty("zSidePeeringType")
    PeeringType zSidePeeringType;

    /** The DOT1Q VLAN tag for the Z-side. A customer input; {@code null} at plan time. */
    @JsonProperty("zSideVlanTag")
    Integer zSideVlanTag;

    /**
     * The Fabric connection-level redundancy group this connection belongs to, or {@code null} for a
     * standalone connection. A primary VC and its diverse secondary VC to the same cloud share one
     * group; when set, {@link ConnectionBodies} stamps it (with {@link #redundancyPriority}) onto the
     * connection body via {@code ConnectionOperator.ConnectionBuilder.redundancy(group, priority)} so a
     * redundant pair is expressed rather than silently dropped. {@code null} means no group is sent.
     */
    @JsonProperty("zSideRedundancyGroup")
    String zSideRedundancyGroup;

    /**
     * This connection's role within its {@link #zSideRedundancyGroup} - 
     * {@link RedundancyPriority#PRIMARY} or {@link RedundancyPriority#SECONDARY}. Ignored (and no
     * redundancy is stamped) when {@link #zSideRedundancyGroup} is {@code null}.
     */
    RedundancyPriority redundancyPriority;

    /**
     * An existing Fabric Cloud Router uuid supplied by the caller to serve as the A-side (lens 3b).
     * When set, the connection has a real A-side endpoint and can be dry-run at PLAN time.
     */
    @JsonProperty("aSideExistingRouterUuid")
    String aSideExistingRouterUuid;

    /**
     * An existing customer port uuid supplied by the caller to serve as the A-side (lens 3b).
     * When set, the connection has a real A-side endpoint and can be dry-run at PLAN time.
     */
    @JsonProperty("aSidePortUuid")
    String aSidePortUuid;

    /**
     * The Equinix project the connection is created in ({@code DeploymentWizard.Builder.projectId}),
     * or {@code null} for the caller's default project. Sent on the create body so a plan's
     * connections can be found again in the same project as its Cloud Routers.
     */
    String projectId;

    /** Optional order term length in months (for example 12, 24 or 36). */
    Integer termLength;

    @JsonIgnore

    public boolean isProviderConnection() {
        return purpose == ConnectionPurpose.PROVIDER;
    }

    @JsonIgnore

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
