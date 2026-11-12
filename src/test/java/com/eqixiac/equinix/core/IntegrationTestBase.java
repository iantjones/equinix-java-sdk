package com.eqixiac.equinix.core;

import com.eqixiac.equinix.core.auth.BasicEquinixCredentials;
import com.eqixiac.equinix.core.client.EquinixClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opentest4j.TestAbortedException;
import com.eqixiac.equinix.core.IntegrationTestReport.Outcome;
import com.eqixiac.equinix.core.exception.EquinixAuthenticationException;
import com.eqixiac.equinix.core.exception.EquinixAuthorizationException;
import com.eqixiac.equinix.core.exception.EquinixServiceException;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Base class for integration tests that provides:
 * <ul>
 *     <li>Three test modes: readonly (default), dryrun, full</li>
 *     <li>Automatic cleanup of created resources (LIFO stack)</li>
 *     <li>Structured API call reporting</li>
 *     <li>Credential and mode management</li>
 * </ul>
 *
 * <h3>Tiers (Maven profile = JUnit tag)</h3>
 * <ul>
 *     <li><b>integration-readonly</b> - GET / list / search only. Zero mutations.</li>
 *     <li><b>integration-dryrun</b> - adds {@code dryRun=true} CREATES and validate endpoints,
 *         which leave nothing behind.</li>
 *     <li><b>integration-dryrun-existing</b> - {@code dryRun=true} UPDATES against pre-existing
 *         resources (no-op payloads); separate opt-in.</li>
 *     <li><b>integration-full</b> / <b>integration-scenario</b> - create real resources. Require
 *         the profile AND {@code -DconfirmDestructive=true} (double opt-in). Sandbox only.</li>
 * </ul>
 * <p>The profiles select by tag; {@link LiveTierExtension} re-checks the mode at runtime and
 * classifies every test as PASS / SKIPPED (with reason) / SERVER_ERROR / FAIL in
 * {@code target/integration-report.json} and {@code target/site/integration-report.html}.</p>
 *
 * <h3>Usage</h3>
 * <pre>
 * export EQUINIX_ACCESS_KEY=... EQUINIX_SECRET_KEY=...      (environment only, never -D)
 * mvn test -Pintegration-readonly
 * mvn test -Pintegration-dryrun
 * mvn test -Pintegration-dryrun-existing
 * mvn test -Pintegration-full -DconfirmDestructive=true
 * mvn test -Pintegration-scenario -DconfirmDestructive=true
 * </pre>
 */
