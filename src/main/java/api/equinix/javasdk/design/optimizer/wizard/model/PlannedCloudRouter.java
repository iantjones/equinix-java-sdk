package api.equinix.javasdk.design.optimizer.wizard.model;

import api.equinix.javasdk.core.model.MetroId;
import api.equinix.javasdk.fabric.enums.GatewayPackageCode;
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

    /**
     * The Cloud Router package tier. Resolved and validated once at plan time by
     * {@code DeploymentWizard.Builder#routerPackage}, so {@link DeploymentPlan#execute()}
     * never has to parse a free-form string mid-deployment.
     */
    GatewayPackageCode packageCode;

    Long accountNumber;

    String projectId;

    String notificationEmail;
}
