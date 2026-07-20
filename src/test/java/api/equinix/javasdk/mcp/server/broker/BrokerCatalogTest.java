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

package api.equinix.javasdk.mcp.server.broker;

import api.equinix.javasdk.mcp.server.EquinixMcpServer;
import api.equinix.javasdk.mcp.server.ServerContext;
import api.equinix.javasdk.mcp.server.ToolRegistration;
import api.equinix.javasdk.mcp.server.Toolset;
import io.modelcontextprotocol.json.jackson2.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Safe Mutation Broker registration — the mutate toolset is OFF unless explicitly enabled")
class BrokerCatalogTest {

    private static final List<String> BROKER_TOOLS = List.of("fabric_propose_change", "fabric_confirm_change");

    private static void withServer(Consumer<EquinixMcpServer.Builder> configure,
                                   Consumer<EquinixMcpServer> assertions) {
        ServerContext context = ServerContext.builder().environment(Map.of()).build();
        EquinixMcpServer.Builder builder = EquinixMcpServer.builder()
                .context(context)
                .transportProvider(new StdioServerTransportProvider(
                        new JacksonMcpJsonMapper(context.objectMapper()),
                        new ByteArrayInputStream(new byte[0]), new ByteArrayOutputStream()));
        configure.accept(builder);
        EquinixMcpServer server = builder.build();
        try {
            assertions.accept(server);
        }
        finally {
            server.close();
        }
    }

    private static List<String> names(EquinixMcpServer server) {
        return server.getRegistrations().stream().map(ToolRegistration::getName).toList();
    }

    @Test
    @DisplayName("toolset selection: mutate is excluded from every default path")
    void mutateExcludedByDefault() {
        assertFalse(Toolset.defaults().contains(Toolset.MUTATE), "defaults() must exclude mutate");
        assertFalse(Toolset.parse("").contains(Toolset.MUTATE), "blank csv must exclude mutate");
        assertFalse(Toolset.parse(null).contains(Toolset.MUTATE), "null csv must exclude mutate");
        assertEquals(5, Toolset.parse("").size(), "the five read-only toolsets are the default");
        assertEquals(EnumSet.of(Toolset.MUTATE), Toolset.parse("mutate"));
        assertEquals(EnumSet.of(Toolset.DESIGN, Toolset.MUTATE), Toolset.parse("design,mutate"));
        assertEquals(Toolset.MUTATE, Toolset.fromId("mutate"));
    }

    @Test
    @DisplayName("a default server serves no broker tools at all")
    void defaultServerHasNoBrokerTools() {
        withServer(builder -> {
        }, server -> {
            List<String> served = names(server);
            assertEquals(12, served.size(), "the default catalog stays the 12 read-only tools: " + served);
            BROKER_TOOLS.forEach(tool ->
                    assertFalse(served.contains(tool), tool + " must be absent unless mutate is enabled"));
        });
    }

    @Test
    @DisplayName("toolsets('mutate') serves exactly the propose/confirm pair, with honest annotations")
    void mutateOnlyServer() {
        withServer(builder -> builder.toolsets(Toolset.MUTATE), server -> {
            assertEquals(BROKER_TOOLS, names(server));

            for (ToolRegistration tool : server.getRegistrations()) {
                assertFalse(tool.isReadOnly(), tool.getName() + " exists to mutate: readOnly=false");
                assertEquals(Boolean.FALSE, tool.getDestructive(),
                        tool.getName() + " is a purely additive create: destructive=false");
                assertFalse(tool.isIdempotent(), tool.getName() + " is not idempotent");
                assertEquals(Toolset.MUTATE, tool.getToolset());
                assertNotNull(tool.getInputSchema());
                assertNotNull(tool.getOutputSchema());
                String lower = tool.getName().toLowerCase(Locale.ROOT);
                assertFalse(lower.contains("delete") || lower.contains("remove") || lower.contains("update"),
                        tool.getName() + " — no delete or update tools exist");
            }

            // The MCP wire carries the same honesty: mutating but non-destructive.
            for (McpSchema.Tool tool : server.getMcpServer().listTools()) {
                assertEquals(Boolean.FALSE, tool.annotations().readOnlyHint(), tool.name());
                assertEquals(Boolean.FALSE, tool.annotations().destructiveHint(),
                        tool.name() + " must carry destructiveHint=false");
                assertEquals(Boolean.FALSE, tool.annotations().idempotentHint(), tool.name());
            }
        });
    }

    @Test
    @DisplayName("mutate composes with read-only toolsets")
    void mutateComposes() {
        withServer(builder -> builder.toolsets(Toolset.DESIGN, Toolset.MUTATE), server -> {
            List<String> served = names(server);
            assertEquals(9, served.size(), "7 design tools + the broker pair: " + served);
            assertTrue(served.containsAll(BROKER_TOOLS));
        });
    }

    @Test
    @DisplayName("the change-type surface is exactly the three creates — smallest honest broker")
    void threeCreateChangeTypesOnly() {
        assertEquals(3, ChangeType.values().length);
        assertEquals(List.of("connection_create", "network_create", "service_token_create"),
                List.of(ChangeType.ids()));
        for (String id : ChangeType.ids()) {
            assertTrue(id.endsWith("_create"), id + " — only creates exist, no updates, no deletes");
        }
    }

    @Test
    @DisplayName("each tools(...) call pairs its two tools on one shared store")
    void toolsShareOneStorePerCall() {
        ProposalStore store = new ProposalStore();
        List<ToolRegistration> pair = BrokerToolFactory.tools(store);
        assertEquals(BROKER_TOOLS, pair.stream().map(ToolRegistration::getName).toList());
        assertEquals(2, BrokerToolFactory.tools().size(), "the no-arg overload builds a fresh pair");
    }
}
