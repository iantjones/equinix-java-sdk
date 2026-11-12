package com.eqixiac.equinix.core;

import com.eqixiac.equinix.Equinix;
import com.eqixiac.equinix.EquinixConfig;
import com.eqixiac.equinix.core.auth.BearerToken;
import com.eqixiac.equinix.core.auth.BearerTokenProvider;
import com.eqixiac.equinix.core.client.EquinixClient;
import com.eqixiac.equinix.core.exception.EquinixAuthenticationException;
import com.eqixiac.equinix.core.exception.EquinixClientException;
import com.eqixiac.equinix.core.http.RetryPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * A client built over a {@link BearerTokenProvider} signs requests with the provider's token,
 * never calls the OAuth2 token endpoint, asks the provider again before a known expiry and once
 * after a 401 (replaying once), and is wired through the {@link Equinix} session and the facade
 * base class.
 */
class BearerTokenProviderWireMockTest extends WireMockTestBase {

    private static final String THING_PATH = "/coretest/v1/things/1";

    @BeforeEach
    void stubs() {
        wireMock.resetAll();
        stubOAuthToken();
        wireMock.stubFor(get(urlPathEqualTo(THING_PATH))
                .withHeader("authorization", equalTo("Bearer good-token"))
                .willReturn(okJson("{}")));
        wireMock.stubFor(get(urlPathEqualTo(THING_PATH))
                .withHeader("authorization", notContaining("good-token"))
                .willReturn(aResponse().withStatus(401).withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":{\"errorCode\":\"unauthenticated\",\"message\":\"token rejected\"}}")));
    }

    private static EquinixClient core(Equinix session) {
        EquinixClient core = session.fabric().getEquinixClient();
        core.setEndPoint(wireMockUrl());
        core.setRetryPolicy(RetryPolicy.none());
        core.appendApiParams("json/core/apiParams_CoreTest.json");
        return core;
    }

    @Test
    @DisplayName("the session signs every request with the provider's token and never mints one")
    void sessionUsesProviderToken() throws Exception {
        try (Equinix session = new Equinix(BearerTokenProvider.of("good-token"))) {
            EquinixClient core = core(session);

            UnauthorizedRecoveryWireMockTest.getThing(core, "Things");
            UnauthorizedRecoveryWireMockTest.getThing(core, "Things");

            wireMock.verify(2, getRequestedFor(urlPathEqualTo(THING_PATH)).withHeader("authorization", equalTo("Bearer good-token")));
            wireMock.verify(0, postRequestedFor(urlPathEqualTo("/oauth2/v1/token")));
        }
    }

    @Test
    @DisplayName("after a 401 the provider is asked once more and the request replayed once")
    void unauthorized_asksProviderAgainAndReplays() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        BearerTokenProvider rotating = BearerTokenProvider.from(
                () -> BearerToken.of(calls.incrementAndGet() == 1 ? "stale-token" : "good-token"));
        try (Equinix session = new Equinix(rotating)) {
            EquinixClient core = core(session);

            UnauthorizedRecoveryWireMockTest.getThing(core, "Things");

            assertEquals(2, calls.get());
            wireMock.verify(2, getRequestedFor(urlPathEqualTo(THING_PATH)));
        }
    }

    @Test
    @DisplayName("a fixed token that keeps being rejected surfaces the second 401")
    void fixedRejectedToken_surfacesSecond401() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        BearerTokenProvider fixed = BearerTokenProvider.from(() -> {
            calls.incrementAndGet();
            return BearerToken.of("rejected-token");
        });
        try (Equinix session = new Equinix(fixed)) {
            EquinixClient core = core(session);

            EquinixAuthenticationException ex = assertThrows(EquinixAuthenticationException.class,
                    () -> UnauthorizedRecoveryWireMockTest.getThing(core, "Things"));

            assertTrue(ex.getMessage().contains("unauthenticated: token rejected"), ex.getMessage());
            assertEquals(2, calls.get());
            wireMock.verify(2, getRequestedFor(urlPathEqualTo(THING_PATH)));
        }
    }

    @Test
    @DisplayName("a token with a known expiry is replaced shortly before it expires (injected clock)")
    void knownExpiry_providerAskedAgainBeforeExpiry() throws Exception {
        ManualClock clock = new ManualClock(java.time.Instant.now());
        AtomicInteger calls = new AtomicInteger();
        BearerTokenProvider expiring = BearerTokenProvider.from(() -> {
            calls.incrementAndGet();
            return new BearerToken("good-token", clock.instant().plus(Duration.ofMinutes(10)));
        });
        try (Equinix session = new Equinix(expiring, EquinixConfig.builder().autoLoadMetros(false).build())) {
            EquinixClient core = core(session);
            core.setClock(clock);

            UnauthorizedRecoveryWireMockTest.getThing(core, "Things");
            clock.advance(Duration.ofMinutes(9));
            UnauthorizedRecoveryWireMockTest.getThing(core, "Things");
            assertEquals(1, calls.get(), "still valid 9 minutes in");

            clock.advance(Duration.ofSeconds(31)); // inside the 30 s safety margin
            UnauthorizedRecoveryWireMockTest.getThing(core, "Things");
            assertEquals(2, calls.get(), "refreshed inside the safety margin");
        }
    }

    @Test
    @DisplayName("the facade base class can be built over a provider, and explicit authenticate() asks it")
    void facadeConstructor_authenticateAsksProvider() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        try (com.eqixiac.equinix.EquinixClient facade = new com.eqixiac.equinix.EquinixClient(
                BearerTokenProvider.from(() -> { calls.incrementAndGet(); return BearerToken.of("good-token"); }))) {
            facade.authenticate();

            assertEquals(1, calls.get());
            assertEquals("good-token", facade.getEquinixClient().getOAuthToken().getSessionToken());
            assertNull(facade.getEquinixClient().getEquinixCredentialsProvider());
            wireMock.verify(0, postRequestedFor(urlPathEqualTo("/oauth2/v1/token")));
        }
    }

    @Test
    @DisplayName("a supplier returning null fails clearly; a blank token is rejected at construction")
    void invalidTokens() {
        assertThrows(EquinixClientException.class, () -> BearerTokenProvider.from(() -> null).getToken());
        assertThrows(IllegalArgumentException.class, () -> BearerToken.of(" "));
        assertFalse(BearerToken.of("secret-value").toString().contains("secret-value"));
    }
}
