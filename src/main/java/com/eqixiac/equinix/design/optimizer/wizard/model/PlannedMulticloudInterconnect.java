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

package com.eqixiac.equinix.design.optimizer.wizard.model;

import com.eqixiac.equinix.core.model.MetroId;
import com.eqixiac.equinix.core.model.multicloud.BandwidthTier;
import com.eqixiac.equinix.design.optimizer.enums.MulticloudEnvironmentStatus;
import com.eqixiac.equinix.design.optimizer.model.MulticloudEnvironment;
import com.eqixiac.equinix.design.optimizer.wizard.enums.CloudToCloudStrategy;
import com.eqixiac.equinix.design.optimizer.wizard.enums.MulticloudLinkRole;
import com.eqixiac.equinix.fabric.enums.ConnectionType;
import com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType;
import lombok.Builder;
import lombok.Value;

import java.util.Collections;
import java.util.List;
import java.util.OptionalInt;

/**
 * A native provider-to-provider multicloud link carried on a {@link DeploymentPlan}: a direct
 * connection between two clouds (for example AWS Interconnect - multicloud paired with a Google
 * Cloud Partner Cross-Cloud Interconnect transport) that uses no Equinix resource.
 *
 * <p><b>Beta</b>: the offerings this models reached general availability in 2026; the region
 * pairs come from a dated catalog ({@code MulticloudEnvironmentCatalog}).</p>
 *
 * <h2>What the SDK does with it</h2>
 * <ul>
 *   <li>It is planned and priced. It is never provisioned: it has no Fabric request body, and this
 *       SDK calls no cloud-provider API. {@code DeploymentPlan.execute()} records it as an
 *       informational entry on the outcome and sends nothing for it.</li>
 *   <li>It has no Cloud Router A-side, no routing protocols, no /30 subnet, no redundancy group and
 *       no Fabric name constraint. The providers build the link's redundancy, encryption and BGP
 *       between themselves.</li>
 *   <li>Plan validation lists it under skipped validations with the reason, except for a
 *       {@link MulticloudLinkRole#UNAVAILABLE} entry, which is an error.</li>
 *   <li>It is excluded from {@code DeploymentPlan.totalResourceCount()} and from the Equinix
 *       totals of {@link PlanPricing}.</li>
 * </ul>
 *
 * <h2>Bandwidth</h2>
 * <p>{@code requestedMbps} is the sum of the effective bandwidths of the workloads in
 * {@code getWorkloadLabels()}, in Mbps. {@code coveringTierMbps} is the smallest size the matched
 * environment lists that is not below the request ({@link BandwidthTier#coveringTier(int)});
 * {@code null} when the environment lists no such size or no environment matched. The native link
 * is priced at the covering tier; the rate card matches that size exactly and does not
 * interpolate.</p>
 */
@Value
@Builder(toBuilder = true)
public class PlannedMulticloudInterconnect {

    /**
     * A plan-unique label, for example {@code aws-gcp-DC}. Not a Fabric resource name: it is never
     * sent to an API and is not subject to Fabric's 24-character limit.
     */
    String name;

    /** One cloud of the pair. */
    CloudProviderType providerA;

    /** {@code providerA}'s region in its own notation, or {@code null} when the plan has none. */
    String regionA;

    /** The other cloud of the pair. */
    CloudProviderType providerZ;

    /** {@code providerZ}'s region in its own notation, or {@code null} when the plan has none. */
    String regionZ;

    /** The catalog entry matched for the region pair, or {@code null} when none matched. */
    MulticloudEnvironment environment;

    /** How the link relates to the Equinix connections for the same flow. */
    MulticloudLinkRole role;

    /** The wizard strategy that produced this entry. */
    CloudToCloudStrategy strategy;

    /** The metro where the workloads are placed and the Equinix path for the flow is planned; may be {@code null}. */
    MetroId metro;

    /** The bandwidth the flow needs, in Mbps. */
    int requestedMbps;

    /** The environment size the link would be ordered at, in Mbps; {@code null} when none covers the request. */
    Integer coveringTierMbps;

    /** The labels of the workloads whose dependency on both clouds implies this flow. */
    List<String> workloadLabels;

