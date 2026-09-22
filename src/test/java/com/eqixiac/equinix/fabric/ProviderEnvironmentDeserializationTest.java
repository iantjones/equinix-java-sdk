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

package com.eqixiac.equinix.fabric;

import com.eqixiac.equinix.core.TestFixtures;
import com.eqixiac.equinix.core.internal.Constants;
import com.eqixiac.equinix.fabric.enums.AccessPointType;
import com.eqixiac.equinix.fabric.enums.CloudRouterType;
import com.eqixiac.equinix.fabric.enums.ConnectionType;
import com.eqixiac.equinix.fabric.enums.EnvironmentActionState;
import com.eqixiac.equinix.fabric.enums.EnvironmentActionType;
import com.eqixiac.equinix.fabric.enums.ProviderEnvironmentType;
import com.eqixiac.equinix.fabric.enums.ServiceProfileType;
import com.eqixiac.equinix.fabric.model.implementation.AccessPoint;
import com.eqixiac.equinix.fabric.model.implementation.ActivationKeyDetails;
import com.eqixiac.equinix.fabric.model.implementation.EnvironmentActionRequest;
import com.eqixiac.equinix.fabric.model.implementation.ProviderEnvironment;
import com.eqixiac.equinix.fabric.model.json.CloudRouterJson;
import com.eqixiac.equinix.fabric.model.json.EnvironmentActionResponseJson;
import com.eqixiac.equinix.fabric.model.json.ServiceProfileJson;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Wire-model tests for the Beta Fabric v4 {@code IC_PROFILE} surfaces (catalog fetched
 * 2026-09-21): {@code ProviderEnvironment}, {@code EnvironmentActionRequest},
 * {@code ActivationKeyDetails}, {@code EnvironmentActionResponse}, and the enum values
 * {@code XF_IC}, {@code IC_ROUTER}, {@code GW_VC}, {@code IPX_VC}.
 *
 * <p>The catalog publishes one {@code ProviderEnvironment} example
 * ({@code ServiceProfileEnvironmentsResponse}) and no {@code IC_PROFILE} service profile example.
 * Tests that need a profile or access point wrapper embed an environment object copied from that
 * example inside a minimal wrapper, and say so.</p>
 */
class ProviderEnvironmentDeserializationTest {

    private static final ObjectMapper MAPPER = Constants.mapper();

    /** The {@code data[index]} item of the catalog's {@code ServiceProfileEnvironmentsResponse} example. */
    private static JsonNode catalogEnvironment(int index) throws Exception {
        return MAPPER.readTree(TestFixtures.load("/json/fabric/service_profile_environments_response.json"))
                .path("data").path(index);
    }

    @Nested
    @DisplayName("ProviderEnvironment")
    class Environment {

        @Test
        @DisplayName("maps every property of the catalog example")
        void mapsCatalogExample() throws Exception {
            ProviderEnvironment env = MAPPER.treeToValue(catalogEnvironment(0), ProviderEnvironment.class);

            assertEquals("6ad498b5-d929-44ac-a199-ce9f0d31d9ac", env.getUuid());
            assertEquals(ProviderEnvironmentType.IC_ENV, env.getType());
            // The catalog example separates the words with U+2013 (en dash), not a hyphen.
            assertEquals("US West 1 " + (char) 0x2013 + " Production", env.getName());
            assertEquals("Primary production environment", env.getDescription());
            assertEquals("us-west-1", env.getRegion());
            assertEquals(List.of(1000, 10000, 100000), env.getSupportedBandwidths());
            assertEquals(List.of("CH", "DC"),
                    env.getMetros().stream().map(m -> m.metroId().code()).toList());
        }

        @Test
        @DisplayName("maps supportedFeatures and changeLog when present (schema example FEATURE_TYPE_L3_BASE)")
        void mapsSchemaOnlyProperties() throws Exception {
            // supportedFeatures: schema-level example. changeLog: values from the catalog's
            // service profile action example. The environments example omits both.
            ProviderEnvironment env = MAPPER.readValue("""
                    {
                      "uuid": "6ad498b5-d929-44ac-a199-ce9f0d31d9ac",
                      "type": "IC_ENV",
                      "supportedFeatures": ["FEATURE_TYPE_L3_BASE"],
                      "changeLog": {"createdBy": "adminuser", "createdDateTime": "2026-03-04T10:30:00Z"}
                    }""", ProviderEnvironment.class);

            assertEquals(List.of("FEATURE_TYPE_L3_BASE"), env.getSupportedFeatures());
            assertEquals("adminuser", env.getChangeLog().getCreatedBy());
            assertNotNull(env.getChangeLog().getCreatedDateTime());
        }

