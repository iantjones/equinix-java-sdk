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

import com.eqixiac.equinix.design.record.DesignDecisionRecord.Alternative;
import com.eqixiac.equinix.design.record.DesignDecisionRecord.Alternatives;
import com.eqixiac.equinix.design.record.DesignDecisionRecord.Assumption;
import com.eqixiac.equinix.design.record.DesignDecisionRecord.Availability;
import com.eqixiac.equinix.design.record.DesignDecisionRecord.ConstraintEffect;
import com.eqixiac.equinix.design.record.DesignDecisionRecord.Context;
import com.eqixiac.equinix.design.record.DesignDecisionRecord.Cost;
import com.eqixiac.equinix.design.record.DesignDecisionRecord.CostItem;
import com.eqixiac.equinix.design.record.DesignDecisionRecord.Decision;
import com.eqixiac.equinix.design.record.DesignDecisionRecord.FxRate;
import com.eqixiac.equinix.design.record.DesignDecisionRecord.Growth;
import com.eqixiac.equinix.design.record.DesignDecisionRecord.GrowthItem;
import com.eqixiac.equinix.design.record.DesignDecisionRecord.Implementation;
import com.eqixiac.equinix.design.record.DesignDecisionRecord.ObjectiveResult;
import com.eqixiac.equinix.design.record.DesignDecisionRecord.PlanSummary;
import com.eqixiac.equinix.design.record.DesignDecisionRecord.Provenance;
import com.eqixiac.equinix.design.record.DesignDecisionRecord.Resilience;
import com.eqixiac.equinix.design.record.DesignDecisionRecord.Risk;
import com.eqixiac.equinix.design.record.DesignDecisionRecord.RiskLevel;
import com.eqixiac.equinix.design.record.DesignDecisionRecord.Sensitivity;
import com.eqixiac.equinix.design.record.DesignDecisionRecord.Value;
import com.eqixiac.equinix.design.record.DesignDecisionRecord.WeightRange;

import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Renders a {@link DesignDecisionRecord} as an ADR: title, status, date, context, decision, consequences,
 * alternatives, then evidence appendices. Deterministic: the same record gives the same text.
 */
final class DecisionRecordMarkdown {

    private DecisionRecordMarkdown() {
    }

    static String render(DesignDecisionRecord r) {
        StringBuilder md = new StringBuilder();
        md.append("# ").append(r.title()).append("\n\n");
        md.append("- Status: ").append(r.status()).append('\n');
        md.append("- Date: ").append(r.generatedAt() == null ? "unknown"
                : r.generatedAt().atOffset(ZoneOffset.UTC).toLocalDate()).append('\n');
        md.append("- Spec: `").append(r.spec().name()).append("`, fingerprint `").append(r.specFingerprint())
                .append("`\n");
        md.append("- Produced by: equinix-sdk-java ").append(r.sdkVersion()).append(", decision record schema ")
                .append(r.schemaVersion()).append("\n\n");

        context(md, r);
        decision(md, r);
        consequences(md, r);
        alternatives(md, r);

        md.append("## Evidence\n\n");
        sensitivity(md, r.sensitivity());
        cost(md, r.cost());
        resilience(md, r.resilience());
        risks(md, r.risks());
        provenance(md, r.provenance());
        implementation(md, r.implementation());
        return clean(md.toString());
    }

    /** Normalises dashes so the document reads the same everywhere it is pasted. */
    private static String clean(String text) {
        return text.replace("\u2014", "-").replace("\u2013", "-");
    }

    private static boolean unavailable(StringBuilder md, RecordSection<?> section) {
        if (section == null) {
            md.append("_Not recorded._\n\n");
            return true;
        }
        if (section.isIncluded()) {
            return false;
        }
        md.append('_').append(section.status() == RecordSection.Status.SKIPPED ? "Skipped" : "Failed").append(": ")
                .append(sentence(section.reason())).append("_\n\n");
        return true;
    }

    // context

