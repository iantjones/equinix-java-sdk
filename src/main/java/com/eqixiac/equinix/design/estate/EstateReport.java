/*
 * Copyright 2021 Ian Jones. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.eqixiac.equinix.design.estate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * The result of an estate analysis: findings in priority order, totals, the addressable saving per
 * currency, coverage gaps, and what was not analysed.
 *
 * <p>Money is never added across currencies. The addressable saving counts only priced positive
 * savings; when several findings save money on the same resource (for example a smaller tier and a
 * term), only the largest is counted, because they do not simply add. Unpriced findings are listed
 * separately with their reasons. Priority is severity weight × confidence weight × (1 + log10(1 +
 * monthly saving)), ties broken by severity, category and id, so the order is deterministic.</p>
 *
 * @param scope                    the scope analysed
 * @param inventoryAsOf            when the inventory was taken
 * @param inventorySource          where the inventory came from
 * @param generatedAt              when the report was generated
 * @param counts                   resource type to count
 * @param utilization              what utilisation data was available
 * @param findings                 every finding, in priority order
 * @param categoryTotals           per category: count, priced monthly saving per currency, unpriced count
 * @param addressableMonthlySaving currency to the total addressable monthly saving
 * @param unpriced                 the findings whose financial effect could not be priced, with reasons
 * @param actions                  the prioritised action list (one per finding)
 * @param coverageGaps             what could not be read
 * @param notAnalysed              what was not analysed and why
 * @param warnings                 loud warnings (truncation, rule failures)
 * @param rulesRun                 the rules that ran
 * @param collection               how the inventory was collected
 * @param reportingCurrency        the currency every priced figure was restated in ({@code null} on reports written
 *                                 before reporting currencies existed)
 * @param currencyNotes            what was converted, at which rates, and any rate source that fell back
 */
