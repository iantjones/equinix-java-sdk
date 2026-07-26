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

package api.equinix.javasdk.design.value.ratecard.provider;

import api.equinix.javasdk.core.WireMockTestBase;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.http.client.config.RequestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ProviderPricingHttpClient}: the hard timeout configuration that makes
 * "graceful degradation" actually bounded, and the credential redaction that keeps API keys
 * out of log lines. (This test lives in the adapter package to reach the package-private
 * client directly.)
 */
class ProviderPricingHttpClientTest extends WireMockTestBase {

    @BeforeEach
    void resetBeforeEach() {
        resetStubs();
    }

    @Test
    @DisplayName("the default client runs every request under hard connect/socket/connection-request timeouts")
    void defaultTimeoutsAreConfigured() {
        RequestConfig config = new ProviderPricingHttpClient().requestConfig();

        assertEquals(ProviderPricingHttpClient.DEFAULT_CONNECT_TIMEOUT_MS, config.getConnectTimeout(),
                "connect timeout must be finite — an unresponsive endpoint must not hang the caller");
        assertEquals(ProviderPricingHttpClient.DEFAULT_SOCKET_TIMEOUT_MS, config.getSocketTimeout(),
                "socket timeout must be finite");
        assertEquals(ProviderPricingHttpClient.DEFAULT_CONNECTION_REQUEST_TIMEOUT_MS,
                config.getConnectionRequestTimeout(),
                "connection-request (pool lease) timeout must be finite");
    }

    @Test
    @DisplayName("the default timeouts stay in the same order as the MCP layer's 12 s pricing watchdog")
    void defaultTimeoutsAlignWithMcpPricingTimeout() {
        // The MCP layer wraps whole rate-card lookups in EQUINIX_MCP_PRICING_TIMEOUT_MS
        // (ServerContext.DEFAULT_PRICING_TIMEOUT_MS = 12 000 ms). The transport-level bounds
        // guarantee a single GET terminates on the same order of magnitude, so callers WITHOUT
        // that watchdog (a plain layered RateCard) still cannot hang.
        assertEquals(5_000, ProviderPricingHttpClient.DEFAULT_CONNECT_TIMEOUT_MS);
        assertEquals(10_000, ProviderPricingHttpClient.DEFAULT_SOCKET_TIMEOUT_MS);
        assertEquals(2_000, ProviderPricingHttpClient.DEFAULT_CONNECTION_REQUEST_TIMEOUT_MS);
    }

    @Test
    @DisplayName("the constructor seam applies custom timeouts")
    void constructorSeamAppliesCustomTimeouts() {
        RequestConfig config = new ProviderPricingHttpClient(111, 222, 333).requestConfig();

        assertEquals(111, config.getConnectTimeout());
        assertEquals(222, config.getSocketTimeout());
        assertEquals(333, config.getConnectionRequestTimeout());
    }

    @Test
    @DisplayName("a hung response is cut by the socket timeout instead of blocking forever")
    void socketTimeoutBoundsAHungResponse() {
        wireMock.stubFor(get(urlPathEqualTo("/slow"))
                .willReturn(okJson("{}").withFixedDelay(5_000)));
        ProviderPricingHttpClient client = new ProviderPricingHttpClient(1_000, 250, 1_000);

        long start = System.nanoTime();
        Optional<JsonNode> result = client.getJson(wireMockUrl() + "/slow");
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

        assertTrue(result.isEmpty(), "a timed-out request degrades to empty, it does not throw");
        assertTrue(elapsedMs < 4_000,
                "the 250 ms socket timeout must cut the 5 s server stall long before it completes, took " + elapsedMs + " ms");
    }

    @Test
    @DisplayName("getJson round-trips JSON on 200 and degrades to empty on non-200 or malformed bodies")
    void getJsonRoundTripAndDegradation() {
        wireMock.stubFor(get(urlPathEqualTo("/ok")).willReturn(okJson("{\"answer\": 42}")));
        wireMock.stubFor(get(urlPathEqualTo("/boom")).willReturn(aResponse().withStatus(500)));
        wireMock.stubFor(get(urlPathEqualTo("/garbage")).willReturn(okJson("this is not json {")));
        ProviderPricingHttpClient client = new ProviderPricingHttpClient();

        assertEquals(42, client.getJson(wireMockUrl() + "/ok?key=super-secret").orElseThrow()
                .path("answer").asInt(), "the credential parameter must not affect the request itself");
        assertTrue(client.getJson(wireMockUrl() + "/boom").isEmpty());
        assertTrue(client.getJson(wireMockUrl() + "/garbage").isEmpty());
    }

    @Test
    @DisplayName("redaction replaces credential query-parameter values with REDACTED")
    void redactsCredentialParameters() {
        assertEquals("https://cloudbilling.googleapis.com/v1/services/X/skus?key=REDACTED&currencyCode=USD&pageSize=5000",
                ProviderPricingHttpClient.redactCredentials(
                        "https://cloudbilling.googleapis.com/v1/services/X/skus?key=AIzaSyExample123&currencyCode=USD&pageSize=5000"),
                "the Google-style ?key= credential must never reach a log line");
        assertEquals("https://x.example/a?api-key=REDACTED",
                ProviderPricingHttpClient.redactCredentials("https://x.example/a?api-key=abc"));
        assertEquals("https://x.example/a?api_key=REDACTED&b=2",
                ProviderPricingHttpClient.redactCredentials("https://x.example/a?api_key=abc&b=2"));
        assertEquals("https://x.example/a?b=2&access_token=REDACTED",
                ProviderPricingHttpClient.redactCredentials("https://x.example/a?b=2&access_token=t0k3n"));
        assertEquals("https://x.example/a?sig=REDACTED&signature=REDACTED",
                ProviderPricingHttpClient.redactCredentials("https://x.example/a?sig=s1&signature=s2"),
                "every credential parameter is redacted, not just the first");
    }

    @Test
    @DisplayName("redaction is case-insensitive on the parameter name and preserves everything else")
    void redactionPreservesNonCredentialContent() {
        assertEquals("https://x.example/a?KEY=REDACTED&region=us-east-1",
                ProviderPricingHttpClient.redactCredentials("https://x.example/a?KEY=abc&region=us-east-1"));
        assertEquals("https://x.example/a?currencyCode=USD&$filter=serviceName%20eq%20'Bandwidth'",
                ProviderPricingHttpClient.redactCredentials(
                        "https://x.example/a?currencyCode=USD&$filter=serviceName%20eq%20'Bandwidth'"),
                "non-credential parameters pass through byte-for-byte");
        assertEquals("https://x.example/plain/path",
                ProviderPricingHttpClient.redactCredentials("https://x.example/plain/path"),
                "a URL without a query string is unchanged");
        assertEquals("https://x.example/a?key=REDACTED#frag",
                ProviderPricingHttpClient.redactCredentials("https://x.example/a?key=abc#frag"),
                "a fragment survives redaction");
        assertNull(ProviderPricingHttpClient.redactCredentials(null));
    }

    @Test
    @DisplayName("parameters whose name merely contains a credential word are not redacted")
    void redactionMatchesWholeParameterNamesOnly() {
        assertEquals("https://x.example/a?monkey=1&keyboard=2&key=REDACTED",
                ProviderPricingHttpClient.redactCredentials("https://x.example/a?monkey=1&keyboard=2&key=abc"),
                "only the exact credential parameter names are redacted");
    }
}
