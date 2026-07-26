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

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.modelcontextprotocol.json.jackson2.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("EquinixMcpServer catalog — the 12 launch tools, toolset filtering, and the seam")
class EquinixMcpServerCatalogTest {

    private static final List<String> LAUNCH_CATALOG = List.of(
            "design_optimize_placement",
            "design_plan_deployment",
            "design_estimate_latency",
            "design_estimate_tco",
            "design_compare_cloud_egress",
            "design_analyze_peering",
            "design_export_terraform",
            "portal_list_open_tickets",
            "portal_get_billing_summary",
            "ne_list_devices",
            "ibx_get_environmentals",
            "ibx_list_power_events");

    private static List<String> names(List<ToolRegistration> registrations) {
        return registrations.stream().map(ToolRegistration::getName).toList();
    }

    @Test
    @DisplayName("the full catalog is exactly the 12 launch tools, in order")
    void fullCatalog() {
        assertEquals(LAUNCH_CATALOG, names(EquinixMcpServer.catalog(EnumSet.allOf(Toolset.class))));
    }

    @Test
    @DisplayName("every tool is read-only, domain-prefixed snake_case, and none is a delete")
    void catalogRules() {
        for (ToolRegistration tool : EquinixMcpServer.catalog(EnumSet.allOf(Toolset.class))) {
            assertTrue(tool.isReadOnly(), tool.getName() + " must be read-only");
            assertTrue(tool.getName().matches("^(design|portal|ne|ibx)_[a-z0-9_]+$"),
                    tool.getName() + " must be domain-prefixed snake_case");
            String lower = tool.getName().toLowerCase(Locale.ROOT);
            assertFalse(lower.contains("delete") || lower.contains("remove"),
                    tool.getName() + " must not be a delete tool");
            assertNotNull(tool.getInputSchema(), tool.getName() + " must declare an input schema");
            assertNotNull(tool.getOutputSchema(), tool.getName() + " must declare an output schema");
            assertNotNull(tool.getDescription(), tool.getName() + " must carry an LLM-facing description");
            assertNotNull(tool.getHandler(), tool.getName() + " must have a handler");
        }
    }

    @Test
    @DisplayName("openWorldHint is set exactly where external services are called")
    void openWorldHonesty() {
        Map<String, Boolean> openWorld = EquinixMcpServer.catalog(EnumSet.allOf(Toolset.class)).stream()
                .collect(java.util.stream.Collectors.toMap(ToolRegistration::getName, ToolRegistration::isOpenWorld));
        assertTrue(openWorld.get("design_compare_cloud_egress"), "live cloud pricing is open-world");
        assertTrue(openWorld.get("design_analyze_peering"), "PeeringDB is open-world");
        assertFalse(openWorld.get("design_estimate_latency"));
        assertFalse(openWorld.get("portal_list_open_tickets"));
        assertFalse(openWorld.get("ibx_list_power_events"));
    }

    @Test
    @DisplayName("toolset filtering selects the matching subsets")
    void toolsetFiltering() {
        assertEquals(7, EquinixMcpServer.catalog(EnumSet.of(Toolset.DESIGN)).size());
        assertEquals(7, EquinixMcpServer.catalog(EnumSet.of(Toolset.FABRIC)).size(),
                "the fabric id serves the same engine tools");
        assertEquals(List.of("portal_list_open_tickets", "portal_get_billing_summary"),
                names(EquinixMcpServer.catalog(EnumSet.of(Toolset.PORTAL))));
        assertEquals(List.of("ne_list_devices"), names(EquinixMcpServer.catalog(EnumSet.of(Toolset.NE))));
        assertEquals(List.of("ibx_get_environmentals", "ibx_list_power_events"),
                names(EquinixMcpServer.catalog(EnumSet.of(Toolset.IBX))));
        assertEquals(9, EquinixMcpServer.catalog(EnumSet.of(Toolset.DESIGN, Toolset.IBX)).size());
    }

    @Test
    @DisplayName("builder: toolset filter + additionalTools seam register on the running server")
    void builderRegistersFilteredAndAdditional() {
        ToolRegistration brokerTool = ToolRegistration.builder()
                .name("fabric_mutation_proposal")
                .title("Broker seam probe")
                .description("Stand-in for a Safe Mutation Broker tool registered via the seam.")
                .inputSchema(Schemas.object(Schemas.props()))
                .readOnly(false)
                .idempotent(false)
                .toolset(Toolset.FABRIC)
                .handler((args, ctx) -> {
                    ObjectNode payload = ctx.objectMapper().createObjectNode();
                    payload.put("phase", "dry-run");
                    return payload;
                })
                .build();

        ServerContext context = ServerContext.builder().environment(Map.of()).build();
        EquinixMcpServer server = EquinixMcpServer.builder()
                .context(context)
                .toolsets(Toolset.IBX)
                .additionalTools(brokerTool)
                .transportProvider(new StdioServerTransportProvider(
                        new JacksonMcpJsonMapper(context.objectMapper()),
                        new ByteArrayInputStream(new byte[0]), new ByteArrayOutputStream()))
                .build();
        try {
            assertEquals(List.of("ibx_get_environmentals", "ibx_list_power_events", "fabric_mutation_proposal"),
                    names(server.getRegistrations()));
            Set<String> served = server.getMcpServer().listTools().stream()
                    .map(io.modelcontextprotocol.spec.McpSchema.Tool::name)
                    .collect(java.util.stream.Collectors.toSet());
            assertEquals(Set.of("ibx_get_environmentals", "ibx_list_power_events", "fabric_mutation_proposal"),
                    served, "the MCP server serves exactly the filtered catalog plus the seam tool");
        }
        finally {
            server.close();
        }
    }

    @Test
    @DisplayName("duplicate tool names are rejected at build time")
    void duplicateNamesRejected() {
        ToolRegistration duplicate = ToolRegistration.builder()
                .name("ibx_get_environmentals")
                .description("Colliding name.")
                .inputSchema(Schemas.object(Schemas.props()))
                .toolset(Toolset.IBX)
                .handler((args, ctx) -> ctx.objectMapper().createObjectNode())
                .build();

        ServerContext context = ServerContext.builder().environment(Map.of()).build();
        EquinixMcpServer.Builder builder = EquinixMcpServer.builder()
                .context(context)
                .toolsets(Toolset.IBX)
                .additionalTools(duplicate)
                .transportProvider(new StdioServerTransportProvider(
                        new JacksonMcpJsonMapper(context.objectMapper()),
                        new ByteArrayInputStream(new byte[0]), new ByteArrayOutputStream()));
        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    @DisplayName("build without session, credentials, or context fails loudly")
    void requiresIdentity() {
        assertThrows(IllegalStateException.class, () -> EquinixMcpServer.builder().build());
    }

    @Test
    @DisplayName("a context without a session fails lazily, per facade, with a clear message")
    void lazyFacadeFailure() {
        ServerContext context = ServerContext.builder().environment(Map.of()).build();
        IllegalStateException e = assertThrows(IllegalStateException.class, context::customerPortal);
        assertTrue(e.getMessage().contains("Customer Portal"), e.getMessage());
    }
}
