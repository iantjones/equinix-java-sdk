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
import api.equinix.javasdk.core.exception.EquinixNotFoundException;
import api.equinix.javasdk.core.exception.EquinixServerException;
import api.equinix.javasdk.iam.enums.ServiceAspect;
import api.equinix.javasdk.iam.model.EffectivePermissions;
import api.equinix.javasdk.iam.model.json.ActionList;
import api.equinix.javasdk.iam.model.json.ResourceTypeActionPage;
import api.equinix.javasdk.iam.model.json.ResourceTypeList;
import api.equinix.javasdk.iam.model.json.ServiceActionSetList;
import api.equinix.javasdk.iam.model.ServicePolicySchema;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static api.equinix.javasdk.core.ResponseStubs.stubError;
import static api.equinix.javasdk.core.ResponseStubs.stubPaginatedGet;
import static api.equinix.javasdk.core.ResponseStubs.stubSingleton;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * WireMock request-contract tests for the IAM access-model discovery reads — effective permissions
 * ({@code iam.effectivePermissions()} / {@link api.equinix.javasdk.iam.client.IAMEffectivePermissions})
 * and resource types ({@code iam.resourceTypes()} /
 * {@link api.equinix.javasdk.iam.client.IAMResourceTypes}).
 *
 * <p>Every operation here is a project-scoped read resolved against
 * {@code v1/projects/{projectId}/...} (see {@code apiParams_IAM.json}). The {@code serviceId} is a
 * required query parameter throughout; the token-paginated lists carry an opaque
 * {@code nextPageToken} and accept {@code pageToken}/{@code pageSize}; {@code pageResourceTypeActions}
 * uses cursor-based paging keyed on {@code lastAction}. Each test asserts the HTTP verb + path (and
 * the relevant query params), and at least one error mapping is exercised per family.</p>
 */
class IAMEffectivePermissionsWireMockTest extends WireMockTestBase {

    private static final String PROJECT_ID = "project:abc-123";
    private static final String SERVICE_ID = "service:equinix/fabric";

    private static final String EFFECTIVE_PERMISSIONS_PATH =
            "/v1/projects/" + PROJECT_ID + "/effectivePermissions";
    private static final String RESOURCE_TYPES_PATH = "/v1/projects/" + PROJECT_ID + "/resourceTypes";
    private static final String ACTIONS_PATH = "/v1/projects/" + PROJECT_ID + "/actions";
    private static final String ACTION_SETS_PATH = "/v1/projects/" + PROJECT_ID + "/actionSets";
    private static final String RESOURCE_TYPE_ACTIONS_PATH =
            "/v1/projects/" + PROJECT_ID + "/resourceTypeActions";
    private static final String SERVICE_POLICY_SCHEMAS_PATH =
            "/v1/projects/" + PROJECT_ID + "/servicePolicySchemas";

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
    @DisplayName("effectivePermissions().get()")
    class EffectivePermissionsGet {

        @Test
        @DisplayName("GETs the resolved permissions and passes the serviceId query param")
        void resolvesPermissions() {
            stubSingleton(wireMock, EFFECTIVE_PERMISSIONS_PATH,
                    "/json/iam/effective_permissions_response.json");

            EffectivePermissions permissions = iam.effectivePermissions().get(PROJECT_ID, SERVICE_ID);

            assertNotNull(permissions);
            assertEquals("user:alice", permissions.getPrincipalId());
            assertEquals(PROJECT_ID, permissions.getProjectId());
            assertEquals(SERVICE_ID, permissions.getServiceId());
            assertEquals(2, permissions.getAccessPolicyIds().size());
            assertEquals("accesspolicy:my-policy", permissions.getAccessPolicyIds().get(0));

            assertEquals(2, permissions.getPermissions().size());
            EffectivePermissions.Permission first = permissions.getPermissions().get(0);
            assertEquals(2, first.getActions().size());
            assertEquals("action:connection.read", first.getActions().get(0));
            // resources is an inclusion selector, metroCodes is an exclusion selector
            assertEquals("ern:fabric:us-west:proj-abc-123:connection/conn-1",
                    first.getResources().getInclude().get(0));
            assertNull(first.getResources().getExcept());
            assertEquals(2, first.getMetroCodes().getExcept().size());
            assertNull(first.getMetroCodes().getInclude());
            assertEquals("SV5", first.getIbxIds().getInclude().get(0));
            assertEquals("context.myAttribute < 5", first.getCondition());

            wireMock.verify(getRequestedFor(urlPathEqualTo(EFFECTIVE_PERMISSIONS_PATH))
                    .withQueryParam("serviceId", equalTo(SERVICE_ID)));
        }

