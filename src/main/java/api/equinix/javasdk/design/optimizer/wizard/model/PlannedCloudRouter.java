package api.equinix.javasdk.design.optimizer.wizard.model;

import api.equinix.javasdk.core.model.MetroId;
import lombok.Builder;
import lombok.Value;

/**
 * A Cloud Router (FCR) to be created as part of a deployment plan.
 * One Cloud Router is created per recommended metro.
 */
@Value
@Builder
public class PlannedCloudRouter {

    MetroId metroId;

    String name;

    String packageCode;

    Long accountNumber;

    String projectId;

    String notificationEmail;
}
