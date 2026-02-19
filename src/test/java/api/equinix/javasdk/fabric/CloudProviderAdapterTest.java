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

package api.equinix.javasdk.fabric;

import api.equinix.javasdk.fabric.enums.AccessPointType;
import api.equinix.javasdk.fabric.enums.ConnectionType;
import api.equinix.javasdk.fabric.enums.PeeringType;
import api.equinix.javasdk.fabric.model.implementation.LinkProtocol;
import api.equinix.javasdk.fabric.model.implementation.SimpleAccessPoint;
import api.equinix.javasdk.fabric.model.implementation.cloud.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Cloud Provider Connection Adapter framework.
 * Tests adapter construction, field extraction, and SimpleAccessPoint integration.
 */
class CloudProviderAdapterTest {

    private static final String TEST_PROFILE_UUID = "test-service-profile-uuid-1234";

    // ========================================================================
    // AWS Direct Connect Adapter Tests
    // ========================================================================

    @Nested
    @DisplayName("AWS Direct Connect Adapter")
    class AwsTests {

        @Test
        @DisplayName("should create adapter with AWS SDK object")
        void shouldCreateWithSource() {
            String mockAwsConnection = "aws-mock-connection-object";

            AwsDirectConnectAdapter<String> adapter = new AwsDirectConnectAdapter<>(
                    mockAwsConnection, "123456789012", "us-east-1", TEST_PROFILE_UUID);

            assertEquals("123456789012", adapter.getAuthenticationKey());
            assertEquals("us-east-1", adapter.getSellerRegion());
            assertEquals(TEST_PROFILE_UUID, adapter.getServiceProfileUuid());
            assertEquals(mockAwsConnection, adapter.getSource());
            assertEquals(CloudProviderType.AWS, adapter.getProviderType());
            assertEquals(ConnectionType.EVPL_VC, adapter.getPreferredConnectionType());
            assertNull(adapter.getPreferredLinkProtocol());
            assertNull(adapter.getPreferredPeeringType());
        }

        @Test
        @DisplayName("should create adapter with static factory method")
        void shouldCreateWithStaticFactory() {
            AwsDirectConnectAdapter<Void> adapter = AwsDirectConnectAdapter.of(
                    "123456789012", "eu-west-1", TEST_PROFILE_UUID);

            assertEquals("123456789012", adapter.getAuthenticationKey());
            assertEquals("eu-west-1", adapter.getSellerRegion());
            assertEquals(TEST_PROFILE_UUID, adapter.getServiceProfileUuid());
            assertNull(adapter.getSource());
        }

        @Test
        @DisplayName("should produce correct description")
        void shouldDescribe() {
            AwsDirectConnectAdapter<Void> adapter = AwsDirectConnectAdapter.of(
                    "123456789012", "us-west-2", TEST_PROFILE_UUID);

            assertEquals("AWS Direct Connect to us-west-2", adapter.describe());
        }
    }

    // ========================================================================
    // Azure ExpressRoute Adapter Tests
    // ========================================================================

    @Nested
    @DisplayName("Azure ExpressRoute Adapter")
    class AzureTests {

        @Test
        @DisplayName("should create adapter with Azure SDK object")
        void shouldCreateWithSource() {
            String mockCircuit = "azure-mock-circuit-object";

            AzureExpressRouteAdapter<String> adapter = new AzureExpressRouteAdapter<>(
                    mockCircuit, "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
                    "Silicon Valley", TEST_PROFILE_UUID, PeeringType.PRIVATE);

            assertEquals("xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx", adapter.getAuthenticationKey());
            assertEquals("Silicon Valley", adapter.getSellerRegion());
            assertEquals(TEST_PROFILE_UUID, adapter.getServiceProfileUuid());
            assertEquals(mockCircuit, adapter.getSource());
            assertEquals(CloudProviderType.AZURE, adapter.getProviderType());
            assertEquals(PeeringType.PRIVATE, adapter.getPreferredPeeringType());
        }

        @Test
        @DisplayName("should create adapter with static factory method")
        void shouldCreateWithStaticFactory() {
            AzureExpressRouteAdapter<Void> adapter = AzureExpressRouteAdapter.of(
                    "service-key-guid", "Washington DC", TEST_PROFILE_UUID, PeeringType.MICROSOFT);

            assertEquals("service-key-guid", adapter.getAuthenticationKey());
            assertEquals("Washington DC", adapter.getSellerRegion());
            assertEquals(PeeringType.MICROSOFT, adapter.getPreferredPeeringType());
            assertNull(adapter.getSource());
        }

