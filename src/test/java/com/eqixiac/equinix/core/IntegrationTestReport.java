package com.eqixiac.equinix.core;

import com.eqixiac.equinix.core.internal.Constants;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Singleton that collects structured results during a live integration run and writes a console
 * summary, a JSON report ({@code target/integration-report.json}) and an HTML report
 * ({@code target/site/integration-report.html}).
 *
 * <p>Two things are recorded:</p>
 * <ul>
 *     <li><b>Tests</b> (one {@link TestRecord} per test or skipped container), classified by
 *         {@link Outcome}. This is the tier verdict: PASS, SKIPPED with the reason, SERVER_ERROR
 *         (the endpoint answered 5xx, which is reported but does not fail the tier) and FAIL.</li>
 *     <li><b>API calls</b> made through {@code IntegrationTestBase.timedCall(...)}, with latency
 *         and the same outcome classification.</li>
 * </ul>
 */
public final class IntegrationTestReport {

    /** How a test or API call ended. */
    public enum Outcome {
        /** The test passed (or the call returned normally, which means a 2xx response). */
        PASS,
        /** The test was skipped by an assumption or a disabled tier; the reason is recorded. */
        SKIPPED,
        /** The endpoint answered HTTP 5xx: a service-side failure, reported without failing the tier. */
        SERVER_ERROR,
        /** A real failure: assertion, deserialization error, unexpected 4xx, or exception. */
        FAIL
    }

    private static final IntegrationTestReport INSTANCE = new IntegrationTestReport();

    private final List<ApiCallRecord> records = new CopyOnWriteArrayList<>();
    private final List<TestRecord> tests = new CopyOnWriteArrayList<>();
    private final LocalDateTime suiteStart = LocalDateTime.now();

    private IntegrationTestReport() {}

    public static IntegrationTestReport getInstance() {
        return INSTANCE;
    }

    // Recording

    public ApiCallRecord startCall(String domain, String operation, String resourceType, String httpMethod) {
        ApiCallRecord record = new ApiCallRecord();
        record.timestamp = Instant.now().toString();
        record.domain = domain;
        record.operation = operation;
        record.resourceType = resourceType;
        record.httpMethod = httpMethod;
        record.startNanos = System.nanoTime();
        return record;
    }

    /**
     * Completes an API-call record.
     *
     * @param statusCode the HTTP status when known; {@code null} for a call that returned
     *                   normally (the SDK does not expose the exact 2xx code) or failed before
     *                   any HTTP response
     */
    public void completeCall(ApiCallRecord record, Integer statusCode, String uuid, Outcome outcome, String message) {
        record.latencyMs = (System.nanoTime() - record.startNanos) / 1_000_000.0;
        record.statusCode = statusCode;
        record.uuid = uuid;
        record.outcome = outcome;
        record.success = outcome == Outcome.PASS;
        record.errorMessage = message;
        records.add(record);
    }

    /** Records the verdict of one test (or of a skipped test container). */
    public void recordTest(String testClass, String displayName, Outcome outcome, String reason) {
        TestRecord record = new TestRecord();
        record.timestamp = Instant.now().toString();
        record.testClass = testClass;
        record.test = displayName;
        record.outcome = outcome;
        record.reason = reason;
        tests.add(record);
    }

    // Reporting

    public List<ApiCallRecord> getRecords() {
        return new ArrayList<>(records);
    }

    public List<TestRecord> getTests() {
        return new ArrayList<>(tests);
    }

    public void clear() {
        records.clear();
        tests.clear();
    }

    /** Test verdict counts, in {@link Outcome} order, including zero counts. */
    public Map<Outcome, Long> testCounts() {
        Map<Outcome, Long> counts = new EnumMap<>(Outcome.class);
        for (Outcome o : Outcome.values()) {
            counts.put(o, tests.stream().filter(t -> t.outcome == o).count());
        }
        return counts;
    }

    private Map<Outcome, Long> callCounts() {
        Map<Outcome, Long> counts = new EnumMap<>(Outcome.class);
        for (Outcome o : Outcome.values()) {
            counts.put(o, records.stream().filter(r -> r.outcome == o).count());
        }
        return counts;
    }

