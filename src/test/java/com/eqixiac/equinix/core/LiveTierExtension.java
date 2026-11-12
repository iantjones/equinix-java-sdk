package com.eqixiac.equinix.core;

import com.eqixiac.equinix.core.IntegrationTestReport.Outcome;
import com.eqixiac.equinix.core.exception.EquinixServiceException;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.LifecycleMethodExecutionExceptionHandler;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.junit.jupiter.api.extension.TestWatcher;
import org.opentest4j.TestAbortedException;

import java.util.Optional;
import java.util.Set;

/**
 * JUnit extension shared by every live-tier test (registered on {@link IntegrationTestBase}, so it
 * also applies to the {@code @Nested} classes of each integration test).
 *
 * <ol>
 *     <li><b>Runtime tier gate</b> ({@link ExecutionCondition}). The Maven profiles already select
 *         tests by tag; this is the second, independent layer. A test or class carrying
 *         {@code integration-full} or {@code integration-scenario} runs only with
 *         {@code testMode=full} AND {@code confirmDestructive=true}; {@code integration-dryrun-existing}
 *         only with {@code testMode=dryrun|full} AND {@code dryRunExisting=true};
 *         {@code integration-dryrun} only with {@code testMode=dryrun|full}.</li>
 *     <li><b>5xx classification</b>. An {@link EquinixServiceException} with status &gt;= 500 thrown
 *         by a test (or its {@code @BeforeEach}/{@code @BeforeAll}) is converted into a
 *         {@link ServerErrorAbortedException}: the test is reported as SERVER_ERROR and does not
 *         fail the tier. Deserialization failures, 4xx and assertion errors still FAIL.</li>
 *     <li><b>Verdict recording</b> ({@link TestWatcher}) of PASS / SKIPPED (with reason) /
 *         SERVER_ERROR / FAIL into {@link IntegrationTestReport}.</li>
 * </ol>
 */
public class LiveTierExtension implements ExecutionCondition, TestWatcher,
        TestExecutionExceptionHandler, LifecycleMethodExecutionExceptionHandler {

    static final String TAG_DRYRUN = "integration-dryrun";
    static final String TAG_DRYRUN_EXISTING = "integration-dryrun-existing";
    static final String TAG_FULL = "integration-full";
    static final String TAG_SCENARIO = "integration-scenario";

    /** Aborts a test because the live endpoint answered HTTP 5xx; reported as SERVER_ERROR. */
    public static class ServerErrorAbortedException extends TestAbortedException {
        private static final long serialVersionUID = 1L;

        public ServerErrorAbortedException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    // Tier gate

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        Optional<String> refusal = refusal(context.getTags(),
                System.getProperty("testMode", "readonly"),
                System.getProperty("confirmDestructive", "false"),
                System.getProperty("dryRunExisting", "false"));
        if (refusal.isEmpty()) {
            return ConditionEvaluationResult.enabled("live tier allowed");
        }
        // TestWatcher only sees disabled METHODS; record a disabled container here.
        if (context.getTestMethod().isEmpty() && context.getTestClass().isPresent()) {
            IntegrationTestReport.getInstance().recordTest(context.getTestClass().get().getName(),
                    context.getDisplayName() + " (class)", Outcome.SKIPPED, refusal.get());
        }
        return ConditionEvaluationResult.disabled(refusal.get());
    }

    /**
     * Returns why a test with these tags must not run in this mode, or empty if it may run.
     * Package-private for {@code LiveTierExtensionTest}.
     */
    static Optional<String> refusal(Set<String> tags, String testMode, String confirmDestructive,
                                    String dryRunExisting) {
        String mode = testMode == null ? "readonly" : testMode.toLowerCase();
        boolean full = mode.equals("full") && "true".equalsIgnoreCase(confirmDestructive);
        boolean dryRun = mode.equals("dryrun") || mode.equals("full");
        if (tags.contains(TAG_FULL) || tags.contains(TAG_SCENARIO)) {
            return full ? Optional.empty() : Optional.of(
                    "Creates real resources: requires -DtestMode=full AND -DconfirmDestructive=true (double opt-in)");
        }
        if (tags.contains(TAG_DRYRUN_EXISTING)) {
            return dryRun && "true".equalsIgnoreCase(dryRunExisting) ? Optional.empty() : Optional.of(
                    "Dry-runs updates against PRE-EXISTING resources: requires -Pintegration-dryrun-existing");
        }
        if (tags.contains(TAG_DRYRUN)) {
            return dryRun ? Optional.empty() : Optional.of(
                    "Dry-run creates: requires -Pintegration-dryrun (testMode=dryrun)");
        }
        return Optional.empty();
    }

    // 5xx -> SERVER_ERROR

    @Override
    public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
        throw classify(throwable);
    }

    @Override
    public void handleBeforeAllMethodExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
        throw classify(throwable);
    }

    @Override
    public void handleBeforeEachMethodExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
        throw classify(throwable);
    }

    /** Converts a 5xx service error (anywhere in the cause chain) into a SERVER_ERROR abort. */
    static Throwable classify(Throwable throwable) {
        if (throwable instanceof TestAbortedException) {
            return throwable;
        }
        EquinixServiceException serverError = serverError(throwable);
        if (serverError != null) {
            return new ServerErrorAbortedException("SERVER_ERROR " + describe(serverError), throwable);
        }
        return throwable;
    }

    /** The first {@link EquinixServiceException} with HTTP status &gt;= 500 in the cause chain, or null. */
    static EquinixServiceException serverError(Throwable throwable) {
        for (Throwable t = throwable; t != null; t = t.getCause() == t ? null : t.getCause()) {
            if (t instanceof EquinixServiceException se && se.getStatusCode() != null && se.getStatusCode() >= 500) {
                return se;
            }
        }
        return null;
    }

    /** One-line description: {@code HTTP 500 EQ-3152100 <message> (<path>)}. */
    static String describe(EquinixServiceException e) {
        StringBuilder sb = new StringBuilder("HTTP ").append(e.getStatusCode());
        if (!e.getExceptionDetails().isEmpty()) {
            var d = e.getExceptionDetails().get(0);
            if (d.getErrorCode() != null) {
                sb.append(' ').append(d.getErrorCode());
            }
            if (d.getErrorMessage() != null) {
                sb.append(' ').append(d.getErrorMessage());
            }
        }
        if (e.getPath() != null) {
            sb.append(" (").append(e.getPath()).append(')');
        }
        return sb.toString();
    }

    // Verdicts

    @Override
    public void testSuccessful(ExtensionContext context) {
        record(context, Outcome.PASS, null);
    }

    @Override
    public void testAborted(ExtensionContext context, Throwable cause) {
        record(context, cause instanceof ServerErrorAbortedException ? Outcome.SERVER_ERROR : Outcome.SKIPPED,
                cause == null ? null : cause.getMessage());
    }

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        record(context, Outcome.FAIL, cause == null ? null : cause.getClass().getSimpleName() + ": " + cause.getMessage());
    }

    @Override
    public void testDisabled(ExtensionContext context, Optional<String> reason) {
        record(context, Outcome.SKIPPED, reason.orElse("disabled"));
    }

    private static void record(ExtensionContext context, Outcome outcome, String reason) {
        String testClass = context.getTestClass().map(Class::getName).orElse("?");
        IntegrationTestReport.getInstance().recordTest(testClass, context.getDisplayName(), outcome, reason);
    }
}
