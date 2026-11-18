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
package com.eqixiac.equinix.design.optimizer;

import com.eqixiac.equinix.FabricGateway;
import com.eqixiac.equinix.core.enums.Region;
import com.eqixiac.equinix.design.access.AccessInventory;
import com.eqixiac.equinix.design.access.AccessKind;
import com.eqixiac.equinix.design.access.AccessMode;
import com.eqixiac.equinix.design.access.AccessPlan;
import com.eqixiac.equinix.design.access.AccessPlanner;
import com.eqixiac.equinix.design.access.AccessSource;
import com.eqixiac.equinix.design.access.ExistingEdgeDevice;
import com.eqixiac.equinix.design.access.ExistingPort;
import com.eqixiac.equinix.design.access.MetroAccess;
import com.eqixiac.equinix.design.access.PlannedAccessItem;
import com.eqixiac.equinix.design.connectivity.ProductAvailability;
import com.eqixiac.equinix.design.optimizer.enums.SiteRole;
import com.eqixiac.equinix.design.optimizer.model.OptimizationResult;
import com.eqixiac.equinix.design.optimizer.wizard.DeploymentWizard;
import com.eqixiac.equinix.design.optimizer.wizard.model.DeploymentPlan;
import com.eqixiac.equinix.design.optimizer.wizard.model.PlannedConnection;
import com.eqixiac.equinix.design.plan.PlanStep;
import com.eqixiac.equinix.design.plan.PlanSteps;
import com.eqixiac.equinix.design.plan.ResourceKind;
import com.eqixiac.equinix.design.resilience.ResilienceAnalyzer;
import com.eqixiac.equinix.design.resilience.ResilienceFindings;
import com.eqixiac.equinix.design.resilience.ResilienceReport;
import com.eqixiac.equinix.design.resilience.ScenarioKind;
import com.eqixiac.equinix.design.resilience.ScenarioOutcome;
import com.eqixiac.equinix.design.resilience.Verdict;
import com.eqixiac.equinix.design.spec.CloudIntent;
import com.eqixiac.equinix.design.spec.DesignConstraints;
import com.eqixiac.equinix.design.spec.DesignSpec;
import com.eqixiac.equinix.design.spec.SiteIntent;
import com.eqixiac.equinix.design.spec.WorkloadIntent;
import com.eqixiac.equinix.design.value.ratecard.ReferenceRateCard;
import com.eqixiac.equinix.fabric.enums.GeoScopeType;
import com.eqixiac.equinix.fabric.enums.RedundancyPriority;
import com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Metro access on a two-metro design (Ashburn with a cage, London with an office): what the access
 * planner chooses, what the Deployment Wizard puts on the plan, and what the failure simulation does
 * with ports and Network Edge devices.
 */
class MetroAccessTest {

    private static final DesignSpec SPEC = DesignSpec.builder().name("access")
            .site(SiteIntent.builder().label("Ashburn cage").metro("DC").role(SiteRole.DATA_CENTER).build())
            .site(SiteIntent.builder().label("London office").metro("LD").build())
            .cloud(CloudIntent.builder().id("aws").provider(CloudProviderType.AWS).build())
            .workload(WorkloadIntent.builder().label("api").bandwidthMbps(500).clouds(List.of("aws")).build())
            .constraints(DesignConstraints.builder().requiredMetros(List.of("DC", "LD")).build())
            .build();

    private static final FabricGateway FABRIC = OptimizerFixtures.gateway(List.of(
            OptimizerFixtures.metro("DC", "Ashburn", Region.AMER, 39.0438, -77.4874,
                    List.of(GeoScopeType.CONUS), OptimizerFixtures.link("LD", 73.4)),
            OptimizerFixtures.metro("LD", "London", Region.EMEA, 51.5074, -0.1278, List.of(GeoScopeType.UK),
                    OptimizerFixtures.link("DC", 73.4))),
            List.of(OptimizerFixtures.profile("sp-aws", "AWS Direct Connect", "DC:us-east-1", "LD:eu-west-2")));

    private static final OptimizationResult RESULT =
            MetroOptimizer.forSpec(FABRIC, SPEC).rateCard(ReferenceRateCard.standard()).run();

    /** The same design with its workload pinned to Ashburn, so it has no metro to fail over to. */
    private static final DesignSpec PINNED = SPEC.toBuilder().clearWorkloads()
            .workload(WorkloadIntent.builder().label("api").bandwidthMbps(500).clouds(List.of("aws"))
                    .pinnedMetro("DC").build())
            // London then carries no workload: it stays in the design as an access metro for the office.
            .constraints(DesignConstraints.builder().requiredMetros(List.of("DC", "LD")).allowAccessMetros(true)
                    .build())
            .build();

