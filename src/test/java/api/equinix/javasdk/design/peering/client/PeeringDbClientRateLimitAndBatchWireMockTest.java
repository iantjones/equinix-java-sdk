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

package api.equinix.javasdk.design.peering.client;

import api.equinix.javasdk.core.WireMockTestBase;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression tests for the {@link PeeringDbClient} rate-limit and batching fixes:
 *
 * <ul>
 *   <li>HTTP 429 is retried (honouring {@code Retry-After}, capped) instead of surfacing as an
 *       immediately-fatal {@link IOException}; other statuses still fail on the first attempt;</li>
 *   <li>multi-ASN lookups are batched with PeeringDB's {@code asn__in} query operator — one
 *       request per endpoint instead of one per ASN — while single-ASN lookups keep the plain
 *       {@code asn=} wire shape;</li>
 *   <li>the client is {@link AutoCloseable} and releases its HTTP client on {@code close()}.</li>
 * </ul>
 */
@DisplayName("PeeringDbClient — 429 rate-limit handling, asn__in batching, close()")
class PeeringDbClientRateLimitAndBatchWireMockTest extends WireMockTestBase {

    private static final long AWS = 16509L;

    private PeeringDbClient client() {
        return PeeringDbClient.withBaseUrl(null, wireMockUrl() + "/api");
    }

    @BeforeEach
    void resetBeforeEach() {
        resetStubs();
    }

    private static String net(long asn, String name) {
        return "{\"asn\":" + asn + ",\"name\":\"" + name + "\"}";
    }

    // ---- 429 handling ----

    @Nested
    @DisplayName("HTTP 429 handling")
    class RateLimitTests {

        @Test
        @DisplayName("a 429 is retried (honouring Retry-After) and the request eventually succeeds")
        void rateLimitedThenSucceeds() throws IOException {
            wireMock.stubFor(get(urlPathEqualTo("/api/net")).inScenario("throttle")
                    .whenScenarioStateIs(Scenario.STARTED)
                    .willReturn(aResponse().withStatus(429)
                            .withHeader("Retry-After", "0").withBody("slow down"))
                    .willSetStateTo("second"));
            wireMock.stubFor(get(urlPathEqualTo("/api/net")).inScenario("throttle")
                    .whenScenarioStateIs("second")
                    .willReturn(aResponse().withStatus(429)
                            .withHeader("Retry-After", "0").withBody("slow down"))
                    .willSetStateTo("third"));
            wireMock.stubFor(get(urlPathEqualTo("/api/net")).inScenario("throttle")
                    .whenScenarioStateIs("third")
                    .willReturn(okJson("{\"data\":[" + net(AWS, "Amazon.com, Inc.") + "]}")));

            PeeringDbNetwork result = client().getNetwork(AWS);

            assertNotNull(result, "the request must succeed after the 429s clear");
            assertEquals(AWS, result.getAsn());
            wireMock.verify(exactly(3), getRequestedFor(urlPathEqualTo("/api/net")));
        }

        @Test
        @DisplayName("a persistent 429 fails after MAX_RATE_LIMIT_RETRIES retries, not on the first attempt")
        void persistent429ExhaustsRetries() {
            wireMock.stubFor(get(urlPathEqualTo("/api/net"))
                    .willReturn(aResponse().withStatus(429)
                            .withHeader("Retry-After", "0").withBody("slow down")));

            IOException ex = assertThrows(IOException.class, () -> client().getNetwork(AWS));

            assertTrue(ex.getMessage().contains("429"), () -> "message was: " + ex.getMessage());
            // 1 initial attempt + MAX_RATE_LIMIT_RETRIES retries.
            wireMock.verify(exactly(PeeringDbClient.MAX_RATE_LIMIT_RETRIES + 1),
                    getRequestedFor(urlPathEqualTo("/api/net")));
        }

