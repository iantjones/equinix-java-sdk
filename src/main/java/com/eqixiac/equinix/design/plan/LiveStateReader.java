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

import com.eqixiac.equinix.FabricGateway;
import com.eqixiac.equinix.core.model.KeyValuePair;
import com.eqixiac.equinix.design.optimizer.wizard.model.DeploymentPlan;
import com.eqixiac.equinix.design.optimizer.wizard.model.PlannedBackboneLink;
import com.eqixiac.equinix.design.optimizer.wizard.model.PlannedCloudRouter;
import com.eqixiac.equinix.design.optimizer.wizard.model.PlannedConnection;
import com.eqixiac.equinix.fabric.model.CloudRouter;
import com.eqixiac.equinix.fabric.model.Connection;
import com.eqixiac.equinix.fabric.model.RoutingProtocol;
import com.eqixiac.equinix.fabric.model.implementation.AccessPoint;
import com.eqixiac.equinix.fabric.model.implementation.ConnectionSide;
import com.eqixiac.equinix.fabric.model.implementation.Notification;
import com.eqixiac.equinix.fabric.model.implementation.filter.FilterPropertyList;
import com.eqixiac.equinix.fabric.model.implementation.filter.FilterType;

import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reads the live Fabric resources relevant to a plan. Read-only: it issues searches and GETs only.
 *
 * <p><b>Scope.</b> When the plan names a project, routers and connections are searched with
 * {@code /project/projectId = <project>}; otherwise by {@code /name IN <planned names>} (a plan with
 * no project cannot see unmanaged resources, and the snapshot says so). From the results it keeps the
 * routers whose name is planned or carries the plan's name prefix, and the connections whose
 * {@code additionalInfo} carries the plan's deployment marker ({@link PlanMarker}), or whose name is
 * planned or carries the prefix <em>and</em> whose own project is the plan's: a shared project's other
 * resources are not the plan's business.</p>
 *
 * <p><b>The server-side project filter is not trusted for connections.</b> Observed read-only on
 * 2026-11-12: {@code POST /fabric/v4/connections/search} with {@code /project/projectId =} returned 86
 * connections from 14 other projects, while the router search honours the filter. The project is
 * therefore re-checked on every connection client-side, and connections recorded in a checkpoint are
 * read by uuid ({@code GET /fabric/v4/connections/{uuid}}; HTTP 404 means gone), which does not
 * depend on any search filter.
 * Routing protocols are listed only for the connections kept; a failure to list them (for example a
 * 403 on a connection owned by someone else) becomes a note, not an error.</p>
 */
public final class LiveStateReader {

    private LiveStateReader() {
    }

    /**
     * Reads the live state for a plan.
     *
     * @param fabric the Fabric client
     * @param plan   the plan
     * @return the snapshot
     * @throws com.eqixiac.equinix.core.exception.EquinixServiceException when a search itself fails
     */
    public static LiveState read(FabricGateway fabric, DeploymentPlan plan) {
        return read(fabric, plan, null);
    }

    /**
     * Reads the live state for a plan, also reading every connection the checkpoint records by uuid.
     *
     * @param fabric     the Fabric client
     * @param plan       the plan
     * @param checkpoint the deployment's checkpoint, or {@code null}
     * @return the snapshot
     * @throws com.eqixiac.equinix.core.exception.EquinixServiceException when a search itself fails
     */
    public static LiveState read(FabricGateway fabric, DeploymentPlan plan, Checkpoint checkpoint) {
        List<String> notes = new ArrayList<>();
        String project = plan.getProjectId();
        String prefix = plan.getNamePrefix();
        String deployment = plan.effectiveDeploymentName();

        Set<String> routerNames = new LinkedHashSet<>();
        for (PlannedCloudRouter r : nz(plan.getCloudRouters())) {
            routerNames.add(r.getName());
        }
        Set<String> connectionNames = new LinkedHashSet<>();
        for (PlannedConnection c : nz(plan.getProviderConnections())) {
            connectionNames.add(c.getName());
        }
        for (PlannedBackboneLink l : nz(plan.getBackboneLinks())) {
            connectionNames.add(l.getConnection() != null ? l.getConnection().getName() : l.getName());
        }
        if (project == null) {
            notes.add("The plan names no project: routers and connections were searched by planned name only, "
                    + "so unmanaged resources cannot be detected.");
        }

        List<LiveRouter> routers = new ArrayList<>();
        if (project != null || !routerNames.isEmpty()) {
            for (CloudRouter cr : fabric.cloudRouters().search(scope(project, routerNames)).loadAll()) {
                LiveRouter live = toLive(cr);
                if (routerNames.contains(live.name()) || PlanMarker.hasPrefix(live.name(), prefix)) {
                    routers.add(live);
                }
            }
        }

        List<LiveConnection> connections = new ArrayList<>();
        if (project != null || !connectionNames.isEmpty()) {
            int outside = 0;
            for (Connection c : fabric.connections().search(scope(project, connectionNames)).loadAll()) {
                LiveConnection live = toLive(c);
                boolean inProject = project == null || live.projectId() == null || project.equals(live.projectId());
                if (!inProject) {
                    outside++;
                }
                if (PlanMarker.belongsTo(live.additionalInfo(), deployment)
                        || (inProject && (connectionNames.contains(live.name()) || PlanMarker.hasPrefix(live.name(), prefix)))) {
                    connections.add(live);
                }
            }
            if (outside > 0) {
                notes.add("The connection search returned " + outside + " connection(s) from other projects despite "
                        + "the project filter; they were ignored unless they carry this deployment's marker.");
            }
        }
        if (checkpoint != null) {
            Set<String> seen = new java.util.HashSet<>();
            connections.forEach(c -> seen.add(c.uuid()));
            for (CompletedStep step : checkpoint.steps().values()) {
                if (!step.kind().isConnection() || step.uuid() == null || seen.contains(step.uuid())) {
                    continue;
                }
                try {
                    connections.add(toLive(fabric.connections().getByUuid(step.uuid())));
                } catch (com.eqixiac.equinix.core.exception.EquinixNotFoundException gone) {
                    // Deleted since the checkpoint was written: it is missing.
                } catch (RuntimeException e) {
                    notes.add("Connection " + step.name() + " (" + step.uuid() + ") recorded in the checkpoint could "
                            + "not be read: " + e.getMessage());
                }
            }
        }

        Map<String, List<LiveRoutingProtocol>> protocols = new LinkedHashMap<>();
        for (LiveConnection c : connections) {
            if (c.isGone() || !"IP_VC".equals(c.type())) {
                continue;
            }
            try {
                List<LiveRoutingProtocol> list = new ArrayList<>();
                for (RoutingProtocol rp : fabric.routingProtocols().list(c.uuid()).loadAll()) {
                    list.add(toLive(rp, c.uuid()));
                }
                protocols.put(c.uuid(), list);
            } catch (RuntimeException e) {
                notes.add("Routing protocols of connection " + c.name() + " (" + c.uuid() + ") could not be read: "
                        + e.getMessage());
            }
        }
        return new LiveState(routers, connections, protocols, notes, Clock.systemUTC().instant());
    }

