package api.equinix.javasdk.mcp.auth;

import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.mcp.McpException;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.notMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * WireMock tests for {@link McpLogin}: RFC 7591 dynamic client registration (with the
 * documented device-grant-rejection fallback), the RFC 8628 device-authorization flow
 * with {@code authorization_pending}/{@code slow_down} polling semantics, and the
 * {@code .env.local} writeback.
 */
@Tag("wiremock")
class McpLoginWireMockTest extends WireMockTestBase {

    private static final String DEVICE_GRANT_ENCODED =
            "grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Adevice_code";
    private static final String SCOPE_ENCODED = "scope=openid+profile+email+api%3Atemporary-full";

    private final ByteArrayOutputStream consoleSink = new ByteArrayOutputStream();
    private final RecordingSleeper sleeper = new RecordingSleeper();

    @BeforeEach
    void cleanStubs() {
        resetStubs();
    }

    /** Sleeper that records requested sleeps without actually sleeping. */
    static final class RecordingSleeper implements McpLogin.Sleeper {
        final List<Long> sleepsMillis = new ArrayList<>();

        @Override
        public void sleep(long millis) {
            sleepsMillis.add(millis);
        }
    }

    private McpLogin newLogin(String prefix, Path envFile) {
        return new McpLogin(
                wireMockUrl() + prefix + "/connect/register",
                wireMockUrl() + prefix + "/oauth2/device_authorization",
                wireMockUrl() + prefix + "/oauth2/token",
                envFile,
                new PrintStream(consoleSink, true, StandardCharsets.UTF_8),
                sleeper);
    }

    private String console() {
        return consoleSink.toString(StandardCharsets.UTF_8);
    }

