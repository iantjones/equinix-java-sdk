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

package com.eqixiac.equinix.design.optimizer.model;

import com.eqixiac.equinix.design.placement.PlacementPlan;
import com.eqixiac.equinix.design.solve.SensitivityReport;
import com.eqixiac.equinix.design.spi.ConstraintVerdict;
import com.eqixiac.equinix.design.spi.CostLine;
import com.eqixiac.equinix.design.spi.DesignOption;
import com.eqixiac.equinix.design.spi.ObjectiveValue;
import com.eqixiac.equinix.design.spi.PortfolioCost;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Renders an {@link OptimizationResult} as a summary or a Markdown report. */
final class OptimizationReport {

    private OptimizationReport() {
    }

    static String summary(OptimizationResult r) {
        StringBuilder sb = new StringBuilder();
        DesignOption rec = r.getRecommended();
        if (rec == null) {
            sb.append("No viable design satisfies every constraint.");
            if (r.getSolverNotes() != null && !r.getSolverNotes().isEmpty()) {
                sb.append(' ').append(String.join(" ", r.getSolverNotes()));
            }
            return sb.toString();
        }
        sb.append("Recommended design: ").append(names(r)).append(String.format(Locale.ROOT,
                " - %d metro(s), weighted score %.1f/100.%n", rec.portfolio().size(), rec.weightedScore()));
        for (Map.Entry<String, ObjectiveValue> e : rec.values().entrySet()) {
            sb.append("  ").append(e.getKey()).append(": ").append(render(e.getValue())).append('\n');
        }
        if (r.getRiskAssessment() != null && !r.getRiskAssessment().critical().isEmpty()) {
            sb.append("WARNING: ").append(r.getRiskAssessment().critical().size()).append(" critical risk(s).\n");
        }
        SolveStatistics s = r.getSolveStatistics();
        if (s != null) {
            sb.append(String.format(Locale.ROOT, "Frontier: %d Pareto-optimal option(s). Searched %d portfolio(s) of "
                            + "%d-%d metros from %d candidates (%d feasible) in %d ms%s.%n", s.frontier(), s.evaluated(),
                    s.minMetros(), s.maxMetros(), s.candidates(), s.feasible(), s.elapsedMillis(),
                    s.exact() ? ", exhaustively" : ", over a narrowed field"));
        }
        if (r.getLatencyCalibration() != null) {
            sb.append(r.getLatencyCalibration().describe()).append('\n');
        }
        sb.append("Computed in ").append(r.getComputeTimeMs()).append(" ms.");
        return sb.toString();
    }

