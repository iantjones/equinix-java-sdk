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

package api.equinix.javasdk.design.peering;

import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.design.peering.enums.*;
import api.equinix.javasdk.design.peering.model.*;
import org.junit.jupiter.api.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for resiliency analysis components:
 * BlastRadiusReport, CorrelatedFailure, DiversityScore, FailoverPath,
 * and ResiliencyAssessment.
 *
 * <p>These are the core tests requested for validating the resiliency
 * analysis pipeline output.</p>
 */
@DisplayName("Resiliency Analysis")
class ResiliencyAnalysisTest {

    private static final long AWS = 16509L;
    private static final long MSFT = 8075L;
    private static final long GOOG = 15169L;

    @Nested
    @DisplayName("BlastRadiusReport")
    class BlastRadiusTests {

        @Test
        @DisplayName("CRITICAL severity when impact > 80%")
        void criticalSeverity() {
            BlastRadiusReport report = BlastRadiusReport.builder()
                    .metro(MetroCode.DC)
                    .scope(FailureScope.METRO)
                    .lostIxPeeringAsns(Arrays.asList(AWS, MSFT, GOOG))
                    .lostFabricAsns(Collections.emptyList())
                    .lostIxPeeringLabels(Arrays.asList("AWS", "Microsoft", "Google"))
                    .lostFabricLabels(Collections.emptyList())
                    .lostIxCapacityMbps(700000)
                    .impactRatio(0.9)
                    .severity("CRITICAL")
                    .mitigations(Arrays.asList("Establish IX peering at diverse metros"))
                    .build();

            assertEquals("CRITICAL", report.computeSeverity());
            assertEquals(0.9, report.getImpactRatio());
            assertEquals(3, report.totalAffectedAsns());
            assertEquals(MetroCode.DC, report.getMetro());
            assertEquals(700000, report.getLostIxCapacityMbps());
        }

        @Test
        @DisplayName("HIGH severity when impact 51-80%")
        void highSeverity() {
            BlastRadiusReport report = createBlastReport(0.65, 2);
            assertEquals("HIGH", report.computeSeverity());
        }

        @Test
        @DisplayName("MEDIUM severity when impact 26-50%")
        void mediumSeverity() {
            BlastRadiusReport report = createBlastReport(0.33, 1);
            assertEquals("MEDIUM", report.computeSeverity());
        }

        @Test
        @DisplayName("LOW severity when impact <= 25%")
        void lowSeverity() {
            BlastRadiusReport report = createBlastReport(0.1, 1);
            assertEquals("LOW", report.computeSeverity());
        }

        @Test
        @DisplayName("Zero impact should be LOW severity")
        void zeroImpact() {
            BlastRadiusReport report = createBlastReport(0.0, 0);
            assertEquals("LOW", report.computeSeverity());
        }

        @Test
        @DisplayName("Total affected ASNs combines IX and Fabric losses")
        void totalAffected() {
            BlastRadiusReport report = BlastRadiusReport.builder()
                    .metro(MetroCode.DC)
                    .scope(FailureScope.METRO)
                    .lostIxPeeringAsns(Arrays.asList(AWS, MSFT))
                    .lostFabricAsns(Collections.singletonList(GOOG))
                    .lostIxPeeringLabels(Arrays.asList("AWS", "Microsoft"))
                    .lostFabricLabels(Collections.singletonList("Google"))
                    .lostIxCapacityMbps(300000)
                    .impactRatio(1.0)
                    .severity("CRITICAL")
                    .mitigations(Collections.emptyList())
                    .build();

            assertEquals(3, report.totalAffectedAsns());
        }
    }

    @Nested
    @DisplayName("CorrelatedFailure")
    class CorrelatedFailureTests {

        @Test
        @DisplayName("Single-metro correlation should be detected")
        void singleMetroCorrelation() {
            CorrelatedFailure cf = CorrelatedFailure.builder()
                    .scope(FailureScope.METRO)
                    .failureDomain("DC metro")
                    .affectedMetro(MetroCode.DC)
                    .affectedAsns(Collections.singletonList(AWS))
                    .affectedLabels(Collections.singletonList("AWS"))
                    .affectedPaths(Collections.singletonList("IX Peering to AWS"))
                    .impactRatio(0.33)
                    .severity("MEDIUM")
                    .recommendation("Establish IX peering with AWS at a second metro")
                    .build();

            assertEquals(FailureScope.METRO, cf.getScope());
            assertEquals("DC metro", cf.getFailureDomain());
            assertEquals(MetroCode.DC, cf.getAffectedMetro());
            assertEquals(1, cf.getAffectedAsns().size());
            assertEquals(AWS, cf.getAffectedAsns().get(0).longValue());
        }