    private static final OptimizationResult PINNED_RESULT =
            MetroOptimizer.forSpec(FABRIC, PINNED).rateCard(ReferenceRateCard.standard()).run();

    private static ResilienceReport simulate(AccessPlan access) {
        return ResilienceAnalyzer.forSpec(FABRIC, PINNED).optimization(PINNED_RESULT)
                .rateCard(ReferenceRateCard.standard()).access(access).run();
    }

    private static AccessPlanner pinnedPlanner() {
        return AccessPlanner.forSpec(PINNED).optimization(PINNED_RESULT).availability(NE_IN_LONDON);
    }

    private static final ProductAvailability NE_IN_LONDON = ProductAvailability.of(Set.of("LD"), Set.of());

    private static ExistingPort port(String uuid, String name, String ibx, String device, String group,
                                     RedundancyPriority priority) {
        return new ExistingPort(uuid, name, "DC", ibx, device, group, priority, 10_000, 9_000, "ACTIVE");
    }

    private static AccessPlanner planner() {
        return AccessPlanner.forSpec(SPEC).optimization(RESULT).availability(NE_IN_LONDON);
    }

    private static DeploymentPlan wizard(AccessPlan access) {
        return DeploymentWizard.builder(FABRIC, RESULT).projectId("project-1")
                .rateCard(ReferenceRateCard.standard()).notifications("noc@example.com").access(access).plan();
    }

    private static ScenarioOutcome outcome(ResilienceReport report, String scenarioId) {
        return report.singleFailures().stream().filter(o -> o.scenario().id().equals(scenarioId)).findFirst()
                .orElseThrow(() -> new AssertionError("no scenario " + scenarioId + " in "
                        + report.singleFailures().stream().map(o -> o.scenario().id()).toList()));
    }

    @Test
    @DisplayName("with nothing in the project: two ports and cross connects to order at the cage, a device pair where Network Edge is offered")
    void ordersWhenNothingExists() {
        AccessPlan plan = planner().plan();

        MetroAccess dc = plan.metro("DC");
        assertEquals(AccessMode.PHYSICAL_PORTS, dc.mode());
        assertEquals(2, dc.items(AccessKind.FABRIC_PORT).size());
        assertEquals(2, dc.items(AccessKind.CROSS_CONNECT).size());
        assertTrue(dc.items().stream().allMatch(i -> i.source() == AccessSource.ORDER));
        assertEquals(2, dc.links().size());
        assertNull(dc.links().get(0).endpointUuid(), "an order has no uuid yet");
        assertEquals(500, dc.links().get(0).bandwidthMbps(), "the 500 Mbps the placement lands in DC");

        MetroAccess ld = plan.metro("LD");
        assertEquals(AccessMode.NETWORK_EDGE, ld.mode());
        assertEquals(2, ld.items(AccessKind.NETWORK_EDGE_DEVICE).size());

        AccessPlan unknown = AccessPlanner.forSpec(SPEC).optimization(RESULT).plan();
        assertEquals(AccessMode.CLOUD_ROUTER_ONLY, unknown.metro("LD").mode());
        assertTrue(unknown.metro("LD").links().isEmpty());
    }

    @Test
    @DisplayName("an existing redundant pair is used before anything is ordered")
    void existingPairFirst() {
        AccessInventory inventory = AccessInventory.of(List.of(
                port("p-3", "spare", "DC2", "dev-a", null, null),
                port("p-1", "cage-a", "DC2", "dev-a", "grp", RedundancyPriority.PRIMARY),
                port("p-2", "cage-b", "DC6", "dev-b", "grp", RedundancyPriority.SECONDARY)), List.of());

        MetroAccess dc = planner().inventory(inventory).plan().metro("DC");

        assertEquals(List.of("p-1", "p-2"), dc.items().stream().map(PlannedAccessItem::existingUuid).toList());
        assertTrue(dc.items().stream().allMatch(i -> i.source() == AccessSource.EXISTING));
        assertTrue(dc.items(AccessKind.CROSS_CONNECT).isEmpty(), "an existing port has its cross connect");
        assertEquals("p-1", dc.links().get(0).endpointUuid());
        assertTrue(dc.findings().isEmpty(), dc.findings().toString());
    }