        @Test
        @DisplayName("a Retry-After beyond the total backoff budget fails fast instead of stalling")
        void oversizedRetryAfterFailsFast() {
            wireMock.stubFor(get(urlPathEqualTo("/api/net"))
                    .willReturn(aResponse().withStatus(429)
                            .withHeader("Retry-After", "3600").withBody("slow down")));

            IOException ex = assertThrows(IOException.class, () -> client().getNetwork(AWS));

            assertTrue(ex.getMessage().contains("429"), () -> "message was: " + ex.getMessage());
            assertTrue(ex.getMessage().contains("budget"),
                    () -> "the failure must name the backoff budget; was: " + ex.getMessage());
            // No pointless retry can honour a 3600s wait under a 30s budget: exactly one attempt.
            wireMock.verify(exactly(1), getRequestedFor(urlPathEqualTo("/api/net")));
        }

        @Test
        @DisplayName("non-429 errors are NOT retried")
        void non429NotRetried() {
            wireMock.stubFor(get(urlPathEqualTo("/api/net"))
                    .willReturn(aResponse().withStatus(500).withBody("boom")));

            IOException ex = assertThrows(IOException.class, () -> client().getNetwork(AWS));

            assertTrue(ex.getMessage().contains("500"), () -> "message was: " + ex.getMessage());
            wireMock.verify(exactly(1), getRequestedFor(urlPathEqualTo("/api/net")));
        }

        @Test
        @DisplayName("retryAfterMillis honours seconds, HTTP-dates, and falls back to exponential backoff")
        void retryAfterParsing() {
            // Integer-seconds form.
            assertEquals(5000L, PeeringDbClient.retryAfterMillis("5", 0));
            assertEquals(0L, PeeringDbClient.retryAfterMillis("0", 0));
            // Negative seconds clamp to zero rather than going back in time.
            assertEquals(0L, PeeringDbClient.retryAfterMillis("-5", 0));

            // HTTP-date in the past clamps to zero.
            String pastDate = DateTimeFormatter.RFC_1123_DATE_TIME
                    .format(ZonedDateTime.now().minusMinutes(5));
            assertEquals(0L, PeeringDbClient.retryAfterMillis(pastDate, 0));

            // HTTP-date in the future waits (roughly) until that instant.
            String futureDate = DateTimeFormatter.RFC_1123_DATE_TIME
                    .format(ZonedDateTime.now().plusSeconds(10));
            long futureWait = PeeringDbClient.retryAfterMillis(futureDate, 0);
            assertTrue(futureWait > 5_000L && futureWait <= 10_000L,
                    "a future HTTP-date must wait until that instant, was " + futureWait + "ms");

            // Absent or unparsable headers fall back to the small exponential backoff.
            assertEquals(1000L, PeeringDbClient.retryAfterMillis(null, 0));
            assertEquals(2000L, PeeringDbClient.retryAfterMillis(null, 1));
            assertEquals(4000L, PeeringDbClient.retryAfterMillis("soon", 2));
        }
    }

    // ---- asn__in batching ----

    @Nested
    @DisplayName("asn__in batching")
    class BatchingTests {

        @Test
        @DisplayName("getNetworks(Collection) collapses multiple ASNs into ONE asn__in request")
        void networksBatchUsesAsnIn() throws IOException {
            wireMock.stubFor(get(urlPathEqualTo("/api/net"))
                    .withQueryParam("asn__in", equalTo("64500,64501"))
                    .willReturn(okJson("{\"data\":["
                            + net(64500L, "Alpha") + "," + net(64501L, "Beta") + "]}")));

            Map<Long, PeeringDbNetwork> nets = client().getNetworks(List.of(64500L, 64501L));

            assertEquals(2, nets.size());
            assertEquals("Alpha", nets.get(64500L).getName());
            assertEquals("Beta", nets.get(64501L).getName());
            // Exactly one HTTP request for the whole batch — the per-ASN loop is gone.
            wireMock.verify(exactly(1), getRequestedFor(urlPathEqualTo("/api/net")));
        }

        @Test
        @DisplayName("a single-element batch keeps the plain asn= wire shape")
        void singleElementBatchDelegates() throws IOException {
            wireMock.stubFor(get(urlPathEqualTo("/api/net"))
                    .withQueryParam("asn", equalTo("16509"))
                    .willReturn(okJson("{\"data\":[" + net(AWS, "Amazon.com, Inc.") + "]}")));

            Map<Long, PeeringDbNetwork> nets = client().getNetworks(List.of(AWS));

            assertEquals(1, nets.size());
            assertEquals("Amazon.com, Inc.", nets.get(AWS).getName());
            wireMock.verify(exactly(1), getRequestedFor(urlPathEqualTo("/api/net"))
                    .withQueryParam("asn", equalTo("16509")));
        }