    static String markdown(OptimizationResult r) {
        StringBuilder md = new StringBuilder("# Metro Optimization Report\n\n");
        if (r.getSpec() != null) {
            md.append("_Spec `").append(r.getSpec().name()).append("`, fingerprint `")
                    .append(r.getSpecFingerprint(), 0, Math.min(12, r.getSpecFingerprint().length()))
                    .append("`; generated ").append(r.getComputedAt()).append("_\n\n");
        }
        md.append("## Summary\n\n").append(summary(r).replace("\n", "  \n")).append("\n\n");
        DesignOption rec = r.getRecommended();
        if (rec != null) {
            md.append("## Recommendation and why\n\n");
            md.append("**").append(names(r)).append("** has the highest weighted score among the feasible designs");
            md.append(rec.paretoOptimal() ? " and no other design is at least as good on every objective.\n\n"
                    : ".\n\n");
            for (Map.Entry<String, ObjectiveValue> e : rec.values().entrySet()) {
                Double score = rec.scores().get(e.getKey());
                md.append("- **").append(e.getKey()).append("**: ").append(e.getValue().explanation())
                        .append(score == null ? " (not scored: unknown)" : String.format(Locale.ROOT, " (score %.1f)", score))
                        .append('\n');
            }
            md.append('\n');
            frontier(md, r);
            if (r.getCostComparability() != null && r.getCostComparability().incomplete()) {
                md.append(r.getCostComparability().toMarkdown()).append('\n');
            }
            sensitivity(md, r.getSensitivity());
        }
        constraints(md, r);
        placement(md, r.getPlacement());
        if (rec != null && rec.cost() != null) {
            cost(md, rec.cost());
        }
        if (r.getLatencyMatrix() != null && r.getLatencyMatrix().hasSites()) {
            md.append("## Latency (round trip)\n\n```\n").append(r.getLatencyMatrix().toTableString()).append("\n```\n\n");
        }
        if (r.getRiskAssessment() != null && !r.getRiskAssessment().getFindings().isEmpty()) {
            md.append("## Findings\n\n");
            for (RiskFinding f : r.getRiskAssessment().getFindings()) {
                md.append("- **[").append(f.getSeverity()).append("] ").append(f.getCategory()).append("**: ")
                        .append(f.getDescription());
                if (f.getRecommendation() != null) {
                    md.append(" _").append(f.getRecommendation()).append("_");
                }
                md.append('\n');
            }
            md.append('\n');
        }
        if (r.getExplanation() != null) {
            md.append("## Method and assumptions\n\n").append(r.getExplanation().getMethodology()).append("\n\n");
            for (String a : r.getExplanation().getAssumptions()) {
                md.append("- ").append(a).append('\n');
            }
            md.append('\n');
        }
        md.append("## Data provenance\n\n");
        md.append("- Facts gathered ").append(r.getDataAsOf()).append('\n');
        if (r.getDataProvenance() != null) {
            for (String p : r.getDataProvenance()) {
                md.append("- ").append(p).append('\n');
            }
        }
        if (r.getLatencyCalibration() != null) {
            md.append("- ").append(r.getLatencyCalibration().describe()).append('\n');
        }
        return md.toString();
    }

    private static void frontier(StringBuilder md, OptimizationResult r) {
        List<DesignOption> frontier = r.getFrontier();
        if (frontier == null || frontier.isEmpty()) {
            return;
        }
        List<String> ids = new ArrayList<>(frontier.get(0).values().keySet());
        md.append("## Trade-off frontier\n\n| Design | Weighted |");
        for (String id : ids) {
            md.append(' ').append(id).append(" |");
        }
        md.append("\n|---|---:|");
        md.append("---:|".repeat(ids.size()));
        md.append('\n');
        for (DesignOption option : frontier) {
            boolean chosen = option.portfolio().equals(r.getRecommended().portfolio());
            boolean flagged = r.getCostComparability() != null && r.getCostComparability().flags(option.portfolio());
            md.append("| ").append(chosen ? "**" + option.portfolio().key() + "** (recommended)" : option.portfolio().key())
                    .append(flagged ? " (cost needs exchange rates)" : "")
                    .append(String.format(Locale.ROOT, " | %.1f |", option.weightedScore()));
            for (String id : ids) {
                ObjectiveValue v = option.values().get(id);
                md.append(' ').append(v == null || !v.known() ? "unknown" : String.format(Locale.ROOT, "%.1f", v.value()))
                        .append(" |");
            }
            md.append('\n');
        }
        md.append('\n');
    }

    private static void sensitivity(StringBuilder md, SensitivityReport s) {
        if (s == null) {
            return;
        }
        md.append("## Sensitivity\n\n");
        for (SensitivityReport.WeightRange w : s.weightRanges()) {
            md.append(String.format(Locale.ROOT, "- **%s** weight %.0f%%: the recommendation holds from %.0f%% to %.0f%%",
                    w.objective(), w.weight() * 100, w.low() * 100, w.high() * 100));
            if (w.belowTakeover() != null) {
                md.append("; below, ").append(w.belowTakeover()).append(" takes over");
            }
            if (w.aboveTakeover() != null) {
                md.append("; above, ").append(w.aboveTakeover()).append(" takes over");
            }
            md.append(".\n");
        }
        if (s.nearest() != null) {
            md.append("- Nearest alternative on the frontier: **").append(s.nearest().portfolio()).append("** (");
            List<String> parts = new ArrayList<>();
            s.nearest().deltas().forEach((k, v) -> parts.add(String.format(Locale.ROOT, "%s %+.1f", k, v)));
            md.append(String.join(", ", parts)).append(" vs the recommendation).\n");
        }
        for (String note : s.notes()) {
            md.append("- ").append(note).append('\n');
        }
        md.append('\n');
    }