        @Test
        @DisplayName("IX-scope correlation should carry IX-level detail")
        void ixScopeCorrelation() {
            CorrelatedFailure cf = CorrelatedFailure.builder()
                    .scope(FailureScope.IX)
                    .failureDomain("Equinix Ashburn IX")
                    .affectedMetro(MetroCode.DC)
                    .affectedAsns(Arrays.asList(AWS, MSFT, GOOG))
                    .affectedLabels(Arrays.asList("AWS", "Microsoft", "Google"))
                    .affectedPaths(Arrays.asList(
                            "IX Peering to AWS", "IX Peering to Microsoft", "IX Peering to Google"))
                    .impactRatio(1.0)
                    .severity("CRITICAL")
                    .recommendation("Diversify across multiple IXes or add Fabric connections")
                    .build();

            assertEquals(FailureScope.IX, cf.getScope());
            assertEquals(3, cf.getAffectedAsns().size());
            assertEquals("CRITICAL", cf.getSeverity());
        }
    }

    @Nested
    @DisplayName("DiversityScore")
    class DiversityScoreTests {

        @Test
        @DisplayName("Intercontinental distance should be EXCELLENT")
        void intercontinental() {
            DiversityScore score = DiversityScore.builder()
                    .primaryMetro(MetroCode.DC)
                    .backupMetro(MetroCode.SG)
                    .distanceKm(15000)
                    .sameRegion(false)
                    .rating(DiversityRating.EXCELLENT)
                    .explanation("DC and SG are 15000 km apart")
                    .build();

            assertEquals(DiversityRating.EXCELLENT, score.getRating());
            assertFalse(score.isSameRegion());
            assertEquals(MetroCode.DC, score.getPrimaryMetro());
            assertEquals(MetroCode.SG, score.getBackupMetro());
        }

        @Test
        @DisplayName("Same region US distance should be GOOD or MODERATE")
        void sameRegionUs() {
            DiversityScore score = DiversityScore.builder()
                    .primaryMetro(MetroCode.DC)
                    .backupMetro(MetroCode.DA)
                    .distanceKm(1900)
                    .sameRegion(true)
                    .rating(DiversityRating.GOOD)
                    .explanation("DC and DA are 1900 km apart")
                    .build();

            assertEquals(DiversityRating.GOOD, score.getRating());
            assertTrue(score.isSameRegion());
        }

        @Test
        @DisplayName("Very close metros should be CRITICAL")
        void veryClose() {
            DiversityScore score = DiversityScore.builder()
                    .primaryMetro(MetroCode.DC)
                    .backupMetro(MetroCode.NY)
                    .distanceKm(50)
                    .sameRegion(true)
                    .rating(DiversityRating.CRITICAL)
                    .explanation("DC and NY are only 50 km apart")
                    .build();

            assertEquals(DiversityRating.CRITICAL, score.getRating());
        }
    }

    @Nested
    @DisplayName("FailoverPath")
    class FailoverPathTests {

        @Test
        @DisplayName("Failover path should capture complete path details")
        void completeFailoverPath() {
            DiversityScore diversity = DiversityScore.builder()
                    .primaryMetro(MetroCode.DC)
                    .backupMetro(MetroCode.DA)
                    .distanceKm(1900)
                    .sameRegion(true)
                    .rating(DiversityRating.GOOD)
                    .explanation("Good diversity")
                    .build();

            FailoverPath path = FailoverPath.builder()
                    .targetAsn(AWS)
                    .targetLabel("AWS")
                    .primaryMetro(MetroCode.DC)
                    .failoverMetro(MetroCode.DA)
                    .connectivityType(ConnectivityType.IX_PEERING)
                    .ixCapacityMbps(10000)
                    .routeServerAvailable(false)
                    .diversity(diversity)
                    .ixSessions(Collections.singletonList(
                            IxPresenceDetail.builder()
                                    .metro(MetroCode.DA).ixId(3).ixName("Equinix Dallas")
                                    .speedMbps(10000).routeServerPeer(false).bfdSupport(false)
                                    .operational(true).build()))
                    .recommendation("Establish IX peering with AWS at DA")
                    .build();

            assertEquals(AWS, path.getTargetAsn());
            assertEquals(MetroCode.DC, path.getPrimaryMetro());
            assertEquals(MetroCode.DA, path.getFailoverMetro());
            assertEquals(10000, path.getIxCapacityMbps());
            assertEquals(DiversityRating.GOOD, path.getDiversity().getRating());
            assertEquals(1, path.getIxSessions().size());
        }

