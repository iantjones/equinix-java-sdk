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

package api.equinix.javasdk.mcp.server;

import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.design.export.TerraformExporter;
import api.equinix.javasdk.design.geo.SpeedOfLightLatency;
import api.equinix.javasdk.design.optimizer.MetroOptimizer;
import api.equinix.javasdk.design.optimizer.enums.LatencySensitivity;
import api.equinix.javasdk.design.optimizer.enums.OptimizationStrategy;
import api.equinix.javasdk.design.optimizer.enums.RedundancyTier;
import api.equinix.javasdk.design.optimizer.enums.WorkloadType;
import api.equinix.javasdk.design.optimizer.model.CostEstimate;
import api.equinix.javasdk.design.optimizer.model.MetroRecommendation;
import api.equinix.javasdk.design.optimizer.model.OptimizationResult;
import api.equinix.javasdk.design.optimizer.wizard.DeploymentWizard;
import api.equinix.javasdk.design.optimizer.wizard.enums.BackboneTopology;
import api.equinix.javasdk.design.optimizer.wizard.model.DeploymentPlan;
import api.equinix.javasdk.design.optimizer.wizard.model.PlanPricing;
import api.equinix.javasdk.design.peering.PeeringIntelligence;
import api.equinix.javasdk.design.peering.model.PeeringIntelligenceResult;
import api.equinix.javasdk.design.value.ratecard.RateCard;
import api.equinix.javasdk.design.value.savings.SavingsCalculator;
import api.equinix.javasdk.design.value.savings.SavingsEstimate;
import api.equinix.javasdk.design.value.tco.DeploymentArchetype;
import api.equinix.javasdk.design.value.tco.TcoCalculator;
import api.equinix.javasdk.design.value.tco.TcoComparison;
import api.equinix.javasdk.fabric.model.Metro;
import api.equinix.javasdk.fabric.model.MetroRegistry;
import api.equinix.javasdk.fabric.model.implementation.cloud.CloudProviderType;
import api.equinix.javasdk.internetaccess.model.Ibx;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static api.equinix.javasdk.mcp.server.Args.enumValue;
import static api.equinix.javasdk.mcp.server.Args.optEnum;
import static api.equinix.javasdk.mcp.server.Args.optInt;
import static api.equinix.javasdk.mcp.server.Args.optLong;
import static api.equinix.javasdk.mcp.server.Args.optNumber;
import static api.equinix.javasdk.mcp.server.Args.optString;
import static api.equinix.javasdk.mcp.server.Args.requireNumber;
import static api.equinix.javasdk.mcp.server.Args.requireString;
import static api.equinix.javasdk.mcp.server.Schemas.array;
import static api.equinix.javasdk.mcp.server.Schemas.integer;
import static api.equinix.javasdk.mcp.server.Schemas.looseObject;
import static api.equinix.javasdk.mcp.server.Schemas.lowerNames;
import static api.equinix.javasdk.mcp.server.Schemas.number;
import static api.equinix.javasdk.mcp.server.Schemas.object;
import static api.equinix.javasdk.mcp.server.Schemas.props;
import static api.equinix.javasdk.mcp.server.Schemas.string;
import static api.equinix.javasdk.mcp.server.Schemas.stringEnum;

/**
 * The seven {@code design_*} tools — every one runs a design <em>engine</em> (optimizer,
 * wizard, speed-of-light geometry, TCO, savings, peering intelligence, Terraform export)
 * rather than mirroring a REST endpoint.
 */
final class DesignToolFactory {

    private static final int MAX_RECOMMENDATIONS = 8;
    private static final int MAX_OPPORTUNITIES = 20;
    private static final int MAX_FINDINGS = 10;

    private static final String[] CLOUD_VALUES = {
            "aws", "azure", "gcp", "google_cloud", "oci", "oracle_cloud", "ibm_cloud", "alibaba_cloud"};

    private DesignToolFactory() {
    }

    static List<ToolRegistration> tools() {
        return List.of(optimizePlacement(), planDeployment(), estimateLatency(), estimateTco(),
                compareCloudEgress(), analyzePeering(), exportTerraform());
    }

    // ── design_optimize_placement ───────────────────────────────────────────