    private static void constraints(StringBuilder md, OptimizationResult r) {
        if (r.getConstraintVerdicts() == null || r.getConstraintVerdicts().isEmpty()) {
            return;
        }
        md.append("## Constraints\n\n| Constraint | Verdict | Binding |\n|---|---|---|\n");
        for (ConstraintVerdict v : r.getConstraintVerdicts()) {
            String binding = "";
            if (r.getSensitivity() != null) {
                for (SensitivityReport.BindingConstraint b : r.getSensitivity().constraints()) {
                    if (b.constraint().equals(v.constraintId())) {
                        binding = b.explanation();
                    }
                }
            }
            md.append("| ").append(v.constraintId()).append(" | ").append(v.satisfied() ? "met: " : "FAILED: ")
                    .append(v.reason()).append(" | ").append(binding).append(" |\n");
        }
        md.append('\n');
    }

    private static void placement(StringBuilder md, PlacementPlan plan) {
        if (plan == null || plan.placements().isEmpty()) {
            return;
        }
        md.append("## Workload placement\n\n");
        for (PlacementPlan.Placement p : plan.placements()) {
            md.append("- **").append(p.workload()).append("**: ").append(p.reasoning());
            if (!p.issues().isEmpty()) {
                md.append(" (").append(String.join("; ", p.issues())).append(')');
            }
            md.append('\n');
        }
        md.append('\n');
    }

    private static void cost(StringBuilder md, PortfolioCost cost) {
        md.append("## Cost\n\n| Item | Metro | Monthly | Range | Source |\n|---|---|---:|---|---|\n");
        for (CostLine line : cost.lines()) {
            md.append("| ").append(line.label()).append(" | ").append(line.metro() == null ? "-" : line.metro().code())
                    .append(" | ").append(line.currency()).append(' ').append(line.monthly().toPlainString())
                    .append(" | ").append(line.monthlyLow().toPlainString()).append('-')
                    .append(line.monthlyHigh().toPlainString()).append(" | ").append(line.source()).append(" |\n");
        }
        if (cost.monthlyTotal().isPresent()) {
            md.append("| **Total** | | **").append(cost.currency().orElse("")).append(' ')
                    .append(cost.monthlyTotal().get().toPlainString()).append("** | ")
                    .append(cost.monthlyLow().map(BigDecimal::toPlainString).orElse("")).append('-')
                    .append(cost.monthlyHigh().map(BigDecimal::toPlainString).orElse("")).append(" | |\n");
        } else if (!cost.lines().isEmpty()) {
            md.append("| **Total** | | not stated: lines span ").append(cost.currencies()).append(" | | |\n");
        }
        md.append('\n');
        if (!cost.unpriced().isEmpty()) {
            md.append("Unpriced (every total understates the cost):\n\n");
            for (String u : cost.unpriced()) {
                md.append("- ").append(u).append('\n');
            }
            md.append('\n');
        }
    }

    private static String names(OptimizationResult r) {
        List<String> names = new ArrayList<>();
        if (r.getRecommendations() != null && !r.getRecommendations().isEmpty()) {
            for (MetroRecommendation m : r.getRecommendations()) {
                names.add(m.getMetroName() == null ? m.getMetroId().code()
                        : m.getMetroName() + " (" + m.getMetroId().code() + ")");
            }
            return String.join(" + ", names);
        }
        return r.getRecommended().portfolio().key();
    }

    private static String render(ObjectiveValue v) {
        return v.known() ? v.explanation() : "unknown (" + v.explanation() + ")";
    }
}
