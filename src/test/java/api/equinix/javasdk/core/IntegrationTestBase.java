package api.equinix.javasdk.core;

import api.equinix.javasdk.core.auth.BasicEquinixCredentials;
import api.equinix.javasdk.core.client.EquinixClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;

/**
 * Base class for integration tests that provides:
 * <ul>
 *     <li>Three test modes: readonly (default), dryrun, full</li>
 *     <li>Automatic cleanup of created resources (LIFO stack)</li>
 *     <li>Structured API call reporting</li>
 *     <li>Credential and mode management</li>
 * </ul>
 *
 * <h3>Test Modes</h3>
 * <ul>
 *     <li><b>readonly</b> — Only GET/list operations. Zero mutations. Safe for production.</li>
 *     <li><b>dryrun</b> — Includes dry-run validation calls. Still zero real mutations.</li>
 *     <li><b>full</b> — Full CRUD lifecycle. Requires both {@code -DtestMode=full}
 *         AND {@code -DconfirmDestructive=true} (double opt-in). Only for sandbox/test.</li>
 * </ul>
 *
 * <h3>Usage</h3>
 * <pre>
 * mvn test -Pintegration-readonly -Dauth.access=ID -Dauth.secret=SECRET
 * mvn test -Pintegration-dryrun   -Dauth.access=ID -Dauth.secret=SECRET
 * mvn test -Pintegration-full     -Dauth.access=ID -Dauth.secret=SECRET -DtestMode=full -DconfirmDestructive=true
 * </pre>
 */
public abstract class IntegrationTestBase {

    public enum TestMode {
        READONLY, DRYRUN, FULL
    }

    protected static TestMode testMode;
    protected static String accessKey;
    protected static String secretKey;
    protected static IntegrationTestReport report;

    private static final Deque<CleanupAction> cleanupStack = new ArrayDeque<>();

    @BeforeAll
    static void initIntegrationBase() {
        accessKey = System.getProperty("accessKey");
        secretKey = System.getProperty("secretKey");

        Assumptions.assumeTrue(accessKey != null && !accessKey.isBlank(),
                "Integration test skipped: -DaccessKey not provided");
        Assumptions.assumeTrue(secretKey != null && !secretKey.isBlank(),
                "Integration test skipped: -DsecretKey not provided");

        // Determine test mode
        String mode = System.getProperty("testMode", "readonly");
        testMode = switch (mode.toLowerCase()) {
            case "dryrun" -> TestMode.DRYRUN;
            case "full" -> {
                String confirm = System.getProperty("confirmDestructive", "false");
                Assumptions.assumeTrue("true".equalsIgnoreCase(confirm),
                        "Full CRUD mode requires -DconfirmDestructive=true for safety");
                yield TestMode.FULL;
            }
            default -> TestMode.READONLY;
        };

        report = IntegrationTestReport.getInstance();

        System.out.println("══════════════════════════════════════════════════════════════");
        System.out.println("  INTEGRATION TEST MODE: " + testMode);
        System.out.println("══════════════════════════════════════════════════════════════");
    }

    @AfterAll
    static void cleanupAndReport() {
        // Execute cleanup in reverse order
        int cleaned = 0;
        int failed = 0;
        while (!cleanupStack.isEmpty()) {
            CleanupAction action = cleanupStack.pop();
            try {
                System.out.printf("  [CLEANUP] %s: %s%n", action.resourceType, action.resourceId);
                action.cleanup.accept(action.resourceId);
                cleaned++;
            } catch (Exception e) {
                failed++;
                System.err.printf("  [CLEANUP FAILED] %s %s: %s%n",
                        action.resourceType, action.resourceId, e.getMessage());
            }
        }

        if (cleaned + failed > 0) {
            System.out.printf("  Cleanup complete: %d succeeded, %d failed%n", cleaned, failed);
        }

        // Print report
        report.printConsoleSummary();
        report.writeJsonReport(Path.of("target", "integration-report.json"));
        report.writeHtmlReport(Path.of("target", "site", "integration-report.html"));
    }

    // ── Mode Checks ────────────────────────────────────────────────────

    protected static boolean isDryRunEnabled() {
        return testMode == TestMode.DRYRUN || testMode == TestMode.FULL;
    }

    protected static boolean isFullCrudEnabled() {
        return testMode == TestMode.FULL;
    }

    // ── Credential Helpers ─────────────────────────────────────────────

    protected static BasicEquinixCredentials testCredentials() {
        return new BasicEquinixCredentials(accessKey, secretKey);
    }

    // ── Cleanup Registry ───────────────────────────────────────────────