        @Test
        @DisplayName("should produce correct description")
        void shouldDescribe() {
            AzureExpressRouteAdapter<Void> adapter = AzureExpressRouteAdapter.of(
                    "key", "Silicon Valley", TEST_PROFILE_UUID, PeeringType.PRIVATE);

            assertEquals("Azure ExpressRoute to Silicon Valley", adapter.describe());
        }
    }

    // ========================================================================
    // Google Cloud Interconnect Adapter Tests
    // ========================================================================

    @Nested
    @DisplayName("Google Cloud Interconnect Adapter")
    class GoogleTests {

        @Test
        @DisplayName("should create adapter with GCP SDK object")
        void shouldCreateWithSource() {
            String mockAttachment = "gcp-mock-attachment-object";

            GoogleCloudInterconnectAdapter<String> adapter = new GoogleCloudInterconnectAdapter<>(
                    mockAttachment, "xxxx/xxxx/xxxx/xxxx", "us-east1", TEST_PROFILE_UUID);

            assertEquals("xxxx/xxxx/xxxx/xxxx", adapter.getAuthenticationKey());
            assertEquals("us-east1", adapter.getSellerRegion());
            assertEquals(TEST_PROFILE_UUID, adapter.getServiceProfileUuid());
            assertEquals(mockAttachment, adapter.getSource());
            assertEquals(CloudProviderType.GOOGLE_CLOUD, adapter.getProviderType());
        }

        @Test
        @DisplayName("should create adapter with static factory method")
        void shouldCreateWithStaticFactory() {
            GoogleCloudInterconnectAdapter<Void> adapter = GoogleCloudInterconnectAdapter.of(
                    "pairing-key", "europe-west1", TEST_PROFILE_UUID);

            assertEquals("pairing-key", adapter.getAuthenticationKey());
            assertEquals("europe-west1", adapter.getSellerRegion());
            assertNull(adapter.getSource());
        }

        @Test
        @DisplayName("should produce correct description")
        void shouldDescribe() {
            GoogleCloudInterconnectAdapter<Void> adapter = GoogleCloudInterconnectAdapter.of(
                    "key", "us-east1", TEST_PROFILE_UUID);

            assertEquals("Google Cloud Interconnect to us-east1", adapter.describe());
        }
    }

    // ========================================================================
    // Oracle FastConnect Adapter Tests
    // ========================================================================

    @Nested
    @DisplayName("Oracle FastConnect Adapter")
    class OracleTests {

        @Test
        @DisplayName("should create adapter with OCI SDK object")
        void shouldCreateWithSource() {
            String mockVc = "oracle-mock-virtual-circuit";

            OracleFastConnectAdapter<String> adapter = new OracleFastConnectAdapter<>(
                    mockVc, "ocid1.virtualcircuit.oc1.iad.abcdef", "us-ashburn-1", TEST_PROFILE_UUID);

            assertEquals("ocid1.virtualcircuit.oc1.iad.abcdef", adapter.getAuthenticationKey());
            assertEquals("us-ashburn-1", adapter.getSellerRegion());
            assertEquals(TEST_PROFILE_UUID, adapter.getServiceProfileUuid());
            assertEquals(mockVc, adapter.getSource());
            assertEquals(CloudProviderType.ORACLE_CLOUD, adapter.getProviderType());
        }

        @Test
        @DisplayName("should create adapter with static factory method")
        void shouldCreateWithStaticFactory() {
            OracleFastConnectAdapter<Void> adapter = OracleFastConnectAdapter.of(
                    "ocid1.virtualcircuit.oc1.uk.xyz", "uk-london-1", TEST_PROFILE_UUID);

            assertEquals("ocid1.virtualcircuit.oc1.uk.xyz", adapter.getAuthenticationKey());
            assertEquals("uk-london-1", adapter.getSellerRegion());
            assertNull(adapter.getSource());
        }

        @Test
        @DisplayName("should produce correct description")
        void shouldDescribe() {
            OracleFastConnectAdapter<Void> adapter = OracleFastConnectAdapter.of(
                    "ocid", "us-ashburn-1", TEST_PROFILE_UUID);

            assertEquals("Oracle FastConnect to us-ashburn-1", adapter.describe());
        }
    }

    // ========================================================================
    // CloudProviderType Enum Tests
    // ========================================================================

