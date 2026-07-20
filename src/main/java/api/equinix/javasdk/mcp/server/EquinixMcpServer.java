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

package api.equinix.javasdk.mcp.server;

import api.equinix.javasdk.Equinix;
import api.equinix.javasdk.core.auth.BasicEquinixCredentials;
import api.equinix.javasdk.core.auth.EquinixCredentials;
import api.equinix.javasdk.mcp.server.broker.BrokerToolFactory;
import io.modelcontextprotocol.json.jackson2.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpServerTransportProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The embedded Equinix Intelligence MCP Server — a <em>community</em> server, not affiliated
 * with Equinix and unrelated to Equinix's private-beta Fabric MCP server. It exposes this
 * SDK's design engines and cross-domain reads as MCP tools over <strong>stdio only</strong>,
 * executing under the operator's client-credentials.
 *
 * <pre>{@code
 * EquinixMcpServer server = EquinixMcpServer.builder()
 *         .credentials(accessKey, secretKey)     // or .session(existingEquinix)
 *         .toolsets(Toolset.DESIGN, Toolset.IBX) // optional; default: everything
 *         .build();                              // registers tools and starts serving stdio
 * }</pre>
 *
 * <p>The tool catalog is deliberately small (see {@link #catalog(Set)}). The Safe Mutation
 * Broker's dry-run-first mutation pair lives in the opt-in {@code mutate} toolset — served
 * only when that toolset is explicitly selected, never by default. Custom extensions register
 * through {@link Builder#additionalTools(ToolRegistration...)} using the exact same
 * {@link ToolRegistration} seam the built-ins (broker included) use.</p>
 *
 * <p><strong>stdout is sacred:</strong> the MCP wire protocol owns it. This class and every
 * handler log via slf4j only; {@link EquinixMcpServerMain} routes slf4j-simple to stderr.</p>
 */
public final class EquinixMcpServer implements AutoCloseable {

    /** The MCP {@code serverInfo.name} presented to clients. */
    public static final String SERVER_NAME = "equinix-sdk-java-intelligence";

    private static final Logger logger = LoggerFactory.getLogger(EquinixMcpServer.class);

    private final McpSyncServer mcpServer;
    private final ServerContext context;
    private final List<ToolRegistration> registrations;
    private final Equinix ownedSession;

    private EquinixMcpServer(McpSyncServer mcpServer, ServerContext context,
                             List<ToolRegistration> registrations, Equinix ownedSession) {
        this.mcpServer = mcpServer;
        this.context = context;
        this.registrations = List.copyOf(registrations);
        this.ownedSession = ownedSession;
    }

    /**
     * @return a new server builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * The built-in <em>read-only</em> tool catalog for the selected toolsets. The
     * {@link Toolset#DESIGN} and {@link Toolset#FABRIC} ids currently select the same seven
     * engine tools (they are all Fabric-backed); {@code portal}, {@code ne}, and {@code ibx}
     * add their domains' reach. The {@link Toolset#MUTATE} toolset is deliberately not part
     * of this static catalog: the Safe Mutation Broker's tool pair shares a per-server
     * proposal store, so {@link Builder#build()} registers it per instance when (and only
     * when) {@code mutate} was explicitly selected.
     *
     * @param toolsets the selected toolsets
     * @return the matching registrations, in catalog order
     */
    public static List<ToolRegistration> catalog(Set<Toolset> toolsets) {
        List<ToolRegistration> selected = new ArrayList<>();
        if (toolsets.contains(Toolset.DESIGN) || toolsets.contains(Toolset.FABRIC)) {
            selected.addAll(DesignToolFactory.tools());
        }
        if (toolsets.contains(Toolset.PORTAL)) {
            selected.addAll(PortalToolFactory.tools());
        }
        if (toolsets.contains(Toolset.NE)) {
            selected.addAll(NetworkEdgeToolFactory.tools());
        }
        if (toolsets.contains(Toolset.IBX)) {
            selected.addAll(IbxToolFactory.tools());
        }
        return selected;
    }

    /**
     * @return the underlying MCP sync server
     */
    public McpSyncServer getMcpServer() {
        return mcpServer;
    }

    /**
     * @return the shared context the tool handlers execute against
     */
    public ServerContext getContext() {
        return context;
    }

    /**
     * @return every registered tool, in registration order
     */
    public List<ToolRegistration> getRegistrations() {
        return registrations;
    }

    /**
     * Resolves this server's advertised version: the jar's implementation version when
     * running from the packaged artifact, else a development placeholder.
     *
     * @return the version string used in {@code serverInfo}
     */
    public static String serverVersion() {
        String version = EquinixMcpServer.class.getPackage() == null
                ? null : EquinixMcpServer.class.getPackage().getImplementationVersion();
        return version == null || version.isEmpty() ? "0.0.0-dev" : version;
    }

    /**
     * Closes the MCP server gracefully and, when this instance created its own
     * {@link Equinix} session from credentials, closes that session too.
     */
    @Override
    public void close() {
        try {
            mcpServer.closeGracefully();
        }
        finally {
            if (ownedSession != null) {
                try {
                    ownedSession.close();
                }
                catch (Exception e) {
                    logger.warn("Closing the Equinix session failed: {}", e.toString());
                }
            }
        }
    }

    /** Builder for {@link EquinixMcpServer}. */
    public static final class Builder {

        private Equinix session;
        private EquinixCredentials credentials;
        private boolean sandbox;
        private ServerContext context;
        private Set<Toolset> toolsets = Toolset.defaults();
        private final List<ToolRegistration> additionalTools = new ArrayList<>();
        private McpServerTransportProvider transportProvider;
        private String instructions;

        /**
         * Serves over an existing authenticated session (shared token + pool). The session
         * is <em>not</em> closed by {@link EquinixMcpServer#close()} — its owner keeps it.
         *
         * @param session the session facade to execute through
         * @return this builder
         */
        public Builder session(Equinix session) {
            this.session = session;
            return this;
        }

        /**
         * Serves under client credentials; a dedicated {@link Equinix} session is created at
         * {@link #build()} and closed with the server.
         *
         * @param accessKey the Equinix API access key ({@code EQUINIX_ACCESS_KEY})
         * @param secretKey the Equinix API secret key ({@code EQUINIX_SECRET_KEY})
         * @return this builder
         */
        public Builder credentials(String accessKey, String secretKey) {
            this.credentials = new BasicEquinixCredentials(accessKey, secretKey);
            return this;
        }

        /**
         * Serves under the given credentials object (alternative to the two-string form).
         *
         * @param credentials the credentials to open the session with
         * @return this builder
         */
        public Builder credentials(EquinixCredentials credentials) {
            this.credentials = credentials;
            return this;
        }

        /**
         * Targets the Equinix sandbox environment when a session is created from credentials.
         *
         * @param sandbox {@code true} for sandbox, {@code false} (default) for production
         * @return this builder
         */
        public Builder sandbox(boolean sandbox) {
            this.sandbox = sandbox;
            return this;
        }

        /**
         * Supplies a fully assembled {@link ServerContext} — the test seam, and the way to
         * inject pre-built facades. Overrides {@link #session} / {@link #credentials}.
         *
         * @param context the context to execute against
         * @return this builder
         */
        public Builder context(ServerContext context) {
            this.context = context;
            return this;
        }

        /**
         * Selects which toolsets to serve. Default: every read-only toolset
         * ({@link Toolset#defaults()}) — the {@code mutate} toolset is served only when
         * explicitly listed here.
         *
         * @param toolsets the toolsets to expose
         * @return this builder
         */
        public Builder toolsets(Toolset... toolsets) {
            this.toolsets = toolsets.length == 0
                    ? Toolset.defaults() : EnumSet.copyOf(Arrays.asList(toolsets));
            return this;
        }

        /**
         * Selects toolsets from a comma-separated id list ({@code "design,ibx"}), as used by
         * {@code EQUINIX_MCP_TOOLSETS} and {@code --toolsets}. Blank selects everything.
         *
         * @param csv the comma-separated toolset ids
         * @return this builder
         */
        public Builder toolsets(String csv) {
            this.toolsets = Toolset.parse(csv);
            return this;
        }

        /**
         * Registers extra tools alongside the built-in catalog — the seam the Safe Mutation
         * Broker's two-phase mutation tools use. Names must not collide with existing tools.
         *
         * @param tools the additional registrations
         * @return this builder
         */
        public Builder additionalTools(ToolRegistration... tools) {
            this.additionalTools.addAll(Arrays.asList(tools));
            return this;
        }

        /**
         * Overrides the transport. Default: {@link StdioServerTransportProvider} over the
         * process's stdin/stdout. Tests inject a stdio provider over piped streams here;
         * there is deliberately no HTTP transport.
         *
         * @param transportProvider the transport to serve on
         * @return this builder
         */
        public Builder transportProvider(McpServerTransportProvider transportProvider) {
            this.transportProvider = transportProvider;
            return this;
        }

        /**
         * Overrides the server instructions sent to clients at initialize time.
         *
         * @param instructions the instructions text
         * @return this builder
         */
        public Builder instructions(String instructions) {
            this.instructions = instructions;
            return this;
        }

        /**
         * Assembles the context, registers the selected tools, and starts serving on the
         * transport (the stdio transport begins reading as soon as a session is created).
         *
         * @return the running server
         * @throws IllegalStateException when neither a context, session, nor credentials
         *         were provided, or when tool names collide
         */
        public EquinixMcpServer build() {
            Equinix ownedSession = null;
            ServerContext effectiveContext = this.context;
            if (effectiveContext == null) {
                Equinix effectiveSession = this.session;
                if (effectiveSession == null && this.credentials != null) {
                    effectiveSession = new Equinix(this.credentials, this.sandbox);
                    ownedSession = effectiveSession;
                }
                if (effectiveSession == null) {
                    throw new IllegalStateException("EquinixMcpServer needs a session, credentials, or a "
                            + "pre-built ServerContext before build().");
                }
                effectiveContext = ServerContext.builder().session(effectiveSession).build();
            }

            List<ToolRegistration> selected = new ArrayList<>(catalog(toolsets));
            if (toolsets.contains(Toolset.MUTATE)) {
                // The Safe Mutation Broker: strictly opt-in, and per-instance because its two
                // tools share this server's own proposal store (single-use confirm tokens).
                selected.addAll(BrokerToolFactory.tools());
            }
            selected.addAll(additionalTools);

            Set<String> names = new HashSet<>();
            for (ToolRegistration registration : selected) {
                if (!names.add(registration.getName())) {
                    throw new IllegalStateException("Duplicate tool name '" + registration.getName() + "'.");
                }
            }

            List<McpServerFeatures.SyncToolSpecification> specifications = new ArrayList<>(selected.size());
            for (ToolRegistration registration : selected) {
                specifications.add(McpToolAdapter.toSpecification(registration, effectiveContext));
            }

            McpServerTransportProvider transport = this.transportProvider != null
                    ? this.transportProvider
                    : new StdioServerTransportProvider(new JacksonMcpJsonMapper(effectiveContext.objectMapper()));

            String effectiveInstructions = this.instructions;
            if (effectiveInstructions == null) {
                String base = "Community Equinix intelligence server (not affiliated with Equinix; "
                        + "unrelated to Equinix's private-beta Fabric MCP server). Design engines "
                        + "(placement optimization, deployment planning, latency, TCO, egress savings, "
                        + "peering) plus portal/network-edge/IBX lookups are read-only; "
                        + "design_plan_deployment only PLANS.";
                effectiveInstructions = toolsets.contains(Toolset.MUTATE)
                        ? base + " The opt-in mutate toolset is enabled: fabric_propose_change runs a real "
                        + "dry-run validation (provisioning nothing) and returns a single-use confirm "
                        + "token for human review; only fabric_confirm_change with that token executes "
                        + "the create. There are no update or delete tools."
                        : base + " All tools are read-only — nothing is ever provisioned by this server.";
            }

            McpSyncServer mcpServer = McpServer.sync(transport)
                    .serverInfo(SERVER_NAME, serverVersion())
                    .capabilities(McpSchema.ServerCapabilities.builder().tools(false).build())
                    .instructions(effectiveInstructions)
                    .jsonMapper(new JacksonMcpJsonMapper(effectiveContext.objectMapper()))
                    .tools(specifications)
                    .build();

            logger.info("{} v{} serving {} tools over stdio (toolsets: {})",
                    SERVER_NAME, serverVersion(), selected.size(), toolsets);
            return new EquinixMcpServer(mcpServer, effectiveContext, selected, ownedSession);
        }
    }
}
