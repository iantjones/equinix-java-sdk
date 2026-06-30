package api.equinix.javasdk.design.optimizer.wizard.model;

import api.equinix.javasdk.core.model.MetroId;
import lombok.Builder;
import lombok.Value;

/**
 * A single resource that was successfully created during deployment execution.
 */
@Value
@Builder
public class ProvisionedResource {

    String resourceType;

    String name;

    String uuid;

    MetroId metroId;

    String status;
}
