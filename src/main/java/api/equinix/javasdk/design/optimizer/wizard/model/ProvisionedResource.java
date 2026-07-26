package api.equinix.javasdk.design.optimizer.wizard.model;

import api.equinix.javasdk.core.model.MetroId;
import lombok.Builder;
import lombok.Value;

/**
 * A single resource that was created during deployment execution — the audit-trail entry a
 * {@link DeploymentOutcome} carries for every create that succeeded, including resources that
 * were subsequently torn down by an abort-and-rollback.
 */
@Value
@Builder
public class ProvisionedResource {

    /**
     * The kind of resource created: {@code "CloudRouter"}, {@code "Connection"},
     * {@code "BackboneLink"}, or {@code "RoutingProtocol"}.
     */
    String resourceType;

    /** The planned resource's name (the same name that appears on the plan). */
    String name;

    /** The real uuid Fabric assigned at creation — the handle rollback deletes by. */
    String uuid;

    /** The metro the resource lives in, or {@code null} where not applicable (routing protocols). */
    MetroId metroId;

    /**
     * The last state execution observed for this resource. One of: the ready state (normally
     * {@code "PROVISIONED"}); a real terminal-failure state the waiter caught ({@code "FAILED"},
     * {@code "CANCELLED"}, {@code "DEPROVISIONED"}, {@code "DEPROVISIONING"}); or the fallback
     * {@code "PROVISIONING"} when no state could be observed at all — the wait timed out or the
     * status fetch itself failed, so the resource may or may not have progressed since.
     */
    String status;
}
