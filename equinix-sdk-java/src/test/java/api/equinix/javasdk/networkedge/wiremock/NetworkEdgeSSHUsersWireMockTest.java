package api.equinix.javasdk.networkedge.wiremock;

import api.equinix.javasdk.NetworkEdge;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.networkedge.model.SSHUser;
import org.junit.jupiter.api.*;

import static api.equinix.javasdk.core.ResponseStubs.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for Network Edge SSH Users.
 */
class NetworkEdgeSSHUsersWireMockTest extends WireMockTestBase {

    static NetworkEdge networkEdge;

    @BeforeAll
    static void setUp() {
        networkEdge = new NetworkEdge(testCredentials());
        redirectToWireMock(networkEdge);
        networkEdge.authenticate();
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (networkEdge != null) networkEdge.close();
    }

    @BeforeEach
    void resetBeforeEach() {
        resetStubs();
    }

    @Nested
    @DisplayName("getByUuid()")
    class GetByUuid {

        @Test
        @DisplayName("returns SSH user for valid UUID")
        void returnsSSHUser() {
            stubSingleton(wireMock, "/ne/v1/sshUsers/.*",
                    "/json/networkedge/sshuser_response.json");

            SSHUser sshUser = networkEdge.sshUsers().getByUuid("c3d4e5f6-a7b8-9012-cdef-34567890abcd");
            assertNotNull(sshUser);
            assertEquals("c3d4e5f6-a7b8-9012-cdef-34567890abcd", sshUser.getUuid());
            assertEquals("test-ssh-user", sshUser.getUsername());
        }

        @Test
        @DisplayName("404 throws EquinixNotFoundException")
        void notFound() {
            stubErrorInline(wireMock, "/ne/v1/sshUsers/.*",
                    404, "[{\"errorCode\":\"ERR-404\",\"errorMessage\":\"SSH user not found\"}]");

            assertThrows(EquinixNotFoundException.class,
                    () -> networkEdge.sshUsers().getByUuid("invalid-uuid"));
        }
    }

    @Nested
    @DisplayName("Error handling")
    class Errors {

        @Test
        @DisplayName("500 throws EquinixServerException")
        void serverError() {
            stubErrorInline(wireMock, "/ne/v1/sshUsers/.*",
                    500, "[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]");

            assertThrows(EquinixServerException.class,
                    () -> networkEdge.sshUsers().getByUuid("test-uuid"));
        }
    }
}
