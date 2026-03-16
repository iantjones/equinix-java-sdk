package api.equinix.javasdk.fabric.optimizer.wizard.model;

import api.equinix.javasdk.core.enums.MetroCode;
import lombok.Builder;
import lombok.Value;

/**
 * A Cloud Router (FCR) to be created as part of a deployment plan.
 * One Cloud Router is created per recommended metro.
 */
@Value
@Builder
public class PlannedCloudRouter {

    /** The metro where this Cloud Router will be deployed. */
    MetroCode metroCode;

    /** The display name for this Cloud Router (e.g., "FCR-DC"). */
    String name;

    /** The Cloud Router package code (e.g., "STANDARD"). */
    String packageCode;

    /** Optional account number for billing. */
    Long accountNumber;

    /** Optional project UUID for resource grouping. */
    String projectId;

    /** Notification email for provisioning updates. */
    String notificationEmail;
}
