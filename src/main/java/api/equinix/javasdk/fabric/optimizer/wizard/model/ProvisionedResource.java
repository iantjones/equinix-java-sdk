package api.equinix.javasdk.fabric.optimizer.wizard.model;

import api.equinix.javasdk.core.enums.MetroCode;
import lombok.Builder;
import lombok.Value;

/**
 * A single resource that was successfully created during deployment execution.
 */
@Value
@Builder
public class ProvisionedResource {

    /** The type of resource (e.g., "CloudRouter", "Connection", "RoutingProtocol"). */
    String resourceType;

    /** The display name of the resource. */
    String name;

    /** The UUID assigned by the Fabric API after creation. */
    String uuid;

    /** The metro where this resource was provisioned (if applicable). */
    MetroCode metroCode;

    /** The current provisioning status. */
    String status;
}
