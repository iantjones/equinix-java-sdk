package api.equinix.javasdk;

import api.equinix.javasdk.core.IntegrationTestBase;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.iam.model.AccessPolicy;
import api.equinix.javasdk.iam.model.AccessPolicyGrant;
import api.equinix.javasdk.iam.model.EffectivePermissions;
import api.equinix.javasdk.iam.model.ListedAction;
import api.equinix.javasdk.iam.model.PermissionSet;
import api.equinix.javasdk.iam.model.PolicyMask;
import api.equinix.javasdk.iam.model.PrincipalPolicy;
import api.equinix.javasdk.iam.model.ResourceType;
import api.equinix.javasdk.iam.model.Role;
import api.equinix.javasdk.iam.model.RoleAssignment;
import api.equinix.javasdk.iam.model.ServiceActionSet;
import api.equinix.javasdk.iam.model.ServicePolicySchema;
import api.equinix.javasdk.iam.model.json.AccessPolicyGrantList;
import api.equinix.javasdk.iam.model.json.AccessPolicyList;
import api.equinix.javasdk.iam.model.json.ActionList;
import api.equinix.javasdk.iam.model.json.PermissionSetList;
import api.equinix.javasdk.iam.model.json.PolicyMaskList;
import api.equinix.javasdk.iam.model.json.PrincipalPolicyList;
import api.equinix.javasdk.iam.model.json.ResourceTypeActionPage;
import api.equinix.javasdk.iam.model.json.ResourceTypeList;
import api.equinix.javasdk.iam.model.json.RoleAssignmentList;
import api.equinix.javasdk.iam.model.json.RoleList;
import api.equinix.javasdk.iam.model.json.ServiceActionSetList;
import api.equinix.javasdk.projects.model.Project;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Live integration tests for the IAM domain of the Equinix Java SDK, catalog-complete against the
 * safe (read-only) operations of the {@code accessv1.yaml} spec: the roles catalog
 * ({@code listRoles} / {@code listRolesByProjectId}), role assignments
 * ({@code listRoleAssignments} / {@code getRoleAssignment}), access policies and their grants
 * ({@code listAccessPolicies} / {@code getAccessPolicy} / {@code listGrants}), permission sets
 * ({@code listPermissionSets} / {@code getPermissionSet}), principal policies
 * ({@code listPrincipalPolicies} / {@code getPrincipalPolicy}), policy masks
 * ({@code listPolicyMasks} / {@code getPolicyMask}) and the access-model discovery reads
 * ({@code listResourceTypes}, {@code listActions}, {@code listActionSets},
 * {@code pageResourceTypeActions}, {@code getServicePolicySchema}, {@code getEffectivePermissions}).
 *
 * <p>Spec-vs-reality contract: every call runs through
 * {@code IntegrationTestBase.requireEntitled}, which skips only on a 401/403 entitlement gap and
 * fails on any other defect (deserialization crash, 5xx, unmapped enum). Item-GETs discover an
 * existing id from the corresponding list first and skip when the account has none.</p>
 *
 * <h3>Test inputs</h3>
 * <ul>
 *     <li><b>projectId</b> — {@code -DiamProjectId=project:...} when supplied (used verbatim);
 *         otherwise derived from the first project returned by the Projects domain
 *         ({@code getAllProjects}), normalized to the accessv1 typed-id form
 *         ({@code project:} prefix). Project-scoped tests skip when neither yields a project.</li>
 *     <li><b>serviceId</b> — accessv1 offers no endpoint to discover service ids, so the
 *         service-scoped discovery reads require {@code -DiamServiceId=service:...} and skip
 *         when it is not supplied.</li>
 * </ul>
 *
 * <h3>Usage</h3>
 * <pre>
 * mvn test -Pintegration-readonly -DaccessKey=ID -DsecretKey=SECRET \
 *     [-DiamProjectId=project:abc-123] [-DiamServiceId=service:fabric]
 * </pre>
 */
@Tag("integration-readonly")
@DisplayName("IAM Integration Tests")
class IAMIntegrationTest extends IntegrationTestBase {

    static IAM iam;
    static String projectId;
    static String serviceId;
    static String projectDiscoveryNote;

    @BeforeAll
    static void setUpIam() {
        iam = new IAM(testCredentials());
        serviceId = System.getProperty("iamServiceId");
        projectId = resolveProjectId();
    }

