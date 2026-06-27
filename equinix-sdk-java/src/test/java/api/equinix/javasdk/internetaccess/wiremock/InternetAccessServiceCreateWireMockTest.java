package api.equinix.javasdk.internetaccess.wiremock;

import api.equinix.javasdk.InternetAccess;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.internetaccess.enums.CustomerAsnRange;
import api.equinix.javasdk.internetaccess.enums.ExportPolicy;
import api.equinix.javasdk.internetaccess.enums.ServiceState;
import api.equinix.javasdk.internetaccess.enums.ServiceTypeV2;
import api.equinix.javasdk.internetaccess.model.InternetAccessService;
import api.equinix.javasdk.internetaccess.model.json.creators.BgpRoutingProtocolRequest;
import api.equinix.javasdk.internetaccess.model.json.creators.CustomerRouteIpv4Request;
import api.equinix.javasdk.internetaccess.model.json.creators.IpBlockIpv4Request;
import api.equinix.javasdk.internetaccess.model.json.creators.RoutingProtocolIpv4Request;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * WireMock-backed test for the single Equinix Internet Access (EIA) v2 operation:
 * {@code POST /internetAccess/v2/services} with a nested BGP routing protocol body. Verifies the
 * routing-protocol discriminator and nested ipBlock are serialized correctly, and that the
 * read-only {@code ServiceV2} response deserializes back into {@link InternetAccessService}.
 */
class InternetAccessServiceCreateWireMockTest extends WireMockTestBase {

    private static final String SERVICE_PATH = "/internetAccess/v2/services";
    private static final String CONNECTION_UUID = "9b8c5042-b553-4d5e-a2ac-c73bf6d4fd81";

    static InternetAccess internetAccess;

    @BeforeAll
    static void setUp() {
        internetAccess = new InternetAccess(testCredentials());
        redirectToWireMock(internetAccess);
        internetAccess.authenticate();
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (internetAccess != null) internetAccess.close();
    }

    @BeforeEach
    void resetBeforeEach() {
        resetStubs();
    }

    @Test
    void create_nestedBgpService_postsExpectedBodyAndDeserializesResponse() {
        String responseBody = "{\n" +
                "  \"uuid\": \"7a4f6c1e-2b3d-4e5f-8a9b-0c1d2e3f4a5b\",\n" +
                "  \"type\": \"SINGLE\",\n" +
                "  \"bandwidth\": 1000,\n" +
                "  \"state\": \"PROVISIONING\"\n" +
                "}";

        wireMock.stubFor(post(urlEqualTo(SERVICE_PATH))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody)));

        BgpRoutingProtocolRequest routingProtocol = BgpRoutingProtocolRequest.builder()
                .name("WebServers Routes")
                .customerAsnRange(CustomerAsnRange.BITS_32)
                .customerAsn(16220L)
                .bgpAuthKey("SecretKey")
                .exportPolicy(ExportPolicy.FULL)
                .ipv4(RoutingProtocolIpv4Request.builder()
                        .customerRoute(CustomerRouteIpv4Request.builder()
                                .prefix("198.51.100.0/24")
                                .ipBlock(IpBlockIpv4Request.builder()
                                        .prefixLength(24)
                                        .build())
                                .build())
                        .build())
                .build();

        InternetAccessService service = internetAccess.services().define()
                .name("WebServers")
                .description("Customer facing web servers")
                .type(ServiceTypeV2.SINGLE)
                .connection(CONNECTION_UUID)
                .routingProtocol(routingProtocol)
                .create();

        assertNotNull(service);
        assertEquals("7a4f6c1e-2b3d-4e5f-8a9b-0c1d2e3f4a5b", service.getUuid());
        assertEquals(ServiceTypeV2.SINGLE, service.getType());
        assertEquals(Integer.valueOf(1000), service.getBandwidth());
        assertEquals(ServiceState.PROVISIONING, service.getState());

        String expectedBody = "{\n" +
                "  \"name\": \"WebServers\",\n" +
                "  \"description\": \"Customer facing web servers\",\n" +
                "  \"type\": \"SINGLE\",\n" +
                "  \"connections\": [\"" + CONNECTION_UUID + "\"],\n" +
                "  \"routingProtocol\": {\n" +
                "    \"type\": \"BGP\",\n" +
                "    \"name\": \"WebServers Routes\",\n" +
                "    \"customerAsnRange\": \"BITS_32\",\n" +
                "    \"customerAsn\": 16220,\n" +
                "    \"bgpAuthKey\": \"SecretKey\",\n" +
                "    \"exportPolicy\": \"FULL\",\n" +
                "    \"ipv4\": {\n" +
                "      \"customerRoutes\": [\n" +
                "        { \"ipBlock\": { \"prefixLength\": 24 }, \"prefix\": \"198.51.100.0/24\" }\n" +
                "      ]\n" +
                "    }\n" +
                "  }\n" +
                "}";

        wireMock.verify(postRequestedFor(urlEqualTo(SERVICE_PATH))
                .withHeader("Content-Type", equalTo("application/json"))
                .withRequestBody(equalToJson(expectedBody, true, true))
                // Explicitly assert the discriminator is nested under routingProtocol.
                .withRequestBody(matchingJsonPath("$.routingProtocol[?(@.type == 'BGP')]"))
                .withRequestBody(matchingJsonPath("$.routingProtocol.ipv4.customerRoutes[0].ipBlock.prefixLength")));
    }
}
