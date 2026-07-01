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

package api.equinix.javasdk.design.peering;

import api.equinix.javasdk.FabricGateway;
import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.core.model.MetroId;
import api.equinix.javasdk.design.peering.client.PeeringDbClient;
import api.equinix.javasdk.design.peering.model.PeeringIntelligenceResult;
import api.equinix.javasdk.design.peering.model.PeeringRequest;

import java.util.*;

/**
 * Entry point for Peering Intelligence — an interconnection analysis tool that combines
 * PeeringDB IX peering data with Equinix Fabric private connectivity data to produce
 * unified presence matrices, resiliency assessments, and peering opportunity discovery.
 *
 * <h3>Capabilities</h3>
 * <ul>
 *   <li><b>Presence Matrix:</b> ASN x Metro grid showing IX peering, Fabric availability,
 *       port capacity, and route server participation at every Equinix location</li>
 *   <li><b>Resiliency Analysis:</b> Blast radius evaluation, correlated failure detection,
 *       geographic diversity scoring, and failover path identification</li>
 *   <li><b>Unified Connectivity:</b> Combined IX + Fabric view per ASN per metro —
 *       every way to reach a network through Equinix</li>
 *   <li><b>Peering Opportunities:</b> Mutual presence discovery — metros where the customer
 *       and target ASN are both at an Equinix IX but not yet peering</li>
 * </ul>
 *
 * <h3>PeeringDB credentials</h3>
 * <p>PeeringDB authentication is an API key (created on
 * <a href="https://docs.peeringdb.com/howto/api_keys/">peeringdb.com</a>) — a separate credential
 * from the Equinix OAuth client. The key is resolved in this order:</p>
 * <ol>
 *   <li>an explicit key — {@code fabric.peeringIntelligence("key")} or {@code builder(fabric, "key")}</li>
 *   <li>the {@code EquinixConfig.peeringDbApiKey} option configured on the client/session</li>
 *   <li>the {@value #PEERINGDB_API_KEY_ENV} environment variable</li>
 *   <li>anonymous access (rate-limited by PeeringDB to ~20 requests/minute)</li>
 * </ol>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * // Basic presence matrix
 * PeeringIntelligenceResult result = fabric.peeringIntelligence("your-peeringdb-api-key")
 *     .addAsn(16509, "AWS")
 *     .addAsn(8075, "Microsoft")
 *     .addAsn(15169, "Google")
 *     .analyze();
 *
 * System.out.println(result.presenceMatrix().toTableString());
 *
 * // Full analysis with customer context
 * PeeringIntelligenceResult result = fabric.peeringIntelligence("your-peeringdb-api-key")
 *     .addAsn(16509, "AWS")
 *     .addAsn(8075, "Microsoft")
 *     .customerMetros(MetroCode.DC, MetroCode.DA, MetroCode.SG)
 *     .customerAsn(65100L)
 *     .includeCapacity(true)
 *     .includePolicies(true)
 *     .includeFabricConnections(true)
 *     .includeResiliency(true)
 *     .analyze();
 *
 * System.out.println(result.toMarkdown());
 * }</pre>
 *
 * @author ianjones
 * @see PeeringIntelligenceResult
 * @see api.equinix.javasdk.design.peering.model.PresenceMatrix
 * @see api.equinix.javasdk.design.peering.model.ResiliencyAssessment
 */
public class PeeringIntelligence {

    /**
     * The environment variable consulted for a PeeringDB API key when none is supplied explicitly
     * or via {@code EquinixConfig.peeringDbApiKey}.
     */
    public static final String PEERINGDB_API_KEY_ENV = "PEERINGDB_API_KEY";

    private PeeringIntelligence() {}

    /**
     * Creates a new builder for configuring a peering intelligence analysis.
     *
     * @param fabric the Fabric client instance (used for Fabric service profile cross-referencing)
     * @param peeringDbApiKey the PeeringDB API key for authenticated access, or {@code null} to fall
     *                        back to the {@value #PEERINGDB_API_KEY_ENV} environment variable (anonymous
     *                        access when that is unset too)
     * @return a new {@link Builder}
     */
    public static Builder builder(FabricGateway fabric, String peeringDbApiKey) {
        return new Builder(fabric, peeringDbApiKey);
    }

    /**
     * Creates a new builder without an explicit PeeringDB API key: the
     * {@value #PEERINGDB_API_KEY_ENV} environment variable is used when set, anonymous access
     * (~20 requests/minute) otherwise.
     *
     * @param fabric the Fabric client instance
     * @return a new {@link Builder}
     */
    public static Builder builder(FabricGateway fabric) {
        return new Builder(fabric, null);
    }

