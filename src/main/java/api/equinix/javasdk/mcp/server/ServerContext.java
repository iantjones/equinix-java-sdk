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

import api.equinix.javasdk.CustomerPortal;
import api.equinix.javasdk.Design;
import api.equinix.javasdk.Equinix;
import api.equinix.javasdk.FabricGateway;
import api.equinix.javasdk.IBXSmartView;
import api.equinix.javasdk.NetworkEdge;
import api.equinix.javasdk.design.value.ratecard.RateCard;
import api.equinix.javasdk.design.value.ratecard.provider.AwsPriceListRateCard;
import api.equinix.javasdk.design.value.ratecard.provider.AzureRetailPricesRateCard;
import api.equinix.javasdk.design.value.ratecard.provider.GcpBillingCatalogRateCard;
import api.equinix.javasdk.design.value.ratecard.provider.OracleCloudPriceListRateCard;
import api.equinix.javasdk.fabric.model.MetroRegistry;
import api.equinix.javasdk.fabric.model.implementation.cloud.CloudProviderType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.modelcontextprotocol.server.McpSyncServerExchange;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;

/**
 * The shared state a {@link ToolHandler} executes against: the lazily-built SDK facades
 * (Fabric + design engines, Customer Portal, Network Edge, IBX SmartView), the environment,
 * the JSON mapper, and the single-process {@link PlanStore}.
 *
 * <p>Facades are created on first use, per facade, so a server launched with only the
 * {@code ibx} toolset never constructs a Customer Portal client. All facades vend from one
 * {@link Equinix} session (one OAuth token, one connection pool) when the context was built
 * from a session or credentials; alternatively each facade can be injected directly — that is
 * how tests supply Mockito-stubbed facades, and how embedders can reuse clients they already
 * hold.</p>
 *
 * <p>This type is part of the public tool seam: broker/extension tools registered through
 * {@link ToolRegistration} receive the same context instance as the built-in catalog.</p>
 */
public final class ServerContext {

    /** Environment variable holding an optional PeeringDB API key for {@code design_analyze_peering}. */
    public static final String ENV_PEERINGDB_KEY = "EQUINIX_PEERINGDB_KEY";

    /** Environment variable holding an optional Google Cloud Billing Catalog API key. */
    public static final String ENV_GCP_BILLING_KEY = "GCP_BILLING_API_KEY";

    /** Environment variable overriding the per-lookup hard timeout for live provider pricing, in ms. */
    public static final String ENV_PRICING_TIMEOUT_MS = "EQUINIX_MCP_PRICING_TIMEOUT_MS";

    /** Default hard timeout for a single live provider-pricing lookup. */
    public static final long DEFAULT_PRICING_TIMEOUT_MS = 12_000L;

    /** Environment variable overriding the hard timeout for a single elicitation round-trip, in ms. */
    public static final String ENV_ELICIT_TIMEOUT_MS = "EQUINIX_MCP_ELICIT_TIMEOUT_MS";

    /**
     * Default hard timeout for a single elicitation round-trip. Generous (a human may be answering
     * the prompt) but bounded, so a tool call can never block forever waiting on the client.
     */
    public static final long DEFAULT_ELICIT_TIMEOUT_MS = 300_000L;

    private final Equinix session;
    private final Map<String, String> environment;
    private final ObjectMapper objectMapper;
    private final PlanStore planStore;
    private final ProviderRateCardFactory providerRateCardFactory;

    /**
     * The MCP client exchange bound for the duration of the current tool call, or {@code null} when a
     * handler is invoked outside a served call (e.g. a unit test invoking a handler directly). Held per
     * thread — the sync server runs each {@code callHandler} on the thread that reads
     * {@link #currentExchange()}, so a request-scoped {@link ThreadLocal} keeps concurrent calls
     * isolated without threading a new parameter through the whole {@link ToolHandler} seam.
     */
    private final ThreadLocal<McpSyncServerExchange> currentExchange = new ThreadLocal<>();

    private FabricGateway fabric;
    private Design design;
    private CustomerPortal customerPortal;
    private NetworkEdge networkEdge;
    private IBXSmartView ibxSmartView;
    private MetroRegistry metroRegistry;

    private ServerContext(Builder builder) {
        this.session = builder.session;
        this.environment = builder.environment != null ? builder.environment : System.getenv();
        this.objectMapper = builder.objectMapper != null ? builder.objectMapper : defaultMapper();
        this.planStore = builder.planStore != null ? builder.planStore : new PlanStore();
        this.providerRateCardFactory = builder.providerRateCardFactory != null
                ? builder.providerRateCardFactory : ServerContext::defaultProviderRateCard;
        this.fabric = builder.fabric;
        this.design = builder.design;
        this.customerPortal = builder.customerPortal;
        this.networkEdge = builder.networkEdge;
        this.ibxSmartView = builder.ibxSmartView;
        this.metroRegistry = builder.metroRegistry;
    }