    private static void context(StringBuilder md, DesignDecisionRecord r) {
        md.append("## Context\n\n");
        if (unavailable(md, r.context())) {
            return;
        }
        Context c = r.context().content();
        if (c.description() != null && !c.description().isBlank()) {
            md.append(c.description().trim()).append("\n\n");
        }
        list(md, "Sites", c.sites());
        list(md, "Workloads", c.workloads());
        list(md, "Clouds", c.clouds());
        list(md, "Peer networks", c.peers());
        list(md, "Constraints", c.constraints().isEmpty() ? List.of("None stated") : c.constraints());
        List<String> weights = new ArrayList<>();
        c.weights().forEach((id, w) -> weights.add(id + " " + pct(w)));
        md.append("Objectives: ").append(c.weightProfile() == null ? "explicit weights" : "profile `"
                + c.weightProfile() + "`").append("; weights ").append(String.join(", ", weights)).append(".\n\n");
        list(md, "Commercial terms", c.commercial());
        list(md, "Background", c.background());
        list(md, "Spec warnings", c.specIssues());
    }

    // decision

    private static void decision(StringBuilder md, DesignDecisionRecord r) {
        md.append("## Decision\n\n");
        if (unavailable(md, r.decision())) {
            return;
        }
        Decision d = r.decision().content();
        md.append("Deploy in **").append(String.join(" + ", d.metros())).append("** (portfolio `").append(d.portfolio())
                .append(String.format(Locale.ROOT, "`, weighted score %.1f of 100).%n%n", d.weightedScore()));
        md.append(d.rationale()).append("\n\n");
        String currency = currency(r);
        md.append("| Objective | Value | Score | Weight | Scale |\n|---|---:|---:|---:|---|\n");
        for (ObjectiveResult o : d.objectives()) {
            md.append("| ").append(o.id()).append(" | ")
                    .append(o.value() == null ? "unknown" : value(o.unit(), currency, o.value())).append(" | ")
                    .append(o.score() == null ? "-" : String.format(Locale.ROOT, "%.1f", o.score())).append(" | ")
                    .append(o.weight() == null ? "-" : pct(o.weight())).append(" | ")
                    .append(cell(o.scale() == null ? "-" : o.scale())).append(" |\n");
        }
        md.append('\n');
        list(md, "Placement", d.placements());
        list(md, "Constraints checked", d.constraints());
    }

    // consequences

    private static void consequences(StringBuilder md, DesignDecisionRecord r) {
        md.append("## Consequences\n\n");
        List<String> lines = new ArrayList<>();
        if (r.cost() != null && r.cost().isIncluded()) {
            Cost c = r.cost().content();
            if (c.monthly() != null) {
                lines.add("Cost: " + money(c.currency(), c.monthly()) + " per month (range "
                        + money(c.currency(), c.monthlyLow()) + " to " + money(c.currency(), c.monthlyHigh()) + ")"
                        + (c.oneTime() != null && c.oneTime().signum() != 0 ? " plus " + money(c.currency(), c.oneTime())
                        + " one-time" : "") + (c.complete() ? "." : "; " + c.unpriced().size()
                        + " item(s) unpriced, so this understates the cost."));
            } else {
                lines.add("Cost: no single monthly total (" + c.unpriced().size() + " item(s) unpriced or the lines "
                        + "span currencies).");
            }
        }
        if (r.resilience() != null && r.resilience().isIncluded()) {
            Resilience res = r.resilience().content();
            if (res.n1() != null) {
                lines.add("Resilience: " + res.n1().statement());
            }
            if (!res.singlePoints().isEmpty()) {
                lines.add(res.singlePoints().size() + " single point(s) of failure, the first: "
                        + res.singlePoints().get(0) + ".");
            }
        }
        if (r.sensitivity() != null && r.sensitivity().isIncluded()) {
            Sensitivity s = r.sensitivity().content();
            List<String> holds = new ArrayList<>();
            for (WeightRange w : s.weights()) {
                if (w.low() > 1e-9 || w.high() < 1 - 1e-9) {
                    holds.add(w.objective() + " " + pct(w.low()) + " to " + pct(w.high()));
                }
            }
            if (!holds.isEmpty()) {
                lines.add("The decision holds while each weight stays within: " + String.join("; ", holds)
                        + "; other weights can take any value.");
            } else if (!s.weights().isEmpty()) {
                lines.add("The decision does not depend on the weights: it holds with any single weight from 0% "
                        + "to 100%.");
            }
        }
        if (r.risks() != null && r.risks().isIncluded()) {
            long serious = r.risks().content().items().stream()
                    .filter(x -> x.severity().compareTo(RiskLevel.HIGH) <= 0).count();
            lines.add(serious == 0 ? "No critical or high findings."
                    : serious + " critical or high finding(s); see appendix D.");
        }
        if (r.implementation() != null && r.implementation().isIncluded()
                && r.implementation().content().plan() != null) {
            PlanSummary p = r.implementation().content().plan();
            lines.add("Implementation: " + p.routers() + " Cloud Router(s), " + p.connections() + " provider "
                    + "connection(s), " + p.backboneLinks() + " backbone link(s), " + p.steps() + " plan step(s).");
        }
        if (lines.isEmpty()) {
            md.append("_No consequences could be stated: the decision was not produced._\n\n");
            return;
        }
        for (String l : lines) {
            md.append("- ").append(l).append('\n');
        }
        md.append('\n');
    }

