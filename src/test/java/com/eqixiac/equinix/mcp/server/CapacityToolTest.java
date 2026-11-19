/*
 * Copyright 2026 Ian Jones. All Rights Reserved.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.eqixiac.opencapacity.OpenCapacity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencapacity.reference.ReferenceProvider;

/**
 * The {@code capacity} toolset: selection rules, configuration from the environment, and the
 * {@code capacity_query} tool against the in-memory reference provider.
 */
class CapacityToolTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ServerContext context = ServerContext.builder().environment(Map.of()).build();
    private ReferenceProvider provider;
    private ToolRegistration tool;

    @BeforeEach
    void start() throws IOException {
        provider = ReferenceProvider.start();
        OpenCapacity client = OpenCapacity.builder().endpoint(provider.endpoint())
                .credentials(ReferenceProvider.CLIENT_ID, ReferenceProvider.CLIENT_SECRET).build();
        tool = CapacityToolFactory.tools(client).get(0);
    }

    @AfterEach
    void stop() {
        provider.close();
    }

    private ObjectNode call(String argsJson) throws Exception {
        return tool.getHandler().handle(MAPPER.readTree(argsJson), context);
    }

    @Test
    void toolsetIsOffByDefaultAndSelectedByName() {
        assertFalse(Toolset.defaults().contains(Toolset.CAPACITY));
        assertFalse(Toolset.parse("").contains(Toolset.CAPACITY));
        assertEquals(EnumSet.of(Toolset.CAPACITY), Toolset.parse("capacity"));
        assertEquals(Toolset.CAPACITY, tool.getToolset());
        assertEquals("capacity_query", tool.getName());
        assertTrue(EquinixMcpServer.catalog(EnumSet.allOf(Toolset.class)).stream()
                .noneMatch(registration -> registration.getToolset() == Toolset.CAPACITY),
                "the tool is registered per server, with its provider, and is not in the static catalog");
    }

    @Test
    void environmentMustNameTheProviderAndItsCredentials() {
        Map<String, String> env = new HashMap<>();
        IllegalArgumentException noEndpoint = assertThrows(IllegalArgumentException.class,
                () -> EquinixMcpServerMain.openCapacityFrom(env));
        assertTrue(noEndpoint.getMessage().contains("OPENCAPACITY_ENDPOINT"), noEndpoint.getMessage());
        env.put("OPENCAPACITY_ENDPOINT", provider.endpoint());
        env.put("OPENCAPACITY_CLIENT_ID", ReferenceProvider.CLIENT_ID);
        IllegalArgumentException noSecret = assertThrows(IllegalArgumentException.class,
                () -> EquinixMcpServerMain.openCapacityFrom(env));
        assertTrue(noSecret.getMessage().contains("OPENCAPACITY_CLIENT_SECRET"), noSecret.getMessage());
        assertFalse(noSecret.getMessage().contains(ReferenceProvider.CLIENT_ID), "the message never shows a value");
        env.put("OPENCAPACITY_CLIENT_SECRET", ReferenceProvider.CLIENT_SECRET);
        env.put("OPENCAPACITY_TOKEN_URL", provider.tokenUrl());
        assertEquals("1.0.0", EquinixMcpServerMain.openCapacityFrom(env).capabilities().specVersion());
        env.put("OPENCAPACITY_ENDPOINT", "http://provider.example/capacity/v1");
        assertThrows(IllegalStateException.class, () -> EquinixMcpServerMain.openCapacityFrom(env),
                "plain HTTP to another host is refused");
    }

    @Test
    void queryReturnsTheGradedAnswerWithTheBandsTheProviderDiscloses() throws Exception {
        ObjectNode payload = call("""
                {"locations": [{"level": "metro", "code": "FR"}, {"level": "METRO", "code": "ZZ"}],
                 "cabinets": 40, "density_target": 20, "density_max": 40,
                 "needed_for_service": "2027-Q2",
                 "features": [{"feature": "liquid_cooling", "required": true}]}
                """);
        assertEquals("AVAILABLE", payload.path("grade").asText());
        assertEquals(2, payload.path("count").asInt());
        assertTrue(payload.path("note").asText().contains("Not an offer"), payload.toString());
        assertTrue(payload.has("valid_until"));
        JsonNode frankfurt = payload.path("results").get(0);
        assertEquals("METRO", frankfurt.path("location").path("level").asText());
        assertEquals("FR", frankfurt.path("location").path("code").asText());
        assertEquals("AVAILABLE", frankfurt.path("grade").asText());
        assertEquals("IN_SERVICE", frankfurt.path("timing").path("status").asText());
        assertEquals("NOW", frankfurt.path("timing").path("available_for_service").asText());
        assertEquals(80.0, frankfurt.path("density_per_cabinet").path("max").asDouble());
        assertEquals(40, frankfurt.path("cabinets").path("min").asInt());
        assertFalse(frankfurt.path("cabinets").has("max"));
        assertEquals("LIQUID_COOLING", frankfurt.path("features").get(0).path("feature").asText());
        assertEquals("AVAILABLE", frankfurt.path("features").get(0).path("support").asText());
        JsonNode unknown = payload.path("results").get(1);
        assertEquals("UNAVAILABLE", unknown.path("grade").asText());
        assertEquals(List.of("LOCATION_NOT_SERVED"),
                MAPPER.convertValue(unknown.path("reasons"), MAPPER.getTypeFactory().constructCollectionType(List.class, String.class)));
    }

    @Test
    void powerOnlyQueryAndGradeOnlyDisclosure() throws Exception {
        provider.disclosure(ReferenceProvider.Disclosure.GRADE_ONLY);
        ObjectNode payload = call("""
                {"locations": [{"level": "COUNTRY", "code": "NL"}], "power": 0.5, "power_unit": "mw"}
                """);
        JsonNode result = payload.path("results").get(0);
        assertEquals("AVAILABLE", result.path("grade").asText());
        assertFalse(result.has("timing"));
        assertFalse(result.has("density_per_cabinet"));
        assertFalse(result.has("cabinets"));
    }

    @Test
    void argumentErrorsSayWhatToChange() {
        assertTrue(assertThrows(IllegalArgumentException.class, () -> call("{}")).getMessage().contains("locations"));
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> call("{\"locations\": [{\"level\": \"AREA\", \"code\": \"FR\"}], \"power\": 1}"))
                .getMessage().contains("REGION, COUNTRY, METRO"));
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> call("{\"locations\": [{\"level\": \"METRO\", \"code\": \"FR\"}], \"cabinets\": 4}"))
                .getMessage().contains("Give 'power'"));
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> call("{\"locations\": [{\"level\": \"METRO\", \"code\": \"FR\"}], \"power\": 1,"
                        + " \"features\": [{\"feature\": \"LIQUID_COOLING\"}]}"))
                .getMessage().contains("'required'"));
        assertThrows(IllegalArgumentException.class,
                () -> call("{\"locations\": [{\"level\": \"METRO\", \"code\": \"FR\"}], \"power\": 1,"
                        + " \"needed_for_service\": \"Q2 2027\"}"));
    }

    @Test
    void providerErrorIsLabelledAsUntrustedText() {
        provider.queryQuota(0);
        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> call("{\"locations\": [{\"level\": \"METRO\", \"code\": \"FR\"}], \"power\": 1}"));
        assertTrue(failure.getMessage().contains("HTTP 429"), failure.getMessage());
        assertTrue(failure.getMessage().contains("untrusted data from that provider"), failure.getMessage());
    }

    @Test
    void serverBuilderNeedsAClientWhenTheToolsetIsSelected() {
        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> EquinixMcpServer.builder().context(context).toolsets(Toolset.CAPACITY).build());
        assertTrue(failure.getMessage().contains("OPENCAPACITY_ENDPOINT"), failure.getMessage());
    }
}
