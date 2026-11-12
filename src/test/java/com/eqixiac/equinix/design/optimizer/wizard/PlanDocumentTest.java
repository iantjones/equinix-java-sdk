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

package com.eqixiac.equinix.design.optimizer.wizard;

import com.eqixiac.equinix.design.optimizer.model.OptimizationResult;
import com.eqixiac.equinix.design.optimizer.wizard.enums.CloudToCloudStrategy;
import com.eqixiac.equinix.design.optimizer.wizard.model.DeploymentPlan;
import com.eqixiac.equinix.design.optimizer.wizard.model.PlannedCloudRouter;
import com.eqixiac.equinix.design.optimizer.wizard.model.PlannedConnection;
import com.eqixiac.equinix.design.optimizer.wizard.model.PlannedInterconnect;
import com.eqixiac.equinix.design.optimizer.wizard.model.PlannedRoutingProtocol;
import com.eqixiac.equinix.design.plan.PlanJson;
import com.eqixiac.equinix.design.plan.PlanStep;
import com.eqixiac.equinix.design.plan.PlanSteps;
import com.eqixiac.equinix.design.plan.ResourceKind;
import com.eqixiac.equinix.design.spec.CloudIntent;
import com.eqixiac.equinix.design.spec.DesignSpec;
import com.eqixiac.equinix.design.spec.WorkloadIntent;
import com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Plan as data: stable keys, identity, JSON round trip, integrity check, Interconnect element. */
@DisplayName("DeploymentPlan as a stored document")
class PlanDocumentTest {

    private static DeploymentWizard.Builder wizard(OptimizationResult result, String prefix) {
        return DeploymentWizard.builder(null, result)
                .routerNamePrefix(prefix)
                .projectId("626329c0-0000-4000-8000-000000000000")
                .notifications("noc@example.com")
                .rateCard(MulticloudWizardFixtures.flatRateCard())
                .cloudToCloudStrategy(CloudToCloudStrategy.COMPARE);
    }

    private static DesignSpec spec() {
        return DesignSpec.builder().name("payments")
                .cloud(CloudIntent.builder().id("aws").provider(CloudProviderType.AWS).build())
                .workload(WorkloadIntent.builder().label("api").bandwidthMbps(1000).clouds(List.of("aws")).build())
                .build();
    }

    @Test
    @DisplayName("every planned resource has a logical key derived from the design, not from the name prefix")
    void keysAreStableAndPrefixIndependent() {
        DeploymentPlan a = wizard(MulticloudWizardFixtures.threeMetroSingleCloudResult(), "FCR").plan();
        DeploymentPlan b = wizard(MulticloudWizardFixtures.threeMetroSingleCloudResult(), "XYZ").plan();

        List<String> keysA = PlanSteps.of(a).stream().map(PlanStep::key).toList();
        List<String> keysB = PlanSteps.of(b).stream().map(PlanStep::key).toList();
        assertEquals(keysA, keysB, "the same design yields the same keys whatever the prefix");
        assertEquals(keysA.size(), keysA.stream().distinct().count(), "keys are unique within a plan");
        for (PlannedCloudRouter r : a.getCloudRouters()) {
            assertEquals("router/" + r.getMetroId().code(), r.getKey());
        }
        for (PlannedConnection c : a.getProviderConnections()) {
            assertTrue(c.getKey().startsWith("connection/" + c.getASideMetro().code() + "/"), c.getKey());
            assertEquals("626329c0-0000-4000-8000-000000000000", c.getProjectId());
        }
        for (PlannedRoutingProtocol rp : a.getRoutingProtocols()) {
            assertTrue(rp.getKey().endsWith("/rp/" + rp.getType().name()), rp.getKey());
        }
        assertNotEquals(a.getCloudRouters().get(0).getName(), b.getCloudRouters().get(0).getName());
    }

    @Test
    @DisplayName("routers come first, BGP after DIRECT on the same connection, connections after their router")
    void stepsAreInDependencyOrder() {
        List<PlanStep> steps = PlanSteps.of(wizard(MulticloudWizardFixtures.threeMetroSingleCloudResult(), "FCR").plan());
        for (int i = 0; i < steps.size(); i++) {
            for (String dep : steps.get(i).dependsOn()) {
                int depIndex = steps.stream().map(PlanStep::key).toList().indexOf(dep);
                assertTrue(depIndex >= 0 && depIndex < i, steps.get(i).key() + " depends on later/unknown " + dep);
            }
        }
        assertEquals(ResourceKind.CLOUD_ROUTER, steps.get(0).kind());
    }

