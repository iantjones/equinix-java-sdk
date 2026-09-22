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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Regression: a plan built with {@link CloudToCloudStrategy#EQUINIX_ONLY} renders exactly what the
 * wizard rendered before the cloud-to-cloud lever existed.
 *
 * <p>The files under {@code src/test/resources/golden/wizard/} were written by
 * {@code MulticloudWizardFixtures.renderEquinixSide(plan)} running against the wizard as of commit
 * {@code 3f8fc655}, which has no {@code cloudToCloudStrategy}, for the two fixtures below. They
 * hold the summary, the Markdown report, every planned field that reaches a Fabric request body,
 * the pricing figures and the three validation buckets. Line endings are normalized on read
 * because the repository checks text out with CRLF on Windows.</p>
 */
@DisplayName("EQUINIX_ONLY output is identical to the pre-change wizard")
class EquinixOnlyOutputUnchangedTest {

    private static DeploymentPlan plan(OptimizationResult result, CloudToCloudStrategy strategy) {
        DeploymentWizard.Builder builder = DeploymentWizard.builder(null, result)
                .notifications("noc@example.com")
                .rateCard(MulticloudWizardFixtures.flatRateCard());
        if (strategy != null) {
            builder.cloudToCloudStrategy(strategy);
        }
        return builder.plan();
    }

    private static String golden(String name) throws IOException {
        try (InputStream in = EquinixOnlyOutputUnchangedTest.class.getResourceAsStream("/golden/wizard/" + name)) {
            assertNotNull(in, "missing golden resource " + name);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).replace("\r\n", "\n");
        }
    }

    private static void assertNoNativeTrace(DeploymentPlan plan) {
        assertNull(plan.getMulticloudLinks());
        assertNull(plan.getPricing().getNativeAlternativeMonthlyCost());
        assertNull(plan.getPricing().getNativeReplacementMonthlyCost());
        assertNull(plan.getPricing().getPerMulticloudLinkCost());
        assertNull(plan.getPricing().getUnpricedMulticloudLinks());
        assertNull(plan.getPricing().getNativeMulticloudDisclaimer());
    }

    @Test
    @DisplayName("two-cloud fixture (AWS us-east-1 + Google Cloud us-east4 at DC, a catalogued GA pair): EQUINIX_ONLY matches the golden")
    void twoCloudFixtureUnchanged() throws IOException {
        DeploymentPlan plan = plan(MulticloudWizardFixtures.awsGcpAtDc(10_000), CloudToCloudStrategy.EQUINIX_ONLY);

        assertEquals(golden("equinix_only_aws_gcp.txt"), MulticloudWizardFixtures.renderEquinixSide(plan));
        assertNoNativeTrace(plan);
    }

    @Test
    @DisplayName("three-metro single-cloud fixture: EQUINIX_ONLY matches the golden")
    void threeMetroFixtureUnchanged() throws IOException {
        DeploymentPlan plan = plan(MulticloudWizardFixtures.threeMetroSingleCloudResult(),
                CloudToCloudStrategy.EQUINIX_ONLY);

        assertEquals(golden("equinix_only_three_metro.txt"), MulticloudWizardFixtures.renderEquinixSide(plan));
        assertNoNativeTrace(plan);
    }

    @Test
    @DisplayName("three-metro single-cloud fixture: the default strategy (COMPARE) also matches, because no workload depends on two clouds")
    void defaultStrategyLeavesSingleCloudPlansUnchanged() throws IOException {
        DeploymentPlan plan = plan(MulticloudWizardFixtures.threeMetroSingleCloudResult(), null);

        assertEquals(golden("equinix_only_three_metro.txt"), MulticloudWizardFixtures.renderEquinixSide(plan));
        assertNoNativeTrace(plan);
    }
}
