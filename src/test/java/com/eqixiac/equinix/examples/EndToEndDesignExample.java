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


package com.eqixiac.equinix.examples;

import com.eqixiac.equinix.Design;
import com.eqixiac.equinix.Equinix;
import com.eqixiac.equinix.FabricGateway;
import com.eqixiac.equinix.core.auth.BasicEquinixCredentials;
import com.eqixiac.equinix.core.enums.Region;
import com.eqixiac.equinix.core.model.MetroId;
import com.eqixiac.equinix.design.cost.GrowthPlan;
import com.eqixiac.equinix.design.cost.GrowthPlanner;
import com.eqixiac.equinix.design.export.TerraformExporter;
import com.eqixiac.equinix.design.export.TopologyDiagram;
import com.eqixiac.equinix.design.optimizer.MetroOptimizer;
import com.eqixiac.equinix.design.optimizer.context.FabricCatalogLoader;
import com.eqixiac.equinix.design.optimizer.enums.LatencySensitivity;
import com.eqixiac.equinix.design.optimizer.enums.RedundancyTier;
import com.eqixiac.equinix.design.optimizer.enums.SiteRole;
import com.eqixiac.equinix.design.optimizer.enums.WorkloadType;
import com.eqixiac.equinix.design.optimizer.model.OptimizationResult;
import com.eqixiac.equinix.design.optimizer.wizard.enums.BackboneTopology;
import com.eqixiac.equinix.design.optimizer.wizard.enums.BandwidthStrategy;
import com.eqixiac.equinix.design.optimizer.wizard.model.ConnectionInputRequirement;
import com.eqixiac.equinix.design.optimizer.wizard.model.DeploymentPlan;
import com.eqixiac.equinix.design.optimizer.wizard.model.ExecutionInputs;
import com.eqixiac.equinix.design.peering.model.PeeringIntelligenceResult;
import com.eqixiac.equinix.design.peering.model.UnifiedConnectivityView;
import com.eqixiac.equinix.design.placement.PlacementPlan;
import com.eqixiac.equinix.design.plan.ExecutionOptions;
import com.eqixiac.equinix.design.plan.FileCheckpointStore;
import com.eqixiac.equinix.design.record.DesignDecisionRecord;
import com.eqixiac.equinix.design.access.AccessInventory;
import com.eqixiac.equinix.design.access.AccessPlan;
import com.eqixiac.equinix.design.access.MetroAccess;
import com.eqixiac.equinix.design.access.PlannedAccessItem;
import com.eqixiac.equinix.design.optimizer.wizard.model.PlannedConnection;
import com.eqixiac.equinix.design.resilience.ResilienceAnalyzer;
import com.eqixiac.equinix.design.resilience.ResilienceReport;
import com.eqixiac.equinix.design.spec.CloudIntent;
import com.eqixiac.equinix.design.spec.CommercialTerms;
import com.eqixiac.equinix.design.spec.DesignConstraints;
import com.eqixiac.equinix.design.spec.DesignObjectives;
import com.eqixiac.equinix.design.spec.DesignSpec;
import com.eqixiac.equinix.design.spec.DesignSpecIO;
import com.eqixiac.equinix.design.spec.PeerIntent;
import com.eqixiac.equinix.design.spec.SiteIntent;
import com.eqixiac.equinix.design.spec.WorkloadIntent;
import com.eqixiac.equinix.design.spi.DesignOption;
import com.eqixiac.equinix.design.value.ratecard.RateCard;
import com.eqixiac.equinix.design.value.ratecard.ReferenceRateCard;
import com.eqixiac.equinix.design.value.ratecard.Term;
import com.eqixiac.equinix.design.value.savings.SavingsCalculator;
import com.eqixiac.equinix.design.value.savings.SavingsEstimate;
import com.eqixiac.equinix.design.value.tco.TcoCalculator;
import com.eqixiac.equinix.design.value.tco.TcoComparison;
import com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType;
import com.eqixiac.equinix.design.estate.EstateAnalyzer;
import com.eqixiac.equinix.design.estate.EstateReport;
import com.eqixiac.equinix.design.estate.EstateScope;
import com.eqixiac.equinix.design.optimizer.context.CatalogDesignContext;
import com.eqixiac.equinix.design.optimizer.wizard.model.DeploymentOutcome;
import com.eqixiac.equinix.design.optimizer.wizard.model.PlanPricing;
import com.eqixiac.equinix.design.optimizer.wizard.DeploymentWizard;
import com.eqixiac.equinix.design.peering.PeeringIntelligence;
import com.eqixiac.equinix.design.resilience.ResilienceFindings;
import com.eqixiac.equinix.design.solve.SensitivityReport;
import com.eqixiac.equinix.design.spec.SpecIssue;
import com.eqixiac.equinix.design.spi.MetroPortfolio;
import com.eqixiac.equinix.design.spi.ObjectiveValue;
import com.eqixiac.equinix.internetaccess.enums.ConnectionType;
import com.eqixiac.equinix.internetaccess.model.Ibx;