        @Test
        @DisplayName("getEquinixIxPresence(Collection) batches, filters to Equinix IXes, and defaults absent ASNs to empty")
        void equinixIxPresenceBatchFiltersAndDefaults() throws IOException {
            wireMock.stubFor(get(urlPathEqualTo("/api/org/2"))
                    .willReturn(okJson(loadFixture("/json/peering/peeringdb_org_response.json"))));
            // 16509 has one Equinix session (ix 100) and one non-Equinix session (ix 999);
            // 64500 has nothing.
            wireMock.stubFor(get(urlPathEqualTo("/api/netixlan"))
                    .withQueryParam("asn__in", equalTo("16509,64500"))
                    .willReturn(okJson("{\"data\":["
                            + "{\"ix_id\":100,\"asn\":16509,\"speed\":100000,\"is_rs_peer\":true,\"operational\":true},"
                            + "{\"ix_id\":999,\"asn\":16509,\"speed\":10000,\"is_rs_peer\":false,\"operational\":true}"
                            + "]}")));

            PeeringDbClient client = client();
            client.loadEquinixCatalog();
            Map<Long, List<PeeringDbNetIxlan>> presence =
                    client.getEquinixIxPresence(List.of(16509L, 64500L));

            assertEquals(2, presence.size(), "every requested ASN must be present as a key");
            assertEquals(1, presence.get(16509L).size(), "non-Equinix ix 999 must be filtered out");
            assertEquals(100, presence.get(16509L).get(0).getIxId());
            assertTrue(presence.get(64500L).isEmpty(), "an ASN with no sessions maps to an empty list");
            wireMock.verify(exactly(1), getRequestedFor(urlPathEqualTo("/api/netixlan")));
        }

        @Test
        @DisplayName("getEquinixFacPresence(Collection) batches netfac by local_asn and filters to Equinix facilities")
        void equinixFacPresenceBatch() throws IOException {
            wireMock.stubFor(get(urlPathEqualTo("/api/org/2"))
                    .willReturn(okJson(loadFixture("/json/peering/peeringdb_org_response.json"))));
            wireMock.stubFor(get(urlPathEqualTo("/api/netfac"))
                    .withQueryParam("asn__in", equalTo("16509,64500"))
                    .willReturn(okJson("{\"data\":["
                            + "{\"fac_id\":200,\"local_asn\":16509,\"city\":\"Ashburn\"},"
                            + "{\"fac_id\":888,\"local_asn\":16509,\"city\":\"Elsewhere\"},"
                            + "{\"fac_id\":201,\"local_asn\":64500,\"city\":\"San Jose\"}"
                            + "]}")));

            PeeringDbClient client = client();
            client.loadEquinixCatalog();
            Map<Long, List<PeeringDbNetFac>> presence =
                    client.getEquinixFacPresence(List.of(16509L, 64500L));

            assertEquals(1, presence.get(16509L).size(), "non-Equinix fac 888 must be filtered out");
            assertEquals(200, presence.get(16509L).get(0).getFacId());
            assertEquals(1, presence.get(64500L).size());
            assertEquals(201, presence.get(64500L).get(0).getFacId());
            wireMock.verify(exactly(1), getRequestedFor(urlPathEqualTo("/api/netfac")));
        }
    }

    // ---- close() ----

    @Nested
    @DisplayName("close()")
    class CloseTests {

        @Test
        @DisplayName("close() releases the HTTP client; further use fails and double-close is safe")
        void closeReleasesClient() {
            wireMock.stubFor(get(urlPathEqualTo("/api/net"))
                    .willReturn(okJson("{\"data\":[" + net(AWS, "Amazon.com, Inc.") + "]}")));

            PeeringDbClient client = client();
            client.close();
            client.close(); // idempotent — must not throw

            assertThrows(Exception.class, () -> client.getNetwork(AWS),
                    "a closed client must not silently keep issuing requests");
        }
    }
}
