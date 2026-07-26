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

package com.eqixiac.equinix.iam.wiremock;

import com.eqixiac.equinix.IAM;
import com.eqixiac.equinix.core.WireMockTestBase;
import com.eqixiac.equinix.core.exception.EquinixConflictException;
import com.eqixiac.equinix.core.exception.EquinixNotFoundException;
import com.eqixiac.equinix.core.exception.EquinixServerException;
import com.eqixiac.equinix.iam.model.PolicyExpression;
import com.eqixiac.equinix.iam.model.PolicyMask;
import com.eqixiac.equinix.iam.model.PrincipalPolicy;
import com.eqixiac.equinix.iam.model.json.PolicyMaskList;
import com.eqixiac.equinix.iam.model.json.PrincipalPolicyList;
import com.eqixiac.equinix.iam.model.json.creators.CreatePolicyMaskRequest;
import com.eqixiac.equinix.iam.model.json.creators.UpdatePolicyMaskRequest;
import com.eqixiac.equinix.iam.model.json.creators.UpdatePrincipalPolicyRequest;
import org.junit.jupiter.api.*;

import java.util.List;

import static com.eqixiac.equinix.core.ResponseStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock request-contract tests for the two project-scoped IAM access-control families:
 * {@link IAM#principalPolicies()} ({@link com.eqixiac.equinix.iam.client.IAMPrincipalPolicies}) and
 * {@link IAM#policyMasks()} ({@link com.eqixiac.equinix.iam.client.IAMPolicyMasks}).
 *
 * <p>Both are rooted at {@code /v1/projects/{projectId}/...} and use opaque-token pagination
 * ({@code nextPageToken}). Principal policies are list/get/update/enable/disable only (no create or
 * delete); policy masks add create + delete (the latter carrying a {@code lastRev} body for
 * optimistic concurrency). Every test asserts the HTTP verb + resolved path via
 * {@code wireMock.verify(...)}, and request-body tests additionally assert the serialized JSON.</p>
 */
class IAMPrincipalPoliciesWireMockTest extends WireMockTestBase {

    private static final String PROJECT_ID = "project:abc-123";
    private static final String PROJECT_SEG = "project:abc-123";

    private static final String PRINCIPAL_POLICIES_PATH =
            "/v1/projects/" + PROJECT_SEG + "/principalPolicies";
    private static final String USER_PRINCIPAL = "user:alice";
    private static final String PRINCIPAL_POLICY_PATH =
            PRINCIPAL_POLICIES_PATH + "/" + USER_PRINCIPAL;

    private static final String POLICY_MASKS_PATH =
            "/v1/projects/" + PROJECT_SEG + "/policyMasks";
    private static final String POLICY_MASK_ID = "policymask:my-mask";
    private static final String POLICY_MASK_PATH = POLICY_MASKS_PATH + "/" + POLICY_MASK_ID;

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

    // ------------------------------------------------------------------
    // Principal policies — list / getByUuid / update / enable / disable
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("principalPolicies().list()")
    class PrincipalPolicyList_ {

        @Test
        @DisplayName("GETs the project principal policies and deserializes the page")
        void listsPolicies() {
            stubPaginatedGet(wireMock, "/v1/projects/.*/principalPolicies",
                    "/json/iam/principal_policy_list.json");

            PrincipalPolicyList page = iam.principalPolicies().list(PROJECT_ID);

            assertNotNull(page);
            assertEquals(2, page.getList().size());
            assertEquals("user:alice", page.getList().get(0).getUserPrincipal());
            assertEquals("eyJvZmZzZXQiOjJ9", page.getNextPageToken());

            wireMock.verify(getRequestedFor(urlPathEqualTo(PRINCIPAL_POLICIES_PATH)));
        }

        @Test
        @DisplayName("forwards pageToken + pageSize as query parameters")
        void listsWithPagination() {
            stubPaginatedGet(wireMock, "/v1/projects/.*/principalPolicies",
                    "/json/iam/principal_policy_list.json");

            iam.principalPolicies().list(PROJECT_ID, "tok-1", 25);

            wireMock.verify(getRequestedFor(urlPathEqualTo(PRINCIPAL_POLICIES_PATH))
                    .withQueryParam("pageToken", equalTo("tok-1"))
                    .withQueryParam("pageSize", equalTo("25")));
        }

        @Test
        @DisplayName("returns an empty page when the project has no principal policies")
        void listsEmpty() {
            stubPaginatedGet(wireMock, "/v1/projects/.*/principalPolicies",
                    "/json/iam/principal_policy_list_empty.json");

            PrincipalPolicyList page = iam.principalPolicies().list(PROJECT_ID);

            assertNotNull(page);
            assertTrue(page.getList().isEmpty());
            assertNull(page.getNextPageToken());

            wireMock.verify(getRequestedFor(urlPathEqualTo(PRINCIPAL_POLICIES_PATH)));
        }
    }

    @Nested
    @DisplayName("principalPolicies().getByUuid()")
    class PrincipalPolicyGet {

        @Test
        @DisplayName("GETs a single principal policy by user principal")
        void getsPolicy() {
            stubSingleton(wireMock, "/v1/projects/.*/principalPolicies/.*",
                    "/json/iam/principal_policy_response.json");

            PrincipalPolicy policy = iam.principalPolicies().getByUuid(PROJECT_ID, USER_PRINCIPAL);

            assertNotNull(policy);
            assertEquals("user:alice", policy.getUserPrincipal());
            assertEquals("rev-7", policy.getRev());
            assertEquals(Boolean.TRUE, policy.getDisabledPolicy());
            // permissions are lossless PolicyExpression members (a bare string + a structured ref)
            assertEquals(2, policy.getPermissions().size());
            assertTrue(policy.getPermissions().get(0).isString());
            assertEquals("permissionset:reader", policy.getPermissions().get(0).asString());
            assertTrue(policy.getPermissions().get(1).isObject());

            wireMock.verify(getRequestedFor(urlPathEqualTo(PRINCIPAL_POLICY_PATH)));
        }

        @Test
        @DisplayName("404 throws EquinixNotFoundException")
        void notFound() {
            stubErrorInline(wireMock, "/v1/projects/.*/principalPolicies/.*",
                    404, "{\"errorCode\":\"IAM-404\",\"errorMessage\":\"Principal policy not found\"}");

            assertThrows(EquinixNotFoundException.class,
                    () -> iam.principalPolicies().getByUuid(PROJECT_ID, "user:ghost"));
        }
    }

    @Nested
    @DisplayName("principalPolicies().update()")
    class PrincipalPolicyUpdate {

        @Test
        @DisplayName("PATCHes the principal policy and asserts the request body")
        void updatesPolicy() {
            wireMock.stubFor(patch(urlPathEqualTo(PRINCIPAL_POLICY_PATH))
                    .willReturn(okJson(loadFixture("/json/iam/principal_policy_response.json"))));

            UpdatePrincipalPolicyRequest request = new UpdatePrincipalPolicyRequest()
                    .description("Updated principal policy")
                    .permissions(List.of(PolicyExpression.of("permissionset:reader")))
                    .lastRev("rev-7");

            PrincipalPolicy updated = iam.principalPolicies().update(PROJECT_ID, USER_PRINCIPAL, request);

            assertNotNull(updated);
            assertEquals("user:alice", updated.getUserPrincipal());

            wireMock.verify(patchRequestedFor(urlPathEqualTo(PRINCIPAL_POLICY_PATH))
                    .withRequestBody(matchingJsonPath("$.description", equalTo("Updated principal policy")))
                    .withRequestBody(matchingJsonPath("$.lastRev", equalTo("rev-7")))
                    .withRequestBody(matchingJsonPath("$.permissions[0]", equalTo("permissionset:reader"))));
        }

        @Test
        @DisplayName("409 throws EquinixConflictException (stale lastRev)")
        void conflict() {
            stubErrorInline(wireMock, "/v1/projects/.*/principalPolicies/.*",
                    409, "{\"errorCode\":\"IAM-409\",\"errorMessage\":\"Revision conflict\"}");

            assertThrows(EquinixConflictException.class,
                    () -> iam.principalPolicies().update(PROJECT_ID, USER_PRINCIPAL,
                            new UpdatePrincipalPolicyRequest().lastRev("stale")));
        }
    }

    @Nested
    @DisplayName("principalPolicies() enable / disable")
    class PrincipalPolicyToggle {

        @Test
        @DisplayName("POSTs to /enable with the lastRev body")
        void enables() {
            wireMock.stubFor(post(urlPathEqualTo(PRINCIPAL_POLICY_PATH + "/enable"))
                    .willReturn(okJson(loadFixture("/json/iam/principal_policy_response.json"))));

            PrincipalPolicy result = iam.principalPolicies().enable(PROJECT_ID, USER_PRINCIPAL, "rev-7");

            assertNotNull(result);
            assertEquals("user:alice", result.getUserPrincipal());

            wireMock.verify(postRequestedFor(urlPathEqualTo(PRINCIPAL_POLICY_PATH + "/enable"))
                    .withRequestBody(matchingJsonPath("$.lastRev", equalTo("rev-7"))));
        }

        @Test
        @DisplayName("POSTs to /disable with the lastRev body")
        void disables() {
            wireMock.stubFor(post(urlPathEqualTo(PRINCIPAL_POLICY_PATH + "/disable"))
                    .willReturn(okJson(loadFixture("/json/iam/principal_policy_response.json"))));

            PrincipalPolicy result = iam.principalPolicies().disable(PROJECT_ID, USER_PRINCIPAL, "rev-7");

            assertNotNull(result);

            wireMock.verify(postRequestedFor(urlPathEqualTo(PRINCIPAL_POLICY_PATH + "/disable"))
                    .withRequestBody(matchingJsonPath("$.lastRev", equalTo("rev-7"))));
        }
    }

    // ------------------------------------------------------------------
    // Policy masks — list / create / getByUuid / update / delete / toggle
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("policyMasks().list()")
    class PolicyMaskListing {

        @Test
        @DisplayName("GETs the project policy masks and deserializes the page")
        void listsMasks() {
            stubPaginatedGet(wireMock, "/v1/projects/.*/policyMasks",
                    "/json/iam/policy_mask_list.json");

            PolicyMaskList page = iam.policyMasks().list(PROJECT_ID);

            assertNotNull(page);
            assertEquals(2, page.getList().size());
            assertEquals("policymask:my-mask", page.getList().get(0).getPolicyMaskId());
            assertEquals("eyJvZmZzZXQiOjJ9", page.getNextPageToken());

            wireMock.verify(getRequestedFor(urlPathEqualTo(POLICY_MASKS_PATH)));
        }

        @Test
        @DisplayName("forwards pageToken + pageSize as query parameters")
        void listsWithPagination() {
            stubPaginatedGet(wireMock, "/v1/projects/.*/policyMasks",
                    "/json/iam/policy_mask_list.json");

            iam.policyMasks().list(PROJECT_ID, "tok-9", 50);

            wireMock.verify(getRequestedFor(urlPathEqualTo(POLICY_MASKS_PATH))
                    .withQueryParam("pageToken", equalTo("tok-9"))
                    .withQueryParam("pageSize", equalTo("50")));
        }

        @Test
        @DisplayName("returns an empty page when the project has no policy masks")
        void listsEmpty() {
            stubPaginatedGet(wireMock, "/v1/projects/.*/policyMasks",
                    "/json/iam/policy_mask_list_empty.json");

            PolicyMaskList page = iam.policyMasks().list(PROJECT_ID);

            assertNotNull(page);
            assertTrue(page.getList().isEmpty());

            wireMock.verify(getRequestedFor(urlPathEqualTo(POLICY_MASKS_PATH)));
        }
    }

    @Nested
    @DisplayName("policyMasks().create()")
    class PolicyMaskCreate {

        @Test
        @DisplayName("POSTs the new mask and asserts the request body (incl. the 'none' oneOf form)")
        void createsMask() {
            wireMock.stubFor(post(urlPathEqualTo(POLICY_MASKS_PATH))
                    .willReturn(aResponse().withStatus(201)
                            .withHeader("Content-Type", "application/json")
                            .withBody(loadFixture("/json/iam/policy_mask_response.json"))));

            CreatePolicyMaskRequest request = new CreatePolicyMaskRequest()
                    .policyMaskId(POLICY_MASK_ID)
                    .description("A description string.")
                    .managedPolicies(PolicyExpression.of("none"))
                    .managedPermissionSets(PolicyExpression.ofStrings(
                            List.of("managedset:reader", "managedset:writer")));

            PolicyMask created = iam.policyMasks().create(PROJECT_ID, request);

            assertNotNull(created);
            assertEquals("policymask:my-mask", created.getPolicyMaskId());

            wireMock.verify(postRequestedFor(urlPathEqualTo(POLICY_MASKS_PATH))
                    .withRequestBody(matchingJsonPath("$.policyMaskId", equalTo(POLICY_MASK_ID)))
                    .withRequestBody(matchingJsonPath("$.description", equalTo("A description string.")))
                    .withRequestBody(matchingJsonPath("$.managedPolicies", equalTo("none")))
                    .withRequestBody(matchingJsonPath("$.managedPermissionSets[0]", equalTo("managedset:reader"))));
        }
    }

    @Nested
    @DisplayName("policyMasks().getByUuid()")
    class PolicyMaskGet {

        @Test
        @DisplayName("GETs a single policy mask by id and exposes the lossless oneOf forms")
        void getsMask() {
            stubSingleton(wireMock, "/v1/projects/.*/policyMasks/.*",
                    "/json/iam/policy_mask_response.json");

            PolicyMask mask = iam.policyMasks().getByUuid(PROJECT_ID, POLICY_MASK_ID);

            assertNotNull(mask);
            assertEquals("policymask:my-mask", mask.getPolicyMaskId());
            assertEquals("rev-1", mask.getRev());
            // "none" string form preserved
            assertTrue(mask.getManagedPolicies().isString());
            assertEquals("none", mask.getManagedPolicies().asString());
            // array form preserved
            assertTrue(mask.getManagedPermissionSets().isArray());
            assertEquals(List.of("managedset:reader", "managedset:writer"),
                    mask.getManagedPermissionSets().asStringList());
            // structured subtract object preserved
            assertTrue(mask.getSubtract().isObject());

            wireMock.verify(getRequestedFor(urlPathEqualTo(POLICY_MASK_PATH)));
        }

        @Test
        @DisplayName("404 throws EquinixNotFoundException")
        void notFound() {
            stubErrorInline(wireMock, "/v1/projects/.*/policyMasks/.*",
                    404, "{\"errorCode\":\"IAM-404\",\"errorMessage\":\"Policy mask not found\"}");

            assertThrows(EquinixNotFoundException.class,
                    () -> iam.policyMasks().getByUuid(PROJECT_ID, "policymask:ghost"));
        }
    }

    @Nested
    @DisplayName("policyMasks().update()")
    class PolicyMaskUpdate {

        @Test
        @DisplayName("PATCHes the mask and asserts the request body")
        void updatesMask() {
            wireMock.stubFor(patch(urlPathEqualTo(POLICY_MASK_PATH))
                    .willReturn(okJson(loadFixture("/json/iam/policy_mask_response.json"))));

            UpdatePolicyMaskRequest request = new UpdatePolicyMaskRequest()
                    .description("Updated mask")
                    .managedPolicies(PolicyExpression.ofStrings(List.of("managedpolicy:base")))
                    .lastRev("rev-1");

            PolicyMask updated = iam.policyMasks().update(PROJECT_ID, POLICY_MASK_ID, request);

            assertNotNull(updated);
            assertEquals("policymask:my-mask", updated.getPolicyMaskId());

            wireMock.verify(patchRequestedFor(urlPathEqualTo(POLICY_MASK_PATH))
                    .withRequestBody(matchingJsonPath("$.description", equalTo("Updated mask")))
                    .withRequestBody(matchingJsonPath("$.lastRev", equalTo("rev-1")))
                    .withRequestBody(matchingJsonPath("$.managedPolicies[0]", equalTo("managedpolicy:base"))));
        }
    }

    @Nested
    @DisplayName("policyMasks().delete()")
    class PolicyMaskDelete {

        @Test
        @DisplayName("DELETEs the mask (204) carrying the lastRev body and returns true")
        void deletesMask() {
            wireMock.stubFor(delete(urlPathEqualTo(POLICY_MASK_PATH))
                    .willReturn(noContent()));

            Boolean result = iam.policyMasks().delete(PROJECT_ID, POLICY_MASK_ID, "rev-1");

            assertEquals(Boolean.TRUE, result);

            wireMock.verify(deleteRequestedFor(urlPathEqualTo(POLICY_MASK_PATH))
                    .withRequestBody(matchingJsonPath("$.lastRev", equalTo("rev-1"))));
        }

        @Test
        @DisplayName("409 throws EquinixConflictException (stale lastRev)")
        void conflict() {
            stubErrorInline(wireMock, "/v1/projects/.*/policyMasks/.*",
                    409, "{\"errorCode\":\"IAM-409\",\"errorMessage\":\"Revision conflict\"}");

            assertThrows(EquinixConflictException.class,
                    () -> iam.policyMasks().delete(PROJECT_ID, POLICY_MASK_ID, "stale"));
        }
    }

    @Nested
    @DisplayName("policyMasks() enable / disable")
    class PolicyMaskToggle {

        @Test
        @DisplayName("POSTs to /enable with the lastRev body")
        void enables() {
            wireMock.stubFor(post(urlPathEqualTo(POLICY_MASK_PATH + "/enable"))
                    .willReturn(okJson(loadFixture("/json/iam/policy_mask_response.json"))));

            PolicyMask result = iam.policyMasks().enable(PROJECT_ID, POLICY_MASK_ID, "rev-1");

            assertNotNull(result);
            assertEquals("policymask:my-mask", result.getPolicyMaskId());

            wireMock.verify(postRequestedFor(urlPathEqualTo(POLICY_MASK_PATH + "/enable"))
                    .withRequestBody(matchingJsonPath("$.lastRev", equalTo("rev-1"))));
        }

        @Test
        @DisplayName("POSTs to /disable with the lastRev body")
        void disables() {
            wireMock.stubFor(post(urlPathEqualTo(POLICY_MASK_PATH + "/disable"))
                    .willReturn(okJson(loadFixture("/json/iam/policy_mask_response.json"))));

            PolicyMask result = iam.policyMasks().disable(PROJECT_ID, POLICY_MASK_ID, "rev-1");

            assertNotNull(result);

            wireMock.verify(postRequestedFor(urlPathEqualTo(POLICY_MASK_PATH + "/disable"))
                    .withRequestBody(matchingJsonPath("$.lastRev", equalTo("rev-1"))));
        }
    }

    @Nested
    @DisplayName("Error handling")
    class Errors {

        @Test
        @DisplayName("500 on list throws EquinixServerException")
        void serverError() {
            stubErrorInline(wireMock, "/v1/projects/.*/policyMasks",
                    500, "{\"errorCode\":\"IAM-500\",\"errorMessage\":\"Internal server error\"}");

            assertThrows(EquinixServerException.class,
                    () -> iam.policyMasks().list(PROJECT_ID));
        }
    }
}
