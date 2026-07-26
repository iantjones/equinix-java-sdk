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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Singleton that collects structured results during integration test runs.
 * Produces console summary, JSON report, and optional HTML report.
 */
public final class IntegrationTestReport {

    private static final IntegrationTestReport INSTANCE = new IntegrationTestReport();

    private final List<ApiCallRecord> records = new CopyOnWriteArrayList<>();
    private final LocalDateTime suiteStart = LocalDateTime.now();

    private IntegrationTestReport() {}

    public static IntegrationTestReport getInstance() {
        return INSTANCE;
    }

    // ── Recording ──────────────────────────────────────────────────────

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

    public void completeCall(ApiCallRecord record, int statusCode, String uuid, boolean success, String errorMessage) {
        record.latencyMs = (System.nanoTime() - record.startNanos) / 1_000_000.0;
        record.statusCode = statusCode;
        record.uuid = uuid;
        record.success = success;
        record.errorMessage = errorMessage;
        records.add(record);
    }

    /**
     * Convenience: record a successful call after the fact.
     */
    public void recordSuccess(String domain, String operation, String resourceType,
                              String httpMethod, int statusCode, String uuid, long latencyMs) {
        ApiCallRecord record = new ApiCallRecord();
        record.timestamp = Instant.now().toString();
        record.domain = domain;
        record.operation = operation;
        record.resourceType = resourceType;
        record.httpMethod = httpMethod;
        record.statusCode = statusCode;
        record.uuid = uuid;
        record.success = true;
        record.latencyMs = latencyMs;
        records.add(record);
    }

    /**
     * Convenience: record a failed call after the fact.
     */
    public void recordFailure(String domain, String operation, String resourceType,
                              String httpMethod, int statusCode, String uuid, long latencyMs, String error) {
        ApiCallRecord record = new ApiCallRecord();
        record.timestamp = Instant.now().toString();
        record.domain = domain;
        record.operation = operation;
        record.resourceType = resourceType;
        record.httpMethod = httpMethod;
        record.statusCode = statusCode;
        record.uuid = uuid;
        record.success = false;
        record.latencyMs = latencyMs;
        record.errorMessage = error;
        records.add(record);
    }

    // ── Reporting ──────────────────────────────────────────────────────

    public List<ApiCallRecord> getRecords() {
        return new ArrayList<>(records);
    }

    public void clear() {
        records.clear();
    }

