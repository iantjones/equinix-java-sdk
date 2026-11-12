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

import com.eqixiac.equinix.design.solve.WeightProfiles;
import com.eqixiac.equinix.design.spec.DesignSpec;
import com.eqixiac.equinix.design.spec.DesignSpecIO;
import com.eqixiac.equinix.design.spec.SpecIssue;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Reads the {@code spec} argument every spec-taking design tool accepts, and turns problems into
 * issues the calling model can fix: each carries the dotted path, the JSON pointer, what is wrong and
 * how to fix it.
 *
 * <p>The argument is either a JSON object (the {@code DesignSpec} document itself) or a string holding
 * that document as JSON or YAML. Reading is strict, as {@link DesignSpecIO} is: an unknown property is
 * an error, not silently ignored.</p>
 */
final class SpecInput {

    /** The argument name every spec-taking tool uses. */
    static final String ARG = "spec";

    /** The argument name of a stored optimization's spec fingerprint. */
    static final String FINGERPRINT_ARG = "spec_fingerprint";

    private SpecInput() {
    }

    /**
     * The outcome of reading a spec argument.
     *
     * @param spec   the spec, or {@code null} when it could not be read at all
     * @param format {@code json-object}, {@code json} or {@code yaml}
     * @param issues every problem: read errors, then {@link DesignSpec#validate()} errors and warnings
     */
    record Parsed(DesignSpec spec, String format, List<Issue> issues) {

        boolean valid() {
            return spec != null && issues.stream().noneMatch(i -> i.severity() == SpecIssue.Severity.ERROR);
        }

        long errorCount() {
            return issues.stream().filter(i -> i.severity() == SpecIssue.Severity.ERROR).count();
        }
    }

    /**
     * One problem with a spec.
     *
     * @param severity    error (the spec cannot be used) or warning (part of a result will be unknown)
     * @param path        dotted path, e.g. {@code sites[0].metro}; {@code $} for the document
     * @param jsonPointer RFC 6901 pointer, e.g. {@code /sites/0/metro}
     * @param message     what is wrong
     * @param fix         how to fix it
     */
    record Issue(SpecIssue.Severity severity, String path, String jsonPointer, String message, String fix) {
    }

    /**
     * Reads and validates the {@code spec} argument without throwing for a bad document.
     *
     * @param args   the tool arguments
     * @param mapper the server's mapper
     * @return what was read and every issue
     * @throws IllegalArgumentException when the argument is missing or is neither an object nor a string
     */
    static Parsed read(JsonNode args, ObjectMapper mapper) {
        JsonNode node = args.get(ARG);
        if (node == null || node.isNull() || (node.isTextual() && node.asText().isBlank())) {
            throw new IllegalArgumentException("'spec' is required: a DesignSpec as a JSON object, or as a JSON or "
                    + "YAML string. Call design_spec_template for a commented starter.");
        }
        String format;
        String text;
        if (node.isObject()) {
            format = "json-object";
            text = node.toString();
        } else if (node.isTextual()) {
            text = node.asText().strip();
            format = text.startsWith("{") ? "json" : "yaml";
        } else {
            throw new IllegalArgumentException("'spec' must be a JSON object or a string (JSON or YAML), not "
                    + node.getNodeType().name().toLowerCase(Locale.ROOT) + ".");
        }
        DesignSpec spec;
        try {
            spec = "yaml".equals(format) ? DesignSpecIO.fromYaml(text) : DesignSpecIO.fromJson(text);
        } catch (IllegalArgumentException e) {
            return new Parsed(null, format, List.of(readIssue(e)));
        }
        List<Issue> issues = new ArrayList<>();
        for (SpecIssue issue : spec.validate()) {
            issues.add(new Issue(issue.severity(), issue.path(), pointer(issue.path()), issue.message(),
                    fix(issue, spec)));
        }
        return new Parsed(spec, format, List.copyOf(issues));
    }

    /**
     * Reads the {@code spec} argument and requires it to be valid.
     *
     * @param args   the tool arguments
     * @param mapper the server's mapper
     * @return the valid spec
     * @throws IllegalArgumentException listing every error with its path and fix
     */
    static DesignSpec require(JsonNode args, ObjectMapper mapper) {
        Parsed parsed = read(args, mapper);
        if (parsed.valid()) {
            return parsed.spec();
        }
        StringBuilder sb = new StringBuilder("The spec is not valid (").append(parsed.errorCount())
                .append(" error(s)); fix these and call again (design_validate_spec checks a spec without running "
                        + "anything):");
        parsed.issues().stream().filter(i -> i.severity() == SpecIssue.Severity.ERROR).limit(10).forEach(i ->
                sb.append(" [").append(i.path()).append("] ").append(i.message()).append(" Fix: ").append(i.fix()));
        throw new IllegalArgumentException(sb.toString());
    }

