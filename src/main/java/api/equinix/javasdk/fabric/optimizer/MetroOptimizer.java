package api.equinix.javasdk.fabric.optimizer;

import api.equinix.javasdk.Fabric;
import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.core.enums.Region;
import api.equinix.javasdk.fabric.model.implementation.cloud.CloudProviderType;
import api.equinix.javasdk.fabric.optimizer.enums.*;
import api.equinix.javasdk.fabric.optimizer.model.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Entry point for the Metro Optimization Engine. Provides a fluent builder API
 * for defining workforce locations, provider requirements, workloads, and constraints,
 * then computes optimal Equinix metro placements.
 *
 * <h3>Example Usage</h3>
 * <pre>{@code
 * OptimizationResult result = fabric.optimizeMetros()
 *     .addSite("NYC HQ").nearestMetro(MetroCode.NY).role(SiteRole.HEADQUARTERS).headcount(500).done()
 *     .requireProvider(CloudProviderType.AWS).sellerRegions("us-east-1").done()
 *     .addWorkload("ML Training").type(WorkloadType.AI_ML_TRAINING).bandwidthMbps(10_000).done()
 *     .constraints().monthlyBudget(50_000, 100_000).redundancy(RedundancyTier.MULTI_METRO).done()
 *     .strategy(OptimizationStrategy.BALANCED)
 *     .optimize();
 *
 * System.out.println(result.toMarkdown());
 * }</pre>
 */
public final class MetroOptimizer {

    private MetroOptimizer() {}

    /**
     * Creates a new optimization builder using the given {@link Fabric} client for API access.
     *
     * @param fabric the authenticated Fabric client used to retrieve metro and service profile data
     * @return a new {@link Builder} instance
     */
    public static Builder builder(Fabric fabric) {
        return new Builder(fabric);
    }

    /**
     * The primary builder for assembling an optimization request. Provides fluent methods
     * to define sites, provider requirements, workloads, constraints, and scoring strategy
     * before executing the optimization via {@link #optimize()}.
     *
     * @see SiteBuilder
     * @see ProviderBuilder
     * @see WorkloadBuilder
     * @see ConstraintsBuilder
     */
    public static final class Builder {

        private final Fabric fabric;
        private final List<UserSite> sites = new ArrayList<>();
        private final List<ProviderRequirement> providers = new ArrayList<>();
        private final List<WorkloadSpec> workloads = new ArrayList<>();
        private OptimizationConstraints constraints;
        private OptimizationStrategy strategy = OptimizationStrategy.BALANCED;
        private ScoringWeights scoringWeights;

        Builder(Fabric fabric) {
            this.fabric = fabric;
        }

        // ── Sites ──

        /**
         * Begins defining a user site (workforce location, customer market, or operational facility).
         * The returned {@link SiteBuilder} provides methods to specify the site's metro proximity,
         * geographic coordinates, role, and headcount.
         *
         * @param label a descriptive name for this site (e.g., "NYC Headquarters")
         * @return a new {@link SiteBuilder} for configuring the site
         */
        public SiteBuilder addSite(String label) {
            return new SiteBuilder(this, label);
        }

        Builder addSiteInternal(UserSite site) {
            sites.add(site);
            return this;
        }

        // ── Providers ──

        /**
         * Begins defining a required cloud provider. The optimizer will penalize or exclude
         * metros where this provider is not available.
         *
         * @param cloudProvider the well-known cloud provider type (e.g., {@code CloudProviderType.AWS})
         * @return a new {@link ProviderBuilder} for configuring additional provider details
         */
        public ProviderBuilder requireProvider(CloudProviderType cloudProvider) {
            return new ProviderBuilder(this, cloudProvider, true);
        }

        /**
         * Begins defining a required service profile provider by name. The optimizer will
         * penalize or exclude metros where this service profile is not available.
         *
         * @param serviceProfileName the Fabric service profile name to require
         * @return a new {@link ProviderBuilder} for configuring additional provider details
         */
        public ProviderBuilder requireProvider(String serviceProfileName) {
            return new ProviderBuilder(this, serviceProfileName, true);
        }

        /**
         * Begins defining a preferred (but not required) cloud provider. Metros with this
         * provider available will receive a scoring bonus, but metros without it are not excluded.
         *
         * @param cloudProvider the preferred cloud provider type
         * @return a new {@link ProviderBuilder} for configuring additional provider details
         */
        public ProviderBuilder preferProvider(CloudProviderType cloudProvider) {
            return new ProviderBuilder(this, cloudProvider, false);
        }

