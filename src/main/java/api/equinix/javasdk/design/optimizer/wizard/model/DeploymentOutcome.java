package api.equinix.javasdk.design.optimizer.wizard.model;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.stream.Collectors;

/**
 * The result of executing a deployment plan. Contains all provisioned resources,
 * any errors encountered, and execution timing.
 */
@Value
@Builder
public class DeploymentOutcome {

    /** The deployment plan that was executed. */
    DeploymentPlan plan;

    /** All resources that were successfully provisioned. */
    List<ProvisionedResource> resources;

    /** Whether all resources were provisioned without errors. */
    boolean fullySuccessful;

    /** Errors encountered during provisioning. */
    List<ProvisioningError> errors;

    /** Total execution time in milliseconds. */
    long executionTimeMs;

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
                        .append(" | ").append(res.getMetroCode() != null ? res.getMetroCode() : "-")
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

        return md.toString();
    }
}