public record EstateReport(EstateScope scope, Instant inventoryAsOf, String inventorySource, Instant generatedAt,
                           Map<String, Integer> counts, UtilizationSummary utilization, List<EstateFinding> findings,
                           List<CategoryTotal> categoryTotals, Map<String, SavingTotal> addressableMonthlySaving,
                           List<UnpricedItem> unpriced, List<ActionItem> actions,
                           List<EstateInventory.CoverageGap> coverageGaps, List<String> notAnalysed,
                           List<String> warnings, List<String> rulesRun, EstateInventory.CollectionStats collection,
                           String reportingCurrency, List<String> currencyNotes) {

    /** Copies the collections. */
    public EstateReport {
        counts = counts == null ? Map.of() : java.util.Collections.unmodifiableMap(new LinkedHashMap<>(counts));
        findings = findings == null ? List.of() : List.copyOf(findings);
        categoryTotals = categoryTotals == null ? List.of() : List.copyOf(categoryTotals);
        addressableMonthlySaving = addressableMonthlySaving == null ? Map.of()
                : java.util.Collections.unmodifiableMap(new TreeMap<>(addressableMonthlySaving));
        unpriced = unpriced == null ? List.of() : List.copyOf(unpriced);
        actions = actions == null ? List.of() : List.copyOf(actions);
        coverageGaps = coverageGaps == null ? List.of() : List.copyOf(coverageGaps);
        notAnalysed = notAnalysed == null ? List.of() : List.copyOf(notAnalysed);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
        rulesRun = rulesRun == null ? List.of() : List.copyOf(rulesRun);
        currencyNotes = currencyNotes == null ? List.of() : List.copyOf(currencyNotes);
    }

    /**
     * Assembles a report from findings: orders them, totals them and builds the action list.
     *
     * @param inventory   the inventory analysed
     * @param utilization the utilisation used
     * @param findings    the findings, in any order
     * @param notAnalysed what was not analysed and why
     * @param warnings    warnings to surface
     * @param rulesRun    the rules that ran
     * @param generatedAt the generation time
     * @return the report
     */
    public static EstateReport assemble(EstateInventory inventory, UtilizationSnapshot utilization,
                                        List<EstateFinding> findings, List<String> notAnalysed, List<String> warnings,
                                        List<String> rulesRun, Instant generatedAt) {
        return assemble(inventory, utilization, findings, notAnalysed, warnings, rulesRun, generatedAt, null,
                List.of());
    }

    /**
     * As {@link #assemble(EstateInventory, UtilizationSnapshot, List, List, List, List, Instant)}, stating the
     * reporting currency and what was converted into it.
     *
     * @param inventory         the inventory analysed
     * @param utilization       the utilisation used
     * @param findings          the findings, in any order
     * @param notAnalysed       what was not analysed and why
     * @param warnings          warnings to surface
     * @param rulesRun          the rules that ran
     * @param generatedAt       the generation time
     * @param reportingCurrency the currency priced figures were restated in, or {@code null}
     * @param currencyNotes     conversion advisories and rate-source fallbacks
     * @return the report
     */
    public static EstateReport assemble(EstateInventory inventory, UtilizationSnapshot utilization,
                                        List<EstateFinding> findings, List<String> notAnalysed, List<String> warnings,
                                        List<String> rulesRun, Instant generatedAt, String reportingCurrency,
                                        List<String> currencyNotes) {
        List<EstateFinding> ordered = new ArrayList<>(findings);
        ordered.sort(Comparator.comparingDouble(EstateReport::priority).reversed()
                .thenComparing(f -> f.severity().ordinal(), Comparator.reverseOrder())
                .thenComparing(f -> f.category().ordinal())
                .thenComparing(EstateFinding::id));

        Map<FindingCategory, int[]> counts = new EnumMap<>(FindingCategory.class);
        Map<FindingCategory, Map<String, BigDecimal>> money = new EnumMap<>(FindingCategory.class);
        Map<FindingCategory, int[]> unpricedCounts = new EnumMap<>(FindingCategory.class);
        List<UnpricedItem> unpriced = new ArrayList<>();
        List<ActionItem> actions = new ArrayList<>();
        int rank = 1;
        for (EstateFinding f : ordered) {
            counts.computeIfAbsent(f.category(), k -> new int[1])[0]++;
            FinancialImpact i = f.impact();
            if (i.isPriced()) {
                money.computeIfAbsent(f.category(), k -> new TreeMap<>()).merge(i.currency(), i.monthlySaving(),
                        BigDecimal::add);
            } else if (i.kind() == FinancialImpact.Kind.UNPRICED) {
                unpricedCounts.computeIfAbsent(f.category(), k -> new int[1])[0]++;
                unpriced.add(new UnpricedItem(f.id(), f.category(), i.reason()));
            }
            actions.add(new ActionItem(rank++, f.id(), f.category(), f.severity(), f.confidence(), f.action(),
                    i.describe(), round(priority(f))));
        }
        List<CategoryTotal> totals = new ArrayList<>();
        for (FindingCategory c : FindingCategory.values()) {
            if (counts.containsKey(c)) {
                totals.add(new CategoryTotal(c, counts.get(c)[0], money.getOrDefault(c, Map.of()),
                        unpricedCounts.containsKey(c) ? unpricedCounts.get(c)[0] : 0));
            }
        }

        List<EstateInventory.CoverageGap> gaps = new ArrayList<>(inventory.coverageGaps());
        gaps.addAll(utilization.coverageGaps());
        List<String> allWarnings = new ArrayList<>(inventory.stats().warnings());
        allWarnings.addAll(warnings);
        UtilizationSummary us = new UtilizationSummary(utilization.windowStart() != null, utilization.windowStart(),
                utilization.windowEnd(), utilization.stats().size(), utilization.withData(), utilization.source());
        return new EstateReport(inventory.scope(), inventory.asOf(), inventory.source(), generatedAt,
                inventory.counts(), us, ordered, totals, addressable(ordered), unpriced, actions, gaps, notAnalysed,
                allWarnings, rulesRun, inventory.stats(), reportingCurrency, currencyNotes);
    }

    /**
     * The priority of a finding (higher first).
     *
     * @param f the finding
     * @return severity weight × confidence weight × (1 + log10(1 + monthly saving))
     */
    public static double priority(EstateFinding f) {
        double money = f.impact().isSaving() ? Math.log10(1 + f.impact().monthlySaving().doubleValue()) : 0;
        return f.severity().weight() * f.confidence().weight() * (1 + money);
    }

    private static Map<String, SavingTotal> addressable(List<EstateFinding> findings) {
        // Per primary resource, keep only the largest saving: overlapping recommendations do not add up.
        Map<String, EstateFinding> best = new TreeMap<>();
        for (EstateFinding f : findings) {
            if (!f.impact().isSaving()) {
                continue;
            }
            String key = f.impact().currency() + "|" + (f.resources().isEmpty() ? f.id() : f.resources().get(0).id());
            EstateFinding cur = best.get(key);
            if (cur == null || f.impact().monthlySaving().compareTo(cur.impact().monthlySaving()) > 0) {
                best.put(key, f);
            }
        }
        Map<String, SavingTotal> out = new HashMap<>();
        for (EstateFinding f : best.values()) {
            FinancialImpact i = f.impact();
            out.merge(i.currency(), new SavingTotal(i.monthlySaving(), i.monthlySavingLow(), i.monthlySavingHigh(), 1),
                    (a, b) -> new SavingTotal(a.monthly().add(b.monthly()), a.low().add(b.low()),
                            a.high().add(b.high()), a.findings() + b.findings()));
        }
        return out;
    }

    private static double round(double v) {
        return BigDecimal.valueOf(v).setScale(4, RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * The findings of one category.
     *
     * @param category the category
     * @return the findings, in priority order
     */
    public List<EstateFinding> findings(FindingCategory category) {
        return findings.stream().filter(f -> f.category() == category).toList();
    }

    /**
     * Serializes the report to JSON.
     *
     * @return the JSON document
     */
    public String toJson() {
        return EstateJson.write(this);
    }

    /**
     * Reads a report written by {@link #toJson()}.
     *
     * @param json the JSON document
     * @return the report
     */
    public static EstateReport fromJson(String json) {
        return EstateJson.read(json, EstateReport.class);
    }

    /**
     * Renders the report as Markdown.
     *
     * @return the Markdown text
     */
    public String toMarkdown() {
        StringBuilder b = new StringBuilder();
        b.append("# Equinix estate analysis\n\n");
        b.append("- Scope: ").append(scope.describe()).append('\n');
        b.append("- Inventory as of: ").append(inventoryAsOf).append(" (").append(inventorySource).append(")\n");
        b.append("- Generated: ").append(generatedAt).append('\n');
        b.append("- Resources: ");
        counts.forEach((k, v) -> b.append(k).append(' ').append(v).append("; "));
        b.setLength(b.length() - 2);
        b.append('\n');
        if (utilization.collected()) {
            b.append("- Utilisation: ").append(utilization.withData()).append(" of ").append(utilization.probed())
                    .append(" resources had data, window ").append(utilization.windowStart()).append(" .. ")
                    .append(utilization.windowEnd()).append(" (").append(utilization.source()).append(")\n");
        } else {
            b.append("- Utilisation: not collected\n");
        }
        if (collection != null && collection.requests() > 0) {
            b.append("- Collection: ").append(collection.requests()).append(" requests in ")
                    .append(collection.totalMillis()).append(" ms\n");
        }
        if (!warnings.isEmpty()) {
            b.append("\n## Warnings\n\n");
            warnings.forEach(w -> b.append("- **").append(w).append("**\n"));
        }
        b.append("\n## Summary\n\n");
        if (reportingCurrency != null) {
            b.append("- Reporting currency: ").append(reportingCurrency).append('\n');
        }
        currencyNotes.forEach(n -> b.append("- Currency: ").append(n).append('\n'));
        if (addressableMonthlySaving.isEmpty()) {
            b.append("No priced saving was found.\n");
        } else {
            addressableMonthlySaving.forEach((ccy, t) -> b.append("- Addressable saving: **").append(ccy).append(' ')
                    .append(t.monthly().toPlainString()).append("/month** (range ").append(t.low().toPlainString())
                    .append(" .. ").append(t.high().toPlainString()).append(", ").append(t.findings())
                    .append(" finding(s); overlapping savings on one resource counted once)\n"));
        }
        if (!unpriced.isEmpty()) {
            b.append("- Unpriced findings: ").append(unpriced.size()).append(" (listed below; not counted as zero)\n");
        }
        b.append("\n| Category | Findings | Priced monthly effect | Unpriced |\n|---|---:|---|---:|\n");
        for (CategoryTotal t : categoryTotals) {
            StringBuilder m = new StringBuilder();
            t.monthlySavingByCurrency().forEach((c, v) -> m.append(m.isEmpty() ? "" : ", ").append(c).append(' ')
                    .append(v.toPlainString()));
            b.append("| ").append(t.category()).append(" | ").append(t.count()).append(" | ")
                    .append(m.isEmpty() ? "-" : m).append(" | ").append(t.unpriced()).append(" |\n");
        }
        b.append("\nPositive amounts are savings; negative amounts are added cost.\n");
        b.append("\n## Prioritised actions\n\n");
        if (actions.isEmpty()) {
            b.append("No findings.\n");
        }
        for (ActionItem a : actions) {
            b.append(a.rank()).append(". [").append(a.severity()).append(", ").append(a.confidence())
                    .append(" confidence] ").append(a.action()).append(" - ").append(a.impact()).append(" (`")
                    .append(a.findingId()).append("`)\n");
        }
        b.append("\n## Findings\n");
        for (EstateFinding f : findings) {
            b.append("\n### ").append(f.title()).append("\n\n");
            b.append("- Id: `").append(f.id()).append("` (rule ").append(f.rule()).append(")\n");
            b.append("- Category / severity / confidence: ").append(f.category()).append(" / ").append(f.severity())
                    .append(" / ").append(f.confidence()).append('\n');
            b.append("- Action: ").append(f.action()).append('\n');
            b.append("- Impact: ").append(f.impact().describe()).append('\n');
            int shown = Math.min(10, f.resources().size());
            b.append("- Resources (").append(f.resources().size()).append("): ");
            for (int i = 0; i < shown; i++) {
                EstateFinding.ResourceRef r = f.resources().get(i);
                b.append(i == 0 ? "" : ", ").append(r.kind()).append(' ').append(r.name() == null ? "" : r.name() + " ")
                        .append('`').append(r.id()).append('`');
            }
            b.append(f.resources().size() > shown ? ", ..." : "").append('\n');
            if (!f.evidence().isEmpty()) {
                b.append("- Evidence:\n");
                for (EstateFinding.Evidence e : f.evidence()) {
                    b.append("  - ").append(e.label()).append(": ").append(e.value())
                            .append(e.unit() == null ? "" : " " + e.unit()).append(" (").append(e.source())
                            .append(e.window() == null ? "" : ", " + e.window()).append(")\n");
                }
            }
            if (!f.assumptions().isEmpty()) {
                b.append("- Assumptions: ").append(String.join("; ", f.assumptions())).append('\n');
            }
        }
        if (!unpriced.isEmpty()) {
            b.append("\n## Unpriced items\n\n");
            unpriced.forEach(u -> b.append("- `").append(u.findingId()).append("`: ").append(u.reason()).append('\n'));
        }
        b.append("\n## What was not analysed, and why\n\n");
        if (notAnalysed.isEmpty() && coverageGaps.isEmpty()) {
            b.append("Everything in scope was analysed.\n");
        }
        notAnalysed.forEach(n -> b.append("- ").append(n).append('\n'));
        for (EstateInventory.CoverageGap g : coverageGaps) {
            b.append("- ").append(g.resourceType()).append(": ").append(g.reason())
                    .append(g.httpStatus() == null ? "" : " (HTTP " + g.httpStatus() + ")").append(" - ")
                    .append(g.detail()).append('\n');
        }
        b.append("\nRules run: ").append(String.join(", ", rulesRun)).append('\n');
        return b.toString();
    }

    /**
     * What utilisation data the analysis had.
     *
     * @param collected   whether the probe ran
     * @param windowStart the window start, or {@code null}
     * @param windowEnd   the window end, or {@code null}
     * @param probed      resources probed
     * @param withData    resources with data
     * @param source      the data source
     */
    public record UtilizationSummary(boolean collected, Instant windowStart, Instant windowEnd, int probed,
                                     int withData, String source) {
    }

    /**
     * Totals for one category.
     *
     * @param category                the category
     * @param count                   the number of findings
     * @param monthlySavingByCurrency the sum of priced monthly effects per currency (positive = saving)
     * @param unpriced                the number of unpriced findings
     */
    public record CategoryTotal(FindingCategory category, int count, Map<String, BigDecimal> monthlySavingByCurrency,
                                int unpriced) {

        /** Copies the map. */
        public CategoryTotal {
            monthlySavingByCurrency = monthlySavingByCurrency == null ? Map.of()
                    : java.util.Collections.unmodifiableMap(new TreeMap<>(monthlySavingByCurrency));
        }
    }

    /**
     * An addressable saving in one currency.
     *
     * @param monthly  the expected monthly saving
     * @param low      the low end
     * @param high     the high end
     * @param findings the number of findings counted
     */
    public record SavingTotal(BigDecimal monthly, BigDecimal low, BigDecimal high, int findings) {
    }

    /**
     * A finding whose financial effect could not be priced.
     *
     * @param findingId the finding id
     * @param category  the category
     * @param reason    why it is unpriced
     */
    public record UnpricedItem(String findingId, FindingCategory category, String reason) {
    }

    /**
     * One entry of the prioritised action list.
     *
     * @param rank       1 for the most important
     * @param findingId  the finding id
     * @param category   the category
     * @param severity   the severity
     * @param confidence the confidence
     * @param action     the recommended action
     * @param impact     the financial impact, as text
     * @param score      the priority score
     */
    public record ActionItem(int rank, String findingId, FindingCategory category, Severity severity,
                             Confidence confidence, String action, String impact, double score) {
    }
}