    /**
     * Print a console summary: test verdict counts, every non-passing test with its reason, then
     * the API-call log grouped by domain.
     */
    public void printConsoleSummary() {
        String rule = "==============================================================";
        System.out.println("\n" + rule);
        System.out.println("  EQUINIX JAVA SDK - LIVE INTEGRATION REPORT");
        System.out.println(rule);
        System.out.printf("  Suite started: %s%n", suiteStart.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        Map<Outcome, Long> tc = testCounts();
        System.out.printf("  Tests: %d  |  PASS %d  |  SKIPPED %d  |  SERVER_ERROR %d  |  FAIL %d%n",
                tests.size(), tc.get(Outcome.PASS), tc.get(Outcome.SKIPPED),
                tc.get(Outcome.SERVER_ERROR), tc.get(Outcome.FAIL));
        Map<Outcome, Long> cc = callCounts();
        System.out.printf("  API calls: %d  |  PASS %d  |  SKIPPED %d  |  SERVER_ERROR %d  |  FAIL %d%n",
                records.size(), cc.get(Outcome.PASS), cc.get(Outcome.SKIPPED),
                cc.get(Outcome.SERVER_ERROR), cc.get(Outcome.FAIL));

        for (Outcome o : new Outcome[]{Outcome.FAIL, Outcome.SERVER_ERROR, Outcome.SKIPPED}) {
            List<TestRecord> list = tests.stream().filter(t -> t.outcome == o).toList();
            if (list.isEmpty()) {
                continue;
            }
            System.out.println("--------------------------------------------------------------");
            System.out.printf("  %s (%d)%n", o, list.size());
            for (TestRecord t : list) {
                System.out.printf("    %s > %s%n", simpleName(t.testClass), t.test);
                if (t.reason != null) {
                    System.out.printf("           - %s%n", firstLine(t.reason));
                }
            }
        }

        if (!records.isEmpty()) {
            Map<String, List<ApiCallRecord>> byDomain = new LinkedHashMap<>();
            for (ApiCallRecord r : records) {
                byDomain.computeIfAbsent(r.domain, k -> new ArrayList<>()).add(r);
            }
            System.out.println("--------------------------------------------------------------");
            for (Map.Entry<String, List<ApiCallRecord>> entry : byDomain.entrySet()) {
                List<ApiCallRecord> domainRecords = entry.getValue();
                double domTotal = domainRecords.stream().mapToDouble(r -> r.latencyMs).sum();
                System.out.printf("%n  [%s] %d calls (%.0fms total)%n", entry.getKey(), domainRecords.size(), domTotal);
                for (ApiCallRecord r : domainRecords) {
                    String uuidStr = r.uuid != null ? r.uuid.substring(0, Math.min(8, r.uuid.length())) + "..." : "-";
                    System.out.printf("    [%-12s] %-6s %-30s %-12s %4s  %6.0fms%n",
                            r.outcome, r.httpMethod, r.operation, uuidStr, statusText(r.statusCode), r.latencyMs);
                    if (r.outcome != Outcome.PASS && r.errorMessage != null) {
                        System.out.printf("           - %s%n", firstLine(r.errorMessage));
                    }
                }
            }
            System.out.println("\n--------------------------------------------------------------");
            System.out.println("  SLOWEST API CALLS (top 5):");
            records.stream()
                    .sorted(Comparator.comparingDouble((ApiCallRecord r) -> r.latencyMs).reversed())
                    .limit(5)
                    .forEach(r -> System.out.printf("    %6.0fms  %s %s [%s]%n",
                            r.latencyMs, r.httpMethod, r.operation, r.domain));
        }
        System.out.println(rule + "\n");
    }

    /**
     * Write the full report as a JSON file.
     */
    public void writeJsonReport(Path outputPath) {
        try {
            Map<String, Object> report = new LinkedHashMap<>();
            report.put("suiteStart", suiteStart.toString());
            report.put("suiteEnd", LocalDateTime.now().toString());
            report.put("testMode", System.getProperty("testMode", "readonly"));
            report.put("testCounts", testCounts());
            report.put("callCounts", callCounts());
            report.put("totalCalls", records.size());
            report.put("totalLatencyMs", records.stream().mapToDouble(r -> r.latencyMs).sum());
            report.put("tests", tests);
            report.put("calls", records);

            Files.createDirectories(outputPath.toAbsolutePath().getParent());
            String json = Constants.mapper().writerWithDefaultPrettyPrinter().writeValueAsString(report);
            Files.writeString(outputPath, json);

            System.out.println("  JSON report written to: " + outputPath.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("Failed to write JSON report: " + e.getMessage());
        }
    }

    /**
     * Write a simple HTML report (all values HTML-escaped).
     */
    public void writeHtmlReport(Path outputPath) {
        try {
            Map<Outcome, Long> tc = testCounts();
            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'>\n");
            html.append("<title>Equinix SDK Integration Test Report</title>\n");
            html.append("<style>\n");
            html.append("body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; margin: 2rem; background: #f8f9fa; }\n");
            html.append(".summary { display: flex; gap: 1rem; margin: 1rem 0; }\n");
            html.append(".card { background: white; padding: 1rem 1.5rem; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }\n");
            html.append(".card h3 { margin: 0; font-size: 2rem; }\n");
            html.append("table { width: 100%; border-collapse: collapse; background: white; margin-top: 1rem; }\n");
            html.append("th { background: #1a1a2e; color: white; padding: 0.5rem; text-align: left; }\n");
            html.append("td { padding: 0.4rem 0.6rem; border-bottom: 1px solid #eee; vertical-align: top; }\n");
            html.append(".PASS { color: #27ae60; font-weight: bold; } .FAIL { color: #e74c3c; font-weight: bold; }\n");
            html.append(".SKIPPED { color: #7f8c8d; font-weight: bold; } .SERVER_ERROR { color: #e67e22; font-weight: bold; }\n");
            html.append("pre { margin: 0; white-space: pre-wrap; font-size: 0.85em; }\n");
            html.append("</style></head><body>\n");
            html.append("<h1>Equinix Java SDK - Live Integration Report</h1>\n");
            html.append("<p>Mode: ").append(esc(System.getProperty("testMode", "readonly")))
                    .append(" · Generated: ").append(esc(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))))
                    .append("</p>\n");

            html.append("<div class='summary'>\n");
            html.append(card(tests.size(), "Tests"));
            for (Outcome o : Outcome.values()) {
                html.append(card(tc.get(o), o.name()));
            }
            html.append(card(records.size(), "API calls"));
            html.append("</div>\n");

            html.append("<h2>Tests</h2>\n<table>\n<tr><th>Outcome</th><th>Class</th><th>Test</th><th>Reason</th></tr>\n");
            for (TestRecord t : tests) {
                html.append("<tr><td class='").append(t.outcome).append("'>").append(t.outcome).append("</td><td>")
                        .append(esc(simpleName(t.testClass))).append("</td><td>").append(esc(t.test))
                        .append("</td><td><pre>").append(esc(t.reason)).append("</pre></td></tr>\n");
            }
            html.append("</table>\n");

            html.append("<h2>API calls</h2>\n<table>\n<tr><th>Outcome</th><th>Domain</th><th>Operation</th><th>Resource</th><th>Method</th><th>Status</th><th>UUID</th><th>Latency</th><th>Message</th></tr>\n");
            for (ApiCallRecord r : records) {
                html.append("<tr><td class='").append(r.outcome).append("'>").append(r.outcome).append("</td><td>")
                        .append(esc(r.domain)).append("</td><td>").append(esc(r.operation)).append("</td><td>")
                        .append(esc(r.resourceType)).append("</td><td>").append(esc(r.httpMethod)).append("</td><td>")
                        .append(statusText(r.statusCode)).append("</td><td>").append(esc(r.uuid)).append("</td><td>")
                        .append(String.format("%.0fms", r.latencyMs)).append("</td><td><pre>")
                        .append(esc(r.errorMessage)).append("</pre></td></tr>\n");
            }
            html.append("</table>\n</body></html>");

            Files.createDirectories(outputPath.toAbsolutePath().getParent());
            Files.writeString(outputPath, html.toString());
            System.out.println("  HTML report written to: " + outputPath.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("Failed to write HTML report: " + e.getMessage());
        }
    }

    private static String card(long value, String label) {
        return "<div class='card'><h3>" + value + "</h3><p>" + esc(label) + "</p></div>\n";
    }

    static String esc(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            switch (c) {
                case '<' -> out.append("&lt;");
                case '>' -> out.append("&gt;");
                case '&' -> out.append("&amp;");
                case '"' -> out.append("&quot;");
                case '\'' -> out.append("&#39;");
                default -> out.append(c);
            }
        }
        return out.toString();
    }

    private static String statusText(Integer status) {
        return status == null ? "2xx" : (status == 0 ? "-" : String.valueOf(status));
    }

    private static String simpleName(String className) {
        if (className == null) {
            return "";
        }
        int dot = className.lastIndexOf('.');
        return dot < 0 ? className : className.substring(dot + 1);
    }

    private static String firstLine(String s) {
        String trimmed = s.strip();
        int nl = trimmed.indexOf('\n');
        return nl < 0 ? trimmed : trimmed.substring(0, nl) + " ...";
    }

    // Record classes

    /** One API call made through the timed helpers. */
    public static class ApiCallRecord {
        public String timestamp;
        public String domain;
        public String operation;
        public String resourceType;
        public String httpMethod;
        /** HTTP status when known; {@code null} means the call returned normally (a 2xx). */
        public Integer statusCode;
        public String uuid;
        public Outcome outcome;
        public boolean success;
        public double latencyMs;
        public String errorMessage;

        // Internal, not serialized
        transient long startNanos;
    }

    /** The verdict of one test (or a skipped container). */
    public static class TestRecord {
        public String timestamp;
        public String testClass;
        public String test;
        public Outcome outcome;
        public String reason;
    }
}
