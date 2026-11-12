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

package com.eqixiac.equinix.mcp.server;

import com.eqixiac.equinix.design.estate.EstateAnalyzer;
import com.eqixiac.equinix.design.estate.EstateFinding;
import com.eqixiac.equinix.design.estate.EstateReport;
import com.eqixiac.equinix.design.estate.EstateScope;
import com.eqixiac.equinix.design.estate.FinancialImpact;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import static com.eqixiac.equinix.mcp.server.DesignPayloads.Detail;
import static com.eqixiac.equinix.mcp.server.DesignPayloads.capped;
import static com.eqixiac.equinix.mcp.server.DesignPayloads.money;
import static com.eqixiac.equinix.mcp.server.Schemas.array;
import static com.eqixiac.equinix.mcp.server.Schemas.detail;
import static com.eqixiac.equinix.mcp.server.Schemas.integer;
import static com.eqixiac.equinix.mcp.server.Schemas.object;
import static com.eqixiac.equinix.mcp.server.Schemas.props;
import static com.eqixiac.equinix.mcp.server.Schemas.string;
import static com.eqixiac.equinix.mcp.server.Schemas.with;

/**
 * {@code design_analyze_estate}: the read-only analysis of what an account already runs, in its own
 * {@link Toolset#ESTATE} toolset because, unlike the design tools, it reads the customer's inventory.
 *
 * <p><strong>Always scoped.</strong> The credentials behind a server can typically see resources of many
 * owners (the account this was built against sees thousands of ports and hundreds of connections across
 * dozens of projects). The tool therefore requires explicit project ids and has no way to ask for the
 * whole account; the analyser re-checks the scope client-side because the Fabric connection search was
 * observed (2026-11-12) to ignore its project filter.</p>
 */
final class EstateTools {

    /** Most projects one call may scope. */
    static final int MAX_PROJECTS = 10;

    /** Default and largest per-type resource ceiling. */
    static final int DEFAULT_MAX_PER_TYPE = 500;
    static final int MAX_MAX_PER_TYPE = EstateScope.DEFAULT_MAX_RESOURCES_PER_TYPE;

    private static final Pattern PROJECT_ID = Pattern.compile("[A-Za-z0-9-]{8,64}");

    private EstateTools() {
    }

    static List<ToolRegistration> tools() {
        return List.of(analyzeEstate());
    }

    private static ToolRegistration analyzeEstate() {
        return ToolRegistration.builder()
                .name("design_analyze_estate")
                .title("Analyse an existing estate in named projects (read-only)")
                .description("Analyses what already runs in the given Fabric projects: ports, connections, Cloud "
                        + "Routers, networks, service tokens and the Network Edge devices those connections use, with "
                        + "optional utilisation metrics (window_days). Findings, each with a stable id, evidence, a "
                        + "concrete action, confidence and financial impact (priced with source and range, or "
                        + "unpriced with the reason): right-size, on-demand to term, consolidate behind a Cloud Router, "
                        + "idle resources, single points of failure, capacity risk, hygiene. Money is never summed "
                        + "across currencies. SCOPE IS REQUIRED: project_ids must name the projects to analyse and "
                        + "there is no 'whole account' option, because these credentials can see thousands of "
                        + "resources belonging to other owners; a call without project_ids is refused. READ-ONLY "
                        + "(searches, GETs and metric queries only). Can take tens of seconds; cancellable.")
                .inputSchema(object(props(
                                "project_ids", with(array("Fabric project ids to analyse (required, 1 to "
                                        + MAX_PROJECTS + "). Only resources in these projects are analysed.",
                                        with(string("A Fabric project id (uuid)."), "pattern", "^[A-Za-z0-9-]{8,64}$")),
                                        "minItems", 1, "maxItems", MAX_PROJECTS),
                                "window_days", with(integer("Read connection utilisation over the last N days "
                                        + "(enables right-size and idle checks; more requests). Omit to skip metrics."),
                                        "minimum", 1, "maximum", 90),
                                "max_resources_per_type", with(integer("Stop reading a resource type after this "
                                        + "many in-scope items (the result then says TRUNCATED). Default "
                                        + DEFAULT_MAX_PER_TYPE + "."), "minimum", 1, "maximum", MAX_MAX_PER_TYPE),
                                "detail", detail("summary", "standard adds evidence, assumptions and resource names "
                                        + "per finding."),
                                "finding_ids", with(array("Return these findings in full (ids from a previous call).",
                                        string("Finding id.")), "maxItems", 20)),
                        "project_ids"))
                .outputSchema(DesignPayloads.outputSchema("scope, counts, actions in priority order, findings, "
                        + "addressable_monthly_saving per currency, unpriced, coverage_gaps, not_analysed, warnings, "
                        + "provenance.", "scope", "findings"))
                .toolset(Toolset.ESTATE)
                .handler((ToolHandler.Cancellable) EstateTools::handle)
                .build();
    }

