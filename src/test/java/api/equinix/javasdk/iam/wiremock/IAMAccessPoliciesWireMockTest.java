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

package api.equinix.javasdk.iam.wiremock;

import api.equinix.javasdk.IAM;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.exception.EquinixConflictException;
import api.equinix.javasdk.core.exception.EquinixNotFoundException;
import api.equinix.javasdk.iam.model.AccessPolicy;
import api.equinix.javasdk.iam.model.AccessPolicyGrant;
import api.equinix.javasdk.iam.model.PolicyExpression;
import api.equinix.javasdk.iam.model.json.AccessPolicyGrantList;
import api.equinix.javasdk.iam.model.json.AccessPolicyList;
import api.equinix.javasdk.iam.model.json.creators.AddGrantRequest;
import api.equinix.javasdk.iam.model.json.creators.CreateAccessPolicyRequest;
import api.equinix.javasdk.iam.model.json.creators.UpdateAccessPolicyRequest;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Map;

import static api.equinix.javasdk.core.ResponseStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based request-contract tests for {@link api.equinix.javasdk.iam.client.IAMAccessPolicies}
 * ({@code iam.accessPolicies()}), covering the project-scoped access-policy lifecycle and grant
 * management under {@code /v1/projects/{projectId}/accessPolicies}.
 *
 * <p>Exercises {@code list} (page + empty), {@code getByUuid}, {@code create} (POST body asserted),
 * {@code update} (PATCH body asserted), {@code enable} / {@code disable}, {@code delete} (DELETE with
 * a {@code lastRev} body), and the grants sub-resource {@code listGrants} / {@code addGrant} /
 * {@code removeGrant}, plus 404 and 409 error mappings. Every operation asserts both the HTTP verb and
 * the resolved path via {@code wireMock.verify(...)}.</p>
 */
class IAMAccessPoliciesWireMockTest extends WireMockTestBase {

    private static final String PROJECT_ID = "proj-123";
    private static final String POLICY_ID = "accesspolicy:my-policy";
    private static final String GRANT_ID = "grant:ABCDEFGHIJ123";

    private static final String COLLECTION = "/v1/projects/" + PROJECT_ID + "/accessPolicies";
    private static final String SINGLE = COLLECTION + "/" + POLICY_ID;
    private static final String GRANTS = SINGLE + "/grants";
    private static final String SINGLE_GRANT = GRANTS + "/" + GRANT_ID;

    static IAM iam;

    @BeforeAll
    static void setUp() {
        iam = new IAM(testCredentials());
        redirectToWireMock(iam);
        iam.authenticate();
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (iam != null) iam.close();
    }

    @BeforeEach
    void resetBeforeEach() {
        resetStubs();
    }

    @Nested
    @DisplayName("list()")
    class ListPolicies {

        @Test
        @DisplayName("GETs the collection and returns a token-paginated page")
        void returnsPage() {
            stubPaginatedGet(wireMock, COLLECTION, "/json/iam/access_policy_list_response.json");

            AccessPolicyList page = iam.accessPolicies().list(PROJECT_ID);

            assertNotNull(page);
            assertEquals(2, page.getList().size());
            assertEquals("accesspolicy:my-policy", page.getList().get(0).getAccessPolicyId());
            assertEquals("eyJvZmZzZXQiOjJ9", page.getNextPageToken());

            wireMock.verify(getRequestedFor(urlPathEqualTo(COLLECTION)));
        }

        @Test
        @DisplayName("forwards pageToken and pageSize as query parameters")
        void forwardsPaginationParams() {
            stubPaginatedGet(wireMock, COLLECTION, "/json/iam/access_policy_list_response.json");

            iam.accessPolicies().list(PROJECT_ID, "tok-abc", 50);

            wireMock.verify(getRequestedFor(urlPathEqualTo(COLLECTION))
                    .withQueryParam("pageToken", equalTo("tok-abc"))
                    .withQueryParam("pageSize", equalTo("50")));
        }

        @Test
        @DisplayName("returns an empty page when the project has no policies")
        void returnsEmptyPage() {
            stubPaginatedGet(wireMock, COLLECTION, "/json/iam/access_policy_list_empty_response.json");

            AccessPolicyList page = iam.accessPolicies().list(PROJECT_ID);

            assertNotNull(page);
            assertTrue(page.getList().isEmpty());
            assertNull(page.getNextPageToken());

            wireMock.verify(getRequestedFor(urlPathEqualTo(COLLECTION))
                    .withQueryParam("pageToken", absent()));
        }
    }

    @Nested
    @DisplayName("getByUuid()")
    class GetByUuid {