    /**
     * Print a console summary of all recorded API calls.
     */
    public void printConsoleSummary() {
        if (records.isEmpty()) {
            System.out.println("\n══════════════════════════════════════════════════════════════");
            System.out.println("  INTEGRATION TEST REPORT — No API calls recorded");
            System.out.println("══════════════════════════════════════════════════════════════\n");
            return;
        }

        long passed = records.stream().filter(r -> r.success).count();
        long failed = records.size() - passed;
        double totalLatency = records.stream().mapToDouble(r -> r.latencyMs).sum();
        double avgLatency = totalLatency / records.size();

        System.out.println("\n══════════════════════════════════════════════════════════════");
        System.out.println("  EQUINIX JAVA SDK — INTEGRATION TEST REPORT");
        System.out.println("══════════════════════════════════════════════════════════════");
        System.out.printf("  Suite started: %s%n", suiteStart.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        System.out.printf("  Total API calls: %d  |  Passed: %d  |  Failed: %d%n", records.size(), passed, failed);
        System.out.printf("  Total latency: %.0f ms  |  Avg: %.0f ms%n", totalLatency, avgLatency);
        System.out.println("──────────────────────────────────────────────────────────────");

        // Group by domain
        Map<String, List<ApiCallRecord>> byDomain = new LinkedHashMap<>();
        for (ApiCallRecord r : records) {
            byDomain.computeIfAbsent(r.domain, k -> new ArrayList<>()).add(r);
        }

        for (Map.Entry<String, List<ApiCallRecord>> entry : byDomain.entrySet()) {
            String domain = entry.getKey();
            List<ApiCallRecord> domainRecords = entry.getValue();
            long domPassed = domainRecords.stream().filter(r -> r.success).count();
            long domFailed = domainRecords.size() - domPassed;
            double domTotal = domainRecords.stream().mapToDouble(r -> r.latencyMs).sum();

            System.out.printf("%n  [%s] %d calls (pass=%d, fail=%d, %.0fms total)%n",
                    domain, domainRecords.size(), domPassed, domFailed, domTotal);

            for (ApiCallRecord r : domainRecords) {
                String status = r.success ? "OK" : "FAIL";
                String uuidStr = r.uuid != null ? r.uuid.substring(0, Math.min(8, r.uuid.length())) + "..." : "-";
                System.out.printf("    [%s] %-6s %-30s %-12s %3d  %6.0fms%n",
                        status, r.httpMethod, r.operation, uuidStr, r.statusCode, r.latencyMs);
                if (!r.success && r.errorMessage != null) {
                    System.out.printf("           └─ %s%n", r.errorMessage);
                }
            }
        }

        // Top 5 slowest calls
        System.out.println("\n──────────────────────────────────────────────────────────────");
        System.out.println("  SLOWEST API CALLS (top 5):");
        records.stream()
                .sorted(Comparator.comparingDouble((ApiCallRecord r) -> r.latencyMs).reversed())
                .limit(5)
                .forEach(r -> System.out.printf("    %6.0fms  %s %s [%s]%n",
                        r.latencyMs, r.httpMethod, r.operation, r.domain));

        System.out.println("══════════════════════════════════════════════════════════════\n");
    }

    /**
     * Write the full report as a JSON file.
     */
    public void writeJsonReport(Path outputPath) {
        try {
            Map<String, Object> report = new LinkedHashMap<>();
            report.put("suiteStart", suiteStart.toString());
            report.put("suiteEnd", LocalDateTime.now().toString());
            report.put("totalCalls", records.size());
            report.put("passedCalls", records.stream().filter(r -> r.success).count());
            report.put("failedCalls", records.stream().filter(r -> !r.success).count());
            report.put("totalLatencyMs", records.stream().mapToDouble(r -> r.latencyMs).sum());
            report.put("calls", records);

            Files.createDirectories(outputPath.getParent());
            String json = Constants.mapper().writerWithDefaultPrettyPrinter().writeValueAsString(report);
            Files.writeString(outputPath, json);

            System.out.println("  JSON report written to: " + outputPath.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("Failed to write JSON report: " + e.getMessage());
        }
    }

    /**
     * Write a simple HTML report.
     */
    public void writeHtmlReport(Path outputPath) {
        try {
            long passed = records.stream().filter(r -> r.success).count();
            long failed = records.size() - passed;
            double totalLatency = records.stream().mapToDouble(r -> r.latencyMs).sum();

            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'>\n");
            html.append("<title>Equinix SDK Integration Test Report</title>\n");
            html.append("<style>\n");
            html.append("body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; margin: 2rem; background: #f8f9fa; }\n");
            html.append("h1 { color: #1a1a2e; border-bottom: 3px solid #e94560; padding-bottom: 0.5rem; }\n");
            html.append(".summary { display: flex; gap: 1rem; margin: 1rem 0; }\n");
            html.append(".card { background: white; padding: 1rem 1.5rem; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }\n");
            html.append(".card.pass { border-left: 4px solid #27ae60; }\n");
            html.append(".card.fail { border-left: 4px solid #e74c3c; }\n");
            html.append(".card.info { border-left: 4px solid #3498db; }\n");
            html.append(".card h3 { margin: 0; font-size: 2rem; }\n");
            html.append(".card p { margin: 0.25rem 0 0; color: #666; }\n");
            html.append("table { width: 100%; border-collapse: collapse; background: white; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 4px rgba(0,0,0,0.1); margin-top: 1rem; }\n");
            html.append("th { background: #1a1a2e; color: white; padding: 0.75rem; text-align: left; }\n");
            html.append("td { padding: 0.5rem 0.75rem; border-bottom: 1px solid #eee; }\n");
            html.append("tr:hover { background: #f0f4ff; }\n");
            html.append(".ok { color: #27ae60; font-weight: bold; }\n");
            html.append(".fail-status { color: #e74c3c; font-weight: bold; }\n");
            html.append("</style></head><body>\n");
            html.append("<h1>Equinix Java SDK — Integration Test Report</h1>\n");
            html.append(String.format("<p>Generated: %s</p>\n",
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));

            // Summary cards
            html.append("<div class='summary'>\n");
            html.append(String.format("<div class='card info'><h3>%d</h3><p>Total API Calls</p></div>\n", records.size()));
            html.append(String.format("<div class='card pass'><h3>%d</h3><p>Passed</p></div>\n", passed));
            html.append(String.format("<div class='card fail'><h3>%d</h3><p>Failed</p></div>\n", failed));
            html.append(String.format("<div class='card info'><h3>%.0fms</h3><p>Total Latency</p></div>\n", totalLatency));
            html.append("</div>\n");

            // Detail table
            html.append("<table>\n<tr><th>Status</th><th>Domain</th><th>Operation</th><th>Resource</th><th>Method</th><th>Code</th><th>UUID</th><th>Latency</th><th>Error</th></tr>\n");
            for (ApiCallRecord r : records) {
                String statusCls = r.success ? "ok" : "fail-status";
                String statusTxt = r.success ? "OK" : "FAIL";
                String uuid = r.uuid != null ? r.uuid : "-";
                String error = r.errorMessage != null ? r.errorMessage : "";
                html.append(String.format("<tr><td class='%s'>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%d</td><td style='font-size:0.8em'>%s</td><td>%.0fms</td><td style='color:#e74c3c;font-size:0.85em'>%s</td></tr>\n",
                        statusCls, statusTxt, r.domain, r.operation, r.resourceType, r.httpMethod, r.statusCode, uuid, r.latencyMs, error));
            }
            html.append("</table>\n</body></html>");

            Files.createDirectories(outputPath.getParent());
            Files.writeString(outputPath, html.toString());
            System.out.println("  HTML report written to: " + outputPath.toAbsolutePath());

        } catch (IOException e) {
            System.err.println("Failed to write HTML report: " + e.getMessage());
        }
    }

    // ── Record class ───────────────────────────────────────────────────

    public static class ApiCallRecord {
        public String timestamp;
        public String domain;
        public String operation;
        public String resourceType;
        public String httpMethod;
        public int statusCode;
        public String uuid;
        public boolean success;
        public double latencyMs;
        public String errorMessage;

        // Internal, not serialized
        transient long startNanos;
    }
}