    private static ObjectNode handle(JsonNode args, ServerContext ctx, CancellationSignal cancellation)
            throws Exception {
        List<String> projects = Args.stringList(args, "project_ids");
        if (projects.isEmpty()) {
            throw new IllegalArgumentException("'project_ids' is required: name the Fabric project(s) to analyse. "
                    + "This tool never scans everything the credentials can see, because they can see resources of "
                    + "many other owners. Find your project id in the Equinix portal or ask the user for it.");
        }
        Set<String> unique = new LinkedHashSet<>();
        for (String p : projects) {
            if (!PROJECT_ID.matcher(p).matches()) {
                throw new IllegalArgumentException("'project_ids' entry '" + UntrustedText.sanitize(p, 80)
                        + "' is not a project id (8-64 letters, digits and hyphens, normally a uuid).");
            }
            unique.add(p);
        }
        if (unique.size() > MAX_PROJECTS) {
            throw new IllegalArgumentException("'project_ids' names " + unique.size() + " projects; at most "
                    + MAX_PROJECTS + " per call.");
        }
        int maxPerType = Args.optInt(args, "max_resources_per_type").orElse(DEFAULT_MAX_PER_TYPE);
        if (maxPerType < 1 || maxPerType > MAX_MAX_PER_TYPE) {
            throw new IllegalArgumentException("'max_resources_per_type' must be 1 to " + MAX_MAX_PER_TYPE + ".");
        }
        EstateScope scope = EstateScope.projects(unique.toArray(new String[0])).withMaxResourcesPerType(maxPerType);
        if (scope.isUnscoped() || scope.entireAccount()) {
            throw new IllegalStateException("Refusing an unscoped estate analysis.");
        }
        Detail detail = Detail.of(args, Detail.SUMMARY);
        PricingBudget budget = ctx.pricingBudget();
        List<TimeoutRateCard> guards = new ArrayList<>();
        EstateAnalyzer.Builder builder = ctx.estateAnalyzer(scope);
        DesignToolFactory.fabricIfAvailable(ctx).ifPresent(fabric ->
                builder.rateCard(TimeoutRateCard.equinixChain(fabric, budget, guards)));
        Optional<Integer> window = Args.optInt(args, "window_days");
        if (window.isPresent()) {
            if (window.get() < 1 || window.get() > 90) {
                throw new IllegalArgumentException("'window_days' must be 1 to 90.");
            }
            builder.window(Duration.ofDays(window.get()));
        }
        List<String> progress = new ArrayList<>();
        builder.progress(line -> {
            synchronized (progress) {
                progress.add(line);
            }
            cancellation.throwIfCancelled("while the estate was being read (read-only)");
        });
        EstateReport report = EngineRunner.run("the estate analysis", cancellation, builder::analyze);
        ObjectNode payload = payload(report, detail, Args.stringList(args, "finding_ids"), ctx);
        synchronized (progress) {
            DesignPayloads.strings(payload, "collection_log", progress, 20, true);
        }
        TimeoutRateCard.reportEquinix(payload, guards, budget);
        return payload;
    }

