package api.equinix.javasdk.iam;

import api.equinix.javasdk.core.internal.Constants;
import api.equinix.javasdk.iam.model.PolicyExpression;
import api.equinix.javasdk.iam.model.json.AccessPolicyJson;
import api.equinix.javasdk.iam.model.json.PolicyMaskJson;
import api.equinix.javasdk.iam.model.json.creators.CreateAccessPolicyRequest;
import api.equinix.javasdk.iam.model.json.creators.CreatePolicyMaskRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Round-trip tests for {@link PolicyExpression}, the lossless typed wrapper for the polymorphic
 * {@code oneOf} union members used by the IAM access-control schemas (the
 * {@code permissions}/{@code intersect}/{@code subtract} entries of access policies / permission
 * sets / principal policies, and the {@code managedPolicies}/{@code managedPermissionSets}/
 * {@code subtract} fields of policy masks).
 *
 * <p>The tests prove that <em>both</em> the bare-string form and the structured (object/array)
 * form survive a deserialize-then-serialize round-trip unchanged — on read (response models) and
 * on write (request creators).</p>
 */
class PolicyExpressionDeserializationTest {

    private static ObjectMapper objectMapper;

    @BeforeAll
    static void setUp() {
        objectMapper = Constants.objectMapper;
    }

    private static JsonNode load(String resource) throws Exception {
        try (InputStream is = PolicyExpressionDeserializationTest.class.getResourceAsStream(resource)) {
            assertNotNull(is, resource + " fixture not found on classpath");
            return objectMapper.readTree(is);
        }
    }

    @Test
    void accessPolicy_permissions_bothFormsDeserialize() throws Exception {
        JsonNode original = load("/json/iam/access_policy_response.json");
        AccessPolicyJson policy = objectMapper.treeToValue(original, AccessPolicyJson.class);

        List<PolicyExpression> permissions = policy.getPermissions();
        assertNotNull(permissions);
        assertEquals(4, permissions.size());

        // First two entries are bare-string members (a permission-set id and an ERN).
        assertTrue(permissions.get(0).isString());
        assertEquals("permissionset:read-only", permissions.get(0).asString());
        assertTrue(permissions.get(1).isString());
        assertEquals("ern:access:us-west:proj-456:permissionset/shared", permissions.get(1).asString());

        // Third entry is a structured inline-permission object.
        assertTrue(permissions.get(2).isObject());
        assertFalse(permissions.get(2).isString());
        assertEquals("all", permissions.get(2).toJsonNode().get("resources").asText());

        // Fourth entry is a structured foreign-access-policy reference object.
        assertTrue(permissions.get(3).isObject());
        assertEquals("ern:access:us-west:proj-789:accesspolicy/foreign",
                permissions.get(3).toJsonNode().get("foreignAccessPolicy").asText());

        // intersect carries the literal "all" string; subtract carries a managed-set id string.
        assertEquals("all", policy.getIntersect().get(0).asString());
        assertEquals("managedset:dangerous", policy.getSubtract().get(0).asString());
    }

    @Test
    void accessPolicy_roundTripsLosslessly_onRead() throws Exception {
        JsonNode original = load("/json/iam/access_policy_response.json");
        AccessPolicyJson policy = objectMapper.treeToValue(original, AccessPolicyJson.class);

        JsonNode reserialized = objectMapper.valueToTree(policy);

        // Every union member (string and structured alike) is re-emitted exactly as received.
        assertEquals(original.get("permissions"), reserialized.get("permissions"));
        assertEquals(original.get("intersect"), reserialized.get("intersect"));
        assertEquals(original.get("subtract"), reserialized.get("subtract"));
    }

    @Test
    void policyMask_bothFormsDeserialize() throws Exception {
        JsonNode original = load("/json/iam/policy_mask_response.json");
        PolicyMaskJson mask = objectMapper.treeToValue(original, PolicyMaskJson.class);

        // managedPolicies is the literal string "none".
        assertTrue(mask.getManagedPolicies().isString());
        assertEquals("none", mask.getManagedPolicies().asString());

        // managedPermissionSets is an array of ids.
        assertTrue(mask.getManagedPermissionSets().isArray());
        assertEquals(List.of("managedset:reader", "managedset:writer"),
                mask.getManagedPermissionSets().asStringList());

        // subtract is a structured object.
        assertTrue(mask.getSubtract().isObject());
        assertEquals("managedpolicy:dangerous",
                mask.getSubtract().toJsonNode().get("managedPolicies").get(0).asText());
    }

    @Test
    void policyMask_roundTripsLosslessly_onRead() throws Exception {
        JsonNode original = load("/json/iam/policy_mask_response.json");
        PolicyMaskJson mask = objectMapper.treeToValue(original, PolicyMaskJson.class);

        JsonNode reserialized = objectMapper.valueToTree(mask);

        assertEquals(original.get("managedPolicies"), reserialized.get("managedPolicies"));
        assertEquals(original.get("managedPermissionSets"), reserialized.get("managedPermissionSets"));
        assertEquals(original.get("subtract"), reserialized.get("subtract"));
    }

    @Test
    void createAccessPolicyRequest_serializesBothForms_onWrite() throws Exception {
        // A structured inline-permission entry built from a JSON node.
        JsonNode inline = objectMapper.readTree(
                "{\"resources\":\"all\",\"serviceActions\":\"all\"}");

        CreateAccessPolicyRequest request = new CreateAccessPolicyRequest()
                .accessPolicyId("accesspolicy:new")
                .permissions(List.of(
                        PolicyExpression.of("permissionset:read-only"),
                        PolicyExpression.of(inline)))
                .intersect(List.of(PolicyExpression.of("all")));

        JsonNode written = objectMapper.valueToTree(request);

        // The bare-string entry is emitted as a string, the structured entry as the object verbatim.
        assertEquals("permissionset:read-only", written.get("permissions").get(0).asText());
        assertTrue(written.get("permissions").get(1).isObject());
        assertEquals(inline, written.get("permissions").get(1));
        assertEquals("all", written.get("intersect").get(0).asText());
    }

    @Test
    void createPolicyMaskRequest_serializesBothForms_onWrite() throws Exception {
        JsonNode subtract = objectMapper.readTree(
                "{\"managedPolicies\":[\"managedpolicy:dangerous\"]}");

        CreatePolicyMaskRequest request = new CreatePolicyMaskRequest()
                .policyMaskId("policymask:new")
                .managedPolicies(PolicyExpression.of("none"))
                .managedPermissionSets(PolicyExpression.ofStrings(List.of("managedset:reader")))
                .subtract(PolicyExpression.of(subtract));

        JsonNode written = objectMapper.valueToTree(request);

        assertEquals("none", written.get("managedPolicies").asText());
        assertTrue(written.get("managedPermissionSets").isArray());
        assertEquals("managedset:reader", written.get("managedPermissionSets").get(0).asText());
        assertEquals(subtract, written.get("subtract"));
    }
}