    /**
     * Resolves the project id used by all project-scoped tests: the {@code -DiamProjectId} system
     * property verbatim when supplied, otherwise the first project discovered through the Projects
     * domain, normalized to the accessv1 typed-id form ({@code project:} prefix).
     */
    private static String resolveProjectId() {
        String explicit = System.getProperty("iamProjectId");
        if (explicit != null && !explicit.isBlank()) {
            return explicit;
        }
        try {
            PaginatedList<Project> projects = new Projects(testCredentials()).projects().list();
            if (projects != null && !projects.isEmpty()) {
                String raw = projects.get(0).getProjectId();
                if (raw != null && !raw.isBlank()) {
                    return raw.startsWith("project:") ? raw : "project:" + raw;
                }
            }
            projectDiscoveryNote = "the project list returned no usable projectId";
        } catch (Exception e) {
            projectDiscoveryNote = "project discovery via the Projects domain failed: " + e.getMessage();
        }
        return null;
    }

    static void assumeProjectAvailable() {
        Assumptions.assumeTrue(projectId != null,
                "No projectId available for project-scoped IAM tests (" +
                        (projectDiscoveryNote != null ? projectDiscoveryNote : "no project discovered") +
                        "); supply -DiamProjectId to run them");
    }

    static void assumeServiceConfigured() {
        Assumptions.assumeTrue(serviceId != null && !serviceId.isBlank(),
                "accessv1 has no service-discovery endpoint; supply -DiamServiceId to run the " +
                        "service-scoped discovery reads");
    }

    // ════════════════════════════════════════════════════════════════════
    //  READONLY TESTS - Safe GET/list operations
    // ════════════════════════════════════════════════════════════════════

    @Nested
    @Tag("integration-readonly")
    @DisplayName("Roles Read-Only Tests")
    class RoleTests {

        @Test
        @DisplayName("roles_list - List all visible roles (listRoles)")
        void roles_list() {
            RoleList roles = requireEntitled("IAM", "listRoles", "Role", "GET",
                    () -> iam.roles().list());
            assertNotNull(roles);

            if (roles.getList() != null && !roles.getList().isEmpty()) {
                Role first = roles.getList().get(0);
                assertNotNull(first.getRoleId(), "listRoles returned a role without a roleId");
                first.getName();
                first.getAssignmentScopeTypes();
                first.getPermissions();
            }
        }

        @Test
        @DisplayName("roles_listByProject - List roles scoped to a project (listRolesByProjectId)")
        void roles_listByProject() {
            assumeProjectAvailable();

            RoleList roles = requireEntitled("IAM", "listRolesByProjectId", "Role", "GET",
                    () -> iam.roles().listByProject(projectId));
            assertNotNull(roles);

            if (roles.getList() != null && !roles.getList().isEmpty()) {
                Role first = roles.getList().get(0);
                assertNotNull(first.getRoleId(), "listRolesByProjectId returned a role without a roleId");
                first.getName();
            }
        }
    }

    @Nested
    @Tag("integration-readonly")
    @DisplayName("Role Assignment Read-Only Tests")
    class RoleAssignmentTests {

        @Test
        @DisplayName("roleAssignments_list - List role assignments for the project scope (listRoleAssignments)")
        void roleAssignments_list() {
            assumeProjectAvailable();

            RoleAssignmentList assignments = requireEntitled("IAM", "listRoleAssignments", "RoleAssignment", "GET",
                    () -> iam.roleAssignments().list(projectId, "PROJECT"));
            assertNotNull(assignments);

            if (assignments.getList() != null && !assignments.getList().isEmpty()) {
                RoleAssignment first = assignments.getList().get(0);
                assertNotNull(first.getRoleAssignmentId(),
                        "listRoleAssignments returned an assignment without a roleAssignmentId");
                first.getPrincipal();
                first.getRoleId();
                first.getAssignmentScope();
            }
        }

        @Test
        @DisplayName("roleAssignments_getByUuid - Get role assignment by id (getRoleAssignment, if any exist)")
        void roleAssignments_getByUuid() {
            assumeProjectAvailable();

            RoleAssignmentList assignments = requireEntitled("IAM", "listRoleAssignments", "RoleAssignment", "GET",
                    () -> iam.roleAssignments().list(projectId, "PROJECT"));
            Assumptions.assumeTrue(assignments.getList() != null && !assignments.getList().isEmpty(),
                    "No role assignments found; skipping get test");

            String roleAssignmentId = assignments.getList().get(0).getRoleAssignmentId();
            RoleAssignment assignment = requireEntitled("IAM", "getRoleAssignment", "RoleAssignment", "GET",
                    () -> iam.roleAssignments().getByUuid(roleAssignmentId));
            assertNotNull(assignment);
            assertEquals(roleAssignmentId, assignment.getRoleAssignmentId());
            assignment.getPrincipal();
            assignment.getRoleName();
        }
    }