    /**
     * Register a resource for automatic cleanup in @AfterAll.
     * Resources are cleaned up in reverse order (LIFO).
     *
     * @param resourceType  descriptive name (e.g., "ServiceToken", "Connection")
     * @param resourceId    UUID or identifier of the created resource
     * @param deleteAction  lambda that deletes the resource given its ID
     */
    protected static void registerCleanup(String resourceType, String resourceId, Consumer<String> deleteAction) {
        cleanupStack.push(new CleanupAction(resourceType, resourceId, deleteAction));
        System.out.printf("  [REGISTERED FOR CLEANUP] %s: %s%n", resourceType, resourceId);
    }

    // ── Timed Execution Helpers ────────────────────────────────────────

    /**
     * Execute an API call with timing and reporting.
     */
    protected static <T> T timedCall(String domain, String operation, String resourceType,
                                     String httpMethod, ApiCall<T> call) {
        IntegrationTestReport.ApiCallRecord record = report.startCall(domain, operation, resourceType, httpMethod);
        try {
            T result = call.execute();
            report.completeCall(record, 200, null, true, null);
            return result;
        } catch (Exception e) {
            int statusCode = extractStatusCode(e);
            report.completeCall(record, statusCode, null, false, e.getMessage());
            throw e instanceof RuntimeException re ? re : new RuntimeException(e);
        }
    }

    /**
     * Execute an API call with timing, reporting, and UUID tracking.
     */
    protected static <T> T timedCall(String domain, String operation, String resourceType,
                                     String httpMethod, String uuid, ApiCall<T> call) {
        IntegrationTestReport.ApiCallRecord record = report.startCall(domain, operation, resourceType, httpMethod);
        try {
            T result = call.execute();
            report.completeCall(record, 200, uuid, true, null);
            return result;
        } catch (Exception e) {
            int statusCode = extractStatusCode(e);
            report.completeCall(record, statusCode, uuid, false, e.getMessage());
            throw e instanceof RuntimeException re ? re : new RuntimeException(e);
        }
    }

    /**
     * Execute an API call that is expected to fail, with timing and reporting.
     */
    protected static void timedExpectedFailure(String domain, String operation, String resourceType,
                                                String httpMethod, String uuid, Runnable call) {
        IntegrationTestReport.ApiCallRecord record = report.startCall(domain, operation, resourceType, httpMethod);
        try {
            call.run();
            report.completeCall(record, 200, uuid, true, "Expected failure but succeeded");
        } catch (Exception e) {
            int statusCode = extractStatusCode(e);
            report.completeCall(record, statusCode, uuid, true, "Expected: " + e.getClass().getSimpleName());
        }
    }

    /**
     * Runs a live call and asserts it succeeds, tolerating ONLY an entitlement gap:
     * a 401/403 from the API skips the test (this credential simply doesn't carry
     * the product), while any other failure — a deserialization error, a 404 on a
     * collection URL, a 5xx, an unmapped enum value — FAILS the test. This is the
     * spec-vs-reality contract of the read-only tier: a read must never skip on a
     * real defect.
     */
    protected static <T> T requireEntitled(String domain, String operation, String resourceType,
                                           String httpMethod, ApiCall<T> call) {
        try {
            return timedCall(domain, operation, resourceType, httpMethod, call);
        } catch (api.equinix.javasdk.core.exception.EquinixAuthenticationException
                 | api.equinix.javasdk.core.exception.EquinixAuthorizationException e) {
            Assumptions.abort(resourceType + " skipped: credential not entitled — " + e.getMessage());
            return null; // unreachable
        }
    }

    @FunctionalInterface
    protected interface ApiCall<T> {
        T execute();
    }

    // ── Naming Convention ──────────────────────────────────────────────

    protected static String testResourceName(String suffix) {
        return "sdk-test-" + suffix + "-" + System.currentTimeMillis();
    }

    // ── Internals ──────────────────────────────────────────────────────

    private static int extractStatusCode(Exception e) {
        // Try to extract status code from EquinixServiceException
        try {
            var method = e.getClass().getMethod("getStatusCode");
            Object result = method.invoke(e);
            if (result instanceof Integer i) return i;
        } catch (Exception ignored) {}
        return 0;
    }

    private static class CleanupAction {
        final String resourceType;
        final String resourceId;
        final Consumer<String> cleanup;

        CleanupAction(String resourceType, String resourceId, Consumer<String> cleanup) {
            this.resourceType = resourceType;
            this.resourceId = resourceId;
            this.cleanup = cleanup;
        }
    }
}
