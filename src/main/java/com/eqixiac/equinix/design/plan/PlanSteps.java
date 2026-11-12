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

import com.eqixiac.equinix.design.optimizer.wizard.model.DeploymentPlan;
import com.eqixiac.equinix.design.optimizer.wizard.model.PlannedBackboneLink;
import com.eqixiac.equinix.design.optimizer.wizard.model.PlannedCloudRouter;
import com.eqixiac.equinix.design.optimizer.wizard.model.PlannedConnection;
import com.eqixiac.equinix.design.optimizer.wizard.model.PlannedInterconnect;
import com.eqixiac.equinix.design.optimizer.wizard.model.PlannedMulticloudInterconnect;
import com.eqixiac.equinix.design.optimizer.wizard.model.PlannedRoutingProtocol;
import com.eqixiac.equinix.fabric.enums.RedundancyPriority;
import com.eqixiac.equinix.fabric.enums.RoutingProtocolType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Derives the dependency-ordered {@link PlanStep}s of a plan: Cloud Routers, then provider
 * connections (a redundancy group's PRIMARY before its SECONDARY), then backbone links, then routing
 * protocols (DIRECT before BGP on the same connection, as Fabric requires), then the elements the SDK
 * does not create (Interconnects and native multicloud links).
 *
 * <p>A planned resource without a logical key (a hand-built plan) gets the fallback key
 * {@code <kind>/<name>}, so every step has a key.</p>
 */
public final class PlanSteps {

    private PlanSteps() {
    }

    /**
     * The steps of a plan, in creation order.
     *
     * @param plan the plan
     * @return the steps; empty for an empty plan
     */
    public static List<PlanStep> of(DeploymentPlan plan) {
        List<PlanStep> steps = new ArrayList<>();
        Map<String, String> routerKeyByName = new HashMap<>();
        for (PlannedCloudRouter router : nz(plan.getCloudRouters())) {
            String key = keyOf(router);
            routerKeyByName.put(router.getName(), key);
            steps.add(new PlanStep(key, ResourceKind.CLOUD_ROUTER, router.getName(), List.of(), null));
        }

        Map<String, String> connectionKeyByName = new HashMap<>();
        Map<String, String> primaryKeyByGroup = new HashMap<>();
        List<PlannedConnection> ordered = new ArrayList<>(nz(plan.getProviderConnections()));
        // A SECONDARY must follow its group's PRIMARY: the secondary is created with the group id
        // Fabric assigned to the primary.
        ordered.sort((a, b) -> Boolean.compare(isSecondary(a), isSecondary(b)));
        for (PlannedConnection conn : ordered) {
            if (hasGroup(conn) && !isSecondary(conn)) {
                primaryKeyByGroup.putIfAbsent(conn.getZSideRedundancyGroup(), keyOf(conn));
            }
        }
        for (PlannedConnection conn : ordered) {
            String key = keyOf(conn);
            connectionKeyByName.put(conn.getName(), key);
            List<String> deps = new ArrayList<>();
            String router = routerKeyByName.get(conn.getASideRouterName());
            if (router != null && conn.getASideExistingRouterUuid() == null && conn.getASidePortUuid() == null) {
                deps.add(router);
            }
            if (isSecondary(conn) && primaryKeyByGroup.containsKey(conn.getZSideRedundancyGroup())) {
                deps.add(primaryKeyByGroup.get(conn.getZSideRedundancyGroup()));
            }
            steps.add(new PlanStep(key, ResourceKind.PROVIDER_CONNECTION, conn.getName(), deps, null));
        }

        for (PlannedBackboneLink link : nz(plan.getBackboneLinks())) {
            PlannedConnection conn = link.getConnection();
            String key = conn != null ? keyOf(conn) : "backbone/" + link.getName();
            String name = conn != null ? conn.getName() : link.getName();
            connectionKeyByName.put(name, key);
            List<String> deps = new ArrayList<>();
            if (conn != null) {
                addIfPresent(deps, routerKeyByName.get(conn.getASideRouterName()));
                addIfPresent(deps, routerKeyByName.get(conn.getZSideRouterName()));
            }
            steps.add(new PlanStep(key, ResourceKind.BACKBONE_LINK, name, deps, null));
        }

        Map<String, String> directKeyByConnection = new HashMap<>();
        for (PlannedRoutingProtocol rp : nz(plan.getRoutingProtocols())) {
            if (rp.getType() == RoutingProtocolType.DIRECT) {
                directKeyByConnection.putIfAbsent(rp.getConnectionName(), keyOf(rp));
            }
        }
        List<PlannedRoutingProtocol> protocols = new ArrayList<>(nz(plan.getRoutingProtocols()));
        protocols.sort((a, b) -> Boolean.compare(a.getType() != RoutingProtocolType.DIRECT,
                b.getType() != RoutingProtocolType.DIRECT));
        for (PlannedRoutingProtocol rp : protocols) {
            List<String> deps = new ArrayList<>();
            addIfPresent(deps, connectionKeyByName.get(rp.getConnectionName()));
            if (rp.getType() == RoutingProtocolType.BGP) {
                addIfPresent(deps, directKeyByConnection.get(rp.getConnectionName()));
            }
            steps.add(new PlanStep(keyOf(rp), ResourceKind.ROUTING_PROTOCOL, rp.getName(), deps, null));
        }

        for (PlannedInterconnect ic : plan.interconnectsOrEmpty()) {
            steps.add(new PlanStep(keyOf(ic), ResourceKind.INTERCONNECT, ic.getName(), List.of(),
                    PlannedInterconnect.NOT_EXECUTABLE_REASON));
        }
        for (PlannedMulticloudInterconnect link : plan.multicloudLinksOrEmpty()) {
            steps.add(new PlanStep("multicloud/" + link.getName(), ResourceKind.MULTICLOUD_LINK, link.getName(),
                    List.of(), "a native provider-to-provider link is created by the customer with the two cloud "
                    + "providers (create, then accept with the activation key); the SDK calls no cloud-provider API"));
        }
        return steps;
    }

    /**
     * The router's logical key, or {@code router/<name>} when it has none.
     *
     * @param router the planned router
     * @return the key
     */
    public static String keyOf(PlannedCloudRouter router) {
        return router.getKey() != null ? router.getKey() : "router/" + router.getName();
    }

    /**
     * The connection's logical key, or {@code connection/<name>} when it has none.
     *
     * @param conn the planned connection
     * @return the key
     */
    public static String keyOf(PlannedConnection conn) {
        return conn.getKey() != null ? conn.getKey() : "connection/" + conn.getName();
    }

    /**
     * The routing protocol's logical key, or {@code rp/<name>} when it has none.
     *
     * @param rp the planned routing protocol
     * @return the key
     */
    public static String keyOf(PlannedRoutingProtocol rp) {
        return rp.getKey() != null ? rp.getKey() : "rp/" + rp.getName();
    }

    /**
     * The Interconnect's logical key, or {@code interconnect/<name>} when it has none.
     *
     * @param ic the planned Interconnect
     * @return the key
     */
    public static String keyOf(PlannedInterconnect ic) {
        return ic.getKey() != null ? ic.getKey() : "interconnect/" + ic.getName();
    }

    static boolean hasGroup(PlannedConnection conn) {
        return conn.getZSideRedundancyGroup() != null && !conn.getZSideRedundancyGroup().isBlank();
    }

    static boolean isSecondary(PlannedConnection conn) {
        return hasGroup(conn) && conn.getRedundancyPriority() == RedundancyPriority.SECONDARY;
    }

    private static void addIfPresent(List<String> deps, String key) {
        if (key != null && !deps.contains(key)) {
            deps.add(key);
        }
    }

    private static <T> List<T> nz(List<T> list) {
        return list == null ? List.of() : list;
    }
}