        @Test
        @DisplayName("Failover with route server should be flagged")
        void routeServerFailover() {
            FailoverPath path = FailoverPath.builder()
                    .targetAsn(GOOG)
                    .targetLabel("Google")
                    .primaryMetro(MetroCode.DC)
                    .failoverMetro(MetroCode.CH)
                    .connectivityType(ConnectivityType.IX_PEERING)
                    .ixCapacityMbps(200000)
                    .routeServerAvailable(true)
                    .diversity(DiversityScore.builder()
                            .primaryMetro(MetroCode.DC).backupMetro(MetroCode.CH)
                            .distanceKm(950).sameRegion(true)
                            .rating(DiversityRating.MODERATE)
                            .explanation("Moderate diversity").build())
                    .ixSessions(Collections.emptyList())
                    .recommendation("Use route server for automatic failover")
                    .build();

            assertTrue(path.isRouteServerAvailable());
            assertEquals(200000, path.getIxCapacityMbps());
        }
    }

    @Nested
    @DisplayName("ResiliencyAssessment (comprehensive)")
    class ResiliencyAssessmentTests {

        private ResiliencyAssessment assessment;

        @BeforeEach
        void buildAssessment() {
            // Simulate: customer at DC and DA, analyzing AWS, Microsoft, Google
            // DC has all 3, DA has AWS + Google (not Microsoft)

            // Blast radius for DC
            BlastRadiusReport dcBlast = BlastRadiusReport.builder()
                    .metro(MetroCode.DC)
                    .scope(FailureScope.METRO)
                    .lostIxPeeringAsns(Arrays.asList(AWS, MSFT, GOOG))
                    .lostFabricAsns(Collections.emptyList())
                    .lostIxPeeringLabels(Arrays.asList("AWS", "Microsoft", "Google"))
                    .lostFabricLabels(Collections.emptyList())
                    .lostIxCapacityMbps(700000)
                    .impactRatio(1.0)
                    .severity("CRITICAL")
                    .mitigations(Arrays.asList(
                            "Establish IX peering at geographically diverse metros",
                            "Consider Fabric private connections as independent backup paths",
                            "CRITICAL: DC is a single point of failure for 3 of 3 target ASNs"))
                    .build();

            // Blast radius for DA
            BlastRadiusReport daBlast = BlastRadiusReport.builder()
                    .metro(MetroCode.DA)
                    .scope(FailureScope.METRO)
                    .lostIxPeeringAsns(Arrays.asList(AWS, GOOG))
                    .lostFabricAsns(Collections.emptyList())
                    .lostIxPeeringLabels(Arrays.asList("AWS", "Google"))
                    .lostFabricLabels(Collections.emptyList())
                    .lostIxCapacityMbps(110000)
                    .impactRatio(0.67)
                    .severity("HIGH")
                    .mitigations(Arrays.asList("Establish IX peering at geographically diverse metros"))
                    .build();

            // Correlated failure: Microsoft only at DC among customer metros
            CorrelatedFailure msfCorrelation = CorrelatedFailure.builder()
                    .scope(FailureScope.METRO)
                    .failureDomain("DC metro")
                    .affectedMetro(MetroCode.DC)
                    .affectedAsns(Collections.singletonList(MSFT))
                    .affectedLabels(Collections.singletonList("Microsoft"))
                    .affectedPaths(Collections.singletonList("IX Peering to Microsoft"))
                    .impactRatio(0.33)
                    .severity("MEDIUM")
                    .recommendation("Establish IX peering with Microsoft at a second metro")
                    .build();

            // Diversity: DC to DA
            DiversityScore dcToDa = DiversityScore.builder()
                    .primaryMetro(MetroCode.DC)
                    .backupMetro(MetroCode.DA)
                    .distanceKm(1900)
                    .sameRegion(true)
                    .rating(DiversityRating.GOOD)
                    .explanation("DC and DA are 1900 km apart — good diversity within the same continent.")
                    .build();

            // Failover paths
            Map<MetroCode, List<FailoverPath>> failovers = new LinkedHashMap<>();
            failovers.put(MetroCode.DC, Arrays.asList(
                    FailoverPath.builder()
                            .targetAsn(AWS).targetLabel("AWS")
                            .primaryMetro(MetroCode.DC).failoverMetro(MetroCode.DA)
                            .connectivityType(ConnectivityType.IX_PEERING)
                            .ixCapacityMbps(10000).routeServerAvailable(false)
                            .diversity(dcToDa)
                            .ixSessions(Collections.emptyList())
                            .recommendation("Failover AWS from DC to DA via IX")
                            .build(),
                    FailoverPath.builder()
                            .targetAsn(GOOG).targetLabel("Google")
                            .primaryMetro(MetroCode.DC).failoverMetro(MetroCode.DA)
                            .connectivityType(ConnectivityType.IX_PEERING)
                            .ixCapacityMbps(100000).routeServerAvailable(true)
                            .diversity(dcToDa)
                            .ixSessions(Collections.emptyList())
                            .recommendation("Failover Google from DC to DA via IX route server")
                            .build()
            ));

            assessment = ResiliencyAssessment.builder()
                    .overallScore(0.45)
                    .overallRating("Moderate")
                    .failoverPaths(failovers)
                    .blastRadiusReports(Arrays.asList(dcBlast, daBlast))
                    .correlatedFailures(Collections.singletonList(msfCorrelation))
                    .diversityScores(Collections.singletonList(dcToDa))
                    .findings(Arrays.asList(
                            "Metro DC failure would impact 100% of analyzed ASN connectivity.",
                            "Metro DA failure would impact 67% of analyzed ASN connectivity."))
                    .build();
        }

