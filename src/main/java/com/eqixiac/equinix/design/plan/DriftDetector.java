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
import com.eqixiac.equinix.design.optimizer.wizard.model.PlannedRoutingProtocol;
import com.eqixiac.equinix.fabric.enums.RoutingProtocolType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Compares a plan with a {@link LiveState} snapshot. A pure function: no I/O.
 *
 * <p><b>Matching</b>, first hit wins: the uuid recorded in the checkpoint (when one is given); for a
 * connection, the {@link PlanMarker} ({@value PlanMarker#DEPLOYMENT_KEY} equal to the plan's deployment
 * name and {@value PlanMarker#LOGICAL_KEY} equal to the step key); the exact planned name; for a routing
 * protocol, the only live protocol of the planned type on the matched connection. A live resource that
 * is {@code DEPROVISIONING}/{@code DEPROVISIONED} (or a {@code CANCELLED} connection) never matches.</p>
 *
 * <p><b>Compared fields.</b> Cloud Router: name, metro, package, notification recipients, project.
 * Connection: name, type, bandwidth, A-side router, Z-side router (backbone), Z-side service profile,
 * seller region, peering type, notification recipients. Routing protocol: DIRECT interface address;
 * BGP customer peer address (compared without the prefix length), customer ASN and BFD. Patchable in
 * place, per the Fabric v4 spec's PATCH examples: router name, package and notifications; connection
 * name, bandwidth and notifications. Everything else is reported, never forced.</p>
 */
public final class DriftDetector {

    private DriftDetector() {
    }

    /**
     * Diffs a plan against live state.
     *
     * @param plan       the plan
     * @param live       the snapshot
     * @param checkpoint the deployment's checkpoint, or {@code null}
     * @return the diff
     */
    public static PlanDiff diff(DeploymentPlan plan, LiveState live, Checkpoint checkpoint) {
        Map<String, String> knownUuid = new HashMap<>();
        if (checkpoint != null) {
            checkpoint.steps().forEach((k, v) -> knownUuid.put(k, v.uuid()));
        }
        List<ResourceDrift> out = new ArrayList<>();
        Set<String> matchedLive = new HashSet<>();
        Map<String, String> routerUuidByName = new HashMap<>();
        Map<String, LiveConnection> connectionByName = new HashMap<>();

        for (PlannedCloudRouter planned : nz(plan.getCloudRouters())) {
            String key = PlanSteps.keyOf(planned);
            Match<LiveRouter> m = matchRouter(planned, key, live, knownUuid);
            if (m == null) {
                out.add(new ResourceDrift(key, ResourceKind.CLOUD_ROUTER, planned.getName(), DriftStatus.MISSING,
                        null, null, List.of(), null));
                continue;
            }
            matchedLive.add(m.value.uuid());
            routerUuidByName.put(planned.getName(), m.value.uuid());
            List<FieldDifference> d = new ArrayList<>();
            compare(d, "name", planned.getName(), m.value.name(), true);
            compare(d, "metro", planned.getMetroId() == null ? null : planned.getMetroId().code(), m.value.metroCode(), false);
            compare(d, "packageCode", planned.getPackageCode() == null ? null : planned.getPackageCode().name(),
                    m.value.packageCode(), true);
            compareEmails(d, planned.getNotificationEmails(), m.value.notificationEmails());
            if (planned.getProjectId() != null && m.value.projectId() != null) {
                compare(d, "projectId", planned.getProjectId(), m.value.projectId(), false);
            }
            out.add(verdict(key, ResourceKind.CLOUD_ROUTER, planned.getName(), m, d));
        }

        List<PlannedConnection> connections = new ArrayList<>();
        nz(plan.getProviderConnections()).forEach(connections::add);
        for (PlannedBackboneLink link : nz(plan.getBackboneLinks())) {
            if (link.getConnection() != null) {
                connections.add(link.getConnection());
            }
        }
        for (PlannedConnection planned : connections) {
            String key = PlanSteps.keyOf(planned);
            ResourceKind kind = planned.isBackboneLink() ? ResourceKind.BACKBONE_LINK : ResourceKind.PROVIDER_CONNECTION;
            Match<LiveConnection> m = matchConnection(planned, key, plan.effectiveDeploymentName(), live, knownUuid);
            if (m == null) {
                out.add(new ResourceDrift(key, kind, planned.getName(), DriftStatus.MISSING, null, null, List.of(), null));
                continue;
            }
            matchedLive.add(m.value.uuid());
            connectionByName.put(planned.getName(), m.value);
            List<FieldDifference> d = new ArrayList<>();
            compare(d, "name", planned.getName(), m.value.name(), true);
            compare(d, "connectionType", planned.getConnectionType() == null ? null : planned.getConnectionType().name(),
                    m.value.type(), false);
            compare(d, "bandwidthMbps", String.valueOf(planned.getBandwidthMbps()),
                    m.value.bandwidthMbps() == null ? null : String.valueOf(m.value.bandwidthMbps()), true);
            String aRouter = planned.getASideExistingRouterUuid() != null ? planned.getASideExistingRouterUuid()
                    : routerUuidByName.get(planned.getASideRouterName());
            if (aRouter != null && planned.getASidePortUuid() == null) {
                compare(d, "aSideRouter", aRouter, m.value.aSideRouterUuid(), false);
            }
            if (planned.isBackboneLink()) {
                String zRouter = routerUuidByName.get(planned.getZSideRouterName());
                if (zRouter != null) {
                    compare(d, "zSideRouter", zRouter, m.value.zSideRouterUuid(), false);
                }
            } else {
                compare(d, "zSideServiceProfileUuid", planned.getZSideServiceProfileUuid(), m.value.zSideProfileUuid(), false);
                if (planned.getZSideSellerRegion() != null) {
                    compare(d, "zSideSellerRegion", planned.getZSideSellerRegion(), m.value.zSideSellerRegion(), false);
                }
                if (planned.getZSidePeeringType() != null) {
                    compare(d, "zSidePeeringType", planned.getZSidePeeringType().name(), m.value.zSidePeeringType(), false);
                }
            }
            compareEmails(d, planned.getNotificationEmails(), m.value.notificationEmails());
            out.add(verdict(key, kind, planned.getName(), m, d));
        }

        for (PlannedRoutingProtocol planned : nz(plan.getRoutingProtocols())) {
            String key = PlanSteps.keyOf(planned);
            LiveConnection parent = connectionByName.get(planned.getConnectionName());
            if (parent == null) {
                out.add(new ResourceDrift(key, ResourceKind.ROUTING_PROTOCOL, planned.getName(), DriftStatus.MISSING,
                        null, null, List.of(), "parent connection " + planned.getConnectionName() + " is missing"));
                continue;
            }
            if (!live.routingProtocols().containsKey(parent.uuid())) {
                out.add(new ResourceDrift(key, ResourceKind.ROUTING_PROTOCOL, planned.getName(), DriftStatus.UNREADABLE,
                        null, null, List.of(), "routing protocols of " + parent.name() + " were not read"));
                continue;
            }
            Match<LiveRoutingProtocol> m = matchProtocol(planned, key, live.protocolsOf(parent.uuid()), knownUuid);
            if (m == null) {
                out.add(new ResourceDrift(key, ResourceKind.ROUTING_PROTOCOL, planned.getName(), DriftStatus.MISSING,
                        null, null, List.of(), null));
                continue;
            }
            List<FieldDifference> d = new ArrayList<>();
            if (planned.getType() == RoutingProtocolType.DIRECT) {
                compare(d, "equinixIfaceIpv4", planned.getEquinixIfaceIpv4(), m.value.equinixIfaceIp(), false);
            } else if (planned.getType() == RoutingProtocolType.BGP) {
                compare(d, "customerPeerIpv4", host(planned.getCustomerPeerIpv4()), host(m.value.customerPeerIp()), false);
                if (planned.getCustomerAsn() != null) {
                    compare(d, "customerAsn", String.valueOf(planned.getCustomerAsn()),
                            m.value.customerAsn() == null ? null : String.valueOf(m.value.customerAsn()), false);
                }
                if (m.value.bfdEnabled() != null) {
                    compare(d, "bfdEnabled", String.valueOf(planned.isBfdEnabled()), String.valueOf(m.value.bfdEnabled()), false);
                }
            }
            out.add(verdict(key, ResourceKind.ROUTING_PROTOCOL, planned.getName(), m, d));
        }

        for (PlannedInterconnect ic : plan.interconnectsOrEmpty()) {
            out.add(new ResourceDrift(PlanSteps.keyOf(ic), ResourceKind.INTERCONNECT, ic.getName(),
                    DriftStatus.NOT_EXECUTABLE, ic.getExistingInterconnectUuid(), null, List.of(),
                    PlannedInterconnect.NOT_EXECUTABLE_REASON));
        }

        List<UnmanagedResource> unmanaged = new ArrayList<>();
        for (LiveRouter r : live.routers()) {
            if (!r.isGone() && !matchedLive.contains(r.uuid()) && PlanMarker.hasPrefix(r.name(), plan.getNamePrefix())) {
                unmanaged.add(new UnmanagedResource(ResourceKind.CLOUD_ROUTER, r.name(), r.uuid(),
                        "name carries the plan's prefix '" + plan.getNamePrefix() + "-' but no planned router has it"));
            }
        }
        for (LiveConnection c : live.connections()) {
            if (c.isGone() || matchedLive.contains(c.uuid())) {
                continue;
            }
            boolean marked = PlanMarker.belongsTo(c.additionalInfo(), plan.effectiveDeploymentName());
            if (marked || PlanMarker.hasPrefix(c.name(), plan.getNamePrefix())) {
                ResourceKind kind = c.zSideRouterUuid() != null ? ResourceKind.BACKBONE_LINK : ResourceKind.PROVIDER_CONNECTION;
                unmanaged.add(new UnmanagedResource(kind, c.name(), c.uuid(), marked
                        ? "carries " + PlanMarker.DEPLOYMENT_KEY + "=" + plan.effectiveDeploymentName() + " ("
                        + PlanMarker.LOGICAL_KEY + "=" + c.additionalInfo().get(PlanMarker.LOGICAL_KEY) + ") but is not in the plan"
                        : "name carries the plan's prefix '" + plan.getNamePrefix() + "-' but no planned connection has it"));
            }
        }
        List<String> notes = new ArrayList<>(live.notes());
        if (!plan.multicloudLinksOrEmpty().isEmpty()) {
            notes.add(plan.multicloudLinksOrEmpty().size() + " native multicloud link(s) are not Fabric resources and "
                    + "are not compared.");
        }
        return new PlanDiff(plan.planId(), plan.effectiveDeploymentName(), plan.getProjectId(), out, unmanaged, notes,
                live.readAt());
    }

    private record Match<T>(T value, String by) {
    }

    private static Match<LiveRouter> matchRouter(PlannedCloudRouter planned, String key, LiveState live,
                                                 Map<String, String> knownUuid) {
        String uuid = knownUuid.get(key);
        for (LiveRouter r : live.routers()) {
            if (!r.isGone() && uuid != null && uuid.equals(r.uuid())) {
                return new Match<>(r, "checkpoint");
            }
        }
        for (LiveRouter r : live.routers()) {
            if (!r.isGone() && Objects.equals(r.name(), planned.getName())) {
                return new Match<>(r, "name");
            }
        }
        return null;
    }

    private static Match<LiveConnection> matchConnection(PlannedConnection planned, String key, String deployment,
                                                         LiveState live, Map<String, String> knownUuid) {
        String uuid = knownUuid.get(key);
        for (LiveConnection c : live.connections()) {
            if (!c.isGone() && uuid != null && uuid.equals(c.uuid())) {
                return new Match<>(c, "checkpoint");
            }
        }
        for (LiveConnection c : live.connections()) {
            if (!c.isGone() && PlanMarker.belongsTo(c.additionalInfo(), deployment)
                    && key.equals(c.additionalInfo().get(PlanMarker.LOGICAL_KEY))) {
                return new Match<>(c, "marker");
            }
        }
        for (LiveConnection c : live.connections()) {
            if (!c.isGone() && Objects.equals(c.name(), planned.getName())) {
                return new Match<>(c, "name");
            }
        }
        return null;
    }

    private static Match<LiveRoutingProtocol> matchProtocol(PlannedRoutingProtocol planned, String key,
                                                           List<LiveRoutingProtocol> candidates,
                                                           Map<String, String> knownUuid) {
        String uuid = knownUuid.get(key);
        List<LiveRoutingProtocol> present = candidates.stream()
                .filter(rp -> !"DEPROVISIONED".equals(rp.state()) && !"DEPROVISIONING".equals(rp.state())).toList();
        for (LiveRoutingProtocol rp : present) {
            if (uuid != null && uuid.equals(rp.uuid())) {
                return new Match<>(rp, "checkpoint");
            }
        }
        for (LiveRoutingProtocol rp : present) {
            if (Objects.equals(rp.name(), planned.getName())) {
                return new Match<>(rp, "name");
            }
        }
        String type = planned.getType() == null ? null : planned.getType().name();
        List<LiveRoutingProtocol> sameType = present.stream().filter(rp -> Objects.equals(rp.type(), type)).toList();
        return sameType.size() == 1 ? new Match<>(sameType.get(0), "type") : null;
    }

    private static <T> ResourceDrift verdict(String key, ResourceKind kind, String name, Match<T> m,
                                             List<FieldDifference> d) {
        String uuid = m.value instanceof LiveRouter r ? r.uuid()
                : m.value instanceof LiveConnection c ? c.uuid()
                : ((LiveRoutingProtocol) m.value).uuid();
        return new ResourceDrift(key, kind, name, d.isEmpty() ? DriftStatus.PRESENT_MATCHING : DriftStatus.PRESENT_DIFFERENT,
                uuid, m.by, d, null);
    }

    private static void compare(List<FieldDifference> out, String field, String planned, String live, boolean patchable) {
        if (planned != null && !planned.equals(live)) {
            out.add(new FieldDifference(field, planned, live, patchable));
        }
    }

    private static void compareEmails(List<FieldDifference> out, List<String> planned, List<String> live) {
        Set<String> p = new LinkedHashSet<>();
        if (planned != null) {
            planned.stream().filter(e -> e != null && !e.isBlank()).forEach(p::add);
        }
        if (p.isEmpty()) {
            return;
        }
        Set<String> l = new LinkedHashSet<>(live == null ? List.of() : live);
        if (!p.equals(l)) {
            out.add(new FieldDifference("notificationEmails", String.join(",", p), String.join(",", l), true));
        }
    }

    /** {@code 10.0.0.2/30} gives {@code 10.0.0.2}; {@code null} stays {@code null}. */
    private static String host(String address) {
        if (address == null) {
            return null;
        }
        int slash = address.indexOf('/');
        return slash < 0 ? address : address.substring(0, slash);
    }

    private static <T> List<T> nz(List<T> list) {
        return list == null ? List.of() : list;
    }
}
