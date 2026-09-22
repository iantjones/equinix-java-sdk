package com.eqixiac.equinix.design.optimizer.wizard.model;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.stream.Collectors;

/**
 * The result of executing a deployment plan. Contains all provisioned resources,
 * any errors encountered, and execution timing.
 *
 * <p><strong>{@code resources} is an audit trail, not a live inventory.</strong> It records every
 * resource execution successfully <em>created</em> — including, after an abort-and-rollback (a
 * pre-flight dry-run rejection or a failed create mid-run), resources that were created and then
 * deleted again by the LIFO teardown. On such a run {@code toSummary()}'s
 * "N/M resources provisioned" therefore describes what was built before the teardown, not what
 * still exists: {@code isFullySuccessful() == false} plus errors mentioning the abort/rollback
 * distinguish a rolled-back run from a partially-standing one.</p>
 *
 * <p><b>Beta.</b> {@code informational} lists the plan's native multicloud links. Execution never
 * provisions them and sends no request for them. An informational entry is not an error: it does
 * not affect {@code isFullySuccessful()} and is not counted in {@code errors}.</p>
 */
@Value
@Builder
public class DeploymentOutcome {

    /** The plan this outcome resulted from executing. */
    DeploymentPlan plan;

    /**
     * Every resource execution created, in creation order — see the class note: after a
     * rollback these entries describe resources that have since been deleted.
     */
    List<ProvisionedResource> resources;

    /** Whether every planned resource was provisioned with no errors recorded. */
    boolean fullySuccessful;

    /**
     * Every error recorded during the run — creation failures, waiter observations, pre-flight
     * notes, and (after an abort) any rollback deletions that failed.
     */
    List<ProvisioningError> errors;

    /** Wall-clock duration of the execution run in milliseconds, including state waits. */
    long executionTimeMs;

    /**
     * <b>Beta.</b> One entry per native multicloud link on the plan, each with
     * {@code resourceType "MulticloudInterconnect"}, the link's name, {@code recoverable == false}
     * (re-running the plan cannot create it) and a reason stating that the link is created outside
     * Fabric with the create-then-accept procedure. {@code null} or empty when the plan has none.
     * Present on every outcome of such a plan, including a fail-fast or rolled-back run.
     */
    List<ProvisioningError> informational;

    /**
     * Generates a concise plain-text summary of the execution outcome.
     */
    public String toSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Deployment ").append(fullySuccessful ? "SUCCEEDED" : "COMPLETED WITH ERRORS").append(": ");
        sb.append(resources.size()).append("/").append(plan.totalResourceCount()).append(" resources provisioned");
        if (!errors.isEmpty()) {
            sb.append(", ").append(errors.size()).append(" error(s)");
        }
        sb.append(" in ").append(executionTimeMs).append("ms.");
        if (informational != null && !informational.isEmpty()) {
            sb.append(" ").append(informational.size())
                    .append(" native multicloud link(s) not provisioned: created outside Fabric by the customer.");
        }
        return sb.toString();
    }

    /**
     * Generates a full markdown-formatted execution report.
     */
    public String toMarkdown() {
        StringBuilder md = new StringBuilder();
        md.append("# Deployment Outcome\n\n");
        md.append("**Status:** ").append(fullySuccessful ? "SUCCESS" : "COMPLETED WITH ERRORS").append("\n");
        md.append("**Resources Provisioned:** ").append(resources.size())
                .append("/").append(plan.totalResourceCount()).append("\n");
        md.append("**Execution Time:** ").append(executionTimeMs).append("ms\n\n");

        // Provisioned Resources
        if (!resources.isEmpty()) {
            md.append("## Provisioned Resources\n\n");
            md.append("| Type | Name | UUID | Metro | Status |\n");
            md.append("|------|------|------|-------|--------|\n");
            for (ProvisionedResource res : resources) {
                md.append("| ").append(res.getResourceType())
                        .append(" | ").append(res.getName())
                        .append(" | ").append(res.getUuid())
                        .append(" | ").append(res.getMetroId() != null ? res.getMetroId() : "-")
                        .append(" | ").append(res.getStatus())
                        .append(" |\n");
            }
            md.append("\n");
        }

        // Errors
        if (!errors.isEmpty()) {
            md.append("## Errors\n\n");
            md.append("| Type | Name | Reason | Recoverable |\n");
            md.append("|------|------|--------|-------------|\n");
            for (ProvisioningError err : errors) {
                md.append("| ").append(err.getResourceType())
                        .append(" | ").append(err.getResourceName())
                        .append(" | ").append(err.getReason())
                        .append(" | ").append(err.isRecoverable() ? "Yes" : "No")
                        .append(" |\n");
            }
            md.append("\n");
        }

        // Native multicloud links: informational, never provisioned here, never an error.
        if (informational != null && !informational.isEmpty()) {
            md.append("## Not provisioned by this SDK\n\n");
            md.append("| Type | Name | Reason |\n");
            md.append("|------|------|--------|\n");
            for (ProvisioningError info : informational) {
                md.append("| ").append(info.getResourceType())
                        .append(" | ").append(info.getResourceName())
                        .append(" | ").append(info.getReason())
                        .append(" |\n");
            }
            md.append("\n");
        }

        return md.toString();
    }
}