        @Test
        @DisplayName("an environment type this SDK does not define reads as UNKNOWN")
        void unknownTypeFallsBack() throws Exception {
            ProviderEnvironment env = MAPPER.readValue("{\"type\":\"IC_ENV_V2\"}", ProviderEnvironment.class);

            assertEquals(ProviderEnvironmentType.UNKNOWN, env.getType());
        }

        @Test
        @DisplayName("ServiceProfile.environments carries the extended model (minimal IC_PROFILE wrapper)")
        void embeddedInServiceProfile() throws Exception {
            String body = "{\"uuid\":\"23ad37d0-6b1c-4d1f-a4eb-9f3d68fbe4fd\",\"type\":\"IC_PROFILE\","
                    + "\"environments\":[" + catalogEnvironment(0) + "," + catalogEnvironment(1) + "]}";

            ServiceProfileJson profile = MAPPER.readValue(body, ServiceProfileJson.class);

            assertEquals(ServiceProfileType.IC_PROFILE, profile.getType());
            assertEquals(2, profile.getEnvironments().size());
            assertEquals(List.of(1000, 10000, 100000), profile.getEnvironments().get(0).getSupportedBandwidths());
            assertEquals("DC", profile.getEnvironments().get(1).getMetros().get(0).metroId().code());
        }

        @Test
        @DisplayName("AccessPoint reads environment, activationKey and authenticationKey as three separate properties")
        void embeddedInAccessPoint() throws Exception {
            String body = "{\"type\":\"SP\",\"authenticationKey\":\"123456789012\",\"activationKey\":\"key_here\","
                    + "\"environment\":" + catalogEnvironment(0) + "}";

            AccessPoint accessPoint = MAPPER.readValue(body, AccessPoint.class);

            assertEquals("123456789012", accessPoint.getAuthenticationKey());
            assertEquals("key_here", accessPoint.getActivationKey());
            assertEquals("us-west-1", accessPoint.getEnvironment().getRegion());
            assertEquals(List.of(1000, 10000, 100000), accessPoint.getEnvironment().getSupportedBandwidths());
        }
    }

    @Nested
    @DisplayName("EnvironmentActionRequest / ActivationKeyDetails")
    class ActionRequest {

        @Test
        @DisplayName("serializes to JSON equal to the catalog's ValidateActivationKey example")
        void serializesCatalogExample() throws Exception {
            EnvironmentActionRequest request =
                    EnvironmentActionRequest.validateActivationKey(ActivationKeyDetails.ofValue("key_here"));

            assertEquals(MAPPER.readTree("{\"type\":\"VALIDATE_ACTIVATION_KEY\",\"keyDetails\":{\"value\":\"key_here\"}}"),
                    MAPPER.readTree(MAPPER.writeValueAsString(request)));
        }

        @Test
        @DisplayName("builder-set properties serialize under their catalog names; bandwidth is an integer (Mbps)")
        void serializesAllProperties() throws Exception {
            ActivationKeyDetails details = ActivationKeyDetails.builder()
                    .value("key_here").providerId("p").accountId("a").bandwidth(10000).region("us-west-1").build();

            JsonNode json = MAPPER.readTree(MAPPER.writeValueAsString(details));

            assertEquals("key_here", json.path("value").asText());
            assertEquals("p", json.path("providerId").asText());
            assertEquals("a", json.path("accountId").asText());
            assertTrue(json.path("bandwidth").isInt());
            assertEquals(10000, json.path("bandwidth").asInt());
            assertEquals("us-west-1", json.path("region").asText());
            assertEquals(5, json.size());
        }

        @Test
        @DisplayName("ofValue rejects null and blank; the request rejects null parts and UNKNOWN")
        void rejectsInvalidInput() {
            assertThrows(IllegalArgumentException.class, () -> ActivationKeyDetails.ofValue(null));
            assertThrows(IllegalArgumentException.class, () -> ActivationKeyDetails.ofValue("  "));
            assertThrows(NullPointerException.class, () -> EnvironmentActionRequest.validateActivationKey(null));
            assertThrows(NullPointerException.class,
                    () -> new EnvironmentActionRequest(null, ActivationKeyDetails.ofValue("k")));
            assertThrows(IllegalArgumentException.class,
                    () -> new EnvironmentActionRequest(EnvironmentActionType.UNKNOWN, ActivationKeyDetails.ofValue("k")));
        }
    }

    @Nested
    @DisplayName("EnvironmentActionResponse")
    class ActionResponse {

