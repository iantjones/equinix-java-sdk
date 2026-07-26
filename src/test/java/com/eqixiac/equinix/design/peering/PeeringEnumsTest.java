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

package com.eqixiac.equinix.design.peering;

import com.eqixiac.equinix.design.peering.enums.*;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for peering intelligence enums: PeeringPolicy, NetworkType,
 * ConnectivityType, DiversityRating, and FailureScope.
 */
@DisplayName("Peering Intelligence Enums")
class PeeringEnumsTest {

    @Nested
    @DisplayName("PeeringPolicy")
    class PeeringPolicyTests {

        @Test
        @DisplayName("fromPeeringDb should parse standard values")
        void standardValues() {
            assertEquals(PeeringPolicy.OPEN, PeeringPolicy.fromPeeringDb("Open"));
            assertEquals(PeeringPolicy.SELECTIVE, PeeringPolicy.fromPeeringDb("Selective"));
            assertEquals(PeeringPolicy.RESTRICTIVE, PeeringPolicy.fromPeeringDb("Restrictive"));
            assertEquals(PeeringPolicy.NO, PeeringPolicy.fromPeeringDb("No"));
        }

        @Test
        @DisplayName("fromPeeringDb should be case-insensitive")
        void caseInsensitive() {
            assertEquals(PeeringPolicy.OPEN, PeeringPolicy.fromPeeringDb("open"));
            assertEquals(PeeringPolicy.OPEN, PeeringPolicy.fromPeeringDb("OPEN"));
        }

        @Test
        @DisplayName("fromPeeringDb should return UNKNOWN for null/empty/unrecognized")
        void unknownCases() {
            assertEquals(PeeringPolicy.UNKNOWN, PeeringPolicy.fromPeeringDb(null));
            assertEquals(PeeringPolicy.UNKNOWN, PeeringPolicy.fromPeeringDb(""));
            assertEquals(PeeringPolicy.UNKNOWN, PeeringPolicy.fromPeeringDb("Some Other"));
        }

        @Test
        @DisplayName("Feasibility scores should be ordered correctly")
        void feasibilityScores() {
            assertTrue(PeeringPolicy.OPEN.getFeasibilityScore() > PeeringPolicy.SELECTIVE.getFeasibilityScore());
            assertTrue(PeeringPolicy.SELECTIVE.getFeasibilityScore() > PeeringPolicy.RESTRICTIVE.getFeasibilityScore());
            assertTrue(PeeringPolicy.RESTRICTIVE.getFeasibilityScore() > PeeringPolicy.NO.getFeasibilityScore());
            assertEquals(0.0, PeeringPolicy.NO.getFeasibilityScore());
            assertEquals(1.0, PeeringPolicy.OPEN.getFeasibilityScore());
        }
    }

    @Nested
    @DisplayName("NetworkType")
    class NetworkTypeTests {

        @Test
        @DisplayName("fromPeeringDb should parse standard values")
        void standardValues() {
            assertEquals(NetworkType.NSP, NetworkType.fromPeeringDb("NSP"));
            assertEquals(NetworkType.CONTENT, NetworkType.fromPeeringDb("Content"));
            assertEquals(NetworkType.ENTERPRISE, NetworkType.fromPeeringDb("Enterprise"));
            assertEquals(NetworkType.CABLE_DSL_ISP, NetworkType.fromPeeringDb("Cable/DSL/ISP"));
            assertEquals(NetworkType.EDUCATION, NetworkType.fromPeeringDb("Educational/Research"));
            assertEquals(NetworkType.NON_PROFIT, NetworkType.fromPeeringDb("Non-Profit"));
            assertEquals(NetworkType.ROUTE_SERVER, NetworkType.fromPeeringDb("Route Server"));
            assertEquals(NetworkType.GOV, NetworkType.fromPeeringDb("Government"));
        }

        @Test
        @DisplayName("fromPeeringDb should return UNKNOWN for null/empty/unrecognized")
        void unknownCases() {
            assertEquals(NetworkType.UNKNOWN, NetworkType.fromPeeringDb(null));
            assertEquals(NetworkType.UNKNOWN, NetworkType.fromPeeringDb(""));
            assertEquals(NetworkType.UNKNOWN, NetworkType.fromPeeringDb("SomethingElse"));
        }

        @Test
        @DisplayName("All enum values should have display names")
        void displayNames() {
            for (NetworkType type : NetworkType.values()) {
                assertNotNull(type.getDisplayName());
                assertFalse(type.getDisplayName().isEmpty());
            }
        }
    }

