package com.eqixiac.equinix.iam.wiremock;

import com.eqixiac.equinix.IAM;
import com.eqixiac.equinix.core.auth.BearerToken;
import com.eqixiac.equinix.core.auth.BearerTokenProvider;
import com.eqixiac.equinix.core.WireMockTestBase;
import com.eqixiac.equinix.core.exception.EquinixAuthenticationException;
import com.eqixiac.equinix.core.exception.EquinixClientException;
import com.eqixiac.equinix.core.exception.EquinixNotFoundException;
import com.eqixiac.equinix.iam.client.ProjectIds;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * {@code new IAM(BearerTokenProvider)}: project-scoped IAM endpoints need an STS access-policy
 * token (live 2026-11-11), so the client sends the supplied token and never logs in with client
 * credentials.
 */
class IAMAccessPolicyTokenWireMockTest extends WireMockTestBase {

    private static final String PROJECT_UUID = "abc-123";
    private static final String BASE_PATH = "/v1/projects/project:abc-123/permissionSets";

    @BeforeEach
    void resetBeforeEach() {
        resetStubs();
    }

    @Test
    void sendsSuppliedTokenAndNeverCallsTheOAuthEndpoint() throws Exception {
        wireMock.stubFor(get(urlPathEqualTo(BASE_PATH))
                .willReturn(okJson(loadFixture("/json/iam/permission_set_list_empty.json"))));
        AtomicInteger calls = new AtomicInteger();

        try (IAM iam = new IAM(BearerTokenProvider.from(
                () -> BearerToken.of("ap-token-" + calls.incrementAndGet())))) {
            redirectToWireMock(iam);
            iam.permissionSets().list(ProjectIds.of(PROJECT_UUID));
            iam.permissionSets().list(ProjectIds.of("project:" + PROJECT_UUID));
        }

        wireMock.verify(getRequestedFor(urlPathEqualTo(BASE_PATH))
                .withHeader("Authorization", equalTo("Bearer ap-token-1")));
        wireMock.verify(0, postRequestedFor(urlPathEqualTo("/oauth2/v1/token")));
        assertEquals(1, calls.get(), "a token with no stated expiry is reused until the service rejects it");
    }

    @Test
    void blankTokenFailsWithoutARequest() throws Exception {
        try (IAM iam = new IAM(BearerTokenProvider.from(() -> BearerToken.of(" ")))) {
            redirectToWireMock(iam);
            assertThrows(RuntimeException.class, () -> iam.permissionSets().list("project:abc-123"));
        }
        wireMock.verify(0, getRequestedFor(urlPathEqualTo(BASE_PATH)));
    }

    @Test
    void liveErrorShapesMapToTypedExceptions() throws Exception {
        // Shapes observed live 2026-11-11 (values shortened): a client-credentials token on a
        // project-scoped path, and an unknown role assignment.
        wireMock.stubFor(get(urlPathEqualTo(BASE_PATH)).willReturn(aResponse().withStatus(401)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"error\":{\"action\":\"action:use/listPermissionSets\",\"authToken\":"
                        + "{\"cloud-id\":\"cloud:eqix\",\"scopes\":[{\"scope-type\":\"role-assignments\"}]}}}")));
        wireMock.stubFor(get(urlPathEqualTo("/v1/roleAssignments/zzz")).willReturn(aResponse().withStatus(404)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"error\":[\"Not Found\"]}")));

        try (IAM iam = new IAM(testCredentials())) {
            redirectToWireMock(iam);
            assertThrows(EquinixAuthenticationException.class, () -> iam.permissionSets().list("project:abc-123"));
            assertThrows(EquinixNotFoundException.class, () -> iam.roleAssignments().getByUuid("zzz"));
        }
    }
}
