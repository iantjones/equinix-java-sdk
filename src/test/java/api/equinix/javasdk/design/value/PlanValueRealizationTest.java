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

package api.equinix.javasdk.design.value;

import api.equinix.javasdk.design.optimizer.wizard.model.DeploymentPlan;
import api.equinix.javasdk.design.optimizer.wizard.model.PlanPricing;
import api.equinix.javasdk.design.optimizer.wizard.model.PlanValueRealization;
import api.equinix.javasdk.design.value.savings.DataUnit;
import api.equinix.javasdk.fabric.model.implementation.cloud.CloudProviderType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests {@link DeploymentPlan#valueRealization()}: egress savings (from the bundled
 * reference card) netted against the plan's actual interconnect cost.
 */
class PlanValueRealizationTest {

    private DeploymentPlan planCosting(String monthly, String setup) {
        return DeploymentPlan.builder()
                .pricing(PlanPricing.builder()
                        .monthlyTotal(new BigDecimal(monthly))
                        .setupTotal(new BigDecimal(setup))
                        .currency("USD")
                        .build())
                .fabric(null)
                .build();
    }

    @Test
    void netsEgressSavingsAgainstPlanCost() {
        // Plan interconnect cost 700/mo, 1000 setup; 50 TB AWS egress.
        PlanValueRealization vr = planCosting("700", "1000").valueRealization()
                .egress(CloudProviderType.AWS, 50, DataUnit.TERABYTE)
                .assess();

        // egress saving = (0.09 - 0.02) * 50,000 = 3500
        assertEquals(0, new BigDecimal("3500").compareTo(vr.getTotalMonthlyEgressSavings()));
        assertEquals(0, new BigDecimal("700").compareTo(vr.getPlanMonthlyCost()));
        assertEquals(0, new BigDecimal("2800").compareTo(vr.getNetMonthlySavings()), "3500 − 700");
        assertEquals(0, new BigDecimal("33600").compareTo(vr.getAnnualNetSavings()));
        assertEquals(0, new BigDecimal("32600").compareTo(vr.getFirstYearNetSavings()), "annual − setup");
        assertEquals(1, vr.getPerProvider().size());
        assertTrue(vr.getPerProvider().get(0).isPriced());
        assertNotNull(vr.toMarkdown());
    }

    @Test
    void aggregatesAcrossMultipleProviders() {
        // GCP reference rates are the per-GB conversions of Google's per-GiB list prices
        // (0.1118 internet / 0.0186 interconnect), so: (0.1118 − 0.0186) × 10,000 = 932.
        PlanValueRealization vr = planCosting("1000", "0").valueRealization()
                .egress(CloudProviderType.AWS, 50, DataUnit.TERABYTE)      // saving 3500
                .egress(CloudProviderType.GOOGLE_CLOUD, 10, DataUnit.TERABYTE) // saving 932
                .assess();

        assertEquals(0, new BigDecimal("4432").compareTo(vr.getTotalMonthlyEgressSavings()), "3500 + 932");
        assertEquals(0, new BigDecimal("3432").compareTo(vr.getNetMonthlySavings()), "4432 − 1000 plan cost");
        assertEquals(2, vr.getPerProvider().size());
    }

    @Test
    void unpricedProviderContributesNothingAndRendersNa() {
        PlanValueRealization vr = planCosting("700", "1000").valueRealization()
                .egress(CloudProviderType.AWS, 50, DataUnit.TERABYTE)            // priced: saving 3500
                .egress(CloudProviderType.ORACLE_CLOUD, 100, DataUnit.TERABYTE)  // no reference egress rate
                .assess();

        assertEquals(2, vr.getPerProvider().size());
        var oracle = vr.getPerProvider().stream()
                .filter(p -> p.getProvider() == CloudProviderType.ORACLE_CLOUD).findFirst().orElseThrow();
        assertFalse(oracle.isPriced());
        assertEquals(0, BigDecimal.ZERO.compareTo(oracle.getMonthlySavings()));
        assertEquals(0, new BigDecimal("3500").compareTo(vr.getTotalMonthlyEgressSavings()),
                "unpriced provider neither inflates nor zeroes the total");
        assertEquals(0, new BigDecimal("2800").compareTo(vr.getNetMonthlySavings()));
        assertTrue(vr.toMarkdown().contains("n/a"));
    }
}
