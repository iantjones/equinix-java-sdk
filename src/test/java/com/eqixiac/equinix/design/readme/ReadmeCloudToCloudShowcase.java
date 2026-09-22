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

package com.eqixiac.equinix.design.readme;

import com.eqixiac.equinix.Fabric;
import com.eqixiac.equinix.core.enums.MetroCode;
import com.eqixiac.equinix.core.model.multicloud.ActivationKey;
import com.eqixiac.equinix.core.model.multicloud.BandwidthTier;
import com.eqixiac.equinix.design.optimizer.enums.MulticloudEnvironmentStatus;
import com.eqixiac.equinix.design.optimizer.model.MulticloudEnvironment;
import com.eqixiac.equinix.design.optimizer.model.MulticloudEnvironmentCatalog;
import com.eqixiac.equinix.design.optimizer.model.OptimizationResult;
import com.eqixiac.equinix.design.optimizer.wizard.enums.CloudToCloudStrategy;
import com.eqixiac.equinix.design.optimizer.wizard.model.DeploymentPlan;
import com.eqixiac.equinix.design.optimizer.wizard.model.MulticloudLinkPricing;
import com.eqixiac.equinix.design.optimizer.wizard.model.PlannedMulticloudInterconnect;
import com.eqixiac.equinix.design.value.ratecard.CustomRateCard;
import com.eqixiac.equinix.design.value.ratecard.RateCard;
import com.eqixiac.equinix.design.value.ratecard.ReferenceRateCard;
import com.eqixiac.equinix.design.value.ratecard.Term;
import com.eqixiac.equinix.design.value.savings.DataUnit;
import com.eqixiac.equinix.design.value.savings.MulticloudPathComparison;
import com.eqixiac.equinix.design.value.savings.SavingsEstimate;
import com.eqixiac.equinix.design.value.tco.CostBreakdown;
import com.eqixiac.equinix.design.value.tco.DeploymentArchetype;
import com.eqixiac.equinix.design.value.tco.TcoComparison;
import com.eqixiac.equinix.fabric.model.EnvironmentActionResponse;
import com.eqixiac.equinix.fabric.model.implementation.ProviderEnvironment;
import com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType;

import java.math.BigDecimal;
import java.util.List;

/**
 * Compile-time mirror of the README Java examples that cover cloud-to-cloud planning: the section
 * "Design: Cloud-to-Cloud" and the Deployment Wizard subsection "Cloud-to-Cloud Flows: Native
 * Multicloud Links (Beta)". Each method body is the README example verbatim. Change both together.
 *
 * <p>Not a test. It has no assertions and the suite never invokes it. It fails compilation when
 * the API differs from what the README shows.</p>
 *
 * <p><b>Beta</b>: every API used here is a Beta surface (see the README section).</p>
 */
final class ReadmeCloudToCloudShowcase {

    private ReadmeCloudToCloudShowcase() {}

    /**
     * README "Comparing three archetypes with TcoCalculator", first example.
     *
     * @param fabric a Fabric client; with an explicit rate card it is not used for pricing
     */
    static void threeArchetypes(Fabric fabric) {
        TcoComparison tco = fabric.tcoComparison()
            .egress(100, DataUnit.TERABYTE)               // AWS -> Google Cloud, per month
            .reverseEgress(40, DataUnit.TERABYTE)         // Google Cloud -> AWS, per month; defaults to the forward volume
            .fromCloud(CloudProviderType.AWS).inRegion("us-east-1")
            .toCloud(CloudProviderType.GOOGLE_CLOUD).toRegion("us-east4")
            .viaMetro(MetroCode.DC)
            .bandwidthMbps(10_000)                        // link size on every path, Mbps
            .includeCloudRouter("STANDARD")               // A-side of the two Fabric connections
            .pathTier(1)                                  // AWS connectivity-scope tier, 1-5
            .term(Term.MONTH_12)
            .archetypes(DeploymentArchetype.PUBLIC_CLOUD_INTERNET,
                        DeploymentArchetype.EQUINIX_INTERCONNECT,
                        DeploymentArchetype.NATIVE_MULTICLOUD_INTERCONNECT)
            .rateCard(ReferenceRateCard.standard())       // bundled figures only, so the output below is reproducible offline
            .compare();

        System.out.println(tco.getTrafficNote());
        System.out.println(tco.getRecommended());         // EQUINIX_INTERCONNECT

        CostBreakdown nativeLink = tco.breakdown(DeploymentArchetype.NATIVE_MULTICLOUD_INTERCONNECT).orElseThrow();
        if (!nativeLink.isPriced()) {
            System.out.println(nativeLink.getNote());     // names the unpriced side and why
        }
        nativeLink.getProvenance().forEach(System.out::println);   // per figure: source URL, retrieval date, USD/h x 730 h
    }