    // alternatives

    private static void alternatives(StringBuilder md, DesignDecisionRecord r) {
        md.append("## Alternatives considered\n\n");
        if (unavailable(md, r.alternatives())) {
            return;
        }
        Alternatives a = r.alternatives().content();
        md.append(String.format(Locale.ROOT, "%,d portfolios examined, %,d feasible, %d on the Pareto frontier%s.%n%n",
                a.evaluated(), a.feasible(), a.frontier().size(), a.exact() ? " (exhaustive search)"
                        : " (the search was narrowed; see the notes)"));
        String currency = currency(r);
        if (!a.frontier().isEmpty()) {
            Set<String> ids = new LinkedHashSet<>();
            for (Alternative alt : a.frontier()) {
                ids.addAll(alt.values().keySet());
            }
            Map<String, String> units = units(r);
            boolean monthly = !ids.contains("cost");
            md.append("| Option | Score |");
            for (String id : ids) {
                md.append(' ').append(id).append(" |");
            }
            md.append(monthly ? " Monthly |" : "").append(" Gains vs recommendation | Gives up vs recommendation |\n"
                    + "|---|---:|");
            for (int i = 0; i < ids.size(); i++) {
                md.append("---:|");
            }
            md.append(monthly ? "---:|" : "").append("---|---|\n");
            for (Alternative alt : a.frontier()) {
                md.append("| ").append(alt.recommended() ? "**" + alt.portfolio() + "** (chosen)" : alt.portfolio())
                        .append(String.format(Locale.ROOT, " | %.1f |", alt.weightedScore()));
                for (String id : ids) {
                    Double v = alt.values().get(id);
                    md.append(' ').append(v == null ? "unknown" : value(units.get(id), currency, v)).append(" |");
                }
                if (monthly) {
                    md.append(' ').append(alt.monthlyCost() == null ? "n/a" : money(alt.currency(), alt.monthlyCost()))
                            .append(" |");
                }
                md.append(' ').append(alt.recommended() ? "-" : cell(join(alt.gains())))
                        .append(" | ").append(alt.recommended() ? "-" : cell(join(alt.tradesAway()))).append(" |\n");
            }
            md.append('\n');
        }
        if (!a.constraints().isEmpty()) {
            md.append("Rejected by constraint:\n\n| Constraint | Binding | Rejected alone | Chosen without it | "
                    + "Explanation |\n|---|---|---:|---|---|\n");
            for (ConstraintEffect c : a.constraints()) {
                md.append("| ").append(c.constraint()).append(" | ").append(c.binding() ? "yes" : "no").append(" | ")
                        .append(c.rejectedAlone()).append(" | ").append(c.withoutIt() == null ? "-" : c.withoutIt())
                        .append(" | ").append(cell(c.explanation())).append(" |\n");
            }
            md.append('\n');
        }
        list(md, "Search notes", a.notes());
    }

