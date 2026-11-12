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

import com.eqixiac.equinix.design.spec.DesignSpec;
import com.eqixiac.equinix.design.spec.DesignSpecIO;
import com.eqixiac.equinix.design.spec.SpecIssue;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;

import static com.eqixiac.equinix.mcp.server.Schemas.bool;
import static com.eqixiac.equinix.mcp.server.Schemas.object;
import static com.eqixiac.equinix.mcp.server.Schemas.objectOrString;
import static com.eqixiac.equinix.mcp.server.Schemas.props;
import static com.eqixiac.equinix.mcp.server.Schemas.stringEnum;
import static com.eqixiac.equinix.mcp.server.Schemas.with;

/**
 * The two tools that make the {@code DesignSpec} the shared currency of the design toolset:
 * {@code design_spec_template} (a commented starter) and {@code design_validate_spec} (structured
 * issues with paths and fixes, the normalised spec and its fingerprint). Both work on this process's
 * own data only: no request leaves the server.
 */
final class SpecTools {

    /** The description of the {@code spec} argument every spec-taking tool shares. */
    static final String SPEC_ARG_DESCRIPTION = "The design as a DesignSpec: a JSON object, or a JSON or YAML string. "
            + "Same document for every design tool; results carry its spec_fingerprint. Start from "
            + "design_spec_template; the JSON Schema is the resource " + DesignResources.SPEC_SCHEMA_URI
            + ". All latency fields are ROUND-TRIP ms.";

    private SpecTools() {
    }

    static List<ToolRegistration> tools() {
        return List.of(template(), validate());
    }

    static ToolRegistration template() {
        return ToolRegistration.builder()
                .name("design_spec_template")
                .title("Get a commented starter DesignSpec")
                .description("Returns a commented starter DesignSpec (YAML) that validates clean: two sites (London, "
                        + "New York), AWS us-east-1, a 1 Gbps workload and at most two metros. Edit it and pass it as "
                        + "'spec' to design_validate_spec, design_optimize, design_analyze_resilience and the other "
                        + "design tools. Also returns the same spec as a JSON object with its fingerprint, and the "
                        + "spec's JSON Schema when include_schema is true. Works offline; reads nothing remote.")
                .inputSchema(object(props(
                        "format", with(stringEnum("Template form. yaml keeps the comments; json is the bare document. "
                                + "Default yaml.", "yaml", "json"), "default", "yaml"),
                        "include_schema", with(bool("Also return the DesignSpec JSON Schema (about 6 KB). Default "
                                + "false: it is also the resource " + DesignResources.SPEC_SCHEMA_URI + "."),
                                "default", false))))
                .outputSchema(DesignPayloads.outputSchema("The starter spec as text and as JSON, its fingerprint, "
                        + "and optionally the JSON Schema.", "template", "spec", "spec_fingerprint"))
                .toolset(Toolset.DESIGN)
                .openWorld(false)
                .handler(SpecTools::handleTemplate)
                .build();
    }

    private static ObjectNode handleTemplate(JsonNode args, ServerContext ctx) {
        String yaml = DesignSpecSchema.templateYaml();
        DesignSpec spec = DesignSpecIO.fromYaml(yaml).requireValid();
        boolean json = Args.optString(args, "format").map("json"::equalsIgnoreCase).orElse(false);
        ObjectNode payload = ctx.objectMapper().createObjectNode();
        payload.put("format", json ? "json" : "yaml");
        payload.put("template", json ? DesignSpecIO.toJson(spec) : yaml);
        payload.set("spec", SpecInput.toNode(spec, ctx.objectMapper()));
        payload.put("spec_fingerprint", spec.fingerprint());
        payload.put("next_step", "Edit the spec (names, sites, clouds, workloads, constraints) and call "
                + "design_validate_spec, then design_optimize.");
        if (Args.optBool(args, "include_schema", false)) {
            payload.set("json_schema", ctx.objectMapper().valueToTree(DesignSpecSchema.schema()));
        }
        DesignPayloads.provenance(payload, "computed", null, spec.fingerprint(),
                List.of("bundled template (this server)"));
        return payload;
    }

    static ToolRegistration validate() {
        return ToolRegistration.builder()
                .name("design_validate_spec")
                .title("Validate a DesignSpec")
                .description("Checks a DesignSpec without running any engine and returns: valid (no errors), "
                        + "every issue with severity (error blocks every engine; warning means part of a result "
                        + "will be unknown), its dotted path and JSON pointer, what is wrong and how to fix it; the "
                        + "normalised spec (defaults applied, as the engines will read it) and its "
                        + "spec_fingerprint (SHA-256 of the canonical JSON; equal content gives an equal "
                        + "fingerprint). Reading is strict: an unknown property is an error that names the valid "
                        + "properties. It does not check metro codes against the live catalogue (design_optimize "
                        + "reports unknown metros). Works offline.")
                .inputSchema(object(props(
                        SpecInput.ARG, objectOrString(SPEC_ARG_DESCRIPTION),
                        "format", with(stringEnum("Form of the returned normalised spec text. Default json (the "
                                + "normalised spec is always also returned as an object).", "json", "yaml"),
                                "default", "json")),
                        SpecInput.ARG))
                .outputSchema(DesignPayloads.outputSchema("valid, error and warning counts, issues (severity, "
                        + "path, json_pointer, message, fix), the normalised spec, its text and spec_fingerprint.",
                        "valid", "issues"))
                .toolset(Toolset.DESIGN)
                .openWorld(false)
                .handler(SpecTools::handleValidate)
                .build();
    }

    private static ObjectNode handleValidate(JsonNode args, ServerContext ctx) {
        SpecInput.Parsed parsed = SpecInput.read(args, ctx.objectMapper());
        ObjectNode payload = ctx.objectMapper().createObjectNode();
        payload.put("valid", parsed.valid());
        payload.put("input_format", parsed.format());
        payload.put("error_count", parsed.errorCount());
        payload.put("warning_count",
                parsed.issues().stream().filter(i -> i.severity() == SpecIssue.Severity.WARNING).count());
        payload.set("issues", SpecInput.toJson(parsed.issues(), ctx.objectMapper()));
        String fingerprint = null;
        if (parsed.spec() != null) {
            DesignSpec spec = parsed.spec();
            fingerprint = spec.fingerprint();
            payload.put("spec_fingerprint", fingerprint);
            payload.set("normalized_spec", SpecInput.toNode(spec, ctx.objectMapper()));
            boolean yaml = Args.optString(args, "format").map("yaml"::equalsIgnoreCase).orElse(false);
            payload.put("normalized_format", yaml ? "yaml" : "json");
            payload.put("normalized_text", yaml ? DesignSpecIO.toYaml(spec) : DesignSpecIO.toJson(spec));
            payload.put("fingerprint_note", "The fingerprint is computed over the normalised spec; it changes "
                    + "whenever any value changes and identifies this spec's results in every design tool.");
        } else {
            payload.putNull("spec_fingerprint");
        }
        payload.put("next_step", parsed.valid()
                ? "Pass this spec to design_optimize (or design_analyze_resilience with a portfolio)."
                : "Fix the errors (each issue says how) and validate again.");
        DesignPayloads.provenance(payload, "computed", null, fingerprint, List.of("DesignSpec.validate() (this server)"));
        return payload;
    }
}
