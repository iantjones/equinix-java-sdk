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

import api.equinix.javasdk.Fabric;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.design.value.ratecard.EquinixRateCard;
import api.equinix.javasdk.design.value.ratecard.PriceQuote;
import api.equinix.javasdk.design.value.ratecard.PriceSource;
import api.equinix.javasdk.design.value.ratecard.Term;
import api.equinix.javasdk.fabric.enums.ConnectionType;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.util.Optional;

import static api.equinix.javasdk.core.ResponseStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based tests for {@link EquinixRateCard}, exercising the live
 * {@code fabric.prices()} path: client-side matching of the price catalogue,
 * charge-to-quote mapping, and graceful degradation when the catalogue cannot
 * be fetched.
 */
class EquinixRateCardWireMockTest extends WireMockTestBase {

    static Fabric fabric;

    @BeforeAll
    static void setUp() {
        fabric = new Fabric(testCredentials());
        redirectToWireMock(fabric);
        fabric.authenticate();
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (fabric != null) fabric.close();
    }

    @BeforeEach
    void resetBeforeEach() {
        resetStubs();
    }

    @Test
    @DisplayName("resolves a live connection price from the catalogue")
    void resolvesLiveConnectionPrice() {
        stubPaginatedPost(wireMock, "/fabric/v4/prices/search", "/json/fabric/paginated_prices.json");

        // Fixture row: EVPL_VC_SV_DC_100, 100 Mbps, MRC 250.00 / NRC 0.00, USD.
        PriceQuote quote = EquinixRateCard.of(fabric)
                .connection(ConnectionType.EVPL_VC, 100, MetroCode.DC, Term.MONTH_12)
                .orElseThrow();

        assertEquals(0, new BigDecimal("250.00").compareTo(quote.getMonthlyRecurring()));
        assertEquals(0, BigDecimal.ZERO.compareTo(quote.getNonRecurring()));
        assertEquals("USD", quote.getCurrency().getCurrencyCode());
        assertEquals(PriceSource.EQUINIX_LIVE, quote.getSource());
        assertNotNull(quote.getNote());
        assertTrue(quote.getNote().contains("EVPL_VC_SV_DC_100"));
    }

    @Test
    @DisplayName("narrows the catalogue fetch server-side by price /type")
    void sendsTypeScopedFilter() {
        stubPaginatedPost(wireMock, "/fabric/v4/prices/search", "/json/fabric/paginated_prices.json");

        // Any lookup forces the (once-cached) catalogue fetch.
        EquinixRateCard.of(fabric).connection(ConnectionType.EVPL_VC, 100, MetroCode.DC, Term.MONTH_12);

        // Two type-scoped POSTs are issued — one per product family this card prices — rather
        // than an unfiltered scan of the whole catalogue.
        wireMock.verify(postRequestedFor(urlPathEqualTo("/fabric/v4/prices/search"))
                .withRequestBody(containing("/type"))
                .withRequestBody(containing("VIRTUAL_CONNECTION_PRODUCT")));
        wireMock.verify(postRequestedFor(urlPathEqualTo("/fabric/v4/prices/search"))
                .withRequestBody(containing("/type"))
                .withRequestBody(containing("CLOUD_ROUTER_PRODUCT")));
    }

    @Test
    @DisplayName("returns empty when no catalogue row matches the bandwidth")
    void emptyWhenNoBandwidthMatch() {
        stubPaginatedPost(wireMock, "/fabric/v4/prices/search", "/json/fabric/paginated_prices.json");

        Optional<PriceQuote> quote = EquinixRateCard.of(fabric)
                .connection(ConnectionType.EVPL_VC, 999, MetroCode.DC, Term.MONTH_12);

        assertTrue(quote.isEmpty(), "unmatched bandwidth must yield empty, not a wrong price");
    }

    @Test
    @DisplayName("degrades gracefully to empty when the pricing API errors")
    void degradesOnApiError() {
        stubErrorInline(wireMock, "/fabric/v4/prices/search",
                500, "[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]");

        EquinixRateCard card = EquinixRateCard.of(fabric);

        assertDoesNotThrow(() -> card.connection(ConnectionType.EVPL_VC, 100, MetroCode.DC, Term.MONTH_12));
        assertTrue(card.connection(ConnectionType.EVPL_VC, 100, MetroCode.DC, Term.MONTH_12).isEmpty());
        assertEquals(PriceSource.EQUINIX_LIVE, card.source());
    }

    @Test
    @DisplayName("matches CLOUD_ROUTER_PRODUCT for cloudRouter; empty when no package token matches")
    void cloudRouterMatchesGatewayProduct() {
        stubPaginatedPost(wireMock, "/fabric/v4/prices/search", "/json/fabric/paginated_prices_multi.json");
        EquinixRateCard card = EquinixRateCard.of(fabric);

        assertEquals(0, new BigDecimal("1200.00").compareTo(
                card.cloudRouter("STANDARD", MetroCode.DC, Term.MONTH_12).orElseThrow().getMonthlyRecurring()));
        assertTrue(card.cloudRouter("LARGE", MetroCode.DC, Term.MONTH_12).isEmpty(),
                "no gateway row mentions LARGE");
    }

    @Test
    @DisplayName("connection prefers the row that mentions the requested metro")
    void connectionPrefersMetroMatch() {
        stubPaginatedPost(wireMock, "/fabric/v4/prices/search", "/json/fabric/paginated_prices_multi.json");
        // Two 1 Gbps EVPL_VC rows: 150 (SV/LA) and 175 (DC). Requesting DC must pick the DC row.
        assertEquals(0, new BigDecimal("175.00").compareTo(
                EquinixRateCard.of(fabric).connection(ConnectionType.EVPL_VC, 1000, MetroCode.DC, Term.MONTH_12)
                        .orElseThrow().getMonthlyRecurring()));
    }