    /**
     * The optimizer's input schema, shared by {@code design_optimize_placement} and the
     * {@code optimization} block of {@code design_plan_deployment}.
     *
     * <p>Every description here is a prompt the calling model reasons from, so each states what the
     * engine actually does with the field — in particular whether it narrows <em>candidacy</em> (which
     * metros are recommended) or only <em>placement</em> (which recommended metro a single workload
     * lands in), because the two read identically in a payload and only one of them changes the
     * ranked list. A field the engine reads nowhere is not documented here; it is removed, so the
     * model cannot spend a turn setting a knob that moves nothing.</p>
     */
    private static Map<String, Object> optimizationProps() {
        Map<String, Object> workloadItem = object(props(
                "label", string("Short name for this workload, e.g. 'ML training'."),
                "type", stringEnum("The workload archetype. It selects this workload's default profile: "
                                + "its latency sensitivity, whether placement is pulled toward the user sites, "
                                + "and any power/cooling needs. 'disaster_recovery' and 'cold_backup' are placed "
                                + "in a different region from the primary where one is recommended. Power and "
                                + "cooling are recorded on the result for cabinet/cage selection only — Fabric "
                                + "publishes no per-metro power or cooling capability, so they never influence "
                                + "the metro ranking.",
                        lowerNames(WorkloadType.class)),
                "bandwidth_mbps", integer("Sustained bandwidth this workload needs, in Mbps. Used for cost "
                        + "sizing only: all workloads' bandwidth is summed and split across the recommended "
                        + "metros to price a representative Fabric connection. It does not change which metros "
                        + "are recommended. Where the archetype declares a higher floor, that floor is priced "
                        + "instead and the raise is named in explanation.assumptions."),
                "latency_sensitivity", stringEnum("How latency-sensitive the workload is. Only 'critical' "
                                + "changes the outcome: it places this workload in the recommended metro with the "
                                + "lowest site-weighted latency instead of the highest-scored one. 'high', "
                                + "'medium' and 'low' are recorded but change neither placement nor ranking. To "
                                + "make latency bind, use max_latency_ms (this workload) or "
                                + "constraints.max_latency_ms (the whole deployment).",
                        lowerNames(LatencySensitivity.class)),
                "max_latency_ms", number("Hard latency ceiling from the user sites to this workload, in "
                        + "milliseconds. It narrows PLACEMENT, not candidacy: the recommended metro set is "
                        + "unchanged, but this workload is placed only in metros whose worst-case latency to "
                        + "every site is within the ceiling. If no recommended metro honours it the workload is "
                        + "still placed and the run says so — a HIGH WORKLOAD_LATENCY_TOLERANCE_UNMET finding "
                        + "names the closest metro and its measured latency. With no 'sites' it cannot be "
                        + "evaluated and is reported as such. Must be positive and finite; zero or negative is "
                        + "rejected with an error."),
                "requires_clouds", array("Cloud providers this workload must reach over private "
                                + "interconnect. Two effects. (1) It HOMES this workload: the workload is placed in "
                                + "the first recommended metro that carries all of them. (2) It widens CANDIDACY: a "
                                + "metro carrying all of THIS workload's clouds qualifies as a candidate even when it "
                                + "lacks one of the top-level require_clouds, so a single-cloud metro can be "
                                + "recommended to host this single-cloud workload instead of being filtered out. If "
                                + "one resolves to no metro, the workload is still placed and a "
                                + "WORKLOAD_PROVIDER_UNAVAILABLE finding says so.",
                        stringEnum("Cloud provider.", CLOUD_VALUES))),
                "label");
        Map<String, Object> siteItem = object(props(
                "label", string("Short name for this user site, e.g. 'London HQ'."),
                "metro_code", string("Nearest Equinix metro code (e.g. 'LD'). Give this OR "
                        + "latitude+longitude. It must be a metro the Fabric catalogue publishes: an "
                        + "unrecognised code has no coordinates to measure from and falls back to a 150 ms "
                        + "placeholder latency for this site."),
                "latitude", number("Site latitude in decimal degrees (use with 'longitude' when no "
                        + "metro_code). Coordinates are measured by great-circle fibre distance whenever "
                        + "Fabric publishes no metro-to-metro latency for the pair."),
                "longitude", number("Site longitude in decimal degrees."),
                "weight", number("Relative importance of this site when the per-site latencies are "
                        + "averaged into the latency score. Must be positive. A site left unweighted is NOT "
                        + "weighted 1.0 — it counts as an average site: with no site weighted at all every site "
                        + "counts equally, and in a mixed request the unweighted sites take the mean of the "
                        + "stated weights. Weights never affect candidacy, only the latency score and the "
                        + "lowest-latency placement rule.")),
                "label");
        Map<String, Object> constraints = object(props(
                "monthly_budget_max", number("Monthly budget ceiling in USD. Optional; omit it for NO cap "
                        + "— nothing installs a hidden default ceiling. This is a REPORTING check, not a filter: no "
                        + "metro is excluded or scored on it. The estimated monthly total is compared against it and "
                        + "the answer appears as cost_estimate.within_budget; when the estimate exceeds the ceiling a "
                        + "MEDIUM BUDGET_EXCEEDED finding is also raised. Cost scoring is regional and independent of "
                        + "this value."),
                "require_metros", array("Metro codes that must be part of the deployment. They are forced "
                                + "in AFTER filtering, so they bypass every constraint including the latency bound, "
                                + "and are counted separately from the metros that met the constraints. A code the "
                                + "catalogue does not carry raises a HIGH REQUIRED_METRO_NOT_FOUND finding rather "
                                + "than being dropped silently.",
                        string("Metro code, e.g. 'DC'.")),
                "exclude_metros", array("Metro codes that must not be used. Excluded before scoring.",
                        string("Metro code, e.g. 'SV'.")),
                "redundancy", stringEnum("Minimum redundancy tier. It raises the number of metros "
                                + "recommended when max_metros is not set and drives the redundancy score from the "
                                + "geographic diversity of the selected set. multi_region and multi_metro now HARD-"
                                + "spread the selected set across regions: candidates are grouped by region and taken "
                                + "round-robin best-per-region (regions where you have sites first), so no single "
                                + "region can fill the set — geographic spread is enforced, not merely nudged in the "
                                + "score. none and n_plus_1 keep the plain top-N-by-score selection. When "
                                + "multi_region is requested but every qualifying metro sits in one region (spread "
                                + "impossible under the given clouds/regions/compliance/latency), that is a HIGH "
                                + "SINGLE_REGION finding naming exactly what to relax; too few metros for the tier is "
                                + "a REDUNDANCY_GAP finding.",
                        lowerNames(RedundancyTier.class)),
                "max_metros", integer("Hard cap on how many metros are recommended. Defaults to 3, or — "
                        + "when a redundancy tier is set — to the greater of 3 and the tier's minimum plus "
                        + "one."),
                "max_latency_ms", number("Ceiling on the estimated latency from a recommended metro to "
                        + "EVERY user site, in milliseconds. Unlike the per-workload ceiling this narrows "
                        + "CANDIDACY: metros beyond it are excluded before scoring (metros forced in by "
                        + "require_metros bypass it and are flagged with a LATENCY_THRESHOLD finding instead). "
                        + "With no 'sites' it cannot be evaluated and the run reports "
                        + "LATENCY_BOUND_NOT_EVALUATED. Must be positive and finite; zero or negative is "
                        + "rejected with an error.")));
        return props(
                "workloads", array("The workloads to place. At least one is required.", workloadItem),
                "sites", array("User/office/data-center sites whose proximity should drive placement. "
                        + "Without at least one site, latency neither ranks nor filters metros and every "
                        + "latency ceiling in this request is reported as unevaluable.", siteItem),
                "require_clouds", array("Clouds that must be reachable SOMEWHERE in the recommended set — a "
                                + "coverage guarantee across the recommended metros, NOT a per-metro filter. A metro "
                                + "is no longer excluded merely for lacking one; instead the engine guarantees each "
                                + "listed cloud is carried by at least one recommended metro and raises a HIGH "
                                + "REQUIRED_CLOUD_NOT_COVERED finding when the chosen set leaves one unreached (the "
                                + "cloud exists in the account but no recommended metro carries it). Carrying all of "
                                + "these clouds is one way a metro qualifies for candidacy; a metro also qualifies by "
                                + "carrying all clouds of any single workload's requires_clouds, so single-cloud "
                                + "metros are not shut out. A cloud that resolves to NO metro at all in the account "
                                + "stays a separate CRITICAL PROVIDER_UNAVAILABLE finding, naming whether the lookup "
                                + "missed or the matched profiles published no coverage.",
                        stringEnum("Cloud provider.", CLOUD_VALUES)),
                "prefer_clouds", array("Clouds that are nice to have; raises the provider-coverage score "
                                + "but never disqualifies a metro.",
                        stringEnum("Cloud provider.", CLOUD_VALUES)),
                "strategy", stringEnum("Which scoring dimension leads. Default is balanced.",
                        lowerNames(OptimizationStrategy.class)),
                "constraints", constraints);
    }

    private static ToolRegistration optimizePlacement() {
        return ToolRegistration.builder()
                .name("design_optimize_placement")
                .title("Optimize Equinix metro placement")
                .description("Runs the Metro Optimizer engine: scores every Equinix metro against the given "
                        + "workloads, user sites, cloud requirements, and constraints (excluded/required "
                        + "metros, required-provider availability, redundancy, compliance, and a latency "
                        + "ceiling) and returns a ranked recommendation with per-dimension scores, reasons, "
                        + "workload placements, cost estimate, risk findings, and methodology. Cost is scored "
                        + "regionally and the budget is reported against rather than enforced. Read the "
                        + "risk_assessment findings and explanation.assumptions: they name every input the run "
                        + "could not honour. Use this before design_plan_deployment. Requires at least one "
                        + "workload.")
                .inputSchema(object(optimizationProps(), "workloads"))
                .outputSchema(looseObject("Ranked metro recommendations with scores, reasons, the workloads "
                        + "placed in each metro and why, cost estimate (with price provenance), risk "
                        + "assessment, and the optimizer's methodology."))
                .toolset(Toolset.DESIGN)
                .handler((args, ctx) -> optimizationPayload(runOptimizer(args, ctx), ctx.objectMapper()))
                .build();
    }

