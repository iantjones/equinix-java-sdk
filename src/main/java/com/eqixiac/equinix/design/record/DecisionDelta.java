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


package com.eqixiac.equinix.design.record;

import com.eqixiac.equinix.design.record.DesignDecisionRecord.Availability;
import com.eqixiac.equinix.design.record.DesignDecisionRecord.CostItem;
import com.eqixiac.equinix.design.record.DesignDecisionRecord.ObjectiveResult;
import com.eqixiac.equinix.design.record.DesignDecisionRecord.Risk;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * What changed between two {@link DesignDecisionRecord}s, for example the same design re-run a quarter later:
 * the spec, the recommendation and its objective values, the cost, the resilience verdicts and the findings.
 * Produced by {@link DesignDecisionRecord#compare}; Jackson-serializable; {@link #toMarkdown()} for a reader.
 *
 * @param beforeGeneratedAt     when the earlier record was produced
 * @param afterGeneratedAt      when the later record was produced
 * @param specChanged           whether the specs' fingerprints differ
 * @param specChanges           one line per changed spec property ({@code path: before -> after})
 * @param recommendationChanged whether the recommended portfolio differs
 * @param recommendationBefore  the earlier recommended portfolio, or {@code null} when it had none
 * @param recommendationAfter   the later recommended portfolio, or {@code null} when it has none
 * @param objectiveChanges      one line per objective whose value changed
 * @param costBefore            the earlier monthly total, or {@code null}
 * @param costAfter             the later monthly total, or {@code null}
 * @param costCurrencyBefore    the earlier total's currency, or {@code null}
 * @param costCurrencyAfter     the later total's currency, or {@code null}
 * @param costChange            the later total minus the earlier one, or {@code null} when not comparable
 * @param costLineChanges       one line per cost line added, removed or repriced (matched by label)
 * @param resilienceChanges     one line per changed resilience verdict, single point of failure or availability
 * @param risksAdded            findings in the later record only
 * @param risksRemoved          findings in the earlier record only
 * @param sectionChanges        one line per section whose status changed
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DecisionDelta(java.time.Instant beforeGeneratedAt, java.time.Instant afterGeneratedAt,
                            boolean specChanged, List<String> specChanges, boolean recommendationChanged,
                            String recommendationBefore, String recommendationAfter, List<String> objectiveChanges,
                            BigDecimal costBefore, BigDecimal costAfter, String costCurrencyBefore,
                            String costCurrencyAfter, BigDecimal costChange, List<String> costLineChanges,
                            List<String> resilienceChanges, List<String> risksAdded, List<String> risksRemoved,
                            List<String> sectionChanges) {

    /** Copies the lists. */
    public DecisionDelta {
        specChanges = copy(specChanges);
        objectiveChanges = copy(objectiveChanges);
        costLineChanges = copy(costLineChanges);
        resilienceChanges = copy(resilienceChanges);
        risksAdded = copy(risksAdded);
        risksRemoved = copy(risksRemoved);
        sectionChanges = copy(sectionChanges);
    }

    private static List<String> copy(List<String> list) {
        return list == null ? List.of() : List.copyOf(list);
    }

    /** @return {@code true} when nothing the delta tracks changed */
    public boolean isEmpty() {
        return !specChanged && !recommendationChanged && objectiveChanges.isEmpty() && costLineChanges.isEmpty()
                && Objects.equals(costBefore, costAfter) && resilienceChanges.isEmpty() && risksAdded.isEmpty()
                && risksRemoved.isEmpty() && sectionChanges.isEmpty();
    }

    static DecisionDelta between(DesignDecisionRecord before, DesignDecisionRecord after) {
        if (before == null || after == null) {
            throw new IllegalArgumentException("compare needs two records");
        }
        boolean specChanged = !before.specFingerprint().equals(after.specFingerprint());
        List<String> specChanges = new ArrayList<>();
        if (specChanged) {
            diff("", RecordJson.mapper().valueToTree(before.spec()), RecordJson.mapper().valueToTree(after.spec()),
                    specChanges);
        }

        String recBefore = portfolio(before);
        String recAfter = portfolio(after);
        List<String> objectives = new ArrayList<>();
        Map<String, ObjectiveResult> ob = objectives(before);
        Map<String, ObjectiveResult> oa = objectives(after);
        Set<String> ids = new LinkedHashSet<>(ob.keySet());
        ids.addAll(oa.keySet());
        String currency = after.spec().commercial().currencyOrDefault();
        for (String id : ids) {
            ObjectiveResult b = ob.get(id);
            ObjectiveResult a = oa.get(id);
            Double vb = b == null ? null : b.value();
            Double va = a == null ? null : a.value();
            if (!Objects.equals(vb, va)) {
                String unit = a != null ? a.unit() : b.unit();
                objectives.add(id + ": " + (vb == null ? "unknown" : DecisionRecordMarkdown.value(unit, currency, vb))
                        + " -> " + (va == null ? "unknown" : DecisionRecordMarkdown.value(unit, currency, va)));
            }
        }

        BigDecimal cb = null;
        BigDecimal ca = null;
        String ccb = null;
        String cca = null;
        List<String> lines = new ArrayList<>();
        Map<String, CostItem> lb = new LinkedHashMap<>();
        Map<String, CostItem> la = new LinkedHashMap<>();
        if (before.cost() != null && before.cost().isIncluded()) {
            cb = before.cost().content().monthly();
            ccb = before.cost().content().currency();
            before.cost().content().lines().forEach(l -> lb.putIfAbsent(l.label(), l));
        }
        if (after.cost() != null && after.cost().isIncluded()) {
            ca = after.cost().content().monthly();
            cca = after.cost().content().currency();
            after.cost().content().lines().forEach(l -> la.putIfAbsent(l.label(), l));
        }
        BigDecimal change = cb != null && ca != null && Objects.equals(ccb, cca) ? ca.subtract(cb) : null;
        for (Map.Entry<String, CostItem> e : lb.entrySet()) {
            CostItem a = la.get(e.getKey());
            CostItem b = e.getValue();
            if (a == null) {
                lines.add("removed: " + b.label() + " (" + DecisionRecordMarkdown.money(b.currency(), b.monthly()) + ")");
            } else if (a.monthly().compareTo(b.monthly()) != 0 || !a.currency().equals(b.currency())
                    || !Objects.equals(a.source(), b.source())) {
                lines.add("repriced: " + b.label() + " " + DecisionRecordMarkdown.money(b.currency(), b.monthly()) + " ("
                        + b.source() + ") -> " + DecisionRecordMarkdown.money(a.currency(), a.monthly()) + " ("
                        + a.source() + ")");
            }
        }
        for (Map.Entry<String, CostItem> e : la.entrySet()) {
            if (!lb.containsKey(e.getKey())) {
                lines.add("added: " + e.getValue().label() + " ("
                        + DecisionRecordMarkdown.money(e.getValue().currency(), e.getValue().monthly()) + ")");
            }
        }

        List<String> resilience = new ArrayList<>();
        DesignDecisionRecord.Resilience rb = before.resilience() != null && before.resilience().isIncluded()
                ? before.resilience().content() : null;
        DesignDecisionRecord.Resilience ra = after.resilience() != null && after.resilience().isIncluded()
                ? after.resilience().content() : null;
        if (rb != null && ra != null) {
            verdict("N-1", rb.n1(), ra.n1(), resilience);
            verdict("N-2", rb.n2(), ra.n2(), resilience);
            if (rb.singlePoints().size() != ra.singlePoints().size()) {
                resilience.add("single points of failure: " + rb.singlePoints().size() + " -> " + ra.singlePoints().size());
            }
            Map<String, Availability> ab = new LinkedHashMap<>();
            rb.availability().forEach(x -> ab.put(x.workload(), x));
            for (Availability x : ra.availability()) {
                Availability y = ab.get(x.workload());
                if (y != null && Math.abs(y.percent() - x.percent()) > 1e-9) {
                    resilience.add(String.format(Locale.ROOT, "availability of %s: %.4f%% -> %.4f%%", x.workload(),
                            y.percent(), x.percent()));
                }
            }
        } else if ((rb == null) != (ra == null)) {
            resilience.add("resilience analysis " + (rb == null ? "added" : "no longer available"));
        }

        Set<String> riskB = risks(before);
        Set<String> riskA = risks(after);
        List<String> added = new ArrayList<>();
        List<String> removed = new ArrayList<>();
        for (String x : riskA) {
            if (!riskB.contains(x)) {
                added.add(x);
            }
        }
        for (String x : riskB) {
            if (!riskA.contains(x)) {
                removed.add(x);
            }
        }

        List<String> sections = new ArrayList<>();
        Map<String, RecordSection<?>> sb = before.sections();
        Map<String, RecordSection<?>> sa = after.sections();
        for (String name : sb.keySet()) {
            RecordSection<?> x = sb.get(name);
            RecordSection<?> y = sa.get(name);
            RecordSection.Status s1 = x == null ? null : x.status();
            RecordSection.Status s2 = y == null ? null : y.status();
            if (s1 != s2) {
                sections.add(name + ": " + s1 + " -> " + s2 + (y != null && y.reason() != null ? " (" + y.reason() + ")"
                        : ""));
            }
        }
        return new DecisionDelta(before.generatedAt(), after.generatedAt(), specChanged, specChanges,
                !Objects.equals(recBefore, recAfter), recBefore, recAfter, objectives, cb, ca, ccb, cca, change, lines,
                resilience, added, removed, sections);
    }

    /** @return the delta as Markdown */
    public String toMarkdown() {
        StringBuilder md = new StringBuilder("# Decision record comparison\n\n");
        md.append("- Before: ").append(beforeGeneratedAt).append('\n');
        md.append("- After: ").append(afterGeneratedAt).append("\n\n");
        if (isEmpty()) {
            md.append("Nothing changed.\n");
            return md.toString();
        }
        md.append("## Spec\n\n");
        if (!specChanged) {
            md.append("Unchanged (same fingerprint).\n\n");
        } else {
            bullets(md, specChanges.isEmpty() ? List.of("The fingerprint differs.") : specChanges);
        }
        md.append("## Recommendation\n\n");
        md.append(recommendationChanged ? "Changed: `" + name(recommendationBefore) + "` -> `"
                + name(recommendationAfter) + "`.\n\n" : "Unchanged: `" + name(recommendationAfter) + "`.\n\n");
        bullets(md, objectiveChanges);
        md.append("## Cost\n\n");
        md.append("Monthly total: ").append(DecisionRecordMarkdown.money(costCurrencyBefore, costBefore)).append(" -> ")
                .append(DecisionRecordMarkdown.money(costCurrencyAfter, costAfter));
        if (costChange != null) {
            md.append(" (").append(costChange.signum() > 0 ? "+" : "")
                    .append(DecisionRecordMarkdown.money(costCurrencyAfter, costChange)).append(')');
        } else if (costBefore != null && costAfter != null) {
            md.append(" (different currencies, not compared)");
        }
        md.append(".\n\n");
        bullets(md, costLineChanges);
        md.append("## Resilience\n\n");
        bullets(md, resilienceChanges.isEmpty() ? List.of("No change.") : resilienceChanges);
        md.append("## Findings\n\n");
        if (risksAdded.isEmpty() && risksRemoved.isEmpty()) {
            md.append("No change.\n\n");
        }
        for (String x : risksAdded) {
            md.append("- New: ").append(x).append('\n');
        }
        for (String x : risksRemoved) {
            md.append("- Resolved: ").append(x).append('\n');
        }
        if (!risksAdded.isEmpty() || !risksRemoved.isEmpty()) {
            md.append('\n');
        }
        if (!sectionChanges.isEmpty()) {
            md.append("## Sections\n\n");
            bullets(md, sectionChanges);
        }
        return md.toString().replace("\u2014", "-").replace("\u2013", "-");
    }

    private static String name(String portfolio) {
        return portfolio == null ? "none" : portfolio;
    }

    private static void bullets(StringBuilder md, List<String> items) {
        if (items.isEmpty()) {
            return;
        }
        for (String i : items) {
            md.append("- ").append(i).append('\n');
        }
        md.append('\n');
    }

    private static String portfolio(DesignDecisionRecord r) {
        return r.decision() != null && r.decision().isIncluded() ? r.decision().content().portfolio() : null;
    }

    private static Map<String, ObjectiveResult> objectives(DesignDecisionRecord r) {
        Map<String, ObjectiveResult> out = new LinkedHashMap<>();
        if (r.decision() != null && r.decision().isIncluded()) {
            r.decision().content().objectives().forEach(o -> out.put(o.id(), o));
        }
        return out;
    }

    private static Set<String> risks(DesignDecisionRecord r) {
        Set<String> out = new TreeSet<>();
        if (r.risks() != null && r.risks().isIncluded()) {
            for (Risk x : r.risks().content().items()) {
                if (!"record".equals(x.source())) {
                    out.add(x.severity() + " " + x.source() + " " + x.category() + ": " + x.statement());
                }
            }
        }
        return out;
    }

    private static void verdict(String name, DesignDecisionRecord.Verdict b, DesignDecisionRecord.Verdict a,
                                List<String> out) {
        String lb = b == null ? null : b.level();
        String la = a == null ? null : a.level();
        if (!Objects.equals(lb, la)) {
            out.add(name + ": " + lb + " -> " + la + (a == null ? "" : " (" + a.statement() + ")"));
        }
    }

    /** Lists the leaf differences between two JSON trees as {@code path: before -> after}. */
    private static void diff(String path, JsonNode b, JsonNode a, List<String> out) {
        if (Objects.equals(b, a)) {
            return;
        }
        if (b != null && a != null && b.isObject() && a.isObject()) {
            Set<String> names = new TreeSet<>();
            b.fieldNames().forEachRemaining(names::add);
            a.fieldNames().forEachRemaining(names::add);
            for (String n : names) {
                diff(path.isEmpty() ? n : path + "." + n, b.get(n), a.get(n), out);
            }
            return;
        }
        out.add(path + ": " + text(b) + " -> " + text(a));
    }

    private static String text(JsonNode n) {
        if (n == null || n.isNull() || n.isMissingNode()) {
            return "(unset)";
        }
        return n.isTextual() ? n.asText() : n.toString();
    }
}
