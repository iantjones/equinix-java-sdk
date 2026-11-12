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

package com.eqixiac.equinix.design.resilience;

import com.eqixiac.equinix.design.resilience.ResilienceFindings.CorrelatedRisk;
import com.eqixiac.equinix.design.resilience.ResilienceFindings.RankedScenario;
import com.eqixiac.equinix.design.resilience.ResilienceFindings.Remediation;
import com.eqixiac.equinix.design.resilience.ResilienceFindings.SinglePointOfFailure;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Renders a {@link ResilienceReport} as Markdown. */
final class ResilienceMarkdown {

    private static final int RANKING_ROWS = 15;

    private ResilienceMarkdown() {
    }

    static String render(ResilienceReport r) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Resilience report: ").append(r.specName()).append(" on ").append(r.portfolio()).append("\n\n");
        sb.append("Spec fingerprint `").append(r.specFingerprint()).append("`; portfolio from ")
                .append(r.portfolioSource());
        if (r.dataAsOf() != null) {
            sb.append("; facts as of ").append(r.dataAsOf());
        }
        sb.append(".\n\n");
        sb.append("- **N-1:** ").append(r.n1().statement()).append('\n');
        sb.append("- **N-2:** ").append(r.n2().statement()).append('\n');
        sb.append("- **Single points of failure:** ").append(r.singlePointsOfFailure().size()).append('\n');
        sb.append("- **Correlated risks:** ").append(r.correlatedRisks().size()).append("\n\n");

        sb.append("## Baseline\n\n");
        ScenarioOutcome b = r.baseline();
        sb.append(String.format(Locale.ROOT, "Service index %.0f%%; sized cloud demand %s Mbps.%n%n",
                100 * b.serviceIndex(), DesignGraph.fmt(b.demandMbps())));
        sb.append("| Workload | Status | Runs in | Sites | Clouds |\n|---|---|---|---|---|\n");
        for (WorkloadOutcome w : b.workloads()) {
            sb.append("| ").append(w.workload()).append(" | ").append(lower(w.status())).append(" | ")
                    .append(w.activeMetro() == null ? "-" : w.activeMetro()).append(" | ")
                    .append(join(w.sitePaths())).append(" | ").append(join(w.cloudPaths())).append(" |\n");
        }
        if (!b.constraintBreaks().isEmpty() || !b.zoneViolations().isEmpty()) {
            sb.append("\nAlready at baseline:\n\n");
            b.constraintBreaks().forEach(x -> sb.append("- ").append(x).append('\n'));
            b.zoneViolations().forEach(x -> sb.append("- ").append(x).append('\n'));
        }

        sb.append("\n## Single failures\n\n");
        sb.append("| Scenario | Verdict | Survival | Site weight affected | Down | Required clouds lost | "
                + "Capacity served | Shortfall | Breaks |\n|---|---|---|---|---|---|---|---|---|\n");
        for (ScenarioOutcome o : r.singleFailures()) {
            sb.append("| ").append(o.scenario().description()).append(" | ").append(lower(o.verdict()))
                    .append(String.format(Locale.ROOT, " | %.0f%% | %.0f%% | ", 100 * o.relativeSurvival(),
                            100 * o.siteWeightAffected()))
                    .append(o.workloadsDown().isEmpty() ? "-" : String.join(", ", o.workloadsDown())).append(" | ")
                    .append(o.requiredCloudsAffected().isEmpty() ? "-" : String.join(", ", o.requiredCloudsAffected()))
                    .append(" | ").append(o.demandMbps() > 0 ? String.format(Locale.ROOT, "%.0f%% of %s Mbps",
                            100 * o.capacityShare(), DesignGraph.fmt(o.demandMbps())) : "n/a")
                    .append(" | ").append(o.shortfallMbps() > 0 ? DesignGraph.fmt(o.shortfallMbps()) + " Mbps" : "-")
                    .append(" | ").append(o.constraintBreaks().size() + o.zoneViolations().size()).append(" |\n");
        }

        sb.append("\n## Blast radius (worst first)\n\n");
        rankTable(sb, r.blastRadius().subList(0, Math.min(RANKING_ROWS, r.blastRadius().size())));

        sb.append("\n## Single points of failure\n\n");
        if (r.singlePointsOfFailure().isEmpty()) {
            sb.append("None: no single element failure causes an outage.\n");
        }
        for (SinglePointOfFailure s : r.singlePointsOfFailure()) {
            sb.append("- **").append(s.description()).append("** (`").append(s.scenarioId()).append("`): ")
                    .append(s.effect()).append('\n');
        }