    @Test
    @DisplayName("the plan carries the design-spec fingerprint, a deployment name and a content id")
    void identity() {
        DesignSpec spec = spec();
        DeploymentPlan plan = wizard(MulticloudWizardFixtures.awsGcpAtDc(10_000), "FCR").designSpec(spec).plan();

        assertEquals(spec.fingerprint(), plan.getSpecFingerprint());
        assertEquals("payments", plan.getDeploymentName(), "the spec's name is the default deployment name");
        assertEquals("FCR", plan.getNamePrefix());
        assertEquals(16, plan.planId().length());
        DeploymentPlan again = wizard(MulticloudWizardFixtures.awsGcpAtDc(10_000), "FCR").designSpec(spec).plan();
        assertEquals(plan.planId(), again.planId(), "same design and configuration, same plan id");
        DeploymentPlan bigger = wizard(MulticloudWizardFixtures.awsGcpAtDc(20_000), "FCR").designSpec(spec).plan();
        assertNotEquals(plan.planId(), bigger.planId(), "a different desired state has a different id");
        assertEquals(plan.planId(), plan.toBuilder().pricing(null).build().planId(),
                "pricing is not part of the plan id");
    }

    @Test
    @DisplayName("JSON round trip preserves the whole plan, including native multicloud links and Interconnects")
    void jsonRoundTrip() {
        DeploymentPlan plan = wizard(MulticloudWizardFixtures.awsGcpAtDc(10_000), "FCR")
                .designSpec(spec())
                .interconnect(PlannedInterconnect.builder().name("core-ic")
                        .metro(MulticloudWizardFixtures.DC).bandwidthMbps(10_000).purpose("Fabric One core").build())
                .plan();
        assertFalse(plan.multicloudLinksOrEmpty().isEmpty(), "fixture plans a native link");

        String json = plan.toJson();
        DeploymentPlan back = DeploymentPlan.fromJson(json);

        assertEquals(plan.getCloudRouters(), back.getCloudRouters());
        assertEquals(plan.getProviderConnections(), back.getProviderConnections());
        assertEquals(plan.getBackboneLinks(), back.getBackboneLinks());
        assertEquals(plan.getRoutingProtocols(), back.getRoutingProtocols());
        assertEquals(plan.getMulticloudLinks(), back.getMulticloudLinks());
        assertEquals(plan.getInterconnects(), back.getInterconnects());
        assertEquals(plan.getPricing(), back.getPricing());
        assertEquals(plan.getRequiredInputs(), back.getRequiredInputs());
        assertEquals(plan.getValidationErrors(), back.getValidationErrors());
        assertEquals(plan.getDeferredValidations(), back.getDeferredValidations());
        assertEquals(plan.getSkippedValidations(), back.getSkippedValidations());
        assertEquals(plan.isValid(), back.isValid());
        assertEquals(plan.getSpecFingerprint(), back.getSpecFingerprint());
        assertEquals(plan.getSourceComputedAt(), back.getSourceComputedAt());
        assertEquals(plan.planId(), back.planId());
        assertNull(back.getSourceOptimization(), "the optimization result is not part of the document");
        assertNull(back.getFabric());
        assertEquals(plan.toMarkdown(), back.toMarkdown(), "a loaded plan renders identically");
        assertEquals(json, back.toJson(), "serialization is stable");
        assertTrue(json.contains("\"planId\" : \"" + plan.planId() + "\""));
    }

    @Test
    @DisplayName("save/load through a file; an edited document is refused by its plan id")
    void saveLoadAndIntegrity(@TempDir Path dir) throws Exception {
        DeploymentPlan plan = wizard(MulticloudWizardFixtures.threeMetroSingleCloudResult(), "FCR").plan();
        Path file = dir.resolve("plan.json");
        plan.save(file);
        assertEquals(plan.getProviderConnections(), DeploymentPlan.load(file).getProviderConnections());

        String edited = Files.readString(file).replaceFirst("\"bandwidthMbps\" : \\d+", "\"bandwidthMbps\" : 1");
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> DeploymentPlan.fromJson(edited));
        assertTrue(e.getMessage().contains("does not match its content"), e.getMessage());
        assertThrows(IllegalArgumentException.class,
                () -> DeploymentPlan.fromJson(Files.readString(file).replace("\"schemaVersion\" : \"1\"",
                        "\"schemaVersion\" : \"9\"")));
    }

    @Test
    @DisplayName("an Interconnect is planned, rendered and priced as unknown, and never executable")
    void interconnectIsHonest() {
        DeploymentPlan plan = wizard(MulticloudWizardFixtures.threeMetroSingleCloudResult(), "FCR")
                .interconnect(PlannedInterconnect.builder().name("core-ic").metro(MulticloudWizardFixtures.DC).build())
                .plan();
        PlannedInterconnect ic = plan.getInterconnects().get(0);
        assertEquals("interconnect/DC/core-ic", ic.getKey());
        assertFalse(ic.isExecutable());
        PlanStep step = PlanSteps.of(plan).stream().filter(s -> s.kind() == ResourceKind.INTERCONNECT)
                .findFirst().orElseThrow();
        assertFalse(step.executable());
        assertTrue(step.notExecutableReason().contains("no endpoint that creates"), step.notExecutableReason());
        String md = plan.toMarkdown();
        assertTrue(md.contains("## Fabric Interconnects (planned, not executable)"), md);
        assertTrue(md.contains("| core-ic | DC | - | - | unpriced |"), md);
        assertEquals(plan.totalResourceCount(), plan.toBuilder().interconnects(null).build().totalResourceCount(),
                "an Interconnect is not counted as a resource execution creates");
        assertTrue(PlanJson.planId(plan) != null);
    }
}
