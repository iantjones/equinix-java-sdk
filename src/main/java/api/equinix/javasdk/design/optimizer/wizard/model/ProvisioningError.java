package api.equinix.javasdk.design.optimizer.wizard.model;

import lombok.Builder;
import lombok.Value;

/**
 * An error that occurred while provisioning a planned resource during deployment execution.
 */
@Value
@Builder
public class ProvisioningError {

    /** The type of resource that failed (e.g., "CloudRouter", "Connection"). */
    String resourceType;

    /** The name of the resource that failed. */
    String resourceName;

    /** The error reason or exception message. */
    String reason;

    /** Whether the error is recoverable by retrying the operation. */
    boolean recoverable;
}
