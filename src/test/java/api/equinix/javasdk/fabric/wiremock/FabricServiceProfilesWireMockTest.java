package api.equinix.javasdk.fabric.wiremock;

import api.equinix.javasdk.Fabric;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.fabric.model.ServiceProfile;
import org.junit.jupiter.api.*;

import static api.equinix.javasdk.core.ResponseStubs.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for Fabric Service Profiles.
 */
class FabricServiceProfilesWireMockTest extends WireMockTestBase {

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
    @DisplayName("getByUuid()")
    class GetByUuid {

        @Test
        @DisplayName("returns service profile for valid UUID")
        void returnsServiceProfile() {
            stubSingleton(wireMock, "/fabric/v4/serviceProfiles/.*",
                    "/json/fabric/service_profile_response.json");

            ServiceProfile profile = fabric.serviceProfiles().getByUuid("f6a7b8c9-d0e1-2345-fabc-567890123def");
            assertNotNull(profile);
            assertEquals("f6a7b8c9-d0e1-2345-fabc-567890123def", profile.getUuid());
            assertEquals("AWS Direct Connect - Production", profile.getName());
        }

        @Test
        @DisplayName("404 throws EquinixNotFoundException")
        void notFound() {
            stubErrorInline(wireMock, "/fabric/v4/serviceProfiles/.*",
                    404, "[{\"errorCode\":\"ERR-404\",\"errorMessage\":\"Service profile not found\"}]");

            assertThrows(EquinixNotFoundException.class,
                    () -> fabric.serviceProfiles().getByUuid("invalid-uuid"));
        }
    }

    @Nested
    @DisplayName("Error handling")
    class Errors {

        @Test
        @DisplayName("500 throws EquinixServerException")
        void serverError() {
            stubErrorInline(wireMock, "/fabric/v4/serviceProfiles/.*",
                    500, "[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]");

            assertThrows(EquinixServerException.class,
                    () -> fabric.serviceProfiles().getByUuid("test-uuid"));
        }
    }
}