    private static OptimizationResult runOptimizer(JsonNode spec, ServerContext ctx) {
        MetroOptimizer.Builder builder = ctx.design().optimizeMetros();

        JsonNode workloads = spec.get("workloads");
        if (workloads == null || !workloads.isArray() || workloads.isEmpty()) {
            throw new IllegalArgumentException("'workloads' is required: give at least one workload object "
                    + "with a 'label' (and ideally a 'type' and 'bandwidth_mbps').");
        }
        for (JsonNode w : workloads) {
            MetroOptimizer.WorkloadBuilder wb = builder.addWorkload(requireString(w, "label"));
            optEnum(w, "type", WorkloadType.class).ifPresent(wb::type);
            optEnum(w, "latency_sensitivity", LatencySensitivity.class).ifPresent(wb::latencySensitivity);
            optInt(w, "bandwidth_mbps").ifPresent(wb::bandwidthMbps);
            optNumber(w, "max_latency_ms").ifPresent(wb::maxLatencyToleranceMs);
            for (String cloud : Args.stringList(w, "requires_clouds")) {
                wb.dependsOn(cloudProvider(cloud, "requires_clouds"));
            }
            wb.done();
        }

        JsonNode sites = spec.get("sites");
        if (sites != null && sites.isArray()) {
            for (JsonNode s : sites) {
                MetroOptimizer.SiteBuilder sb = builder.addSite(requireString(s, "label"));
                Optional<String> metroCode = optString(s, "metro_code");
                if (metroCode.isPresent()) {
                    sb.nearestMetro(metroCode.get());
                }
                else {
                    double lat = requireNumber(s, "latitude");
                    double lon = requireNumber(s, "longitude");
                    sb.coordinates(lat, lon);
                }
                optNumber(s, "weight").ifPresent(sb::weight);
                sb.done();
            }
        }

        for (String cloud : Args.stringList(spec, "require_clouds")) {
            builder.requireProvider(cloudProvider(cloud, "require_clouds")).done();
        }
        for (String cloud : Args.stringList(spec, "prefer_clouds")) {
            builder.preferProvider(cloudProvider(cloud, "prefer_clouds")).done();
        }
        optEnum(spec, "strategy", OptimizationStrategy.class).ifPresent(builder::strategy);

        JsonNode c = spec.get("constraints");
        if (c != null && c.isObject()) {
            MetroOptimizer.ConstraintsBuilder cb = builder.constraints();
            // Only the ceiling is accepted. BudgetRange.minMonthly is read by nothing in the engine —
            // it neither filters nor scores nor appears in any diagnostic — so exposing a
            // 'monthly_budget_min' knob asked the model to state a floor that could never bind.
            // The ceiling is kept because it does produce an observable answer
            // (CostEstimate.withinBudget), and its schema description says that is all it does.
            optNumber(c, "monthly_budget_max").ifPresent(max -> cb.monthlyBudget(0.0, max));
            List<String> required = Args.stringList(c, "require_metros");
            if (!required.isEmpty()) {
                cb.requireMetro(required.toArray(new String[0]));
            }
            List<String> excluded = Args.stringList(c, "exclude_metros");
            if (!excluded.isEmpty()) {
                cb.excludeMetro(excluded.toArray(new String[0]));
            }
            optEnum(c, "redundancy", RedundancyTier.class).ifPresent(cb::redundancy);
            optInt(c, "max_metros").ifPresent(cb::maxMetros);
            optNumber(c, "max_latency_ms").ifPresent(cb::maxLatencyMs);
            cb.done();
        }

        return builder.optimize();
    }

    private static ObjectNode optimizationPayload(OptimizationResult result, ObjectMapper mapper) {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("summary", result.toSummary());

        ArrayNode recommendations = payload.putArray("recommendations");
        List<MetroRecommendation> all = result.getRecommendations();
        for (MetroRecommendation rec : all.subList(0, Math.min(all.size(), MAX_RECOMMENDATIONS))) {
            ObjectNode r = recommendations.addObject();
            r.put("rank", rec.getRank());
            r.put("metro", String.valueOf(rec.getMetroId()));
            r.put("metro_name", rec.getMetroName());
            if (rec.getRegion() != null) {
                r.put("region", rec.getRegion().name());
            }
            if (rec.getScore() != null) {
                r.put("composite_score", round1(rec.getScore().getComposite()));
                ArrayNode components = r.putArray("score_components");
                rec.getScore().getComponents().forEach(sc -> {
                    ObjectNode cn = components.addObject();
                    cn.put("category", String.valueOf(sc.getCategory()));
                    cn.put("score", round1(sc.getScore()));
                    cn.put("weight", sc.getWeight());
                    cn.put("explanation", sc.getExplanation());
                });
            }
            ArrayNode reasons = r.putArray("reasons");
            if (rec.getReasons() != null) {
                rec.getReasons().forEach(reasons::add);
            }
            if (rec.getEstimatedCost() != null) {
                ObjectNode cost = r.putObject("estimated_cost");
                cost.put("monthly_recurring", rec.getEstimatedCost().getMonthlyRecurring());
                cost.put("non_recurring", rec.getEstimatedCost().getNonRecurring());
                cost.put("price_source", String.valueOf(rec.getEstimatedCost().getSource()));
            }
            // The workloads placed here, with the engine's rationale. This is the only place the
            // per-workload levers become visible: a workload's own max_latency_ms, its recorded
            // power/cooling requirements, and an unhonoured provider dependency are all stated on the
            // placement rationale. Omitting it left those levers invisible from the tool surface even
            // after the engine started honouring and disclosing them.
            if (rec.getAssignedWorkloads() != null && !rec.getAssignedWorkloads().isEmpty()) {
                ArrayNode placed = r.putArray("assigned_workloads");
                rec.getAssignedWorkloads().forEach(wp -> {
                    ObjectNode w = placed.addObject();
                    w.put("workload", wp.getWorkloadLabel());
                    w.put("reasoning", wp.getReasoning());
                });
            }
        }
        if (all.size() > MAX_RECOMMENDATIONS) {
            payload.put("truncated", true);
            payload.put("truncation_note",
                    "Showing top " + MAX_RECOMMENDATIONS + " of " + all.size() + " scored metros.");
        }

        CostEstimate cost = result.getCostEstimate();
        if (cost != null) {
            ObjectNode c = payload.putObject("cost_estimate");
            c.put("monthly_total", cost.getMonthlyTotal());
            c.put("setup_total", cost.getSetupTotal());
            c.put("currency", cost.getCurrency());
            c.put("within_budget", cost.isWithinBudget());
            c.put("price_source", String.valueOf(cost.getSource()));
            c.put("disclaimer", cost.getCostDisclaimer());
        }

        if (result.getRiskAssessment() != null) {
            ObjectNode risk = payload.putObject("risk_assessment");
            risk.put("overall_severity", String.valueOf(result.getRiskAssessment().getOverallSeverity()));
            risk.put("resiliency_score", round1(result.getRiskAssessment().getResiliencyScore()));
            ArrayNode findings = risk.putArray("findings");
            List<api.equinix.javasdk.design.optimizer.model.RiskFinding> riskFindings =
                    result.getRiskAssessment().getFindings();
            if (riskFindings != null) {
                riskFindings.stream().limit(MAX_FINDINGS).forEach(f -> {
                    ObjectNode fn = findings.addObject();
                    fn.put("severity", String.valueOf(f.getSeverity()));
                    fn.put("category", f.getCategory());
                    fn.put("description", f.getDescription());
                    fn.put("recommendation", f.getRecommendation());
                });
            }
        }

        if (result.getExplanation() != null) {
            ObjectNode explanation = payload.putObject("explanation");
            explanation.put("methodology", result.getExplanation().getMethodology());
            ArrayNode assumptions = explanation.putArray("assumptions");
            if (result.getExplanation().getAssumptions() != null) {
                result.getExplanation().getAssumptions().forEach(assumptions::add);
            }
            explanation.put("data_freshness", result.getExplanation().getDataFreshness());
            explanation.put("human_readable", result.getExplanation().getHumanReadable());
        }
        if (result.getComputedAt() != null) {
            payload.put("computed_at", result.getComputedAt().toString());
        }
        payload.put("compute_time_ms", result.getComputeTimeMs());
        return payload;
    }

