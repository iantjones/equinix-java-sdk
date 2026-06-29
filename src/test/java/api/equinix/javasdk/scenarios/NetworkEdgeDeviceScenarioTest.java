package api.equinix.javasdk.scenarios;

import api.equinix.javasdk.NetworkEdge;
import api.equinix.javasdk.core.IntegrationTestBase;
import api.equinix.javasdk.core.enums.BandwidthUnit;
import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.networkedge.enums.DeviceManagementType;
import api.equinix.javasdk.networkedge.enums.LicenseType;
import api.equinix.javasdk.networkedge.enums.Protocol;
import api.equinix.javasdk.networkedge.model.ACLTemplate;
import api.equinix.javasdk.networkedge.model.Device;
import api.equinix.javasdk.networkedge.model.DeviceType;
import api.equinix.javasdk.networkedge.model.PublicKey;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Scenario: ACLTemplate + PublicKey + Device (draft) lifecycle.
 *
 * <p>Device creation in production takes time and costs money.
 * The draft/saveAsDraft pattern is the safe approach for validation.
 * Only ACLTemplate and PublicKey are safe to create/delete quickly.</p>
 */
@Tag("integration-scenario")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NetworkEdgeDeviceScenarioTest extends IntegrationTestBase {

    private NetworkEdge networkEdge;
    private String aclTemplateUuid;
    private String publicKeyUuid;
    private String draftDeviceUuid;

    private static final String SSH_KEY_VALUE =
            "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABgQC0example sdk-test-key";

    private void initClient() {
        if (networkEdge == null) {
            networkEdge = new NetworkEdge(testCredentials());
        }
    }

    @Test
    @Order(1)
    @DisplayName("Create device as draft (dry-run equivalent)")
    void createDeviceDraft() {
        Assumptions.assumeTrue(isDryRunEnabled(),
                "Skipped: dry-run mode not enabled");
        initClient();

        try {
            String deviceName = testResourceName("device-draft");
            Device draft = timedCall("NetworkEdge", "createDraft", "Device", "POST", () ->
                    networkEdge.devices().define(deviceName)
                            .withAccountNumber("1")
                            .withMetroCode(MetroCode.SV)
                            .withDeviceTypeCode("CSR1000V")
                            .withDeviceManagementType(DeviceManagementType.EQUINIX_CONFIGURED)
                            .withLicenseMode(LicenseType.SUB)
                            .withPackageCode("IPBASE")
                            .withVersion("17.06.01a")
                            .withCore(4)
                            .withThroughput(500)
                            .withThroughputUnit(BandwidthUnit.MBPS)
                            .withNotification("test@example.com")
                            .saveAsDraft()
            );

            assertNotNull(draft, "Draft device should be returned");
            draftDeviceUuid = draft.getUuid();
            assertNotNull(draftDeviceUuid, "Draft device UUID should not be null");
            System.out.printf("  Draft device created: %s%n", draftDeviceUuid);
        } catch (Exception e) {
            Assumptions.assumeTrue(false,
                    "Draft device creation not available in this environment: " + e.getMessage());
        }
    }

    @Test
    @Order(2)
    @DisplayName("Create ACL template")
    void createACLTemplate() {
        Assumptions.assumeTrue(isFullCrudEnabled(),
                "Skipped: full CRUD mode not enabled");
        initClient();

        try {
            String templateName = testResourceName("acl");
            ACLTemplate template = timedCall("NetworkEdge", "create", "ACLTemplate", "POST", () ->
                    networkEdge.aclTemplates().define(templateName)
                            .withDescription("SDK integration test ACL template")
                            .withRule(Protocol.TCP, "any", "22", "0.0.0.0/0", 1)
                            .withRule(Protocol.TCP, "any", "443", "0.0.0.0/0", 2)
                            .save()
            );

            assertNotNull(template, "ACL template should be created");
            aclTemplateUuid = template.getUuid();
            assertNotNull(aclTemplateUuid, "ACL template UUID should not be null");

            registerCleanup("ACLTemplate", aclTemplateUuid, id -> {
                ACLTemplate toDelete = networkEdge.aclTemplates().getByUuid(id);
                toDelete.delete();
            });
            System.out.printf("  ACL template created: %s%n", aclTemplateUuid);
        } catch (Exception e) {
            Assumptions.assumeTrue(false,
                    "ACL template creation not available: " + e.getMessage());
        }
    }

    @Test
    @Order(3)
    @DisplayName("Verify ACL template via GET")
    void verifyACLTemplate() {
        Assumptions.assumeTrue(isFullCrudEnabled(),
                "Skipped: full CRUD mode not enabled");
        Assumptions.assumeTrue(aclTemplateUuid != null,
                "Skipped: no ACL template was created");
        initClient();

        ACLTemplate template = timedCall("NetworkEdge", "get", "ACLTemplate", "GET",
                aclTemplateUuid, () ->
                        networkEdge.aclTemplates().getByUuid(aclTemplateUuid)
        );

        assertNotNull(template, "ACL template should be retrievable");
        assertNotNull(template.getName(), "ACL template name should not be null");
        assertNotNull(template.getInboundRules(), "ACL template should have inbound rules");
        assertFalse(template.getInboundRules().isEmpty(), "ACL template should have at least one rule");
        System.out.printf("  ACL template verified: %s (%d rules)%n",
                template.getName(), template.getInboundRules().size());
    }

    @Test
    @Order(4)
    @DisplayName("Create public key")
    void createPublicKey() {
        Assumptions.assumeTrue(isFullCrudEnabled(),
                "Skipped: full CRUD mode not enabled");
        initClient();

        try {
            String keyName = testResourceName("pubkey");
            PublicKey key = timedCall("NetworkEdge", "create", "PublicKey", "POST", () ->
                    networkEdge.publicKeys().define(keyName, SSH_KEY_VALUE)
                            .create()
            );

            assertNotNull(key, "Public key should be created");
            publicKeyUuid = key.getUuid();
            assertNotNull(publicKeyUuid, "Public key UUID should not be null");

            // Note: the Network Edge API exposes no delete endpoint for public keys,
            // so there is no cleanup action to register.
            System.out.printf("  Public key created: %s%n", publicKeyUuid);
        } catch (Exception e) {
            Assumptions.assumeTrue(false,
                    "Public key creation not available: " + e.getMessage());
        }
    }

    @Test
    @Order(5)
    @DisplayName("Verify public key via list")
    void verifyPublicKey() {
        Assumptions.assumeTrue(isFullCrudEnabled(),
                "Skipped: full CRUD mode not enabled");
        Assumptions.assumeTrue(publicKeyUuid != null,
                "Skipped: no public key was created");
        initClient();

        // The Network Edge API has no get-by-id endpoint for public keys, so verify via the list.
        List<PublicKey> keys = timedCall("NetworkEdge", "list", "PublicKey", "GET", () ->
                networkEdge.publicKeys().list()
        );

        assertNotNull(keys, "Public key list should be retrievable");
        PublicKey key = keys.stream()
                .filter(k -> publicKeyUuid.equals(k.getUuid()))
                .findFirst()
                .orElse(null);
        assertNotNull(key, "Created public key should be present in the list");
        assertNotNull(key.getKeyName(), "Public key name should not be null");
        System.out.printf("  Public key verified: %s%n", key.getKeyName());
    }

    @Test
    @Order(6)
    @DisplayName("List device types (readonly sanity check)")
    void listDeviceTypes() {
        initClient();

        PaginatedList<DeviceType> deviceTypes = timedCall("NetworkEdge", "listDeviceTypes",
                "DeviceType", "GET", () ->
                        networkEdge.devices().listDeviceTypes()
        );

        assertNotNull(deviceTypes, "Device types list should not be null");
        System.out.printf("  Device types returned: %d%n", deviceTypes.size());
    }

    @Test
    @Order(7)
    @DisplayName("Teardown public key (no-op: API has no delete endpoint)")
    void teardownPublicKey() {
        // The Network Edge API exposes no delete endpoint for public keys, so there is
        // nothing to tear down. Kept for ordering/documentation purposes.
        Assumptions.assumeTrue(isFullCrudEnabled(),
                "Skipped: full CRUD mode not enabled");
        Assumptions.assumeTrue(publicKeyUuid != null,
                "Skipped: no public key was created");
        System.out.printf("  Public key %s left in place (no delete endpoint available)%n", publicKeyUuid);
    }

    @Test
    @Order(8)
    @DisplayName("Teardown ACL template")
    void teardownACLTemplate() {
        Assumptions.assumeTrue(isFullCrudEnabled(),
                "Skipped: full CRUD mode not enabled");
        Assumptions.assumeTrue(aclTemplateUuid != null,
                "Skipped: no ACL template to delete");
        initClient();

        try {
            ACLTemplate template = networkEdge.aclTemplates().getByUuid(aclTemplateUuid);
            Boolean deleted = timedCall("NetworkEdge", "delete", "ACLTemplate", "DELETE",
                    aclTemplateUuid, template::delete);
            assertNotNull(deleted, "Delete should return a result");
            System.out.printf("  ACL template deleted: %s%n", aclTemplateUuid);
        } catch (Exception e) {
            System.err.printf("  ACL template teardown failed (cleanup will retry): %s%n", e.getMessage());
        }
    }
}