    @Nested
    @DisplayName("CloudProviderType Enum")
    class ProviderTypeTests {

        @Test
        @DisplayName("should have correct display names")
        void shouldHaveDisplayNames() {
            assertEquals("AWS Direct Connect", CloudProviderType.AWS.getDisplayName());
            assertEquals("Azure ExpressRoute", CloudProviderType.AZURE.getDisplayName());
            assertEquals("Google Cloud Interconnect", CloudProviderType.GOOGLE_CLOUD.getDisplayName());
            assertEquals("Oracle FastConnect", CloudProviderType.ORACLE_CLOUD.getDisplayName());
            assertEquals("IBM Cloud Direct Link", CloudProviderType.IBM_CLOUD.getDisplayName());
            assertEquals("Alibaba Express Connect", CloudProviderType.ALIBABA_CLOUD.getDisplayName());
            assertEquals("Custom Provider", CloudProviderType.OTHER.getDisplayName());
        }

        @Test
        @DisplayName("should have correct provider names")
        void shouldHaveProviderNames() {
            assertEquals("Amazon Web Services", CloudProviderType.AWS.getProviderName());
            assertEquals("Microsoft Azure", CloudProviderType.AZURE.getProviderName());
            assertEquals("Google Cloud Platform", CloudProviderType.GOOGLE_CLOUD.getProviderName());
            assertEquals("Oracle Cloud Infrastructure", CloudProviderType.ORACLE_CLOUD.getProviderName());
        }

        @Test
        @DisplayName("should enumerate all 7 providers")
        void shouldHaveAllProviders() {
            assertEquals(7, CloudProviderType.values().length);
        }
    }

    // ========================================================================
    // SimpleAccessPoint Integration Tests
    // ========================================================================

    @Nested
    @DisplayName("SimpleAccessPoint Cloud Provider Integration")
    class AccessPointIntegrationTests {

        @Test
        @DisplayName("should build access point from AWS adapter")
        void shouldBuildFromAwsAdapter() {
            AwsDirectConnectAdapter<Void> adapter = AwsDirectConnectAdapter.of(
                    "123456789012", "us-east-1", TEST_PROFILE_UUID);

            LinkProtocol linkProtocol = LinkProtocol.dot1q().vlanTag(1000).create();

            SimpleAccessPoint ap = SimpleAccessPoint.define(AccessPointType.SP)
                    .fromCloudProvider(adapter)
                    .linkProtocol(linkProtocol)
                    .create();

            assertEquals(AccessPointType.SP, ap.getType());
            assertEquals("123456789012", ap.getAuthenticationKey());
            assertEquals("us-east-1", ap.getSellerRegion());
            assertNotNull(ap.getProfile());
            assertNotNull(ap.getLinkProtocol());
            assertNull(ap.getPeeringType());
        }

        @Test
        @DisplayName("should build access point from Azure adapter with peering type")
        void shouldBuildFromAzureAdapterWithPeering() {
            AzureExpressRouteAdapter<Void> adapter = AzureExpressRouteAdapter.of(
                    "service-key", "Silicon Valley", TEST_PROFILE_UUID, PeeringType.PRIVATE);

            LinkProtocol linkProtocol = LinkProtocol.dot1q().vlanTag(1500).create();

            SimpleAccessPoint ap = SimpleAccessPoint.define(AccessPointType.SP)
                    .fromCloudProvider(adapter)
                    .linkProtocol(linkProtocol)
                    .create();

            assertEquals(AccessPointType.SP, ap.getType());
            assertEquals("service-key", ap.getAuthenticationKey());
            assertEquals("Silicon Valley", ap.getSellerRegion());
            assertEquals(PeeringType.PRIVATE, ap.getPeeringType());
            assertNotNull(ap.getProfile());
        }

        @Test
        @DisplayName("should build access point from Google adapter")
        void shouldBuildFromGoogleAdapter() {
            GoogleCloudInterconnectAdapter<Void> adapter = GoogleCloudInterconnectAdapter.of(
                    "pairing-key-12345", "us-east1", TEST_PROFILE_UUID);

            SimpleAccessPoint ap = SimpleAccessPoint.define(AccessPointType.SP)
                    .fromCloudProvider(adapter)
                    .linkProtocol(LinkProtocol.dot1q().vlanTag(2000).create())
                    .create();

            assertEquals("pairing-key-12345", ap.getAuthenticationKey());
            assertEquals("us-east1", ap.getSellerRegion());
        }

