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

package com.eqixiac.equinix.design.optimizer;

import com.eqixiac.equinix.FabricGateway;
import com.eqixiac.equinix.core.enums.MetroCode;
import com.eqixiac.equinix.core.enums.Region;
import com.eqixiac.equinix.core.model.MetroId;
import com.eqixiac.equinix.design.constraint.Constraints;
import com.eqixiac.equinix.design.cost.CostComparability;
import com.eqixiac.equinix.design.cost.RateCardCostModel;
import com.eqixiac.equinix.design.objective.BuiltInObjectives;
import com.eqixiac.equinix.design.optimizer.context.CatalogDesignContext;
import com.eqixiac.equinix.design.optimizer.context.FabricCatalogLoader;
import com.eqixiac.equinix.design.optimizer.enums.ComplianceZone;
import com.eqixiac.equinix.design.optimizer.enums.LatencySensitivity;
import com.eqixiac.equinix.design.optimizer.enums.OptimizationStrategy;
import com.eqixiac.equinix.design.optimizer.enums.RedundancyTier;
import com.eqixiac.equinix.design.optimizer.enums.SiteRole;
import com.eqixiac.equinix.design.optimizer.enums.WorkloadType;
import com.eqixiac.equinix.design.optimizer.model.MulticloudEnvironmentCatalog;
import com.eqixiac.equinix.design.optimizer.model.OptimizationResult;
import com.eqixiac.equinix.design.solve.ExhaustiveSolver;
import com.eqixiac.equinix.design.solve.SensitivityAnalysis;
import com.eqixiac.equinix.design.solve.SensitivityReport;
import com.eqixiac.equinix.design.solve.SolveRun;
import com.eqixiac.equinix.design.solve.WeightProfiles;
import com.eqixiac.equinix.design.spec.CloudIntent;
import com.eqixiac.equinix.design.spec.CommercialTerms;
import com.eqixiac.equinix.design.spec.DesignConstraints;
import com.eqixiac.equinix.design.spec.DesignObjectives;
import com.eqixiac.equinix.design.spec.DesignSpec;
import com.eqixiac.equinix.design.spec.SiteIntent;
import com.eqixiac.equinix.design.spec.WorkloadIntent;
import com.eqixiac.equinix.design.spi.Constraint;
import com.eqixiac.equinix.design.spi.CostModel;
import com.eqixiac.equinix.design.spi.DesignPlugin;
import com.eqixiac.equinix.design.spi.Objective;
import com.eqixiac.equinix.design.spi.SolveProblem;
import com.eqixiac.equinix.design.value.ratecard.RateCard;
import com.eqixiac.equinix.design.value.ratecard.Term;
import com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The Metro Optimizer: which set of Equinix metros best serves a {@link DesignSpec}.
 *
 * <pre>{@code
 * OptimizationResult result = MetroOptimizer.forSpec(fabric, spec).run();
 * result.getRecommended();   // the chosen metros, every objective, priced cost lines, constraint verdicts
 * result.getFrontier();      // every Pareto-optimal trade-off
 * result.getSensitivity();   // weight ranges, nearest alternative, binding constraints
 * result.toMarkdown();
 * }</pre>
 *
 * <p>The optimizer is an orchestrator. It gathers the facts once ({@link FabricCatalogLoader}: metros,
 * measured latencies, cloud reachability), prices with a {@link RateCardCostModel}, measures the
 * built-in objectives ({@link BuiltInObjectives}) plus any {@link DesignPlugin} objectives, applies the
 * built-in constraints ({@link Constraints#forSpec}) plus plug-in constraints, searches every admissible
 * set of metros with {@link ExhaustiveSolver}, analyses the answer's sensitivity, and places the
 * workloads on the chosen set. Every decision lives in those plug-ins.</p>
 *
 * <p>Design size: {@code constraints.minMetros} (or the redundancy tier's minimum, or 1) to
 * {@code constraints.maxMetros} (default {@value #DEFAULT_MAX_METROS}).</p>
 */
public final class MetroOptimizer {

    /** Most metros a design may use when the spec sets no {@code maxMetros}. */
    public static final int DEFAULT_MAX_METROS = 3;

    private final FabricGateway fabric;
    private final CatalogDesignContext offlineContext;
    private final DesignSpec spec;
    private RateCard rateCard;
    private CostModel costModel;
    private ExhaustiveSolver solver = new ExhaustiveSolver();
    private final List<DesignPlugin> plugins = new ArrayList<>();
    private MulticloudEnvironmentCatalog environments;
    private com.eqixiac.equinix.design.spi.CurrencyConverter currencyConverter;
    private com.eqixiac.equinix.design.spi.CurrencyConverter defaultRates;

    private MetroOptimizer(FabricGateway fabric, CatalogDesignContext context, DesignSpec spec) {
        this.fabric = fabric;
        this.offlineContext = context;
        this.spec = spec;
    }

    /**
     * Optimises a spec against live Fabric data (read-only).
     *
     * @param fabric the Fabric API
     * @param spec   the design
     * @return a run to configure and {@link #run()}
     */
    public static MetroOptimizer forSpec(FabricGateway fabric, DesignSpec spec) {
        if (fabric == null || spec == null) {
            throw new IllegalArgumentException("forSpec needs a Fabric gateway and a spec");
        }
        return new MetroOptimizer(fabric, null, spec);
    }

    /**
     * Optimises against prepared facts instead of live Fabric (offline what-ifs, tests, a cached catalogue).
     *
     * @param context the facts; its spec is the design
     * @return a run to configure and {@link #run()}
     */
    public static MetroOptimizer forContext(CatalogDesignContext context) {
        return new MetroOptimizer(null, context, context.spec());
    }

    /**
     * A fluent convenience that assembles a {@link DesignSpec} and runs {@link #forSpec}.
     *
     * @param fabric the Fabric API
     * @return the builder
     */
    public static Builder builder(FabricGateway fabric) {
        return new Builder(fabric);
    }

    /**
     * @param card the rate card to price with; default {@code RateCard.standardChain(fabric)} (live Equinix,
     *             then reference figures)
     * @return this run
     */
    public MetroOptimizer rateCard(RateCard card) {
        this.rateCard = card;
        return this;
    }

    /**
     * @param model a cost model replacing the default {@link RateCardCostModel}
     * @return this run
     */
    public MetroOptimizer costModel(CostModel model) {
        this.costModel = model;
        return this;
    }

    /**
     * @param converter a currency converter (for example a live FX feed) ranked below rates pinned in the spec and
     *                  above the default sources; a {@link DesignPlugin#currencyConverter()} is used when this is unset
     * @return this run
     */
    public MetroOptimizer currencyConverter(com.eqixiac.equinix.design.spi.CurrencyConverter converter) {
        this.currencyConverter = converter;
        return this;
    }

    /**
     * @param defaults replaces the default rate sources (live ECB rates via Frankfurter, then the bundled table),
     *                 for example {@code CurrencyConverters.bundledOnly()} for an offline run
     * @return this run
     */
    public MetroOptimizer defaultRates(com.eqixiac.equinix.design.spi.CurrencyConverter defaults) {
        this.defaultRates = defaults;
        return this;
    }

    /**
     * @param exhaustiveSolver the solver (for example with another portfolio ceiling)
     * @return this run
     */
    public MetroOptimizer solver(ExhaustiveSolver exhaustiveSolver) {
        this.solver = exhaustiveSolver;
        return this;
    }

    /**
     * @param plugin extra objectives and constraints; {@link DesignPlugin#discover()} finds classpath ones
     * @return this run
     */
    public MetroOptimizer plugin(DesignPlugin plugin) {
        plugins.add(plugin);
        return this;
    }

    /**
     * @param catalog the native multicloud environment catalogue (Beta) for the informational finding
     * @return this run
     */
    public MetroOptimizer multicloudEnvironments(MulticloudEnvironmentCatalog catalog) {
        this.environments = catalog;
        return this;
    }

    /**
     * Runs the optimisation.
     *
     * @return the result; never {@code null}: when nothing is feasible the result says why
     * @throws IllegalArgumentException when the spec has errors ({@link DesignSpec#validate()})
     */
    public OptimizationResult run() {
        long started = System.currentTimeMillis();
        spec.requireValid();
        Term term = spec.commercial().term() == null ? Term.MONTH_1 : spec.commercial().term();
        CostModel model = costModel;
        if (model == null && (rateCard != null || fabric != null)) {
            com.eqixiac.equinix.design.spi.CurrencyConverter supplied = currencyConverter;
            for (DesignPlugin plugin : plugins) {
                if (supplied == null) {
                    supplied = plugin.currencyConverter();
                }
            }
            model = RateCardCostModel.forSpec(spec, rateCard != null ? rateCard : RateCard.standardChain(fabric))
                    .term(term).currencyConverter(supplied).defaultRates(defaultRates).build();
        }
        CatalogDesignContext context;
        if (offlineContext != null) {
            context = model != null ? offlineContext.withCostModel(model) : offlineContext;
        } else {
            context = FabricCatalogLoader.load(fabric, spec, model);
        }

        List<Objective> objectives = new ArrayList<>(BuiltInObjectives.all());
        List<Constraint> constraints = new ArrayList<>(Constraints.forSpec(spec));
        for (DesignPlugin plugin : plugins) {
            objectives.addAll(plugin.objectives());
            constraints.addAll(plugin.constraints());
        }
        // A spec that weights a plug-in objective (for example carbon or simulated_resilience) gets it from the
        // plug-ins on the classpath without the caller passing one; constraints stay opt-in.
        Map<String, Double> weighted = com.eqixiac.equinix.design.solve.WeightProfiles.resolve(spec.objectives());
        for (DesignPlugin discovered : DesignPlugin.discover()) {
            for (Objective objective : discovered.objectives()) {
                Double weight = weighted.get(objective.id());
                if (weight != null && weight > 0
                        && objectives.stream().noneMatch(o -> o.id().equals(objective.id()))) {
                    objectives.add(objective);
                }
            }
        }
        // constraints.minSurvivalPct is a first-class spec limit: honour it even when no resilience plug-in is passed.
        if (spec.constraints().minSurvivalPct() != null && constraints.stream()
                .noneMatch(k -> com.eqixiac.equinix.design.resilience.MinSurvivalConstraint.ID.equals(k.id()))) {
            constraints.add(new com.eqixiac.equinix.design.resilience.MinSurvivalConstraint());
        }
        DesignConstraints c = spec.constraints();
        int min = 1;
        if (c.minMetros() != null) {
            min = Math.max(min, c.minMetros());
        }
        if (c.minimumRedundancy() != null) {
            min = Math.max(min, c.minimumRedundancy().getMinimumMetros());
        }
        int max = c.maxMetros() != null ? c.maxMetros()
                : Math.max(min, Math.max(DEFAULT_MAX_METROS, c.requiredMetros().size()));
        Map<String, Double> weights = WeightProfiles.resolve(spec.objectives());

        SolveProblem problem = new SolveProblem(context, context.metros(), objectives, constraints, weights, min, max);
        SolveRun run = solver.solveDetailed(problem);
        // Safeguard for plug-in cost models that leave lines unconverted (the built-in model converts every line):
        // a design must not lose the cost weight only for want of an exchange rate (CostComparability.RULE).
        List<com.eqixiac.equinix.design.spi.MetroPortfolio> feasible = new ArrayList<>();
        for (SolveRun.Evaluated e : run.feasible()) {
            feasible.add(e.portfolio());
        }
        CostComparability comparability = CostComparability.assess(feasible, context,
                weights.getOrDefault(Objective.COST, 0.0));
        List<Objective> ranked = objectives;
        if (comparability.costWithdrawn()) {
            ranked = CostComparability.withdrawCost(objectives);
            run = solver.solveDetailed(new SolveProblem(context, context.metros(), ranked, constraints, weights, min,
                    max));
        }
        SensitivityReport sensitivity = SensitivityAnalysis.analyse(run);
        return ResultAssembler.assemble(context, run, sensitivity, min, max, rateCard, term, environments, started,
                comparability, ranked);
    }

    /**
     * Fluent convenience over {@link DesignSpec}: each call adds to the spec, {@link #toSpec()} returns it
     * and {@link #optimize()} runs {@link MetroOptimizer#forSpec}. Clouds are identified by their label
     * (default: the provider's name, or the profile name); a workload's {@code dependsOn} refers to the
     * same cloud, adding it as required when it was not declared.
     */
    public static final class Builder {
        private final FabricGateway fabric;
        private String name = "metro-optimization";
        private final List<SiteIntent> sites = new ArrayList<>();
        private final Map<String, CloudIntent> clouds = new LinkedHashMap<>();
        private final List<WorkloadIntent> workloads = new ArrayList<>();
        private DesignConstraints constraints = DesignConstraints.none();
        private CommercialTerms commercial = new CommercialTerms(null, null, null, null, null, null);
        private DesignObjectives objectives = DesignObjectives.balanced();
        private RateCard rateCard;
        private MulticloudEnvironmentCatalog environments;

        Builder(FabricGateway fabric) {
            this.fabric = fabric;
        }

        /**
         * @param name the spec name
         * @return this builder
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * @param label the site label
         * @return a site builder; {@link SiteBuilder#done()} returns here
         */
        public SiteBuilder addSite(String label) {
            return new SiteBuilder(this, label);
        }

        /**
         * @param provider a cloud every design must reach
         * @return a cloud builder
         */
        public ProviderBuilder requireProvider(CloudProviderType provider) {
            return new ProviderBuilder(this, CloudIntent.builder().provider(provider).required(true).build(), provider.getProviderName());
        }

        /**
         * @param serviceProfileName a service provider (profile name substring) every design must reach
         * @return a cloud builder
         */
        public ProviderBuilder requireProvider(String serviceProfileName) {
            return new ProviderBuilder(this, CloudIntent.builder().serviceProfileName(serviceProfileName).required(true).build(),
                    serviceProfileName);
        }

        /**
         * @param provider a cloud that counts towards coverage but is not required
         * @return a cloud builder
         */
        public ProviderBuilder preferProvider(CloudProviderType provider) {
            return new ProviderBuilder(this, CloudIntent.builder().provider(provider).required(false).build(),
                    provider.getProviderName());
        }

        /**
         * @param serviceProfileName a service provider that counts towards coverage but is not required
         * @return a cloud builder
         */
        public ProviderBuilder preferProvider(String serviceProfileName) {
            return new ProviderBuilder(this, CloudIntent.builder().serviceProfileName(serviceProfileName).required(false).build(),
                    serviceProfileName);
        }

        /**
         * @param label the workload label
         * @return a workload builder
         */
        public WorkloadBuilder addWorkload(String label) {
            return new WorkloadBuilder(this, label);
        }

        /** @return a constraints builder */
        public ConstraintsBuilder constraints() {
            return new ConstraintsBuilder(this);
        }

        /**
         * @param strategy a named weight profile
         * @return this builder
         */
        public Builder strategy(OptimizationStrategy strategy) {
            objectives = new DesignObjectives(strategy.profile(), objectives.weights());
            return this;
        }

        /**
         * @param weights explicit objective weights by id, applied on top of the profile
         * @return this builder
         */
        public Builder weights(Map<String, Double> weights) {
            objectives = new DesignObjectives(objectives.profile(), weights);
            return this;
        }

        /**
         * @param card the rate card to price with
         * @return this builder
         */
        public Builder rateCard(RateCard card) {
            this.rateCard = card;
            return this;
        }

        /**
         * @param term the commitment term to price at
         * @return this builder
         */
        public Builder term(Term term) {
            commercial = commercial.toBuilder().term(term).build();
            return this;
        }

        /**
         * @param catalog the native multicloud environment catalogue (Beta)
         * @return this builder
         */
        public Builder multicloudEnvironments(MulticloudEnvironmentCatalog catalog) {
            this.environments = catalog;
            return this;
        }

        /** @return the spec assembled so far */
        public DesignSpec toSpec() {
            return DesignSpec.builder().name(name).sites(new ArrayList<>(sites))
                    .clouds(new ArrayList<>(clouds.values())).workloads(new ArrayList<>(workloads))
                    .constraints(constraints).commercial(commercial).objectives(objectives).build();
        }

        /** @return the result of {@code MetroOptimizer.forSpec(fabric, toSpec())} */
        public OptimizationResult optimize() {
            return MetroOptimizer.forSpec(fabric, toSpec()).rateCard(rateCard).multicloudEnvironments(environments).run();
        }

        private String cloudFor(CloudProviderType provider) {
            for (CloudIntent cloud : clouds.values()) {
                if (cloud.provider() == provider && cloud.serviceProfileName() == null) {
                    return cloud.id();
                }
            }
            String id = provider.getProviderName();
            clouds.put(id, CloudIntent.builder().id(id).provider(provider).required(true).build());
            return id;
        }

        private String cloudFor(String serviceProfileName) {
            for (CloudIntent cloud : clouds.values()) {
                if (serviceProfileName.equals(cloud.serviceProfileName())) {
                    return cloud.id();
                }
            }
            clouds.put(serviceProfileName, CloudIntent.builder().id(serviceProfileName)
                    .serviceProfileName(serviceProfileName).required(true).build());
            return serviceProfileName;
        }
    }

    /** Adds one {@link SiteIntent}. */
    public static final class SiteBuilder {
        private final Builder parent;
        private SiteIntent site;

        SiteBuilder(Builder parent, String label) {
            this.parent = parent;
            this.site = SiteIntent.builder().label(label).build();
        }

        /**
         * @param metro the site's metro
         * @return this builder
         */
        public SiteBuilder nearestMetro(MetroCode metro) {
            site = site.toBuilder().metro(metro.name()).build();
            return this;
        }

        /**
         * @param metroCode the site's metro code
         * @return this builder
         */
        public SiteBuilder nearestMetro(String metroCode) {
            site = site.toBuilder().metro(MetroId.of(metroCode).code()).build();
            return this;
        }

        /**
         * @param metro the site's metro
         * @return this builder
         */
        public SiteBuilder nearestMetro(MetroId metro) {
            site = site.toBuilder().metro(metro.code()).build();
            return this;
        }

        /**
         * @param latitude  decimal degrees
         * @param longitude decimal degrees
         * @return this builder
         */
        public SiteBuilder coordinates(double latitude, double longitude) {
            site = site.toBuilder().latitude(latitude).longitude(longitude).build();
            return this;
        }

        /**
         * @param role what the site is
         * @return this builder
         */
        public SiteBuilder role(SiteRole role) {
            site = site.toBuilder().role(role).build();
            return this;
        }

        /**
         * @param headcount people at the site
         * @return this builder
         */
        public SiteBuilder headcount(int headcount) {
            site = site.toBuilder().headcount(headcount).build();
            return this;
        }

        /**
         * @param weight explicit relative importance
         * @return this builder
         */
        public SiteBuilder weight(double weight) {
            site = site.toBuilder().weight(weight).build();
            return this;
        }

        /** @return the parent builder */
        public Builder done() {
            parent.sites.add(site);
            return parent;
        }
    }

    /** Adds one {@link CloudIntent}. */
    public static final class ProviderBuilder {
        private final Builder parent;
        private CloudIntent cloud;
        private String label;

        ProviderBuilder(Builder parent, CloudIntent cloud, String defaultLabel) {
            this.parent = parent;
            this.cloud = cloud;
            this.label = defaultLabel;
        }

        /**
         * @param label the cloud id
         * @return this builder
         */
        public ProviderBuilder label(String label) {
            this.label = label;
            return this;
        }

        /**
         * @param uuid a specific service profile
         * @return this builder
         */
        public ProviderBuilder serviceProfileUuid(String uuid) {
            cloud = cloud.toBuilder().serviceProfileUuid(uuid).build();
            return this;
        }

        /**
         * @param region the provider region (the preferred seller region)
         * @return this builder
         */
        public ProviderBuilder region(String region) {
            cloud = cloud.toBuilder().region(region).build();
            return this;
        }

        /**
         * @param regions provider regions; the first becomes the cloud's {@code region} (preferred seller region)
         * @return this builder
         */
        public ProviderBuilder sellerRegions(String... regions) {
            if (regions.length > 0) {
                cloud = cloud.toBuilder().region(regions[0]).build();
            }
            return this;
        }

        /**
         * @param gb data leaving the cloud per month
         * @return this builder
         */
        public ProviderBuilder monthlyEgressGb(double gb) {
            cloud = cloud.toBuilder().monthlyEgressGb(gb).build();
            return this;
        }

        /** @return the parent builder */
        public Builder done() {
            parent.clouds.put(label, cloud.toBuilder().id(label).build());
            return parent;
        }
    }

    /** Adds one {@link WorkloadIntent}. */
    public static final class WorkloadBuilder {
        private final Builder parent;
        private WorkloadIntent workload;
        private final List<String> clouds = new ArrayList<>();
        private final String label;

        WorkloadBuilder(Builder parent, String label) {
            this.parent = parent;
            this.label = label;
            this.workload = WorkloadIntent.builder().label(label).build();
        }

        /**
         * @param type the workload archetype
         * @return this builder
         */
        public WorkloadBuilder type(WorkloadType type) {
            workload = workload.toBuilder().type(type).build();
            return this;
        }

        /**
         * @param sensitivity the latency sensitivity
         * @return this builder
         */
        public WorkloadBuilder latencySensitivity(LatencySensitivity sensitivity) {
            workload = workload.toBuilder().latencySensitivity(sensitivity).build();
            return this;
        }

        /**
         * @param mbps sustained bandwidth
         * @return this builder
         */
        public WorkloadBuilder bandwidthMbps(int mbps) {
            workload = workload.toBuilder().bandwidthMbps(mbps).build();
            return this;
        }

        /**
         * @param maxMs hard round-trip ceiling from every site to this workload
         * @return this builder
         */
        public WorkloadBuilder maxLatencyToleranceMs(double maxMs) {
            if (!(maxMs > 0) || !Double.isFinite(maxMs)) {
                throw new IllegalArgumentException("Workload '" + label + "': maxLatencyMs must be a positive, finite "
                        + "number of milliseconds, got " + maxMs);
            }
            workload = workload.toBuilder().maxLatencyMs(maxMs).build();
            return this;
        }

        /**
         * @param metro the metro the workload must run in
         * @return this builder
         */
        public WorkloadBuilder pinnedMetro(String metro) {
            workload = workload.toBuilder().pinnedMetro(MetroId.of(metro).code()).build();
            return this;
        }

        /**
         * @param provider a cloud the workload must reach
         * @return this builder
         */
        public WorkloadBuilder dependsOn(CloudProviderType provider) {
            clouds.add(parent.cloudFor(provider));
            return this;
        }

        /**
         * @param serviceProfileName a service provider the workload must reach
         * @return this builder
         */
        public WorkloadBuilder dependsOn(String serviceProfileName) {
            clouds.add(parent.cloudFor(serviceProfileName));
            return this;
        }

        /** @return the parent builder */
        public Builder done() {
            parent.workloads.add(workload.toBuilder().clouds(clouds).build());
            return parent;
        }
    }

    /** Sets {@link DesignConstraints} and the budget. */
    public static final class ConstraintsBuilder {
        private final Builder parent;
        private DesignConstraints c;

        ConstraintsBuilder(Builder parent) {
            this.parent = parent;
            this.c = parent.constraints;
        }

        /**
         * @param max      most the design may cost per month
         * @param currency ISO 4217 code the budget is stated in
         * @return this builder
         */
        public ConstraintsBuilder monthlyBudget(BigDecimal max, String currency) {
            parent.commercial = parent.commercial.toBuilder().budgetMaxMonthly(max).currency(currency).build();
            return this;
        }

        /**
         * @param max most the design may cost per month, in USD
         * @return this builder
         */
        public ConstraintsBuilder monthlyBudget(double max) {
            return monthlyBudget(BigDecimal.valueOf(max), "USD");
        }

        /**
         * @param regions regions every design must have a metro in
         * @return this builder
         */
        public ConstraintsBuilder requiredRegions(Region... regions) {
            c = c.toBuilder().requiredRegions(Arrays.asList(regions)).build();
            return this;
        }

        /**
         * @param regions regions no design may use
         * @return this builder
         */
        public ConstraintsBuilder excludedRegions(Region... regions) {
            c = c.toBuilder().excludedRegions(Arrays.asList(regions)).build();
            return this;
        }

        /**
         * @param metroCodes metros every design must include
         * @return this builder
         */
        public ConstraintsBuilder requireMetro(String... metroCodes) {
            c = c.toBuilder().requiredMetros(codes(metroCodes)).build();
            return this;
        }

        /**
         * @param metros metros every design must include
         * @return this builder
         */
        public ConstraintsBuilder requireMetro(MetroCode... metros) {
            return requireMetro(Arrays.stream(metros).map(MetroCode::name).toArray(String[]::new));
        }

        /**
         * @param metroCodes metros no design may use
         * @return this builder
         */
        public ConstraintsBuilder excludeMetro(String... metroCodes) {
            c = c.toBuilder().excludedMetros(codes(metroCodes)).build();
            return this;
        }

        /**
         * @param metros metros no design may use
         * @return this builder
         */
        public ConstraintsBuilder excludeMetro(MetroCode... metros) {
            return excludeMetro(Arrays.stream(metros).map(MetroCode::name).toArray(String[]::new));
        }

        /**
         * @param zones compliance zones every metro must be allowed by
         * @return this builder
         */
        public ConstraintsBuilder compliance(ComplianceZone... zones) {
            c = c.toBuilder().complianceZones(Arrays.asList(zones)).build();
            return this;
        }

        /**
         * @param zones Fabric geo scopes / zones every metro must belong to (data sovereignty)
         * @return this builder
         */
        public ConstraintsBuilder geoZones(String... zones) {
            c = c.toBuilder().geoZones(Arrays.asList(zones)).build();
            return this;
        }

        /**
         * @param tier the least redundancy acceptable
         * @return this builder
         */
        public ConstraintsBuilder redundancy(RedundancyTier tier) {
            c = c.toBuilder().minimumRedundancy(tier).build();
            return this;
        }

        /**
         * @param maxMs round-trip ceiling from every site to its nearest metro
         * @return this builder
         */
        public ConstraintsBuilder maxLatencyMs(double maxMs) {
            c = c.toBuilder().maxLatencyMs(maxMs).build();
            return this;
        }

        /**
         * @param min fewest metros
         * @return this builder
         */
        public ConstraintsBuilder minMetros(int min) {
            c = c.toBuilder().minMetros(min).build();
            return this;
        }

        /**
         * @param max most metros
         * @return this builder
         */
        public ConstraintsBuilder maxMetros(int max) {
            c = c.toBuilder().maxMetros(max).build();
            return this;
        }

        /** @return the parent builder */
        public Builder done() {
            parent.constraints = c;
            return parent;
        }

        private static List<String> codes(String... codes) {
            List<String> list = new ArrayList<>();
            for (String code : codes) {
                list.add(MetroId.of(code).code());
            }
            return list;
        }
    }
}