        /**
         * Begins defining a preferred (but not required) service profile provider by name.
         * Metros with this provider available will receive a scoring bonus.
         *
         * @param serviceProfileName the preferred Fabric service profile name
         * @return a new {@link ProviderBuilder} for configuring additional provider details
         */
        public ProviderBuilder preferProvider(String serviceProfileName) {
            return new ProviderBuilder(this, serviceProfileName, false);
        }

        Builder addProviderInternal(ProviderRequirement provider) {
            providers.add(provider);
            return this;
        }

        // ── Workloads ──

        /**
         * Begins defining a workload to be placed by the optimizer. The returned
         * {@link WorkloadBuilder} provides methods to specify the workload type,
         * bandwidth, latency requirements, and provider dependencies.
         *
         * @param label a descriptive name for this workload (e.g., "ML Training Pipeline")
         * @return a new {@link WorkloadBuilder} for configuring the workload
         */
        public WorkloadBuilder addWorkload(String label) {
            return new WorkloadBuilder(this, label);
        }

        Builder addWorkloadInternal(WorkloadSpec workload) {
            workloads.add(workload);
            return this;
        }

        // ── Constraints ──

        /**
         * Opens the constraints builder for defining hard and soft limits on the optimization
         * search space, including budget, region/metro restrictions, compliance zones,
         * redundancy requirements, and latency bounds.
         *
         * @return a new {@link ConstraintsBuilder} for configuring optimization constraints
         */
        public ConstraintsBuilder constraints() {
            return new ConstraintsBuilder(this);
        }

        Builder setConstraintsInternal(OptimizationConstraints constraints) {
            this.constraints = constraints;
            return this;
        }

        // ── Strategy & Weights ──

        /**
         * Sets the optimization strategy, which determines the default scoring weight
         * distribution across latency, provider coverage, cost, redundancy, and compliance.
         * Defaults to {@link OptimizationStrategy#BALANCED} if not set.
         *
         * @param strategy the optimization strategy to use
         * @return this builder for method chaining
         * @see OptimizationStrategy
         */
        public Builder strategy(OptimizationStrategy strategy) {
            this.strategy = strategy;
            return this;
        }

        /**
         * Sets custom scoring weights and latency thresholds that override the strategy defaults.
         * Only non-null fields in the provided {@link ScoringWeights} take precedence; null fields
         * fall back to the selected strategy's defaults.
         *
         * @param weights the custom scoring weights and thresholds
         * @return this builder for method chaining
         * @see ScoringWeights
         */
        public Builder scoringWeights(ScoringWeights weights) {
            this.scoringWeights = weights;
            return this;
        }

        // ── Execute ──

        /**
         * Executes the optimization pipeline. Assembles an {@link OptimizationRequest} from
         * the configured sites, providers, workloads, constraints, strategy, and scoring weights,
         * then delegates to the {@link MetroOptimizerEngine} for scoring and ranking.
         *
         * <p>This method makes live API calls to retrieve metro and service profile data from
         * Equinix Fabric. The {@link Fabric} client must be authenticated before calling this method.</p>
         *
         * @return the complete optimization result with ranked recommendations, topology, and reports
         * @see OptimizationResult
         */
        public OptimizationResult optimize() {
            OptimizationRequest request = OptimizationRequest.builder()
                    .sites(new ArrayList<>(sites))
                    .providers(new ArrayList<>(providers))
                    .workloads(new ArrayList<>(workloads))
                    .constraints(constraints != null ? constraints : OptimizationConstraints.builder().build())
                    .strategy(strategy)
                    .scoringWeights(scoringWeights != null ? scoringWeights : ScoringWeights.defaults())
                    .build();

            return MetroOptimizerEngine.execute(request, fabric);
        }
    }

    // ══════════════════════════════════════════════
    //  Sub-Builders
    // ══════════════════════════════════════════════

    /**
     * Fluent builder for defining a user site. A site represents a workforce location,
     * customer market, or operational facility whose proximity to candidate metros is
     * factored into the optimization scoring.
     *
     * <p>Location can be specified either by {@link #nearestMetro(MetroCode)} or by
     * {@link #coordinates(double, double)}. If both are provided, the nearest metro
     * takes precedence for latency lookups while coordinates are used as a fallback
     * for distance-based estimates.</p>
     *
     * @see UserSite
     * @see SiteRole
     */
    public static final class SiteBuilder {

