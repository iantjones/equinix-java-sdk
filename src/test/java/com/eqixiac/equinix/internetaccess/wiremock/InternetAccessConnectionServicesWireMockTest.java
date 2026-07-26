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

package com.eqixiac.equinix.internetaccess.wiremock;

import com.eqixiac.equinix.InternetAccess;
import com.eqixiac.equinix.core.WireMockTestBase;
import com.eqixiac.equinix.core.exception.EquinixServiceException;
import com.eqixiac.equinix.core.http.response.PaginatedList;
import com.eqixiac.equinix.internetaccess.model.ConnectionService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.eqixiac.equinix.core.ResponseStubs.stubErrorInline;
import static com.eqixiac.equinix.core.ResponseStubs.stubPaginatedGet;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * WireMock-backed tests for the Equinix Internet Access (EIA) v1 ConnectionServices read surface —
 * the {@code ConnectionServicesV1} group in {@code apiParams_InternetAccess.json}. The single
 * {@code list(ibx)} product-availability lookup routes through the shared uriFormat
 * {@code internetAccess/v{version}/{rootUri}} with {@code defaultVersion: 1} and
 * {@code rootUri: connectionServices}, i.e. {@code GET /internetAccess/v1/connectionServices},
 * narrowing by the {@code mediaTypes.connectorTypes.locations.ibx} query parameter.
 */
class InternetAccessConnectionServicesWireMockTest extends WireMockTestBase {

    private static final String PATH = "/internetAccess/v1/connectionServices";
    private static final String FIXTURE = "/json/internetaccess/paginated_connection_services.json";

    static InternetAccess internetAccess;

    @BeforeAll
    static void setUp() {
        internetAccess = new InternetAccess(testCredentials());
        redirectToWireMock(internetAccess);
        internetAccess.authenticate();
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (internetAccess != null) internetAccess.close();
    }

    @BeforeEach
    void resetBeforeEach() {
        resetStubs();
    }

    @Nested
    class List {

        @Test
        void list_getsConnectionServicesPath_withIbxQueryParam() {
            stubPaginatedGet(wireMock, PATH, FIXTURE);

            PaginatedList<ConnectionService> services = internetAccess.connectionServices().list("SG1");

            assertEquals(2, services.size());
            assertEquals("Internet Access Direct", services.get(0).getName());
            assertNotNull(services.get(0).getMediaTypes());
            assertEquals("Single Mode Fiber", services.get(0).getMediaTypes().get(0).getName());

            wireMock.verify(getRequestedFor(urlPathEqualTo(PATH))
                    .withQueryParam("mediaTypes.connectorTypes.locations.ibx", equalTo("SG1")));
        }

        @Test
        void list_differentIbx_passesThroughToQueryParam() {
            stubPaginatedGet(wireMock, PATH, FIXTURE);

            PaginatedList<ConnectionService> services = internetAccess.connectionServices().list("DC11");

            assertEquals(2, services.size());

            wireMock.verify(getRequestedFor(urlPathEqualTo(PATH))
                    .withQueryParam("mediaTypes.connectorTypes.locations.ibx", equalTo("DC11")));
        }

        @Test
        void list_loadAllFetchesSecondPageByAdvancingTheQueryOffset() {
            // First request always carries offset=0&limit=100 (PAGE_LIMIT default); page 2 must
            // advance the offset from the SERVER-reported pagination while re-sending the filter.
            wireMock.stubFor(get(urlPathEqualTo(PATH))
                    .withQueryParam("offset", equalTo("0"))
                    .willReturn(okJson("{ \"pagination\": { \"offset\": 0, \"limit\": 100, \"total\": 150 }, "
                            + "\"data\": [ { \"name\": \"PAGE1_SERVICE\" } ] }")));
            wireMock.stubFor(get(urlPathEqualTo(PATH))
                    .withQueryParam("offset", equalTo("100"))
                    .willReturn(okJson("{ \"pagination\": { \"offset\": 100, \"limit\": 100, \"total\": 150 }, "
                            + "\"data\": [ { \"name\": \"PAGE2_SERVICE\" } ] }")));

            PaginatedList<ConnectionService> services = internetAccess.connectionServices().list("SG1");
            assertEquals(1, services.size());
            assertTrue(services.hasNextPage());

            services.loadAll();

            assertEquals(2, services.size());
            assertEquals("PAGE1_SERVICE", services.get(0).getName());
            assertEquals("PAGE2_SERVICE", services.get(1).getName());
            assertFalse(services.hasNextPage());

            wireMock.verify(1, getRequestedFor(urlPathEqualTo(PATH))
                    .withQueryParam("offset", equalTo("100"))
                    .withQueryParam("limit", equalTo("100"))
                    .withQueryParam("mediaTypes.connectorTypes.locations.ibx", equalTo("SG1")));
        }

        @Test
        void list_badRequest400_throwsEquinixServiceException() {
            stubErrorInline(wireMock, PATH,
                    400, "[{\"errorCode\":\"EQ-3000400\",\"errorMessage\":\"Invalid ibx\"}]");

            assertThrows(EquinixServiceException.class,
                    () -> internetAccess.connectionServices().list("BAD"));
        }
    }
}
