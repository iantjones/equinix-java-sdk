package api.equinix.javasdk.design.optimizer.wizard.model;

import api.equinix.javasdk.core.model.MetroId;
import api.equinix.javasdk.design.optimizer.wizard.enums.BackboneTopology;
import lombok.Builder;
import lombok.Value;

/**
 * An inter-metro Fabric backbone link between two Cloud Routers, enabling private connectivity
 * between deployment metros. Which pairs of metros get a link is decided by the plan's
 * {@link BackboneTopology}; this type is the topology view of one such link, wrapping the
 * underlying {@link PlannedConnection} (purpose {@code BACKBONE}) that actually gets
 * provisioned.
 */
@Value
@Builder
public class PlannedBackboneLink {

    /** The A-side metro of the link. */
    MetroId metroA;

    /** The Z-side metro of the link. */
    MetroId metroZ;

    /** The generated link name — the same name as the underlying connection's. */
    String name;

    /** The link bandwidth in Mbps ({@code DeploymentWizard.Builder.backboneBandwidthMbps}). */
    int bandwidthMbps;

    /** The topology this link was generated under. */
    BackboneTopology topology;

    /**
     * The underlying Cloud Router &rarr; Cloud Router connection that is dry-run, created, and
     * exported for this link. Its A/Z-side router names resolve to real uuids at execution time.
     */
    PlannedConnection connection;
}