    // ── design_plan_deployment ──────────────────────────────────────────────

    private static ToolRegistration planDeployment() {
        Map<String, Object> deployment = object(props(
                "customer_asn", integer("Your BGP ASN, stamped on every planned BGP routing protocol "
                        + "(e.g. 65001). Defaults to 65100 when omitted."),
                "router_package", stringEnum("Fabric Cloud Router package code. Case-insensitive; any "
                                + "other value is rejected with an error naming the valid codes.",
                        "LAB", "BASIC", "STANDARD", "ADVANCED", "PREMIUM"),
                "router_name_prefix", string("Name prefix for the planned Cloud Routers and every "
                        + "connection derived from them ('{prefix}-{metro}'). Defaults to 'FCR'."),
                "backbone_topology", stringEnum("Inter-metro backbone shape. Only applies when the "
                                + "optimization recommends more than one metro; with a single metro no backbone "
                                + "link is planned at all.",
                        lowerNames(BackboneTopology.class)),
                "backbone_bandwidth_mbps", integer("Bandwidth for each inter-metro backbone link, in Mbps. "
                        + "Defaults to 10000. Applies only to backbone links, never to provider connections, "
                        + "which are sized from the workloads placed at each metro."),
                "project_id", string("Equinix Fabric project id recorded on each planned Cloud Router."),
                "account_number", integer("Equinix billing account number recorded on each planned Cloud "
                        + "Router."),
                "notifications", array("Email addresses for provisioning notifications. Only the FIRST "
                                + "address is used — a planned Fabric resource carries a single notification "
                                + "address — so put the address you want on the plan first.",
                        string("Email address."))));
        return ToolRegistration.builder()
                .name("design_plan_deployment")
                .title("Plan a deployment (no execution)")
                .description("Runs the Metro Optimizer and then the Deployment Wizard in PLAN-ONLY mode: "
                        + "produces a reviewable deployment plan (Cloud Routers, provider connections, "
                        + "backbone links, routing protocols) with aggregated pricing and validation "
                        + "findings. NOTHING is provisioned — this tool never executes a plan. Pricing is "
                        + "not term-scoped: the rate cards this server reads resolve by product, bandwidth "
                        + "and metro only, so no contract term is accepted or applied. The returned plan_id "
                        + "can be passed to design_export_terraform while this server process is running "
                        + "(plans are held in memory for 30 minutes).")
                .inputSchema(object(props(
                                "optimization", object(optimizationProps(), "workloads"),
                                "deployment", deployment),
                        "optimization"))
                .outputSchema(looseObject("The serialized deployment plan: plan_id, planned resources, "
                        + "pricing with provenance, and validation errors/warnings."))
                .toolset(Toolset.DESIGN)
                .handler(DesignToolFactory::handlePlanDeployment)
                .build();
    }

    private static ObjectNode handlePlanDeployment(JsonNode args, ServerContext ctx) {
        JsonNode optimization = args.get("optimization");
        if (optimization == null || !optimization.isObject()) {
            throw new IllegalArgumentException("'optimization' is required: the same specification "
                    + "design_optimize_placement takes (workloads, sites, clouds, constraints).");
        }
        OptimizationResult optimized = runOptimizer(optimization, ctx);
        if (optimized.getRecommendations() == null || optimized.getRecommendations().isEmpty()) {
            throw new IllegalStateException("The optimizer found no viable metros for this specification, "
                    + "so there is nothing to plan. Relax the constraints and try again.");
        }

        DeploymentWizard.Builder wizard = ctx.design().deploymentWizard(optimized);
        JsonNode d = args.get("deployment");
        if (d != null && d.isObject()) {
            optLong(d, "customer_asn").ifPresent(wizard::customerAsn);
            optString(d, "router_package").ifPresent(wizard::routerPackage);
            optString(d, "router_name_prefix").ifPresent(wizard::routerNamePrefix);
            optEnum(d, "backbone_topology", BackboneTopology.class).ifPresent(wizard::backboneTopology);
            optInt(d, "backbone_bandwidth_mbps").ifPresent(wizard::backboneBandwidthMbps);
            optString(d, "project_id").ifPresent(wizard::projectId);
            optLong(d, "account_number").ifPresent(wizard::accountNumber);
            List<String> notifications = Args.stringList(d, "notifications");
            if (!notifications.isEmpty()) {
                wizard.notifications(notifications.toArray(new String[0]));
            }
            // No 'term' knob: see the note on handleEstimateTco. The wizard prices through
            // EquinixRateCard, which resolves a price row by product/bandwidth/metro and never by
            // term, so a term accepted here would change no figure in the returned plan.
        }

        DeploymentPlan plan = wizard.plan();
        String planId = ctx.planStore().put(plan);
        return planPayload(plan, planId, ctx);
    }