    /**
     * Fluent builder for configuring and executing a peering intelligence analysis.
     *
     * <p>The builder collects target ASNs (with optional labels), customer metro locations,
     * the customer's own ASN, and analysis options. Calling {@link #analyze()} executes
     * the analysis pipeline and returns a {@link PeeringIntelligenceResult}.</p>
     */
    public static class Builder {

        private final FabricGateway fabric;
        private final String peeringDbApiKey;
        private final Map<Long, String> targetAsns = new LinkedHashMap<>();
        private final Set<MetroId> customerMetros = new LinkedHashSet<>();
        private long customerAsn;
        private boolean includeCapacity = true;
        private boolean includePolicies = true;
        private boolean includeFabricConnections;
        private boolean includeResiliency;

        /**
         * Test-only override for the PeeringDB base URL. When {@code null} (the default and only
         * value reachable through the public API) the client uses the real PeeringDB endpoint;
         * tests set it to point the analysis at a local stub server.
         */
        private String peeringDbBaseUrl;

        /**
         * Environment lookup used for the {@value #PEERINGDB_API_KEY_ENV} fallback. Defaults to
         * {@link System#getenv(String)}; package-private and replaceable only by tests (the JVM
         * offers no portable way to set an environment variable in a test).
         */
        private java.util.function.Function<String, String> envLookup = System::getenv;

        Builder(FabricGateway fabric, String peeringDbApiKey) {
            this.fabric = fabric;
            this.peeringDbApiKey = peeringDbApiKey;
        }

        /**
         * Test-only seam: replaces the environment lookup used for the
         * {@value #PEERINGDB_API_KEY_ENV} fallback. Package-private so it is never part of the
         * public builder surface.
         *
         * @param envLookup the replacement lookup (name → value, {@code null} when unset)
         * @return this builder
         */
        Builder envLookup(java.util.function.Function<String, String> envLookup) {
            this.envLookup = envLookup;
            return this;
        }

        /**
         * Test-only seam: points the PeeringDB client at an alternate base URL so
         * {@link #analyze()} can be exercised end-to-end against a stub server. Package-private
         * so it is never part of the public builder surface.
         *
         * @param baseUrl the PeeringDB API base URL (e.g. {@code http://localhost:PORT/api})
         * @return this builder
         */
        Builder peeringDbBaseUrl(String baseUrl) {
            this.peeringDbBaseUrl = baseUrl;
            return this;
        }

        /**
         * Adds a target ASN to analyze with a human-readable label.
         *
         * @param asn   the autonomous system number (e.g., 16509 for AWS)
         * @param label a short label for display (e.g., "AWS")
         * @return this builder
         */
        public Builder addAsn(long asn, String label) {
            targetAsns.put(asn, label);
            return this;
        }

        /**
         * Adds a target ASN to analyze using the PeeringDB network name as label.
         *
         * @param asn the autonomous system number
         * @return this builder
         */
        public Builder addAsn(long asn) {
            targetAsns.put(asn, null);
            return this;
        }

        /**
         * Adds multiple ASNs from a map of ASN to label.
         *
         * @param asns map of ASN → label
         * @return this builder
         */
        public Builder addAsns(Map<Long, String> asns) {
            targetAsns.putAll(asns);
            return this;
        }

        /**
         * Sets the customer's metro locations for resiliency analysis.
         *
         * <p>When customer metros are specified, the engine performs blast radius
         * analysis and failover path identification for each metro.</p>
         *
         * @param metros the customer's Equinix metro locations
         * @return this builder
         */
        public Builder customerMetros(MetroCode... metros) {
            for (MetroCode metro : metros) {
                customerMetros.add(MetroId.of(metro));
            }
            return this;
        }

        /**
         * Sets the customer's metro locations by {@link MetroId} — accepts metros not in the
         * {@link MetroCode} enum (e.g. one added to Fabric after this SDK was built).
         *
         * @param metros the customer's Equinix metro locations
         * @return this builder
         */
        public Builder customerMetros(MetroId... metros) {
            customerMetros.addAll(Arrays.asList(metros));
            return this;
        }

        /**
         * Sets the customer's metro locations by metro code string (e.g. {@code "DC"}, {@code "da"}).
         * Accepts metros not in the {@link MetroCode} enum.
         *
         * @param metros the customer's Equinix metro codes
         * @return this builder
         */
        public Builder customerMetros(String... metros) {
            for (String metro : metros) {
                customerMetros.add(MetroId.of(metro));
            }
            return this;
        }

