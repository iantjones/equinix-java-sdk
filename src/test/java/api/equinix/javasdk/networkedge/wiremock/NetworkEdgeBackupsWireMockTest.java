package api.equinix.javasdk.networkedge.wiremock;

import api.equinix.javasdk.NetworkEdge;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.networkedge.model.Backup;
import org.junit.jupiter.api.*;

import static api.equinix.javasdk.core.ResponseStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
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

    @Nested
    @DisplayName("define(...).save()")
    class Define {

        @Test
        @DisplayName("POSTs deviceUuid + name to CreateBackup, then refetches")
        void createsBackup() {
            // CreateBackup returns a UUID envelope; the SDK then refetches via GetBackup.
            wireMock.stubFor(post(urlPathEqualTo("/ne/v1/deviceBackups"))
                    .willReturn(okJson(loadFixture("/json/networkedge/backup_create_response.json"))));
            stubSingleton(wireMock, "/ne/v1/deviceBackups/.*",
                    "/json/networkedge/backup_response.json");

            Backup backup = networkEdge.backups()
                    .define("dev-9999-8888-7777-666655554444", "test-backup")
                    .save();

            assertNotNull(backup);
            assertEquals("bkp-1111-2222-3333-444455556666", backup.getUuid());

            wireMock.verify(postRequestedFor(urlPathEqualTo("/ne/v1/deviceBackups"))
                    .withRequestBody(equalToJson(
                            "{\"deviceUuid\":\"dev-9999-8888-7777-666655554444\",\"name\":\"test-backup\"}",
                            true, true)));
            wireMock.verify(getRequestedFor(
                    urlPathEqualTo("/ne/v1/deviceBackups/bkp-1111-2222-3333-444455556666")));
        }
    }

    @Nested
    @DisplayName("Backup.update(...).save()")
    class Update {

        @Test
        @DisplayName("PATCHes new name to UpdateBackup, then refetches")
        void updatesBackup() {
            stubSingleton(wireMock, "/ne/v1/deviceBackups/.*",
                    "/json/networkedge/backup_response.json");
            wireMock.stubFor(patch(urlPathEqualTo("/ne/v1/deviceBackups/bkp-1111-2222-3333-444455556666"))
                    .willReturn(okJson("{}")));

            Backup backup = networkEdge.backups().getByUuid("bkp-1111-2222-3333-444455556666");
            Backup updated = backup.update().withConfigName("renamed-backup").save();

            assertNotNull(updated);

            wireMock.verify(patchRequestedFor(
                    urlPathEqualTo("/ne/v1/deviceBackups/bkp-1111-2222-3333-444455556666"))
                    .withRequestBody(equalToJson("{\"name\":\"renamed-backup\"}", true, true)));
        }
    }

    @Nested
    @DisplayName("Backup.delete()")
    class Delete {

        @Test
        @DisplayName("DELETEs the backup by UUID")
        void deletesBackup() {
            stubSingleton(wireMock, "/ne/v1/deviceBackups/.*",
                    "/json/networkedge/backup_response.json");
            wireMock.stubFor(delete(urlPathEqualTo("/ne/v1/deviceBackups/bkp-1111-2222-3333-444455556666"))
                    .willReturn(noContent()));

            Backup backup = networkEdge.backups().getByUuid("bkp-1111-2222-3333-444455556666");
            assertTrue(backup.delete());

            wireMock.verify(deleteRequestedFor(
                    urlPathEqualTo("/ne/v1/deviceBackups/bkp-1111-2222-3333-444455556666")));
        }
    }

    @Nested
    @DisplayName("download(uuid)")
    class Download {

        @Test
        @DisplayName("GETs the backup contents from {uuid}/download")
        void downloadsBackup() {
            wireMock.stubFor(get(urlPathEqualTo(
                    "/ne/v1/deviceBackups/bkp-1111-2222-3333-444455556666/download"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "text/plain")
                            .withBody("! running configuration\nhostname test\n")));

            String contents = networkEdge.backups().download("bkp-1111-2222-3333-444455556666");

            assertNotNull(contents);
            assertTrue(contents.contains("hostname test"));

            wireMock.verify(getRequestedFor(urlPathEqualTo(
                    "/ne/v1/deviceBackups/bkp-1111-2222-3333-444455556666/download")));
        }
    }

    @Nested
    @DisplayName("Backup.restore()")
    class Restore {

        @Test
        @DisplayName("PATCHes name to devices/{uuid}/restore keyed by the backup UUID")
        void restoresBackup() {
            stubSingleton(wireMock, "/ne/v1/deviceBackups/.*",
                    "/json/networkedge/backup_response.json");
            // RestoreBackup overrides the root URI to devices/{backupUuid}/restore.
            wireMock.stubFor(patch(urlPathEqualTo(
                    "/ne/v1/devices/bkp-1111-2222-3333-444455556666/restore"))
                    .willReturn(okJson("{}")));

            Backup backup = networkEdge.backups().getByUuid("bkp-1111-2222-3333-444455556666");
            assertTrue(backup.restore());

            wireMock.verify(patchRequestedFor(urlPathEqualTo(
                    "/ne/v1/devices/bkp-1111-2222-3333-444455556666/restore"))
                    .withRequestBody(equalToJson("{\"name\":\"test-backup\"}", true, true)));
        }
    }
}