    // A. sensitivity

    private static void sensitivity(StringBuilder md, RecordSection<Sensitivity> section) {
        md.append("### A. Sensitivity\n\n");
        if (unavailable(md, section)) {
            return;
        }
        Sensitivity s = section.content();
        if (!s.weights().isEmpty()) {
            md.append("| Objective | Weight now | Holds from | Holds to | Below, chosen | Above, chosen |\n"
                    + "|---|---:|---:|---:|---|---|\n");
            for (WeightRange w : s.weights()) {
                md.append("| ").append(w.objective()).append(" | ").append(pct(w.share())).append(" | ")
                        .append(pct(w.low())).append(" | ").append(pct(w.high())).append(" | ")
                        .append(w.belowTakeover() == null ? "-" : w.belowTakeover()).append(" | ")
                        .append(w.aboveTakeover() == null ? "-" : w.aboveTakeover()).append(" |\n");
            }
            md.append('\n');
        }
        if (s.nearest() != null) {
            List<String> deltas = new ArrayList<>();
            s.nearest().deltas().forEach((k, v) -> deltas.add(k + " " + signed(v)));
            md.append("Nearest alternative: ").append(s.nearest().portfolio())
                    .append(String.format(Locale.ROOT, " (score distance %.1f)", s.nearest().distance()))
                    .append(deltas.isEmpty() ? "" : "; difference from the recommendation: " + String.join(", ", deltas))
                    .append(".\n\n");
        } else {
            md.append("No other option is on the frontier.\n\n");
        }
        list(md, "Notes", s.notes());
    }

    // B. cost