        @Test
        @DisplayName("passes the projectErn query param when supplied")
        void resolvesWithProjectErn() {
            stubSingleton(wireMock, EFFECTIVE_PERMISSIONS_PATH,
                    "/json/iam/effective_permissions_response.json");

            iam.effectivePermissions().get(PROJECT_ID, SERVICE_ID,
                    "ern:access:us-west:proj-abc-123:project");

            wireMock.verify(getRequestedFor(urlPathEqualTo(EFFECTIVE_PERMISSIONS_PATH))
                    .withQueryParam("serviceId", equalTo(SERVICE_ID))
                    .withQueryParam("projectErn", equalTo("ern:access:us-west:proj-abc-123:project")));
        }

        @Test
        @DisplayName("404 throws EquinixNotFoundException")
        void notFound() {
            stubError(wireMock, EFFECTIVE_PERMISSIONS_PATH, 404, "/json/core/error_404_response.json");

            assertThrows(EquinixNotFoundException.class,
                    () -> iam.effectivePermissions().get(PROJECT_ID, SERVICE_ID));

            wireMock.verify(getRequestedFor(urlPathEqualTo(EFFECTIVE_PERMISSIONS_PATH)));
        }
    }

    @Nested
    @DisplayName("resourceTypes().listResourceTypes()")
    class ListResourceTypes {

        @Test
        @DisplayName("GETs the resource types, maps the page and passes serviceId")
        void listsFirstPage() {
            stubPaginatedGet(wireMock, RESOURCE_TYPES_PATH, "/json/iam/resource_type_list_response.json");

            ResourceTypeList page = iam.resourceTypes().listResourceTypes(PROJECT_ID, SERVICE_ID);

            assertNotNull(page);
            assertEquals(2, page.getList().size());
            assertEquals("connection", page.getList().get(0).getResourceType());
            assertEquals("ern:fabric::service:fabric:resourceType/connection", page.getList().get(0).getErn());
            assertEquals("platform", page.getList().get(0).getTags().get("team"));
            assertEquals("rev-1", page.getList().get(0).getRev());
            // attributes are objects per the accessv1 AttributeSet schema, not bare strings
            assertEquals(2, page.getList().get(0).getAttributes().size());
            assertEquals("attribute:metroCode", page.getList().get(0).getAttributes().get(0).getAttributeId());
            assertEquals("port", page.getList().get(1).getResourceType());
            assertEquals("next-page-token-abc", page.getNextPageToken());

            wireMock.verify(getRequestedFor(urlPathEqualTo(RESOURCE_TYPES_PATH))
                    .withQueryParam("serviceId", equalTo(SERVICE_ID)));
        }

        @Test
        @DisplayName("passes pageToken + pageSize + projectErn query params")
        void listsWithPagination() {
            stubPaginatedGet(wireMock, RESOURCE_TYPES_PATH, "/json/iam/resource_type_list_response.json");

            iam.resourceTypes().listResourceTypes(PROJECT_ID, SERVICE_ID, "next-page-token-abc", 50,
                    "ern:access:us-west:proj-abc-123:project");

            wireMock.verify(getRequestedFor(urlPathEqualTo(RESOURCE_TYPES_PATH))
                    .withQueryParam("serviceId", equalTo(SERVICE_ID))
                    .withQueryParam("pageToken", equalTo("next-page-token-abc"))
                    .withQueryParam("pageSize", equalTo("50"))
                    .withQueryParam("projectErn", equalTo("ern:access:us-west:proj-abc-123:project")));
        }