    @Test
    @DisplayName("one existing port is used and the second is ordered; two ports in one IBX are flagged")
    void oneExistingPortAndSharedIbx() {
        MetroAccess one = planner().inventory(AccessInventory.of(List.of(
                port("p-1", "cage-a", "DC2", "dev-a", null, null)), List.of())).plan().metro("DC");
        assertEquals(List.of(AccessSource.EXISTING, AccessSource.ORDER, AccessSource.ORDER),
                one.items().stream().map(PlannedAccessItem::source).toList());
        assertTrue(one.findings().stream().anyMatch(f -> f.contains("until it is in service")), one.findings().toString());

        MetroAccess sameIbx = planner().inventory(AccessInventory.of(List.of(
                port("p-1", "cage-a", "DC2", "dev-a", null, null),
                port("p-2", "cage-b", "DC2", "dev-b", null, null)), List.of())).plan().metro("DC");
        assertTrue(sameIbx.findings().stream().anyMatch(f -> f.contains("IBX DC2")), sameIbx.findings().toString());

        MetroAccess full = planner().inventory(AccessInventory.of(List.of(new ExistingPort("p-9", "full", "DC",
                "DC2", "dev-a", null, null, 10_000, 100, "ACTIVE")), List.of())).plan().metro("DC");
        assertTrue(full.items().stream().allMatch(i -> i.source() == AccessSource.ORDER), "no room on the port");
    }

    @Test
    @DisplayName("a named uuid the inventory does not hold is assumed to exist; existing devices are used as a pair")
    void namedAndDevices() {
        MetroAccess named = planner().usePorts("dc", "p-x", "p-y").plan().metro("DC");
        assertEquals(List.of(AccessSource.ASSUMED, AccessSource.ASSUMED),
                named.items().stream().map(PlannedAccessItem::source).toList());
        assertEquals("p-y", named.links().get(1).endpointUuid());

        AccessInventory devices = AccessInventory.of(List.of(), List.of(
                new ExistingEdgeDevice("d-2", "edge-b", "LD", "C8000V", "PROVISIONED", "SECONDARY", "d-1"),
                new ExistingEdgeDevice("d-1", "edge-a", "LD", "C8000V", "PROVISIONED", "PRIMARY", "d-2"),
                new ExistingEdgeDevice("d-3", "edge-c", "LD", "C8000V", "PROVISIONING", null, null)));
        MetroAccess ld = planner().inventory(devices).plan().metro("LD");
        assertEquals(List.of("d-1", "d-2"), ld.items().stream().map(PlannedAccessItem::existingUuid).toList());

        AccessPlan stray = planner().usePorts("SV", "p-z").plan();
        assertTrue(stray.notes().stream().anyMatch(n -> n.contains("SV")), stray.notes().toString());
    }

    @Test
    @DisplayName("the wizard connects each Cloud Router to the plan's ports and devices, and creates only the ones that exist")
    void wizardPutsAccessOnThePlan() {
        AccessPlan access = planner().usePorts("DC", "p-1", "p-2").plan();
        DeploymentPlan with = wizard(access);
        DeploymentPlan without = wizard(null);

        assertTrue(with.isValid(), String.valueOf(with.getValidationErrors()));
        assertEquals(4, with.accessConnectionsOrEmpty().size(), "two ports in DC, two devices in LD");
        PlannedConnection toPort = with.accessConnectionsOrEmpty().get(0);
        assertEquals("p-1", toPort.getZSidePortUuid());
        assertTrue(toPort.hasAccessEndpoint());
        assertFalse(with.accessConnectionsOrEmpty().get(2).hasAccessEndpoint(), "the device is an order");

        List<PlanStep> steps = PlanSteps.of(with);
        assertEquals(2, steps.stream().filter(s -> s.kind() == ResourceKind.ACCESS_CONNECTION && s.executable()).count());
        assertEquals(2, steps.stream().filter(s -> s.kind() == ResourceKind.ACCESS_CONNECTION && !s.executable()).count());
        assertTrue(steps.stream().filter(s -> s.kind() == ResourceKind.ACCESS_ITEM).noneMatch(PlanStep::executable));

        assertEquals(without.getProviderConnections(), with.getProviderConnections());
        assertEquals(without.getRoutingProtocols(),
                with.getRoutingProtocols().subList(0, without.getRoutingProtocols().size()),
                "access protocols come after the others, so their subnets do not move");
        assertEquals(without.getRoutingProtocols().size() + 4, with.getRoutingProtocols().size(), "DIRECT and BGP for the two connections that can be created");

        assertEquals(0, without.getPricing().getMonthlyTotal().compareTo(with.getPricing().getMonthlyTotal()));
        assertEquals(0, without.getPricing().getDesignMonthly().compareTo(with.getPricing().getDesignMonthly()),
                "the optimizer-scope total does not change");
        assertNotNull(with.getPricing().getAccessConnectionMonthly());
        assertEquals(0, with.getPricing().getDesignMonthly().add(with.getPricing().getAccessConnectionMonthly())
                .compareTo(with.getPricing().getDesignWithAccessMonthly()));
        assertEquals(2, with.getPricing().getAccessUnpriced().size(), "the two Network Edge devices to order");
        assertNull(without.getPricing().getAccessConnectionMonthly());

        assertTrue(with.toMarkdown().contains("### Access connections"));
        assertTrue(with.getRequiredInputs().stream().anyMatch(r -> r.isVlanTagRequired()
                && r.getConnectionName().equals(toPort.getName())));
        String hcl = new com.eqixiac.equinix.design.export.TerraformExporter().export(with);
        assertEquals(with.totalResourceCount(), hcl.split("\nresource \"", -1).length - 1,
                "one resource block per resource execution creates");
        assertTrue(hcl.contains("\"COLO\"") && hcl.contains("p-1") && hcl.contains("does not exist yet"), hcl);
        String diagram = new com.eqixiac.equinix.design.export.TopologyDiagram().toMermaid(with);
        assertTrue(diagram.contains("Port ") && diagram.contains("to order"), diagram);
        assertNotEquals(without.planId(), with.planId());
        assertFalse(without.toJson().contains("\"access\""), "a plan without access serialises as before");
        DeploymentPlan reread = DeploymentPlan.fromJson(with.toJson());
        assertEquals(with.planId(), reread.planId());
        assertEquals(access, reread.getAccess());
    }

