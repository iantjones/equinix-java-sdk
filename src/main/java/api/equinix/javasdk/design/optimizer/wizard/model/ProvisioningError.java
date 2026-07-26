package api.equinix.javasdk.design.optimizer.wizard.model;

import lombok.Builder;
import lombok.Value;

/**
 * An error recorded while executing a deployment plan — a resource that could not be
 * provisioned, a pre-flight or waiter observation, or a rollback deletion that failed.
 * Collected on {@link DeploymentOutcome} rather than thrown, so a partial deployment can be
 * inspected and rolled back.
 */
@Value
@Builder
public class ProvisioningError {

    /**
     * The kind of resource the error concerns: {@code "CloudRouter"}, {@code "Connection"},
     * {@code "BackboneLink"}, or {@code "RoutingProtocol"}.
     */
    String resourceType;

    /** The planned resource's name (the same name that appears on the plan). */
    String resourceName;

    /** A human-readable description of what failed, including any API error message. */
    String reason;

    /**
     * Whether the failure is operational rather than a defect in the plan or its inputs.
     * {@code true} for transient/API-side failures — a create that errored, a resource that
     * timed out or landed in a terminal state, or a pre-flight dry-run that could not run —
     * where retrying the deployment (or operator intervention) may succeed with the same plan.
     * {@code false} when re-running cannot succeed without fixing the named cause first: a
     * missing customer authorization key, a missing/unknown package code on a hand-built plan,
     * a dependent resource skipped because its prerequisite was never provisioned, or a
     * rollback deletion that failed (manual cleanup required).
     */
    boolean recoverable;
}