    private void stubRegistration(String prefix, String clientId) {
        wireMock.stubFor(post(urlPathEqualTo(prefix + "/connect/register"))
                .willReturn(aResponse().withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"client_id\":\"" + clientId + "\",\"client_name\":\"equinix-java-sdk\"}")));
    }

    private void stubDeviceAuthorization(String prefix) {
        wireMock.stubFor(post(urlPathEqualTo(prefix + "/oauth2/device_authorization"))
                .willReturn(okJson("{"
                        + "\"device_code\":\"dev-1\","
                        + "\"user_code\":\"WDJB-MJHT\","
                        + "\"verification_uri\":\"https://as.equinix.com/activate\","
                        + "\"verification_uri_complete\":\"https://as.equinix.com/activate?user_code=WDJB-MJHT\","
                        + "\"interval\":5,"
                        + "\"expires_in\":600}")));
    }

    private static com.github.tomakehurst.wiremock.http.HttpHeader jsonHeader() {
        return new com.github.tomakehurst.wiremock.http.HttpHeader("Content-Type", "application/json");
    }

    @Test
    @DisplayName("Full device-code flow: DCR, pending + slow_down polling, token grant, .env.local writeback preserving other lines")
    void fullDeviceFlowWritesEnvFile(@TempDir Path tempDir) throws Exception {
        String prefix = "/t1";
        Path envFile = tempDir.resolve(".env.local");
        Files.write(envFile, List.of(
                "# local secrets - do not commit",
                "EQUINIX_ACCESS_KEY=ak-123",
                "EQUINIX_MCP_CLIENT_ID=",
                "EQUINIX_MCP_REFRESH_TOKEN=stale-token"), StandardCharsets.UTF_8);

        stubRegistration(prefix, "dyn-client-1");
        stubDeviceAuthorization(prefix);

        String tokenPath = prefix + "/oauth2/token";
        wireMock.stubFor(post(urlPathEqualTo(tokenPath)).inScenario("t1-poll")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(400).withHeaders(
                                new com.github.tomakehurst.wiremock.http.HttpHeaders(jsonHeader()))
                        .withBody("{\"error\":\"authorization_pending\"}"))
                .willSetStateTo("second-poll"));
        wireMock.stubFor(post(urlPathEqualTo(tokenPath)).inScenario("t1-poll")
                .whenScenarioStateIs("second-poll")
                .willReturn(aResponse().withStatus(400).withHeaders(
                                new com.github.tomakehurst.wiremock.http.HttpHeaders(jsonHeader()))
                        .withBody("{\"error\":\"slow_down\"}"))
                .willSetStateTo("third-poll"));
        wireMock.stubFor(post(urlPathEqualTo(tokenPath)).inScenario("t1-poll")
                .whenScenarioStateIs("third-poll")
                .willReturn(okJson("{\"access_token\":\"at-live\",\"refresh_token\":\"rt-live\","
                        + "\"token_type\":\"Bearer\",\"expires_in\":300,"
                        + "\"scope\":\"openid profile email api:temporary-full\"}")));

        newLogin(prefix, envFile).login();

        // .env.local: values written, every other line preserved, stale value replaced.
        List<String> lines = Files.readAllLines(envFile, StandardCharsets.UTF_8);
        assertTrue(lines.contains("# local secrets - do not commit"));
        assertTrue(lines.contains("EQUINIX_ACCESS_KEY=ak-123"));
        assertTrue(lines.contains("EQUINIX_MCP_CLIENT_ID=dyn-client-1"));
        assertTrue(lines.contains("EQUINIX_MCP_REFRESH_TOKEN=rt-live"));
        assertFalse(String.join("\n", lines).contains("stale-token"));

        // RFC 8628 pacing: 5s, 5s, then 10s after slow_down.
        assertEquals(List.of(5000L, 5000L, 10000L), sleeper.sleepsMillis);

        // The user was clearly told where to go and which code to expect.
        assertTrue(console().contains("https://as.equinix.com/activate?user_code=WDJB-MJHT"));
        assertTrue(console().contains("WDJB-MJHT"));

        // RFC 7591 registration body.
        wireMock.verify(1, postRequestedFor(urlPathEqualTo(prefix + "/connect/register"))
                .withRequestBody(matchingJsonPath("$.client_name", equalTo("equinix-java-sdk")))
                .withRequestBody(matchingJsonPath("$.token_endpoint_auth_method", equalTo("none")))
                .withRequestBody(matchingJsonPath("$.grant_types[0]",
                        equalTo("urn:ietf:params:oauth:grant-type:device_code")))
                .withRequestBody(matchingJsonPath("$.grant_types[1]", equalTo("refresh_token")))
                .withRequestBody(matchingJsonPath("$.scope",
                        equalTo("openid profile email api:temporary-full"))));

        // RFC 8628 device-authorization request: form-encoded, client_id + scope.
        wireMock.verify(1, postRequestedFor(urlPathEqualTo(prefix + "/oauth2/device_authorization"))
                .withHeader("Content-Type", containing("application/x-www-form-urlencoded"))
                .withRequestBody(containing("client_id=dyn-client-1"))
                .withRequestBody(containing(SCOPE_ENCODED)));

        // Token polling: form-encoded device-code grant, public client (no secret), 3 polls.
        wireMock.verify(3, postRequestedFor(urlPathEqualTo(tokenPath))
                .withHeader("Content-Type", containing("application/x-www-form-urlencoded"))
                .withRequestBody(containing(DEVICE_GRANT_ENCODED))
                .withRequestBody(containing("device_code=dev-1"))
                .withRequestBody(containing("client_id=dyn-client-1"))
                .withRequestBody(notMatching(".*client_secret.*")));
    }

    @Test
    @DisplayName("When .env.local is absent it is created from .env.local.example, preserving the example's lines")
    void envFileCreatedFromExample(@TempDir Path tempDir) throws Exception {
        String prefix = "/t2";
        Path envFile = tempDir.resolve(".env.local");
        Files.write(tempDir.resolve(".env.local.example"), List.of(
                "# Example env - copy me",
                "EQUINIX_ACCESS_KEY=",
                "EQUINIX_MCP_CLIENT_ID=",
                "EQUINIX_MCP_REFRESH_TOKEN="), StandardCharsets.UTF_8);

        stubRegistration(prefix, "dyn-client-2");
        stubDeviceAuthorization(prefix);
        wireMock.stubFor(post(urlPathEqualTo(prefix + "/oauth2/token"))
                .willReturn(okJson("{\"access_token\":\"at-2\",\"refresh_token\":\"rt-2\",\"expires_in\":300}")));

        newLogin(prefix, envFile).login();

        assertTrue(Files.isRegularFile(envFile));
        List<String> lines = Files.readAllLines(envFile, StandardCharsets.UTF_8);
        assertTrue(lines.contains("# Example env - copy me"));
        assertTrue(lines.contains("EQUINIX_ACCESS_KEY="));
        assertTrue(lines.contains("EQUINIX_MCP_CLIENT_ID=dyn-client-2"));
        assertTrue(lines.contains("EQUINIX_MCP_REFRESH_TOKEN=rt-2"));
    }

    @Test
    @DisplayName("DCR device-grant rejection retries with authorization_code; a device-authorization refusal then fails loud with auth-code guidance")
    void dcrDeviceGrantRejectedThenDeviceFlowRefused(@TempDir Path tempDir) {
        String prefix = "/t3";
        Path envFile = tempDir.resolve(".env.local");

        String registerPath = prefix + "/connect/register";
        wireMock.stubFor(post(urlPathEqualTo(registerPath)).inScenario("t3-reg")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(400).withHeaders(
                                new com.github.tomakehurst.wiremock.http.HttpHeaders(jsonHeader()))
                        .withBody("{\"error\":\"invalid_client_metadata\","
                                + "\"error_description\":\"device_code grant not allowed\"}"))
                .willSetStateTo("fallback"));
        wireMock.stubFor(post(urlPathEqualTo(registerPath)).inScenario("t3-reg")
                .whenScenarioStateIs("fallback")
                .willReturn(aResponse().withStatus(201).withHeaders(
                                new com.github.tomakehurst.wiremock.http.HttpHeaders(jsonHeader()))
                        .withBody("{\"client_id\":\"authcode-client\"}")));

        wireMock.stubFor(post(urlPathEqualTo(prefix + "/oauth2/device_authorization"))
                .willReturn(aResponse().withStatus(400).withHeaders(
                                new com.github.tomakehurst.wiremock.http.HttpHeaders(jsonHeader()))
                        .withBody("{\"error\":\"unauthorized_client\"}")));

        McpException ex = assertThrows(McpException.class, () -> newLogin(prefix, envFile).login());

        assertTrue(ex.getMessage().contains("unauthorized_client"), "message should carry the AS refusal");
        assertTrue(ex.getMessage().contains("PKCE"), "message should carry the authorization-code guidance");
        assertTrue(console().contains("Retrying registration with authorization_code"),
                "the user should be told about the registration fallback");

        // The fallback registration asked for authorization_code + refresh_token, not device_code.
        wireMock.verify(2, postRequestedFor(urlPathEqualTo(registerPath)));
        wireMock.verify(1, postRequestedFor(urlPathEqualTo(registerPath))
                .withRequestBody(matchingJsonPath("$.grant_types[0]", equalTo("authorization_code")))
                .withRequestBody(matchingJsonPath("$.grant_types[1]", equalTo("refresh_token"))));

        assertFalse(Files.exists(envFile), "no credentials should be written on failure");
    }

    @Test
    @DisplayName("access_denied during polling fails loud and writes nothing")
    void accessDeniedFailsLoud(@TempDir Path tempDir) {
        String prefix = "/t4";
        Path envFile = tempDir.resolve(".env.local");

        stubRegistration(prefix, "dyn-client-4");
        stubDeviceAuthorization(prefix);
        wireMock.stubFor(post(urlPathEqualTo(prefix + "/oauth2/token"))
                .willReturn(aResponse().withStatus(400).withHeaders(
                                new com.github.tomakehurst.wiremock.http.HttpHeaders(jsonHeader()))
                        .withBody("{\"error\":\"access_denied\"}")));

        McpException ex = assertThrows(McpException.class, () -> newLogin(prefix, envFile).login());

        assertTrue(ex.getMessage().contains("denied"));
        assertFalse(Files.exists(envFile), "no credentials should be written on denial");
    }

    @Test
    @DisplayName("A token grant without a refresh_token fails loud (non-interactive re-auth would be impossible)")
    void missingRefreshTokenFailsLoud(@TempDir Path tempDir) {
        String prefix = "/t5";
        Path envFile = tempDir.resolve(".env.local");

        stubRegistration(prefix, "dyn-client-5");
        stubDeviceAuthorization(prefix);
        wireMock.stubFor(post(urlPathEqualTo(prefix + "/oauth2/token"))
                .willReturn(okJson("{\"access_token\":\"at-only\",\"expires_in\":300}")));

        McpException ex = assertThrows(McpException.class, () -> newLogin(prefix, envFile).login());

        assertTrue(ex.getMessage().contains("refresh_token"));
        assertFalse(Files.exists(envFile));
    }
}