    /**
     * The Equinix side of the same flow: one entry per cloud for which the wizard planned a Cloud
     * Router to cloud connection at {@code getMetro()}, whether that connection is still on the
     * plan or was omitted. The Equinix-path cost in {@code getPricing()} is computed from these
     * entries, so a plan can be repriced after a connection was omitted.
     */
    List<EquinixLeg> equinixLegs;

    /**
     * The names of the plan's Cloud Router to cloud connections that carry the same flow and are
     * still on the plan. Empty when both were omitted or none was planned.
     */
    List<String> equinixConnectionNames;

    /**
     * The names the omitted Cloud Router to cloud connections would have had. Non-empty only for
     * {@link MulticloudLinkRole#REPLACEMENT}.
     */
    List<String> replacedConnectionNames;

    /** Both paths' cost for the flow and the break-even rate; {@code null} for an {@code UNAVAILABLE} entry. */
    MulticloudLinkPricing pricing;

    /**
     * A plain-language statement of which path costs less at which sustained rate, or why that
     * cannot be stated. Derived from {@code getPricing()}; refreshed by
     * {@code DeploymentWizard.Builder.reprice(plan)}.
     */
    String recommendation;

    /** Why the link has its role: the environment match, any round-up, and each reason a connection was kept. */
    List<String> reasoning;

    /**
     * The steps the customer performs with the two cloud providers to create the link, from the
     * providers' documentation. Empty when no documented procedure was verified for the pair.
     */
    List<String> createThenAcceptRecipe;

    /**
     * One Cloud Router to cloud connection of the Equinix path for the flow.
     *
     * <p><b>Beta.</b></p>
     */
    @Value
    @Builder
    public static class EquinixLeg {

        /** The cloud the connection reaches. */
        CloudProviderType cloud;

        /** The connection's plan name, for example {@code FCR-DC-to-aws}. */
        String connectionName;

        /** The Fabric connection type the connection is priced as. */
        ConnectionType connectionType;

        /**
         * The bandwidth, in Mbps, the connection is priced at for this flow: the connection's own
         * stamped bandwidth when it serves this flow's workloads only, otherwise the flow's
         * requested bandwidth.
         */
        int pricedMbps;

        /** Whether the connection is also sized for workloads outside this flow. */
        boolean sharedWithOtherWorkloads;

        /** Whether the connection is on the plan ({@code false} when the replacement rule omitted it). */
        boolean onPlan;
    }

    /**
     * @return {@code getEquinixLegs()}, or an empty list when that is {@code null}
     */
    public List<EquinixLeg> equinixLegsOrEmpty() {
        return equinixLegs == null ? Collections.emptyList() : equinixLegs;
    }

    /**
     * @return the matched environment's status, or {@code null} when no environment matched
     */
    public MulticloudEnvironmentStatus environmentStatus() {
        return environment == null ? null : environment.getStatus();
    }

    /**
     * @return the covering size in Mbps, or empty when none covers the request
     */
    public OptionalInt coveringTier() {
        return coveringTierMbps == null ? OptionalInt.empty() : OptionalInt.of(coveringTierMbps);
    }

    /**
     * @return {@code true} when the covering size is larger than the requested bandwidth
     */
    public boolean isRoundedUp() {
        return coveringTierMbps != null && coveringTierMbps > requestedMbps;
    }

    /**
     * @return {@code true} when the plan omits at least one Equinix connection because of this link
     */
    public boolean replacesEquinixConnections() {
        return role == MulticloudLinkRole.REPLACEMENT;
    }

    /**
     * A one-line description, for example
     * {@code AWS us-east-1 <-> GOOGLE_CLOUD us-east4, 10000 Mbps, GA, ALTERNATIVE}.
     *
     * @return the description
     */
    public String describe() {
        return providerA + (regionA == null ? "" : " " + regionA) + " <-> "
                + providerZ + (regionZ == null ? "" : " " + regionZ)
                + ", " + (coveringTierMbps != null ? coveringTierMbps : requestedMbps) + " Mbps, "
                + (environment == null ? "no catalog environment" : environment.getStatus())
                + ", " + role;
    }
}
