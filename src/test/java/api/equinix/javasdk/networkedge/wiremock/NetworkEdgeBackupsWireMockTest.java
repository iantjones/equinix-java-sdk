package api.equinix.javasdk.networkedge.wiremock;

import api.equinix.javasdk.NetworkEdge;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.networkedge.model.Backup;
import org.junit.jupiter.api.*;

import static api.equinix.javasdk.core.ResponseStubs.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for Network Edge device Backups.
 */
class NetworkEdgeBackupsWireMockTest extends WireMockTestBase {

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
        @DisplayName("returns backup for valid UUID")
        void returnsBackup() {
            stubSingleton(wireMock, "/ne/v1/deviceBackups/.*",
                    "/json/networkedge/backup_response.json");

            Backup backup = networkEdge.backups().getByUuid("bkp-1111-2222-3333-444455556666");
            assertNotNull(backup);
            assertEquals("bkp-1111-2222-3333-444455556666", backup.getUuid());
            assertEquals("test-backup", backup.getName());
        }

        @Test
        @DisplayName("404 throws EquinixNotFoundException")
        void notFound() {
            stubErrorInline(wireMock, "/ne/v1/deviceBackups/.*",
                    404, "[{\"errorCode\":\"ERR-404\",\"errorMessage\":\"Backup not found\"}]");

            assertThrows(EquinixNotFoundException.class,
                    () -> networkEdge.backups().getByUuid("invalid-uuid"));
        }
    }

    @Nested
    @DisplayName("Error handling")
    class Errors {

        @Test
        @DisplayName("500 throws EquinixServerException")
        void serverError() {
            stubErrorInline(wireMock, "/ne/v1/deviceBackups/.*",
                    500, "[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]");

            assertThrows(EquinixServerException.class,
                    () -> networkEdge.backups().getByUuid("test-uuid"));
        }
    }
}
