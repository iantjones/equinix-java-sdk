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

package api.equinix.javasdk;

import api.equinix.javasdk.design.optimizer.MetroOptimizer;
import api.equinix.javasdk.design.optimizer.model.OptimizationResult;
import api.equinix.javasdk.design.optimizer.wizard.DeploymentWizard;
import api.equinix.javasdk.design.peering.PeeringIntelligence;
import api.equinix.javasdk.design.value.savings.SavingsCalculator;
import api.equinix.javasdk.design.value.tco.TcoCalculator;

/**
 * A root-level entry point that gathers the SDK's value-add <em>design</em> capabilities —
 * Metro Optimizer, Deployment Wizard, Peering Intelligence, Savings Calculator, and TCO
 * comparison — in one discoverable place, alongside the API-domain clients ({@link Fabric},
 * {@link NetworkEdge}, …).
 *
 * <p>Unlike those domain clients, {@code Design} is <strong>not</strong> constructed from
 * credentials: it is a thin facade over an already-authenticated {@link FabricGateway}, which
 * it reuses. This is intentional — the design engines run on top of an existing client (its
 * metros, service profiles, prices, …) rather than opening a second connection pool / token.
 * Construct it over your {@code Fabric}, or obtain one from a shared {@link Equinix} session
 * via {@link Equinix#design()}:</p>
 *
 * <pre>{@code
 * Design design = Design.over(fabric);          // reuses fabric's client — no new transport
 * OptimizationResult result = design.optimizeMetros()
 *         .addWorkload("ML Training").type(WorkloadType.AI_ML_TRAINING).bandwidthMbps(10_000).done()
 *         .optimize();
 * SavingsEstimate savings = design.savingsCalculator()
 *         .egress(50, DataUnit.TERABYTE).fromCloud(CloudProviderType.AWS).inRegion("us-east-1")
 *         .calculate();
 * }</pre>
 *
 * <p>Each method returns the same fluent builder you would get from the corresponding
 * {@code Fabric} accessor (e.g. {@link Fabric#optimizeMetros()}) or the underlying
 * {@code *.builder(FabricGateway)} factory. Export utilities ({@code design.export.TerraformExporter},
 * {@code design.export.TopologyDiagram}) operate on a produced plan/result and are used directly.</p>
 */
public final class Design {

    private final FabricGateway fabric;

    private Design(FabricGateway fabric) {
        this.fabric = fabric;
    }

    /**
     * Creates a design facade over an existing Fabric client (or any {@link FabricGateway}),
     * reusing its authenticated transport.
     *
     * @param fabric the Fabric client the design engines read from
     * @return a design facade
     */
    public static Design over(FabricGateway fabric) {
        return new Design(fabric);
    }

    /**
     * Begins a metro-optimization request.
     *
     * @return a {@link MetroOptimizer.Builder} bound to this facade's Fabric client
     */
    public MetroOptimizer.Builder optimizeMetros() {
        return MetroOptimizer.builder(fabric);
    }

    /**
     * Begins a deployment-plan build from a prior optimization result.
     *
     * @param optimizationResult the optimization result to plan from
     * @return a {@link DeploymentWizard.Builder} bound to this facade's Fabric client
     */
    public DeploymentWizard.Builder deploymentWizard(OptimizationResult optimizationResult) {
        return DeploymentWizard.builder(fabric, optimizationResult);
    }

    /**
     * Begins a peering-intelligence request, resolving the PeeringDB credential the same way
     * {@link Fabric#peeringIntelligence()} does: the {@code EquinixConfig.peeringDbApiKey} option
     * configured on the underlying client (if this facade wraps a {@link Fabric}), else the
     * {@code PEERINGDB_API_KEY} environment variable, else anonymous access.
     *
     * @return a {@link PeeringIntelligence.Builder} bound to this facade's Fabric client
     */
    public PeeringIntelligence.Builder peeringIntelligence() {
        if (fabric instanceof Fabric fabricClient) {
            return fabricClient.peeringIntelligence();
        }
        return PeeringIntelligence.builder(fabric);
    }

    /**
     * Begins a peering-intelligence request authenticated to PeeringDB.
     *
     * @param peeringDbApiKey the PeeringDB API key (higher rate limits / contact fields)
     * @return a {@link PeeringIntelligence.Builder} bound to this facade's Fabric client
     */
    public PeeringIntelligence.Builder peeringIntelligence(String peeringDbApiKey) {
        return PeeringIntelligence.builder(fabric, peeringDbApiKey);
    }

    /**
     * Begins an egress savings calculation (public internet vs. private interconnect).
     *
     * @return a {@link SavingsCalculator.Builder} bound to this facade's Fabric client
     */
    public SavingsCalculator.Builder savingsCalculator() {
        return SavingsCalculator.builder(fabric);
    }

    /**
     * Begins a total-cost-of-ownership comparison (public cloud vs. on-prem vs. Equinix interconnect).
     *
     * @return a {@link TcoCalculator.Builder} bound to this facade's Fabric client
     */
    public TcoCalculator.Builder tcoComparison() {
        return TcoCalculator.builder(fabric);
    }
}