        private final Builder parent;
        private final String label;
        private MetroCode nearestMetro;
        private Double latitude;
        private Double longitude;
        private SiteRole role = SiteRole.BRANCH_OFFICE;
        private int headcount;
        private double weight;

        SiteBuilder(Builder parent, String label) {
            this.parent = parent;
            this.label = label;
        }

        /**
         * Sets the nearest Equinix metro to this site. Used for direct latency lookups
         * in the Fabric metro interconnection data. This is the preferred way to specify
         * site location when the nearest metro is known.
         *
         * @param metro the Equinix metro code nearest to this site
         * @return this builder for method chaining
         * @see #coordinates(double, double)
         */
        public SiteBuilder nearestMetro(MetroCode metro) {
            this.nearestMetro = metro;
            return this;
        }

        /**
         * Sets the geographic coordinates of this site. Used for great-circle distance
         * calculations when no direct metro latency data is available, or when
         * {@link #nearestMetro(MetroCode)} is not set.
         *
         * @param latitude  the latitude in decimal degrees (e.g., 40.7128 for New York)
         * @param longitude the longitude in decimal degrees (e.g., -74.0060 for New York)
         * @return this builder for method chaining
         * @see #nearestMetro(MetroCode)
         */
        public SiteBuilder coordinates(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
            return this;
        }

        /**
         * Sets the business role of this site. The role determines an importance multiplier
         * that influences how heavily the optimizer weighs proximity to this site.
         * Defaults to {@link SiteRole#BRANCH_OFFICE}.
         *
         * @param role the business role of this site
         * @return this builder for method chaining
         * @see SiteRole#getImportanceMultiplier()
         */
        public SiteBuilder role(SiteRole role) {
            this.role = role;
            return this;
        }

        /**
         * Sets the number of employees or users at this site. When no explicit
         * {@link #weight(double)} is set, headcount is normalized across all sites
         * to derive relative importance in scoring.
         *
         * @param headcount the number of employees or users at this site
         * @return this builder for method chaining
         * @see #weight(double)
         */
        public SiteBuilder headcount(int headcount) {
            this.headcount = headcount;
            return this;
        }

        /**
         * Sets an explicit importance weight for this site, overriding the headcount-based
         * normalization. Values are relative to other sites; for example, a weight of 2.0
         * makes this site twice as important as a site with weight 1.0.
         *
         * @param weight the explicit importance weight (must be positive to take effect)
         * @return this builder for method chaining
         * @see #headcount(int)
         */
        public SiteBuilder weight(double weight) {
            this.weight = weight;
            return this;
        }

        /**
         * Finalizes this site definition and returns to the parent {@link Builder}.
         *
         * @return the parent builder for continued configuration
         */
        public Builder done() {
            return parent.addSiteInternal(UserSite.builder()
                    .label(label)
                    .nearestMetro(nearestMetro)
                    .latitude(latitude)
                    .longitude(longitude)
                    .role(role)
                    .headcount(headcount)
                    .weight(weight)
                    .build());
        }
    }

    /**
     * Fluent builder for defining a provider requirement. A provider can be identified
     * by a well-known {@link CloudProviderType}, a Fabric service profile name, or a
     * direct service profile UUID. Providers can be marked as required (hard constraint)
     * or preferred (soft scoring bonus).
     *
     * @see ProviderRequirement
     * @see CloudProviderType
     */
    public static final class ProviderBuilder {

        private final Builder parent;
        private final CloudProviderType cloudProvider;
        private final String serviceProfileName;
        private final boolean required;
        private String label;
        private String serviceProfileUuid;
        private List<String> preferredSellerRegions;

        ProviderBuilder(Builder parent, CloudProviderType cloudProvider, boolean required) {
            this.parent = parent;
            this.cloudProvider = cloudProvider;
            this.serviceProfileName = null;
            this.required = required;
        }

        ProviderBuilder(Builder parent, String serviceProfileName, boolean required) {
            this.parent = parent;
            this.cloudProvider = null;
            this.serviceProfileName = serviceProfileName;
            this.required = required;
        }

        /**
         * Sets a custom display label for this provider requirement. If not set, the
         * label is derived from the cloud provider name or service profile name.
         *
         * @param label the display label for this provider
         * @return this builder for method chaining
         */
        public ProviderBuilder label(String label) {
            this.label = label;
            return this;
        }