    static ObjectNode payload(EstateReport report, Detail detail, List<String> wanted, ServerContext ctx) {
        ObjectNode payload = ctx.objectMapper().createObjectNode();
        ObjectNode scope = payload.putObject("scope");
        ArrayNode ids = scope.putArray("project_ids");
        report.scope().projectIds().forEach(ids::add);
        scope.put("max_resources_per_type", report.scope().maxResourcesPerType());
        scope.put("description", report.scope().describe());
        ObjectNode counts = payload.putObject("counts");
        report.counts().forEach(counts::put);
        counts.put("findings", report.findings().size());

        ObjectNode saving = payload.putObject("addressable_monthly_saving");
        report.addressableMonthlySaving().forEach((currency, total) -> {
            ObjectNode t = saving.putObject(currency);
            money(t, "monthly", total.monthly());
            money(t, "low", total.low());
            money(t, "high", total.high());
            t.put("findings", total.findings());
        });
        payload.put("saving_note", "Per currency, never summed across currencies; counts only priced savings, and "
                + "only the largest per resource.");

        capped(payload, "actions", report.actions(), detail.cap() * 2, (array, a) -> {
            ObjectNode n = array.addObject();
            n.put("rank", a.rank());
            n.put("finding_id", a.findingId());
            n.put("category", a.category().name());
            n.put("severity", a.severity().name());
            n.put("confidence", a.confidence().name());
            UntrustedText.put(n, "action", a.action());
            UntrustedText.put(n, "impact", a.impact());
        });
        capped(payload, "findings", report.findings(), detail.cap() * 2, (array, f) ->
                finding(array.addObject(), f, detail.atLeast(Detail.STANDARD)));
        if (!wanted.isEmpty()) {
            ArrayNode full = payload.putArray("finding_details");
            for (String id : wanted) {
                report.findings().stream().filter(f -> f.id().equals(id)).findFirst()
                        .ifPresent(f -> finding(full.addObject(), f, true));
            }
        }
        capped(payload, "unpriced", report.unpriced(), detail.cap(), (array, u) -> {
            ObjectNode n = array.addObject();
            n.put("finding_id", u.findingId());
            n.put("category", u.category().name());
            UntrustedText.put(n, "reason", u.reason());
        });
        capped(payload, "coverage_gaps", report.coverageGaps(), detail.cap(), (array, g) -> {
            ObjectNode n = array.addObject();
            n.put("resource_type", g.resourceType());
            n.put("reason", g.reason());
            if (g.httpStatus() != null) {
                n.put("http_status", g.httpStatus());
            }
            UntrustedText.put(n, "detail", g.detail());
        });
        DesignPayloads.strings(payload, "not_analysed", report.notAnalysed(), detail.cap(), false);
        DesignPayloads.strings(payload, "warnings", report.warnings(), detail.cap(), true);
        if (report.utilization() != null) {
            ObjectNode u = payload.putObject("utilization");
            u.put("collected", report.utilization().collected());
            u.put("probed", report.utilization().probed());
            u.put("with_data", report.utilization().withData());
            u.put("source", report.utilization().source());
        }
        if (report.collection() != null) {
            payload.put("requests", report.collection().requests());
            payload.put("collection_ms", report.collection().totalMillis());
        }
        if (detail == Detail.FULL) {
            payload.set("markdown", UntrustedText.document(report.toMarkdown()));
        }
        DesignPayloads.provenance(payload, "live",
                report.inventoryAsOf() == null ? null : report.inventoryAsOf().toString(), null,
                List.of(report.inventorySource() == null ? "Fabric inventory (live, read-only)"
                                : report.inventorySource(),
                        "prices: live Equinix under the call's pricing budget, else reference"));
        return payload;
    }

    private static void finding(ObjectNode n, EstateFinding f, boolean full) {
        n.put("id", f.id());
        n.put("category", f.category().name());
        n.put("severity", f.severity().name());
        n.put("confidence", f.confidence().name());
        UntrustedText.put(n, "title", f.title());
        UntrustedText.put(n, "action", f.action());
        FinancialImpact impact = f.impact();
        ObjectNode i = n.putObject("impact");
        i.put("kind", impact.kind().name().toLowerCase(Locale.ROOT));
        if (impact.kind() == FinancialImpact.Kind.PRICED) {
            i.put("currency", impact.currency());
            money(i, "monthly_saving", impact.monthlySaving());
            money(i, "monthly_saving_low", impact.monthlySavingLow());
            money(i, "monthly_saving_high", impact.monthlySavingHigh());
            money(i, "one_time_cost", impact.oneTimeCost());
            i.put("source", impact.source() == null ? null : impact.source().name().toLowerCase(Locale.ROOT));
        } else {
            UntrustedText.put(i, "reason", impact.reason());
        }
        ArrayNode resources = n.putArray("resources");
        f.resources().forEach(r -> {
            ObjectNode rn = resources.addObject();
            rn.put("kind", r.kind());
            rn.put("id", r.id());
            if (full) {
                UntrustedText.put(rn, "name", r.name());
            }
        });
        if (full) {
            ArrayNode evidence = n.putArray("evidence");
            f.evidence().forEach(e -> {
                ObjectNode en = evidence.addObject();
                en.put("label", e.label());
                UntrustedText.put(en, "value", e.value());
                en.put("unit", e.unit());
                en.put("source", e.source());
                en.put("window", e.window());
            });
            DesignPayloads.strings(n, "assumptions", f.assumptions(), 10, true);
            n.put("rule", f.rule());
        }
    }
}