    /**
     * @param issues issues to render
     * @param mapper the mapper
     * @return the issues as a JSON array
     */
    static ArrayNode toJson(List<Issue> issues, ObjectMapper mapper) {
        ArrayNode array = mapper.createArrayNode();
        for (Issue issue : issues) {
            ObjectNode n = array.addObject();
            n.put("severity", issue.severity().name().toLowerCase(Locale.ROOT));
            n.put("path", issue.path());
            n.put("json_pointer", issue.jsonPointer());
            n.put("message", issue.message());
            n.put("fix", issue.fix());
        }
        return array;
    }

    /**
     * Converts a dotted path ({@code sites[0].metro}) into a JSON pointer ({@code /sites/0/metro}).
     *
     * @param path the dotted path
     * @return the pointer; {@code ""} for the whole document
     */
    static String pointer(String path) {
        if (path == null || path.isEmpty() || "$".equals(path)) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String part : path.split("\\.")) {
            int bracket = part.indexOf('[');
            String name = bracket < 0 ? part : part.substring(0, bracket);
            if (!name.isEmpty()) {
                sb.append('/').append(name.replace("~", "~0").replace("/", "~1"));
            }
            while (bracket >= 0) {
                int close = part.indexOf(']', bracket);
                sb.append('/').append(part, bracket + 1, close);
                bracket = part.indexOf('[', close);
            }
        }
        return sb.toString();
    }

    private static Issue readIssue(IllegalArgumentException e) {
        Throwable cause = e.getCause();
        String path = "$";
        String message = e.getMessage();
        String fix = "Send a DesignSpec document: a JSON object, or a JSON/YAML string. Call design_spec_template "
                + "for a commented starter.";
        if (cause instanceof JsonMappingException mapping) {
            path = dotted(mapping.getPath());
            message = mapping.getOriginalMessage();
            if (mapping instanceof UnrecognizedPropertyException unknown) {
                List<String> known = unknown.getKnownPropertyIds() == null ? List.of()
                        : unknown.getKnownPropertyIds().stream().map(String::valueOf).sorted().toList();
                message = "unknown property '" + unknown.getPropertyName() + "'";
                fix = "Remove '" + unknown.getPropertyName() + "' or rename it to one of: " + String.join(", ", known)
                        + ". Spec property names are camelCase (bandwidthMbps, maxLatencyMs, monthlyEgressGb).";
            } else if (mapping instanceof InvalidFormatException invalid && invalid.getTargetType() != null
                    && invalid.getTargetType().isEnum()) {
                String values = Arrays.stream(invalid.getTargetType().getEnumConstants())
                        .map(String::valueOf).collect(Collectors.joining(", "));
                message = "'" + invalid.getValue() + "' is not a valid " + invalid.getTargetType().getSimpleName();
                fix = "Use one of: " + values + ".";
            } else if (rootMessage(mapping).contains("exchangeRates")) {
                message = rootMessage(mapping);
                fix = "commercial.exchangeRates needs rates (ISO code to a positive number of spec-currency units per "
                        + "one unit), asOf and source; bandPercent is optional (0 to 50).";
            } else {
                message = rootMessage(mapping);
                fix = "Correct the value at " + path + " to the type the schema expects (see the resource "
                        + DesignResources.SPEC_SCHEMA_URI + ").";
            }
        } else if (message != null && message.contains("empty")) {
            fix = "The document is empty: send at least a name and one workload, cloud or peer.";
        }
        return new Issue(SpecIssue.Severity.ERROR, path, pointer(path), message, fix);
    }

    private static String rootMessage(Throwable t) {
        Throwable root = t;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String m = root instanceof JsonMappingException jm ? jm.getOriginalMessage() : root.getMessage();
        return m == null ? root.getClass().getSimpleName() : m;
    }

    private static String dotted(List<JsonMappingException.Reference> refs) {
        if (refs == null || refs.isEmpty()) {
            return "$";
        }
        StringBuilder sb = new StringBuilder();
        for (JsonMappingException.Reference ref : refs) {
            if (ref.getFieldName() != null) {
                if (!sb.isEmpty()) {
                    sb.append('.');
                }
                sb.append(ref.getFieldName());
            } else if (ref.getIndex() >= 0) {
                sb.append('[').append(ref.getIndex()).append(']');
            }
        }
        return sb.isEmpty() ? "$" : sb.toString();
    }

    /**
     * How to fix a validation issue, written for the calling model.
     *
     * @param issue the issue
     * @param spec  the spec it was found in
     * @return the instruction
     */
    static String fix(SpecIssue issue, DesignSpec spec) {
        String m = issue.message();
        String p = issue.path();
        if (m.startsWith("unsupported version")) {
            return "Set schemaVersion to \"" + DesignSpec.SCHEMA_VERSION + "\" or leave it out.";
        }
        if (m.contains("needs a name")) {
            return "Add a top-level \"name\", a short identifier such as \"eu-us-analytics\"; a deployment plan "
                    + "uses it as the deployment name.";
        }
        if (m.contains("states nothing to connect")) {
            return "Add at least one workload ({label, bandwidthMbps, clouds}), cloud ({id, provider, region}) or "
                    + "peer ({asn}).";
        }
        if (m.contains("not an Equinix metro code")) {
            return "Use the two-letter, upper-case Fabric metro code, e.g. LD London, NY New York, DC Ashburn, "
                    + "SV Silicon Valley, CH Chicago, FR Frankfurt, AM Amsterdam, PA Paris, SG Singapore, TY Tokyo, "
                    + "SY Sydney. For a site you may give latitude/longitude instead.";
        }
        if (m.startsWith("duplicate")) {
            return "Make every label, id and ASN unique within its list.";
        }
        if (m.contains("needs a label")) {
            return "Give the entry a short, unique \"label\".";
        }
        if (m.contains("needs an id")) {
            return "Give the cloud a short, unique \"id\" (for example \"aws-use1\") and reference it from "
                    + "workloads[].clouds.";
        }
        if (m.contains("give both latitude and longitude")) {
            return "Give both latitude and longitude (decimal degrees), or remove both and give metro.";
        }
        if (m.contains("no cloud with id")) {
            List<String> ids = spec.clouds().stream().filter(c -> c != null && c.id() != null)
                    .map(c -> c.id()).toList();
            return ids.isEmpty()
                    ? "Declare the cloud under clouds ({id, provider, region}) or remove the reference."
                    : "Reference one of the declared cloud ids " + ids + ", or declare a cloud with this id.";
        }
        if (m.contains("name a provider")) {
            return "Set provider to one of AWS, AZURE, GOOGLE_CLOUD, ORACLE_CLOUD, IBM_CLOUD, ALIBABA_CLOUD, or give "
                    + "serviceProfileName / serviceProfileUuid of a Fabric service profile.";
        }
        if (m.contains("unsized workload")) {
            return "Set bandwidthMbps (sustained Mbps) on the workload so its connections can be sized and priced.";
        }
        if (m.startsWith("no sites")) {
            return "Add sites (label plus metro, or latitude/longitude) so latency can rank the design; all latency "
                    + "is round-trip ms.";
        }
        if (m.contains("no metro and no coordinates")) {
            return "Give the site a metro code or latitude/longitude.";
        }
        if (m.contains("both required and excluded")) {
            return "Remove it from requiredMetros/requiredRegions or from excludedMetros/excludedRegions.";
        }
        if (m.contains("exceeds maxMetros") || m.contains("but maxMetros is")) {
            return "Raise constraints.maxMetros, or lower minMetros / the required metros / minimumRedundancy.";
        }
        if (m.contains("ISO 4217")) {
            return "Use an ISO 4217 currency code such as USD, EUR, GBP or JPY.";
        }
        if (m.contains("every weight is zero")) {
            return "Name objectives.profile (one of " + String.join(", ", WeightProfiles.names())
                    + ") or give at least one positive weight.";
        }
        if (p.startsWith("objectives.weights")) {
            return "Weights are non-negative numbers keyed by objective id (latency, cost, provider_coverage, "
                    + "resilience, compliance); they are normalised to sum 1.";
        }
        if (m.contains("budgetMinMonthly exceeds")) {
            return "Make commercial.budgetMinMonthly at most commercial.budgetMaxMonthly.";
        }
        if (m.contains("finite fraction")) {
            return "Give the annual growth as a fraction, e.g. 0.25 for 25% a year.";
        }
        if (m.contains("must be") || m.contains("must not")) {
            return "Change the value at " + p + ": it " + m.substring(m.indexOf("must")) + ".";
        }
        return "Correct the value at " + p + ".";
    }

    /**
     * The JSON form of a spec in the server's mapper, for embedding in a payload.
     *
     * @param spec   the spec
     * @param mapper the mapper
     * @return the spec as a JSON object
     */
    static JsonNode toNode(DesignSpec spec, ObjectMapper mapper) {
        try {
            return mapper.readTree(DesignSpecIO.toJson(spec));
        } catch (java.io.IOException e) {
            throw new IllegalStateException("A DesignSpec is always serializable", e);
        }
    }

    /**
     * Keys of a map, sorted, as a readable list.
     *
     * @param map the map
     * @return the keys
     */
    static String keys(Map<String, ?> map) {
        return map.keySet().stream().sorted().collect(Collectors.joining(", "));
    }
}
