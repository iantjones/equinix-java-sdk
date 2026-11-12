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

import com.eqixiac.equinix.FabricGateway;
import com.eqixiac.equinix.design.optimizer.model.OptimizationResult;
import com.eqixiac.equinix.design.record.DesignDecisionRecord;
import com.eqixiac.equinix.design.record.DesignDecisionRecorder;
import com.eqixiac.equinix.design.record.RecordSection;
import com.eqixiac.equinix.design.spec.DesignSpec;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.eqixiac.equinix.mcp.server.Args.optInt;
import static com.eqixiac.equinix.mcp.server.Args.optString;
import static com.eqixiac.equinix.mcp.server.Schemas.bool;
import static com.eqixiac.equinix.mcp.server.Schemas.integer;
import static com.eqixiac.equinix.mcp.server.Schemas.object;
import static com.eqixiac.equinix.mcp.server.Schemas.objectOrString;
import static com.eqixiac.equinix.mcp.server.Schemas.props;
import static com.eqixiac.equinix.mcp.server.Schemas.string;
import static com.eqixiac.equinix.mcp.server.Schemas.with;

/**
 * {@code design_decision_record}: the Design Decision Record of a spec, as an ADR-style Markdown
 * document plus the status of each section.
 *
 * <p>A record for a real design is longer than {@link UntrustedText#MAX_DOCUMENT_CHARS}, so the
 * Markdown is returned in pages of at most {@link #PAGE_CHARS} characters: the result states the total
 * length and the next offset, and nothing is silently cut.</p>
 */
final class DecisionRecordTools {

    /** The most Markdown characters one call returns. */
    static final int PAGE_CHARS = 16_000;

    private DecisionRecordTools() {
    }

    static ToolRegistration decisionRecord() {
        return ToolRegistration.builder()
                .name("design_decision_record")
                .title("Write the Design Decision Record for a spec")
                .description("Produces the document an architecture review reads: what was decided for this "
                        + "DesignSpec, why, on what evidence, and what would change the decision. It chains the "
                        + "engines (optimizer, failure simulation, growth and term projection, TCO and egress "
                        + "savings when the spec states egress, and an offline deployment plan) and returns an "
                        + "ADR-style Markdown document: context, decision with each objective's value, score, "
                        + "weight and scale, consequences, alternatives on the Pareto frontier, and appendices for "
                        + "sensitivity, cost (source, range and exchange rates per line), resilience (N-1 / N-2, "
                        + "single points of failure, availability as planning assumptions, not SLAs), risks, "
                        + "assumptions and how to reproduce the run. A section that cannot be produced is marked "
                        + "skipped or failed with the reason; the record is still returned. Reuses the stored "
                        + "design_optimize result for the spec when there is one, else runs the optimizer. The "
                        + "Markdown is paged: read 'markdown_next_offset' and call again with 'offset' until it is "
                        + "null. All latency is ROUND-TRIP ms. READ-ONLY; reads the live catalogue and prices, "
                        + "creates nothing. Cancellable; allow tens of seconds.")
                .inputSchema(object(props(
                        SpecInput.ARG, objectOrString(SpecTools.SPEC_ARG_DESCRIPTION),
                        SpecInput.FINGERPRINT_ARG, DesignToolFactory.fingerprintSchema(),
                        "title", with(string("Document title. Default: derived from the spec name."),
                                "maxLength", 200),
                        "status", with(string("ADR status line, e.g. Proposed, Accepted. Default: Proposed."),
                                "maxLength", 40),
                        "build_plan", bool("Include the offline deployment plan summary (steps and estimated "
                                + "cost; no live request). Default true."),
                        "offset", with(integer("Character offset into the Markdown to continue from (the "
                                + "'markdown_next_offset' of the previous call). Default 0."), "minimum", 0))))
                .outputSchema(DesignPayloads.outputSchema("title, status, recommended, sections (status and "
                        + "reason per section), markdown (one page), markdown_total_chars, markdown_offset, "
                        + "markdown_next_offset, spec_fingerprint, provenance.", "sections", "markdown",
                        "spec_fingerprint"))
                .toolset(Toolset.DESIGN)
                .handler((ToolHandler.Cancellable) DecisionRecordTools::handle)
                .build();
    }

