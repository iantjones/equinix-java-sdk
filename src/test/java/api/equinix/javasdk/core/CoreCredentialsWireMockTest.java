package api.equinix.javasdk.core;

import api.equinix.javasdk.Fabric;
import api.equinix.javasdk.core.auth.BasicEquinixCredentials;
import api.equinix.javasdk.core.auth.EquinixCredentials;
import api.equinix.javasdk.core.auth.EquinixCredentialsProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * Verifies the token request body for the client-credentials grant (the only grant the Equinix
 * token endpoint still supports) and that a custom {@link EquinixCredentialsProvider} is consulted
 * on every authentication. Bodies are matched with
 * {@code equalToJson(..., ignoreExtraElements = false)} so an unexpected or omitted field fails
 * the test.
 */
class CoreCredentialsWireMockTest extends WireMockTestBase {

    @BeforeEach
    void clean() {
        resetStubs();
    }

    @AfterEach
    void resetCount() {
        wireMock.resetRequests();
    }

    @Test
    @DisplayName("client-credentials grant sends exactly client_id, client_secret, grant_type")
    void clientCredentialsGrant_body() throws Exception {
        try (Fabric fabric = new Fabric(new BasicEquinixCredentials("cc-id", "cc-secret"))) {
            redirectToWireMock(fabric);
            fabric.authenticate();

            wireMock.verify(postRequestedFor(urlPathEqualTo("/oauth2/v1/token"))
                    .withRequestBody(equalToJson(
                            "{\"client_id\":\"cc-id\",\"client_secret\":\"cc-secret\","
                                    + "\"grant_type\":\"client_credentials\"}", true, false)));
        }
    }

    @Test
    @DisplayName("a custom credentials provider supplies the credentials serialized in the token request")
    void customProvider_isUsed() throws Exception {
        EquinixCredentialsProvider provider = () -> new BasicEquinixCredentials("vault-id", "vault-secret");
        try (Fabric fabric = new Fabric(provider)) {
            redirectToWireMock(fabric);
            fabric.authenticate();

            wireMock.verify(postRequestedFor(urlPathEqualTo("/oauth2/v1/token"))
                    .withRequestBody(matchingJsonPath("$.client_id", equalTo("vault-id")))
                    .withRequestBody(matchingJsonPath("$.client_secret", equalTo("vault-secret"))));
        }
    }

    @Test
    @DisplayName("the provider is re-consulted on each authentication, so rotated credentials take effect")
    void customProvider_rotation() throws Exception {
        // The provider returns a rotated secret on its second call; authenticating twice must send both.
        EquinixCredentialsProvider rotating = new EquinixCredentialsProvider() {
            private int calls = 0;
            @Override
            public EquinixCredentials getCredentials() {
                calls++;
                return calls == 1
                        ? new BasicEquinixCredentials("id", "secret-v1")
                        : new BasicEquinixCredentials("id", "secret-v2");
            }
        };

        try (Fabric fabric = new Fabric(rotating)) {
            redirectToWireMock(fabric);
            fabric.authenticate();
            fabric.authenticate();

            wireMock.verify(postRequestedFor(urlPathEqualTo("/oauth2/v1/token"))
                    .withRequestBody(matchingJsonPath("$.client_secret", equalTo("secret-v1"))));
            wireMock.verify(postRequestedFor(urlPathEqualTo("/oauth2/v1/token"))
                    .withRequestBody(matchingJsonPath("$.client_secret", equalTo("secret-v2"))));
        }
    }
}
