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

    MetroCode metroA;

    MetroCode metroZ;

    String name;

    int bandwidthMbps;

    BackboneTopology topology;

    PlannedConnection connection;
}