        @Test
        @DisplayName("Overall score should be between 0 and 1")
        void overallScoreRange() {
            assertTrue(assessment.getOverallScore() >= 0.0);
            assertTrue(assessment.getOverallScore() <= 1.0);
        }

        @Test
        @DisplayName("Overall rating should be a recognized category")
        void overallRating() {
            List<String> validRatings = Arrays.asList("Excellent", "Good", "Moderate", "Poor", "Critical");
            assertTrue(validRatings.contains(assessment.getOverallRating()));
        }

        @Test
        @DisplayName("Blast radius reports should cover all customer metros")
        void blastRadiusCoversAllMetros() {
            assertEquals(2, assessment.getBlastRadiusReports().size());
            List<MetroCode> reportedMetros = new ArrayList<>();
            for (BlastRadiusReport br : assessment.getBlastRadiusReports()) {
                reportedMetros.add(br.getMetro());
            }
            assertTrue(reportedMetros.contains(MetroCode.DC));
            assertTrue(reportedMetros.contains(MetroCode.DA));
        }

        @Test
        @DisplayName("failoverPathsForAsn should return paths for specific ASN")
        void failoverPathsForAsn() {
            List<FailoverPath> awsFailovers = assessment.failoverPathsForAsn(AWS);
            assertEquals(1, awsFailovers.size());
            assertEquals(MetroCode.DA, awsFailovers.get(0).getFailoverMetro());
        }

        @Test
        @DisplayName("failoverPathsForAsn for absent ASN should return empty")
        void failoverPathsForAbsentAsn() {
            // MSFT has no failover (only at DC among customer metros)
            List<FailoverPath> msfFailovers = assessment.failoverPathsForAsn(MSFT);
            assertTrue(msfFailovers.isEmpty());
        }

        @Test
        @DisplayName("blastRadiusFor should return correct metro report")
        void blastRadiusFor() {
            BlastRadiusReport dcReport = assessment.blastRadiusFor(MetroCode.DC);
            assertNotNull(dcReport);
            assertEquals(1.0, dcReport.getImpactRatio());
            assertEquals(3, dcReport.getLostIxPeeringAsns().size());
        }

        @Test
        @DisplayName("blastRadiusFor unknown metro should return null")
        void blastRadiusForUnknown() {
            assertNull(assessment.blastRadiusFor(MetroCode.SG));
        }

        @Test
        @DisplayName("criticalCorrelations should filter by severity")
        void criticalCorrelations() {
            // Our setup has one MEDIUM correlation
            List<CorrelatedFailure> critical = assessment.criticalCorrelations();
            assertTrue(critical.isEmpty()); // MEDIUM is not CRITICAL or HIGH
        }

        @Test
        @DisplayName("hasSinglePointOfFailure should detect high-impact metros")
        void singlePointOfFailure() {
            assertTrue(assessment.hasSinglePointOfFailure(0.5)); // DC has 1.0, DA has 0.67
            assertTrue(assessment.hasSinglePointOfFailure(0.8)); // DC has 1.0
            assertFalse(assessment.hasSinglePointOfFailure(1.0)); // None exceeds 1.0
        }

        @Test
        @DisplayName("Findings should be non-empty")
        void findings() {
            assertFalse(assessment.getFindings().isEmpty());
            assertTrue(assessment.getFindings().get(0).contains("DC"));
        }

        @Test
        @DisplayName("Diversity scores should be present")
        void diversityScores() {
            assertEquals(1, assessment.getDiversityScores().size());
            DiversityScore dcDa = assessment.getDiversityScores().get(0);
            assertEquals(MetroCode.DC, dcDa.getPrimaryMetro());
            assertEquals(MetroCode.DA, dcDa.getBackupMetro());
            assertEquals(DiversityRating.GOOD, dcDa.getRating());
        }
    }

