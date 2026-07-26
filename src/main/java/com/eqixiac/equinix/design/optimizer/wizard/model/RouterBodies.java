/*
 * Copyright 2021 Ian Jones. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.eqixiac.equinix.design.optimizer.wizard.model;

import com.eqixiac.equinix.fabric.client.CloudRouters;
import com.eqixiac.equinix.fabric.enums.NotificationType;
import com.eqixiac.equinix.fabric.model.json.creators.CloudRouterOperator;

import java.util.ArrayList;
import java.util.List;

/**
 * Assembles a <em>complete</em> Fabric Cloud Router body from a {@link PlannedCloudRouter} — name,
 * metro, package, the optional account/project references, and the mandatory notification
 * recipients — mirroring {@link ConnectionBodies} for connections.
 *
 * <p>This is the <strong>single home</strong> of the router-create body shape, so the plan-time
 * Layer&nbsp;2 dry-run ({@code PlanValidator.routerDryRun}, {@code POST /routers?dryRun=true}) and
 * the execution-time Phase&nbsp;1 real create ({@code DeploymentPlan.execute()}) send the exact same
 * wire body — the only difference the caller adds is {@code .dryRun()}. Before this helper existed
 * the two call sites drifted: the dry-run stamped {@code notifications} (which Fabric MANDATES —
 * a router POSTed without one is rejected with HTTP&nbsp;400 {@code EQ-3040013}) and guarded null
 * account/project references, while the real create did neither — so a green dry-run was followed by
 * every real create failing. Both paths now converge here.</p>
 *
 * <p>The returned builder is left un-terminated: chain {@code .dryRun().create()} to validate, or
 * {@code .create()} to provision.</p>
 */
public final class RouterBodies {

    private RouterBodies() {}

    /**
     * Assembles the Cloud Router create body for the given planned router. Null account number and
     * project id are omitted (never sent as null-valued references), and every non-blank notification
     * email is stamped as one {@code ALL}-type notification — the field Fabric mandates
     * ({@code EQ-3040013}).
     *
     * @param routers the Fabric Cloud Routers client
     * @param planned the planned Cloud Router
     * @return an un-terminated builder; chain {@code .dryRun().create()} or {@code .create()}
     */
    public static CloudRouterOperator.CloudRouterBuilder routerBody(
            CloudRouters routers, PlannedCloudRouter planned) {
        CloudRouterOperator.CloudRouterBuilder builder = routers.define()
                .name(planned.getName())
                .inMetro(planned.getMetroId().code())
                .withPackage(planned.getPackageCode());
        if (planned.getAccountNumber() != null) {
            builder.accountNumber(planned.getAccountNumber());
        }
        if (planned.getProjectId() != null) {
            builder.projectId(planned.getProjectId());
        }
        List<String> emails = usableNotificationEmails(planned);
        if (!emails.isEmpty()) {
            builder.notification(NotificationType.ALL, emails);
        }
        return builder;
    }

    /**
     * The planned router's non-blank notification recipients, in declared order. Empty when the plan
     * carries none — the caller (validator or executor) is responsible for having flagged that
     * upstream, because Fabric rejects a notification-less router ({@code EQ-3040013}).
     *
     * @param planned the planned Cloud Router
     * @return the usable notification emails, never {@code null}
     */
    public static List<String> usableNotificationEmails(PlannedCloudRouter planned) {
        List<String> usable = new ArrayList<>();
        if (planned.getNotificationEmails() != null) {
            for (String email : planned.getNotificationEmails()) {
                if (email != null && !email.isBlank()) {
                    usable.add(email);
                }
            }
        }
        return usable;
    }
}
