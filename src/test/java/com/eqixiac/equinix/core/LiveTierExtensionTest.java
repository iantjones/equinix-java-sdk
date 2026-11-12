package com.eqixiac.equinix.core;

import com.eqixiac.equinix.core.IntegrationTestReport.ApiCallRecord;
import com.eqixiac.equinix.core.IntegrationTestReport.Outcome;
import com.eqixiac.equinix.core.exception.EquinixAuthorizationException;
import com.eqixiac.equinix.core.exception.EquinixClientException;
import com.eqixiac.equinix.core.exception.EquinixServerException;
import com.eqixiac.equinix.core.exception.EquinixServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;
import org.opentest4j.TestAbortedException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Offline tests of the live-tier plumbing ({@link LiveTierExtension}, the timed helpers of
 * {@link IntegrationTestBase} and {@link IntegrationTestReport}). Runs in the default suite.
 */
class LiveTierExtensionTest {

    private final IntegrationTestReport report = IntegrationTestReport.getInstance();

    @BeforeEach
    void clearReport() {
        report.clear();
    }

    private static EquinixServiceException http(int status) {
        return switch (status) {
            case 403 -> new EquinixAuthorizationException("forbidden", status, "/x", null, null);
            case 500, 502 -> new EquinixServerException("server", status, "/x", null, null);
            default -> new EquinixServiceException("err", status, "/x", null, null);
        };
    }

    private ApiCallRecord lastCall() {
        List<ApiCallRecord> calls = report.getRecords();
        return calls.get(calls.size() - 1);
    }

    // Tier gate

    @Test
    void fullAndScenarioRequireDoubleOptIn() {
        for (String tag : List.of("integration-full", "integration-scenario")) {
            Set<String> tags = Set.of("integration-readonly", tag);
            assertTrue(LiveTierExtension.refusal(tags, "readonly", "true", "false").isPresent());
            assertTrue(LiveTierExtension.refusal(tags, "full", "false", "false").isPresent(),
                    "testMode=full alone must not run " + tag);
            assertTrue(LiveTierExtension.refusal(tags, "dryrun", "true", "true").isPresent());
            assertFalse(LiveTierExtension.refusal(tags, "full", "true", "false").isPresent());
        }
    }

    @Test
    void dryRunExistingNeedsItsOwnOptIn() {
        Set<String> tags = Set.of("integration-readonly", "integration-dryrun-existing");
        assertTrue(LiveTierExtension.refusal(tags, "dryrun", "false", "false").isPresent(),
                "the plain dry-run tier must not run dry-run updates against existing resources");
        assertTrue(LiveTierExtension.refusal(tags, "readonly", "false", "true").isPresent());
        assertFalse(LiveTierExtension.refusal(tags, "dryrun", "false", "true").isPresent());
    }

    @Test
    void dryRunNeedsDryRunMode() {
        Set<String> tags = Set.of("integration-readonly", "integration-dryrun");
        assertTrue(LiveTierExtension.refusal(tags, "readonly", "false", "false").isPresent());
        assertFalse(LiveTierExtension.refusal(tags, "dryrun", "false", "false").isPresent());
        assertFalse(LiveTierExtension.refusal(Set.of("integration-readonly"), "readonly", "false", "false").isPresent());
    }

    // 5xx classification

    @Test
    void serverErrorBecomesServerErrorAbort() {
        Throwable classified = LiveTierExtension.classify(new RuntimeException("wrapped", http(500)));
        assertInstanceOf(LiveTierExtension.ServerErrorAbortedException.class, classified);
        assertTrue(classified.getMessage().startsWith("SERVER_ERROR HTTP 500"));
    }

    @Test
    void clientErrorsAndDeserializationFailuresStillFail() {
        EquinixServiceException badRequest = http(400);
        assertSame(badRequest, LiveTierExtension.classify(badRequest));
        EquinixClientException parse = new EquinixClientException("Failed to deserialize response",
                new IllegalStateException("bad json"));
        assertSame(parse, LiveTierExtension.classify(parse));
        AssertionFailedError assertion = new AssertionFailedError("x");
        assertSame(assertion, LiveTierExtension.classify(assertion));
    }

    // timedCall

    @Test
    void timedCallDoesNotInventAStatusOnSuccess() {
        assertEquals("ok", IntegrationTestBase.timedCall("D", "op", "R", "GET", () -> "ok"));
        assertNull(lastCall().statusCode, "success status is unknown (2xx), not a fabricated 200");
        assertEquals(Outcome.PASS, lastCall().outcome);
    }