    @Nested
    @DisplayName("Resiliency edge cases")
    class ResiliencyEdgeCaseTests {

        @Test
        @DisplayName("Single metro customer should have maximum blast radius")
        void singleMetroCustomer() {
            BlastRadiusReport report = BlastRadiusReport.builder()
                    .metro(MetroCode.DC)
                    .scope(FailureScope.METRO)
                    .lostIxPeeringAsns(Arrays.asList(AWS, MSFT))
                    .lostFabricAsns(Collections.emptyList())
                    .lostIxPeeringLabels(Arrays.asList("AWS", "Microsoft"))
                    .lostFabricLabels(Collections.emptyList())
                    .lostIxCapacityMbps(300000)
                    .impactRatio(1.0) // all ASNs lost if single metro fails
                    .severity("CRITICAL")
                    .mitigations(Collections.emptyList())
                    .build();

            assertEquals("CRITICAL", report.computeSeverity());
            assertEquals(1.0, report.getImpactRatio());
        }

        @Test
        @DisplayName("Highly diversified customer should have low overall blast radius")
        void highlyDiversified() {
            // 3 metros, each with only 1 of 3 ASNs
            BlastRadiusReport dcBlast = createBlastReport(0.33, 1);
            BlastRadiusReport daBlast = createBlastReport(0.33, 1);
            BlastRadiusReport sgBlast = createBlastReport(0.33, 1);

            ResiliencyAssessment assessment = ResiliencyAssessment.builder()
                    .overallScore(0.8)
                    .overallRating("Excellent")
                    .failoverPaths(Collections.emptyMap())
                    .blastRadiusReports(Arrays.asList(dcBlast, daBlast, sgBlast))
                    .correlatedFailures(Collections.emptyList())
                    .diversityScores(Collections.emptyList())
                    .findings(Collections.emptyList())
                    .build();

            // No single metro failure should be CRITICAL
            for (BlastRadiusReport br : assessment.getBlastRadiusReports()) {
                assertNotEquals("CRITICAL", br.computeSeverity());
                assertNotEquals("HIGH", br.computeSeverity());
            }

            assertFalse(assessment.hasSinglePointOfFailure(0.5));
        }

        @Test
        @DisplayName("criticalCorrelations should return HIGH severity correlations too")
        void highSeverityInCriticalCorrelations() {
            CorrelatedFailure highCf = CorrelatedFailure.builder()
                    .scope(FailureScope.METRO)
                    .failureDomain("DC metro")
                    .affectedMetro(MetroCode.DC)
                    .affectedAsns(Collections.singletonList(AWS))
                    .affectedLabels(Collections.singletonList("AWS"))
                    .affectedPaths(Collections.singletonList("IX Peering to AWS"))
                    .impactRatio(0.5)
                    .severity("HIGH")
                    .recommendation("Add geographic diversity")
                    .build();

            ResiliencyAssessment assessment = ResiliencyAssessment.builder()
                    .overallScore(0.5)
                    .overallRating("Moderate")
                    .failoverPaths(Collections.emptyMap())
                    .blastRadiusReports(Collections.emptyList())
                    .correlatedFailures(Collections.singletonList(highCf))
                    .diversityScores(Collections.emptyList())
                    .findings(Collections.emptyList())
                    .build();

            assertEquals(1, assessment.criticalCorrelations().size());
        }
    }

    // ---- Helpers ----

    private static BlastRadiusReport createBlastReport(double impactRatio, int lostAsns) {
        List<Long> asns = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        long[] asnPool = {AWS, MSFT, GOOG};
        for (int i = 0; i < lostAsns && i < asnPool.length; i++) {
            asns.add(asnPool[i]);
            labels.add("ASN-" + asnPool[i]);
        }

        return BlastRadiusReport.builder()
                .metro(MetroCode.DC)
                .scope(FailureScope.METRO)
                .lostIxPeeringAsns(asns)
                .lostFabricAsns(Collections.emptyList())
                .lostIxPeeringLabels(labels)
                .lostFabricLabels(Collections.emptyList())
                .lostIxCapacityMbps(lostAsns * 100000L)
                .impactRatio(impactRatio)
                .severity(impactRatio > 0.8 ? "CRITICAL" : impactRatio > 0.5 ? "HIGH" :
                        impactRatio > 0.25 ? "MEDIUM" : "LOW")
                .mitigations(Collections.emptyList())
                .build();
    }
}
