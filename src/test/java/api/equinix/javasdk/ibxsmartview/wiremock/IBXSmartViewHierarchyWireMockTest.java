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

package api.equinix.javasdk.ibxsmartview.wiremock;

import api.equinix.javasdk.IBXSmartView;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.ibxsmartview.enums.HierarchyLevelType;
import api.equinix.javasdk.ibxsmartview.enums.PowerLevelType;
import api.equinix.javasdk.ibxsmartview.model.implementation.HierarchyNode;
import api.equinix.javasdk.ibxsmartview.model.implementation.PowerHierarchyNode;
import org.junit.jupiter.api.*;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static api.equinix.javasdk.core.ResponseStubs.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for the IBX SmartView Hierarchy read endpoints
 * (GET hierarchy/location and GET hierarchy/power). These verify the exact path, verb
 * and accountNo/ibx query params, and that the bare recursive-node array deserializes.
 */
class IBXSmartViewHierarchyWireMockTest extends WireMockTestBase {

    static IBXSmartView smartView;

    static final String ACCOUNT_NO = "123456";
    static final String IBX = "SV5";

    static final String LOCATION_PATH = "/smartview/v1/hierarchy/location";
    static final String POWER_PATH = "/smartview/v1/hierarchy/power";

    @BeforeAll
    static void setUp() {
        smartView = new IBXSmartView(testCredentials());
        redirectToWireMock(smartView);
        smartView.authenticate();
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (smartView != null) smartView.close();
    }

    @BeforeEach
    void resetBeforeEach() {
        resetStubs();
    }

    @Nested
    @DisplayName("getLocationHierarchy()")
    class GetLocationHierarchy {

        @Test
        @DisplayName("GETs hierarchy/location with accountNo/ibx query params and deserializes the node tree")
        void getsLocationHierarchy() {
            stubSingleton(wireMock, LOCATION_PATH, "/json/ibxsmartview/location_hierarchy_response.json");

            List<HierarchyNode> nodes = smartView.hierarchy().getLocationHierarchy(ACCOUNT_NO, IBX);

            assertNotNull(nodes);
            assertEquals(1, nodes.size());
            HierarchyNode root = nodes.get(0);
            assertEquals(HierarchyLevelType.IBX, root.getLevelType());
            assertEquals("SV5", root.getLevelValue());
            assertEquals("Silicon Valley 5", root.getLabel());
            assertEquals(1, root.getChildren().size());
            HierarchyNode cage = root.getChildren().get(0);
            assertEquals(HierarchyLevelType.CAGE, cage.getLevelType());
            assertEquals("SV5:01:0001", cage.getLevelValue());
            assertEquals(HierarchyLevelType.SENSOR,
                    cage.getChildren().get(0).getChildren().get(0).getLevelType());

            wireMock.verify(getRequestedFor(urlPathEqualTo(LOCATION_PATH))
                    .withQueryParam("accountNo", equalTo(ACCOUNT_NO))
                    .withQueryParam("ibx", equalTo(IBX)));
        }

        @Test
        @DisplayName("404 throws EquinixNotFoundException")
        void notFound() {
            wireMock.stubFor(get(urlPathEqualTo(LOCATION_PATH))
                    .willReturn(aResponse().withStatus(404)
                            .withHeader("Content-Type", "application/json")
                            .withBody("[{\"errorCode\":\"ERR-404\",\"errorMessage\":\"Account not found\"}]")));

            assertThrows(EquinixNotFoundException.class,
                    () -> smartView.hierarchy().getLocationHierarchy(ACCOUNT_NO, IBX));
        }
    }

    @Nested
    @DisplayName("getPowerHierarchy()")
    class GetPowerHierarchy {

        @Test
        @DisplayName("GETs hierarchy/power with accountNo/ibx query params and deserializes the node tree")
        void getsPowerHierarchy() {
            stubSingleton(wireMock, POWER_PATH, "/json/ibxsmartview/power_hierarchy_response.json");

            List<PowerHierarchyNode> nodes = smartView.hierarchy().getPowerHierarchy(ACCOUNT_NO, IBX);

            assertNotNull(nodes);
            assertEquals(1, nodes.size());
            PowerHierarchyNode root = nodes.get(0);
            assertEquals(PowerLevelType.IBX, root.getLevelType());
            assertEquals("SV5", root.getLevelValue());
            assertEquals("Silicon Valley 5", root.getLabel());
            PowerHierarchyNode cabinet = root.getChildren().get(0).getChildren().get(0);
            assertEquals(PowerLevelType.CABINET, cabinet.getLevelType());
            assertEquals(PowerLevelType.CIRCUIT, cabinet.getChildren().get(0).getLevelType());
            assertEquals("CIRCUIT-A1", cabinet.getChildren().get(0).getLevelValue());

            wireMock.verify(getRequestedFor(urlPathEqualTo(POWER_PATH))
                    .withQueryParam("accountNo", equalTo(ACCOUNT_NO))
                    .withQueryParam("ibx", equalTo(IBX)));
        }

        @Test
        @DisplayName("500 throws EquinixServerException")
        void serverError() {
            wireMock.stubFor(get(urlPathEqualTo(POWER_PATH))
                    .willReturn(aResponse().withStatus(500)
                            .withHeader("Content-Type", "application/json")
                            .withBody("[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]")));

            assertThrows(EquinixServerException.class,
                    () -> smartView.hierarchy().getPowerHierarchy(ACCOUNT_NO, IBX));
        }
    }
}
