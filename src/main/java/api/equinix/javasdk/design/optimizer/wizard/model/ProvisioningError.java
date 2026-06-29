package api.equinix.javasdk.design.optimizer.wizard.model;

import lombok.Builder;
import lombok.Value;

/**
 * An error that occurred while provisioning a planned resource during deployment execution.
 */
@Value
@Builder
public class ProvisioningError {

    String resourceType;

    String resourceName;

    String reason;

    boolean recoverable;
}