    private static void cost(StringBuilder md, RecordSection<Cost> section) {
        md.append("### B. Cost\n\n");
        if (unavailable(md, section)) {
            return;
        }
        Cost c = section.content();
        md.append("| Item | Metro | Monthly | Range | One-time | Source | Note |\n|---|---|---:|---|---:|---|---|\n");
        for (CostItem l : c.lines()) {
            String note = l.note() == null ? "" : l.note();
            md.append("| ").append(cell(l.label())).append(" | ").append(l.metro() == null ? "-" : l.metro())
                    .append(" | ").append(money(l.currency(), l.monthly())).append(" | ")
                    .append(plain(l.monthlyLow())).append(" to ").append(plain(l.monthlyHigh())).append(" | ")
                    .append(plain(l.oneTime())).append(" | ").append(l.source()).append(" | ").append(cell(note))
                    .append(" |\n");
        }
        if (c.monthly() != null) {
            md.append("| **Total** | | **").append(money(c.currency(), c.monthly())).append("** | ")
                    .append(plain(c.monthlyLow())).append(" to ").append(plain(c.monthlyHigh())).append(" | ")
                    .append(plain(c.oneTime())).append(" | | ").append(c.complete() ? "" : "incomplete").append(" |\n");
        }
        md.append('\n');
        list(md, "Unpriced (not counted as zero)", c.unpriced());
        if (!c.ratesUsed().isEmpty()) {
            md.append("Exchange rates used:\n\n| From | To | Rate | Kind | Source | As of | Band |\n"
                    + "|---|---|---:|---|---|---|---:|\n");
            for (FxRate f : c.ratesUsed()) {
                md.append("| ").append(f.from()).append(" | ").append(f.to()).append(" | ")
                        .append(f.rate().stripTrailingZeros().toPlainString()).append(" | ").append(f.kind())
                        .append(" | ").append(cell(f.source())).append(" | ").append(f.asOf()).append(" | +/-")
                        .append(num(f.bandPercent())).append("% |\n");
            }
            md.append('\n');
        }
        md.append("#### Term and growth\n\n");
        if (c.growth() == null) {
            md.append("_Not available: ").append(sentence(c.growthNote())).append("_\n\n");
        } else {
            Growth g = c.growth();
            md.append(String.format(Locale.ROOT, "Traffic growth %.1f%%/year over %d months. On-demand total over the "
                    + "horizon: %s.%n%n", g.annualGrowth() * 100, g.horizonMonths(),
                    g.onDemandTotal() == null ? "incomplete" : money(g.currency(), g.onDemandTotal())));
            md.append("| Item | Upgrade | Recommended term | Why |\n|---|---|---|---|\n");
            for (GrowthItem i : g.items()) {
                md.append("| ").append(cell(i.label())).append(" | ")
                        .append(i.upgradeMonth() == null ? "none in horizon" : "month " + i.upgradeMonth()).append(" | ")
                        .append(i.recommendedTerm() == null ? "none priced" : i.recommendedTerm()).append(" | ")
                        .append(cell(i.recommendation())).append(" |\n");
            }
            md.append('\n');
            list(md, "Unpriced", g.unpriced());
        }
        md.append("#### TCO and savings\n\n");
        if (c.value() == null) {
            md.append("_Not available: ").append(sentence(c.valueNote())).append("_\n\n");
        } else {
            Value v = c.value();
            md.append("Compared for cloud `").append(v.cloud()).append("`.\n\n");
            if (v.savings() != null) {
                var s = v.savings();
                String volume = s.monthlyEgressGb() == null ? ""
                        : String.format(Locale.ROOT, " at %,.0f GB/month", s.monthlyEgressGb());
                if (s.netMonthlySavings() == null) {
                    md.append("- Private-path egress saving: not priced").append(volume);
                } else if (s.netMonthlySavings().signum() < 0) {
                    md.append("- The private path costs ").append(money(s.currency(), s.netMonthlySavings().negate()))
                            .append(" per month more than the internet path").append(volume)
                            .append(" (egress savings do not cover the interconnect)");
                } else {
                    md.append("- The private path saves ").append(money(s.currency(), s.netMonthlySavings()))
                            .append(" per month net").append(volume)
                            .append(s.paybackMonths() == null ? "" : "; payback " + plain(s.paybackMonths())
                                    + " months");
                }
                md.append(s.complete() ? "" : "; incomplete: " + String.join("; ", s.unpriced())).append(".\n");
            }
            if (v.tco() != null) {
                var t = v.tco();
                md.append("- TCO over ").append(t.term()).append(": cheapest archetype ").append(t.recommended());
                if (t.monthlySavingsVsBaseline() != null && t.baseline() != null
                        && !t.baseline().equals(t.recommended())) {
                    md.append(", ").append(money(t.currency(), t.monthlySavingsVsBaseline())).append(" per month below ")
                            .append(t.baseline());
                }
                md.append(".\n");
                if (t.note() != null) {
                    md.append("- ").append(sentence(t.note())).append('\n');
                }
            }
            for (String n : v.notes()) {
                md.append("- ").append(sentence(n)).append('\n');
            }
            md.append('\n');
        }
    }

    // C. resilience

