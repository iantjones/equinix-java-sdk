package com.eqixiac.equinix.spec;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A deliberately small validator for the subset of OpenAPI 3.0 / Swagger 2.0 schema keywords the
 * Equinix specifications use, applied to test fixtures by {@link FixtureSpecConformanceTest}.
 *
 * <p>Semantics (chosen to flag fixtures that do not follow the spec, without drowning in spec
 * quirks):</p>
 * <ul>
 *     <li>{@code $ref} to any local JSON pointer ({@code #/components/schemas/...},
 *         {@code #/definitions/...}, {@code #/paths/...}) is followed.</li>
 *     <li>{@code allOf} parts are merged: properties, required and additionalProperties.</li>
 *     <li>{@code oneOf}/{@code anyOf} pass when ANY alternative passes (several Equinix specs use
 *         {@code oneOf} for overlapping shapes); otherwise the alternative with the fewest
 *         violations is reported.</li>
 *     <li>JSON {@code null} is accepted anywhere: the live services send null for absent values
 *         and the SDK reads it as absent.</li>
 *     <li>{@code required}, {@code type} (integer accepts integral doubles) and string
 *         {@code enum} are enforced.</li>
 *     <li>A property not declared by an object schema that declares properties is an
 *         {@code undocumented} violation (unless {@code additionalProperties} allows it): this is
 *         what catches a fixture that invents fields the API does not return.</li>
 *     <li>Formats, patterns, lengths and numeric ranges are not checked.</li>
 * </ul>
 */
final class OpenApiSubsetValidator {

    /** One violation: where, which rule, and a human-readable detail. */
    record Violation(String pointer, String kind, String detail) {
        /** The pointer with array indexes replaced by {@code *}, as used in the allow-lists. */
        String normalizedPointer() {
            return normalize(pointer);
        }
    }

    private static final int MAX_DEPTH = 60;

    private final JsonNode spec;

    OpenApiSubsetValidator(JsonNode spec) {
        this.spec = spec;
    }

    /** Validates {@code node} against the schema at {@code schemaPointer} (a {@code #/...} pointer). */
    List<Violation> validate(JsonNode node, String schemaPointer) {
        JsonNode schema = at(schemaPointer);
        if (schema == null || schema.isMissingNode()) {
            throw new IllegalArgumentException("No schema at " + schemaPointer);
        }
        List<Violation> out = new ArrayList<>();
        validate(node, schema, "", out, 0);
        return out;
    }

    static String normalize(String pointer) {
        String p = pointer.replaceAll("/\\d+(?=/|$)", "/*");
        return p.isEmpty() ? "/" : p;
    }

    JsonNode at(String pointer) {
        if (!pointer.startsWith("#/")) {
            return null;
        }
        JsonNode node = spec;
        for (String part : pointer.substring(2).split("/")) {
            node = node.path(part.replace("~1", "/").replace("~0", "~"));
        }
        return node;
    }

    private JsonNode resolve(JsonNode schema) {
        JsonNode s = schema;
        int hops = 0;
        while (s != null && s.isObject() && s.has("$ref") && hops++ < 50) {
            String ref = s.get("$ref").asText();
            s = ref.startsWith("#/") ? at(ref) : null;
        }
        return s != null && s.isObject() ? s : null;
    }

    private record Merged(Map<String, JsonNode> properties, List<String> required, JsonNode additional) {
    }

    private Merged merged(JsonNode schema, int depth) {
        JsonNode s = resolve(schema);
        Map<String, JsonNode> props = new LinkedHashMap<>();
        List<String> required = new ArrayList<>();
        JsonNode additional = null;
        if (s == null || depth > MAX_DEPTH) {
            return new Merged(props, required, null);
        }
        s.path("properties").fields().forEachRemaining(e -> props.put(e.getKey(), e.getValue()));
        s.path("required").forEach(r -> required.add(r.asText()));
        if (s.has("additionalProperties")) {
            additional = s.get("additionalProperties");
        }
        for (JsonNode part : s.path("allOf")) {
            Merged m = merged(part, depth + 1);
            props.putAll(m.properties());
            required.addAll(m.required());
            if (m.additional() != null) {
                additional = m.additional();
            }
        }
        return new Merged(props, required, additional);
    }

    private void validate(JsonNode node, JsonNode schema, String ptr, List<Violation> out, int depth) {
        if (depth > MAX_DEPTH) {
            return;
        }
        JsonNode s = resolve(schema);
        if (s == null || s.isEmpty() || node == null || node.isNull()) {
            return;
        }
        List<JsonNode> alternatives = new ArrayList<>();
        s.path("oneOf").forEach(alternatives::add);
        s.path("anyOf").forEach(alternatives::add);
        if (!alternatives.isEmpty()) {
            List<Violation> best = null;
            for (JsonNode alternative : alternatives) {
                List<Violation> errors = new ArrayList<>();
                validate(node, alternative, ptr, errors, depth + 1);
                if (errors.isEmpty()) {
                    best = errors;
                    break;
                }
                if (best == null || errors.size() < best.size()) {
                    best = errors;
                }
            }
            if (!best.isEmpty()) {
                out.addAll(best);
                return;
            }
            if (!(s.has("properties") || s.has("allOf"))) {
                return;
            }
        }
        String type = s.has("type") ? s.get("type").asText() : null;
        Merged m = merged(s, depth);
        if (type == null && (!m.properties().isEmpty() || s.has("allOf"))) {
            type = "object";
        }
        if ("object".equals(type) || (type == null && node.isObject() && !m.properties().isEmpty())) {
            String here = ptr.isEmpty() ? "/" : ptr;
            if (!node.isObject()) {
                out.add(new Violation(here, "type", "expected object, got " + kind(node)));
                return;
            }
            for (String r : m.required()) {
                if (!node.has(r)) {
                    out.add(new Violation(here, "required", "missing required property \"" + r + "\""));
                }
            }
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String child = ptr + "/" + field.getKey();
                JsonNode additional = m.additional();
                if (m.properties().containsKey(field.getKey())) {
                    validate(field.getValue(), m.properties().get(field.getKey()), child, out, depth + 1);
                } else if (additional != null && additional.isObject()) {
                    validate(field.getValue(), additional, child, out, depth + 1);
                } else if ((additional != null && additional.isBoolean() && !additional.asBoolean())
                        || (!m.properties().isEmpty() && additional == null)) {
                    out.add(new Violation(child, "undocumented", "property not in the spec schema"));
                }
            }
            return;
        }
        if (type == null) {
            return;
        }
        switch (type) {
            case "array" -> {
                if (!node.isArray()) {
                    out.add(new Violation(ptr.isEmpty() ? "/" : ptr, "type", "expected array, got " + kind(node)));
                    return;
                }
                JsonNode items = s.path("items");
                for (int i = 0; i < node.size(); i++) {
                    validate(node.get(i), items.isMissingNode() ? null : items, ptr + "/" + i, out, depth + 1);
                }
            }
            case "string" -> {
                if (!node.isTextual()) {
                    out.add(new Violation(ptr, "type", "expected string, got " + kind(node)));
                } else if (s.has("enum") && s.get("enum").isArray() && !s.get("enum").isEmpty()) {
                    boolean found = false;
                    for (JsonNode e : s.get("enum")) {
                        if (e.isTextual() && e.asText().equals(node.asText())) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        out.add(new Violation(ptr, "enum", "value \"" + node.asText() + "\" not in spec enum"));
                    }
                }
            }
            case "integer" -> {
                boolean integral = node.isIntegralNumber()
                        || (node.isFloatingPointNumber() && node.asDouble() == Math.rint(node.asDouble()));
                if (!integral) {
                    out.add(new Violation(ptr, "type", "expected integer, got " + kind(node)));
                }
            }
            case "number" -> {
                if (!node.isNumber()) {
                    out.add(new Violation(ptr, "type", "expected number, got " + kind(node)));
                }
            }
            case "boolean" -> {
                if (!node.isBoolean()) {
                    out.add(new Violation(ptr, "type", "expected boolean, got " + kind(node)));
                }
            }
            default -> {
                // file / unknown types: not checked
            }
        }
    }

    private static String kind(JsonNode node) {
        return node.getNodeType().name().toLowerCase();
    }
}