    @Test
    void timedCallRecordsTheRealStatusAndOutcome() {
        assertThrows(EquinixServiceException.class,
                () -> IntegrationTestBase.timedCall("D", "op", "R", "GET", () -> { throw http(404); }));
        assertEquals(404, lastCall().statusCode);
        assertEquals(Outcome.FAIL, lastCall().outcome);

        assertThrows(EquinixServerException.class,
                () -> IntegrationTestBase.timedCall("D", "op", "R", "GET", () -> { throw http(502); }));
        assertEquals(502, lastCall().statusCode);
        assertEquals(Outcome.SERVER_ERROR, lastCall().outcome);
    }

    // timedExpectedFailure

    @Test
    void expectedFailureFailsWhenTheCallSucceeds() {
        assertThrows(AssertionFailedError.class,
                () -> IntegrationTestBase.timedExpectedFailure("D", "op", "R", "POST", null, () -> { }));
        assertEquals(Outcome.FAIL, lastCall().outcome);
    }

    @Test
    void expectedFailurePassesOnA4xxOrClientSideRejection() {
        IntegrationTestBase.timedExpectedFailure("D", "op", "R", "POST", null, () -> { throw http(400); });
        assertEquals(Outcome.PASS, lastCall().outcome);
        assertEquals(400, lastCall().statusCode);
        IntegrationTestBase.timedExpectedFailure("D", "op", "R", "POST", null,
                () -> { throw new IllegalArgumentException("vlan"); });
        assertEquals(Outcome.PASS, lastCall().outcome);
    }

    @Test
    void expectedFailureDoesNotSwallowUnexpectedErrors() {
        assertThrows(EquinixServerException.class, () -> IntegrationTestBase.timedExpectedFailure(
                "D", "op", "R", "POST", null, () -> { throw http(500); }));
        assertEquals(Outcome.SERVER_ERROR, lastCall().outcome);
        assertThrows(NullPointerException.class, () -> IntegrationTestBase.timedExpectedFailure(
                "D", "op", "R", "POST", null, () -> { throw new NullPointerException(); }));
        assertEquals(Outcome.FAIL, lastCall().outcome);
    }

    // skipWhen / requireEntitled

    @Test
    void skipWhenAbortsOnlyForTheAcceptedErrors() {
        assertThrows(TestAbortedException.class, () -> IntegrationTestBase.skipWhen("D", "op", "R", "GET",
                IntegrationTestBase.status(404), "resource not visible", () -> { throw http(404); }));
        assertEquals(Outcome.SKIPPED, lastCall().outcome);
        assertTrue(lastCall().errorMessage.startsWith("resource not visible - HTTP 404"));

        assertThrows(EquinixServiceException.class, () -> IntegrationTestBase.skipWhen("D", "op", "R", "GET",
                IntegrationTestBase.status(404), "resource not visible", () -> { throw http(400); }));
        assertEquals(Outcome.FAIL, lastCall().outcome);
    }

    @Test
    void requireEntitledSkipsOn403Only() {
        assertThrows(TestAbortedException.class, () -> IntegrationTestBase.requireEntitled("D", "op", "R", "GET",
                () -> { throw http(403); }));
        assertThrows(EquinixServiceException.class, () -> IntegrationTestBase.requireEntitled("D", "op", "R", "GET",
                () -> { throw http(400); }));
    }

    // Report

    @Test
    void reportCountsEveryOutcomeAndEscapesHtml(@org.junit.jupiter.api.io.TempDir Path dir) throws Exception {
        report.recordTest("a.B", "t1", Outcome.PASS, null);
        report.recordTest("a.B", "t2", Outcome.SKIPPED, "no <accountNo>");
        report.recordTest("a.B", "t3", Outcome.SERVER_ERROR, "HTTP 500");
        report.recordTest("a.B", "t4", Outcome.FAIL, "boom");
        assertEquals(1L, report.testCounts().get(Outcome.SKIPPED));
        assertEquals(1L, report.testCounts().get(Outcome.SERVER_ERROR));

        Path html = dir.resolve("r.html");
        report.writeHtmlReport(html);
        String text = Files.readString(html);
        assertTrue(text.contains("no &lt;accountNo&gt;"));
        assertFalse(text.contains("no <accountNo>"));

        Path json = dir.resolve("r.json");
        report.writeJsonReport(json);
        String body = Files.readString(json);
        assertTrue(body.contains("\"SERVER_ERROR\" : 1"), body);
    }
}
