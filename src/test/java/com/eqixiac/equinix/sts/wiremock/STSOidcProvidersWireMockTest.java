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

package com.eqixiac.equinix.sts.wiremock;

import com.eqixiac.equinix.STS;
import com.eqixiac.equinix.core.WireMockTestBase;
import com.eqixiac.equinix.core.exception.EquinixConflictException;
import com.eqixiac.equinix.core.exception.EquinixNotFoundException;
import com.eqixiac.equinix.core.exception.EquinixServerException;
import com.eqixiac.equinix.sts.enums.ProviderStatus;
import com.eqixiac.equinix.sts.model.OidcProvider;
import com.eqixiac.equinix.sts.model.json.OidcProviderPage;
import com.eqixiac.equinix.sts.model.json.creators.CreateOidcProviderRequest;
import com.eqixiac.equinix.sts.model.json.creators.PatchOidcProviderRequest;
import org.junit.jupiter.api.*;

import java.util.List;

import static com.eqixiac.equinix.core.ResponseStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for the STS OIDC provider client ({@code sts.oidcProviders()}).
 *
 * <p>The {@code OidcProviders} functional area uses {@code overrideUriFormat: v{$version}/{$requestUri}}
 * with {@code defaultVersion = 1}, so every operation resolves under
 * {@code /v1/projects/{projectId}/oidcProviders}. Exercises the full lifecycle exposed by
 * {@link com.eqixiac.equinix.sts.client.STSOidcProviders}:</p>
 * <ul>
 *   <li>{@code list(...)} — GET (operationId {@code pageOidcProviders}), including the empty-list
 *       case, the {@code includeSuspended}/{@code pageSize} query params and opaque-token
 *       ({@code nextPageToken}) two-page paging.</li>
 *   <li>{@code create(...)} — POST (operationId {@code createOidcProvider}), asserting the request body.</li>
 *   <li>{@code patch(...)} — PATCH (operationId {@code patchOidcProvider}), asserting the request body
 *       including the {@code lastRev} concurrency guard.</li>
 *   <li>{@code delete(...)} — DELETE returning 204 No Content (operationId {@code deleteOidcProvider}).</li>
 *   <li>{@code suspend(...)} / {@code resume(...)} — POST sub-resource actions (operationIds
 *       {@code suspendOidcProvider} / {@code resumeOidcProvider}).</li>
 *   <li>Error mapping (404 / 409 / 500).</li>
 * </ul>
 *
 * <p>Identifiers here intentionally omit the spec's {@code project:} / {@code idp:} prefixes (the SDK
 * does not enforce those patterns) so that path-segment matching is unambiguous — path parameters are
 * substituted into the URI verbatim.</p>
 */
class STSOidcProvidersWireMockTest extends WireMockTestBase {

    private static final String PROJECT_ID = "project-abc123";
    private static final String IDP_ID = "github-actions-idp";
    private static final String BASE = "/v1/projects/" + PROJECT_ID + "/oidcProviders";

    static STS sts;

    @BeforeAll
    static void setUp() {
        sts = new STS(testCredentials());
        redirectToWireMock(sts);
        sts.authenticate();
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (sts != null) sts.close();
    }

    @BeforeEach
    void resetBeforeEach() {
        resetStubs();
    }

    @Nested
    @DisplayName("page()")
    class Page {

        @Test
        @DisplayName("GETs the providers of a project and parses the page")
        void pagesProviders() {
            stubPaginatedGet(wireMock, BASE, "/json/sts/oidc_providers_page1.json");

            OidcProviderPage page = sts.oidcProviders().list(PROJECT_ID);

            assertNotNull(page);
            assertNotNull(page.getList());
            assertEquals(2, page.getList().size());
            assertEquals("idp:github-actions", page.getList().get(0).getIdpId());
            assertEquals(ProviderStatus.ENABLED, page.getList().get(0).getStatus());
            assertEquals("idp:gitlab-ci", page.getList().get(1).getIdpId());
            assertEquals(ProviderStatus.SUSPENDED, page.getList().get(1).getStatus());
            assertEquals("eyJvZmZzZXQiOjJ9", page.getNextPageToken());

            wireMock.verify(getRequestedFor(urlPathEqualTo(BASE)));
        }

        @Test
        @DisplayName("returns an empty page when the project has no providers")
        void emptyPage() {
            stubPaginatedGet(wireMock, BASE, "/json/sts/oidc_providers_empty.json");

            OidcProviderPage page = sts.oidcProviders().list(PROJECT_ID);

            assertNotNull(page);
            assertNotNull(page.getList());
            assertTrue(page.getList().isEmpty());
            assertNull(page.getNextPageToken());

            wireMock.verify(getRequestedFor(urlPathEqualTo(BASE)));
        }

