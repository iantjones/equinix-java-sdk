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

package com.eqixiac.equinix.design.resilience;

import com.eqixiac.equinix.core.enums.Region;
import com.eqixiac.equinix.design.spec.CloudIntent;
import com.eqixiac.equinix.design.spec.DesignSpec;
import com.eqixiac.equinix.design.spec.SiteIntent;
import com.eqixiac.equinix.design.spec.WorkloadIntent;
import com.eqixiac.equinix.design.spi.FakeDesignContext;
import com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType;

import java.util.List;

/**
 * A small two-region world for the resilience tests. Round trips are Fabric's live figures where
 * Fabric publishes them (DC-NY 6.3 ms, DC-LD 73.4 ms, 2026-11-11) and plain test values otherwise.
 *
 * <ul>
 *   <li>Sites: HQ in DC (weight 3), Branch in NY (weight 1).</li>
 *   <li>Clouds: aws (AWS us-east-1) and azure (AZURE eastus), both required.</li>
 *   <li>Workloads: api (1000 Mbps, aws, max 20 ms) and batch (500 Mbps, aws + azure).</li>
 *   <li>Metros: DC, NY (AMER, zone US), LD (EMEA, zone UK); aws and azure reachable in DC and NY, aws in LD.</li>
 * </ul>
 * On the portfolio DC+NY both workloads run in DC with a standby in NY (DC has the lower site-weighted
 * round trip), so the cloud legs are DC-aws, DC-azure (primary) and NY-aws, NY-azure (standby).
 */
final class ResilienceFixtures {

    private ResilienceFixtures() {
    }

    static DesignSpec.DesignSpecBuilder specBuilder() {
        return DesignSpec.builder().name("two-region")
                .site(SiteIntent.builder().label("HQ").metro("DC").weight(3.0).build())
                .site(SiteIntent.builder().label("Branch").metro("NY").weight(1.0).build())
                .cloud(CloudIntent.builder().id("aws").provider(CloudProviderType.AWS).region("us-east-1").build())
                .cloud(CloudIntent.builder().id("azure").provider(CloudProviderType.AZURE).region("eastus").build())
                .workload(WorkloadIntent.builder().label("api").bandwidthMbps(1000).maxLatencyMs(20.0)
                        .clouds(List.of("aws")).build())
                .workload(WorkloadIntent.builder().label("batch").bandwidthMbps(500)
                        .clouds(List.of("aws", "azure")).build());
    }

    static DesignSpec spec() {
        return specBuilder().build();
    }

    static FakeDesignContext context(DesignSpec spec) {
        return new FakeDesignContext(spec)
                .metro("DC", Region.AMER, "US").metro("NY", Region.AMER, "US").metro("LD", Region.EMEA, "UK")
                .latency("DC", "HQ", 0.5).latency("NY", "HQ", 6.3).latency("LD", "HQ", 73.4)
                .latency("DC", "Branch", 6.3).latency("NY", "Branch", 0.5).latency("LD", "Branch", 70.0)
                .metroLatency("DC", "NY", 6.3).metroLatency("DC", "LD", 73.4).metroLatency("NY", "LD", 70.0)
                .reachable("DC", "aws", "azure").reachable("NY", "aws", "azure").reachable("LD", "aws");
    }

    static FakeDesignContext context() {
        return context(spec());
    }
}