    @Nested
    @Tag("integration-readonly")
    @DisplayName("Access Policy Read-Only Tests")
    class AccessPolicyTests {

        @Test
        @DisplayName("accessPolicies_list - List access policies of a project (listAccessPolicies)")
        void accessPolicies_list() {
            assumeProjectAvailable();

            AccessPolicyList policies = requireEntitled("IAM", "listAccessPolicies", "AccessPolicy", "GET",
                    () -> iam.accessPolicies().list(projectId));
            assertNotNull(policies);

            if (policies.getList() != null && !policies.getList().isEmpty()) {
                AccessPolicy first = policies.getList().get(0);
                assertNotNull(first.getAccessPolicyId(),
                        "listAccessPolicies returned a policy without an accessPolicyId");
                first.getErn();
                first.getPermissions();
                first.getRev();
            }
        }

        @Test
        @DisplayName("accessPolicies_getByUuid - Get access policy by id (getAccessPolicy, if any exist)")
        void accessPolicies_getByUuid() {
            assumeProjectAvailable();

            AccessPolicyList policies = requireEntitled("IAM", "listAccessPolicies", "AccessPolicy", "GET",
                    () -> iam.accessPolicies().list(projectId));
            Assumptions.assumeTrue(policies.getList() != null && !policies.getList().isEmpty(),
                    "No access policies found; skipping get test");

            String accessPolicyId = policies.getList().get(0).getAccessPolicyId();
            AccessPolicy policy = requireEntitled("IAM", "getAccessPolicy", "AccessPolicy", "GET",
                    () -> iam.accessPolicies().getByUuid(projectId, accessPolicyId));
            assertNotNull(policy);
            assertEquals(accessPolicyId, policy.getAccessPolicyId());
            policy.getPermissions();
            policy.getCreatedBy();
        }

        @Test
        @DisplayName("accessPolicies_listGrants - List grants of an access policy (listGrants, if any exist)")
        void accessPolicies_listGrants() {
            assumeProjectAvailable();

            AccessPolicyList policies = requireEntitled("IAM", "listAccessPolicies", "AccessPolicy", "GET",
                    () -> iam.accessPolicies().list(projectId));
            Assumptions.assumeTrue(policies.getList() != null && !policies.getList().isEmpty(),
                    "No access policies found; skipping grants test");

            String accessPolicyId = policies.getList().get(0).getAccessPolicyId();
            AccessPolicyGrantList grants = requireEntitled("IAM", "listGrants", "AccessPolicyGrant", "GET",
                    () -> iam.accessPolicies().listGrants(projectId, accessPolicyId));
            assertNotNull(grants);

            if (grants.getList() != null && !grants.getList().isEmpty()) {
                AccessPolicyGrant first = grants.getList().get(0);
                assertNotNull(first.getGrantId(), "listGrants returned a grant without a grantId");
                first.getGrantee();
                first.getAccessPolicyErn();
            }
        }
    }

    @Nested
    @Tag("integration-readonly")
    @DisplayName("Permission Set Read-Only Tests")
    class PermissionSetTests {

        @Test
        @DisplayName("permissionSets_list - List permission sets of a project (listPermissionSets)")
        void permissionSets_list() {
            assumeProjectAvailable();

            PermissionSetList sets = requireEntitled("IAM", "listPermissionSets", "PermissionSet", "GET",
                    () -> iam.permissionSets().list(projectId));
            assertNotNull(sets);

            if (sets.getList() != null && !sets.getList().isEmpty()) {
                PermissionSet first = sets.getList().get(0);
                assertNotNull(first.getPermissionSetId(),
                        "listPermissionSets returned a set without a permissionSetId");
                first.getErn();
                first.getPermissions();
                first.getRev();
            }
        }