        @Test
        @DisplayName("passes includeSuspended / pageToken / pageSize as query params")
        void passesQueryParams() {
            stubPaginatedGet(wireMock, BASE, "/json/sts/oidc_providers_page1.json");

            sts.oidcProviders().list(PROJECT_ID, true, "tok-123", 50);

            wireMock.verify(getRequestedFor(urlPathEqualTo(BASE))
                    .withQueryParam("includeSuspended", equalTo("true"))
                    .withQueryParam("pageToken", equalTo("tok-123"))
                    .withQueryParam("pageSize", equalTo("50")));
        }

        @Test
        @DisplayName("opaque-token paging walks page 1 -> page 2 via nextPageToken")
        void walksTwoPages() {
            // First page: no pageToken query param, returns nextPageToken.
            wireMock.stubFor(get(urlPathEqualTo(BASE))
                    .withQueryParam("pageToken", absent())
                    .willReturn(okJson(loadFixture("/json/sts/oidc_providers_page1.json"))));
            // Second page: pageToken echoed back from the first response, terminal (null nextPageToken).
            wireMock.stubFor(get(urlPathEqualTo(BASE))
                    .withQueryParam("pageToken", equalTo("eyJvZmZzZXQiOjJ9"))
                    .willReturn(okJson(loadFixture("/json/sts/oidc_providers_page2.json"))));

            OidcProviderPage first = sts.oidcProviders().list(PROJECT_ID);
            assertEquals("eyJvZmZzZXQiOjJ9", first.getNextPageToken());

            OidcProviderPage second = sts.oidcProviders().list(PROJECT_ID, null, first.getNextPageToken(), null);
            assertNotNull(second);
            assertEquals(1, second.getList().size());
            assertEquals("idp:okta-workforce", second.getList().get(0).getIdpId());
            assertNull(second.getNextPageToken());

            wireMock.verify(getRequestedFor(urlPathEqualTo(BASE)).withQueryParam("pageToken", absent()));
            wireMock.verify(getRequestedFor(urlPathEqualTo(BASE))
                    .withQueryParam("pageToken", equalTo("eyJvZmZzZXQiOjJ9")));
        }

        @Test
        @DisplayName("500 throws EquinixServerException")
        void serverError() {
            stubErrorInline(wireMock, BASE, 500,
                    "{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}");

            assertThrows(EquinixServerException.class, () -> sts.oidcProviders().list(PROJECT_ID));

            wireMock.verify(getRequestedFor(urlPathEqualTo(BASE)));
        }
    }

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("POSTs the registration body and returns the created provider")
        void createsProvider() {
            stubCreate(wireMock, BASE, "/json/sts/oidc_provider_response.json");

            OidcProvider created = sts.oidcProviders().create(PROJECT_ID,
                    new CreateOidcProviderRequest()
                            .name("GitHub Actions OIDC")
                            .idpPrefix("github-actions")
                            .issuerLocation("https://token.actions.githubusercontent.com")
                            .trustedClientIds(List.of("my-oauth-client-id", "ci-deploy-client"))
                            .groupMembershipClaim("groups"));

            assertNotNull(created);
            assertEquals("idp:github-actions", created.getIdpId());
            assertEquals("GitHub Actions OIDC", created.getName());
            assertEquals(ProviderStatus.ENABLED, created.getStatus());
            assertEquals("abc123", created.getRev());
            assertEquals(List.of("my-oauth-client-id", "ci-deploy-client"), created.getTrustedClientIds());

            wireMock.verify(postRequestedFor(urlPathEqualTo(BASE))
                    .withRequestBody(matchingJsonPath("$.name", equalTo("GitHub Actions OIDC")))
                    .withRequestBody(matchingJsonPath("$.idpPrefix", equalTo("github-actions")))
                    .withRequestBody(matchingJsonPath("$.issuerLocation",
                            equalTo("https://token.actions.githubusercontent.com")))
                    .withRequestBody(matchingJsonPath("$.groupMembershipClaim", equalTo("groups")))
                    .withRequestBody(matchingJsonPath("$.trustedClientIds[0]", equalTo("my-oauth-client-id")))
                    .withRequestBody(matchingJsonPath("$.trustedClientIds[1]", equalTo("ci-deploy-client"))));
        }