        /**
         * Sets a specific Fabric service profile UUID to look up for metro availability.
         * Use this when you have the exact profile UUID rather than relying on name-based matching.
         *
         * @param uuid the Fabric service profile UUID
         * @return this builder for method chaining
         */
        public ProviderBuilder serviceProfileUuid(String uuid) {
            this.serviceProfileUuid = uuid;
            return this;
        }

        /**
         * Sets the preferred seller regions for this provider. Metros that support these
         * specific seller regions receive a higher provider coverage score.
         *
         * @param regions the preferred seller region identifiers (e.g., "us-east-1", "eu-west-1")
         * @return this builder for method chaining
         */
        public ProviderBuilder sellerRegions(String... regions) {
            this.preferredSellerRegions = Arrays.asList(regions);
            return this;
        }

        /**
         * Finalizes this provider requirement and returns to the parent {@link Builder}.
         *
         * @return the parent builder for continued configuration
         */
        public Builder done() {
            return parent.addProviderInternal(ProviderRequirement.builder()
                    .cloudProvider(cloudProvider)
                    .serviceProfileName(serviceProfileName)
                    .serviceProfileUuid(serviceProfileUuid)
                    .label(label)
                    .preferredSellerRegions(preferredSellerRegions)
                    .required(required)
                    .build());
        }
    }

    /**
     * Fluent builder for defining a workload specification. Each workload carries a
     * {@link WorkloadType} with built-in infrastructure defaults that can be individually
     * overridden. Workloads can also declare provider dependencies to ensure they are
     * placed in metros where those providers are available.
     *
     * <p>Profile resolution order: explicit {@link #profile(WorkloadProfile)} overrides take
     * precedence, then individual method overrides (e.g., {@link #maxLatencyToleranceMs(double)}),
     * then the {@link WorkloadType}'s built-in defaults.</p>
     *
     * @see WorkloadSpec
     * @see WorkloadType
     * @see WorkloadProfile
     */
    public static final class WorkloadBuilder {

        private final Builder parent;
        private final String label;
        private WorkloadType type = WorkloadType.GENERAL_COMPUTE;
        private WorkloadProfile profileOverride;
        private LatencySensitivity latencySensitivity;
        private int bandwidthMbps;
        private final List<ProviderRequirement> dependsOnProviders = new ArrayList<>();

        WorkloadBuilder(Builder parent, String label) {
            this.parent = parent;
            this.label = label;
        }

        /**
         * Sets the workload type, which determines default infrastructure requirements
         * (latency sensitivity, power density, cooling, proximity weighting). Defaults
         * to {@link WorkloadType#GENERAL_COMPUTE}.
         *
         * @param type the workload archetype
         * @return this builder for method chaining
         * @see WorkloadType#getDefaultProfile()
         */
        public WorkloadBuilder type(WorkloadType type) {
            this.type = type;
            return this;
        }

        /**
         * Sets a complete custom workload profile, overriding all defaults from the
         * {@link WorkloadType}. Use this for full control over infrastructure requirements,
         * or when using {@link WorkloadType#CUSTOM}.
         *
         * @param profile the custom workload profile
         * @return this builder for method chaining
         * @see WorkloadProfile
         */
        public WorkloadBuilder profile(WorkloadProfile profile) {
            this.profileOverride = profile;
            return this;
        }

        /**
         * Overrides the latency sensitivity tier for this workload, independent of the
         * {@link WorkloadType}'s default. This affects how aggressively the optimizer
         * penalizes high-latency metros for this workload.
         *
         * @param sensitivity the latency sensitivity tier
         * @return this builder for method chaining
         * @see LatencySensitivity#getThresholdMs()
         */
        public WorkloadBuilder latencySensitivity(LatencySensitivity sensitivity) {
            this.latencySensitivity = sensitivity;
            return this;
        }

        /**
         * Sets the required network bandwidth for this workload in megabits per second.
         * Used in cost estimation and capacity validation.
         *
         * @param bandwidth the required bandwidth in Mbps
         * @return this builder for method chaining
         */
        public WorkloadBuilder bandwidthMbps(int bandwidth) {
            this.bandwidthMbps = bandwidth;
            return this;
        }

