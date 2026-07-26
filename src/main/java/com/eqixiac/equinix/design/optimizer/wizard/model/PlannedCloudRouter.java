package com.eqixiac.equinix.design.optimizer.wizard.model;

import com.eqixiac.equinix.core.model.MetroId;
import com.eqixiac.equinix.fabric.enums.GatewayPackageCode;
import lombok.Builder;
import lombok.Value;

import java.util.List;

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

    /**
     * Every notification recipient configured on the wizard ({@code deployment.notifications}) —
     * Fabric mandates at least one on a Cloud Router ({@code EQ-3040013}), and ALL of them are sent
     * on the wire body ({@code RouterBodies}), not just the first. {@code null}/empty means none was
     * configured, which Layer-1 validation flags as an error.
     */
    List<String> notificationEmails;
}