        @Test
        @DisplayName("empty list -> empty page, no nextPageToken")
        void listsEmpty() {
            wireMock.stubFor(get(urlPathEqualTo(RESOURCE_TYPES_PATH))
                    .willReturn(okJson(loadFixture("/json/iam/resource_type_list_empty.json"))));

            ResourceTypeList page = iam.resourceTypes().listResourceTypes(PROJECT_ID, SERVICE_ID);

            assertNotNull(page);
            assertTrue(page.getList().isEmpty());
            assertNull(page.getNextPageToken());

            wireMock.verify(getRequestedFor(urlPathEqualTo(RESOURCE_TYPES_PATH)));
        }

        @Test
        @DisplayName("404 throws EquinixNotFoundException")
        void notFound() {
            stubError(wireMock, RESOURCE_TYPES_PATH, 404, "/json/core/error_404_response.json");

            assertThrows(EquinixNotFoundException.class,
                    () -> iam.resourceTypes().listResourceTypes(PROJECT_ID, SERVICE_ID));

            wireMock.verify(getRequestedFor(urlPathEqualTo(RESOURCE_TYPES_PATH)));
        }
    }

    @Nested
    @DisplayName("resourceTypes().listActions()")
    class ListActions {

        @Test
        @DisplayName("GETs the actions, maps the page and passes serviceId")
        void listsActions() {
            stubPaginatedGet(wireMock, ACTIONS_PATH, "/json/iam/action_list_response.json");

            ActionList page = iam.resourceTypes().listActions(PROJECT_ID, SERVICE_ID);

            assertNotNull(page);
            assertEquals(2, page.getList().size());
            assertEquals("action:use/listPermissionSets", page.getList().get(0).getActionId());
            assertEquals(ServiceAspect.USE, page.getList().get(0).getServiceAspect());
            assertEquals("fabric.permissionsets.read",
                    page.getList().get(0).getRbacPermission().getPermission());
            assertTrue(page.getList().get(0).getPermissionCodes().get("READ").getRequiresAll());
            assertEquals("actions-next-token", page.getNextPageToken());

            wireMock.verify(getRequestedFor(urlPathEqualTo(ACTIONS_PATH))
                    .withQueryParam("serviceId", equalTo(SERVICE_ID)));
        }

        @Test
        @DisplayName("passes pageToken + pageSize query params")
        void listsActionsWithPagination() {
            stubPaginatedGet(wireMock, ACTIONS_PATH, "/json/iam/action_list_response.json");

            iam.resourceTypes().listActions(PROJECT_ID, SERVICE_ID, "actions-next-token", 25, null);

            wireMock.verify(getRequestedFor(urlPathEqualTo(ACTIONS_PATH))
                    .withQueryParam("serviceId", equalTo(SERVICE_ID))
                    .withQueryParam("pageToken", equalTo("actions-next-token"))
                    .withQueryParam("pageSize", equalTo("25")));
        }
    }

    @Nested
    @DisplayName("resourceTypes().listActionSets()")
    class ListActionSets {

        @Test
        @DisplayName("GETs the action sets, maps the page and passes serviceId")
        void listsActionSets() {
            stubPaginatedGet(wireMock, ACTION_SETS_PATH, "/json/iam/action_set_list_response.json");

            ServiceActionSetList page = iam.resourceTypes().listActionSets(PROJECT_ID, SERVICE_ID);

            assertNotNull(page);
            assertEquals(1, page.getList().size());
            assertEquals("actionset:viewer", page.getList().get(0).getActionSetId());
            assertEquals(SERVICE_ID, page.getList().get(0).getServiceId());
            assertEquals(2, page.getList().get(0).getActionSet().size());
            assertEquals("action:use/listConnections", page.getList().get(0).getActionSet().get(0));
            assertEquals("action-sets-next-token", page.getNextPageToken());

            wireMock.verify(getRequestedFor(urlPathEqualTo(ACTION_SETS_PATH))
                    .withQueryParam("serviceId", equalTo(SERVICE_ID)));
        }