        /**
         * Sets an explicit maximum latency tolerance in milliseconds for this workload,
         * overriding the threshold derived from the latency sensitivity tier. Metros
         * exceeding this latency to any required site will be penalized.
         *
         * @param maxMs the maximum acceptable latency in milliseconds
         * @return this builder for method chaining
         */
        public WorkloadBuilder maxLatencyToleranceMs(double maxMs) {
            if (profileOverride == null) {
                profileOverride = WorkloadProfile.builder().maxLatencyToleranceMs(maxMs).build();
            } else {
                profileOverride = profileOverride.toBuilder().maxLatencyToleranceMs(maxMs).build();
            }
            return this;
        }

        /**
         * Marks this workload as requiring high power density infrastructure (e.g., for
         * GPU-intensive AI/ML training). This may affect metro scoring if facility
         * capability data is available.
         *
         * @return this builder for method chaining
         */
        public WorkloadBuilder requiresHighPowerDensity() {
            if (profileOverride == null) {
                profileOverride = WorkloadProfile.builder().requiresHighPowerDensity(true).build();
            } else {
                profileOverride = profileOverride.toBuilder().requiresHighPowerDensity(true).build();
            }
            return this;
        }

        /**
         * Marks this workload as requiring liquid cooling infrastructure. This may affect
         * metro scoring if facility capability data is available.
         *
         * @return this builder for method chaining
         */
        public WorkloadBuilder requiresLiquidCooling() {
            if (profileOverride == null) {
                profileOverride = WorkloadProfile.builder().requiresLiquidCooling(true).build();
            } else {
                profileOverride = profileOverride.toBuilder().requiresLiquidCooling(true).build();
            }
            return this;
        }

        /**
         * Declares that this workload depends on a specific cloud provider being available
         * at the assigned metro. The optimizer will ensure this workload is only placed in
         * metros where the specified provider has connectivity.
         *
         * @param provider the required cloud provider for this workload
         * @return this builder for method chaining
         */
        public WorkloadBuilder dependsOn(CloudProviderType provider) {
            dependsOnProviders.add(ProviderRequirement.builder()
                    .cloudProvider(provider)
                    .required(true)
                    .build());
            return this;
        }

        /**
         * Declares that this workload depends on a specific service profile being available
         * at the assigned metro. The optimizer will ensure this workload is only placed in
         * metros where the specified service profile is present.
         *
         * @param serviceProfileName the required Fabric service profile name for this workload
         * @return this builder for method chaining
         */
        public WorkloadBuilder dependsOn(String serviceProfileName) {
            dependsOnProviders.add(ProviderRequirement.builder()
                    .serviceProfileName(serviceProfileName)
                    .required(true)
                    .build());
            return this;
        }

        /**
         * Finalizes this workload definition and returns to the parent {@link Builder}.
         *
         * @return the parent builder for continued configuration
         */
        public Builder done() {
            return parent.addWorkloadInternal(WorkloadSpec.builder()
                    .label(label)
                    .type(type)
                    .profileOverride(profileOverride)
                    .latencySensitivity(latencySensitivity)
                    .bandwidthMbps(bandwidthMbps)
                    .dependsOnProviders(new ArrayList<>(dependsOnProviders))
                    .build());
        }
    }

    /**
     * Fluent builder for defining optimization constraints. Constraints define hard and soft
     * limits on the search space including budget, geographic restrictions, compliance
     * requirements, redundancy tiers, and latency bounds.
     *
     * <p>All constraints are optional. When omitted, the optimizer uses unconstrained defaults.</p>
     *
     * @see OptimizationConstraints
     */
    public static final class ConstraintsBuilder {

        private final Builder parent;
        private BudgetRange budget;
        private List<Region> requiredRegions;
        private List<Region> excludedRegions;
        private List<MetroCode> requiredMetros;
        private List<MetroCode> excludedMetros;
        private List<ComplianceZone> complianceZones;
        private RedundancyTier minimumRedundancy;
        private Double maxLatencyMs;
        private Integer maxMetroCount;
        private Integer minMetroCount;

        ConstraintsBuilder(Builder parent) {
            this.parent = parent;
        }

        /**
         * Sets the acceptable monthly budget range in USD. Metros whose estimated cost
         * falls outside this range will be penalized in the cost scoring dimension.
         *
         * @param min the minimum acceptable monthly spend in USD
         * @param max the maximum acceptable monthly spend in USD
         * @return this builder for method chaining
         */
        public ConstraintsBuilder monthlyBudget(double min, double max) {
            this.budget = new BudgetRange(min, max);
            return this;
        }

