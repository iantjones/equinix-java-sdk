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

package com.eqixiac.equinix.design.optimizer.wizard;

import com.eqixiac.equinix.core.enums.MetroCode;
import com.eqixiac.equinix.core.model.MetroId;
import com.eqixiac.equinix.design.optimizer.model.DeploymentTopology;
import com.eqixiac.equinix.design.optimizer.model.MetroRecommendation;
import com.eqixiac.equinix.design.optimizer.model.MetroScore;
import com.eqixiac.equinix.design.optimizer.model.OptimizationRequest;
import com.eqixiac.equinix.design.optimizer.model.OptimizationResult;
import com.eqixiac.equinix.design.optimizer.model.ProviderAvailability;
import com.eqixiac.equinix.design.optimizer.model.ProviderRequirement;
import com.eqixiac.equinix.design.optimizer.model.UserSite;
import com.eqixiac.equinix.design.optimizer.model.WorkloadPlacement;
import com.eqixiac.equinix.design.optimizer.model.WorkloadSpec;
import com.eqixiac.equinix.design.optimizer.wizard.model.DeploymentPlan;
import com.eqixiac.equinix.design.optimizer.wizard.model.PlannedBackboneLink;
import com.eqixiac.equinix.design.optimizer.wizard.model.PlannedCloudRouter;
import com.eqixiac.equinix.design.optimizer.wizard.model.PlannedConnection;
import com.eqixiac.equinix.design.optimizer.wizard.model.PlannedRoutingProtocol;
import com.eqixiac.equinix.design.value.ratecard.PriceQuote;
import com.eqixiac.equinix.design.value.ratecard.PriceSource;
import com.eqixiac.equinix.design.value.ratecard.RateCard;
import com.eqixiac.equinix.design.value.ratecard.Term;
import com.eqixiac.equinix.fabric.enums.ConnectionType;
import com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Currency;
import java.util.List;
import java.util.Optional;

/**
 * Hand-built optimizer results shared by the cloud-to-cloud wizard tests, and a deterministic text
 * rendering of a plan's Equinix-side content used by the unchanged-output regression.
 *
 * <p>Every fixture uses a fixed {@code computedAt} so rendered output is reproducible. Region
 * names are the provider's own notation and are chosen to match (or deliberately miss) entries of
 * the bundled multicloud environment catalog.</p>
 */
final class MulticloudWizardFixtures {

    static final MetroId DC = MetroId.of(MetroCode.DC);
    static final MetroId DA = MetroId.of(MetroCode.DA);
    static final MetroId SV = MetroId.of(MetroCode.SV);

    static final Instant COMPUTED_AT = Instant.parse("2026-09-21T00:00:00Z");

    private MulticloudWizardFixtures() {}

    /**
     * A rate card that prices every Fabric connection at USD 500 per month and every Cloud Router
     * at USD 300 per month, with no one-time charge. It does not override
     * {@code multicloudLink} or {@code egress}, so both return empty and the wizard's bundled
     * reference fallback supplies the native-link and per-GB figures.
     */
    static RateCard flatRateCard() {
        return new RateCard() {
            @Override
            public Optional<PriceQuote> connection(ConnectionType type, int bandwidthMbps, MetroCode metro, Term term) {
                return Optional.of(PriceQuote.of(BigDecimal.valueOf(500), BigDecimal.ZERO,
                        Currency.getInstance("USD"), PriceSource.ESTIMATE));
            }

            @Override
            public Optional<PriceQuote> cloudRouter(String packageCode, MetroCode metro, Term term) {
                return Optional.of(PriceQuote.of(BigDecimal.valueOf(300), BigDecimal.ZERO,
                        Currency.getInstance("USD"), PriceSource.ESTIMATE));
            }

            @Override
            public PriceSource source() {
                return PriceSource.ESTIMATE;
            }
        };
    }

    static ProviderRequirement cloud(CloudProviderType type, String label, String... preferredRegions) {
        return ProviderRequirement.builder()
                .cloudProvider(type)
                .label(label)
                .preferredSellerRegions(preferredRegions.length == 0 ? null : List.of(preferredRegions))
                .build();
    }

    static ProviderAvailability available(String label, String sellerRegion, String profileUuid) {
        return ProviderAvailability.builder()
                .providerLabel(label)
                .available(true)
                .sellerRegions(List.of(sellerRegion))
                .serviceProfileUuid(profileUuid)
                .build();
    }

    static UserSite site(String label, MetroId nearest) {
        return UserSite.builder().label(label).nearestMetro(nearest).headcount(100).build();
    }

