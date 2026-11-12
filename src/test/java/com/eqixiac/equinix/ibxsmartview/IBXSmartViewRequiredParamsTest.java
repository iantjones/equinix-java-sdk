package com.eqixiac.equinix.ibxsmartview;

import com.eqixiac.equinix.IBXSmartView;
import com.eqixiac.equinix.core.auth.BasicEquinixCredentials;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The v1 hierarchy, legacy environmental/power and asset calls fail fast, before any request, when a
 * parameter the spec marks required is missing. Live (2026-11-11) the service answers HTTP 400
 * ("accountNo is a mandatory parameter/header", "Required parameter 'accountNo'") instead.
 */
class IBXSmartViewRequiredParamsTest {

    static IBXSmartView smartView;

    @BeforeAll
    static void setUp() {
        // No request is sent: the checks run before authentication.
        smartView = new IBXSmartView(new BasicEquinixCredentials("unused-id", "unused-secret"));
    }

    @AfterAll
    static void tearDown() throws Exception {
        smartView.close();
    }

    private static void assertNamed(String param, Executable call) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, call);
        assertTrue(e.getMessage().startsWith(param + " is required"), e.getMessage());
    }

    @Test
    void hierarchyRequiresAccountNo() {
        assertNamed("accountNo", () -> smartView.hierarchy().getLocationHierarchy(null, "DC2"));
        assertNamed("accountNo", () -> smartView.hierarchy().getPowerHierarchy(" ", null));
    }

    @Test
    void legacyEnvironmentalRequiresAccountNoAndLevel() {
        assertNamed("accountNo", () -> smartView.legacyEnvironmentals().getCurrent(null, "DC2", "CAGE", "DC2:01"));
        assertNamed("accountNo", () -> smartView.legacyEnvironmentals().listCurrent(null, "DC2", "CAGE"));
        assertNamed("levelType", () -> smartView.legacyEnvironmentals().listCurrent("1", "DC2", null));
        assertNamed("accountNo", () -> smartView.legacyEnvironmentals()
                .getTrending(null, "DC2", "TEMPERATURE", "CAGE", "DC2:01", "DAILY", "2026-01-01", "2026-01-02"));
    }

    @Test
    void legacyPowerRequiresAccountNo() {
        assertNamed("accountNo", () -> smartView.legacyPower().getCurrent(null, "DC2", "CAGE", "DC2:01"));
        assertNamed("accountNo", () -> smartView.legacyPower()
                .getTrending(null, "DC2", "CAGE", "DC2:01", "DAILY", "2026-01-01", "2026-01-02"));
    }

    @Test
    void assetCallsRequireTheirParams() {
        assertNamed("searchString", () -> smartView.smartViewAssets().search("1", "DC2", null));
        assertNamed("tagId", () -> smartView.smartViewAssets().getCurrentTagPoint("1", "DC2", null));
        assertNamed("classification", () -> smartView.smartViewAssets().getAffectedAssets("1", "DC2", "A1", null));
    }
}