        sb.append("\n## Correlated risks\n\n");
        if (r.correlatedRisks().isEmpty()) {
            sb.append("None found.\n");
        }
        for (CorrelatedRisk c : r.correlatedRisks()) {
            sb.append("- ").append(lower(c.severity())).append(": ").append(c.description())
                    .append(c.scenarioId() == null ? "" : " (scenario `" + c.scenarioId() + "`)").append('\n');
        }

        sb.append("\n## Pairs (N-2)\n\n").append(r.n2().statement()).append("\n\n");
        if (!r.pairs().worst().isEmpty()) {
            rankTable(sb, r.pairs().worst());
        }

        sb.append("\n## Availability estimate\n\n");
        sb.append("Estimates under the assumptions below, not guarantees: Equinix publishes SLAs per product and "
                + "the contractual figures are the ones that apply.\n\n");
        sb.append("| Workload | Core | End to end | Downtime / year | Composition |\n|---|---|---|---|---|\n");
        for (WorkloadAvailability a : r.availability()) {
            sb.append("| ").append(a.workload()).append(String.format(Locale.ROOT, " | %.5f%% | %.5f%% | %.0f min | ",
                    100 * a.coreAvailability(), a.percent(), a.downtimeMinutesPerYear())).append(a.formula())
                    .append(" |\n");
        }
        sb.append("\nAssumed availability per element (").append(r.availabilityAssumptions().source()).append("):\n\n");
        for (Map.Entry<ElementType, Double> e : r.availabilityAssumptions().availability().entrySet()) {
            sb.append(String.format(Locale.ROOT, "- %s: %.4f%%%n", lower(e.getKey()), 100 * e.getValue()));
        }

        sb.append("\n## Remediation\n\n");
        if (r.remediations().isEmpty()) {
            sb.append("Nothing to remediate.\n");
        }
        for (Remediation m : r.remediations()) {
            sb.append("- ").append(m.action()).append(m.verified() ? " **(re-simulated: removes " : " (addresses ")
                    .append(String.join(", ", m.addresses().stream().map(x -> "`" + x + "`").toList()))
                    .append(m.verified() ? ")**" : ")").append(": ").append(m.rationale()).append('\n');
        }

        sb.append("\n## Design graph\n\n```mermaid\n").append(r.graph().toMermaid()).append("```\n\n")
                .append(r.graph().toTable());

        sb.append("\n## Assumptions\n\n");
        r.assumptions().forEach(a -> sb.append("- ").append(a).append('\n'));
        if (!r.notes().isEmpty()) {
            sb.append("\n## Notes\n\n");
            r.notes().forEach(n -> sb.append("- ").append(n).append('\n'));
        }
        if (!r.timingsMs().isEmpty()) {
            sb.append("\n## Timings\n\n");
            r.timingsMs().forEach((k, v) -> sb.append("- ").append(k).append(": ").append(v).append(" ms\n"));
        }
        return sb.toString();
    }

    private static void rankTable(StringBuilder sb, List<RankedScenario> rows) {
        sb.append("| # | Scenario | Verdict | Service lost | Site weight affected | Shortfall | Down |\n"
                + "|---|---|---|---|---|---|---|\n");
        for (RankedScenario s : rows) {
            sb.append("| ").append(s.rank()).append(" | ").append(s.description()).append(" | ")
                    .append(lower(s.verdict())).append(String.format(Locale.ROOT, " | %.0f%% | %.0f%% | ",
                            100 * s.impact(), 100 * s.siteWeightAffected()))
                    .append(s.shortfallMbps() > 0 ? DesignGraph.fmt(s.shortfallMbps()) + " Mbps" : "-").append(" | ")
                    .append(s.workloadsDown().isEmpty() ? "-" : String.join(", ", s.workloadsDown())).append(" |\n");
        }
    }

    private static String join(Map<String, String> paths) {
        if (paths.isEmpty()) {
            return "-";
        }
        StringBuilder sb = new StringBuilder();
        paths.forEach((k, v) -> sb.append(sb.length() == 0 ? "" : "; ").append(k).append(": ").append(v));
        return sb.toString().replace("|", "/");
    }

    private static String lower(Enum<?> e) {
        return e.name().toLowerCase(Locale.ROOT).replace('_', ' ');
    }
}
