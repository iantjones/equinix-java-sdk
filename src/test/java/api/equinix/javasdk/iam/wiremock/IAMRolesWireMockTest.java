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
import api.equinix.javasdk.iam.model.RoleAssignment;
import api.equinix.javasdk.iam.model.json.RoleAssignmentList;
import api.equinix.javasdk.iam.model.json.RoleList;
import api.equinix.javasdk.iam.model.json.creators.CreateRoleAssignmentRequest;
import org.junit.jupiter.api.*;

import static api.equinix.javasdk.core.ResponseStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for the IAM RBAC layer — the roles catalog ({@code iam.roles()})
 * and role assignments ({@code iam.roleAssignments()}).
 *
 * <p>Roles is read-only and token-paginated:
 * {@code GET /v1/roles} (operationId {@code listRoles}) and
 * {@code GET /v1/projects/{projectId}/roles} (operationId {@code listRolesByProjectId}).</p>
 *
 * <p>Role assignments support list/get/create/delete:
 * {@code GET /v1/roleAssignments} (listRoleAssignments),
 * {@code POST /v1/roleAssignments} (createRoleAssignment),
 * {@code GET /v1/roleAssignments/{roleAssignmentId}} (getRoleAssignment) and
 * {@code DELETE /v1/roleAssignments/{roleAssignmentId}} (deleteRoleAssignment).</p>
 */
class IAMRolesWireMockTest extends WireMockTestBase {

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
    @DisplayName("roles().list()")
    class ListRoles {

        @Test
        @DisplayName("GETs /v1/roles and returns the token-paginated role list")
        void listsRoles() {
            stubPaginatedGet(wireMock, "/v1/roles", "/json/iam/roles_list.json");

            RoleList roles = iam.roles().list();

            assertNotNull(roles);
            assertEquals(2, roles.getList().size());
            assertEquals("role:550e8400-e29b-41d4-a716-446655440000", roles.getList().get(0).getRoleId());
            assertEquals("Network Admin", roles.getList().get(0).getName());
            assertEquals("eyJvZmZzZXQiOjJ9", roles.getNextPageToken());
            assertEquals("iam.role.assignment.read",
                    roles.getList().get(0).getPermissions().get(0).getAction());

            wireMock.verify(getRequestedFor(urlPathEqualTo("/v1/roles")));
        }

        @Test
        @DisplayName("passes pageToken/pageSize/projectErn as query params")
        void listsRolesWithPaging() {
            stubPaginatedGet(wireMock, "/v1/roles", "/json/iam/roles_list.json");

            RoleList roles = iam.roles().list("tok-1", 50, "ern:project:abc-123");

            assertNotNull(roles);
            assertEquals(2, roles.getList().size());

            wireMock.verify(getRequestedFor(urlPathEqualTo("/v1/roles"))
                    .withQueryParam("pageToken", equalTo("tok-1"))
                    .withQueryParam("pageSize", equalTo("50"))
                    .withQueryParam("projectErn", equalTo("ern:project:abc-123")));
        }

        @Test
        @DisplayName("returns an empty list when the catalog has no roles")
        void listsRolesEmpty() {
            stubPaginatedGet(wireMock, "/v1/roles", "/json/iam/roles_empty.json");

            RoleList roles = iam.roles().list();

            assertNotNull(roles);
            assertTrue(roles.getList().isEmpty());
            assertNull(roles.getNextPageToken());

            wireMock.verify(getRequestedFor(urlPathEqualTo("/v1/roles")));
        }
    }

    @Nested
    @DisplayName("roles().listByProject()")
    class ListRolesByProject {

        @Test
        @DisplayName("GETs /v1/projects/{projectId}/roles and returns the project-scoped roles")
        void listsByProject() {
            stubPaginatedGet(wireMock, "/v1/projects/[^/]+/roles", "/json/iam/roles_by_project.json");

            RoleList roles = iam.roles().listByProject("project:abc-123");

            assertNotNull(roles);
            assertEquals(1, roles.getList().size());
            assertEquals("Project Owner", roles.getList().get(0).getName());
            assertNull(roles.getNextPageToken());

            wireMock.verify(getRequestedFor(urlPathEqualTo("/v1/projects/project:abc-123/roles")));
        }