    @Test
    @DisplayName("an access plan for another design is a validation error")
    void accessPlanForAnotherDesign() {
        AccessPlan other = new AccessPlan("another-fingerprint", planner().plan().metros(), List.of());
        DeploymentPlan plan = wizard(other);
        assertFalse(plan.isValid());
        assertTrue(plan.getValidationErrors().get(0).contains("another design"));
    }

    @Test
    @DisplayName("the simulation fails ports, cross connects and devices one at a time; a pair survives, a single port does not")
    void resilienceCoversAccess() {
        ResilienceReport pair = simulate(pinnedPlanner().plan());
        ResilienceReport none = simulate(null);

        assertNotEquals(Verdict.OUTAGE, outcome(pair, "access:DC/DC-port-primary").verdict());
        assertNotEquals(Verdict.OUTAGE, outcome(pair, "xc:DC/DC-port-primary").verdict());
        assertNotEquals(Verdict.OUTAGE, outcome(pair, "accessconn:LD/LD-ne-secondary").verdict());
        assertEquals(none.n1().outages(), pair.n1().outages(), "a redundant pair adds no single point of failure");
        assertTrue(pair.n1().scenarios() > none.n1().scenarios(), "the access elements are in the N-1 set");
        assertTrue(none.singleFailures().stream().noneMatch(o -> o.scenario().kind() == ScenarioKind.ACCESS_ELEMENT));

        ResilienceReport single = simulate(pinnedPlanner().usePorts("DC", "p-1").plan());
        assertEquals(Verdict.OUTAGE, outcome(single, "access:DC/DC-port-1").verdict());
        assertEquals(Verdict.OUTAGE, outcome(single, "xc:DC/DC-port-1").verdict());
        assertEquals(ResilienceFindings.Level.FAIL, single.n1().level());
        assertTrue(single.remediations().stream().anyMatch(r -> r.action().contains("second port")),
                single.remediations().toString());

        ResilienceReport failover = ResilienceAnalyzer.forSpec(FABRIC, SPEC).optimization(RESULT)
                .rateCard(ReferenceRateCard.standard()).access(planner().usePorts("DC", "p-1").plan()).run();
        assertNotEquals(Verdict.OUTAGE, outcome(failover, "access:DC/DC-port-1").verdict(),
                "a workload with a standby metro moves there when the only port is lost");
    }

    @Test
    @DisplayName("two ports in one IBX share fate: the IBX loss is reported and is not counted in N-1")
    void sharedIbx() {
        AccessInventory inventory = AccessInventory.of(List.of(
                port("p-1", "cage-a", "DC2", "dev-a", null, null),
                port("p-2", "cage-b", "DC2", "dev-b", null, null)), List.of());
        ResilienceReport report = simulate(pinnedPlanner().inventory(inventory).plan());

        ScenarioOutcome ibx = outcome(report, "ibx:DC2");
        assertEquals(ScenarioKind.FACILITY, ibx.scenario().kind());
        assertEquals(Verdict.OUTAGE, ibx.verdict());
        assertNotEquals(Verdict.OUTAGE, outcome(report, "access:DC/cage-a").verdict());
        assertEquals(simulate(pinnedPlanner().plan()).n1().outages(), report.n1().outages());
    }
}