    private static ObjectNode planPayload(DeploymentPlan plan, String planId, ServerContext ctx) {
        ObjectMapper mapper = ctx.objectMapper();
        ObjectNode payload = mapper.createObjectNode();
        payload.put("plan_id", planId);
        payload.put("plan_id_scope", "Valid only inside this MCP server process, for "
                + ctx.planStore().ttl().toMinutes() + " minutes. Pass it to design_export_terraform.");
        payload.put("executed", false);
        payload.put("summary", plan.toSummary());

        ArrayNode routers = payload.putArray("cloud_routers");
        if (plan.getCloudRouters() != null) {
            plan.getCloudRouters().forEach(cr -> {
                ObjectNode r = routers.addObject();
                r.put("name", cr.getName());
                r.put("metro", String.valueOf(cr.getMetroId()));
                r.put("package", String.valueOf(cr.getPackageCode()));
                r.put("project_id", cr.getProjectId());
            });
        }

        ArrayNode connections = payload.putArray("provider_connections");
        if (plan.getProviderConnections() != null) {
            plan.getProviderConnections().forEach(pc -> {
                ObjectNode cn = connections.addObject();
                cn.put("name", pc.getName());
                cn.put("bandwidth_mbps", pc.getBandwidthMbps());
                cn.put("metro", String.valueOf(pc.getASideMetro()));
                cn.put("provider", pc.getZSideProviderLabel());
                cn.put("seller_region", pc.getZSideSellerRegion());
                cn.put("router", pc.getASideRouterName());
            });
        }

        ArrayNode backbone = payload.putArray("backbone_links");
        if (plan.getBackboneLinks() != null) {
            plan.getBackboneLinks().forEach(bl -> {
                ObjectNode ln = backbone.addObject();
                ln.put("name", bl.getName());
                ln.put("metro_a", String.valueOf(bl.getMetroA()));
                ln.put("metro_z", String.valueOf(bl.getMetroZ()));
                ln.put("bandwidth_mbps", bl.getBandwidthMbps());
            });
        }

        payload.put("routing_protocol_count",
                plan.getRoutingProtocols() == null ? 0 : plan.getRoutingProtocols().size());

        PlanPricing pricing = plan.getPricing();
        if (pricing != null) {
            ObjectNode p = payload.putObject("pricing");
            p.put("monthly_total", pricing.getMonthlyTotal());
            p.put("setup_total", pricing.getSetupTotal());
            p.put("currency", pricing.getCurrency());
            p.put("router_monthly", pricing.getRouterMonthlyCost());
            p.put("provider_connection_monthly", pricing.getProviderConnectionMonthlyCost());
            p.put("backbone_monthly", pricing.getBackboneMonthlyCost());
            p.put("price_source", String.valueOf(pricing.getSource()));
            p.put("disclaimer", pricing.getDisclaimer());
        }

        ArrayNode validation = payload.putArray("validation_errors");
        if (plan.getValidationErrors() != null) {
            plan.getValidationErrors().forEach(validation::add);
        }
        return payload;
    }

    // ── design_estimate_latency ─────────────────────────────────────────────

    private static ToolRegistration estimateLatency() {
        return ToolRegistration.builder()
                .name("design_estimate_latency")
                .title("Estimate metro/IBX latency")
                .description("Estimates fibre latency between two Equinix endpoints using speed-of-light "
                        + "geometry over the live metro catalogue: great-circle distance between the two "
                        + "endpoints' coordinates × the refractive index of glass fibre. Endpoints are "
                        + "metro codes (e.g. 'DC', 'LD') or IBX data-center codes (e.g. 'DC11'). This is a "
                        + "physics lower bound, not a measured path — real routes are longer.")
                .inputSchema(object(props(
                                "from", string("Origin endpoint: an Equinix metro code (e.g. 'DC') or IBX code (e.g. 'DC11')."),
                                "to", string("Destination endpoint: metro code or IBX code."),
                                "mode", stringEnum("round_trip (RTT, default) or one_way.", "round_trip", "one_way")),
                        "from", "to"))
                .outputSchema(looseObject("Distance in km, estimated latency in ms, the calculation method, "
                        + "and its caveats."))
                .toolset(Toolset.DESIGN)
                .handler(DesignToolFactory::handleEstimateLatency)
                .build();
    }

    private static ObjectNode handleEstimateLatency(JsonNode args, ServerContext ctx) {
        String fromCode = requireString(args, "from");
        String toCode = requireString(args, "to");
        boolean oneWay = optString(args, "mode").map(m -> m.equalsIgnoreCase("one_way")).orElse(false);

        MetroRegistry registry = ctx.metroRegistry();
        Endpoint from = resolveEndpoint(registry, fromCode, "from");
        Endpoint to = resolveEndpoint(registry, toCode, "to");

        SpeedOfLightLatency latency = oneWay ? SpeedOfLightLatency.oneWay() : SpeedOfLightLatency.roundTrip();
        double km;
        double ms;
        if (from.metro != null && to.metro != null) {
            km = SpeedOfLightLatency.distanceKm(from.metro, to.metro);
            ms = latency.millisBetween(from.metro, to.metro);
        }
        else if (from.ibx != null && to.ibx != null) {
            km = SpeedOfLightLatency.distanceKm(from.ibx, to.ibx);
            ms = latency.millisBetween(from.ibx, to.ibx);
        }
        else if (from.ibx != null) {
            km = SpeedOfLightLatency.distanceKm(from.ibx, to.metro);
            ms = latency.millisBetween(from.ibx, to.metro);
        }
        else {
            km = SpeedOfLightLatency.distanceKm(from.metro, to.ibx);
            ms = latency.millisBetween(from.metro, to.ibx);
        }

        ObjectNode payload = ctx.objectMapper().createObjectNode();
        payload.set("from", from.describe(ctx.objectMapper()));
        payload.set("to", to.describe(ctx.objectMapper()));
        payload.put("distance_km", Math.round(km * 10.0) / 10.0);
        payload.put("estimated_latency_ms", Math.round(ms * 100.0) / 100.0);
        payload.put("mode", oneWay ? "one_way" : "round_trip");
        payload.put("method", "Great-circle (haversine) distance between endpoint coordinates, over fibre "
                + "with refractive index " + SpeedOfLightLatency.DEFAULT_FIBER_REFRACTIVE_INDEX + ".");
        ArrayNode caveats = payload.putArray("caveats");
        caveats.add("This is a straight-fibre physics lower bound; real routes add path length and equipment latency.");
        caveats.add("Metro endpoints use the metro centroid; IBX endpoints use the building's coordinates.");
        caveats.add("For contractual latency figures use Equinix's published Fabric latency statistics.");
        return payload;
    }

    private static Endpoint resolveEndpoint(MetroRegistry registry, String code, String field) {
        Optional<Metro> metro = registry.get(code.toUpperCase(Locale.ROOT));
        if (metro.isPresent()) {
            return Endpoint.ofMetro(metro.get());
        }
        Optional<Ibx> ibx = registry.ibx(code.toUpperCase(Locale.ROOT));
        if (ibx.isPresent()) {
            return Endpoint.ofIbx(ibx.get());
        }
        throw new IllegalArgumentException("'" + field + "' value '" + code + "' is neither a known metro code "
                + "nor a known IBX code. Metro codes are two letters (e.g. 'DC'); IBX codes are metro code + "
                + "number (e.g. 'DC11'). IBX coordinates require the enriched registry — if IBX codes keep "
                + "failing, use metro codes.");
    }

    private static final class Endpoint {
        private final Metro metro;
        private final Ibx ibx;

        private Endpoint(Metro metro, Ibx ibx) {
            this.metro = metro;
            this.ibx = ibx;
        }

        static Endpoint ofMetro(Metro metro) {
            return new Endpoint(metro, null);
        }

        static Endpoint ofIbx(Ibx ibx) {
            return new Endpoint(null, ibx);
        }

        ObjectNode describe(ObjectMapper mapper) {
            ObjectNode node = mapper.createObjectNode();
            if (metro != null) {
                node.put("kind", "metro");
                node.put("code", String.valueOf(metro.getCode()));
                node.put("name", metro.getName());
            }
            else {
                node.put("kind", "ibx");
                node.put("code", ibx.getIbxCode());
                node.put("metro", ibx.getMetroCode());
            }
            return node;
        }
    }