import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * One design carried from requirements to a deployment decision, using every design engine and the API
 * clients around them. Run {@link #main(String[])} against a live account (read-only by default), or
 * call {@link #run(Inputs)} with any {@link FabricGateway} and rate card.
 *
 * <p>The steps, in order:</p>
 * <ol>
 *   <li>Build and validate a {@link DesignSpec}: sites in three regions, three clouds, five workloads
 *       (two pinned, one custom), three peers, redundancy and survival limits, a budget, growth and a
 *       commitment term, and a weight on the Beta {@code carbon} objective.</li>
 *   <li>Choose metros with {@link MetroOptimizer}; print the frontier and the sensitivity.</li>
 *   <li>Simulate failures with {@link ResilienceAnalyzer}.</li>
 *   <li>Project growth and commitment terms with {@link GrowthPlanner}.</li>
 *   <li>Compare egress cost with {@link SavingsCalculator} and {@link TcoCalculator}.</li>
 *   <li>Decide how each destination is reached ({@code ConnectivityPlanner}, see {@link #connectivityFor}).</li>
 *   <li>Decide how each metro is entered ({@code ConnectivityPlanner}, see {@link #accessFor}): existing
 *       ports and devices first, then the orders to place.</li>
 *   <li>Write the {@link DesignDecisionRecord}.</li>
 *   <li>Build the deployment plan with {@link DeploymentWizard}; export Markdown, Terraform and a
 *       diagram.</li>
 *   <li>Apply the deploy gate. Nothing is created unless the gate passes and deployment was requested
 *       twice: on the command line and in the environment.</li>
 * </ol>
 *
 * <p><b>What the design layer decides and what this example decides.</b> The engines choose metros, place
 * workloads, price the design, simulate failures and plan Cloud Routers, cloud connections, backbone
 * links and routing protocols. {@code ConnectivityPlanner} decides how each destination is reached and
 * how each metro is entered, using the Fabric ports and Network Edge devices the project already has
 * before it plans an order. The deployment plan lists those ports, cross connects and devices and
 * connects each Cloud Router to them, and the failure simulation fails each of them. Nothing here
 * orders a port, cross connect, Network Edge device or Internet Access service, and Internet Exchange
 * (IX) membership is not planned.</p>
 *
 * <p>Cost figures are design-time estimates, not quotes. Availability figures in the resilience report
 * are planning assumptions, not SLAs.</p>
 */
public final class EndToEndDesignExample {

    /** The environment variable that, with {@code --deploy}, allows the example to create resources. */
    public static final String DEPLOY_ENV = "EQIXIAC_EXAMPLE_DEPLOY";

    /** The private ASN the example announces from the Cloud Routers. */
    static final long CUSTOMER_ASN = 64512L;

    private EndToEndDesignExample() {
    }

    // Inputs and results

    /**
     * What a run needs.
     *
     * @param fabric          the Fabric API (read by the engines)
     * @param rateCard        the price source for every engine
     * @param discovery       where Network Edge and Internet Access are available
     * @param peering         a Peering Intelligence result for the spec's peers, or {@code null} when it
     *                        was not run
     * @param outputDir       where the documents are written, or {@code null} to write none
     * @param out             where progress is printed
     * @param deployRequested whether deployment was requested on the command line and in the environment
     * @param projectId       the project to deploy into, or {@code null}
     * @param accountNumber   the billing account for Cloud Routers, or {@code null}
     * @param estateProjectId a project whose deployed Fabric resources are reviewed, or {@code null}
     * @param inventory       the Fabric ports and Network Edge devices the project already has, used before
     *                        a new order is planned; {@code null} when none were read
     * @param portsByMetro    ports to use in a metro, named by uuid (the first is the primary); may be empty
     */
    public record Inputs(FabricGateway fabric, RateCard rateCard, Discovery discovery,
                         PeeringIntelligenceResult peering, Path outputDir, PrintStream out,
                         boolean deployRequested, String projectId, Long accountNumber, String estateProjectId,
                         AccessInventory inventory, Map<String, List<String>> portsByMetro) {
    }

    /** Where products outside the design layer are available. */
    public interface Discovery {

        /** @return metro codes where Network Edge is offered; empty when unknown */
        Set<String> networkEdgeMetros();

        /** @return metro codes with at least one IBX offering Internet Access; empty when unknown */
        Set<String> internetAccessMetros();

        /** @return what the lookups could not determine, for the report */
        List<String> notes();

        /**
         * @param networkEdge    Network Edge metros
         * @param internetAccess Internet Access metros
         * @return a fixed discovery, for tests and offline runs
         */
        static Discovery of(Set<String> networkEdge, Set<String> internetAccess) {
            return new Discovery() {
                @Override
                public Set<String> networkEdgeMetros() {
                    return networkEdge;
                }

                @Override
                public Set<String> internetAccessMetros() {
                    return internetAccess;
                }

                @Override
                public List<String> notes() {
                    return List.of();
                }
            };
        }

        /**
         * Reads availability from the live APIs (GET only). A lookup that fails is recorded in
         * {@link #notes()} and leaves its set empty.
         *
         * @param eq a session
         * @return the discovery
         */
        static Discovery live(Equinix eq) {
            Set<String> ne = new TreeSet<>();
            Set<String> eia = new TreeSet<>();
            List<String> notes = new ArrayList<>();
            try {
                for (com.eqixiac.equinix.networkedge.model.Metro metro : eq.networkEdge().setup().listMetros()) {
                    if (metro.getMetroCode() != null) {
                        ne.add(metro.getMetroCode().name());
                    }
                }
            } catch (RuntimeException e) {
                notes.add("Network Edge metros could not be read: " + e.getClass().getSimpleName());
            }
            try {
                for (Ibx ibx : eq.internetAccess().ibxs().availability(ConnectionType.IA_VC)) {
                    if (ibx.getMetroCode() != null) {
                        eia.add(ibx.getMetroCode().toUpperCase(Locale.ROOT));
                    }
                }
            } catch (RuntimeException e) {
                notes.add("Internet Access availability could not be read: " + e.getClass().getSimpleName());
            }
            return new Discovery() {
                @Override
                public Set<String> networkEdgeMetros() {
                    return ne;
                }

                @Override
                public Set<String> internetAccessMetros() {
                    return eia;
                }

                @Override
                public List<String> notes() {
                    return notes;
                }
            };
        }
    }

    /** How a destination is reached. */
    public enum Reach {
        /** A private Fabric connection from a Cloud Router to the cloud on-ramp. */
        FABRIC_CLOUD_ROUTER,
        /** Public peering at an Internet Exchange (IX). */
        INTERNET_EXCHANGE,
        /** A private Fabric connection to the peer's service profile. */
        FABRIC_PRIVATE,
        /** Equinix Internet Access. */
        INTERNET_ACCESS,
        /** Not decided: the data the rule needs is missing. */
        UNDECIDED
    }

    /**
     * How one destination is reached.
     *
     * @param destination the cloud id, peer label or {@code internet}
     * @param reach       the choice
     * @param metros      the metros it is reached in
     * @param reason      the rule that applied
     */
    public record ConnectivityDecision(String destination, Reach reach, List<String> metros, String reason) {
    }

    /** How a metro is entered. */
    public enum Access {
        /** Redundant Fabric ports with cross connects from the customer's cage. */
        PHYSICAL_PORTS,
        /** A redundant pair of Network Edge virtual devices. */
        NETWORK_EDGE,
        /** Cloud Router only: sites reach the metro over an existing WAN or the internet. */
        CLOUD_ROUTER_ONLY
    }

    /**
     * How one metro is entered and what would be ordered for it.
     *
     * @param metro  the metro code
     * @param access the choice
     * @param reason the rule that applied
     * @param orders the orders that follow, described; none is sent
     */
    public record AccessDecision(String metro, Access access, String reason, List<String> orders) {
    }

    /**
     * The deploy gate.
     *
     * @param checks   each check and whether it passed
     * @param deployed whether resources were created
     * @param summary  one line saying why it deployed or did not
     */
    public record DeployGate(Map<String, Boolean> checks, boolean deployed, String summary) {

        /** @return whether every check passed */
        public boolean open() {
            return checks.values().stream().allMatch(Boolean::booleanValue);
        }
    }

    /**
     * Everything a run produced.
     *
     * @param spec         the design
     * @param optimization the optimizer result
     * @param resilience   the failure simulation
     * @param growth       the growth and term plan, or {@code null} when it could not be built
     * @param connectivity how each destination is reached
     * @param access       how each metro is entered
     * @param record       the decision record
     * @param plan         the deployment plan
     * @param gate         the deploy gate
     * @param skipped      steps that were skipped, with the reason
     * @param accessPlan   the ports, cross connects, devices and links behind {@code access}
     */
    public record Outcome(DesignSpec spec, OptimizationResult optimization, ResilienceReport resilience,
                          GrowthPlan growth, List<ConnectivityDecision> connectivity, List<AccessDecision> access,
                          DesignDecisionRecord record, DeploymentPlan plan, DeployGate gate, List<String> skipped,
                          AccessPlan accessPlan) {
    }

    // Step 1: the design

    /**
     * The example design: a company with offices in North America, Europe and Asia, its own cage in
     * Ashburn, three clouds, and peers it exchanges traffic with.
     *
     * @return the spec
     */
    public static DesignSpec spec() {
        return DesignSpec.builder()
                .name("global-interconnect")
                .description("Private access to three clouds from six sites, with peering and internet breakout.")
                .site(SiteIntent.builder().label("Ashburn data center").metro("DC").role(SiteRole.DATA_CENTER)
                        .weight(3.0).build())
                .site(SiteIntent.builder().label("Chicago headquarters").metro("CH").role(SiteRole.HEADQUARTERS)
                        .headcount(1200).build())
                .site(SiteIntent.builder().label("Dallas operations").metro("DA").role(SiteRole.EMPLOYEE_HUB)
                        .headcount(400).build())
                .site(SiteIntent.builder().label("London office").metro("LD").role(SiteRole.PRIMARY_MARKET)
                        .headcount(600).build())
                .site(SiteIntent.builder().label("Frankfurt plant").metro("FR").role(SiteRole.MANUFACTURING)
                        .headcount(300).build())
                .site(SiteIntent.builder().label("Singapore hub").metro("SG").role(SiteRole.BRANCH_OFFICE)
                        .headcount(250).build())
                .cloud(CloudIntent.builder().id("aws").provider(CloudProviderType.AWS).region("us-east-1")
                        .monthlyEgressGb(60_000d).build())
                .cloud(CloudIntent.builder().id("azure").provider(CloudProviderType.AZURE).region("westeurope")
                        .monthlyEgressGb(30_000d).build())
                .cloud(CloudIntent.builder().id("gcp").provider(CloudProviderType.GOOGLE_CLOUD)
                        .region("asia-southeast1").monthlyEgressGb(15_000d).build())
                .workload(WorkloadIntent.builder().label("order-api").type(WorkloadType.TRANSACTIONAL)
                        .bandwidthMbps(1000).latencySensitivity(LatencySensitivity.CRITICAL).maxLatencyMs(250.0)
                        .clouds(List.of("aws")).build())
                .workload(WorkloadIntent.builder().label("analytics").type(WorkloadType.BIG_DATA_ANALYTICS)
                        .bandwidthMbps(2000).latencySensitivity(LatencySensitivity.LOW)
                        .clouds(List.of("aws", "azure")).build())
                .workload(WorkloadIntent.builder().label("inference").type(WorkloadType.AI_ML_INFERENCE)
                        .bandwidthMbps(500).latencySensitivity(LatencySensitivity.HIGH).pinnedMetro("SG")
                        .clouds(List.of("gcp")).build())
                .workload(WorkloadIntent.builder().label("plant-erp").type(WorkloadType.CUSTOM)
                        .bandwidthMbps(500).pinnedMetro("FR").clouds(List.of("azure")).build())
                .workload(WorkloadIntent.builder().label("disaster-recovery").type(WorkloadType.DISASTER_RECOVERY)
                        .bandwidthMbps(1000).latencySensitivity(LatencySensitivity.LOW)
                        .clouds(List.of("aws", "azure")).build())
                .peer(PeerIntent.builder().asn(13335L).label("CDN and DNS provider").build())
                .peer(PeerIntent.builder().asn(20940L).label("Content delivery provider").build())
                .peer(PeerIntent.builder().asn(8075L).label("SaaS productivity provider").build())
                .constraints(DesignConstraints.builder()
                        .requiredRegions(List.of(Region.AMER, Region.EMEA, Region.APAC))
                        .requiredMetros(List.of("DC", "FR"))   // the cage is in DC; plant-erp is pinned to FR
                        .minimumRedundancy(RedundancyTier.MULTI_REGION)
                        .minMetros(3).maxMetros(4)
                        .minSurvivalPct(40.0)
                        .build())
                .objectives(new DesignObjectives(null, weights()))
                .commercial(CommercialTerms.builder().currency("USD").budgetMaxMonthly(new BigDecimal("60000"))
                        .term(Term.MONTH_12).annualGrowthRate(0.30).horizonMonths(36).build())
                .build();
    }

    private static Map<String, Double> weights() {
        Map<String, Double> weights = new LinkedHashMap<>();
        weights.put("latency", 0.30);
        weights.put("cost", 0.20);
        weights.put("provider_coverage", 0.20);
        weights.put("resilience", 0.15);
        weights.put("compliance", 0.05);
        weights.put("carbon", 0.10);   // Beta: country-level grid intensity, a siting indicator
        return weights;
    }

    // The flow

    /**
     * Runs the whole flow.
     *
     * @param in the inputs
     * @return what was decided and produced
     */
    public static Outcome run(Inputs in) {
        PrintStream out = in.out();
        List<String> skipped = new ArrayList<>(in.discovery().notes());

        // 1. Spec
        DesignSpec spec = spec();
        List<SpecIssue> issues = spec.validate();
        out.printf("1. Spec %s, fingerprint %s, %d validation issue(s)%n", spec.name(),
                spec.fingerprint().substring(0, 12), issues.size());
        issues.forEach(i -> out.println("   " + i.severity() + " " + i.path() + ": " + i.message()));
        spec.requireValid();
        write(in, "spec.yaml", DesignSpecIO.toYaml(spec));

        // 2. Metros
        OptimizationResult optimization = MetroOptimizer.forSpec(in.fabric(), spec).rateCard(in.rateCard()).run();
        DesignOption best = optimization.getRecommended();
        if (best == null) {
            throw new IllegalStateException("No feasible design: " + optimization.getSolverNotes());
        }
        MetroPortfolio portfolio = best.portfolio();
        out.printf("2. Recommended metros %s, weighted score %.1f of 100, %d option(s) on the frontier%n",
                portfolio.key(), best.weightedScore(), optimization.getFrontier().size());
        for (Map.Entry<String, ObjectiveValue> e : best.values().entrySet()) {
            out.printf("   %-18s %s%n", e.getKey(), e.getValue().known()
                    ? String.format(Locale.ROOT, "%.1f (score %.1f)", e.getValue().value(),
                            best.scores().getOrDefault(e.getKey(), Double.NaN))
                    : "unknown: " + e.getValue().explanation());
        }
        SensitivityReport sensitivity = optimization.getSensitivity();
        if (sensitivity != null) {
            for (SensitivityReport.WeightRange range : sensitivity.weightRanges()) {
                out.printf("   holds while the %s weight is between %.0f%% and %.0f%%%n", range.objective(),
                        100 * range.low(), 100 * range.high());
            }
        }
        write(in, "optimization.md", optimization.toMarkdown());

        // Connectivity and metro access are decided here, because the failure simulation in step 3 covers the
        // ports, cross connects and Network Edge devices. Steps 6 and 7 print the decisions.
        com.eqixiac.equinix.design.connectivity.ConnectivityPlan connectivityPlan =
                connectivityPlan(spec, optimization, in.peering(), in.discovery(), in.inventory(), in.portsByMetro());
        AccessPlan accessPlan = connectivityPlan.accessPlan();

        // 3. Failure simulation, including the loss of each port, cross connect, device and access connection
        ResilienceReport resilience = ResilienceAnalyzer.forSpec(in.fabric(), spec).optimization(optimization)
                .rateCard(in.rateCard()).access(accessPlan).run();
        out.println("3. N-1: " + resilience.n1().statement());
        out.println("   N-2: " + resilience.n2().statement());
        resilience.remediations().stream().limit(3)
                .forEach(r -> out.println("   remediation" + (r.verified() ? " (re-simulated)" : "") + ": " + r.action()));
        write(in, "resilience.md", resilience.toMarkdown());

        // 4. Growth and commitment terms
        GrowthPlan growth = null;
        try {
            CatalogDesignContext context = FabricCatalogLoader.load(in.fabric(), spec, null);
            growth = GrowthPlanner.forSpec(spec, portfolio, context, in.rateCard(), ReferenceRateCard.standard()).plan();
            out.printf("4. Growth plan over %d months at %.0f%% a year: %d item(s)%n",
                    spec.commercial().horizonMonthsOrDefault(), 100 * spec.commercial().annualGrowthRate(),
                    growth.items().size());
            write(in, "growth.md", growth.toMarkdown());
        } catch (RuntimeException e) {
            skipped.add("Growth plan: " + e.getMessage());
            out.println("4. Growth plan skipped: " + e.getMessage());
        }

        // 5. Egress value for the cloud with the most egress
        try {
            SavingsEstimate savings = SavingsCalculator.forSpec(in.fabric(), spec, "aws", portfolio)
                    .rateCard(in.rateCard()).calculate();
            TcoComparison tco = TcoCalculator.forSpec(in.fabric(), spec, "aws", portfolio)
                    .rateCard(in.rateCard()).compare();
            out.printf("5. AWS egress over Fabric: net %s %s a month (%s); TCO recommends %s%n",
                    savings.getNetMonthlySavings(), savings.getCurrency(),
                    savings.isComplete() ? "complete" : "incomplete", tco.getRecommended());
            write(in, "value.md", savings.toMarkdown() + "\n\n" + tco.toMarkdown());
        } catch (RuntimeException e) {
            skipped.add("Egress value: " + e.getMessage());
            out.println("5. Egress value skipped: " + e.getMessage());
        }

        // 6. How each destination is reached
        List<ConnectivityDecision> connectivity = connectivityFor(connectivityPlan);
        out.println("6. Connectivity");
        connectivity.forEach(c -> out.printf("   %-28s %-20s %s  (%s)%n", c.destination(), c.reach(), c.metros(),
                c.reason()));
        if (in.peering() == null) {
            skipped.add("Peering Intelligence was not run, so peers are undecided between IX and a private connection; "
                    + "set PEERINGDB_API_KEY to run it");
        }

        // 7. How each metro is entered: existing ports and devices first, then the orders to place
        List<AccessDecision> access = accessFor(connectivityPlan);
        out.println("7. Access per metro");
        for (MetroAccess metro : accessPlan.metros()) {
            out.printf("   %s %s: %s%n", metro.metro(), metro.mode(), metro.reason());
            for (PlannedAccessItem item : metro.items()) {
                out.printf("      %s %s %s%s%n", item.exists() ? "use  " : "order", PlannedAccessItem.label(item.kind()),
                        item.name(), item.existingUuid() == null ? "" : " (" + item.existingUuid() + ")");
            }
            metro.links().forEach(l -> out.printf("      connect %s to the Cloud Router, %d Mbps%n", l.item(),
                    l.bandwidthMbps()));
            metro.findings().forEach(f -> out.println("      finding: " + f));
        }
        write(in, "access.md", connectivityPlan.toMarkdown());

        // Existing estate, when a project is given
        if (in.estateProjectId() != null && in.fabric() instanceof com.eqixiac.equinix.Fabric live) {
            try {
                EstateReport estate = EstateAnalyzer.builder(live)
                        .scope(EstateScope.projects(in.estateProjectId())).window(Duration.ofDays(30)).analyze();
                write(in, "estate.md", estate.toMarkdown());
                out.println("   Estate of the project reviewed: see estate.md");
            } catch (RuntimeException e) {
                skipped.add("Estate analysis: " + e.getMessage());
            }
        }

        // 8. Decision record
        DesignDecisionRecord record = Design.over(in.fabric()).decisionRecord(spec).rateCard(in.rateCard())
                .optimization(optimization).resilience(resilience).record();
        out.println("8. Decision record sections: " + sectionStatuses(record));
        write(in, "decision-record.md", record.toMarkdown());

        // 9. Deployment plan
        DeploymentWizard.Builder wizard = Design.over(in.fabric()).deploymentWizard(optimization)
                .designSpec(spec).deploymentName("global-interconnect").rateCard(in.rateCard())
                .backboneTopology(BackboneTopology.FULL_MESH).bandwidthStrategy(BandwidthStrategy.PER_WORKLOAD)
                .customerAsn(CUSTOMER_ASN).withBFD(true, 300).notifications("noc@example.com")
                .access(accessPlan);
        if (in.projectId() != null) {
            wizard.projectId(in.projectId());
        }
        if (in.accountNumber() != null) {
            wizard.accountNumber(in.accountNumber());
        }
        DeploymentPlan plan = wizard.plan();
        PlanPricing pricing = plan.getPricing();
        out.printf("9. Plan %s: %d Cloud Router(s), %d cloud connection(s), %d backbone link(s), %d routing "
                        + "protocol(s); estimated %s %s a month%n", plan.planId(), plan.getCloudRouters().size(),
                plan.getProviderConnections().size(), plan.getBackboneLinks().size(),
                plan.getRoutingProtocols().size(), pricing == null ? "unpriced" : pricing.getMonthlyTotal(),
                pricing == null || pricing.getCurrency() == null ? "" : pricing.getCurrency());
        long ready = plan.accessConnectionsOrEmpty().stream().filter(PlannedConnection::hasAccessEndpoint).count();
        out.printf("   Metro access: %d item(s), %d access connection(s), of which %d can be created now and %d "
                        + "wait for an order%n", accessPlan.items().size(), plan.accessConnectionsOrEmpty().size(),
                ready, plan.accessConnectionsOrEmpty().size() - ready);
        write(in, "plan.md", plan.toMarkdown());
        write(in, "plan.json", plan.toJson());
        write(in, "main.tf", new TerraformExporter().export(plan));
        write(in, "topology.mmd", new TopologyDiagram().toMermaid(plan));

        // 10. Deploy gate
        DeployGate gate = gate(in, spec, resilience, plan, skipped);
        out.println("10. Deploy gate");
        gate.checks().forEach((check, ok) -> out.printf("   [%s] %s%n", ok ? "pass" : "FAIL", check));
        out.println("   " + gate.summary());
        skipped.forEach(s -> out.println("   note: " + s));
        return new Outcome(spec, optimization, resilience, growth, connectivity, access, record, plan, gate,
                List.copyOf(skipped), accessPlan);
    }

    // Steps 6 and 7

    /**
     * How each destination is reached, as {@code ConnectivityPlanner} decided it.
     *
     * @param plan the connectivity plan
     * @return one decision per cloud, per peer, and one for the internet
     */
    static List<ConnectivityDecision> connectivityFor(com.eqixiac.equinix.design.connectivity.ConnectivityPlan plan) {
        List<ConnectivityDecision> decisions = new ArrayList<>();
        for (com.eqixiac.equinix.design.connectivity.ConnectivityPlan.ReachDecision d : plan.reach()) {
            decisions.add(new ConnectivityDecision(d.destination(), Reach.valueOf(d.reach().name()), d.metros(),
                    d.reason()));
        }
        return decisions;
    }

    /**
     * Runs the SDK's {@code ConnectivityPlanner}. It decides how each destination is reached and how each
     * metro is entered. For access it uses a port or Network Edge device the project already has before
     * it plans an order, and it uses the ports named in {@code portsByMetro} as given.
     */
    private static com.eqixiac.equinix.design.connectivity.ConnectivityPlan connectivityPlan(
            DesignSpec spec, OptimizationResult optimization, PeeringIntelligenceResult peering, Discovery discovery,
            AccessInventory inventory, Map<String, List<String>> portsByMetro) {
        com.eqixiac.equinix.design.connectivity.ConnectivityPlanner planner =
                com.eqixiac.equinix.design.connectivity.ConnectivityPlanner.forSpec(spec).optimization(optimization)
                        .peering(peering)
                        .availability(new com.eqixiac.equinix.design.connectivity.ProductAvailability(
                                discovery.networkEdgeMetros(), discovery.internetAccessMetros(), discovery.notes()))
                        .inventory(inventory);
        if (portsByMetro != null) {
            portsByMetro.forEach((metro, uuids) -> planner.usePorts(metro, uuids.toArray(String[]::new)));
        }
        return planner.plan();
    }

    /**
     * How each chosen metro is entered, as {@code ConnectivityPlanner} decided it.
     *
     * @param plan the connectivity plan
     * @return one decision per metro
     */
    static List<AccessDecision> accessFor(com.eqixiac.equinix.design.connectivity.ConnectivityPlan plan) {
        List<AccessDecision> decisions = new ArrayList<>();
        for (com.eqixiac.equinix.design.connectivity.ConnectivityPlan.AccessDecision d : plan.access()) {
            decisions.add(new AccessDecision(d.metro(), Access.valueOf(d.access().name()), d.reason(), d.orders()));
        }
        return decisions;
    }

    // Step 10

    /**
     * The deploy gate: the plan is valid, its estimate is within the budget, the design survives every
     * single failure, every input the plan needs is present, a project and billing account were given,
     * and deployment was requested. Only when every check passes does it call
     * {@link DeploymentPlan#reconcile}.
     */
    private static DeployGate gate(Inputs in, DesignSpec spec, ResilienceReport resilience, DeploymentPlan plan,
                                   List<String> skipped) {
        Map<String, Boolean> checks = new LinkedHashMap<>();
        checks.put("plan is valid (" + plan.getValidationErrors().size() + " error(s))", plan.isValid());
        BigDecimal budget = spec.commercial().budgetMaxMonthly();
        PlanPricing pricing = plan.getPricing();
        BigDecimal total = pricing == null ? null : pricing.getMonthlyTotal();
        checks.put("estimate " + (total == null ? "unknown" : total.toPlainString()) + " is within the budget "
                + (budget == null ? "(none set)" : budget.toPlainString()),
                budget == null || (total != null && total.compareTo(budget) <= 0));
        checks.put("N-1 passes: " + resilience.n1().level(),
                resilience.n1().level() == ResilienceFindings.Level.PASS);
        long missing = plan.getRequiredInputs().stream()
                .filter(r -> r.isAuthenticationKeyRequired() && !r.isAuthenticationKeyProvided()).count();
        checks.put("provider inputs present (" + missing + " missing)", missing == 0);
        checks.put("project and billing account given", in.projectId() != null && in.accountNumber() != null);
        checks.put("deployment requested (--deploy and " + DEPLOY_ENV + "=yes)", in.deployRequested());

        boolean open = checks.values().stream().allMatch(Boolean::booleanValue);
        if (!open) {
            long failed = checks.values().stream().filter(ok -> !ok).count();
            return new DeployGate(checks, false, "Not deploying: " + failed + " of " + checks.size()
                    + " checks failed.");
        }
        Path checkpoints = in.outputDir() == null ? Path.of("checkpoints") : in.outputDir().resolve("checkpoints");
        DeploymentOutcome outcome = plan.reconcile(ExecutionInputs.none(), ExecutionOptions.defaults(),
                new FileCheckpointStore(checkpoints));
        write(in, "deployment.md", outcome.toMarkdown());
        if (!outcome.isFullySuccessful()) {
            skipped.add("Deployment did not complete: " + outcome.getErrors().size() + " error(s); see deployment.md");
        }
        return new DeployGate(checks, true, outcome.toSummary());
    }

    // Helpers

    private static String sectionStatuses(DesignDecisionRecord record) {
        StringBuilder sb = new StringBuilder();
        record.sections().forEach((name, section) -> sb.append(sb.length() == 0 ? "" : ", ").append(name).append(' ')
                .append(section.status().name().toLowerCase(Locale.ROOT)));
        return sb.toString();
    }

    private static void write(Inputs in, String name, String content) {
        if (in.outputDir() == null || content == null) {
            return;
        }
        try {
            Files.createDirectories(in.outputDir());
            Files.writeString(in.outputDir().resolve(name), content.endsWith("\n") ? content : content + "\n");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String env(String name) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? null : value.trim();
    }

    /**
     * Runs the example against a live account. It reads the catalogue and prices and creates nothing
     * unless {@code --deploy} is passed and {@value #DEPLOY_ENV} is {@code yes}, a project and billing
     * account are given, and the gate passes.
     *
     * <p>Environment: {@code EQUINIX_ACCESS_KEY} and {@code EQUINIX_SECRET_KEY} (required);
     * {@code EQIXIAC_EXAMPLE_PROJECT} and {@code EQIXIAC_EXAMPLE_ACCOUNT} (for the plan and deployment, and
     * to read the project's existing Fabric ports and the account's Network Edge devices);
     * {@code EQIXIAC_EXAMPLE_PORTS} (ports to use, as {@code DC=uuid1,uuid2;FR=uuid3});
     * {@code EQIXIAC_EXAMPLE_ESTATE_PROJECT} (to review a project's deployed resources).</p>
     *
     * <p>Peering Intelligence runs when {@code PEERINGDB_API_KEY} is set, because it calls PeeringDB and
     * anonymous access allows about 20 requests a minute. Without the key the peers stay undecided.</p>
     *
     * @param args {@code [output directory] [--deploy]}
     */
    /** Parses {@code DC=uuid1,uuid2;FR=uuid3} into the ports to use per metro. */
    static Map<String, List<String>> portsByMetro(String value) {
        Map<String, List<String>> ports = new LinkedHashMap<>();
        if (value == null) {
            return ports;
        }
        for (String entry : value.split(";")) {
            String[] parts = entry.split("=", 2);
            if (parts.length == 2 && !parts[1].isBlank()) {
                ports.put(parts[0].trim().toUpperCase(Locale.ROOT), List.of(parts[1].trim().split("\\s*,\\s*")));
            }
        }
        return ports;
    }

    public static void main(String[] args) throws IOException {
        List<String> flags = List.of(args);
        Path output = flags.stream().filter(a -> !a.startsWith("--")).findFirst().map(Path::of)
                .orElse(Path.of("target", "end-to-end-example"));
        boolean deploy = flags.contains("--deploy") && "yes".equalsIgnoreCase(System.getenv(DEPLOY_ENV));
        String account = env("EQIXIAC_EXAMPLE_ACCOUNT");
        try (Equinix eq = new Equinix(new BasicEquinixCredentials(
                System.getenv("EQUINIX_ACCESS_KEY"), System.getenv("EQUINIX_SECRET_KEY")))) {
            FabricGateway fabric = eq.fabric();
            PeeringIntelligenceResult peering = null;
            if (env(PeeringIntelligence.PEERINGDB_API_KEY_ENV) != null) {
                peering = PeeringIntelligence.forSpec(fabric, spec()).includeAll().analyze();
            }
            // Existing ports are read for the project and devices for the account. Without them nothing is
            // read, because an API client can see resources of other projects and accounts.
            AccessInventory inventory = AccessInventory.read(eq, env("EQIXIAC_EXAMPLE_PROJECT"), account);
            Outcome outcome = run(new Inputs(fabric, RateCard.standardChain(fabric), Discovery.live(eq), peering,
                    output, System.out, deploy, env("EQIXIAC_EXAMPLE_PROJECT"),
                    account == null ? null : Long.valueOf(account), env("EQIXIAC_EXAMPLE_ESTATE_PROJECT"),
                    inventory, portsByMetro(env("EQIXIAC_EXAMPLE_PORTS"))));
            System.out.println("Documents written to " + output.toAbsolutePath());
            System.out.println(outcome.gate().summary());
        }
    }
}