    private static void resilience(StringBuilder md, RecordSection<Resilience> section) {
        md.append("### C. Resilience\n\n");
        if (unavailable(md, section)) {
            return;
        }
        Resilience r = section.content();
        md.append("Analysed portfolio: `").append(r.portfolio()).append("`.\n\n");
        if (r.n1() != null) {
            md.append("- N-1 ").append(r.n1().level()).append(": ").append(sentence(r.n1().statement())).append('\n');
        }
        if (r.n2() != null) {
            md.append("- N-2 ").append(r.n2().level()).append(": ").append(sentence(r.n2().statement())).append('\n');
        }
        md.append('\n');
        list(md, "Single points of failure", r.singlePoints().isEmpty() ? List.of("None") : r.singlePoints());
        list(md, "Correlated risks", r.correlatedRisks().isEmpty() ? List.of("None") : r.correlatedRisks());
        if (!r.availability().isEmpty()) {
            md.append("Estimated availability. These are computed from planning assumptions, not SLAs: ")
                    .append(r.availabilityBasis() == null ? "" : sentence(r.availabilityBasis())).append("\n\n");
            md.append("| Workload | Availability | Downtime per year | Composition |\n|---|---:|---:|---|\n");
            for (Availability a : r.availability()) {
                md.append("| ").append(a.workload()).append(String.format(Locale.ROOT, " | %.4f%% | %.0f min | ",
                        a.percent(), a.downtimeMinutesPerYear())).append(cell(a.formula())).append(" |\n");
            }
            md.append('\n');
            if (!r.elementAvailability().isEmpty()) {
                List<String> parts = new ArrayList<>();
                r.elementAvailability().forEach((k, v) -> parts.add(k + " " + String.format(Locale.ROOT, "%.5f", v)));
                md.append("Per-element availability assumed: ").append(String.join(", ", parts)).append(".\n\n");
            }
        }
        list(md, "Remediation", r.remediations());
        list(md, "Notes", r.notes());
        if (r.mermaid() != null && !r.mermaid().isBlank()) {
            md.append("```mermaid\n").append(r.mermaid().strip()).append("\n```\n\n");
        }
    }

    // D. risks

    private static void risks(StringBuilder md, RecordSection<DesignDecisionRecord.Risks> section) {
        md.append("### D. Risks and findings\n\n");
        if (unavailable(md, section)) {
            return;
        }
        List<Risk> items = section.content().items();
        if (items.isEmpty()) {
            md.append("No findings.\n\n");
            return;
        }
        md.append("| Severity | Source | Category | Finding | Action |\n|---|---|---|---|---|\n");
        for (Risk x : items) {
            md.append("| ").append(x.severity()).append(" | ").append(x.source()).append(" | ").append(x.category())
                    .append(" | ").append(cell(x.statement())).append(" | ")
                    .append(x.recommendation() == null ? "-" : cell(x.recommendation())).append(" |\n");
        }
        md.append('\n');
    }

    // E. provenance

    private static void provenance(StringBuilder md, RecordSection<Provenance> section) {
        md.append("### E. Assumptions and provenance\n\n");
        if (unavailable(md, section)) {
            return;
        }
        Provenance p = section.content();
        int total = p.measuredLatencies() + p.estimatedLatencies() + p.unknownLatencies();
        if (total > 0) {
            md.append(String.format(Locale.ROOT, "Site to metro round trips of the recommendation: %d measured by "
                    + "Fabric, %d estimated, %d unknown.%n%n", p.measuredLatencies(), p.estimatedLatencies(),
                    p.unknownLatencies()));
        }
        if (p.latencyCalibration() != null) {
            md.append(sentence(p.latencyCalibration())).append("\n\n");
        }
        list(md, "As of", p.asOf());
        list(md, "Data notes", p.dataNotes());
        if (!p.assumptions().isEmpty()) {
            md.append("Assumptions:\n\n");
            for (Assumption a : p.assumptions()) {
                md.append("- (").append(a.source()).append(") ").append(sentence(a.text())).append('\n');
            }
            md.append('\n');
        }
    }

    // F. implementation