        /**
         * Sets the customer's metro locations from a collection of {@link MetroCode}.
         *
         * @param metros the customer's Equinix metro locations
         * @return this builder
         */
        public Builder customerMetros(Collection<MetroCode> metros) {
            for (MetroCode metro : metros) {
                customerMetros.add(MetroId.of(metro));
            }
            return this;
        }

        /**
         * Sets the customer's own ASN for mutual peering opportunity discovery.
         *
         * <p>When provided, the engine queries PeeringDB for the customer's IX
         * presence and identifies metros where both the customer and target ASNs
         * are present at the same Equinix IX but not currently peering.</p>
         *
         * @param asn the customer's autonomous system number
         * @return this builder
         */
        public Builder customerAsn(long asn) {
            this.customerAsn = asn;
            return this;
        }

        /**
         * Enables or disables IX port capacity analysis. Enabled by default.
         *
         * @param include {@code true} to include port speed data from PeeringDB
         * @return this builder
         */
        public Builder includeCapacity(boolean include) {
            this.includeCapacity = include;
            return this;
        }

        /**
         * Enables or disables peering policy feasibility analysis. Enabled by default.
         *
         * @param include {@code true} to include policy data from PeeringDB
         * @return this builder
         */
        public Builder includePolicies(boolean include) {
            this.includePolicies = include;
            return this;
        }

        /**
         * Enables Equinix Fabric service profile cross-referencing.
         *
         * <p>When enabled, the engine queries the Fabric API for service profiles
         * matching the target ASNs, adding Fabric connection availability to the
         * presence matrix and unified connectivity views.</p>
         *
         * @param include {@code true} to cross-reference Fabric service profiles
         * @return this builder
         */
        public Builder includeFabricConnections(boolean include) {
            this.includeFabricConnections = include;
            return this;
        }

        /**
         * Enables resiliency analysis (blast radius, correlated failures, failover paths).
         *
         * <p>Requires {@link #customerMetros(MetroCode...)} to have been called with
         * at least one metro. Without customer metros, there is no context for
         * evaluating failure impact.</p>
         *
         * @param include {@code true} to perform resiliency analysis
         * @return this builder
         */
        public Builder includeResiliency(boolean include) {
            this.includeResiliency = include;
            return this;
        }

        /**
         * Enables all analysis features (capacity, policies, Fabric, resiliency).
         *
         * @return this builder
         */
        public Builder includeAll() {
            this.includeCapacity = true;
            this.includePolicies = true;
            this.includeFabricConnections = true;
            this.includeResiliency = true;
            return this;
        }

        /**
         * Executes the peering intelligence analysis.
         *
         * <p>This method makes live API calls to PeeringDB (and optionally the Equinix
         * Fabric API) to collect network presence data, then runs the analysis pipeline
         * to produce the result.</p>
         *
         * @return the complete analysis result
         * @throws IllegalStateException if no target ASNs have been specified
         * @throws RuntimeException if PeeringDB API calls fail
         */
        public PeeringIntelligenceResult analyze() {
            if (targetAsns.isEmpty()) {
                throw new IllegalStateException("At least one target ASN must be specified via addAsn().");
            }

            PeeringRequest request = PeeringRequest.builder()
                    .targetAsns(new LinkedHashMap<>(targetAsns))
                    .customerMetros(new LinkedHashSet<>(customerMetros))
                    .customerAsn(customerAsn)
                    .includeCapacity(includeCapacity)
                    .includePolicies(includePolicies)
                    .includeFabricConnections(includeFabricConnections)
                    .includeResiliency(includeResiliency)
                    .build();

            String resolvedApiKey = peeringDbApiKey != null && !peeringDbApiKey.isBlank()
                    ? peeringDbApiKey
                    : envLookup.apply(PEERINGDB_API_KEY_ENV);

            PeeringDbClient peeringDbClient = peeringDbBaseUrl != null
                    ? PeeringDbClient.withBaseUrl(resolvedApiKey, peeringDbBaseUrl)
                    : (resolvedApiKey != null ? new PeeringDbClient(resolvedApiKey) : new PeeringDbClient());

            PeeringIntelligenceEngine engine = new PeeringIntelligenceEngine(
                    fabric, peeringDbClient, request);

            return engine.execute();
        }
    }
}