        @Test
        @DisplayName("409 throws EquinixConflictException")
        void conflict() {
            stubError(wireMock, BASE, 409, "/json/core/error_409_response.json");

            assertThrows(EquinixConflictException.class, () -> sts.oidcProviders().create(PROJECT_ID,
                    new CreateOidcProviderRequest()
                            .name("Duplicate")
                            .idpPrefix("github-actions")
                            .issuerLocation("https://token.actions.githubusercontent.com")
                            .trustedClientIds(List.of("my-oauth-client-id"))));

            wireMock.verify(postRequestedFor(urlPathEqualTo(BASE)));
        }
    }

    @Nested
    @DisplayName("patch()")
    class Patch {

        @Test
        @DisplayName("PATCHes the provider by id and returns the updated provider")
        void patchesProvider() {
            wireMock.stubFor(patch(urlPathEqualTo(BASE + "/" + IDP_ID))
                    .willReturn(okJson(loadFixture("/json/sts/oidc_provider_patched_response.json"))));

            OidcProvider updated = sts.oidcProviders().update(PROJECT_ID, IDP_ID,
                    new PatchOidcProviderRequest()
                            .name("GitHub Actions OIDC (renamed)")
                            .trustedClientIds(List.of("my-oauth-client-id"))
                            .lastRev("abc123"));

            assertNotNull(updated);
            assertEquals("GitHub Actions OIDC (renamed)", updated.getName());
            assertEquals("def456", updated.getRev());

            wireMock.verify(patchRequestedFor(urlPathEqualTo(BASE + "/" + IDP_ID))
                    .withRequestBody(matchingJsonPath("$.name", equalTo("GitHub Actions OIDC (renamed)")))
                    .withRequestBody(matchingJsonPath("$.lastRev", equalTo("abc123")))
                    .withRequestBody(matchingJsonPath("$.trustedClientIds[0]", equalTo("my-oauth-client-id"))));
        }

        @Test
        @DisplayName("unsetGroupMembershipClaim sends {\"$unset\": true}")
        void patchUnsetsClaim() {
            wireMock.stubFor(patch(urlPathEqualTo(BASE + "/" + IDP_ID))
                    .willReturn(okJson(loadFixture("/json/sts/oidc_provider_patched_response.json"))));

            sts.oidcProviders().update(PROJECT_ID, IDP_ID,
                    new PatchOidcProviderRequest().lastRev("abc123").unsetGroupMembershipClaim());

            wireMock.verify(patchRequestedFor(urlPathEqualTo(BASE + "/" + IDP_ID))
                    .withRequestBody(matchingJsonPath("$.groupMembershipClaim['$unset']", equalTo("true")))
                    .withRequestBody(matchingJsonPath("$.lastRev", equalTo("abc123"))));
        }

        @Test
        @DisplayName("404 throws EquinixNotFoundException")
        void notFound() {
            stubError(wireMock, BASE + "/.*", 404, "/json/core/error_404_response.json");

            assertThrows(EquinixNotFoundException.class, () -> sts.oidcProviders().update(PROJECT_ID, IDP_ID,
                    new PatchOidcProviderRequest().name("nope").lastRev("abc123")));

            wireMock.verify(patchRequestedFor(urlPathEqualTo(BASE + "/" + IDP_ID)));
        }
    }

    @Nested
    @DisplayName("delete()")
    class Delete {

        @Test
        @DisplayName("DELETEs the provider and returns true on 204 No Content")
        void deletesProvider() {
            stubDeleteNoContent(wireMock, BASE + "/" + IDP_ID);

            Boolean deleted = sts.oidcProviders().delete(PROJECT_ID, IDP_ID);

            assertEquals(Boolean.TRUE, deleted);

            wireMock.verify(deleteRequestedFor(urlPathEqualTo(BASE + "/" + IDP_ID)));
        }

        @Test
        @DisplayName("404 throws EquinixNotFoundException")
        void notFound() {
            stubError(wireMock, BASE + "/.*", 404, "/json/core/error_404_response.json");

            assertThrows(EquinixNotFoundException.class,
                    () -> sts.oidcProviders().delete(PROJECT_ID, IDP_ID));

            wireMock.verify(deleteRequestedFor(urlPathEqualTo(BASE + "/" + IDP_ID)));
        }
    }

    @Nested
    @DisplayName("suspend() / resume()")
    class SuspendResume {

        @Test
        @DisplayName("POSTs {idpId}/suspend and returns true")
        void suspendsProvider() {
            wireMock.stubFor(post(urlPathEqualTo(BASE + "/" + IDP_ID + "/suspend"))
                    .willReturn(aResponse().withStatus(204)));

            Boolean suspended = sts.oidcProviders().suspend(PROJECT_ID, IDP_ID);

            assertEquals(Boolean.TRUE, suspended);

            wireMock.verify(postRequestedFor(urlPathEqualTo(BASE + "/" + IDP_ID + "/suspend")));
        }

        @Test
        @DisplayName("POSTs {idpId}/resume and returns true")
        void resumesProvider() {
            wireMock.stubFor(post(urlPathEqualTo(BASE + "/" + IDP_ID + "/resume"))
                    .willReturn(aResponse().withStatus(204)));

            Boolean resumed = sts.oidcProviders().resume(PROJECT_ID, IDP_ID);

            assertEquals(Boolean.TRUE, resumed);

            wireMock.verify(postRequestedFor(urlPathEqualTo(BASE + "/" + IDP_ID + "/resume")));
        }

        @Test
        @DisplayName("suspend 409 throws EquinixConflictException")
        void suspendConflict() {
            stubError(wireMock, BASE + "/.*/suspend", 409, "/json/core/error_409_response.json");

            assertThrows(EquinixConflictException.class,
                    () -> sts.oidcProviders().suspend(PROJECT_ID, IDP_ID));

            wireMock.verify(postRequestedFor(urlPathEqualTo(BASE + "/" + IDP_ID + "/suspend")));
        }
    }
}