    private static void implementation(StringBuilder md, RecordSection<Implementation> section) {
        md.append("### F. Implementation and reproduction\n\n");
        if (unavailable(md, section)) {
            return;
        }
        Implementation i = section.content();
        if (i.plan() != null) {
            PlanSummary p = i.plan();
            md.append("| Plan | Value |\n|---|---|\n");
            md.append("| Plan id | `").append(p.planId()).append("` |\n");
            md.append("| Deployment name | ").append(p.deploymentName()).append(" |\n");
            md.append("| Steps | ").append(p.steps()).append(" (").append(p.executableSteps()).append(" executable) |\n");
            md.append("| Cloud Routers / connections / backbone links | ").append(p.routers()).append(" / ")
                    .append(p.connections()).append(" / ").append(p.backboneLinks()).append(" |\n");
            md.append("| Monthly estimate, Equinix items | ").append(p.monthlyTotal() == null ? "no single total"
                    : money(p.currency(), p.monthlyTotal())).append(p.pricingSource() == null ? ""
                    : " (" + p.pricingSource() + ")").append(" |\n");
            md.append("| One-time estimate | ").append(p.setupTotal() == null ? "n/a"
                    : money(p.currency(), p.setupTotal())).append(" |\n");
            md.append("| Validation | ").append(p.valid() ? "passed" : "failed: " + cell(join(p.validationErrors())))
                    .append(" |\n\n");
        }
        if (i.planNote() != null) {
            md.append(sentence(i.planNote())).append("\n\n");
        }
        var rep = i.reproduction();
        md.append("To reproduce: run spec fingerprint `").append(rep.specFingerprint()).append("` with SDK ")
                .append(rep.sdkVersion());
        if (rep.pinnedRates().isEmpty()) {
            md.append(". No price was converted, so no exchange rate needs pinning.\n");
        } else {
            List<String> rates = new ArrayList<>();
            rep.pinnedRates().forEach((k, v) -> rates.add(k + " " + v.stripTrailingZeros().toPlainString()));
            md.append(", with these exchange rates pinned in `commercial.exchangeRates`: ").append(String.join(", ", rates))
                    .append(" (as of ").append(rep.ratesAsOf()).append("). The pinned spec has fingerprint `")
                    .append(rep.pinnedSpecFingerprint()).append("` (`DesignDecisionRecord.reproducibleSpec()`).\n");
        }
    }

    // formatting

    private static Map<String, String> units(DesignDecisionRecord r) {
        Map<String, String> units = new java.util.HashMap<>();
        if (r.decision() != null && r.decision().isIncluded()) {
            for (ObjectiveResult o : r.decision().content().objectives()) {
                units.put(o.id(), o.unit());
            }
        }
        return units;
    }

    private static String currency(DesignDecisionRecord r) {
        return r.spec().commercial().currencyOrDefault();
    }

    /**
     * An objective value in its unit, as the record prints it.
     *
     * @param unit     the objective's unit
     * @param currency the spec currency (for cost)
     * @param v        the value
     * @return the text
     */
    static String value(String unit, String currency, double v) {
        if ("per month".equals(unit)) {
            return String.format(Locale.ROOT, "%s %,.2f/month", currency, v);
        }
        if ("ms".equals(unit)) {
            return String.format(Locale.ROOT, "%.1f ms", v);
        }
        if ("%".equals(unit)) {
            return String.format(Locale.ROOT, "%.1f%%", v);
        }
        return String.format(Locale.ROOT, "%.2f", v) + (unit == null || unit.isBlank() ? "" : " " + unit);
    }

    static String money(String currency, BigDecimal amount) {
        return amount == null ? "n/a" : (currency == null ? "" : currency + " ")
                + String.format(Locale.ROOT, "%,.2f", amount);
    }

    private static String plain(BigDecimal amount) {
        return amount == null ? "n/a" : String.format(Locale.ROOT, "%,.2f", amount);
    }

    private static String pct(double share) {
        return String.format(Locale.ROOT, "%.0f%%", share * 100);
    }

    private static String signed(double v) {
        return (v > 0 ? "+" : "") + String.format(Locale.ROOT, "%.2f", v);
    }

    private static String num(double v) {
        return BigDecimal.valueOf(v).stripTrailingZeros().toPlainString();
    }

    private static String join(List<String> parts) {
        return parts == null || parts.isEmpty() ? "-" : String.join("; ", parts);
    }

    private static String cell(String text) {
        return text == null ? "" : text.replace("|", "\\|").replace("\r", " ").replace("\n", " ");
    }

    private static String sentence(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String t = text.strip();
        return t.endsWith(".") || t.endsWith("!") || t.endsWith("?") ? t : t + ".";
    }

    private static void list(StringBuilder md, String heading, List<String> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        md.append(heading).append(":\n\n");
        for (String item : items) {
            md.append("- ").append(item).append('\n');
        }
        md.append('\n');
    }
}
