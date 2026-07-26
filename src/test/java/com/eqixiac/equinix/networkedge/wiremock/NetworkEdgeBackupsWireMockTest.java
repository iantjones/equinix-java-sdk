package com.eqixiac.equinix.networkedge.wiremock;

import com.eqixiac.equinix.NetworkEdge;
import com.eqixiac.equinix.core.WireMockTestBase;
import com.eqixiac.equinix.core.exception.*;
import com.eqixiac.equinix.core.http.response.PaginatedList;
import com.eqixiac.equinix.networkedge.client.RequestBuilder;
import com.eqixiac.equinix.networkedge.enums.BackupStatus;
import com.eqixiac.equinix.networkedge.model.Backup;
import org.junit.jupiter.api.*;

import java.time.LocalDateTime;

import static com.eqixiac.equinix.core.ResponseStubs.*;
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
            // Spec DeviceBackupInfoVerbose carries deviceUuid and lastUpdatedDateTime (not the
            // shared Lifecycle lastUpdatedDate name) — both must map.
            assertEquals("dev-9999-8888-7777-666655554444", backup.getDeviceUuid());
            assertEquals(LocalDateTime.of(2018, 1, 30, 10, 30, 31), backup.getLastUpdatedDate());
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
    @DisplayName("list(deviceUuid)")
    class ListAll {

        @Test
        @DisplayName("GETs /ne/v1/deviceBackups carrying only virtualDeviceUuid, and maps the paginated body")
        void listsAllBackups() {
            // ListBackups -> GET /ne/v1/deviceBackups (rootUri "deviceBackups", no requestUri override).
            stubPaginatedGet(wireMock, "/ne/v1/deviceBackups/?",
                    "/json/networkedge/backup_list_response.json");

            PaginatedList<Backup> backups =
                    networkEdge.backups().list("dev-9999-8888-7777-666655554444");

            assertNotNull(backups);
            assertEquals(2, backups.size());
            assertEquals("bkp-1111-2222-3333-444455556666", backups.get(0).getUuid());
            assertEquals("test-backup", backups.get(0).getName());
            assertEquals("dev-9999-8888-7777-666655554444", backups.get(0).getDeviceUuid());
            assertEquals("bkp-7777-8888-9999-aaaabbbbcccc", backups.get(1).getUuid());
            assertEquals("second-backup", backups.get(1).getName());

            // deviceUuid is always sent as virtualDeviceUuid; the unfiltered call carries no status filter.
            wireMock.verify(getRequestedFor(urlPathEqualTo("/ne/v1/deviceBackups"))
                    .withQueryParam("virtualDeviceUuid", equalTo("dev-9999-8888-7777-666655554444"))
                    .withQueryParam("status", absent()));
        }
    }

    @Nested
    @DisplayName("list(deviceUuid, RequestBuilder.Backup)")
    class ListFiltered {

        @Test
        @DisplayName("GETs /ne/v1/deviceBackups applying the builder's status filter alongside virtualDeviceUuid")
        void listsWithStatusFilter() {
            stubPaginatedGet(wireMock, "/ne/v1/deviceBackups/?",
                    "/json/networkedge/backup_list_response.json");

            PaginatedList<Backup> backups = networkEdge.backups().list(
                    "dev-9999-8888-7777-666655554444",
                    RequestBuilder.backup().withStatus(BackupStatus.COMPLETED));

            assertNotNull(backups);
            assertEquals(2, backups.size());

            wireMock.verify(getRequestedFor(urlPathEqualTo("/ne/v1/deviceBackups"))
                    .withQueryParam("virtualDeviceUuid", equalTo("dev-9999-8888-7777-666655554444"))
                    .withQueryParam("status", equalTo("COMPLETED")));
        }

        @Test
        @DisplayName("omits status when the builder sets no filter")
        void listsWithoutStatusFilter() {
            stubPaginatedGet(wireMock, "/ne/v1/deviceBackups/?",
                    "/json/networkedge/backup_list_response.json");

            PaginatedList<Backup> backups = networkEdge.backups().list(
                    "dev-9999-8888-7777-666655554444",
                    RequestBuilder.backup());

            assertNotNull(backups);
            assertEquals(2, backups.size());

            wireMock.verify(getRequestedFor(urlPathEqualTo("/ne/v1/deviceBackups"))
                    .withQueryParam("virtualDeviceUuid", equalTo("dev-9999-8888-7777-666655554444"))
                    .withQueryParam("status", absent()));
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

        @Test
        @DisplayName("restoreToDevice(deviceUuid) is a pure alias of restore(): same PATCH keyed by the backup UUID")
        void restoreToDeviceByUuidAliasesRestore() {
            stubSingleton(wireMock, "/ne/v1/deviceBackups/.*",
                    "/json/networkedge/backup_response.json");
            wireMock.stubFor(patch(urlPathEqualTo(
                    "/ne/v1/devices/bkp-1111-2222-3333-444455556666/restore"))
                    .willReturn(okJson("{}")));

            Backup backup = networkEdge.backups().getByUuid("bkp-1111-2222-3333-444455556666");
            // The supplied deviceUuid is documented as ignored: the endpoint is keyed by the
            // backup uuid, so the wire request is identical to restore().
            @SuppressWarnings("deprecation")
            Boolean result = backup.restoreToDevice("some-other-device-uuid");

            assertTrue(result);
            wireMock.verify(patchRequestedFor(urlPathEqualTo(
                    "/ne/v1/devices/bkp-1111-2222-3333-444455556666/restore"))
                    .withRequestBody(equalToJson("{\"name\":\"test-backup\"}", true, true)));
        }

        @Test
        @DisplayName("restoreToDevice(Device) is a pure alias of restore(): same PATCH keyed by the backup UUID")
        void restoreToDeviceByObjectAliasesRestore() {
            stubSingleton(wireMock, "/ne/v1/deviceBackups/.*",
                    "/json/networkedge/backup_response.json");
            // The Device argument is only used as a handle; fetch one to pass in.
            stubSingleton(wireMock, "/ne/v1/devices/ed7891f4-7a67-11e9-9bea-1681be663d3e",
                    "/json/networkedge/device_response.json");
            wireMock.stubFor(patch(urlPathEqualTo(
                    "/ne/v1/devices/bkp-1111-2222-3333-444455556666/restore"))
                    .willReturn(okJson("{}")));

            Backup backup = networkEdge.backups().getByUuid("bkp-1111-2222-3333-444455556666");
            com.eqixiac.equinix.networkedge.model.Device device =
                    networkEdge.devices().getByUuid("ed7891f4-7a67-11e9-9bea-1681be663d3e");

            @SuppressWarnings("deprecation")
            Boolean result = backup.restoreToDevice(device);

            assertTrue(result);
            // The restore PATCH is keyed by the BACKUP uuid — not the supplied device's uuid.
            wireMock.verify(patchRequestedFor(urlPathEqualTo(
                    "/ne/v1/devices/bkp-1111-2222-3333-444455556666/restore"))
                    .withRequestBody(equalToJson("{\"name\":\"test-backup\"}", true, true)));
        }
    }

    @Nested
    @DisplayName("refresh()")
    class Refresh {

        private static final String UUID = "bkp-1111-2222-3333-444455556666";
        private static final String PATH = "/ne/v1/deviceBackups/" + UUID;

        @Test
        @DisplayName("re-GETs the backup and updates the wrapper's state in place")
        void refreshesInPlace() {
            // First GET returns the original state; the second GET — triggered by
            // wrapper.refresh() — returns a DIFFERENT payload (renamed, status changed).
            wireMock.stubFor(get(urlPathEqualTo(PATH))
                    .inScenario("backup-refresh")
                    .whenScenarioStateIs(com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED)
                    .willReturn(okJson(loadFixture("/json/networkedge/backup_response.json")))
                    .willSetStateTo("state-changed"));
            wireMock.stubFor(get(urlPathEqualTo(PATH))
                    .inScenario("backup-refresh")
                    .whenScenarioStateIs("state-changed")
                    .willReturn(okJson(loadFixture("/json/networkedge/backup_response_refreshed.json"))));

            Backup backup = networkEdge.backups().getByUuid(UUID);
            assertEquals("test-backup", backup.getName());
            assertEquals(BackupStatus.COMPLETED, backup.getStatus());

            assertTrue(backup.refresh());

            // The same wrapper instance now reflects the re-fetched server state.
            assertEquals("renamed-backup", backup.getName());
            assertEquals(BackupStatus.IN_PROGRESS, backup.getStatus());
            assertEquals(UUID, backup.getUuid());

            wireMock.verify(2, getRequestedFor(urlPathEqualTo(PATH)));
        }
    }

    @Nested
    @DisplayName("Multi-page list paging")
    class Paging {

        private static final String DEVICE_UUID = "dev-9999-8888-7777-666655554444";

        private static final String PAGE_1 = """
                {
                  "pagination": { "offset": 0, "limit": 1, "total": 2 },
                  "data": [ {
                    "uuid": "bkp-1111-2222-3333-444455556666",
                    "name": "page1-backup",
                    "type": "CONFIG",
                    "status": "COMPLETED",
                    "deviceUuid": "dev-9999-8888-7777-666655554444",
                    "restores": []
                  } ]
                }
                """;

        private static final String PAGE_2 = """
                {
                  "pagination": { "offset": 1, "limit": 1, "total": 2 },
                  "data": [ {
                    "uuid": "bkp-7777-8888-9999-aaaabbbbcccc",
                    "name": "page2-backup",
                    "type": "CONFIG",
                    "status": "IN_PROGRESS",
                    "deviceUuid": "dev-9999-8888-7777-666655554444",
                    "restores": []
                  } ]
                }
                """;

        @Test
        @DisplayName("loadAll() fetches page 2 by advancing offset/limit and re-sends virtualDeviceUuid")
        void loadAllFetchesSecondPage() {
            wireMock.stubFor(get(urlPathEqualTo("/ne/v1/deviceBackups"))
                    .withQueryParam("offset", equalTo("0"))
                    .willReturn(okJson(PAGE_1)));
            wireMock.stubFor(get(urlPathEqualTo("/ne/v1/deviceBackups"))
                    .withQueryParam("offset", equalTo("1"))
                    .willReturn(okJson(PAGE_2)));

            PaginatedList<Backup> backups = networkEdge.backups().list(DEVICE_UUID);
            assertEquals(1, backups.size());
            assertTrue(backups.hasNextPage());

            backups.loadAll();

            assertEquals(2, backups.size());
            assertEquals("page1-backup", backups.get(0).getName());
            assertEquals("page2-backup", backups.get(1).getName());
            assertFalse(backups.hasNextPage());

            // Page 2 request: offset advanced from the server-reported pagination, limit carried,
            // and the SAME virtualDeviceUuid query param re-sent.
            wireMock.verify(1, getRequestedFor(urlPathEqualTo("/ne/v1/deviceBackups"))
                    .withQueryParam("offset", equalTo("1"))
                    .withQueryParam("limit", equalTo("1"))
                    .withQueryParam("virtualDeviceUuid", equalTo(DEVICE_UUID)));
        }
    }
}
