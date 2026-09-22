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
import com.eqixiac.equinix.core.enums.MetroCode;
import com.eqixiac.equinix.core.enums.Region;
import com.eqixiac.equinix.core.http.response.PaginatedFilteredList;
import com.eqixiac.equinix.core.http.response.PaginatedList;
import com.eqixiac.equinix.core.model.MetroId;
import com.eqixiac.equinix.design.optimizer.enums.RiskSeverity;
import com.eqixiac.equinix.design.optimizer.enums.WorkloadType;
import com.eqixiac.equinix.design.optimizer.model.MulticloudEnvironmentCatalog;
import com.eqixiac.equinix.design.optimizer.model.OptimizationResult;
import com.eqixiac.equinix.design.optimizer.model.RiskFinding;
import com.eqixiac.equinix.design.value.ratecard.ReferenceRateCard;
import com.eqixiac.equinix.fabric.client.Metros;
import com.eqixiac.equinix.fabric.client.ServiceProfiles;
import com.eqixiac.equinix.fabric.model.Metro;
import com.eqixiac.equinix.fabric.model.ServiceProfile;
import com.eqixiac.equinix.fabric.model.implementation.ConnectedMetro;
import com.eqixiac.equinix.fabric.model.implementation.GeoCoordinate;
import com.eqixiac.equinix.fabric.model.implementation.ServiceProfileMetro;
import com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The informational {@code NATIVE_MULTICLOUD_ALTERNATIVE} finding: raised when a workload depends
 * on two clouds the multicloud environment catalog lists an environment for; never a risk.
 *
 * <p>Fixture: metros DC and DA; an AWS Direct Connect profile and a Google Cloud profile, both
 * offered at DC, with a configurable seller region each.</p>
 */
@DisplayName("MetroOptimizer: NATIVE_MULTICLOUD_ALTERNATIVE finding")
class MetroOptimizerNativeMulticloudFindingTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static FabricGateway gateway(String awsRegion, String gcpRegion) throws Exception {
        Metro dc = metro("DC", "Ashburn", 39.0438, -77.4874, List.of(connectedMetro("DA", 10.0)));
        Metro da = metro("DA", "Dallas", 32.7767, -96.7970, List.of(connectedMetro("DC", 10.0)));
        Metros metros = mock(Metros.class);
        when(metros.list()).thenReturn(new PaginatedList<>(List.of(dc, da), null, null, null, null));

        // Built before the when(...) below: stubbing a mock inside another stubbing call is a Mockito misuse.
        ServiceProfile aws = profile("sp-aws-1", "AWS Direct Connect", awsRegion);
        ServiceProfile gcp = profile("sp-gcp-1", "Google Cloud Partner Interconnect Zone 1", gcpRegion);
        ServiceProfiles serviceProfiles = mock(ServiceProfiles.class);
        when(serviceProfiles.search()).thenReturn(new PaginatedFilteredList<>(List.of(aws, gcp),
                null, null, null, null));