@ExtendWith(LiveTierExtension.class)
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
        accessKey = credential("EQUINIX_ACCESS_KEY", "accessKey");
        secretKey = credential("EQUINIX_SECRET_KEY", "secretKey");

        Assumptions.assumeTrue(accessKey != null && !accessKey.isBlank(),
                "Integration test skipped: EQUINIX_ACCESS_KEY not set");
        Assumptions.assumeTrue(secretKey != null && !secretKey.isBlank(),
                "Integration test skipped: EQUINIX_SECRET_KEY not set");

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

        System.out.println("==============================================================");
        System.out.println("  INTEGRATION TEST MODE: " + testMode);
        System.out.println("==============================================================");
    }

    /**
     * Reads a live-tier credential from its environment variable, falling back to a system
     * property for one-off command lines. Environment variables are preferred because surefire
     * can copy system properties into its XML reports (the integration profiles also set
     * {@code enablePropertiesElement=false}). An unresolved Maven placeholder such as
     * {@code ${auth.access}} counts as unset.
     */
    private static String credential(String envName, String propertyName) {
        String value = System.getenv(envName);
        if (value == null || value.isBlank()) {
            value = System.getProperty(propertyName);
        }
        if (value == null || value.isBlank() || value.startsWith("${")) {
            return null;
        }
        return value.trim();
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

        // No report exists when @BeforeAll skipped the class (credentials not set): nothing ran.
        if (report == null) {
            return;
        }
        // Print report
        report.printConsoleSummary();
        report.writeJsonReport(Path.of("target", "integration-report.json"));
        report.writeHtmlReport(Path.of("target", "site", "integration-report.html"));
    }

    // Mode Checks

    protected static boolean isDryRunEnabled() {
        return testMode == TestMode.DRYRUN || testMode == TestMode.FULL;
    }

    protected static boolean isFullCrudEnabled() {
        return testMode == TestMode.FULL;
    }

    // Credential Helpers

    protected static BasicEquinixCredentials testCredentials() {
        return new BasicEquinixCredentials(accessKey, secretKey);
    }

    // Cleanup Registry

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

    // Timed Execution Helpers

    /**
     * Execute an API call with timing and reporting. A normal return is recorded as PASS with
     * status "2xx" (the SDK does not expose the exact success code, so none is invented); a
     * service error is recorded with its real HTTP status, as SERVER_ERROR for 5xx and FAIL
     * otherwise, and rethrown (the {@link LiveTierExtension} turns a 5xx into a SERVER_ERROR
     * abort; anything else fails the test).
     */
    protected static <T> T timedCall(String domain, String operation, String resourceType,
                                     String httpMethod, ApiCall<T> call) {
        return timedCall(domain, operation, resourceType, httpMethod, null, call);
    }

    /**
     * Execute an API call with timing, reporting, and UUID tracking; see
     * {@link #timedCall(String, String, String, String, ApiCall)}.
     */
    protected static <T> T timedCall(String domain, String operation, String resourceType,
                                     String httpMethod, String uuid, ApiCall<T> call) {
        IntegrationTestReport.ApiCallRecord record =
                IntegrationTestReport.getInstance().startCall(domain, operation, resourceType, httpMethod);
        try {
            T result = call.execute();
            IntegrationTestReport.getInstance().completeCall(record, null, uuid, Outcome.PASS, null);
            return result;
        } catch (TestAbortedException e) {
            IntegrationTestReport.getInstance().completeCall(record, null, uuid, Outcome.SKIPPED, e.getMessage());
            throw e;
        } catch (RuntimeException e) {
            Integer status = statusOf(e);
            Outcome outcome = status != null && status >= 500 ? Outcome.SERVER_ERROR : Outcome.FAIL;
            IntegrationTestReport.getInstance().completeCall(record, status, uuid, outcome, e.getMessage());
            throw e;
        }
    }

    /**
     * Execute an API call that is EXPECTED to be rejected. Passes (and records PASS) when the
     * service answers 4xx or the SDK rejects the request client-side
     * ({@link IllegalArgumentException} / {@link IllegalStateException}). FAILS the test when
     * the call succeeds, and rethrows anything else (a 5xx becomes SERVER_ERROR through the
     * extension; a deserialization error or NPE fails).
     */
    protected static void timedExpectedFailure(String domain, String operation, String resourceType,
                                                String httpMethod, String uuid, Runnable call) {
        IntegrationTestReport report = IntegrationTestReport.getInstance();
        IntegrationTestReport.ApiCallRecord record = report.startCall(domain, operation, resourceType, httpMethod);
        try {
            call.run();
        } catch (EquinixServiceException e) {
            Integer status = e.getStatusCode();
            if (status != null && status >= 400 && status < 500) {
                report.completeCall(record, status, uuid, Outcome.PASS, "Expected rejection: HTTP " + status);
                return;
            }
            report.completeCall(record, status, uuid,
                    status != null && status >= 500 ? Outcome.SERVER_ERROR : Outcome.FAIL, e.getMessage());
            throw e;
        } catch (IllegalArgumentException | IllegalStateException e) {
            report.completeCall(record, null, uuid, Outcome.PASS,
                    "Expected client-side rejection: " + e.getClass().getSimpleName());
            return;
        } catch (RuntimeException e) {
            report.completeCall(record, null, uuid, Outcome.FAIL, e.toString());
            throw e;
        }
        report.completeCall(record, null, uuid, Outcome.FAIL, "Expected a rejection but the call succeeded");
        fail(domain + " " + operation + ": expected the call to be rejected, but it succeeded");
    }

    /**
     * Runs a live call and asserts it succeeds, tolerating ONLY an entitlement gap:
     * a 401/403 from the API skips the test (this credential simply doesn't carry
     * the product), while any other failure - a deserialization error, a 404 on a
     * collection URL, an unmapped enum value - FAILS the test, and a 5xx is reported as
     * SERVER_ERROR. This is the spec-vs-reality contract of the read-only tier: a read must
     * never skip on a real defect.
     */
    protected static <T> T requireEntitled(String domain, String operation, String resourceType,
                                           String httpMethod, ApiCall<T> call) {
        return skipWhen(domain, operation, resourceType, httpMethod,
                e -> e instanceof EquinixAuthenticationException || e instanceof EquinixAuthorizationException,
                "credential not entitled", call);
    }

    /**
     * Runs a live call, SKIPPING the test (recorded as SKIPPED with {@code reason} and the
     * service's answer) when the service rejects it in a way {@code skipOn} accepts as a
     * known test-data or entitlement limitation rather than an SDK defect. Every other failure
     * behaves as in {@link #timedCall(String, String, String, String, ApiCall)}.
     *
     * @param skipOn which service errors mean "cannot be exercised with this account / data"
     * @param reason why such an error is not an SDK defect (shown in the report)
     */
    protected static <T> T skipWhen(String domain, String operation, String resourceType, String httpMethod,
                                    Predicate<EquinixServiceException> skipOn, String reason, ApiCall<T> call) {
        IntegrationTestReport report = IntegrationTestReport.getInstance();
        IntegrationTestReport.ApiCallRecord record = report.startCall(domain, operation, resourceType, httpMethod);
        try {
            T result = call.execute();
            report.completeCall(record, null, null, Outcome.PASS, null);
            return result;
        } catch (TestAbortedException e) {
            report.completeCall(record, null, null, Outcome.SKIPPED, e.getMessage());
            throw e;
        } catch (EquinixServiceException e) {
            Integer status = e.getStatusCode();
            if (skipOn.test(e)) {
                String why = reason + " - " + LiveTierExtension.describe(e);
                report.completeCall(record, status, null, Outcome.SKIPPED, why);
                Assumptions.abort(resourceType + " " + operation + " skipped: " + why);
            }
            report.completeCall(record, status, null,
                    status != null && status >= 500 ? Outcome.SERVER_ERROR : Outcome.FAIL, e.getMessage());
            throw e;
        } catch (RuntimeException e) {
            report.completeCall(record, null, null, Outcome.FAIL, e.toString());
            throw e;
        }
    }

    /** Matches a service error with one of the given HTTP statuses. */
    protected static Predicate<EquinixServiceException> status(int... statuses) {
        return e -> e.getStatusCode() != null && IntStream.of(statuses).anyMatch(s -> s == e.getStatusCode());
    }

    /** Matches a service error with the given HTTP status and Equinix error code (e.g. {@code EQ-3045014}). */
    protected static Predicate<EquinixServiceException> errorCode(int status, String code) {
        return e -> e.getStatusCode() != null && e.getStatusCode() == status
                && e.getExceptionDetails().stream().anyMatch(d -> code.equals(d.getErrorCode()));
    }

    /** Matches a service error whose status is one of {@code statuses} and whose details mention {@code text}. */
    protected static Predicate<EquinixServiceException> statusWithText(String text, int... statuses) {
        return status(statuses).and(e -> e.getMessage() != null
                && e.getMessage().toLowerCase().contains(text.toLowerCase()));
    }

    @FunctionalInterface
    protected interface ApiCall<T> {
        T execute();
    }

    // Naming Convention

    protected static String testResourceName(String suffix) {
        return "sdk-test-" + suffix + "-" + System.currentTimeMillis();
    }

    // Internals

    private static Integer statusOf(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause() == t ? null : t.getCause()) {
            if (t instanceof EquinixServiceException se) {
                return se.getStatusCode();
            }
        }
        return null;
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