        @Test
        @DisplayName("passes pagination + projectErn query params on the project-scoped list")
        void listsByProjectWithPaging() {
            stubPaginatedGet(wireMock, "/v1/projects/[^/]+/roles", "/json/iam/roles_by_project.json");

            RoleList roles = iam.roles().listByProject("project:abc-123", "tok-2", 10, null);

            assertNotNull(roles);
            assertEquals(1, roles.getList().size());

            wireMock.verify(getRequestedFor(urlPathEqualTo("/v1/projects/project:abc-123/roles"))
                    .withQueryParam("pageToken", equalTo("tok-2"))
                    .withQueryParam("pageSize", equalTo("10")));
        }
    }

    @Nested
    @DisplayName("roleAssignments().list()")
    class ListRoleAssignments {

        @Test
        @DisplayName("GETs /v1/roleAssignments with scope query params and returns the list")
        void listsAssignments() {
            stubPaginatedGet(wireMock, "/v1/roleAssignments", "/json/iam/role_assignments_list.json");

            RoleAssignmentList assignments = iam.roleAssignments().list("abc-123", "PROJECT");

            assertNotNull(assignments);
            assertEquals(2, assignments.getList().size());
            assertEquals("roleassignment:07218c48-343d-43e2-9831-a2080445cd22",
                    assignments.getList().get(0).getRoleAssignmentId());
            assertEquals("user:alice", assignments.getList().get(0).getPrincipal());
            assertEquals("PROJECT", assignments.getList().get(0).getAssignmentScope().getType());
            assertEquals("eyJvZmZzZXQiOjJ9", assignments.getNextPageToken());

            wireMock.verify(getRequestedFor(urlPathEqualTo("/v1/roleAssignments"))
                    .withQueryParam("assignmentScopeId", equalTo("abc-123"))
                    .withQueryParam("assignmentScopeType", equalTo("PROJECT")));
        }

        @Test
        @DisplayName("passes pageToken/pageSize on the paged overload")
        void listsAssignmentsWithPaging() {
            stubPaginatedGet(wireMock, "/v1/roleAssignments", "/json/iam/role_assignments_list.json");

            RoleAssignmentList assignments =
                    iam.roleAssignments().list("abc-123", "PROJECT", "tok-3", 25);

            assertNotNull(assignments);
            assertEquals(2, assignments.getList().size());

            wireMock.verify(getRequestedFor(urlPathEqualTo("/v1/roleAssignments"))
                    .withQueryParam("assignmentScopeId", equalTo("abc-123"))
                    .withQueryParam("assignmentScopeType", equalTo("PROJECT"))
                    .withQueryParam("pageToken", equalTo("tok-3"))
                    .withQueryParam("pageSize", equalTo("25")));
        }
    }

    @Nested
    @DisplayName("roleAssignments().getByUuid()")
    class GetRoleAssignment {

        @Test
        @DisplayName("GETs /v1/roleAssignments/{id} and returns the assignment")
        void getsAssignment() {
            stubSingleton(wireMock, "/v1/roleAssignments/[^/]+", "/json/iam/role_assignment_response.json");

            RoleAssignment assignment = iam.roleAssignments()
                    .getByUuid("roleassignment:07218c48-343d-43e2-9831-a2080445cd22");

            assertNotNull(assignment);
            assertEquals("roleassignment:07218c48-343d-43e2-9831-a2080445cd22", assignment.getRoleAssignmentId());
            assertEquals("project:abc-123", assignment.getProjectId());
            assertEquals("Network Admin", assignment.getRoleName());
            assertEquals("My Project", assignment.getAssignmentScope().getName());

            wireMock.verify(getRequestedFor(urlPathEqualTo(
                    "/v1/roleAssignments/roleassignment:07218c48-343d-43e2-9831-a2080445cd22")));
        }