        @Test
        @DisplayName("permissionSets_getByUuid - Get permission set by id (getPermissionSet, if any exist)")
        void permissionSets_getByUuid() {
            assumeProjectAvailable();

            PermissionSetList sets = requireEntitled("IAM", "listPermissionSets", "PermissionSet", "GET",
                    () -> iam.permissionSets().list(projectId));
            Assumptions.assumeTrue(sets.getList() != null && !sets.getList().isEmpty(),
                    "No permission sets found; skipping get test");

            String permissionSetId = sets.getList().get(0).getPermissionSetId();
            PermissionSet set = requireEntitled("IAM", "getPermissionSet", "PermissionSet", "GET",
                    () -> iam.permissionSets().getByUuid(projectId, permissionSetId));
            assertNotNull(set);
            assertEquals(permissionSetId, set.getPermissionSetId());
            set.getPermissions();
            set.getCreatedBy();
        }
    }

    @Nested
    @Tag("integration-readonly")
    @DisplayName("Principal Policy Read-Only Tests")
    class PrincipalPolicyTests {

        @Test
        @DisplayName("principalPolicies_list - List principal policies of a project (listPrincipalPolicies)")
        void principalPolicies_list() {
            assumeProjectAvailable();

            PrincipalPolicyList policies = requireEntitled("IAM", "listPrincipalPolicies", "PrincipalPolicy", "GET",
                    () -> iam.principalPolicies().list(projectId));
            assertNotNull(policies);

            if (policies.getList() != null && !policies.getList().isEmpty()) {
                PrincipalPolicy first = policies.getList().get(0);
                assertNotNull(first.getUserPrincipal(),
                        "listPrincipalPolicies returned a policy without a userPrincipal");
                first.getPermissions();
                first.getRev();
            }
        }

        @Test
        @DisplayName("principalPolicies_getByUuid - Get principal policy by user principal (getPrincipalPolicy, if any exist)")
        void principalPolicies_getByUuid() {
            assumeProjectAvailable();

            PrincipalPolicyList policies = requireEntitled("IAM", "listPrincipalPolicies", "PrincipalPolicy", "GET",
                    () -> iam.principalPolicies().list(projectId));
            Assumptions.assumeTrue(policies.getList() != null && !policies.getList().isEmpty(),
                    "No principal policies found; skipping get test");

            String userPrincipal = policies.getList().get(0).getUserPrincipal();
            PrincipalPolicy policy = requireEntitled("IAM", "getPrincipalPolicy", "PrincipalPolicy", "GET",
                    () -> iam.principalPolicies().getByUuid(projectId, userPrincipal));
            // The spec documents that a disabled policy is returned as an empty (nil) body, so a
            // null result is tolerated; when present, the body must deserialize coherently.
            if (policy != null) {
                assertEquals(userPrincipal, policy.getUserPrincipal());
                policy.getPermissions();
                policy.getDisabledPolicy();
            }
        }
    }

    @Nested
    @Tag("integration-readonly")
    @DisplayName("Policy Mask Read-Only Tests")
    class PolicyMaskTests {

        @Test
        @DisplayName("policyMasks_list - List policy masks of a project (listPolicyMasks)")
        void policyMasks_list() {
            assumeProjectAvailable();

            PolicyMaskList masks = requireEntitled("IAM", "listPolicyMasks", "PolicyMask", "GET",
                    () -> iam.policyMasks().list(projectId));
            assertNotNull(masks);

            if (masks.getList() != null && !masks.getList().isEmpty()) {
                PolicyMask first = masks.getList().get(0);
                assertNotNull(first.getPolicyMaskId(),
                        "listPolicyMasks returned a mask without a policyMaskId");
                first.getManagedPolicies();
                first.getManagedPermissionSets();
                first.getRev();
            }
        }

        @Test
        @DisplayName("policyMasks_getByUuid - Get policy mask by id (getPolicyMask, if any exist)")
        void policyMasks_getByUuid() {
            assumeProjectAvailable();

            PolicyMaskList masks = requireEntitled("IAM", "listPolicyMasks", "PolicyMask", "GET",
                    () -> iam.policyMasks().list(projectId));
            Assumptions.assumeTrue(masks.getList() != null && !masks.getList().isEmpty(),
                    "No policy masks found; skipping get test");

            String policyMaskId = masks.getList().get(0).getPolicyMaskId();
            PolicyMask mask = requireEntitled("IAM", "getPolicyMask", "PolicyMask", "GET",
                    () -> iam.policyMasks().getByUuid(projectId, policyMaskId));
            assertNotNull(mask);
            assertEquals(policyMaskId, mask.getPolicyMaskId());
            mask.getManagedPolicies();
            mask.getSubtract();
        }
    }