        @Test
        @DisplayName("should build access point from Oracle adapter")
        void shouldBuildFromOracleAdapter() {
            OracleFastConnectAdapter<Void> adapter = OracleFastConnectAdapter.of(
                    "ocid1.vc.oc1.iad.abcdef", "us-ashburn-1", TEST_PROFILE_UUID);

            SimpleAccessPoint ap = SimpleAccessPoint.define(AccessPointType.SP)
                    .fromCloudProvider(adapter)
                    .linkProtocol(LinkProtocol.dot1q().vlanTag(3000).create())
                    .create();

            assertEquals("ocid1.vc.oc1.iad.abcdef", ap.getAuthenticationKey());
            assertEquals("us-ashburn-1", ap.getSellerRegion());
        }

        @Test
        @DisplayName("should allow manual seller region and authentication key on builder")
        void shouldAllowManualFields() {
            SimpleAccessPoint ap = SimpleAccessPoint.define(AccessPointType.SP)
                    .serviceProfile("my-profile-uuid")
                    .sellerRegion("us-west-2")
                    .authenticationKey("my-auth-key")
                    .peeringType(PeeringType.MICROSOFT)
                    .linkProtocol(LinkProtocol.dot1q().vlanTag(500).create())
                    .create();

            assertEquals(AccessPointType.SP, ap.getType());
            assertEquals("us-west-2", ap.getSellerRegion());
            assertEquals("my-auth-key", ap.getAuthenticationKey());
            assertEquals(PeeringType.MICROSOFT, ap.getPeeringType());
        }
    }

    // ========================================================================
    // Custom Adapter Implementation Test
    // ========================================================================

    @Nested
    @DisplayName("Custom Adapter Implementation")
    class CustomAdapterTests {

        @Test
        @DisplayName("should support custom adapter implementation")
        void shouldSupportCustomAdapter() {
            // Simulate a custom cloud provider
            CloudProviderConnectionAdapter<String> customAdapter = new CloudProviderConnectionAdapter<>() {
                @Override public String getServiceProfileUuid() { return TEST_PROFILE_UUID; }
                @Override public String getAuthenticationKey() { return "custom-auth-key"; }
                @Override public String getSellerRegion() { return "custom-region-1"; }
                @Override public String getSource() { return "custom-source"; }
                @Override public CloudProviderType getProviderType() { return CloudProviderType.OTHER; }
            };

            assertEquals(TEST_PROFILE_UUID, customAdapter.getServiceProfileUuid());
            assertEquals("custom-auth-key", customAdapter.getAuthenticationKey());
            assertEquals("custom-region-1", customAdapter.getSellerRegion());
            assertEquals("custom-source", customAdapter.getSource());
            assertEquals(CloudProviderType.OTHER, customAdapter.getProviderType());
            assertEquals(ConnectionType.EVPL_VC, customAdapter.getPreferredConnectionType());
            assertNull(customAdapter.getPreferredLinkProtocol());
            assertNull(customAdapter.getPreferredPeeringType());
            assertEquals("Custom Provider to custom-region-1", customAdapter.describe());
        }

        @Test
        @DisplayName("should integrate custom adapter with SimpleAccessPoint builder")
        void shouldIntegrateCustomWithAccessPoint() {
            CloudProviderConnectionAdapter<Void> customAdapter = new CloudProviderConnectionAdapter<>() {
                @Override public String getServiceProfileUuid() { return "custom-profile"; }
                @Override public String getAuthenticationKey() { return "custom-key"; }
                @Override public String getSellerRegion() { return "custom-region"; }
                @Override public Void getSource() { return null; }
                @Override public CloudProviderType getProviderType() { return CloudProviderType.OTHER; }
                @Override public PeeringType getPreferredPeeringType() { return PeeringType.PUBLIC; }
                @Override public LinkProtocol getPreferredLinkProtocol() {
                    return LinkProtocol.dot1q().vlanTag(999).create();
                }
            };

            SimpleAccessPoint ap = SimpleAccessPoint.define(AccessPointType.SP)
                    .fromCloudProvider(customAdapter)
                    .create();

            assertEquals("custom-key", ap.getAuthenticationKey());
            assertEquals("custom-region", ap.getSellerRegion());
            assertEquals(PeeringType.PUBLIC, ap.getPeeringType());
            assertNotNull(ap.getLinkProtocol());
        }
    }
}
