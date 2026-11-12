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

import com.eqixiac.equinix.Operations;
import com.eqixiac.equinix.Design;
import com.eqixiac.equinix.Equinix;
import com.eqixiac.equinix.FabricGateway;
import com.eqixiac.equinix.IBXSmartView;
import com.eqixiac.equinix.NetworkEdge;
import com.eqixiac.equinix.design.optimizer.model.MulticloudEnvironmentCatalog;
import com.eqixiac.equinix.design.value.ratecard.RateCard;
import com.eqixiac.equinix.design.value.ratecard.provider.AwsPriceListRateCard;
import com.eqixiac.equinix.design.value.ratecard.provider.AzureRetailPricesRateCard;
import com.eqixiac.equinix.design.value.ratecard.provider.GcpBillingCatalogRateCard;
import com.eqixiac.equinix.design.value.ratecard.provider.OracleCloudPriceListRateCard;
import com.eqixiac.equinix.fabric.model.MetroRegistry;
import com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.modelcontextprotocol.server.McpSyncServerExchange;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The shared state a {@link ToolHandler} executes against: the lazily-built SDK facades
 * (Fabric + design engines, operations, Network Edge, IBX SmartView), the environment,
 * the JSON mapper, and the single-process {@link DesignStore} of plans and optimization results.
 *
 * <p>Facades are created on first use, per facade, so a server launched with only the
 * {@code ibx} toolset never constructs a operations client. All facades vend from one
 * {@link Equinix} session (one OAuth token, one connection pool) when the context was built
 * from a session or credentials; alternatively each facade can be injected directly - that is
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

    /**
     * Environment variable overriding the live-pricing budget of one tool call, in ms: every live
     * pricing lookup the call makes (cloud price lists and the Equinix rate card) shares it.
     */
    public static final String ENV_PRICING_TIMEOUT_MS = "EQUINIX_MCP_PRICING_TIMEOUT_MS";

    /** Default live-pricing budget of one tool call. */
    public static final long DEFAULT_PRICING_TIMEOUT_MS = 12_000L;

    /** Smallest accepted pricing budget; smaller values are raised to it with a warning. */
    public static final long MIN_PRICING_TIMEOUT_MS = 100L;

    /**
     * Largest accepted pricing budget (2 minutes); larger values are lowered to it with a warning,
     * because a tool call that waits longer than that is indistinguishable from a hung one.
     */
    public static final long MAX_PRICING_TIMEOUT_MS = 120_000L;

    /**
     * Environment variable that, when {@code true}, lets {@code fabric_confirm_change} execute when
     * the MCP client did not declare elicitation support (so no human prompt can be shown). Off by
     * default: without it the broker refuses to execute rather than create billable resources with
     * no human in the loop.
     */
    public static final String ENV_ALLOW_UNCONFIRMED_MUTATIONS = "EQUINIX_MCP_ALLOW_UNCONFIRMED_MUTATIONS";

    /** Environment variable overriding the hard timeout for a single elicitation round-trip, in ms. */
    public static final String ENV_ELICIT_TIMEOUT_MS = "EQUINIX_MCP_ELICIT_TIMEOUT_MS";

    /**
     * Default hard timeout for a single elicitation round-trip. Generous (a human may be answering
     * the prompt) but bounded, so a tool call can never block forever waiting on the client.
     */
    public static final long DEFAULT_ELICIT_TIMEOUT_MS = 300_000L;

    /** Smallest accepted elicitation timeout; smaller values are raised to it with a warning. */
    public static final long MIN_ELICIT_TIMEOUT_MS = 100L;

    /**
     * Largest accepted elicitation timeout (1 hour); larger values are lowered to it with a warning.
     * The MCP SDK's request timeout is set just above this value, so it also bounds how long a
     * stalled client can hold a server-to-client request open.
     */
    public static final long MAX_ELICIT_TIMEOUT_MS = 3_600_000L;

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ServerContext.class);

    /** Timeout settings already warned about, so a bad value is logged once, not on every call. */
    private final java.util.Set<String> warnedSettings = ConcurrentHashMap.newKeySet();

    private final Equinix session;
    private final Map<String, String> environment;
    private final ObjectMapper objectMapper;
    private final DesignStore designStore;
    private final java.util.function.Function<com.eqixiac.equinix.design.estate.EstateScope,
            com.eqixiac.equinix.design.estate.EstateAnalyzer.Builder> estateAnalyzerFactory;
    private final ProviderRateCardFactory providerRateCardFactory;

    /**
     * Lazily-built live pricing adapters, one per provider for the context's lifetime. The
     * adapters memoize their (multi-megabyte) offer/SKU catalogues internally, so they must be
     * reused across tool calls - constructing a fresh adapter per call would refetch the
     * catalogue on every invocation and the adapter-lifetime caching would never help.
     */
    private final Map<CloudProviderType, Optional<RateCard>> providerRateCards = new ConcurrentHashMap<>();

    /**
     * What is bound for the duration of the current tool call (client exchange, cancellation signal,
     * pricing budget), or {@code null} when a handler is invoked outside a served call (e.g. a unit
     * test invoking a handler directly). Held per thread - the sync server runs each
     * {@code callHandler} on one thread - so concurrent calls stay isolated.
     */
    private final ThreadLocal<CallScope> currentCall = new ThreadLocal<>();

    /** How {@link #confirmWithHuman(String)} asks a person; MCP form elicitation by default. */
    private final HumanConfirmer humanConfirmer;

    private FabricGateway fabric;
    private Design design;
    private Operations operations;
    private NetworkEdge networkEdge;
    private IBXSmartView ibxSmartView;
    private MetroRegistry metroRegistry;
    private MulticloudEnvironmentCatalog multicloudEnvironments;

    private ServerContext(Builder builder) {
        this.session = builder.session;
        this.environment = builder.environment != null ? builder.environment : System.getenv();
        this.objectMapper = builder.objectMapper != null ? builder.objectMapper : defaultMapper();
        this.designStore = builder.designStore != null ? builder.designStore : new DesignStore();
        this.estateAnalyzerFactory = builder.estateAnalyzerFactory;
        this.providerRateCardFactory = builder.providerRateCardFactory != null
                ? builder.providerRateCardFactory : ServerContext::defaultProviderRateCard;
        this.fabric = builder.fabric;
        this.design = builder.design;
        this.operations = builder.operations;
        this.networkEdge = builder.networkEdge;
        this.ibxSmartView = builder.ibxSmartView;
        this.metroRegistry = builder.metroRegistry;
        this.multicloudEnvironments = builder.multicloudEnvironments;
        this.humanConfirmer = builder.humanConfirmer != null
                ? builder.humanConfirmer : HumanConfirmer.elicitation();
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
     * @return the operations client
     */
    public synchronized Operations operations() {
        if (operations == null) {
            operations = requireSession("Operations").operations();
        }
        return operations;
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
     * The catalog of native provider-to-provider multicloud environments the design tools read:
     * {@code design_list_multicloud_environments}, {@code design_compare_cloud_to_cloud} and the
     * cloud-to-cloud step of {@code design_plan_deployment}. <b>Beta.</b> Defaults to
     * {@link MulticloudEnvironmentCatalog#standard()}, a dated copy of provider documentation bundled
     * with the SDK; it is loaded on first use and is not refreshed at run time. An embedder with newer
     * data injects {@code MulticloudEnvironmentCatalog.standard().with(...)} through the builder.
     *
     * @return the catalog; never {@code null}
     */
    public synchronized MulticloudEnvironmentCatalog multicloudEnvironments() {
        if (multicloudEnvironments == null) {
            multicloudEnvironments = MulticloudEnvironmentCatalog.standard();
        }
        return multicloudEnvironments;
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
     * @return the single-process store of deployment plans and optimization results the design tools
     *         chain through ({@code plan_id}, {@code spec_fingerprint})
     */
    public DesignStore designStore() {
        return designStore;
    }

    /**
     * A read-only estate analysis bound to {@code scope}: by default over this context's Fabric client
     * (which must be a full {@code Fabric}, not a bare gateway) plus Network Edge when a session is
     * available; a builder-supplied factory replaces it (tests, snapshots).
     *
     * @param scope the scope; must name projects, accounts or resources
     * @return the analysis builder with the scope set
     * @throws IllegalArgumentException when the scope is unscoped or asks for the entire account
     */
    public com.eqixiac.equinix.design.estate.EstateAnalyzer.Builder estateAnalyzer(
            com.eqixiac.equinix.design.estate.EstateScope scope) {
        if (scope == null || scope.isUnscoped() || scope.entireAccount()) {
            throw new IllegalArgumentException("An estate analysis served over MCP must be scoped to named "
                    + "projects; the whole account is never analysed.");
        }
        if (estateAnalyzerFactory != null) {
            return estateAnalyzerFactory.apply(scope).scope(scope);
        }
        com.eqixiac.equinix.design.estate.EstateAnalyzer.Builder builder = design().analyzeEstate().scope(scope);
        if (session != null) {
            builder.networkEdge(networkEdge());
        }
        return builder;
    }

    /**
     * Returns the live pricing adapter for a cloud provider, if one can be constructed
     * (GCP requires {@link #ENV_GCP_BILLING_KEY}). The adapter is built once per provider and
     * reused for the context's lifetime, so its internal offer/SKU catalogue caches actually pay
     * off across tool calls. The returned card is <em>not</em> yet timeout-guarded - 
     * {@code design_compare_cloud_egress} wraps it per invocation.
     *
     * @param provider the cloud provider
     * @return the live rate card, or empty when no adapter is available for the provider
     */
    public Optional<RateCard> providerRateCard(CloudProviderType provider) {
        if (provider == null) {
            return providerRateCardFactory.create(null, this);
        }
        return providerRateCards.computeIfAbsent(provider, p -> providerRateCardFactory.create(p, this));
    }

    /**
     * The live-pricing budget of one tool call, from {@link #ENV_PRICING_TIMEOUT_MS}. A value that is
     * not a positive whole number falls back to {@link #DEFAULT_PRICING_TIMEOUT_MS}; a value outside
     * [{@link #MIN_PRICING_TIMEOUT_MS}, {@link #MAX_PRICING_TIMEOUT_MS}] is clipped to the nearest
     * bound. Either case logs one warning naming the variable (the value is a number, not a secret).
     *
     * @return the budget in milliseconds
     */
    public long pricingTimeoutMillis() {
        return boundedMillis(ENV_PRICING_TIMEOUT_MS, DEFAULT_PRICING_TIMEOUT_MS,
                MIN_PRICING_TIMEOUT_MS, MAX_PRICING_TIMEOUT_MS);
    }

    /**
     * The hard timeout for one elicitation round trip, from {@link #ENV_ELICIT_TIMEOUT_MS}, with the
     * same fallback and clipping rules as {@link #pricingTimeoutMillis()} over
     * [{@link #MIN_ELICIT_TIMEOUT_MS}, {@link #MAX_ELICIT_TIMEOUT_MS}].
     *
     * @return the timeout in milliseconds
     */
    public long elicitTimeoutMillis() {
        return boundedMillis(ENV_ELICIT_TIMEOUT_MS, DEFAULT_ELICIT_TIMEOUT_MS,
                MIN_ELICIT_TIMEOUT_MS, MAX_ELICIT_TIMEOUT_MS);
    }

    private long boundedMillis(String variable, long fallback, long min, long max) {
        Optional<String> raw = env(variable);
        if (raw.isEmpty()) {
            return fallback;
        }
        long parsed;
        try {
            parsed = Long.parseLong(raw.get());
        }
        catch (NumberFormatException e) {
            warnOnce(variable, raw.get(), "is not a whole number of milliseconds; using the default "
                    + fallback + " ms");
            return fallback;
        }
        if (parsed <= 0) {
            warnOnce(variable, raw.get(), "must be positive; using the default " + fallback + " ms");
            return fallback;
        }
        if (parsed < min) {
            warnOnce(variable, raw.get(), "is below the minimum; using " + min + " ms");
            return min;
        }
        if (parsed > max) {
            warnOnce(variable, raw.get(), "is above the maximum; using " + max + " ms");
            return max;
        }
        return parsed;
    }

    private void warnOnce(String variable, String raw, String what) {
        if (warnedSettings.add(variable + "=" + raw)) {
            // Only the variable name and a length are logged: these settings hold numbers, but a
            // misplaced secret must never reach stderr.
            logger.warn("{} ({} characters) {}.", variable, raw.length(), what);
        }
    }

    /**
     * The live-pricing budget of the current tool call: started when the call began and shared by
     * every pricing lookup it makes. Outside a served call (a direct handler invocation) a fresh
     * budget is returned, so a handler should read this once and reuse it.
     *
     * @return the budget
     */
    public PricingBudget pricingBudget() {
        CallScope scope = currentCall.get();
        return scope != null ? scope.pricingBudget() : new PricingBudget(pricingTimeoutMillis(), null);
    }

    /**
     * The Equinix rate-card chain (live Equinix pricing, then the bundled reference figures) with
     * the live layer under the current call's {@link #pricingBudget()}: the chain extension tools
     * such as the Safe Mutation Broker price with. A stalled live lookup falls through to the
     * reference figures when the budget runs out.
     *
     * @return the budgeted chain
     */
    public RateCard budgetedEquinixRateCard() {
        return TimeoutRateCard.equinixChain(fabric(), pricingBudget(), new java.util.ArrayList<>());
    }

    /**
     * @return the cancellation signal of the current tool call; a never-cancelled signal outside a
     *         served call
     */
    public CancellationSignal currentCancellation() {
        CallScope scope = currentCall.get();
        return scope != null ? scope.cancellation() : CancellationSignal.none();
    }

    /**
     * @return {@code true} only when {@link #ENV_ALLOW_UNCONFIRMED_MUTATIONS} is set to {@code true}:
     *         the operator has accepted that, on a client without elicitation, the host's own
     *         tool-approval prompt is the only human check before a create executes
     */
    public boolean allowUnconfirmedMutations() {
        return env(ENV_ALLOW_UNCONFIRMED_MUTATIONS).map(v -> "true".equalsIgnoreCase(v.trim())).orElse(false);
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
        CallScope scope = currentCall.get();
        return scope == null ? null : scope.exchange();
    }

    /**
     * Asks the person at the MCP client bound to the current tool call to approve one action, through
     * a form elicitation with one required boolean field. This is the public seam for tools outside
     * this package (the Safe Mutation Broker) that gate a mutation on a human decision; the
     * elicitation helper itself stays package-private.
     *
     * <p>Behaviour: when {@link #currentExchange()} is {@code null} or the client did not declare
     * form elicitation at initialize, nothing is sent and the result is
     * {@link HumanConfirmation.Status#UNSUPPORTED_BY_CLIENT}. Otherwise the call blocks until the
     * client answers or {@link #elicitTimeoutMillis()} elapses. It does not throw for a client-side
     * failure: decline, cancel, timeout and transport errors are statuses on the result. Only
     * {@link HumanConfirmation.Status#ACCEPTED} is an approval.</p>
     *
     * <p>The prompt is delivered by this context's {@link HumanConfirmer} (MCP form elicitation by
     * default) and is abandoned when the client cancels the tool call
     * ({@link HumanConfirmation.Status#CALL_CANCELLED}).</p>
     *
     * @param message the prompt shown to the user; state what will be executed and what each answer does
     * @return what the client reported
     */
    public HumanConfirmation confirmWithHuman(String message) {
        return humanConfirmer.confirm(this, message, currentCancellation());
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
        return withCall(exchange, CancellationSignal.none(), body);
    }

    /**
     * Runs {@code body} as one tool call: binds the exchange, the call's cancellation signal and a
     * freshly started {@link PricingBudget}, and restores the previous binding afterwards.
     *
     * @param exchange the exchange to bind (may be {@code null})
     * @param cancellation the call's cancellation signal
     * @param body the work to run with the binding in effect
     * @param <T> the body's result type
     * @return the body's result
     * @throws Exception whatever {@code body} throws
     */
    <T> T withCall(McpSyncServerExchange exchange, CancellationSignal cancellation, Callable<T> body)
            throws Exception {
        CallScope previous = currentCall.get();
        currentCall.set(new CallScope(exchange, cancellation,
                new PricingBudget(pricingTimeoutMillis(), cancellation)));
        try {
            return body.call();
        }
        finally {
            if (previous == null) {
                currentCall.remove();
            }
            else {
                currentCall.set(previous);
            }
        }
    }

    /** What is bound to the thread for the duration of one tool call. */
    private record CallScope(McpSyncServerExchange exchange, CancellationSignal cancellation,
                             PricingBudget pricingBudget) {
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
     * failing cards) and for embedders with custom pricing endpoints. Invoked at most once per
     * provider per context - {@link #providerRateCard(CloudProviderType)} caches the result.
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
        private DesignStore designStore;
        private java.util.function.Function<com.eqixiac.equinix.design.estate.EstateScope,
                com.eqixiac.equinix.design.estate.EstateAnalyzer.Builder> estateAnalyzerFactory;
        private ProviderRateCardFactory providerRateCardFactory;
        private FabricGateway fabric;
        private Design design;
        private Operations operations;
        private NetworkEdge networkEdge;
        private IBXSmartView ibxSmartView;
        private MetroRegistry metroRegistry;
        private MulticloudEnvironmentCatalog multicloudEnvironments;
        private HumanConfirmer humanConfirmer;

        /**
         * Replaces how {@link ServerContext#confirmWithHuman(String)} asks a person. Default:
         * {@link HumanConfirmer#elicitation()}, an MCP form elicitation over the bound exchange.
         *
         * @param confirmer the confirmer; {@code null} restores the default
         * @return this builder
         */
        public Builder humanConfirmer(HumanConfirmer confirmer) {
            this.humanConfirmer = confirmer;
            return this;
        }

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
         * Overrides the design store (e.g. with a custom TTL or a test clock).
         *
         * @param designStore the store to use
         * @return this builder
         */
        public Builder designStore(DesignStore designStore) {
            this.designStore = designStore;
            return this;
        }

        /**
         * Replaces how {@code design_analyze_estate} obtains its analysis (for example
         * {@code scope -> EstateAnalyzer.fromSnapshot(inventory)} in tests). The scope is set on the
         * returned builder afterwards.
         *
         * @param factory the factory
         * @return this builder
         */
        public Builder estateAnalyzerFactory(java.util.function.Function<com.eqixiac.equinix.design.estate.EstateScope,
                com.eqixiac.equinix.design.estate.EstateAnalyzer.Builder> factory) {
            this.estateAnalyzerFactory = factory;
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
         * Injects the operations client directly.
         *
         * @param operations the client to use
         * @return this builder
         */
        public Builder operations(Operations operations) {
            this.operations = operations;
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
         * Overrides the native multicloud environment catalog (defaults to the bundled
         * {@link MulticloudEnvironmentCatalog#standard()}). <b>Beta.</b>
         *
         * @param catalog the catalog the design tools read; {@code null} restores the default
         * @return this builder
         */
        public Builder multicloudEnvironments(MulticloudEnvironmentCatalog catalog) {
            this.multicloudEnvironments = catalog;
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