    /**
     * @return a new context builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return the Fabric gateway the design engines read from
     */
    public synchronized FabricGateway fabric() {
        if (fabric == null) {
            fabric = requireSession("Fabric").fabric();
        }
        return fabric;
    }

    /**
     * @return the design facade (optimizer, wizard, peering, savings, TCO) over {@link #fabric()}
     */
    public synchronized Design design() {
        if (design == null) {
            design = Design.over(fabric());
        }
        return design;
    }

    /**
     * @return the Customer Portal client
     */
    public synchronized CustomerPortal customerPortal() {
        if (customerPortal == null) {
            customerPortal = requireSession("Customer Portal").customerPortal();
        }
        return customerPortal;
    }

    /**
     * @return the Network Edge client
     */
    public synchronized NetworkEdge networkEdge() {
        if (networkEdge == null) {
            networkEdge = requireSession("Network Edge").networkEdge();
        }
        return networkEdge;
    }

    /**
     * @return the IBX SmartView client
     */
    public synchronized IBXSmartView ibxSmartView() {
        if (ibxSmartView == null) {
            ibxSmartView = requireSession("IBX SmartView").ibxSmartView();
        }
        return ibxSmartView;
    }

    /**
     * @return the live metro catalogue (metros, IBXs, coordinates) shared by the design tools
     */
    public synchronized MetroRegistry metroRegistry() {
        if (metroRegistry == null) {
            metroRegistry = requireSession("Metro registry").metroRegistry();
        }
        return metroRegistry;
    }

    /**
     * Looks up an environment variable from this context's environment view (the real process
     * environment unless overridden for tests).
     *
     * @param name the variable name
     * @return the value, or empty when unset or blank
     */
    public Optional<String> env(String name) {
        String value = environment.get(name);
        return value == null || value.trim().isEmpty() ? Optional.empty() : Optional.of(value.trim());
    }

    /**
     * @return the JSON mapper used for argument parsing and payload building
     */
    public ObjectMapper objectMapper() {
        return objectMapper;
    }

    /**
     * @return the single-process plan store shared by {@code design_plan_deployment} and
     *         {@code design_export_terraform}
     */
    public PlanStore planStore() {
        return planStore;
    }

    /**
     * Builds the live pricing adapter for a cloud provider, if one can be constructed
     * (GCP requires {@link #ENV_GCP_BILLING_KEY}). The returned card is <em>not</em> yet
     * timeout-guarded — {@code design_compare_cloud_egress} wraps it.
     *
     * @param provider the cloud provider
     * @return the live rate card, or empty when no adapter is available for the provider
     */
    public Optional<RateCard> providerRateCard(CloudProviderType provider) {
        return providerRateCardFactory.create(provider, this);
    }

    /**
     * @return the hard per-lookup timeout for live provider pricing, from
     *         {@link #ENV_PRICING_TIMEOUT_MS} or the {@link #DEFAULT_PRICING_TIMEOUT_MS default}
     */
    public long pricingTimeoutMillis() {
        return env(ENV_PRICING_TIMEOUT_MS).map(v -> {
            try {
                return Long.parseLong(v);
            }
            catch (NumberFormatException e) {
                return DEFAULT_PRICING_TIMEOUT_MS;
            }
        }).orElse(DEFAULT_PRICING_TIMEOUT_MS);
    }

    /**
     * @return the hard timeout for a single elicitation round-trip, from {@link #ENV_ELICIT_TIMEOUT_MS}
     *         or the {@link #DEFAULT_ELICIT_TIMEOUT_MS default}
     */
    public long elicitTimeoutMillis() {
        return env(ENV_ELICIT_TIMEOUT_MS).map(v -> {
            try {
                return Long.parseLong(v);
            }
            catch (NumberFormatException e) {
                return DEFAULT_ELICIT_TIMEOUT_MS;
            }
        }).orElse(DEFAULT_ELICIT_TIMEOUT_MS);
    }

    /**
     * The MCP client exchange bound for the current tool call, or {@code null} when the handler is
     * running outside a served call (a direct unit-test invocation, or a transport that supplied none).
     * A handler that wants to prompt the user reads this and hands it to {@link ElicitationSupport};
     * a {@code null} exchange means "no interactive client", and the elicitation degrades cleanly.
     *
     * @return the current call's exchange, or {@code null}
     */
    public McpSyncServerExchange currentExchange() {
        return currentExchange.get();
    }

    /**
     * Runs {@code body} with {@code exchange} bound as the {@link #currentExchange()} for its duration,
     * restoring the previous binding afterwards. The adapter wraps every handler invocation in this so a
     * handler can elicit; tests use it to bind a stub exchange around a direct handler call. A
     * {@code null} exchange clears the binding for the call.
     *
     * @param exchange the exchange to bind (may be {@code null})
     * @param body the work to run with the binding in effect
     * @param <T> the body's result type
     * @return the body's result
     * @throws Exception whatever {@code body} throws
     */
    <T> T withExchange(McpSyncServerExchange exchange, Callable<T> body) throws Exception {
        McpSyncServerExchange previous = currentExchange.get();
        if (exchange == null) {
            currentExchange.remove();
        }
        else {
            currentExchange.set(exchange);
        }
        try {
            return body.call();
        }
        finally {
            if (previous == null) {
                currentExchange.remove();
            }
            else {
                currentExchange.set(previous);
            }
        }
    }

