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
import com.eqixiac.equinix.iam.model.PermissionSet;
import com.eqixiac.equinix.iam.model.PolicyExpression;
import com.eqixiac.equinix.iam.model.json.PermissionSetList;
import com.eqixiac.equinix.iam.model.json.creators.CreatePermissionSetRequest;
import com.eqixiac.equinix.iam.model.json.creators.UpdatePermissionSetRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.eqixiac.equinix.core.ResponseStubs.stubCreate;
import static com.eqixiac.equinix.core.ResponseStubs.stubDeleteNoContent;
import static com.eqixiac.equinix.core.ResponseStubs.stubError;
import static com.eqixiac.equinix.core.ResponseStubs.stubPaginatedGet;
import static com.eqixiac.equinix.core.ResponseStubs.stubSingleton;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.patch;
import static com.github.tomakehurst.wiremock.client.WireMock.patchRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * WireMock request-contract tests for IAM project-scoped permission sets
 * ({@code iam.permissionSets()} / {@link com.eqixiac.equinix.iam.client.IAMPermissionSets}).
 *
 * <p>The permission set lifecycle is resolved against
 * {@code v1/projects/{projectId}/permissionSets[/{permissionSetId}]} (see
 * {@code apiParams_IAM.json}). Each operation asserts the HTTP verb + path, and the create / update
 * tests additionally assert the request body via {@code matchingJsonPath}; delete sends the
 * {@code lastRev} body and expects a 204. At least one error mapping is exercised per
 * read/conflict path.</p>
 */
class IAMPermissionSetsWireMockTest extends WireMockTestBase {

    private static final String PROJECT_ID = "project:abc-123";
    private static final String PERMISSION_SET_ID = "permissionset:network-admin";

    private static final String BASE_PATH = "/v1/projects/" + PROJECT_ID + "/permissionSets";
    private static final String ITEM_PATH = BASE_PATH + "/" + PERMISSION_SET_ID;

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
    class ListSets {

        @Test
        @DisplayName("GETs the project permission sets and maps the page")
        void listsFirstPage() {
            stubPaginatedGet(wireMock, BASE_PATH, "/json/iam/permission_set_list.json");

            PermissionSetList page = iam.permissionSets().list(PROJECT_ID);

            assertNotNull(page);
            assertEquals(2, page.getList().size());
            assertEquals("permissionset:network-admin", page.getList().get(0).getPermissionSetId());
            assertEquals("permissionset:billing-readers", page.getList().get(1).getPermissionSetId());
            assertTrue(page.getList().get(1).getManaged());
            assertEquals("eyJvZmZzZXQiOjJ9", page.getNextPageToken());

            wireMock.verify(getRequestedFor(urlPathEqualTo(BASE_PATH)));
        }

        @Test
        @DisplayName("passes pageToken + pageSize as query params")
        void listsWithPagination() {
            stubPaginatedGet(wireMock, BASE_PATH, "/json/iam/permission_set_list_page2.json");

            PermissionSetList page = iam.permissionSets().list(PROJECT_ID, "eyJvZmZzZXQiOjJ9", 50);

            assertNotNull(page);
            assertEquals(1, page.getList().size());
            assertEquals("permissionset:support-agents", page.getList().get(0).getPermissionSetId());
            assertEquals(null, page.getNextPageToken());

            wireMock.verify(getRequestedFor(urlPathEqualTo(BASE_PATH))
                    .withQueryParam("pageToken", equalTo("eyJvZmZzZXQiOjJ9"))
                    .withQueryParam("pageSize", equalTo("50")));
        }

        @Test
        @DisplayName("empty list -> empty page, no items")
        void listsEmpty() {
            wireMock.stubFor(get(urlPathEqualTo(BASE_PATH))
                    .willReturn(okJson(loadFixture("/json/iam/permission_set_list_empty.json"))));

            PermissionSetList page = iam.permissionSets().list(PROJECT_ID);

            assertNotNull(page);
            assertTrue(page.getList().isEmpty());
            assertEquals(null, page.getNextPageToken());

            wireMock.verify(getRequestedFor(urlPathEqualTo(BASE_PATH)));
        }
    }

    @Nested
    @DisplayName("getByUuid()")
    class GetByUuid {

        @Test
        @DisplayName("GETs a single permission set by id and maps polymorphic permissions")
        void getsOne() {
            stubSingleton(wireMock, ITEM_PATH, "/json/iam/permission_set.json");

            PermissionSet set = iam.permissionSets().getByUuid(PROJECT_ID, PERMISSION_SET_ID);

            assertNotNull(set);
            assertEquals(PERMISSION_SET_ID, set.getPermissionSetId());
            assertEquals("ern:eqix:equinix/access:global:abc-123:permissionset:network-admin", set.getErn());
            assertEquals("Network administrators", set.getDescription());
            assertEquals("rev-001", set.getRev());
            assertEquals("networking", set.getTags().get("team"));

            // permissions[0] is a bare string member, permissions[1] is a structured InlinePermission
            List<PolicyExpression> permissions = set.getPermissions();
            assertEquals(2, permissions.size());
            assertTrue(permissions.get(0).isString());
            assertEquals("permissionset:base-readers", permissions.get(0).asString());
            assertTrue(permissions.get(1).isObject());

            wireMock.verify(getRequestedFor(urlPathEqualTo(ITEM_PATH)));
        }