    private static FilterPropertyList scope(String project, Set<String> names) {
        FilterPropertyList filter = new FilterPropertyList(FilterType.AND);
        if (project != null) {
            filter.equals("/project/projectId", project);
        } else {
            filter.in("/name", new ArrayList<>(names));
        }
        return filter;
    }

    static LiveRouter toLive(CloudRouter cr) {
        return new LiveRouter(cr.getUuid(), cr.getName(), name(cr.getState()),
                cr.getLocation() != null && cr.getLocation().getMetroCode() != null
                        ? cr.getLocation().getMetroCode().name() : null,
                cr.getRouterPackage() != null ? name(cr.getRouterPackage().getCode()) : null,
                cr.getProject() != null ? cr.getProject().getProjectId() : null,
                emails(cr.getNotifications()));
    }

    static LiveConnection toLive(Connection c) {
        AccessPoint a = accessPoint(c.getASide());
        AccessPoint z = accessPoint(c.getZSide());
        Map<String, String> info = new LinkedHashMap<>();
        if (c.getAdditionalInfo() != null) {
            for (KeyValuePair kv : c.getAdditionalInfo()) {
                if (kv != null && kv.getKey() != null) {
                    info.put(kv.getKey(), kv.getValue() == null ? "" : kv.getValue());
                }
            }
        }
        return new LiveConnection(c.getUuid(), c.getName(), name(c.getState()), name(c.getType()), c.getBandwidth(),
                a != null && a.getRouter() != null ? a.getRouter().getUuid() : null,
                z != null && z.getRouter() != null ? z.getRouter().getUuid() : null,
                z != null && z.getProfile() != null ? z.getProfile().getUuid() : null,
                z != null ? z.getSellerRegion() : null,
                z != null ? name(z.getPeeringType()) : null,
                c.getProject() != null ? c.getProject().getProjectId() : null,
                info, emails(c.getNotifications()));
    }

    static LiveRoutingProtocol toLive(RoutingProtocol rp, String connectionUuid) {
        return new LiveRoutingProtocol(rp.getUuid(), rp.getName(), name(rp.getType()), name(rp.getState()),
                connectionUuid,
                rp.getDirectIpv4() != null ? rp.getDirectIpv4().getEquinixIfaceIp() : null,
                rp.getBgpIpv4() != null ? rp.getBgpIpv4().getCustomerPeerIp() : null,
                rp.getCustomerAsn(),
                rp.getBfd() != null ? rp.getBfd().getEnabled() : null);
    }

    private static AccessPoint accessPoint(ConnectionSide side) {
        return side == null ? null : side.getAccessPoint();
    }

    private static List<String> emails(List<Notification> notifications) {
        List<String> emails = new ArrayList<>();
        if (notifications != null) {
            for (Notification n : notifications) {
                if (n != null && n.getEmails() != null) {
                    emails.addAll(n.getEmails());
                }
            }
        }
        return emails;
    }

    private static String name(Enum<?> value) {
        return value == null ? null : value.name();
    }

    private static <T> List<T> nz(List<T> list) {
        return list == null ? List.of() : list;
    }
}
