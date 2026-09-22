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

package com.eqixiac.equinix.design.value;

import com.eqixiac.equinix.core.enums.MetroCode;
import com.eqixiac.equinix.design.value.ratecard.ReferenceRateCard;
import com.eqixiac.equinix.design.value.savings.DataUnit;
import com.eqixiac.equinix.design.value.savings.SavingsCalculator;
import com.eqixiac.equinix.design.value.savings.SavingsEstimate;
import com.eqixiac.equinix.design.value.tco.CostBreakdown;
import com.eqixiac.equinix.design.value.tco.DeploymentArchetype;
import com.eqixiac.equinix.design.value.tco.TcoCalculator;
import com.eqixiac.equinix.design.value.tco.TcoComparison;
import com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression lock: a comparison or estimate with NO peer cloud produces exactly the output it
 * produced before the cloud-to-cloud levers existed.
 *
 * <p>The two expected reports were captured from the build of commit {@code 3f8fc655} (the last
 * commit before the native multicloud path was added) by running the same scenario against its
 * classes, and were byte-identical to the output of the changed classes. Scenario: 100 TB per
 * month from AWS us-east-1, metro DC, 10 Gbps, the bundled reference card.</p>
 */
class SingleCloudOutputUnchangedTest {

    private static final String TCO_REPORT = String.join("\n",
            "## Total Cost of Ownership — Deployment Comparison",
            "",
            "**Term:** 12 months (archetypes are ranked by total cost over the term, including one-time setup)",
            "",
            "| Approach | Monthly | One-time | Total over term | |",
            "|---|---|---|---|---|",
            "| Public cloud over internet | USD 9000.00 | USD 0.00 | USD 108000.00 |  |",
            "| On-premises / self-managed | USD 9025.00 | USD 0.00 | USD 108300.00 |  |",
            "| Equinix interconnected | USD 4292.50 | USD 0.00 | USD 51510.00 | ✅ recommended |",
            "",
            "**Recommended:** Equinix interconnected",
            "",
            "- Monthly saving vs. Public cloud over internet: USD 4707.50",
            "- Annual saving vs. Public cloud over internet: USD 56490.00",
            "- Saving over the 12-month term vs. Public cloud over internet: USD 56490.00",
            "",
            "_Design-time TCO estimate, not a quote. Equinix Fabric connection costs use live pricing where "
                    + "available; cloud-egress, cloud-provider interconnect-port, cross-connect, and on-prem figures "
                    + "are indicative reference midpoints (the on-prem inputs are coarse and overridable). Compute, "
                    + "storage, software, staffing, and per-provider free-tier egress allowances are out of scope. "
                    + "Actual costs depend on region, volume, tiering, and contract terms. (reference data as of "
                    + "2026-06)_",
            "");

    private static final String SAVINGS_REPORT = String.join("\n",
            "## Egress Savings Estimate",
            "",
            "**Workload:** 100000 GB/mo egress from AWS (us-east-1) via metro DC",
            "",
            "| Line item | Monthly |",
            "|---|---|",
            "| Egress over public internet | USD 9000.00 |",
            "| Egress over private interconnect | USD 2000.00 |",
            "| **Egress saving** | **USD 7000.00** |",
            "| Equinix interconnect (recurring) | −USD 350.00 |",
            "| **Net monthly saving** | **USD 6650.00** |",
            "",
            "- One-time Equinix setup: USD 0.00",
            "- Annual net saving (steady state): USD 79800.00",
            "- First-year net saving (incl. setup): USD 79800.00",
            "- Break-even egress volume: 5000 GB/mo",
            "",
            "_Design-time estimate, not a quote. Equinix interconnect costs use live Fabric pricing where "
                    + "available; egress rates are indicative reference or caller-supplied figures. Actual costs "
                    + "depend on region, tiering, volume, and contract terms. Excludes per-provider free-tier egress "
                    + "allowances and compute/storage costs._",
            "");

    private static TcoComparison tco() {
        return TcoCalculator.builder(null)
                .egress(100, DataUnit.TERABYTE)
                .fromCloud(CloudProviderType.AWS).inRegion("us-east-1")
                .viaMetro(MetroCode.DC).bandwidthMbps(10_000)
                .rateCard(ReferenceRateCard.standard())
                .compare();
    }

    private static SavingsEstimate savings() {
        return SavingsCalculator.builder(null)
                .egress(100, DataUnit.TERABYTE)
                .fromCloud(CloudProviderType.AWS).inRegion("us-east-1")
                .viaMetro(MetroCode.DC).bandwidthMbps(10_000)
                .rateCard(ReferenceRateCard.standard())
                .calculate();
    }

    @Test
    void tcoReportWithoutAPeerCloudIsByteForByteTheEarlierReport() {
        assertEquals(TCO_REPORT, tco().toMarkdown());
    }

    @Test
    void savingsReportWithoutAPeerCloudIsByteForByteTheEarlierReport() {
        assertEquals(SAVINGS_REPORT, savings().toMarkdown());
    }

    @Test
    void tcoWithoutAPeerCloudComparesTheThreeSingleCloudArchetypesOnly() {
        TcoComparison tco = tco();

        assertEquals(List.of(DeploymentArchetype.PUBLIC_CLOUD_INTERNET, DeploymentArchetype.ON_PREM,
                        DeploymentArchetype.EQUINIX_INTERCONNECT),
                tco.getBreakdowns().stream().map(CostBreakdown::getArchetype).collect(Collectors.toList()),
                "the native archetype joins the default set only when toCloud(...) is set");
        assertNull(tco.getTrafficNote());
        for (CostBreakdown breakdown : tco.getBreakdowns()) {
            assertNull(breakdown.getProvenance(), breakdown.getArchetype() + " carries no provenance list");
        }
        CostBreakdown equinix = tco.breakdown(DeploymentArchetype.EQUINIX_INTERCONNECT).orElseThrow();
        assertEquals(List.of("Cloud egress (private interconnect)", "Equinix Fabric connection",
                        "Cloud provider interconnect port", "Equinix cross-connect"),
                List.copyOf(equinix.getLineItems().keySet()),
                "single-cloud line-item labels and order are unchanged");
    }

    @Test
    void savingsWithoutAPeerCloudCarriesNoCloudToCloudSection() {
        SavingsEstimate estimate = savings();

        assertNull(estimate.getMulticloudComparison());
        assertTrue(estimate.breakEvenSustainedMbps().isEmpty());
        assertTrue(estimate.isComplete());
    }
}