    // ── design_estimate_tco ─────────────────────────────────────────────────

    private static ToolRegistration estimateTco() {
        return ToolRegistration.builder()
                .name("design_estimate_tco")
                .title("Estimate total cost of ownership")
                .description("Runs the TCO engine over the layered rate-card machinery: models the monthly "
                        + "and setup cost of the same workload across deployment archetypes — public cloud "
                        + "over the internet, on-prem, and Equinix private interconnect — and reports each "
                        + "line item with its price provenance (live Equinix pricing vs reference figures). "
                        + "Give the monthly egress volume and the cloud it leaves from. Figures are not "
                        + "term-scoped: the rate cards this server reads (live Fabric prices, then reference "
                        + "figures) resolve by product, bandwidth, metro and region only, so no contract term "
                        + "is accepted or applied.")
                .inputSchema(object(props(
                                "monthly_egress_gb", number("Data leaving the cloud per month, in GB."),
                                "cloud", stringEnum("The cloud provider the egress leaves from.", CLOUD_VALUES),
                                "region", string("The provider region the egress leaves from, e.g. 'us-east-1'."),
                                "metro_code", string("Equinix metro for the interconnect side, e.g. 'DC'. Used "
                                        + "to prefer a metro-specific price row where the live catalogue "
                                        + "publishes one; without it (or without such a row) the same-bandwidth "
                                        + "price is used whatever the metro."),
                                "bandwidth_mbps", integer("Interconnect bandwidth in Mbps, default 1000. Prices "
                                        + "the Equinix connection AND sizes the on-prem archetype's carrier IP "
                                        + "transit line item, so it moves both sides of the comparison."),
                                "cloud_router_package", string("Include a Fabric Cloud Router of this package "
                                        + "code (e.g. 'STANDARD') in the Equinix archetype. Skipped silently if "
                                        + "no rate card can price that code."),
                                "power_kw", number("On-prem power draw in kW, default 5. Used by the on-prem "
                                        + "archetype only; it is ignored if 'archetypes' excludes on_prem."),
                                "archetypes", array("Restrict the comparison to these archetypes.",
                                        stringEnum("Deployment archetype.", lowerNames(DeploymentArchetype.class))),
                                "on_prem_transit_per_mbps_month", number("Override: on-prem IP transit $ per Mbps per month."),
                                "on_prem_hardware_monthly", number("Override: amortized on-prem hardware $ per month."),
                                "on_prem_cross_connect_monthly", number("Override: on-prem cross-connect $ per month."),
                                "on_prem_power_per_kw_month", number("Override: on-prem power $ per kW per month.")),
                        "monthly_egress_gb", "cloud", "region"))
                .outputSchema(looseObject("Per-archetype cost breakdowns with line items and price provenance, "
                        + "the recommended archetype, and savings vs the baseline."))
                .toolset(Toolset.DESIGN)
                .handler(DesignToolFactory::handleEstimateTco)
                .build();
    }

    private static ObjectNode handleEstimateTco(JsonNode args, ServerContext ctx) {
        TcoCalculator.Builder builder = ctx.design().tcoComparison()
                .egress(requireNumber(args, "monthly_egress_gb"),
                        api.equinix.javasdk.design.value.savings.DataUnit.GIGABYTE)
                .fromCloud(cloudProvider(requireString(args, "cloud"), "cloud"))
                .inRegion(requireString(args, "region"));
        optString(args, "metro_code").ifPresent(code -> builder.viaMetro(metroCode(code)));
        optInt(args, "bandwidth_mbps").ifPresent(builder::bandwidthMbps);
        // No 'term' knob. TcoCalculator.term() only binds against a CustomRateCard, and this server
        // always prices through RateCard.standardChain (EquinixRateCard, then ReferenceRateCard) —
        // neither of which resolves a rate by term. Accepting a term here would have let the model
        // report "priced at a 36-month term" over figures identical to the 12-month ones. Reinstate
        // it (schema field + builder::term) only once a rate card in the MCP chain resolves by term.
        optString(args, "cloud_router_package").ifPresent(builder::includeCloudRouter);
        optNumber(args, "power_kw").ifPresent(builder::powerKw);
        List<String> archetypes = Args.stringList(args, "archetypes");
        if (!archetypes.isEmpty()) {
            builder.archetypes(archetypes.stream()
                    .map(a -> enumValue(DeploymentArchetype.class, a, "archetypes"))
                    .toArray(DeploymentArchetype[]::new));
        }
        optNumber(args, "on_prem_transit_per_mbps_month")
                .ifPresent(v -> builder.onPremTransitPerMbpsMonth(BigDecimal.valueOf(v)));
        optNumber(args, "on_prem_hardware_monthly")
                .ifPresent(v -> builder.onPremHardwareMonthly(BigDecimal.valueOf(v)));
        optNumber(args, "on_prem_cross_connect_monthly")
                .ifPresent(v -> builder.onPremCrossConnectMonthly(BigDecimal.valueOf(v)));
        optNumber(args, "on_prem_power_per_kw_month")
                .ifPresent(v -> builder.onPremPowerPerKwMonth(BigDecimal.valueOf(v)));

        TcoComparison comparison = builder.compare();

        ObjectNode payload = ctx.objectMapper().createObjectNode();
        ArrayNode breakdowns = payload.putArray("breakdowns");
        if (comparison.getBreakdowns() != null) {
            comparison.getBreakdowns().forEach(b -> {
                ObjectNode bn = breakdowns.addObject();
                bn.put("archetype", String.valueOf(b.getArchetype()));
                bn.put("monthly_total", b.getMonthlyTotal());
                bn.put("setup_total", b.getSetupTotal());
                bn.put("currency", b.getCurrency());
                bn.put("fully_priced", b.isPriced());
                ObjectNode items = bn.putObject("line_items");
                if (b.getLineItems() != null) {
                    b.getLineItems().forEach(items::put);
                }
                bn.put("note", b.getNote());
            });
        }
        payload.put("recommended_archetype", String.valueOf(comparison.getRecommended()));
        payload.put("baseline_archetype", String.valueOf(comparison.getBaseline()));
        payload.put("monthly_savings_vs_baseline", comparison.getMonthlySavingsVsBaseline());
        payload.put("annual_savings_vs_baseline", comparison.getAnnualSavingsVsBaseline());
        payload.put("currency", comparison.getCurrency());
        payload.put("as_of", comparison.getAsOf());
        payload.put("disclaimer", comparison.getDisclaimer());
        payload.put("price_provenance_note", "Each breakdown's 'note' names its price sources; "
                + "'fully_priced' is false where reference figures filled gaps.");
        return payload;
    }

    // ── design_compare_cloud_egress ─────────────────────────────────────────

