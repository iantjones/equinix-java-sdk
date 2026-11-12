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


package com.eqixiac.equinix.design.plan;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * How resources created from a plan are marked so drift detection and reconcile can find them again.
 *
 * <p><b>What Fabric persists</b> (read-only calls and {@code dryRun=true} creates, 2026-11-12):</p>
 * <ul>
 *   <li><b>Connections</b> persist a top-level {@code additionalInfo} list of key/value pairs: live
 *       connections in the test project return it on {@code GET} / search, and a {@code dryRun=true}
 *       IP_VC create carrying the three keys below was accepted (HTTP 200) and echoed them back. The
 *       spec lists {@code additionalInfo} on {@code ConnectionPostRequest} and on {@code Connection}.
 *       A connection therefore carries {@value #DEPLOYMENT_KEY} (the plan's deployment name),
 *       {@value #LOGICAL_KEY} (the step's logical key) and {@value #PLAN_KEY} (the content hash of the
 *       plan that created it).</li>
 *   <li><b>Cloud Routers</b> have no description, tag or additional-info field: the live router read
 *       returns only {@code package, type, name, location, order, project, account, notifications},
 *       ids and state, and {@code CloudRouterPostRequest} has nothing else. A router is found by its
 *       exact planned name inside the plan's project; a router whose name starts with the plan's name
 *       prefix but is not in the plan is reported as unmanaged.</li>
 *   <li><b>Routing protocols</b> are found under their (already matched) parent connection, by name and
 *       then by type.</li>
 * </ul>
 * <p>Fabric names are limited to 24 characters, too short to embed a plan id; the name prefix
 * ({@code DeploymentWizard.Builder.routerNamePrefix}) is the human-visible marker, so give each
 * deployment that shares a project a distinct prefix.</p>
 */
public final class PlanMarker {

    /** additionalInfo key carrying the plan's deployment name (stable across edits of the plan). */
    public static final String DEPLOYMENT_KEY = "eqxSdkDeployment";

    /** additionalInfo key carrying the step's logical key. */
    public static final String LOGICAL_KEY = "eqxSdkKey";

    /** additionalInfo key carrying the content hash (plan id) of the plan that created the resource. */
    public static final String PLAN_KEY = "eqxSdkPlan";

    private PlanMarker() {
    }

    /**
     * The additionalInfo entries stamped on a connection created from a plan.
     *
     * @param deploymentName the plan's deployment name
     * @param logicalKey     the connection's logical key
     * @param planId         the plan id
     * @return the entries, in a stable order; entries with a {@code null} value are omitted
     */
    public static Map<String, String> connectionMarker(String deploymentName, String logicalKey, String planId) {
        Map<String, String> marker = new LinkedHashMap<>();
        if (deploymentName != null) {
            marker.put(DEPLOYMENT_KEY, deploymentName);
        }
        if (logicalKey != null) {
            marker.put(LOGICAL_KEY, logicalKey);
        }
        if (planId != null) {
            marker.put(PLAN_KEY, planId);
        }
        return marker;
    }

    /**
     * Whether a live connection's additionalInfo marks it as belonging to the given deployment.
     *
     * @param additionalInfo the live entries (may be {@code null})
     * @param deploymentName the plan's deployment name
     * @return {@code true} when {@value #DEPLOYMENT_KEY} equals {@code deploymentName}
     */
    public static boolean belongsTo(Map<String, String> additionalInfo, String deploymentName) {
        return additionalInfo != null && deploymentName != null
                && deploymentName.equals(additionalInfo.get(DEPLOYMENT_KEY));
    }

    /**
     * Whether a router name carries the plan's name prefix ({@code "<prefix>-"}).
     *
     * @param name   the live router name
     * @param prefix the plan's name prefix
     * @return {@code true} when the name starts with the prefix and a dash
     */
    public static boolean hasPrefix(String name, String prefix) {
        return name != null && prefix != null && !prefix.isBlank() && name.startsWith(prefix + "-");
    }
}