    @Test
    @DisplayName("invalid currency on a price row falls back to USD")
    void invalidCurrencyFallsBackToUsd() {
        stubPaginatedPost(wireMock, "/fabric/v4/prices/search", "/json/fabric/paginated_prices_multi.json");
        PriceQuote q = EquinixRateCard.of(fabric)
                .connection(ConnectionType.EVPL_VC, 5000, null, Term.MONTH_12).orElseThrow();
        assertEquals("USD", q.getCurrency().getCurrencyCode());
    }

    @Test
    @DisplayName("maps a NON_RECURRING charge to the setup component")
    void mapsNonRecurringCharge() {
        stubPaginatedPost(wireMock, "/fabric/v4/prices/search", "/json/fabric/paginated_prices_multi.json");
        PriceQuote q = EquinixRateCard.of(fabric)
                .connection(ConnectionType.EVPL_VC, 2000, MetroCode.DC, Term.MONTH_12).orElseThrow();
        assertEquals(0, new BigDecimal("300.00").compareTo(q.getMonthlyRecurring()));
        assertEquals(0, new BigDecimal("1000.00").compareTo(q.getNonRecurring()));
    }

    @Test
    @DisplayName("C1: a 2-letter metro code does not false-match an unrelated row via substring")
    void metroCodeDoesNotFalseMatchUnrelatedRow() {
        // Two 1 Gbps EVPL_VC rows. The first is a DC/SV "Gateway" row (999) whose name/code
        // merely CONTAINS the substring "at" (in "Gateway"); the second is the genuine AT
        // (Atlanta) row (175). Requesting metro AT must pick the real AT row via a whole-token
        // match — never false-match "Gateway" as the AT-specific result and return 999.
        stubPaginatedPost(wireMock, "/fabric/v4/prices/search", "/json/fabric/paginated_prices_metrotoken.json");

        PriceQuote q = EquinixRateCard.of(fabric)
                .connection(ConnectionType.EVPL_VC, 1000, MetroCode.AT, Term.MONTH_12).orElseThrow();

        assertEquals(0, new BigDecimal("175.00").compareTo(q.getMonthlyRecurring()),
                "metro AT must match the real AT row, not substring-match 'Gateway'");
        assertNotEquals(0, new BigDecimal("999.00").compareTo(q.getMonthlyRecurring()),
                "the wrong-metro 'Gateway' row must not be returned as the AT-specific quote");
        assertTrue(q.getNote().contains("EVPL_VC_AT_FR_1000"));
    }

    @Test
    @DisplayName("C3: a row with no priced charge yields empty (not a phantom $0) and defers to a priced row")
    void unpricedRowYieldsEmptyNotPhantomZero() {
        stubPaginatedPost(wireMock, "/fabric/v4/prices/search", "/json/fabric/paginated_prices_unpriced.json");
        EquinixRateCard card = EquinixRateCard.of(fabric);

        // 400 Mbps: only an all-null-price row exists. It must NOT resolve to a present $0
        // EQUINIX_LIVE quote — it must be empty so the caller defers to the fallback.
        Optional<PriceQuote> nullPriced = card.connection(ConnectionType.EVPL_VC, 400, MetroCode.DC, Term.MONTH_12);
        assertTrue(nullPriced.isEmpty(),
                "a row whose charges are all null-priced must yield empty, not a phantom $0 quote");

        // 300 Mbps: an empty-charges row precedes a genuinely priced row. The unpriced row is
        // skipped and the priced row (99.00) wins — proving "no priced charge" is skipped, not
        // returned as $0.
        PriceQuote priced = card.connection(ConnectionType.EVPL_VC, 300, MetroCode.DC, Term.MONTH_12).orElseThrow();
        assertEquals(0, new BigDecimal("99.00").compareTo(priced.getMonthlyRecurring()),
                "the empty-charges row must be skipped in favour of the priced 300 Mbps row");
        assertTrue(priced.getNote().contains("EVPL_VC_DC_PRICED_300"));
    }

    @Test
    @DisplayName("C5: a failed catalogue fetch is not cached — the next lookup retries and can succeed")
    void failedFetchIsNotCachedAndRetriesNextLookup() {
        // First fetch fails with a 500: the card yields empty and flags the live source unavailable,
        // but must NOT cache the failure for its lifetime.
        stubErrorInline(wireMock, "/fabric/v4/prices/search",
                500, "[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]");
        EquinixRateCard card = EquinixRateCard.of(fabric);

        assertTrue(card.connection(ConnectionType.EVPL_VC, 100, MetroCode.DC, Term.MONTH_12).isEmpty(),
                "a failed fetch must yield empty, not throw");
        assertTrue(card.isLiveSourceUnavailable(),
                "a failed fetch must expose the 'live source unavailable' signal");

        // The catalogue endpoint recovers. Because the failure was not memoized, the next lookup
        // re-fetches and now resolves the live price — a transient error did not poison the card.
        resetStubs();
        stubPaginatedPost(wireMock, "/fabric/v4/prices/search", "/json/fabric/paginated_prices.json");

        PriceQuote q = card.connection(ConnectionType.EVPL_VC, 100, MetroCode.DC, Term.MONTH_12).orElseThrow();
        assertEquals(0, new BigDecimal("250.00").compareTo(q.getMonthlyRecurring()),
                "the retried fetch must resolve the live price, proving the failure was not cached");
        assertFalse(card.isLiveSourceUnavailable(),
                "a subsequent successful fetch must clear the 'live source unavailable' signal");
    }
}
