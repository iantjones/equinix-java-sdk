package api.equinix.javasdk.design.optimizer.wizard.model;

import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.design.optimizer.wizard.enums.BackboneTopology;
import lombok.Builder;
import lombok.Value;

/**
 * An inter-metro Fabric backbone link between two Cloud Routers, enabling
 * private connectivity between deployment metros.
 */
@Value
@Builder
public class PlannedBackboneLink {

    /** The metro of the first Cloud Router (A-side). */
    MetroCode metroA;

    /** The metro of the second Cloud Router (Z-side). */
    MetroCode metroZ;

    /** Display name for this backbone link. */
    String name;

    /** Bandwidth allocated for this backbone link in Mbps. */
    int bandwidthMbps;

    /** The topology strategy that generated this link. */
    BackboneTopology topology;

    /** The underlying planned connection for this backbone link. */
    PlannedConnection connection;
}