        @Test
        @DisplayName("404 throws EquinixNotFoundException")
        void notFound() {
            stubErrorInline(wireMock, "/v1/roleAssignments/[^/]+",
                    404, "{\"errorCode\":\"IAM-404\",\"errorMessage\":\"Role assignment not found\"}");

            assertThrows(EquinixNotFoundException.class,
                    () -> iam.roleAssignments().getByUuid("roleassignment:does-not-exist"));

            wireMock.verify(getRequestedFor(urlPathEqualTo("/v1/roleAssignments/roleassignment:does-not-exist")));
        }
    }

    @Nested
    @DisplayName("roleAssignments().create()")
    class CreateRoleAssignment {

        @Test
        @DisplayName("POSTs /v1/roleAssignments with the principal/roleId/scope body")
        void createsAssignment() {
            stubCreate(wireMock, "/v1/roleAssignments", "/json/iam/role_assignment_response.json");

            RoleAssignment created = iam.roleAssignments().create(
                    new CreateRoleAssignmentRequest()
                            .principal("user:alice")
                            .roleId("role:550e8400-e29b-41d4-a716-446655440000")
                            .assignmentScope(new CreateRoleAssignmentRequest.AssignmentScope()
                                    .id("abc-123")
                                    .type("PROJECT")));

            assertNotNull(created);
            assertEquals("roleassignment:07218c48-343d-43e2-9831-a2080445cd22", created.getRoleAssignmentId());

            wireMock.verify(postRequestedFor(urlPathEqualTo("/v1/roleAssignments"))
                    .withRequestBody(matchingJsonPath("$.principal", equalTo("user:alice")))
                    .withRequestBody(matchingJsonPath("$.roleId", equalTo("role:550e8400-e29b-41d4-a716-446655440000")))
                    .withRequestBody(matchingJsonPath("$.assignmentScope.id", equalTo("abc-123")))
                    .withRequestBody(matchingJsonPath("$.assignmentScope.type", equalTo("PROJECT"))));
        }

        @Test
        @DisplayName("409 throws EquinixConflictException")
        void conflict() {
            stubErrorInline(wireMock, "/v1/roleAssignments",
                    409, "{\"errorCode\":\"IAM-409\",\"errorMessage\":\"Role assignment already exists\"}");

            assertThrows(EquinixConflictException.class,
                    () -> iam.roleAssignments().create(
                            new CreateRoleAssignmentRequest()
                                    .principal("user:alice")
                                    .roleId("role:550e8400-e29b-41d4-a716-446655440000")
                                    .assignmentScope(new CreateRoleAssignmentRequest.AssignmentScope()
                                            .id("abc-123")
                                            .type("PROJECT"))));

            wireMock.verify(postRequestedFor(urlPathEqualTo("/v1/roleAssignments")));
        }
    }

    @Nested
    @DisplayName("roleAssignments().delete()")
    class DeleteRoleAssignment {

        @Test
        @DisplayName("DELETEs /v1/roleAssignments/{id} and returns true on 204")
        void deletesAssignment() {
            stubDeleteNoContent(wireMock, "/v1/roleAssignments/[^/]+");

            Boolean deleted = iam.roleAssignments()
                    .delete("roleassignment:07218c48-343d-43e2-9831-a2080445cd22");

            assertTrue(deleted);

            wireMock.verify(deleteRequestedFor(urlPathEqualTo(
                    "/v1/roleAssignments/roleassignment:07218c48-343d-43e2-9831-a2080445cd22")));
        }

        @Test
        @DisplayName("404 on delete throws EquinixNotFoundException")
        void deleteNotFound() {
            stubErrorInline(wireMock, "/v1/roleAssignments/[^/]+",
                    404, "{\"errorCode\":\"IAM-404\",\"errorMessage\":\"Role assignment not found\"}");

            assertThrows(EquinixNotFoundException.class,
                    () -> iam.roleAssignments().delete("roleassignment:does-not-exist"));

            wireMock.verify(deleteRequestedFor(urlPathEqualTo("/v1/roleAssignments/roleassignment:does-not-exist")));
        }
    }
}