        @Test
        @DisplayName("404 throws EquinixNotFoundException")
        void notFound() {
            stubError(wireMock, ITEM_PATH, 404, "/json/core/error_404_response.json");

            assertThrows(EquinixNotFoundException.class,
                    () -> iam.permissionSets().getByUuid(PROJECT_ID, PERMISSION_SET_ID));

            wireMock.verify(getRequestedFor(urlPathEqualTo(ITEM_PATH)));
        }
    }

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("POSTs the create body and returns the created set")
        void createsSet() {
            stubCreate(wireMock, BASE_PATH, "/json/iam/permission_set.json");

            CreatePermissionSetRequest request = new CreatePermissionSetRequest()
                    .permissionSetId(PERMISSION_SET_ID)
                    .description("Network administrators")
                    .tags(Map.of("team", "networking", "env", "prod"))
                    .permissions(List.of(PolicyExpression.of("permissionset:base-readers")));

            PermissionSet created = iam.permissionSets().create(PROJECT_ID, request);

            assertNotNull(created);
            assertEquals(PERMISSION_SET_ID, created.getPermissionSetId());
            assertEquals("rev-001", created.getRev());

            wireMock.verify(postRequestedFor(urlPathEqualTo(BASE_PATH))
                    .withRequestBody(matchingJsonPath("$.permissionSetId", equalTo(PERMISSION_SET_ID)))
                    .withRequestBody(matchingJsonPath("$.description", equalTo("Network administrators")))
                    .withRequestBody(matchingJsonPath("$.tags.team", equalTo("networking")))
                    .withRequestBody(matchingJsonPath("$.permissions[0]", equalTo("permissionset:base-readers"))));
        }

        @Test
        @DisplayName("409 throws EquinixConflictException")
        void conflict() {
            stubError(wireMock, BASE_PATH, 409, "/json/core/error_409_response.json");

            CreatePermissionSetRequest request = new CreatePermissionSetRequest()
                    .permissionSetId(PERMISSION_SET_ID)
                    .tags(Map.of("team", "networking"))
                    .permissions(List.of(PolicyExpression.of("permissionset:base-readers")));

            assertThrows(EquinixConflictException.class,
                    () -> iam.permissionSets().create(PROJECT_ID, request));

            wireMock.verify(postRequestedFor(urlPathEqualTo(BASE_PATH)));
        }
    }

    @Nested
    @DisplayName("update()")
    class Update {

        @Test
        @DisplayName("PATCHes the set by id with the update body and lastRev")
        void updatesSet() {
            wireMock.stubFor(patch(urlPathEqualTo(ITEM_PATH))
                    .willReturn(okJson(loadFixture("/json/iam/permission_set_updated.json"))));

            UpdatePermissionSetRequest request = new UpdatePermissionSetRequest()
                    .description("Network administrators (revised)")
                    .tags(Map.of("team", "networking", "env", "prod", "reviewed", "true"))
                    .permissions(List.of(PolicyExpression.of("permissionset:base-readers")))
                    .lastRev("rev-001");

            PermissionSet updated = iam.permissionSets().update(PROJECT_ID, PERMISSION_SET_ID, request);

            assertNotNull(updated);
            assertEquals("rev-002", updated.getRev());
            assertEquals("Network administrators (revised)", updated.getDescription());

            wireMock.verify(patchRequestedFor(urlPathEqualTo(ITEM_PATH))
                    .withRequestBody(matchingJsonPath("$.description", equalTo("Network administrators (revised)")))
                    .withRequestBody(matchingJsonPath("$.lastRev", equalTo("rev-001")))
                    .withRequestBody(matchingJsonPath("$.tags.reviewed", equalTo("true")))
                    .withRequestBody(matchingJsonPath("$.permissions[0]", equalTo("permissionset:base-readers"))));
        }
    }

    @Nested
    @DisplayName("delete()")
    class Delete {

        @Test
        @DisplayName("DELETEs the set with lastRev body and returns true on 204")
        void deletesSet() {
            stubDeleteNoContent(wireMock, ITEM_PATH);

            Boolean deleted = iam.permissionSets().delete(PROJECT_ID, PERMISSION_SET_ID, "rev-001");

            assertTrue(deleted);

            wireMock.verify(deleteRequestedFor(urlPathEqualTo(ITEM_PATH))
                    .withRequestBody(matchingJsonPath("$.lastRev", equalTo("rev-001"))));
        }

        @Test
        @DisplayName("409 throws EquinixConflictException (stale lastRev)")
        void deleteConflict() {
            stubError(wireMock, ITEM_PATH, 409, "/json/core/error_409_response.json");

            assertThrows(EquinixConflictException.class,
                    () -> iam.permissionSets().delete(PROJECT_ID, PERMISSION_SET_ID, "rev-stale"));

            wireMock.verify(deleteRequestedFor(urlPathEqualTo(ITEM_PATH)));
        }
    }
}