    /**
     * README "Comparing three archetypes with TcoCalculator", break-even example.
     *
     * @param fabric a Fabric client; with an explicit rate card it is not used for pricing
     */
    static void breakEven(Fabric fabric) {
        SavingsEstimate estimate = fabric.savingsCalculator()
            .egress(100, DataUnit.TERABYTE)
            .reverseEgress(40, DataUnit.TERABYTE)
            .fromCloud(CloudProviderType.AWS).inRegion("us-east-1")
            .toCloud(CloudProviderType.GOOGLE_CLOUD).toRegion("us-east4")
            .viaMetro(MetroCode.DC).bandwidthMbps(10_000)
            .includeCloudRouter("STANDARD")
            .rateCard(ReferenceRateCard.standard())
            .calculate();

        estimate.breakEvenSustainedMbps()                 // empty when a side is unpriced or currencies differ
            .ifPresent(mbps -> System.out.println(mbps + " Mbps, both directions summed"));   // 2784.3

        MulticloudPathComparison paths = estimate.getMulticloudComparison();   // null without toCloud(...)
        System.out.println(paths.getLowestCostPath());    // PRIVATE at these volumes
        paths.getNotes().forEach(System.out::println);    // provenance and the reason for each null figure
    }

    /**
     * README "Comparing three archetypes with TcoCalculator", caller-supplied native-link rate.
     *
     * @return the layered card of the example
     */
    static RateCard suppliedNativeRate() {
        RateCard rates = RateCard.layered(
            CustomRateCard.builder()
                .currency("USD")
                // placeholder value: use the hourly rate on your AWS quote. Arguments: provider, Mbps, path tier, USD/h.
                .multicloudLinkHourlyRate(CloudProviderType.AWS, 1_000, 1, new BigDecimal("2.00"))
                .build(),
            ReferenceRateCard.standard());                // Google Cloud side and both per-GB rates
        return rates;
    }

    /**
     * README "Native links on a deployment plan".
     *
     * @param fabric a Fabric client
     * @param result a Metro Optimizer result
     */
    static void nativeLinksOnAPlan(Fabric fabric, OptimizationResult result) {
        DeploymentPlan plan = fabric.deploymentWizard(result)
            .cloudToCloudStrategy(CloudToCloudStrategy.COMPARE)   // the default, shown for clarity
            .notifications("noc@example.com")
            .plan();

        // getMulticloudLinks() is null or empty when the plan has no native link; this accessor is null-safe.
        for (PlannedMulticloudInterconnect link : plan.multicloudLinksOrEmpty()) {
            System.out.println(link.getName() + ": " + link.describe() + ", role " + link.getRole());

            MulticloudLinkPricing price = link.getPricing();      // null for an UNAVAILABLE entry
            if (price != null && price.isNativePriced()) {
                System.out.println("native " + price.getNativeMonthly() + " " + price.getNativeCurrency() + "/month");
            }
            if (price != null) {
                price.breakEvenSustainedMbps().ifPresent(mbps -> System.out.println("break-even " + mbps + " Mbps"));
            }
            link.getCreateThenAcceptRecipe().forEach(step -> System.out.println("  " + step));
        }
    }