        @Test
        @DisplayName("GETs the single policy by id")
        void returnsPolicy() {
            stubSingleton(wireMock, "/v1/projects/.*/accessPolicies/.*",
                    "/json/iam/access_policy_response.json");

            AccessPolicy policy = iam.accessPolicies().getByUuid(PROJECT_ID, POLICY_ID);

            assertNotNull(policy);
            assertEquals("accesspolicy:my-policy", policy.getAccessPolicyId());
            assertEquals("rev-1", policy.getRev());
            assertEquals("platform", policy.getTags().get("team"));

            wireMock.verify(getRequestedFor(urlPathEqualTo(SINGLE)));
        }

        @Test
        @DisplayName("404 throws EquinixNotFoundException")
        void notFound() {
            stubErrorInline(wireMock, "/v1/projects/.*/accessPolicies/.*",
                    404, "[{\"errorCode\":\"ERR-404\",\"errorMessage\":\"Access policy not found\"}]");

            assertThrows(EquinixNotFoundException.class,
                    () -> iam.accessPolicies().getByUuid(PROJECT_ID, "accesspolicy:missing"));
        }
    }

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("POSTs the policy body to the collection and returns the created policy")
        void createsPolicy() {
            stubCreate(wireMock, COLLECTION, "/json/iam/access_policy_response.json");

            CreateAccessPolicyRequest request = new CreateAccessPolicyRequest()
                    .accessPolicyId("accesspolicy:my-policy")
                    .description("A description string.")
                    .tags(Map.of("team", "platform"))
                    .permissions(List.of(PolicyExpression.of("permissionset:read-only")));

            AccessPolicy created = iam.accessPolicies().create(PROJECT_ID, request);

            assertNotNull(created);
            assertEquals("accesspolicy:my-policy", created.getAccessPolicyId());

            wireMock.verify(postRequestedFor(urlPathEqualTo(COLLECTION))
                    .withRequestBody(matchingJsonPath("$.accessPolicyId", equalTo("accesspolicy:my-policy")))
                    .withRequestBody(matchingJsonPath("$.description", equalTo("A description string.")))
                    .withRequestBody(matchingJsonPath("$.tags.team", equalTo("platform")))
                    .withRequestBody(matchingJsonPath("$.permissions[0]", equalTo("permissionset:read-only"))));
        }