        @Test
        @DisplayName("maps the schema-example fixture; isKeyUnused is not serialized as a property")
        void mapsFixture() throws Exception {
            EnvironmentActionResponseJson response = TestFixtures.deserialize(
                    "/json/fabric/service_profile_environment_action_response.json", EnvironmentActionResponseJson.class);

            assertEquals(EnvironmentActionType.VALIDATE_ACTIVATION_KEY, response.getType());
            assertEquals(EnvironmentActionState.ACTIVE, response.getState());
            assertFalse(response.isKeyUnused());
            assertEquals("key_here", response.getKeyDetails().getValue());

            JsonNode reserialized = MAPPER.readTree(MAPPER.writeValueAsString(response));
            assertFalse(reserialized.has("keyUnused"), "derived helper must stay out of the JSON form");
        }

        @Test
        @DisplayName("a response without a state is not reported as unused")
        void missingState() throws Exception {
            EnvironmentActionResponseJson response =
                    MAPPER.readValue("{\"type\":\"VALIDATE_ACTIVATION_KEY\"}", EnvironmentActionResponseJson.class);

            assertNull(response.getState());
            assertFalse(response.isKeyUnused());
        }
    }

    @Nested
    @DisplayName("Enum values added from the catalog")
    class EnumDrift {

        @Test
        @DisplayName("XF_IC, IC_ROUTER, GW_VC and IPX_VC parse to their constants, not UNKNOWN")
        void catalogValuesParse() {
            assertEquals(AccessPointType.XF_IC, AccessPointType.fromString("XF_IC"));
            assertEquals(CloudRouterType.IC_ROUTER, CloudRouterType.fromString("IC_ROUTER"));
            assertEquals(ConnectionType.GW_VC, ConnectionType.fromString("GW_VC"));
            assertEquals(ConnectionType.IPX_VC, ConnectionType.fromString("IPX_VC"));
        }

        @Test
        @DisplayName("the new constants serialize as their catalog literals")
        void catalogValuesSerialize() throws Exception {
            assertEquals("\"XF_IC\"", MAPPER.writeValueAsString(AccessPointType.XF_IC));
            assertEquals("\"IC_ROUTER\"", MAPPER.writeValueAsString(CloudRouterType.IC_ROUTER));
            assertEquals("\"GW_VC\"", MAPPER.writeValueAsString(ConnectionType.GW_VC));
            assertEquals("\"IPX_VC\"", MAPPER.writeValueAsString(ConnectionType.IPX_VC));
        }

        @Test
        @DisplayName("constants retained without a ConnectionType/AccessPointType catalog entry still parse")
        void retainedConstantsStillParse() {
            assertEquals(ConnectionType.IC_VC, ConnectionType.fromString("IC_VC"));
            assertEquals(ConnectionType.VD_CHAIN_VC, ConnectionType.fromString("VD_CHAIN_VC"));
            assertEquals(AccessPointType.CHAINGROUP, AccessPointType.fromString("CHAINGROUP"));
            assertEquals(AccessPointType.CX_PORT, AccessPointType.fromString("CX_PORT"));
        }

        @Test
        @DisplayName("environment action enums fall back to UNKNOWN")
        void actionEnumsFallBack() {
            assertEquals(EnvironmentActionType.VALIDATE_ACTIVATION_KEY,
                    EnvironmentActionType.fromString("VALIDATE_ACTIVATION_KEY"));
            assertEquals(EnvironmentActionType.UNKNOWN, EnvironmentActionType.fromString("SOMETHING_NEW"));
            assertEquals(EnvironmentActionState.ACTIVE, EnvironmentActionState.fromString("ACTIVE"));
            assertEquals(EnvironmentActionState.INACTIVE, EnvironmentActionState.fromString("INACTIVE"));
            assertEquals(EnvironmentActionState.UNKNOWN, EnvironmentActionState.fromString("EXPIRED"));
            assertEquals(EnvironmentActionState.UNKNOWN, EnvironmentActionState.fromString(null));
        }

        @Test
        @DisplayName("a Cloud Router read with type IC_ROUTER (CloudRouterReadResponse.type) maps to the constant")
        void icRouterReadModel() throws Exception {
            // href/type/uuid copied from the router object of the catalog's InterconnectResponse example.
            CloudRouterJson router = MAPPER.readValue("""
                    {"href": "https://api.equinix.com/fabric/v4/routers/24884a4e-bb47-403a-9426-a1b766305226",
                     "type": "IC_ROUTER",
                     "uuid": "24884a4e-bb47-403a-9426-a1b766305226"}""", CloudRouterJson.class);

            assertEquals(CloudRouterType.IC_ROUTER, router.getType());
            assertEquals("24884a4e-bb47-403a-9426-a1b766305226", router.getUuid());
        }

        @Test
        @DisplayName("an access point of type XF_IC maps to the constant")
        void xfIcAccessPoint() throws Exception {
            AccessPoint accessPoint = MAPPER.readValue("{\"type\":\"XF_IC\"}", AccessPoint.class);

            assertEquals(AccessPointType.XF_IC, accessPoint.getType());
        }
    }
}