    @Nested
    @Tag("integration-readonly")
    @DisplayName("Access-Model Discovery Read-Only Tests")
    class AccessModelDiscoveryTests {

        @Test
        @DisplayName("resourceTypes_list - List resource types of a service (listResourceTypes)")
        void resourceTypes_list() {
            assumeProjectAvailable();
            assumeServiceConfigured();

            ResourceTypeList types = requireEntitled("IAM", "listResourceTypes", "ResourceType", "GET",
                    () -> iam.resourceTypes().listResourceTypes(projectId, serviceId));
            assertNotNull(types);

            if (types.getList() != null && !types.getList().isEmpty()) {
                ResourceType first = types.getList().get(0);
                assertNotNull(first.getResourceType(),
                        "listResourceTypes returned an entry without a resourceType");
                first.getErn();
                first.getAttributes();
            }
        }

        @Test
        @DisplayName("actions_list - List actions of a service (listActions)")
        void actions_list() {
            assumeProjectAvailable();
            assumeServiceConfigured();

            ActionList actions = requireEntitled("IAM", "listActions", "ListedAction", "GET",
                    () -> iam.resourceTypes().listActions(projectId, serviceId));
            assertNotNull(actions);

            if (actions.getList() != null && !actions.getList().isEmpty()) {
                ListedAction first = actions.getList().get(0);
                assertNotNull(first.getActionId(), "listActions returned an action without an actionId");
                first.getServiceAspect();
                first.getRbacPermission();
            }
        }

        @Test
        @DisplayName("actionSets_list - List action sets of a service (listActionSets)")
        void actionSets_list() {
            assumeProjectAvailable();
            assumeServiceConfigured();

            ServiceActionSetList actionSets = requireEntitled("IAM", "listActionSets", "ServiceActionSet", "GET",
                    () -> iam.resourceTypes().listActionSets(projectId, serviceId));
            assertNotNull(actionSets);

            if (actionSets.getList() != null && !actionSets.getList().isEmpty()) {
                ServiceActionSet first = actionSets.getList().get(0);
                assertNotNull(first.getActionSetId(),
                        "listActionSets returned an action set without an actionSetId");
                first.getServiceId();
                first.getActionSet();
            }
        }

        @Test
        @DisplayName("resourceTypeActions_page - Page resource-type actions (pageResourceTypeActions, if any resource types exist)")
        void resourceTypeActions_page() {
            assumeProjectAvailable();
            assumeServiceConfigured();

            ResourceTypeList types = requireEntitled("IAM", "listResourceTypes", "ResourceType", "GET",
                    () -> iam.resourceTypes().listResourceTypes(projectId, serviceId));
            Assumptions.assumeTrue(types.getList() != null && !types.getList().isEmpty(),
                    "No resource types found for serviceId " + serviceId + "; skipping resource-type actions test");

            String resourceType = types.getList().get(0).getResourceType();
            ResourceTypeActionPage page = requireEntitled("IAM", "pageResourceTypeActions", "ResourceTypeAction", "GET",
                    () -> iam.resourceTypes().listResourceTypeActions(projectId, serviceId, resourceType));
            assertNotNull(page);

            if (page.getList() != null && !page.getList().isEmpty()) {
                assertNotNull(page.getList().get(0).getAction(),
                        "pageResourceTypeActions returned an entry without an action");
                page.getList().get(0).getResourceType();
            }
        }

        @Test
        @DisplayName("servicePolicySchema_get - Get the Cedar policy schema of a service (getServicePolicySchema)")
        void servicePolicySchema_get() {
            assumeProjectAvailable();
            assumeServiceConfigured();

            ServicePolicySchema schema = requireEntitled("IAM", "getServicePolicySchema", "ServicePolicySchema", "GET",
                    () -> iam.resourceTypes().getServicePolicySchema(projectId, serviceId));
            assertNotNull(schema);
            schema.getSchema();
        }

        @Test
        @DisplayName("effectivePermissions_get - Resolve the caller's effective permissions (getEffectivePermissions)")
        void effectivePermissions_get() {
            assumeProjectAvailable();
            assumeServiceConfigured();

            EffectivePermissions permissions = requireEntitled("IAM", "getEffectivePermissions",
                    "EffectivePermissions", "GET",
                    () -> iam.effectivePermissions().get(projectId, serviceId));
            assertNotNull(permissions);
            permissions.getPrincipalId();
            permissions.getAccessPolicyIds();
            permissions.getPermissions();
        }
    }
}