        FabricGateway fabric = mock(FabricGateway.class);
        when(fabric.metros()).thenReturn(metros);
        when(fabric.serviceProfiles()).thenReturn(serviceProfiles);
        return fabric;
    }

    private static List<RiskFinding> findings(OptimizationResult result, String category) {
        return result.getRiskAssessment().getFindings().stream()
                .filter(f -> category.equals(f.getCategory()))
                .collect(Collectors.toList());
    }

    private static MetroOptimizer.Builder twoCloudWorkload(FabricGateway fabric) {
        return MetroOptimizer.builder(fabric)
                .addSite("HQ").nearestMetro(MetroCode.DC).headcount(500).done()
                .addWorkload("Replication").type(WorkloadType.DISASTER_RECOVERY).bandwidthMbps(10_000)
                    .dependsOn(CloudProviderType.AWS)
                    .dependsOn(CloudProviderType.GOOGLE_CLOUD).done()
                .constraints().maxMetros(2).done()
                .rateCard(ReferenceRateCard.standard());
    }

    @Test
    @DisplayName("a two-cloud workload whose seller regions match a catalog pair gets one INFO finding naming that environment")
    void matchedEnvironmentIsNamed() throws Exception {
        OptimizationResult result = twoCloudWorkload(gateway("us-east-1", "us-east4")).optimize();

        List<RiskFinding> alternatives = findings(result, "NATIVE_MULTICLOUD_ALTERNATIVE");
        assertEquals(1, alternatives.size(), () -> String.valueOf(result.getRiskAssessment().getFindings()));
        RiskFinding finding = alternatives.get(0);
        assertEquals(RiskSeverity.INFO, finding.getSeverity());
        assertNull(finding.getAffectedMetro());
        assertTrue(finding.getDescription().contains("Workload 'Replication' depends on Amazon Web Services and "
                + "Google Cloud Platform"), finding.getDescription());
        assertTrue(finding.getDescription().contains("the environment aws us-east-1 <-> gcp us-east4 (GA, as of "
                + "2026-09-21), which matches the regions in this request"), finding.getDescription());
        assertTrue(finding.getDescription().contains("catalog (as of 2026-09-21)"), finding.getDescription());
        assertTrue(finding.getDescription().contains("it does not connect user sites"), finding.getDescription());
        assertTrue(finding.getRecommendation().contains("CloudToCloudStrategy.COMPARE"), finding.getRecommendation());
        assertTrue(finding.getRecommendation().contains("does not evaluate a deployment with none"),
                "the engine makes no zero-metro recommendation: " + finding.getRecommendation());
    }

    @Test
    @DisplayName("the finding is not a risk: same resiliency score and overall severity as with an empty catalog, and metros are still recommended")
    void findingIsNotARisk() throws Exception {
        OptimizationResult with = twoCloudWorkload(gateway("us-east-1", "us-east4")).optimize();
        OptimizationResult without = twoCloudWorkload(gateway("us-east-1", "us-east4"))
                .multicloudEnvironments(MulticloudEnvironmentCatalog.empty()).optimize();

        assertTrue(findings(without, "NATIVE_MULTICLOUD_ALTERNATIVE").isEmpty());
        assertEquals(without.getRiskAssessment().getResiliencyScore(), with.getRiskAssessment().getResiliencyScore());
        assertEquals(without.getRiskAssessment().getOverallSeverity(), with.getRiskAssessment().getOverallSeverity());
        assertEquals(without.getRiskAssessment().getFindings().size() + 1, with.getRiskAssessment().getFindings().size());
        assertEquals(without.getRiskAssessment().getFindings(),
                with.getRiskAssessment().getFindings().subList(0, without.getRiskAssessment().getFindings().size()),
                "the informational finding is appended after every risk finding");
        assertFalse(with.getRecommendations().isEmpty());
        assertEquals(without.getRecommendations().stream().map(r -> r.getMetroId().code()).collect(Collectors.toList()),
                with.getRecommendations().stream().map(r -> r.getMetroId().code()).collect(Collectors.toList()),
                "the finding changes no recommendation");
    }

    @Test
    @DisplayName("when no region matches, the finding lists the catalogued pairs and says none matched")
    void unmatchedRegionsListEveryPair() throws Exception {
        OptimizationResult result = twoCloudWorkload(gateway("us-east-2", "us-central1")).optimize();

        RiskFinding finding = findings(result, "NATIVE_MULTICLOUD_ALTERNATIVE").get(0);
        assertTrue(finding.getDescription().contains("8 environment(s) for the pair, none matched to a region in "
                + "this request"), finding.getDescription());
        assertTrue(finding.getDescription().contains("aws eu-west-2 <-> gcp europe-west2 (GA, as of 2026-09-21)"),
                finding.getDescription());
    }

    @Test
    @DisplayName("a single-cloud workload, or a pair the catalog does not list, raises no finding")
    void noFindingWithoutACataloguedPair() throws Exception {
        OptimizationResult singleCloud = MetroOptimizer.builder(gateway("us-east-1", "us-east4"))
                .addSite("HQ").nearestMetro(MetroCode.DC).headcount(500).done()
                .addWorkload("Web").type(WorkloadType.DISASTER_RECOVERY).bandwidthMbps(1000)
                    .dependsOn(CloudProviderType.AWS).done()
                .rateCard(ReferenceRateCard.standard())
                .optimize();
        assertTrue(findings(singleCloud, "NATIVE_MULTICLOUD_ALTERNATIVE").isEmpty());

        OptimizationResult unlistedPair = MetroOptimizer.builder(gateway("us-east-1", "us-east4"))
                .addSite("HQ").nearestMetro(MetroCode.DC).headcount(500).done()
                .addWorkload("Sync").type(WorkloadType.DISASTER_RECOVERY).bandwidthMbps(1000)
                    .dependsOn(CloudProviderType.GOOGLE_CLOUD)
                    .dependsOn(CloudProviderType.IBM_CLOUD).done()
                .rateCard(ReferenceRateCard.standard())
                .optimize();
        assertTrue(findings(unlistedPair, "NATIVE_MULTICLOUD_ALTERNATIVE").isEmpty());
    }

    @Test
    @DisplayName("the finding is rendered in the Markdown report's risk section at INFO")
    void findingIsRenderedInMarkdown() throws Exception {
        String markdown = twoCloudWorkload(gateway("us-east-1", "us-east4")).optimize().toMarkdown();

        assertTrue(markdown.contains("- **[INFO]** Workload 'Replication' depends on Amazon Web Services and Google "
                + "Cloud Platform."), markdown);
        assertTrue(markdown.contains("_Recommendation: Plan with the Deployment Wizard's default "
                + "CloudToCloudStrategy.COMPARE"), markdown);
    }

    // ── mock helpers (same shapes as MetroOptimizerPlacementDependencyTest) ──

    private static ServiceProfile profile(String uuid, String name, String sellerRegion) throws Exception {
        ServiceProfile profile = mock(ServiceProfile.class);
        lenient().when(profile.getUuid()).thenReturn(uuid);
        lenient().when(profile.getName()).thenReturn(name);
        lenient().when(profile.metros()).thenReturn(List.of(MAPPER.readValue("{\"code\":\"DC\",\"name\":\"DC\","
                + "\"sellerRegions\":{\"" + sellerRegion + "\":\"" + sellerRegion + "\"}}", ServiceProfileMetro.class)));
        return profile;
    }

    private static Metro metro(String code, String name, double lat, double lon,
                               List<ConnectedMetro> connected) throws Exception {
        Metro m = mock(Metro.class);
        lenient().when(m.metroId()).thenReturn(MetroId.of(code));
        lenient().when(m.getCode()).thenReturn(MetroCode.fromCode(code));
        lenient().when(m.getName()).thenReturn(name);
        lenient().when(m.getRegion()).thenReturn(Region.AMER);
        lenient().when(m.geoCoordinates()).thenReturn(MAPPER.readValue(
                "{\"latitude\":" + lat + ",\"longitude\":" + lon + "}", GeoCoordinate.class));
        lenient().when(m.getConnectedMetros()).thenReturn(connected);
        return m;
    }

    private static ConnectedMetro connectedMetro(String code, double avgLatency) throws Exception {
        return MAPPER.readValue("{\"code\":\"" + code + "\",\"avgLatency\":" + avgLatency + "}", ConnectedMetro.class);
    }
}