    /**
     * One metro (DC) carrying two clouds, one workload depending on both, and no user site.
     *
     * @param a            the first cloud
     * @param aLabel       its provider label (shared by the requirement and the availability entry)
     * @param aRegion      its seller region at DC
     * @param z            the second cloud
     * @param zLabel       its provider label
     * @param zRegion      its seller region at DC
     * @param workloadMbps the workload's declared bandwidth in Mbps
     */
    static OptimizationResult twoCloudResult(CloudProviderType a, String aLabel, String aRegion,
                                             CloudProviderType z, String zLabel, String zRegion,
                                             int workloadMbps) {
        return twoCloudResult(a, aLabel, aRegion, z, zLabel, zRegion, workloadMbps,
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
    }

    /**
     * As {@link #twoCloudResult(CloudProviderType, String, String, CloudProviderType, String, String, int)}
     * with user sites, request-level provider requirements and additional workloads placed at DC.
     */
    static OptimizationResult twoCloudResult(CloudProviderType a, String aLabel, String aRegion,
                                             CloudProviderType z, String zLabel, String zRegion,
                                             int workloadMbps,
                                             List<UserSite> sites,
                                             List<ProviderRequirement> requestProviders,
                                             List<WorkloadSpec> extraWorkloadsAtDc) {
        WorkloadSpec replication = WorkloadSpec.builder()
                .label("Replication").bandwidthMbps(workloadMbps)
                .dependsOnProviders(List.of(cloud(a, aLabel), cloud(z, zLabel)))
                .build();

        List<WorkloadSpec> workloads = new ArrayList<>();
        workloads.add(replication);
        workloads.addAll(extraWorkloadsAtDc);

        List<WorkloadPlacement> placements = new ArrayList<>();
        for (WorkloadSpec workload : workloads) {
            placements.add(WorkloadPlacement.builder()
                    .workloadLabel(workload.getLabel())
                    .assignedMetro(MetroId.of(DC.code()))
                    .reasoning("clouds at DC")
                    .build());
        }

        MetroRecommendation dc = MetroRecommendation.builder()
                .rank(1).metroId(DC).metroName("Ashburn")
                .score(new MetroScore(90.0, Collections.emptyList()))
                .reasons(List.of("Primary"))
                .availableProviders(List.of(
                        available(aLabel, aRegion, "sp-" + a.shortCode()),
                        available(zLabel, zRegion, "sp-" + z.shortCode())))
                .build();

        return OptimizationResult.builder()
                .request(OptimizationRequest.builder()
                        .sites(sites)
                        .providers(requestProviders)
                        .workloads(workloads)
                        .build())
                .recommendations(List.of(dc))
                .topology(new DeploymentTopology(placements))
                .computedAt(COMPUTED_AT)
                .computeTimeMs(7)
                .build();
    }

    /** AWS {@code us-east-1} and Google Cloud {@code us-east4} at DC: a GA pair in the bundled catalog. */
    static OptimizationResult awsGcpAtDc(int workloadMbps) {
        return twoCloudResult(CloudProviderType.AWS, "AWS", "us-east-1",
                CloudProviderType.GOOGLE_CLOUD, "GCP", "us-east4", workloadMbps);
    }

    /**
     * The three-metro shape used by {@code DeploymentWizardPlanTest}: DC (AWS, two AWS-dependent
     * workloads), DA (Azure, one Azure-dependent workload), SV (one unavailable provider). No
     * workload depends on two clouds. Provider requirements carry a label only, as in that suite.
     */
    static OptimizationResult threeMetroSingleCloudResult() {
        MetroScore score = new MetroScore(90.0, Collections.emptyList());
        List<WorkloadSpec> workloads = List.of(
                WorkloadSpec.builder().label("ML Training").bandwidthMbps(8000)
                        .dependsOnProviders(List.of(ProviderRequirement.builder().label("AWS").build())).build(),
                WorkloadSpec.builder().label("DR Backup").bandwidthMbps(2000)
                        .dependsOnProviders(List.of(ProviderRequirement.builder().label("AWS").build())).build(),
                WorkloadSpec.builder().label("Analytics").bandwidthMbps(4000)
                        .dependsOnProviders(List.of(ProviderRequirement.builder().label("AZURE").build())).build());

        DeploymentTopology topology = new DeploymentTopology(List.of(
                WorkloadPlacement.builder().workloadLabel("ML Training")
                        .assignedMetro(MetroId.of(DC.code())).reasoning("AWS at DC").build(),
                WorkloadPlacement.builder().workloadLabel("DR Backup")
                        .assignedMetro(MetroId.of(DC.code())).reasoning("AWS at DC").build(),
                WorkloadPlacement.builder().workloadLabel("Analytics")
                        .assignedMetro(MetroId.of(DA.code())).reasoning("AZURE at DA").build()));

        return OptimizationResult.builder()
                .request(OptimizationRequest.builder().workloads(workloads).build())
                .recommendations(List.of(
                        MetroRecommendation.builder().rank(1).metroId(DC).metroName("Ashburn").score(score)
                                .reasons(List.of("Primary"))
                                .availableProviders(List.of(available("AWS", "us-east-1", "sp-aws"))).build(),
                        MetroRecommendation.builder().rank(2).metroId(DA).metroName("Dallas").score(score)
                                .reasons(List.of("Secondary"))
                                .availableProviders(List.of(available("AZURE", "eastus", "sp-azure"))).build(),
                        MetroRecommendation.builder().rank(3).metroId(SV).metroName("Silicon Valley").score(score)
                                .reasons(List.of("Tertiary"))
                                .availableProviders(List.of(ProviderAvailability.builder()
                                        .providerLabel("GCP").available(false)
                                        .sellerRegions(List.of("us-west1")).serviceProfileUuid("sp-gcp").build()))
                                .build()))
                .topology(topology)
                .computedAt(COMPUTED_AT)
                .computeTimeMs(42)
                .build();
    }

    /**
     * Renders the Equinix-side content of a plan as text: the summary, the Markdown report, every
     * planned resource field that reaches a Fabric request body, the pricing figures and the three
     * validation buckets. Native multicloud links are not rendered; the regression that uses this
     * asserts their absence separately.
     */
    static String renderEquinixSide(DeploymentPlan plan) {
        StringBuilder sb = new StringBuilder();
        sb.append("== summary\n").append(plan.toSummary()).append("\n");
        sb.append("== markdown\n").append(plan.toMarkdown()).append("\n");
        sb.append("== totalResourceCount\n").append(plan.totalResourceCount()).append("\n");
        sb.append("== routers\n");
        for (PlannedCloudRouter router : plan.getCloudRouters()) {
            sb.append(router.getName()).append('|').append(router.getMetroId()).append('|')
                    .append(router.getPackageCode()).append('|').append(router.getNotificationEmails()).append("\n");
        }
        sb.append("== providerConnections\n");
        for (PlannedConnection conn : plan.getProviderConnections()) {
            appendConnection(sb, conn);
        }
        sb.append("== backboneLinks\n");
        for (PlannedBackboneLink link : plan.getBackboneLinks()) {
            sb.append(link.getName()).append('|').append(link.getMetroA()).append('|').append(link.getMetroZ())
                    .append('|').append(link.getBandwidthMbps()).append('|').append(link.getTopology()).append("\n");
            appendConnection(sb, link.getConnection());
        }
        sb.append("== routingProtocols\n");
        for (PlannedRoutingProtocol rp : plan.getRoutingProtocols()) {
            sb.append(rp.getName()).append('|').append(rp.getType()).append('|').append(rp.getConnectionName())
                    .append('|').append(rp.getEquinixIfaceIpv4()).append('|').append(rp.getCustomerPeerIpv4())
                    .append('|').append(rp.getEquinixPeerIpv4()).append('|').append(rp.getCustomerAsn())
                    .append('|').append(rp.isBfdEnabled()).append('|').append(rp.getBfdInterval()).append("\n");
        }
        sb.append("== pricing\n");
        sb.append(plan.getPricing().getMonthlyTotal()).append('|').append(plan.getPricing().getSetupTotal())
                .append('|').append(plan.getPricing().getCurrency())
                .append('|').append(plan.getPricing().getMonthlyByCurrency())
                .append('|').append(plan.getPricing().getRouterMonthlyCost())
                .append('|').append(plan.getPricing().getProviderConnectionMonthlyCost())
                .append('|').append(plan.getPricing().getBackboneMonthlyCost())
                .append('|').append(plan.getPricing().getPerConnectionCost())
                .append('|').append(plan.getPricing().getSource())
                .append('|').append(plan.getPricing().getDisclaimer()).append("\n");
        sb.append("== valid\n").append(plan.isValid()).append("\n");
        sb.append("== validationErrors\n").append(plan.getValidationErrors()).append("\n");
        sb.append("== deferredValidations\n").append(plan.getDeferredValidations()).append("\n");
        sb.append("== skippedValidations\n").append(plan.getSkippedValidations()).append("\n");
        sb.append("== requiredInputs\n").append(plan.getRequiredInputs()).append("\n");
        return sb.toString();
    }

    private static void appendConnection(StringBuilder sb, PlannedConnection conn) {
        sb.append(conn.getName()).append('|').append(conn.getConnectionType()).append('|').append(conn.getPurpose())
                .append('|').append(conn.getBandwidthMbps())
                .append('|').append(conn.getBandwidthAllocation())
                .append('|').append(conn.getASideMetro()).append('|').append(conn.getASideRouterName())
                .append('|').append(conn.getZSideServiceProfileUuid()).append('|').append(conn.getZSideProviderLabel())
                .append('|').append(conn.getZSideSellerRegion()).append('|').append(conn.getZSideCloudType())
                .append('|').append(conn.getZSideMetro()).append('|').append(conn.getZSideRouterName())
                .append('|').append(conn.getNotificationEmails()).append("\n");
    }
}