    private static ToolRegistration compareCloudEgress() {
        return ToolRegistration.builder()
                .name("design_compare_cloud_egress")
                .title("Compare cloud egress: internet vs private interconnect")
                .description("Runs the Savings Calculator with LIVE provider pricing where available: fetches "
                        + "the named cloud's current internet-egress rate (AWS/Azure/OCI public price APIs; "
                        + "GCP when a GCP_BILLING_API_KEY is configured), compares it against the same "
                        + "volume over Equinix private interconnect, and reports monthly/annual savings, "
                        + "break-even volume, and payback. Live lookups run under a hard timeout — when a "
                        + "provider API is slow or down the result degrades to reference rates and names "
                        + "the provider that failed instead of hanging. Figures are not term-scoped: no rate "
                        + "card in this server's chain resolves a price by contract term, so none is "
                        + "accepted.")
                .inputSchema(object(props(
                                "monthly_egress_gb", number("Data leaving the cloud per month, in GB."),
                                "cloud", stringEnum("The cloud provider to compare.", CLOUD_VALUES),
                                "region", string("The provider region the egress leaves from, e.g. 'us-east-1'. "
                                        + "Drives the live egress-rate lookup, so give the real region."),
                                "metro_code", string("Equinix metro for the interconnect side, e.g. 'DC'. Used "
                                        + "to prefer a metro-specific Fabric price row where one is published; "
                                        + "without it the same-bandwidth price is used whatever the metro."),
                                "bandwidth_mbps", integer("Interconnect bandwidth to price, in Mbps. Default 1000."),
                                "cloud_router_package", string("Also price a Fabric Cloud Router of this package "
                                        + "code. Skipped silently if no rate card can price that code.")),
                        "monthly_egress_gb", "cloud", "region"))
                .outputSchema(looseObject("The savings estimate (rates, monthly costs, net savings, break-even, "
                        + "payback) plus live-pricing provenance and any degraded providers."))
                .toolset(Toolset.DESIGN)
                .openWorld(true)
                .handler(DesignToolFactory::handleCompareCloudEgress)
                .build();
    }

    private static ObjectNode handleCompareCloudEgress(JsonNode args, ServerContext ctx) {
        CloudProviderType provider = cloudProvider(requireString(args, "cloud"), "cloud");
        Optional<RateCard> liveCard = ctx.providerRateCard(provider);
        TimeoutRateCard guarded = liveCard
                .map(card -> new TimeoutRateCard(provider.name(), card, ctx.pricingTimeoutMillis()))
                .orElse(null);

        RateCard chain = guarded != null
                ? RateCard.layered(guarded, RateCard.standardChain(ctx.fabric()))
                : RateCard.standardChain(ctx.fabric());

        try {
            SavingsCalculator.Builder builder = ctx.design().savingsCalculator()
                    .egressGigabytes(requireNumber(args, "monthly_egress_gb"))
                    .fromCloud(provider)
                    .inRegion(requireString(args, "region"))
                    .rateCard(chain);
            optString(args, "metro_code").ifPresent(code -> builder.viaMetro(metroCode(code)));
            optInt(args, "bandwidth_mbps").ifPresent(builder::bandwidthMbps);
            optString(args, "cloud_router_package").ifPresent(builder::includeCloudRouter);
            // No 'term' knob: see the note on handleEstimateTco. Neither the live provider cards nor
            // the Equinix/reference chain resolves a rate by term, so it could change no figure here.

            SavingsEstimate estimate = builder.calculate();

            ObjectNode payload = ctx.objectMapper().createObjectNode();
            payload.put("provider", provider.name());
            payload.put("region", estimate.getRegion());
            payload.put("monthly_egress_gb", estimate.getMonthlyEgressGb());
            payload.put("internet_rate_per_gb", estimate.getInternetRatePerGb());
            payload.put("private_rate_per_gb", estimate.getPrivateRatePerGb());
            payload.put("internet_egress_monthly_cost", estimate.getInternetEgressMonthlyCost());
            payload.put("private_egress_monthly_cost", estimate.getPrivateEgressMonthlyCost());
            payload.put("monthly_egress_savings", estimate.getMonthlyEgressSavings());
            payload.put("equinix_monthly_cost", estimate.getEquinixMonthlyCost());
            payload.put("equinix_setup_cost", estimate.getEquinixSetupCost());
            payload.put("net_monthly_savings", estimate.getNetMonthlySavings());
            payload.put("annual_net_savings", estimate.getAnnualNetSavings());
            payload.put("first_year_net_savings", estimate.getFirstYearNetSavings());
            payload.put("break_even_gb_per_month", estimate.getBreakEvenGbPerMonth());
            payload.put("payback_months", estimate.getPaybackMonths());
            payload.put("currency", estimate.getCurrency());
            payload.put("complete", estimate.isComplete());
            payload.put("disclaimer", estimate.getDisclaimer());

            ObjectNode live = payload.putObject("live_pricing");
            live.put("attempted", guarded != null);
            if (guarded == null) {
                live.put("note", provider == CloudProviderType.GOOGLE_CLOUD
                        ? "No live GCP adapter: set GCP_BILLING_API_KEY to enable the Cloud Billing "
                        + "Catalog lookup. Reference rates were used."
                        : "No live pricing adapter exists for " + provider.name() + "; reference rates were used.");
            }
            else if (guarded.degraded()) {
                live.put("degraded", true);
                ArrayNode failures = live.putArray("failures");
                guarded.failures().forEach(failures::add);
                live.put("note", "Live " + provider.name() + " pricing was unavailable within "
                        + ctx.pricingTimeoutMillis() + " ms; the estimate fell back to reference rates.");
            }
            else {
                live.put("degraded", false);
            }
            return payload;
        }
        finally {
            if (guarded != null) {
                guarded.shutdown();
            }
        }
    }

    // ── design_analyze_peering ──────────────────────────────────────────────

    private static ToolRegistration analyzePeering() {
        return ToolRegistration.builder()
                .name("design_analyze_peering")
                .title("Analyze peering intelligence for ASNs")
                .description("Runs the Peering Intelligence engine: cross-references the given ASNs against "
                        + "PeeringDB and the Equinix IX footprint to report where each network is present, "
                        + "concrete peering opportunities (shared IXes per metro), and a resiliency "
                        + "assessment. Two of the three sections are conditional: peering_opportunities is "
                        + "empty without 'customer_asn', and resiliency is absent without 'metros'. Uses the "
                        + "EQUINIX_PEERINGDB_KEY environment variable when configured (higher PeeringDB rate "
                        + "limits); anonymous otherwise.")
                .inputSchema(object(props(
                                "asns", array("The ASNs to analyze (at least one), e.g. [13335, 15169].",
                                        integer("An autonomous system number.")),
                                "metros", array("The metros where YOU are present. These do not restrict the "
                                                + "analysis — the ASNs are reported wherever they are present — they "
                                                + "add your metros to the presence matrix and are the metros the "
                                                + "resiliency assessment measures blast radius and diversity across. "
                                                + "Without them no resiliency assessment is produced.",
                                        string("Metro code, e.g. 'FR'.")),
                                "customer_asn", integer("Your own ASN. Peering opportunities are computed from "
                                        + "your side, so without it the peering_opportunities list is empty.")),
                        "asns"))
                .outputSchema(looseObject("Per-ASN presence, peering opportunities (when customer_asn is "
                        + "given), resiliency assessment (when metros are given), and the data sources "
                        + "consulted."))
                .toolset(Toolset.DESIGN)
                .openWorld(true)
                .handler(DesignToolFactory::handleAnalyzePeering)
                .build();
    }

