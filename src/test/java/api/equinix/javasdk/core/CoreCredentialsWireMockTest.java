package api.equinix.javasdk.core;

import api.equinix.javasdk.Fabric;
import api.equinix.javasdk.core.auth.BasicEquinixCredentials;
import api.equinix.javasdk.core.auth.EquinixCredentials;
import api.equinix.javasdk.core.auth.EquinixCredentialsProvider;
import api.equinix.javasdk.core.auth.PasswordEquinixCredentials;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * Verifies the token request body for the supported grant types and that a custom
 * {@link EquinixCredentialsProvider} is consulted on every authentication. Bodies are matched with
 * {@code equalToJson(..., ignoreExtraElements = false)} so an unexpected or omitted field fails the
 * test (e.g. proving {@code password_encoding} is absent when none is supplied).
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
    @DisplayName("password grant sends grant_type=password with user credentials; no password_encoding by default")
    void passwordGrant_body() throws Exception {
        PasswordEquinixCredentials credentials =
                new PasswordEquinixCredentials("pw-id", "pw-secret", "user@example.com", "s3cret");
        try (Fabric fabric = new Fabric(credentials)) {
            redirectToWireMock(fabric);
            fabric.authenticate();

            wireMock.verify(postRequestedFor(urlPathEqualTo("/oauth2/v1/token"))
                    .withRequestBody(equalToJson(
                            "{\"client_id\":\"pw-id\",\"client_secret\":\"pw-secret\","
                                    + "\"user_name\":\"user@example.com\",\"user_password\":\"s3cret\","
                                    + "\"grant_type\":\"password\"}", true, false)));
        }
    }

    @Test
    @DisplayName("password grant includes password_encoding when supplied")
    void passwordGrant_withEncoding() throws Exception {
        try (Fabric fabric = new Fabric(
                new PasswordEquinixCredentials("id", "secret", "user", "aGFzaA==", "md5-b64"))) {
            redirectToWireMock(fabric);
            fabric.authenticate();

            wireMock.verify(postRequestedFor(urlPathEqualTo("/oauth2/v1/token"))
                    .withRequestBody(equalToJson(
                            "{\"client_id\":\"id\",\"client_secret\":\"secret\",\"user_name\":\"user\","
                                    + "\"user_password\":\"aGFzaA==\",\"grant_type\":\"password\","
                                    + "\"password_encoding\":\"md5-b64\"}", true, false)));
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