    private Equinix requireSession(String what) {
        if (session == null) {
            throw new IllegalStateException(what + " is unavailable: this server context was built without "
                    + "an Equinix session or credentials, and no facade was injected for it.");
        }
        return session;
    }

    private static ObjectMapper defaultMapper() {
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        return mapper;
    }

    private static Optional<RateCard> defaultProviderRateCard(CloudProviderType provider, ServerContext ctx) {
        switch (provider) {
            case AWS:
                return Optional.of(AwsPriceListRateCard.create());
            case AZURE:
                return Optional.of(AzureRetailPricesRateCard.create());
            case GOOGLE_CLOUD:
                return ctx.env(ENV_GCP_BILLING_KEY).map(GcpBillingCatalogRateCard::create);
            case ORACLE_CLOUD:
                return Optional.of(OracleCloudPriceListRateCard.create());
            default:
                return Optional.empty();
        }
    }

    /**
     * Creates the live pricing adapter for a provider. Replaceable for tests (canned or
     * failing cards) and for embedders with custom pricing endpoints.
     */
    @FunctionalInterface
    public interface ProviderRateCardFactory {

        /**
         * @param provider the cloud provider to price against
         * @param context the owning context (for environment lookups)
         * @return the adapter, or empty when none is available
         */
        Optional<RateCard> create(CloudProviderType provider, ServerContext context);
    }

    /** Builder for {@link ServerContext}. */
    public static final class Builder {

        private Equinix session;
        private Map<String, String> environment;
        private ObjectMapper objectMapper;
        private PlanStore planStore;
        private ProviderRateCardFactory providerRateCardFactory;
        private FabricGateway fabric;
        private Design design;
        private CustomerPortal customerPortal;
        private NetworkEdge networkEdge;
        private IBXSmartView ibxSmartView;
        private MetroRegistry metroRegistry;

        /**
         * Uses an authenticated {@link Equinix} session as the source of every facade not
         * explicitly injected.
         *
         * @param session the shared session
         * @return this builder
         */
        public Builder session(Equinix session) {
            this.session = session;
            return this;
        }

        /**
         * Overrides the environment view (defaults to {@link System#getenv()}).
         *
         * @param environment the replacement environment map
         * @return this builder
         */
        public Builder environment(Map<String, String> environment) {
            this.environment = environment;
            return this;
        }

        /**
         * Overrides the JSON mapper.
         *
         * @param objectMapper the mapper to use
         * @return this builder
         */
        public Builder objectMapper(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
            return this;
        }

        /**
         * Overrides the plan store (e.g. with a custom TTL or a test clock).
         *
         * @param planStore the store to use
         * @return this builder
         */
        public Builder planStore(PlanStore planStore) {
            this.planStore = planStore;
            return this;
        }

        /**
         * Overrides how live cloud-pricing adapters are created.
         *
         * @param factory the replacement factory
         * @return this builder
         */
        public Builder providerRateCardFactory(ProviderRateCardFactory factory) {
            this.providerRateCardFactory = factory;
            return this;
        }

        /**
         * Injects the Fabric gateway directly (bypassing the session).
         *
         * @param fabric the gateway to use
         * @return this builder
         */
        public Builder fabric(FabricGateway fabric) {
            this.fabric = fabric;
            return this;
        }

        /**
         * Injects the design facade directly.
         *
         * @param design the facade to use
         * @return this builder
         */
        public Builder design(Design design) {
            this.design = design;
            return this;
        }

        /**
         * Injects the Customer Portal client directly.
         *
         * @param customerPortal the client to use
         * @return this builder
         */
        public Builder customerPortal(CustomerPortal customerPortal) {
            this.customerPortal = customerPortal;
            return this;
        }

        /**
         * Injects the Network Edge client directly.
         *
         * @param networkEdge the client to use
         * @return this builder
         */
        public Builder networkEdge(NetworkEdge networkEdge) {
            this.networkEdge = networkEdge;
            return this;
        }

        /**
         * Injects the IBX SmartView client directly.
         *
         * @param ibxSmartView the client to use
         * @return this builder
         */
        public Builder ibxSmartView(IBXSmartView ibxSmartView) {
            this.ibxSmartView = ibxSmartView;
            return this;
        }

        /**
         * Injects the metro registry directly.
         *
         * @param metroRegistry the registry to use
         * @return this builder
         */
        public Builder metroRegistry(MetroRegistry metroRegistry) {
            this.metroRegistry = metroRegistry;
            return this;
        }

        /**
         * @return the built context
         */
        public ServerContext build() {
            return new ServerContext(this);
        }
    }
}