    private static ObjectNode handle(JsonNode args, ServerContext ctx, CancellationSignal cancellation)
            throws Exception {
        DesignToolFactory.SpecAndRun input = DesignToolFactory.specOf(args, ctx);
        DesignSpec spec = input.spec();
        int offset = optInt(args, "offset").orElse(0);
        if (offset < 0) {
            throw new IllegalArgumentException("'offset' must not be negative.");
        }
        PricingBudget budget = ctx.pricingBudget();
        List<TimeoutRateCard> guards = new ArrayList<>();
        FabricGateway fabric = ctx.fabric();

        OptimizationResult optimized = input.optimized();
        List<String> evicted = List.of();
        String optimizationSource = "stored design_optimize result";
        if (optimized == null) {
            DesignToolFactory.Optimized run = DesignToolFactory.optimize(spec, ctx, budget, guards, cancellation);
            optimized = run.result();
            evicted = run.evicted();
            optimizationSource = "design_optimize run by this call (now stored)";
        }
        DesignDecisionRecorder recorder = DesignDecisionRecorder.forSpec(fabric, spec)
                .rateCard(TimeoutRateCard.equinixChain(fabric, budget, guards))
                .optimization(optimized)
                .buildPlan(!args.hasNonNull("build_plan") || args.get("build_plan").asBoolean(true));
        optString(args, "title").ifPresent(recorder::title);
        optString(args, "status").ifPresent(recorder::status);
        DesignDecisionRecord record = EngineRunner.run("the decision record", cancellation, recorder::record);

        ObjectNode payload = ctx.objectMapper().createObjectNode();
        payload.put("spec_fingerprint", record.specFingerprint());
        UntrustedText.put(payload, "title", record.title());
        UntrustedText.put(payload, "status", record.status());
        if (record.decision().isIncluded() && record.decision().content() != null) {
            payload.put("recommended", record.decision().content().portfolio());
        } else {
            payload.putNull("recommended");
        }
        payload.put("optimization_source", optimizationSource);
        ObjectNode sections = payload.putObject("sections");
        for (Map.Entry<String, RecordSection<?>> section : record.sections().entrySet()) {
            ObjectNode s = sections.putObject(section.getKey());
            s.put("status", section.getValue().status().name().toLowerCase(java.util.Locale.ROOT));
            if (section.getValue().reason() != null) {
                UntrustedText.put(s, "reason", section.getValue().reason());
            }
        }
        String markdown = record.toMarkdown();
        if (offset > markdown.length()) {
            throw new IllegalArgumentException("'offset' " + offset + " is past the end of the document ("
                    + markdown.length() + " characters).");
        }
        int end = pageEnd(markdown, offset);
        payload.set("markdown", UntrustedText.document(markdown.substring(offset, end)));
        payload.put("markdown_total_chars", markdown.length());
        payload.put("markdown_offset", offset);
        if (end < markdown.length()) {
            payload.put("markdown_next_offset", end);
        } else {
            payload.putNull("markdown_next_offset");
        }
        DesignPayloads.provenance(payload, "mixed", record.generatedAt() == null ? null
                : record.generatedAt().toString(), record.specFingerprint(), List.of(
                "Equinix Fabric catalogue and price search (live), reference price table and exchange rates as "
                        + "stated per cost line in the document",
                "failure simulation and availability figures computed by this server from stated assumptions"));
        DesignToolFactory.reportEvicted(payload, evicted, DesignStore.Kind.OPTIMIZATION);
        TimeoutRateCard.reportEquinix(payload, guards, budget);
        return payload;
    }

    /** The end of the page starting at {@code offset}: at most {@link #PAGE_CHARS} on, cut at a line end. */
    static int pageEnd(String markdown, int offset) {
        int limit = offset + PAGE_CHARS;
        if (limit >= markdown.length()) {
            return markdown.length();
        }
        int newline = markdown.lastIndexOf('\n', limit);
        return newline > offset ? newline + 1 : limit;
    }
}