        @Test
        @DisplayName("409 throws EquinixConflictException")
        void conflict() {
            stubErrorInline(wireMock, COLLECTION,
                    409, "[{\"errorCode\":\"ERR-409\",\"errorMessage\":\"Access policy already exists\"}]");

            assertThrows(EquinixConflictException.class,
                    () -> iam.accessPolicies().create(PROJECT_ID,
                            new CreateAccessPolicyRequest().accessPolicyId("accesspolicy:dupe")
                                    .tags(Map.of("team", "platform"))
                                    .permissions(List.of(PolicyExpression.of("all")))));
        }
    }

    @Nested
    @DisplayName("update()")
    class Update {

        @Test
        @DisplayName("PATCHes the single policy with the update body and lastRev")
        void updatesPolicy() {
            wireMock.stubFor(patch(urlPathEqualTo(SINGLE))
                    .willReturn(okJson(loadFixture("/json/iam/access_policy_response.json"))));

            UpdateAccessPolicyRequest request = new UpdateAccessPolicyRequest()
                    .description("Updated description.")
                    .permissions(List.of(PolicyExpression.of("permissionset:editor")))
                    .lastRev("rev-1");

            AccessPolicy updated = iam.accessPolicies().update(PROJECT_ID, POLICY_ID, request);

            assertNotNull(updated);
            assertEquals("accesspolicy:my-policy", updated.getAccessPolicyId());

            wireMock.verify(patchRequestedFor(urlPathEqualTo(SINGLE))
                    .withRequestBody(matchingJsonPath("$.description", equalTo("Updated description.")))
                    .withRequestBody(matchingJsonPath("$.permissions[0]", equalTo("permissionset:editor")))
                    .withRequestBody(matchingJsonPath("$.lastRev", equalTo("rev-1"))));
        }
    }

    @Nested
    @DisplayName("enable() / disable()")
    class EnableDisable {

        @Test
        @DisplayName("POSTs to .../enable with the lastRev body")
        void enables() {
            wireMock.stubFor(post(urlPathEqualTo(SINGLE + "/enable"))
                    .willReturn(okJson(loadFixture("/json/iam/access_policy_response.json"))));

            AccessPolicy policy = iam.accessPolicies().enable(PROJECT_ID, POLICY_ID, "rev-1");

            assertNotNull(policy);

            wireMock.verify(postRequestedFor(urlPathEqualTo(SINGLE + "/enable"))
                    .withRequestBody(matchingJsonPath("$.lastRev", equalTo("rev-1"))));
        }

        @Test
        @DisplayName("POSTs to .../disable with the lastRev body")
        void disables() {
            wireMock.stubFor(post(urlPathEqualTo(SINGLE + "/disable"))
                    .willReturn(okJson(loadFixture("/json/iam/access_policy_response.json"))));

            AccessPolicy policy = iam.accessPolicies().disable(PROJECT_ID, POLICY_ID, "rev-1");

            assertNotNull(policy);

            wireMock.verify(postRequestedFor(urlPathEqualTo(SINGLE + "/disable"))
                    .withRequestBody(matchingJsonPath("$.lastRev", equalTo("rev-1"))));
        }
    }

    @Nested
    @DisplayName("delete()")
    class Delete {

        @Test
        @DisplayName("DELETEs the single policy with the lastRev body and returns true on 204")
        void deletesPolicy() {
            wireMock.stubFor(delete(urlPathEqualTo(SINGLE)).willReturn(noContent()));

            assertTrue(iam.accessPolicies().delete(PROJECT_ID, POLICY_ID, "rev-1"));

            wireMock.verify(deleteRequestedFor(urlPathEqualTo(SINGLE))
                    .withRequestBody(matchingJsonPath("$.lastRev", equalTo("rev-1"))));
        }

        @Test
        @DisplayName("404 throws EquinixNotFoundException")
        void notFound() {
            stubErrorInline(wireMock, "/v1/projects/.*/accessPolicies/.*",
                    404, "[{\"errorCode\":\"ERR-404\",\"errorMessage\":\"Access policy not found\"}]");

            assertThrows(EquinixNotFoundException.class,
                    () -> iam.accessPolicies().delete(PROJECT_ID, "accesspolicy:missing", "rev-1"));
        }
    }

    @Nested
    @DisplayName("listGrants()")
    class ListGrants {

        @Test
        @DisplayName("GETs the grants sub-collection and returns a token-paginated page")
        void returnsGrantPage() {
            stubPaginatedGet(wireMock, GRANTS, "/json/iam/access_policy_grant_list_response.json");

            AccessPolicyGrantList page = iam.accessPolicies().listGrants(PROJECT_ID, POLICY_ID);

            assertNotNull(page);
            assertEquals(2, page.getList().size());
            assertEquals("grant:ABCDEFGHIJ123", page.getList().get(0).getGrantId());
            assertEquals("user:dave", page.getList().get(0).getGrantee());

            wireMock.verify(getRequestedFor(urlPathEqualTo(GRANTS)));
        }

        @Test
        @DisplayName("forwards pageToken and pageSize as query parameters")
        void forwardsPaginationParams() {
            stubPaginatedGet(wireMock, GRANTS, "/json/iam/access_policy_grant_list_response.json");

            iam.accessPolicies().listGrants(PROJECT_ID, POLICY_ID, "g-tok", 25);

            wireMock.verify(getRequestedFor(urlPathEqualTo(GRANTS))
                    .withQueryParam("pageToken", equalTo("g-tok"))
                    .withQueryParam("pageSize", equalTo("25")));
        }
    }

    @Nested
    @DisplayName("addGrant()")
    class AddGrant {

        @Test
        @DisplayName("POSTs the grantee body to the grants sub-collection and returns the created grant")
        void addsGrant() {
            stubCreate(wireMock, GRANTS, "/json/iam/access_policy_grant_response.json");

            AccessPolicyGrant grant = iam.accessPolicies().addGrant(PROJECT_ID, POLICY_ID,
                    new AddGrantRequest().grantee("user:dave").lastRev("rev-1"));

            assertNotNull(grant);
            assertEquals("grant:ABCDEFGHIJ123", grant.getGrantId());
            assertEquals("user:dave", grant.getGrantee());

            wireMock.verify(postRequestedFor(urlPathEqualTo(GRANTS))
                    .withRequestBody(matchingJsonPath("$.grantee", equalTo("user:dave")))
                    .withRequestBody(matchingJsonPath("$.lastRev", equalTo("rev-1"))));
        }
    }

    @Nested
    @DisplayName("removeGrant()")
    class RemoveGrant {

        @Test
        @DisplayName("DELETEs the single grant and returns true on 204")
        void removesGrant() {
            wireMock.stubFor(delete(urlPathEqualTo(SINGLE_GRANT)).willReturn(noContent()));

            assertTrue(iam.accessPolicies().removeGrant(PROJECT_ID, POLICY_ID, GRANT_ID));

            wireMock.verify(deleteRequestedFor(urlPathEqualTo(SINGLE_GRANT)));
        }

        @Test
        @DisplayName("404 throws EquinixNotFoundException")
        void notFound() {
            stubErrorInline(wireMock, "/v1/projects/.*/accessPolicies/.*/grants/.*",
                    404, "[{\"errorCode\":\"ERR-404\",\"errorMessage\":\"Grant not found\"}]");

            assertThrows(EquinixNotFoundException.class,
                    () -> iam.accessPolicies().removeGrant(PROJECT_ID, POLICY_ID, "grant:missing"));
        }
    }
}
