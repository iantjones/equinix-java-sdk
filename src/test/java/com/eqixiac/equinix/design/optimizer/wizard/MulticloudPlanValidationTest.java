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

import com.eqixiac.equinix.design.optimizer.model.MulticloudEnvironment;
import com.eqixiac.equinix.design.optimizer.model.MulticloudEnvironmentCatalog;
import com.eqixiac.equinix.design.optimizer.wizard.enums.CloudToCloudStrategy;
import com.eqixiac.equinix.design.optimizer.wizard.enums.MulticloudLinkRole;
import com.eqixiac.equinix.design.optimizer.wizard.model.DeploymentPlan;
import com.eqixiac.equinix.design.optimizer.wizard.model.PlannedMulticloudInterconnect;
import com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * How {@link PlanValidator} classifies native multicloud links: SKIPPED with a reason naming the
 * link; never DEFERRED; an ERROR only for a malformed entry or a {@code NATIVE_ONLY} flow without
 * a usable catalog environment.
 */
@DisplayName("PlanValidator: native multicloud links")
class MulticloudPlanValidationTest {

    private static final MulticloudEnvironment AWS_GCP = MulticloudEnvironmentCatalog.standard()
            .find(CloudProviderType.AWS, "us-east-1", CloudProviderType.GOOGLE_CLOUD, "us-east4").orElseThrow();

    private static PlannedMulticloudInterconnect.PlannedMulticloudInterconnectBuilder link(String name) {
        return PlannedMulticloudInterconnect.builder()
                .name(name)
                .providerA(CloudProviderType.AWS).regionA("us-east-1")
                .providerZ(CloudProviderType.GOOGLE_CLOUD).regionZ("us-east4")
                .environment(AWS_GCP)
                .role(MulticloudLinkRole.ALTERNATIVE)
                .strategy(CloudToCloudStrategy.COMPARE)
                .requestedMbps(10_000)
                .coveringTierMbps(10_000)
                .workloadLabels(List.of("Replication"));
    }

    private static PlanValidator.Result validate(List<PlannedMulticloudInterconnect> links) {
        return PlanValidator.validate(null, null, null, Collections.emptyList(), Collections.emptyList(),
                Collections.emptyList(), Collections.emptyList(), links, null, null);
    }

    @Test
    @DisplayName("a well-formed link is SKIPPED with a reason that names it; it is neither deferred nor an error")
    void linkIsSkippedNotDeferredNotError() {
        PlanValidator.Result result = validate(List.of(link("aws-gcp-DC").build()));

        assertTrue(result.errors.isEmpty(), () -> String.valueOf(result.errors));
        assertTrue(result.deferred.isEmpty(), () -> String.valueOf(result.deferred));
        assertEquals(1, result.skipped.size());
        String reason = result.skipped.get(0);
        assertTrue(reason.startsWith("Native multicloud link 'aws-gcp-DC' (AWS us-east-1 <-> GOOGLE_CLOUD us-east4, "
                + "role ALTERNATIVE) is not validated"), reason);
        assertTrue(reason.contains("created outside Fabric"), reason);
        assertTrue(reason.contains("no Fabric request body"), reason);
        assertTrue(reason.contains("calls no cloud-provider API"), reason);
        assertTrue(reason.contains("It is not deferred"), reason);
        assertTrue(result.requiredInputs.isEmpty(), "a native link adds no Fabric connection input");
    }

    @Test
    @DisplayName("a REPLACEMENT link is skipped the same way")
    void replacementLinkIsSkipped() {
        PlanValidator.Result result = validate(List.of(link("aws-gcp-DC")
                .role(MulticloudLinkRole.REPLACEMENT).strategy(CloudToCloudStrategy.NATIVE_WHEN_AVAILABLE).build()));

        assertTrue(result.errors.isEmpty());
        assertTrue(result.skipped.get(0).contains("role REPLACEMENT"));
    }

    @Test
    @DisplayName("NATIVE_ONLY with no environment, a non-GA environment or no covering size is a Layer-1 error")
    void nativeOnlyWithoutUsableEnvironmentIsAnError() {
        PlannedMulticloudInterconnect noEnvironment = link("a").strategy(CloudToCloudStrategy.NATIVE_ONLY)
                .role(MulticloudLinkRole.UNAVAILABLE).environment(null).coveringTierMbps(null).build();
        PlannedMulticloudInterconnect preview = link("b").strategy(CloudToCloudStrategy.NATIVE_ONLY)
                .role(MulticloudLinkRole.UNAVAILABLE)
                .providerZ(CloudProviderType.AZURE).regionZ("eastus")
                .environment(MulticloudEnvironmentCatalog.standard()
                        .find(CloudProviderType.AWS, "us-east-1", CloudProviderType.AZURE, "eastus").orElseThrow())
                .requestedMbps(1000).coveringTierMbps(1000).build();
        PlannedMulticloudInterconnect tooLarge = link("c").strategy(CloudToCloudStrategy.NATIVE_ONLY)
                .role(MulticloudLinkRole.UNAVAILABLE).requestedMbps(200_000).coveringTierMbps(null).build();

        PlanValidator.Result result = validate(List.of(noEnvironment, preview, tooLarge));

        assertEquals(3, result.errors.size(), () -> String.valueOf(result.errors));
        assertTrue(result.errors.get(0).contains("the catalog has no environment for the region pair"));
        assertTrue(result.errors.get(1).contains("is not GA"));
        assertTrue(result.errors.get(2).contains("lists no size covering 200000 Mbps"));
        assertTrue(result.errors.stream().allMatch(e -> e.startsWith("NATIVE_ONLY: ")));
        assertTrue(result.errors.stream().allMatch(e -> e.contains("multicloudEnvironments(...)")),
                "the error tells the caller how to supply a newer catalog entry");
        assertTrue(result.skipped.isEmpty(), "an error is not reported a second time as skipped");
    }

