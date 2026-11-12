package com.eqixiac.equinix.spec;

import com.eqixiac.equinix.spec.OpenApiSubsetValidator.Violation;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Rule-by-rule tests of {@link OpenApiSubsetValidator}. */
class OpenApiSubsetValidatorTest {

    private static final ObjectMapper M = new ObjectMapper();

    private static final String SPEC = """
            {"components": {"schemas": {
              "Base": {"type": "object", "required": ["uuid"], "properties": {"uuid": {"type": "string"}}},
              "Port": {"allOf": [{"$ref": "#/components/schemas/Base"},
                        {"type": "object", "properties": {
                           "state": {"type": "string", "enum": ["ACTIVE", "DELETED"]},
                           "speed": {"type": "integer"},
                           "tags": {"type": "array", "items": {"type": "string"}},
                           "extra": {"type": "object", "additionalProperties": {"type": "string"}}}}]},
              "Either": {"oneOf": [{"$ref": "#/components/schemas/Base"},
                                   {"type": "object", "required": ["id"], "properties": {"id": {"type": "integer"}}}]}
            }}}""";

    private static List<Violation> check(String json, String schema) throws Exception {
        JsonNode spec = M.readTree(SPEC);
        return new OpenApiSubsetValidator(spec).validate(M.readTree(json), "#/components/schemas/" + schema);
    }

    @Test
    void conformingFixturePasses() throws Exception {
        assertTrue(check("{\"uuid\":\"u\",\"state\":\"ACTIVE\",\"speed\":10.0,\"tags\":[\"a\"],\"extra\":{\"k\":\"v\"}}", "Port").isEmpty());
    }

    @Test
    void requiredFromAllOfIsEnforced() throws Exception {
        List<Violation> v = check("{\"state\":\"ACTIVE\"}", "Port");
        assertEquals(1, v.size());
        assertEquals("required", v.get(0).kind());
    }

    @Test
    void enumTypeAndUndocumentedAreReportedWithNormalizedPointers() throws Exception {
        List<Violation> v = check("{\"uuid\":\"u\",\"state\":\"GONE\",\"speed\":1.5,\"tags\":[1],\"invented\":true}", "Port");
        assertEquals(List.of("enum /state", "type /speed", "type /tags/*", "undocumented /invented"),
                v.stream().map(x -> x.kind() + " " + x.normalizedPointer()).sorted().toList());
    }

    @Test
    void nullIsAcceptedAndOneOfNeedsOneMatchingAlternative() throws Exception {
        assertTrue(check("{\"uuid\":null}", "Port").isEmpty());
        assertTrue(check("{\"id\":3}", "Either").isEmpty());
        assertEquals("required", check("{\"other\":1}", "Either").get(0).kind());
    }
}