    private static ObjectNode handleAnalyzePeering(JsonNode args, ServerContext ctx) {
        JsonNode asns = args.get("asns");
        if (asns == null || !asns.isArray() || asns.isEmpty()) {
            throw new IllegalArgumentException("'asns' is required: an array with at least one ASN, e.g. [13335].");
        }

        PeeringIntelligence.Builder builder = ctx.env(ServerContext.ENV_PEERINGDB_KEY)
                .map(key -> PeeringIntelligence.builder(ctx.fabric(), key))
                .orElseGet(() -> PeeringIntelligence.builder(ctx.fabric()));

        for (JsonNode asn : asns) {
            if (!asn.isNumber()) {
                throw new IllegalArgumentException("'asns' entries must be numbers, e.g. [13335, 15169].");
            }
            builder.addAsn(asn.asLong());
        }
        List<String> metros = Args.stringList(args, "metros");
        if (!metros.isEmpty()) {
            builder.customerMetros(metros.toArray(new String[0]));
        }
        optLong(args, "customer_asn").ifPresent(builder::customerAsn);
        // No 'include_fabric_connections' knob. PeeringIntelligence.Builder accepts the flag and
        // PeeringRequest stores it, but PeeringIntelligenceEngine reads it nowhere: no Fabric
        // connection lookup exists behind it, so the tool advertised "extra Equinix API calls" that
        // were never made and a section that could never appear. includeResiliency IS read (it gates
        // the resiliency assessment, which also needs customerMetros).
        builder.includeCapacity(true)
                .includePolicies(true)
                .includeResiliency(true);

        PeeringIntelligenceResult result = builder.analyze();

        ObjectMapper mapper = ctx.objectMapper();
        ObjectNode payload = mapper.createObjectNode();

        ArrayNode networks = payload.putArray("networks");
        if (result.getNetworkPresences() != null) {
            result.getNetworkPresences().values().forEach(np -> {
                ObjectNode n = networks.addObject();
                n.put("asn", np.getAsn());
                n.put("label", np.getLabel());
                n.put("peeringdb_name", np.getPeeringDbName());
                n.put("peering_policy", String.valueOf(np.getPeeringPolicy()));
                n.put("network_type", String.valueOf(np.getNetworkType()));
                n.put("traffic_volume", np.getTrafficVolume());
                n.put("traffic_ratio", np.getTrafficRatio());
                n.put("route_server_participant", np.isRouteServerParticipant());
                n.put("ipv6_capable", np.isIpv6Capable());
            });
        }

        ArrayNode opportunities = payload.putArray("peering_opportunities");
        if (result.getPeeringOpportunities() != null) {
            List<api.equinix.javasdk.design.peering.model.PeeringOpportunity> all = result.getPeeringOpportunities();
            all.stream().limit(MAX_OPPORTUNITIES).forEach(op -> {
                ObjectNode o = opportunities.addObject();
                o.put("customer_asn", op.getCustomerAsn());
                o.put("target_asn", op.getTargetAsn());
                o.put("target_label", op.getTargetLabel());
                o.put("metro", String.valueOf(op.getMetro()));
                o.put("ix_name", op.getIxName());
                o.put("target_policy", String.valueOf(op.getTargetPolicy()));
                o.put("target_uses_route_server", op.isTargetUsesRouteServer());
            });
            if (all.size() > MAX_OPPORTUNITIES) {
                payload.put("truncated", true);
                payload.put("truncation_note", "Showing " + MAX_OPPORTUNITIES + " of " + all.size()
                        + " peering opportunities.");
            }
        }

        if (result.getResiliency() != null) {
            ObjectNode resiliency = payload.putObject("resiliency");
            resiliency.put("overall_score", round1(result.getResiliency().getOverallScore()));
            resiliency.put("overall_rating", result.getResiliency().getOverallRating());
            ArrayNode findings = resiliency.putArray("findings");
            if (result.getResiliency().getFindings() != null) {
                result.getResiliency().getFindings().stream().limit(MAX_FINDINGS).forEach(findings::add);
            }
        }

        ArrayNode sources = payload.putArray("data_sources");
        if (result.getDataSources() != null) {
            result.getDataSources().forEach(sources::add);
        }
        if (result.getComputedAt() != null) {
            payload.put("computed_at", result.getComputedAt().toString());
        }
        return payload;
    }

    // ── design_export_terraform ─────────────────────────────────────────────

    private static ToolRegistration exportTerraform() {
        return ToolRegistration.builder()
                .name("design_export_terraform")
                .title("Export a planned deployment as Terraform")
                .description("Renders the deployment plan identified by plan_id (from a previous "
                        + "design_plan_deployment call in THIS server process) as Terraform HCL using the "
                        + "official equinix/equinix provider — Cloud Routers, connections, and routing "
                        + "protocols as infrastructure-as-code. Plans are held in memory for 30 minutes; "
                        + "if the id has expired, re-run design_plan_deployment first.")
                .inputSchema(object(props(
                                "plan_id", string("The plan_id returned by design_plan_deployment.")),
                        "plan_id"))
                .outputSchema(looseObject("The Terraform HCL for the plan, plus the plan_id and scope note."))
                .toolset(Toolset.DESIGN)
                .handler(DesignToolFactory::handleExportTerraform)
                .build();
    }

    private static ObjectNode handleExportTerraform(JsonNode args, ServerContext ctx) {
        String planId = requireString(args, "plan_id");
        DeploymentPlan plan = ctx.planStore().get(planId).orElseThrow(() -> new IllegalArgumentException(
                "plan_id '" + planId + "' is unknown, expired, or from another server process. Plans live "
                        + "in this process's memory for " + ctx.planStore().ttl().toMinutes() + " minutes — "
                        + "run design_plan_deployment again and use the fresh plan_id."));
        String hcl = new TerraformExporter().export(plan);
        ObjectNode payload = ctx.objectMapper().createObjectNode();
        payload.put("plan_id", planId);
        payload.put("format", "terraform-hcl");
        payload.put("resource_count", plan.totalResourceCount());
        payload.put("terraform", hcl);
        payload.put("note", "Review before applying; the plan was produced from an optimization estimate "
                + "and has not been provisioned by this server.");
        return payload;
    }

    // ── shared helpers ──────────────────────────────────────────────────────

    private static CloudProviderType cloudProvider(String raw, String field) {
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        if (normalized.equals("gcp") || normalized.equals("google")) {
            return CloudProviderType.GOOGLE_CLOUD;
        }
        if (normalized.equals("oci") || normalized.equals("oracle")) {
            return CloudProviderType.ORACLE_CLOUD;
        }
        return enumValue(CloudProviderType.class, raw, field);
    }

    private static MetroCode metroCode(String code) {
        try {
            return MetroCode.fromCode(code);
        }
        catch (RuntimeException e) {
            throw new IllegalArgumentException("'" + code + "' is not a known Equinix metro code. Use the "
                    + "two-letter code, e.g. 'DC' (Ashburn), 'LD' (London), 'SG' (Singapore).");
        }
    }

    private static double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