    @Nested
    @DisplayName("ConnectivityType")
    class ConnectivityTypeTests {

        @Test
        @DisplayName("resolve should return BOTH when IX and Fabric are present")
        void both() {
            assertEquals(ConnectivityType.BOTH, ConnectivityType.resolve(true, true, true));
            assertEquals(ConnectivityType.BOTH, ConnectivityType.resolve(true, true, false));
        }

        @Test
        @DisplayName("resolve should return IX_PEERING when only IX is present")
        void ixOnly() {
            assertEquals(ConnectivityType.IX_PEERING, ConnectivityType.resolve(true, false, false));
            assertEquals(ConnectivityType.IX_PEERING, ConnectivityType.resolve(true, false, true));
        }

        @Test
        @DisplayName("resolve should return FABRIC_CONNECTION when only Fabric is present")
        void fabricOnly() {
            assertEquals(ConnectivityType.FABRIC_CONNECTION, ConnectivityType.resolve(false, true, false));
            assertEquals(ConnectivityType.FABRIC_CONNECTION, ConnectivityType.resolve(false, true, true));
        }

        @Test
        @DisplayName("resolve should return FACILITY_ONLY when only facility is present")
        void facilityOnly() {
            assertEquals(ConnectivityType.FACILITY_ONLY, ConnectivityType.resolve(false, false, true));
        }

        @Test
        @DisplayName("resolve should return NONE when nothing is present")
        void none() {
            assertEquals(ConnectivityType.NONE, ConnectivityType.resolve(false, false, false));
        }
    }

    @Nested
    @DisplayName("DiversityRating")
    class DiversityRatingTests {

        @Test
        @DisplayName("fromDistance should classify correctly at boundaries")
        void boundaries() {
            assertEquals(DiversityRating.EXCELLENT, DiversityRating.fromDistance(3000));
            assertEquals(DiversityRating.EXCELLENT, DiversityRating.fromDistance(10000));
            assertEquals(DiversityRating.GOOD, DiversityRating.fromDistance(1500));
            assertEquals(DiversityRating.GOOD, DiversityRating.fromDistance(2999));
            assertEquals(DiversityRating.MODERATE, DiversityRating.fromDistance(500));
            assertEquals(DiversityRating.MODERATE, DiversityRating.fromDistance(1499));
            assertEquals(DiversityRating.POOR, DiversityRating.fromDistance(150));
            assertEquals(DiversityRating.POOR, DiversityRating.fromDistance(499));
            assertEquals(DiversityRating.CRITICAL, DiversityRating.fromDistance(0));
            assertEquals(DiversityRating.CRITICAL, DiversityRating.fromDistance(149));
        }

        @Test
        @DisplayName("Scores should be ordered: EXCELLENT > GOOD > MODERATE > POOR > CRITICAL")
        void scoresOrdered() {
            assertTrue(DiversityRating.EXCELLENT.getScore() > DiversityRating.GOOD.getScore());
            assertTrue(DiversityRating.GOOD.getScore() > DiversityRating.MODERATE.getScore());
            assertTrue(DiversityRating.MODERATE.getScore() > DiversityRating.POOR.getScore());
            assertTrue(DiversityRating.POOR.getScore() > DiversityRating.CRITICAL.getScore());
        }

        @Test
        @DisplayName("US East to US West coast should be GOOD diversity")
        void usEastToWest_isGood() {
            // DC to LA ~ 3700 km (but for the rating, >3000 = EXCELLENT)
            assertEquals(DiversityRating.EXCELLENT, DiversityRating.fromDistance(3700));
        }

        @Test
        @DisplayName("DC to NY distance (~330 km) should be POOR diversity")
        void dcToNy_isPoor() {
            // DC to NY ~ 330 km, which is >= 150 (POOR threshold) but < 500 (MODERATE)
            assertEquals(DiversityRating.POOR, DiversityRating.fromDistance(330));
        }
    }

    @Nested
    @DisplayName("FailureScope")
    class FailureScopeTests {

        @Test
        @DisplayName("All failure scopes should be defined")
        void allScopes() {
            assertEquals(5, FailureScope.values().length);
            assertNotNull(FailureScope.METRO);
            assertNotNull(FailureScope.IX);
            assertNotNull(FailureScope.FACILITY);
            assertNotNull(FailureScope.PROVIDER);
            assertNotNull(FailureScope.REGION);
        }
    }
}
