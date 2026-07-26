package com.eqixiac.equinix.networkedge.wiremock;

import com.eqixiac.equinix.NetworkEdge;
import com.eqixiac.equinix.core.WireMockTestBase;
import com.eqixiac.equinix.core.exception.*;
import com.eqixiac.equinix.networkedge.model.RestoreFeasibility;
import org.junit.jupiter.api.*;

import static com.eqixiac.equinix.core.ResponseStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based API tests for Network Edge restore feasibility analysis
 * (networkEdge.backups().checkRestoreFeasibility(backupUuid, deviceUuid)).
 */
class NetworkEdgeRestoreFeasibilityWireMockTest extends WireMockTestBase {

    static NetworkEdge networkEdge;

    static final String BACKUP_UUID = "bkp-1111-2222-3333-444455556666";
    static final String DEVICE_UUID = "dev-9999-8888-7777-666655554444";

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

    @Test
    @DisplayName("checkRestoreFeasibility() returns feasibility with deviceBackup and flags")
    void returnsFeasibility() {
        stubSingleton(wireMock, "/ne/v1/devices/.*/restoreAnalysis",
                "/json/networkedge/restore_feasibility_response.json");

        RestoreFeasibility feasibility =
                networkEdge.backups().checkRestoreFeasibility(BACKUP_UUID, DEVICE_UUID);

        assertNotNull(feasibility);
        assertEquals(Boolean.TRUE, feasibility.getRestoreAllowedAfterDeleteOrEdit());
        assertNotNull(feasibility.getDeviceBackup());
        assertEquals(BACKUP_UUID, feasibility.getDeviceBackup().getUuid());
        assertEquals("test-backup", feasibility.getDeviceBackup().getName());
    }

    @Test
    @DisplayName("checkRestoreFeasibility() sends backupUuid query param")
    void sendsBackupUuidQueryParam() {
        stubSingleton(wireMock, "/ne/v1/devices/.*/restoreAnalysis",
                "/json/networkedge/restore_feasibility_response.json");

        networkEdge.backups().checkRestoreFeasibility(BACKUP_UUID, DEVICE_UUID);

        wireMock.verify(getRequestedFor(urlPathMatching("/ne/v1/devices/" + DEVICE_UUID + "/restoreAnalysis"))
                .withQueryParam("backupUuid", equalTo(BACKUP_UUID)));
    }

    @Test
    @DisplayName("500 throws EquinixServerException")
    void serverError() {
        stubErrorInline(wireMock, "/ne/v1/devices/.*/restoreAnalysis",
                500, "[{\"errorCode\":\"ERR-500\",\"errorMessage\":\"Internal server error\"}]");

        assertThrows(EquinixServerException.class,
                () -> networkEdge.backups().checkRestoreFeasibility(BACKUP_UUID, DEVICE_UUID));
    }
}
