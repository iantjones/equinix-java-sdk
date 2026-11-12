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

import com.eqixiac.equinix.design.spec.DesignSpecIO;
import com.eqixiac.equinix.mcp.server.broker.BrokerToolFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.networknt.schema.Error;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import static com.eqixiac.equinix.mcp.server.DesignFixtures.MAPPER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every tool's input and output schema is a valid JSON Schema 2020-12 document (checked against the
 * meta-schema), descriptions are present, the DesignSpec schema accepts the starter spec and rejects
 * what the strict reader rejects, and real design results satisfy their output schemas.
 */
@DisplayName("tool schemas: valid 2020-12, precise, and honoured by the results")
class ToolSchemaValidityTest {

    private static final SchemaRegistry REGISTRY = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12);

    private static List<ToolRegistration> allTools() {
        List<ToolRegistration> tools = new ArrayList<>(EquinixMcpServer.catalog(EnumSet.allOf(Toolset.class)));
        tools.addAll(BrokerToolFactory.tools());
        return tools;
    }

    private static List<Error> metaErrors(Map<String, Object> schema) {
        Schema meta = REGISTRY.getSchema(SchemaLocation.of("https://json-schema.org/draft/2020-12/schema"));
        return meta.validate(MAPPER.valueToTree(schema));
    }

    @Test
    @DisplayName("every input and output schema validates against the 2020-12 meta-schema")
    void metaSchema() {
        for (ToolRegistration tool : allTools()) {
            assertEquals(List.of(), metaErrors(tool.getInputSchema()), tool.getName() + " input schema");
            if (tool.getOutputSchema() != null) {
                assertEquals(List.of(), metaErrors(tool.getOutputSchema()), tool.getName() + " output schema");
            }
            assertEquals("object", tool.getInputSchema().get("type"), tool.getName());
        }
        assertEquals(List.of(), metaErrors(DesignSpecSchema.schema()), "the DesignSpec schema");
    }

    @Test
    @DisplayName("every design input property has a description, and closed objects are closed")
    @SuppressWarnings("unchecked")
    void descriptions() {
        for (ToolRegistration tool : EquinixMcpServer.catalog(EnumSet.of(Toolset.FABRIC))) {
            Map<String, Object> props = (Map<String, Object>) tool.getInputSchema().get("properties");
            assertEquals(Boolean.FALSE, tool.getInputSchema().get("additionalProperties"), tool.getName());
            props.forEach((name, schema) -> assertTrue(((Map<String, Object>) schema).get("description") != null
                    || ((Map<String, Object>) schema).get("properties") != null, tool.getName() + "." + name));
            assertTrue(((Map<String, Object>) tool.getOutputSchema()).get("required") != null, tool.getName()
                    + " output schema names its required fields");
        }
    }

    @Test
    @DisplayName("the DesignSpec schema accepts the starter spec and rejects unknown properties")
    void specSchema() throws Exception {
        Schema schema = REGISTRY.getSchema(MAPPER.valueToTree(DesignSpecSchema.schema()));
        JsonNode template = new YAMLMapper().readTree(DesignSpecSchema.templateYaml());
        assertEquals(List.of(), schema.validate(template));
        JsonNode normalised = MAPPER.readTree(DesignSpecIO.toJson(DesignSpecIO.fromYaml(DesignSpecSchema.templateYaml())));
        assertEquals(List.of(), schema.validate(normalised), "the normalised form also validates");
        ObjectNode bad = template.deepCopy();
        ((ObjectNode) bad.get("workloads").get(0)).put("bandwidth_mbps", 1);
        assertFalse(schema.validate(bad).isEmpty(), "unknown property rejected, as the reader does");
    }

    @Test
    @DisplayName("design results satisfy their output schemas")
    void resultsMatchOutputSchemas() throws Exception {
        ServerContext ctx = DesignFixtures.context();
        String spec = DesignFixtures.withSpec(DesignFixtures.SPEC_DC_AWS, null);
        Map<String, String> calls = new java.util.LinkedHashMap<>();
        calls.put("design_spec_template", "{}");
        calls.put("design_validate_spec", spec);
        calls.put("design_optimize", spec);
        calls.put("design_analyze_resilience", spec);
        calls.put("design_growth_and_terms", spec);
        calls.put("design_estimate_tco", spec);
        calls.put("design_estimate_latency", "{\"from\": \"DC\", \"to\": \"SV\"}");
        calls.put("design_plan_deployment", DesignFixtures.withSpec(DesignFixtures.SPEC_DC_AWS,
                "\"deployment\": {\"notifications\": [\"noc@example.com\"]}"));
        for (Map.Entry<String, String> call : calls.entrySet()) {
            ToolRegistration tool = DesignFixtures.tool(call.getKey());
            ObjectNode payload = DesignFixtures.call(call.getKey(), ctx, call.getValue());
            Schema output = REGISTRY.getSchema(MAPPER.valueToTree(tool.getOutputSchema()));
            assertEquals(List.of(), output.validate(payload), call.getKey());
        }
    }
}
