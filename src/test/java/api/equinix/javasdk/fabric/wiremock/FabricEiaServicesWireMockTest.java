package api.equinix.javasdk.fabric.wiremock;

import api.equinix.javasdk.Fabric;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.fabric.enums.EiaBillingType;
import api.equinix.javasdk.fabric.enums.EiaRoutingProtocolType;
import api.equinix.javasdk.fabric.enums.EiaServiceType;
import api.equinix.javasdk.fabric.model.EiaService;
import api.equinix.javasdk.fabric.model.Project;
import api.equinix.javasdk.fabric.model.json.creators.EiaRoutingProtocolRequest;
import org.junit.jupiter.api.*;

import static api.equinix.javasdk.core.ResponseStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for Fabric Equinix Internet Access (EIA) services.
 * Covers define()/create() request-body serialization against the
 * {@code /fabric/v4/internetAccessServices} collection endpoint.
 */
class FabricEiaServicesWireMockTest extends WireMockTestBase {

    static Fabric fabric;

    @BeforeAll
    static void setUp() {
        fabric = new Fabric(testCredentials());
        redirectToWireMock(fabric);
        fabric.authenticate();
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (fabric != null) fabric.close();
    }

    @BeforeEach
    void resetBeforeEach() {
        resetStubs();
    }

    @Nested
    @DisplayName("define() / create()")
    class Create {

        @Test
        @DisplayName("POSTs a full EIA service body to the collection and returns the created service")
        void createsEiaService() {
            stubCreate(wireMock, "/fabric/v4/internetAccessServices", "/json/fabric/eia_service_response.json");

            EiaService service = fabric.eiaServices().define()
                    .ofType(EiaServiceType.SINGLE_IA)
                    .name("My-EIA-Service")
                    .bandwidth(1000)
                    .bandwidthCommit(500)
                    .withRoutingProtocol(new EiaRoutingProtocolRequest(EiaRoutingProtocolType.DIRECT))
                    .withProject(new Project("proj-abc-123"))
                    .withAccountNumber("123456")
                    .withBillingType(EiaBillingType.FIXED)
                    .purchaseOrderNumber("PO-98765")
                    .create();

            assertNotNull(service);
            assertEquals("f1e2d3c4-b5a6-7890-abcd-ef0123456789", service.getUuid());
            assertEquals("My-EIA-Service", service.getName());
            assertEquals(EiaServiceType.SINGLE_IA, service.getType());

            wireMock.verify(postRequestedFor(urlPathEqualTo("/fabric/v4/internetAccessServices"))
                    .withHeader("Content-Type", containing("application/json"))
                    .withRequestBody(matchingJsonPath("$.type", equalTo("SINGLE_IA")))
                    .withRequestBody(matchingJsonPath("$.name", equalTo("My-EIA-Service")))
                    .withRequestBody(matchingJsonPath("$.bandwidth", equalTo("1000")))
                    .withRequestBody(matchingJsonPath("$.bandwidthCommit", equalTo("500")))
                    .withRequestBody(matchingJsonPath("$.routingProtocol.type", equalTo("DIRECT")))
                    .withRequestBody(matchingJsonPath("$.project.projectId", equalTo("proj-abc-123")))
                    .withRequestBody(matchingJsonPath("$.account.accountNumber", equalTo("123456")))
                    .withRequestBody(matchingJsonPath("$.billing.type", equalTo("FIXED")))
                    .withRequestBody(matchingJsonPath("$.order.purchaseOrderNumber", equalTo("PO-98765"))));
        }

        @Test
        @DisplayName("omits null optional fields (account/billing/order) from the create body")
        void createsMinimalEiaService() {
            stubCreate(wireMock, "/fabric/v4/internetAccessServices", "/json/fabric/eia_service_response.json");

            EiaService service = fabric.eiaServices().define()
                    .ofType(EiaServiceType.DUAL_IA)
                    .name("Minimal-EIA")
                    .bandwidth(200)
                    .withRoutingProtocol(new EiaRoutingProtocolRequest(EiaRoutingProtocolType.BGP))
                    .withProject(new Project("proj-min-1"))
                    .create();

            assertNotNull(service);

            wireMock.verify(postRequestedFor(urlPathEqualTo("/fabric/v4/internetAccessServices"))
                    .withRequestBody(matchingJsonPath("$.type", equalTo("DUAL_IA")))
                    .withRequestBody(matchingJsonPath("$.name", equalTo("Minimal-EIA")))
                    .withRequestBody(matchingJsonPath("$.routingProtocol.type", equalTo("BGP")))
                    .withRequestBody(matchingJsonPath("$.project.projectId", equalTo("proj-min-1")))
                    .withRequestBody(notMatching("(?s).*\"account\".*"))
                    .withRequestBody(notMatching("(?s).*\"billing\".*"))
                    .withRequestBody(notMatching("(?s).*\"order\".*")));
        }
    }

    @Nested
    @DisplayName("create() error handling")
    class Errors {

        @Test
        @DisplayName("400 throws EquinixServiceException")
        void badRequest() {
            stubErrorInline(wireMock, "/fabric/v4/internetAccessServices",
                    400, "[{\"errorCode\":\"ERR-400\",\"errorMessage\":\"Invalid EIA service request\"}]");

            assertThrows(EquinixServiceException.class,
                    () -> fabric.eiaServices().define()
                            .ofType(EiaServiceType.SINGLE_IA)
                            .name("Bad-EIA")
                            .withRoutingProtocol(new EiaRoutingProtocolRequest(EiaRoutingProtocolType.DIRECT))
                            .withProject(new Project("proj-bad"))
                            .create());
        }

        @Test
        @DisplayName("401 throws EquinixAuthenticationException")
        void unauthorized() {
            stubErrorInline(wireMock, "/fabric/v4/internetAccessServices",
                    401, "[{\"errorCode\":\"ERR-401\",\"errorMessage\":\"Unauthorized\"}]");

            assertThrows(EquinixAuthenticationException.class,
                    () -> fabric.eiaServices().define()
                            .ofType(EiaServiceType.SINGLE_IA)
                            .name("Unauth-EIA")
                            .withRoutingProtocol(new EiaRoutingProtocolRequest(EiaRoutingProtocolType.DIRECT))
                            .withProject(new Project("proj-unauth"))
                            .create());
        }
    }
}