    /**
     * README "Cloud-to-Cloud Flows: Native Multicloud Links (Beta)", first example.
     *
     * @param fabric a Fabric client
     * @param result a Metro Optimizer result
     */
    static void wizardStrategyAndPathTier(Fabric fabric, OptimizationResult result) {
        DeploymentPlan plan = fabric.deploymentWizard(result)
            .cloudToCloudStrategy(CloudToCloudStrategy.COMPARE)   // the default
            .multicloudPathTier(1)                                // AWS connectivity-scope tier 1-5; an input, default 1
            .notifications("noc@example.com")
            .plan();

        for (PlannedMulticloudInterconnect link : plan.multicloudLinksOrEmpty()) {
            System.out.println(link.describe());            // AWS us-east-1 <-> GOOGLE_CLOUD us-east4, 10000 Mbps, GA, ALTERNATIVE
            System.out.println(link.getRecommendation());   // both fixed costs, the break-even rate, which path costs less on each side
            link.getPricing().breakEvenSustainedMbps()
                .ifPresent(mbps -> System.out.println(mbps + " Mbps sustained, both directions summed"));
        }
    }

    /**
     * README "Cloud-to-Cloud Flows: Native Multicloud Links (Beta)", caller-supplied catalog entry.
     *
     * @param fabric a Fabric client
     * @param result a Metro Optimizer result
     */
    static void wizardCustomCatalog(Fabric fabric, OptimizationResult result) {
        MulticloudEnvironmentCatalog catalog = MulticloudEnvironmentCatalog.standard().with(
            MulticloudEnvironment.of(CloudProviderType.AWS, "eu-west-1", CloudProviderType.GOOGLE_CLOUD, "europe-west1",
                MulticloudEnvironmentStatus.GA, BandwidthTier.of(1000, 10_000),
                "https://docs.aws.amazon.com/interconnect/latest/userguide/region-availability.html", "2026-12-01"));

        DeploymentPlan plan = fabric.deploymentWizard(result)
            .multicloudEnvironments(catalog)
            .notifications("noc@example.com")
            .plan();

        System.out.println(plan.multicloudLinksOrEmpty().size());
    }

    /**
     * README "Decoding an activation key".
     *
     * @param keyFromProvider the key string as displayed by the issuing provider
     * @return the decoded key
     */
    static ActivationKey decodeActivationKey(String keyFromProvider) {
        ActivationKey key = ActivationKey.decode(keyFromProvider);   // throws only for a null or blank argument

        String summary = switch (key) {
            case ActivationKey.V1 v1 ->
                v1.connectionSizeMbps() + " Mbps to " + v1.destinationEnvironmentUri();
            case ActivationKey.V2Encrypted v2 ->
                "encrypted; the envelope names " + v2.destinationEnvironmentUri();
            case ActivationKey.Opaque opaque ->
                "format not recognized, " + opaque.raw().length() + " characters";
        };
        System.out.println(summary);

        key.destinationEnvironmentId().ifPresent(System.out::println);   // the id after "environments/" in the URI

        System.out.println(key);
        // ActivationKey.V1{version=1, destinationEnvironmentUri=providers/gcp/environments/aws-us-east-1--gcp-us-east4,
        //   sharedConnectionUuid=<redacted>, connectionSizeMbps=10000, destinationAccountId=<redacted>}

        String toEnterAtTheOtherProvider = key.encode();   // the issued string with ASCII whitespace removed; not re-serialized
        System.out.println(toEnterAtTheOtherProvider.length());
        return key;
    }

    /**
     * README "Beta Fabric v4: provider environments and validateActivationKey".
     *
     * @param fabric        an authenticated Fabric client
     * @param icProfileUuid the uuid of an {@code IC_PROFILE} service profile
     * @param key           a decoded activation key
     */
    static void validateActivationKey(Fabric fabric, String icProfileUuid, ActivationKey key) {
        List<ProviderEnvironment> environments = fabric.serviceProfiles().getEnvironments(icProfileUuid);
        for (ProviderEnvironment environment : environments) {
            System.out.println(environment.getUuid() + " " + environment.getRegion()
                + " " + environment.getSupportedBandwidths());            // Mbps, for example [1000, 10000, 100000]
        }

        EnvironmentActionResponse response = fabric.serviceProfiles()
            .validateActivationKey(icProfileUuid, environments.get(0).getUuid(), key.encode());

        if (response.isKeyUnused()) {                                     // true only for state INACTIVE
            System.out.println("key not yet used: " + response.getUuid());
        }
    }
}
