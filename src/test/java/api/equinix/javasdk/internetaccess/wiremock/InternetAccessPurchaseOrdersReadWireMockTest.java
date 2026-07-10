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

package api.equinix.javasdk.internetaccess.wiremock;

import api.equinix.javasdk.InternetAccess;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.exception.EquinixRateLimitException;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.internetaccess.enums.PurchaseOrderCategory;
import api.equinix.javasdk.internetaccess.enums.PurchaseOrderStatus;
import api.equinix.javasdk.internetaccess.enums.PurchaseOrderType;
import api.equinix.javasdk.internetaccess.model.PurchaseOrder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static api.equinix.javasdk.core.ResponseStubs.stubErrorInline;
import static api.equinix.javasdk.core.ResponseStubs.stubPaginatedGet;
import static com.github.tomakehurst.wiremock.client.WireMock.absent;
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
 * WireMock-backed tests for the Equinix Internet Access (EIA) v1 PurchaseOrders read surface — the
 * {@code PurchaseOrdersV1} group in {@code apiParams_InternetAccess.json}. Covers the list
 * ({@code ListPurchaseOrders}) and its IBX/category-narrowed overload. These route through the shared
 * uriFormat {@code internetAccess/v{version}/{rootUri}/{requestUri}} with {@code defaultVersion: 1},
 * {@code rootUri: accounts} and {@code requestUri: {accountNumber}/purchaseOrders}, i.e.
 * {@code GET /internetAccess/v1/accounts/{accountNumber}/purchaseOrders}. The single-arg
 * {@code list(accountNumber)} overload delegates to the three-arg form with null ibx/category, so it
 * must add no query parameters; the filtered overload adds {@code locations.ibx} and {@code category}.
 */
class InternetAccessPurchaseOrdersReadWireMockTest extends WireMockTestBase {

    private static final String LIST_PATH = "/internetAccess/v1/accounts/100013200/purchaseOrders";
    private static final String FIXTURE = "/json/internetaccess/paginated_purchase_orders.json";

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
        void list_byAccount_getsAccountScopedPath_noQueryParams() {
            stubPaginatedGet(wireMock, LIST_PATH, FIXTURE);

            PaginatedList<PurchaseOrder> orders = internetAccess.purchaseOrders().list("100013200");

            assertEquals(1, orders.size());
            PurchaseOrder po = orders.get(0);
            assertEquals("PO-100013200-001", po.getNumber());
            assertEquals(PurchaseOrderType.STANDARD_PURCHASE_ORDER, po.getType());
            assertEquals(PurchaseOrderStatus.ACTIVE, po.getStatus());
            assertNotNull(po.getAccount());
            assertEquals("100013200", po.getAccount().getAccountNumber());

            wireMock.verify(getRequestedFor(urlPathEqualTo(LIST_PATH))
                    .withQueryParam("locations.ibx", absent())
                    .withQueryParam("category", absent()));
        }

        @Test
        void list_withIbxAndCategory_addsFilterQueryParams() {
            stubPaginatedGet(wireMock, LIST_PATH, FIXTURE);

            PaginatedList<PurchaseOrder> orders =
                    internetAccess.purchaseOrders().list("100013200", "SG1", PurchaseOrderCategory.NETWORK);

            assertEquals(1, orders.size());

            wireMock.verify(getRequestedFor(urlPathEqualTo(LIST_PATH))
                    .withQueryParam("locations.ibx", equalTo("SG1"))
                    .withQueryParam("category", equalTo("NETWORK")));
        }

        @Test
        void list_withNullFilters_omitsFilterQueryParams() {
            stubPaginatedGet(wireMock, LIST_PATH, FIXTURE);

            PaginatedList<PurchaseOrder> orders =
                    internetAccess.purchaseOrders().list("100013200", null, null);

            assertEquals(1, orders.size());

            wireMock.verify(getRequestedFor(urlPathEqualTo(LIST_PATH))
                    .withQueryParam("locations.ibx", absent())
                    .withQueryParam("category", absent()));
        }

        @Test
        void list_loadAllFetchesSecondPageByAdvancingTheQueryOffset() {
            // First request always carries offset=0&limit=100 (PAGE_LIMIT default); page 2 must
            // advance the offset from the SERVER-reported pagination.
            wireMock.stubFor(get(urlPathEqualTo(LIST_PATH))
                    .withQueryParam("offset", equalTo("0"))
                    .willReturn(okJson("{ \"pagination\": { \"offset\": 0, \"limit\": 100, \"total\": 150 }, "
                            + "\"data\": [ { \"number\": \"PAGE1_PO\" } ] }")));
            wireMock.stubFor(get(urlPathEqualTo(LIST_PATH))
                    .withQueryParam("offset", equalTo("100"))
                    .willReturn(okJson("{ \"pagination\": { \"offset\": 100, \"limit\": 100, \"total\": 150 }, "
                            + "\"data\": [ { \"number\": \"PAGE2_PO\" } ] }")));

            PaginatedList<PurchaseOrder> orders = internetAccess.purchaseOrders().list("100013200");
            assertEquals(1, orders.size());
            assertTrue(orders.hasNextPage());

            orders.loadAll();

            assertEquals(2, orders.size());
            assertEquals("PAGE1_PO", orders.get(0).getNumber());
            assertEquals("PAGE2_PO", orders.get(1).getNumber());
            assertFalse(orders.hasNextPage());

            wireMock.verify(1, getRequestedFor(urlPathEqualTo(LIST_PATH))
                    .withQueryParam("offset", equalTo("100"))
                    .withQueryParam("limit", equalTo("100")));
        }

        @Test
        void list_rateLimited429_throwsEquinixRateLimitException() {
            stubErrorInline(wireMock, LIST_PATH,
                    429, "[{\"errorCode\":\"EQ-3000429\",\"errorMessage\":\"Too many requests\"}]");

            assertThrows(EquinixRateLimitException.class,
                    () -> internetAccess.purchaseOrders().list("100013200"));
        }
    }
}
