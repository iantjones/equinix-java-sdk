package com.eqixiac.equinix.scenarios;

import com.eqixiac.equinix.Equinix;
import com.eqixiac.equinix.EquinixConfig;
import com.eqixiac.equinix.Fabric;
import com.eqixiac.equinix.core.WireMockTestBase;
import com.eqixiac.equinix.fabric.enums.ConnectionType;
import com.eqixiac.equinix.fabric.enums.InterfaceType;
import com.eqixiac.equinix.fabric.model.Connection;
import com.eqixiac.equinix.fabric.model.ServiceToken;
import com.eqixiac.equinix.fabric.model.implementation.LinkProtocol;
import com.eqixiac.equinix.networkedge.model.Device;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.eqixiac.equinix.core.ResponseStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Offline (WireMock) cross-domain provisioning scenarios — the wiring a real user performs when
 * an identifier obtained from one resource/domain is redeemed against another:
 *
 * <ul>
 *   <li><b>NetworkEdge → Fabric</b>: read a virtual device from the NE API, then use its UUID as
 *       the A-side {@code VD} access point of a Fabric connection. The NE-sourced UUID must land
 *       verbatim in the Fabric {@code POST /connections} body.</li>
 *   <li><b>Fabric ServiceToken → Connection redemption</b>: fetch a service token, then redeem it
 *       as the Z-side of a new connection ({@code zSide.serviceToken.uuid}).</li>
 * </ul>
 *
 * <p>Unlike {@link CrossDomainBGPScenarioTest} (live, environment-dependent), these run entirely
 * against WireMock so the cross-domain seam is asserted at the wire level on every build.</p>
 */
@DisplayName("Cross-domain provisioning — identifiers minted in one API redeemed in another")
class CrossDomainProvisioningScenarioTest extends WireMockTestBase {

    private static final String NE_DEVICE_UUID = "ed7891f4-7a67-11e9-9bea-1681be663d3e";
    private static final String SERVICE_TOKEN_UUID = "ab7f685-41b0-1b07-6de0-3a7c54b08b8f";

    @BeforeEach
    void resetBeforeEach() {
        resetStubs();
    }

    private static EquinixConfig quietConfig() {
        // No eager metro load — keeps the wire down to exactly the calls under test.
        return EquinixConfig.builder().autoLoadMetros(false).build();
    }

    @Test
    @DisplayName("NE device UUID becomes the Fabric connection's A-side VD access point")
    void networkEdgeDeviceToFabricConnection() throws Exception {
        stubSingleton(wireMock, "/ne/v1/devices/" + NE_DEVICE_UUID,
                "/json/networkedge/device_response.json");
        stubCreate(wireMock, "/fabric/v4/connections",
                "/json/fabric/connection_response.json");

        try (Equinix eq = new Equinix(testCredentials(), quietConfig())) {
            redirectToWireMock(eq.fabric());
            eq.authenticate();

            // Step 1 — NetworkEdge: look up the virtual device.
            Device device = eq.networkEdge().devices().getByUuid(NE_DEVICE_UUID);
            assertNotNull(device);
            assertEquals(NE_DEVICE_UUID, device.getUuid(), "NE device UUID read from the NE API");

            // Step 2 — Fabric: provision a connection from that device (VD access point).
            Connection connection = eq.fabric().connections()
                    .define(ConnectionType.EVPL_VC)
                    .name("NE-Device-To-Cloud")
                    .bandwidth(100)
                    .aSideAccessPoint(device.getUuid(),
                            LinkProtocol.dot1q().vlanTag(1001).create(),
                            InterfaceType.CLOUD, 5)
                    .zSideAccessPointServiceProfile("20d32a80-0d61-4333-bc03-4b2d446794a0",
                            LinkProtocol.dot1q().vlanTag(1002).create())
                    .notification("ops@example.com")
                    .create();

            assertNotNull(connection);
            assertEquals("3a58dd05-f46d-4b1d-a154-2e85c396ea85", connection.getUuid());

            // The NE-sourced UUID landed verbatim in the Fabric POST body as a VD access point.
            wireMock.verify(getRequestedFor(urlPathEqualTo("/ne/v1/devices/" + NE_DEVICE_UUID)));
            wireMock.verify(postRequestedFor(urlPathEqualTo("/fabric/v4/connections"))
                    .withRequestBody(matchingJsonPath("$.type", equalTo("EVPL_VC")))
                    .withRequestBody(matchingJsonPath("$.aSide.accessPoint.type", equalTo("VD")))
                    .withRequestBody(matchingJsonPath("$.aSide.accessPoint.virtualDevice.uuid",
                            equalTo(NE_DEVICE_UUID)))
                    .withRequestBody(matchingJsonPath("$.aSide.accessPoint.interface.type",
                            equalTo("CLOUD")))
                    .withRequestBody(matchingJsonPath("$.aSide.accessPoint.interface.id",
                            equalTo("5")))
                    .withRequestBody(matchingJsonPath("$.aSide.accessPoint.linkProtocol.vlanTag",
                            equalTo("1001")))
                    .withRequestBody(matchingJsonPath("$.zSide.accessPoint.profile.uuid",
                            equalTo("20d32a80-0d61-4333-bc03-4b2d446794a0"))));

            // Both domains rode one shared session: a single OAuth token fetch.
            wireMock.verify(1, postRequestedFor(urlPathEqualTo("/oauth2/v1/token")));
        }
    }

    @Test
    @DisplayName("a fetched ServiceToken is redeemed as the Z-side of a new connection")
    void serviceTokenRedeemedIntoConnection() throws Exception {
        stubSingleton(wireMock, "/fabric/v4/serviceTokens/" + SERVICE_TOKEN_UUID,
                "/json/fabric/service_token_response.json");
        stubCreate(wireMock, "/fabric/v4/connections",
                "/json/fabric/connection_response.json");

        try (Fabric fabric = new Fabric(testCredentials(), quietConfig())) {
            redirectToWireMock(fabric);
            fabric.authenticate();

            // Step 1 — fetch the token issued to us (e.g. by a partner).
            ServiceToken token = fabric.serviceTokens().getByUuid(SERVICE_TOKEN_UUID);
            assertNotNull(token);
            assertEquals(SERVICE_TOKEN_UUID, token.getUuid());

            // Step 2 — redeem it: the token object itself is the Z-side of the new connection.
            Connection connection = fabric.connections()
                    .define(ConnectionType.EVPL_VC)
                    .name("Token-Redemption")
                    .bandwidth(100)
                    .aSideAccessPointPort("c791f8cb-5cc9-cc90-8ce0-306a5c00a4ee",
                            LinkProtocol.dot1q().vlanTag(1001).create())
                    .zSideServiceToken(token)
                    .notification("ops@example.com")
                    .create();

            assertNotNull(connection);

            wireMock.verify(getRequestedFor(
                    urlPathEqualTo("/fabric/v4/serviceTokens/" + SERVICE_TOKEN_UUID)));
            wireMock.verify(postRequestedFor(urlPathEqualTo("/fabric/v4/connections"))
                    .withRequestBody(matchingJsonPath("$.zSide.serviceToken.uuid",
                            equalTo(SERVICE_TOKEN_UUID)))
                    .withRequestBody(matchingJsonPath("$.aSide.accessPoint.port.uuid",
                            equalTo("c791f8cb-5cc9-cc90-8ce0-306a5c00a4ee"))));
        }
    }
}