        /**
         * Sets the acceptable monthly budget range using a pre-built {@link BudgetRange}
         * that supports custom currencies.
         *
         * @param budget the budget range with min, max, and currency
         * @return this builder for method chaining
         * @see BudgetRange
         */
        public ConstraintsBuilder monthlyBudget(BudgetRange budget) {
            this.budget = budget;
            return this;
        }

        /**
         * Restricts the optimizer to only consider metros in the specified Equinix regions.
         * Metros outside these regions are excluded from candidacy.
         *
         * @param regions the required Equinix regions (e.g., {@code Region.AMER}, {@code Region.EMEA})
         * @return this builder for method chaining
         */
        public ConstraintsBuilder requiredRegions(Region... regions) {
            this.requiredRegions = Arrays.asList(regions);
            return this;
        }

        /**
         * Excludes metros in the specified Equinix regions from consideration.
         *
         * @param regions the Equinix regions to exclude
         * @return this builder for method chaining
         */
        public ConstraintsBuilder excludedRegions(Region... regions) {
            this.excludedRegions = Arrays.asList(regions);
            return this;
        }

        /**
         * Requires that the specified metros appear in the final recommendation set.
         * These metros are guaranteed to be included regardless of scoring.
         *
         * @param metros the metro codes that must be included
         * @return this builder for method chaining
         */
        public ConstraintsBuilder requireMetro(MetroCode... metros) {
            this.requiredMetros = Arrays.asList(metros);
            return this;
        }

        /**
         * Excludes the specified metros from consideration entirely.
         *
         * @param metros the metro codes to exclude
         * @return this builder for method chaining
         */
        public ConstraintsBuilder excludeMetro(MetroCode... metros) {
            this.excludedMetros = Arrays.asList(metros);
            return this;
        }

        /**
         * Sets compliance zone requirements for data sovereignty. Metros outside the
         * allowed regions for the specified compliance zones will be penalized or excluded.
         *
         * @param zones the compliance zones to enforce (e.g., {@code ComplianceZone.EU_GDPR})
         * @return this builder for method chaining
         * @see ComplianceZone#getAllowedRegions()
         */
        public ConstraintsBuilder compliance(ComplianceZone... zones) {
            this.complianceZones = Arrays.asList(zones);
            return this;
        }

        /**
         * Sets the minimum redundancy tier for the deployment. This influences the
         * minimum number of metros recommended and their geographic diversity.
         *
         * @param tier the minimum redundancy requirement
         * @return this builder for method chaining
         * @see RedundancyTier#getMinimumMetros()
         */
        public ConstraintsBuilder redundancy(RedundancyTier tier) {
            this.minimumRedundancy = tier;
            return this;
        }

        /**
         * Sets a hard upper bound on latency from any recommended metro to any user site.
         * Metros that exceed this threshold to any site are excluded.
         *
         * @param maxMs the maximum acceptable latency in milliseconds
         * @return this builder for method chaining
         */
        public ConstraintsBuilder maxLatencyMs(double maxMs) {
            this.maxLatencyMs = maxMs;
            return this;
        }

        /**
         * Limits the maximum number of metros in the recommendation set.
         *
         * @param max the maximum number of metros to recommend
         * @return this builder for method chaining
         */
        public ConstraintsBuilder maxMetros(int max) {
            this.maxMetroCount = max;
            return this;
        }

        /**
         * Sets the minimum number of metros that must be included in the recommendation set.
         *
         * @param min the minimum number of metros to recommend
         * @return this builder for method chaining
         */
        public ConstraintsBuilder minMetros(int min) {
            this.minMetroCount = min;
            return this;
        }

        /**
         * Finalizes the constraints definition and returns to the parent {@link Builder}.
         *
         * @return the parent builder for continued configuration
         */
        public Builder done() {
            return parent.setConstraintsInternal(OptimizationConstraints.builder()
                    .budget(budget)
                    .requiredRegions(requiredRegions)
                    .excludedRegions(excludedRegions)
                    .requiredMetros(requiredMetros)
                    .excludedMetros(excludedMetros)
                    .complianceZones(complianceZones)
                    .minimumRedundancy(minimumRedundancy)
                    .maxLatencyMs(maxLatencyMs)
                    .maxMetroCount(maxMetroCount)
                    .minMetroCount(minMetroCount)
                    .build());
        }
    }
}