        @Test
        @DisplayName("paged overload passes serviceId/pageToken/pageSize/projectErn query params")
        void listsActionSetsWithPaging() {
            stubPaginatedGet(wireMock, ACTION_SETS_PATH, "/json/iam/action_set_list_response.json");

            ServiceActionSetList page = iam.resourceTypes().listActionSets(
                    PROJECT_ID, SERVICE_ID, "action-sets-next-token", 25,
                    "ern:access:us-west:proj-abc-123:project");

            assertNotNull(page);
            assertEquals(1, page.getList().size());
            assertEquals("actionset:viewer", page.getList().get(0).getActionSetId());
            assertEquals("action-sets-next-token", page.getNextPageToken());

            wireMock.verify(getRequestedFor(urlPathEqualTo(ACTION_SETS_PATH))
                    .withQueryParam("serviceId", equalTo(SERVICE_ID))
                    .withQueryParam("pageToken", equalTo("action-sets-next-token"))
                    .withQueryParam("pageSize", equalTo("25"))
                    .withQueryParam("projectErn", equalTo("ern:access:us-west:proj-abc-123:project")));
        }
    }

    @Nested
    @DisplayName("resourceTypes().pageResourceTypeActions()")
    class PageResourceTypeActions {

        @Test
        @DisplayName("GETs the resource-type actions and passes serviceId + resourceType")
        void pagesFirst() {
            stubPaginatedGet(wireMock, RESOURCE_TYPE_ACTIONS_PATH,
                    "/json/iam/resource_type_action_page_response.json");

            ResourceTypeActionPage page = iam.resourceTypes()
                    .pageResourceTypeActions(PROJECT_ID, SERVICE_ID, "resourcetype:connection");

            assertNotNull(page);
            assertEquals(2, page.getList().size());
            assertEquals("action:use/getConnection", page.getList().get(0).getAction());
            assertEquals("resourcetype:connection", page.getList().get(0).getResourceType());
            assertEquals("ern:fabric::service:fabric:resourceType/connection",
                    page.getList().get(0).getResourceTypeErn());
            assertEquals("rta-next-token", page.getNextPageToken());

            wireMock.verify(getRequestedFor(urlPathEqualTo(RESOURCE_TYPE_ACTIONS_PATH))
                    .withQueryParam("serviceId", equalTo(SERVICE_ID))
                    .withQueryParam("resourceType", equalTo("resourcetype:connection")));
        }

        @Test
        @DisplayName("passes the lastAction cursor + pageSize query params")
        void pagesWithCursor() {
            stubPaginatedGet(wireMock, RESOURCE_TYPE_ACTIONS_PATH,
                    "/json/iam/resource_type_action_page_response.json");

            iam.resourceTypes().pageResourceTypeActions(PROJECT_ID, SERVICE_ID, "resourcetype:connection",
                    null, "action:use/getConnection", 10, null);

            wireMock.verify(getRequestedFor(urlPathEqualTo(RESOURCE_TYPE_ACTIONS_PATH))
                    .withQueryParam("serviceId", equalTo(SERVICE_ID))
                    .withQueryParam("resourceType", equalTo("resourcetype:connection"))
                    .withQueryParam("lastAction", equalTo("action:use/getConnection"))
                    .withQueryParam("pageSize", equalTo("10")));
        }
    }

    @Nested
    @DisplayName("resourceTypes().getServicePolicySchema()")
    class GetServicePolicySchema {

        @Test
        @DisplayName("GETs the service policy schema and passes serviceId")
        void getsSchema() {
            stubSingleton(wireMock, SERVICE_POLICY_SCHEMAS_PATH,
                    "/json/iam/service_policy_schema_response.json");

            ServicePolicySchema schema = iam.resourceTypes().getServicePolicySchema(PROJECT_ID, SERVICE_ID);

            assertNotNull(schema);
            assertNotNull(schema.getSchema());
            assertEquals("string", schema.getSchema().get("metroCode"));
            assertEquals("integer", schema.getSchema().get("bandwidth"));

            wireMock.verify(getRequestedFor(urlPathEqualTo(SERVICE_POLICY_SCHEMAS_PATH))
                    .withQueryParam("serviceId", equalTo(SERVICE_ID)));
        }

        @Test
        @DisplayName("500 throws EquinixServerException")
        void serverError() {
            stubError(wireMock, SERVICE_POLICY_SCHEMAS_PATH, 500, "/json/core/error_500_response.json");

            assertThrows(EquinixServerException.class,
                    () -> iam.resourceTypes().getServicePolicySchema(PROJECT_ID, SERVICE_ID));

            wireMock.verify(getRequestedFor(urlPathEqualTo(SERVICE_POLICY_SCHEMAS_PATH)));
        }
    }
}