    @Test
    @DisplayName("the same unusable environment under COMPARE is not an error")
    void sameConditionUnderCompareIsNotAnError() {
        PlanValidator.Result result = validate(List.of(
                link("c").requestedMbps(200_000).coveringTierMbps(null).build()));

        assertTrue(result.errors.isEmpty());
        assertEquals(1, result.skipped.size());
    }

    @Test
    @DisplayName("malformed entries are Layer-1 errors: blank name, duplicate name, one cloud twice, non-positive bandwidth")
    void malformedEntriesAreErrors() {
        PlanValidator.Result result = validate(List.of(
                link(" ").build(),
                link("dup").build(),
                link("dup").build(),
                link("same").providerZ(CloudProviderType.AWS).build(),
                link("zero").requestedMbps(0).build()));

        assertEquals(4, result.errors.size(), () -> String.valueOf(result.errors));
        assertTrue(result.errors.get(0).contains("blank name"));
        assertTrue(result.errors.get(1).contains("Duplicate native multicloud link name 'dup'"));
        assertTrue(result.errors.get(2).contains("'same' must join two different clouds"));
        assertTrue(result.errors.get(3).contains("'zero' has a non-positive bandwidth"));
        assertEquals(1, result.skipped.size(), "only the first 'dup' is well-formed");
    }

    @Test
    @DisplayName("a native link name is not a Fabric name: the 24-character limit does not apply")
    void linkNameIsNotAFabricName() {
        PlanValidator.Result result = validate(List.of(
                link("aws-gcp-a-name-longer-than-twenty-four-characters").build()));

        assertTrue(result.errors.isEmpty(), () -> String.valueOf(result.errors));
    }

    @Test
    @DisplayName("the nine-argument validate() equals the ten-argument one with no links")
    void legacyOverloadEqualsNoLinks() {
        PlanValidator.Result legacy = PlanValidator.validate(null, null, null, Collections.emptyList(),
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), null, null);
        PlanValidator.Result withNull = validate(null);
        PlanValidator.Result withEmpty = validate(Collections.emptyList());

        assertEquals(legacy.errors, withNull.errors);
        assertEquals(legacy.skipped, withNull.skipped);
        assertEquals(legacy.deferred, withNull.deferred);
        assertEquals(legacy.skipped, withEmpty.skipped);
    }

    @Test
    @DisplayName("dryRun() keeps the links and classifies them again")
    void dryRunKeepsAndReclassifiesLinks() {
        DeploymentPlan plan = DeploymentWizard.builder(null, MulticloudWizardFixtures.awsGcpAtDc(10_000))
                .notifications("noc@example.com")
                .rateCard(MulticloudWizardFixtures.flatRateCard())
                .plan();

        DeploymentPlan refreshed = plan.dryRun();

        assertEquals(plan.getMulticloudLinks(), refreshed.getMulticloudLinks());
        assertEquals(plan.getSkippedValidations(), refreshed.getSkippedValidations());
        assertTrue(refreshed.isValid());
        assertEquals(1, refreshed.getSkippedValidations().stream()
                .filter(s -> s.startsWith("Native multicloud link 'aws-gcp-DC'")).count());
        assertFalse(refreshed.getDeferredValidations().stream().anyMatch(s -> s.contains("aws-gcp-DC")));
    }

    @Test
    @DisplayName("dryRun() of a NATIVE_ONLY plan without an environment stays invalid")
    void dryRunKeepsNativeOnlyError() {
        DeploymentPlan plan = DeploymentWizard.builder(null, MulticloudWizardFixtures.twoCloudResult(
                        CloudProviderType.AWS, "AWS", "us-east-2", CloudProviderType.GOOGLE_CLOUD, "GCP", "us-central1", 1000))
                .notifications("noc@example.com")
                .rateCard(MulticloudWizardFixtures.flatRateCard())
                .cloudToCloudStrategy(CloudToCloudStrategy.NATIVE_ONLY)
                .plan();

        DeploymentPlan refreshed = plan.dryRun();

        assertFalse(refreshed.isValid());
        assertEquals(plan.getValidationErrors(), refreshed.getValidationErrors());
    }
}
