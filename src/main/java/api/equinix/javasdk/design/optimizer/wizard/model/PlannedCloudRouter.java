package api.equinix.javasdk.design.optimizer.wizard.model;

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

    MetroCode metroCode;

    String name;

    String packageCode;

    Long accountNumber;

    String projectId;

    String notificationEmail;
}
