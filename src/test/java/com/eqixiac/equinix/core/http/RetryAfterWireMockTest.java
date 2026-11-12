package com.eqixiac.equinix.core.http;

import com.eqixiac.equinix.core.ManualClock;
import com.eqixiac.equinix.core.WireMockTestBase;
import com.eqixiac.equinix.core.enums.HttpMethod;
import com.eqixiac.equinix.core.exception.EquinixClientException;
import com.eqixiac.equinix.core.exception.EquinixServerException;
import com.eqixiac.equinix.core.http.request.EquinixRequest;
import com.eqixiac.equinix.core.http.request.RequestBody;
import com.eqixiac.equinix.core.http.response.EquinixResponse;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * {@code Retry-After} handling and method-idempotency gating of {@link EquinixHttpClient}, with an
 * injected clock and a recording sleeper, so no test waits on the wall clock.
 */
class RetryAfterWireMockTest extends WireMockTestBase {

    private static final String PATH = "/things";
    private final ManualClock clock = new ManualClock(Instant.parse("2026-11-11T12:00:00Z"));
    private final List<Long> sleeps = new ArrayList<>();

    @BeforeEach
    void reset() {
        resetStubs();
        sleeps.clear();
    }

    private EquinixHttpClient client(RetryPolicy policy) {
        EquinixHttpClient client = new EquinixHttpClient();
        client.setRetryPolicy(policy);
        client.setClock(clock);
        client.setSleeper(sleeps::add);
        return client;
    }

    private static RetryPolicy policy(boolean retryNonIdempotent) {
        // Max delay well above the Retry-After values used, so the header value is observable.
        return new RetryPolicy(2, 1, 120_000, Set.of(429, 500, 502, 503, 504), true, true, retryNonIdempotent);
    }

    private static EquinixRequest<Object> request(HttpMethod method) {
        EquinixRequest<Object> request = new EquinixRequest<>();
        request.setHttpMethod(method);
        request.setEndPoint(URI.create(wireMockUrl()));
        request.setResourcePath(PATH);
        request.setHeaders(new java.util.HashMap<>(Map.of("content-type", "application/json")));
        if (method == HttpMethod.POST) {
            request.setBody(RequestBody.json(Map.of("name", "x")));
        }
        return request;
    }

    private static void stubFailThenOk(int status, String retryAfter) {
        wireMock.stubFor(any(urlPathEqualTo(PATH)).inScenario("retry").whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(status).withHeader("Retry-After", retryAfter))
                .willSetStateTo("recovered"));
        wireMock.stubFor(any(urlPathEqualTo(PATH)).inScenario("retry").whenScenarioStateIs("recovered")
                .willReturn(okJson("{}")));
    }

    private static void drain(EquinixResponse<Object> response) throws Exception {
        try (InputStream ignored = response.getContent()) {
            assertEquals(200, response.getStatusCode());
        }
    }

    @Test
    @DisplayName("an HTTP-date Retry-After is honoured relative to the client's clock")
    void httpDateRetryAfter_isHonoured() throws Exception {
        String sevenSecondsLater = DateTimeFormatter.RFC_1123_DATE_TIME
                .format(clock.instant().plusSeconds(7).atOffset(ZoneOffset.UTC));
        stubFailThenOk(503, sevenSecondsLater);

        try (EquinixHttpClient client = client(policy(false))) {
            drain(client.executeWithRetries(request(HttpMethod.GET)));
        }

        assertEquals(List.of(7_000L), sleeps);
        wireMock.verify(2, getRequestedFor(urlPathEqualTo(PATH)));
    }

    @Test
    @DisplayName("a delta-seconds Retry-After on a 429 is honoured")
    void deltaSecondsRetryAfter_isHonoured() throws Exception {
        stubFailThenOk(429, "3");

        try (EquinixHttpClient client = client(policy(false))) {
            drain(client.executeWithRetries(request(HttpMethod.GET)));
        }

        assertEquals(List.of(3_000L), sleeps);
    }

    @Test
    @DisplayName("parseRetryAfterMillis: both forms, past dates floor at 0, garbage is ignored")
    void parseRetryAfter() {
        Instant now = Instant.parse("2026-11-11T12:00:00Z");
        assertEquals(30_000L, EquinixHttpClient.parseRetryAfterMillis("30", now));
        assertEquals(90_000L, EquinixHttpClient.parseRetryAfterMillis("Wed, 11 Nov 2026 12:01:30 GMT", now));
        assertEquals(0L, EquinixHttpClient.parseRetryAfterMillis("Wed, 11 Nov 2026 11:00:00 GMT", now));
        assertEquals(0L, EquinixHttpClient.parseRetryAfterMillis("-5", now));
        assertNull(EquinixHttpClient.parseRetryAfterMillis("soon", now));
        assertNull(EquinixHttpClient.parseRetryAfterMillis(null, now));
    }

    @Test
    @DisplayName("a POST answered 503 is not retried by default")
    void post503_notRetriedByDefault() throws Exception {
        stubFailThenOk(503, "1");

        try (EquinixHttpClient client = client(policy(false))) {
            assertThrows(EquinixServerException.class, () -> client.executeWithRetries(request(HttpMethod.POST)));
        }

        wireMock.verify(1, postRequestedFor(urlPathEqualTo(PATH)));
        assertTrue(sleeps.isEmpty());
    }

    @Test
    @DisplayName("a POST whose connection is reset is not retried by default")
    void postTransportFailure_notRetriedByDefault() throws Exception {
        wireMock.stubFor(post(urlPathEqualTo(PATH)).willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

        try (EquinixHttpClient client = client(policy(false))) {
            assertThrows(EquinixClientException.class, () -> client.executeWithRetries(request(HttpMethod.POST)));
        }

        wireMock.verify(1, postRequestedFor(urlPathEqualTo(PATH)));
    }

    @Test
    @DisplayName("the default policy does not retry a POST either")
    void defaultPolicy_doesNotRetryPost() throws Exception {
        stubFailThenOk(503, "1");

        try (EquinixHttpClient client = client(RetryPolicy.defaultPolicy())) {
            assertThrows(EquinixServerException.class, () -> client.executeWithRetries(request(HttpMethod.POST)));
        }

        wireMock.verify(1, postRequestedFor(urlPathEqualTo(PATH)));
    }

    @Test
    @DisplayName("a POST is retried once non-idempotent retries are opted in")
    void post503_retriedWhenOptedIn() throws Exception {
        stubFailThenOk(503, "2");

        try (EquinixHttpClient client = client(policy(true))) {
            drain(client.executeWithRetries(request(HttpMethod.POST)));
        }

        wireMock.verify(2, postRequestedFor(urlPathEqualTo(PATH)));
        assertEquals(List.of(2_000L), sleeps);
    }

    @Test
    @DisplayName("a Retry-After beyond the policy's max delay is capped")
    void retryAfter_cappedAtMaxDelay() throws Exception {
        stubFailThenOk(503, String.valueOf(Duration.ofHours(1).toSeconds()));

        try (EquinixHttpClient client = client(new RetryPolicy(1, 1, 5_000, Set.of(503), true, true))) {
            drain(client.executeWithRetries(request(HttpMethod.GET)));
        }

        assertEquals(List.of(5_000L), sleeps);
    }
}
