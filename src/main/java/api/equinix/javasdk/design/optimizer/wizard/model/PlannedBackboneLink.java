package api.equinix.javasdk.design.optimizer.wizard.model;

import api.equinix.javasdk.core.model.MetroId;
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

    MetroId metroA;

    MetroId metroZ;

    String name;

    int bandwidthMbps;

    BackboneTopology topology;

    PlannedConnection connection;
}
