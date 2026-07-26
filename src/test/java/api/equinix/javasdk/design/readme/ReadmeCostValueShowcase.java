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

package api.equinix.javasdk.design.readme;

import api.equinix.javasdk.Fabric;
import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.design.value.ratecard.ColocationItem;
import api.equinix.javasdk.design.value.ratecard.CustomRateCard;
import api.equinix.javasdk.design.value.ratecard.EgressPath;
import api.equinix.javasdk.design.value.ratecard.RateCard;
import api.equinix.javasdk.design.value.ratecard.Term;
import api.equinix.javasdk.design.value.savings.DataUnit;
import api.equinix.javasdk.design.value.savings.SavingsEstimate;
import api.equinix.javasdk.design.value.tco.CostBreakdown;
import api.equinix.javasdk.design.value.tco.DeploymentArchetype;
import api.equinix.javasdk.design.value.tco.TcoComparison;
import api.equinix.javasdk.fabric.enums.ConnectionType;
import api.equinix.javasdk.fabric.model.implementation.cloud.CloudProviderType;

import java.math.BigDecimal;

/**
 * Mirrors the README "Cost &amp; Value Engineering" example verbatim — compiled with the test
 * tree so the README can never rot. Update BOTH together.
 *
 * <p>Not a test: no assertions, never invoked by the suite. Its sole job is to fail compilation
 * the moment the cost/value API drifts from what the README's "GlobalPay — Multi-Cloud Egress
 * Consolidation" worked scenario shows.</p>
 */
final class ReadmeCostValueShowcase {

    private ReadmeCostValueShowcase() {}

    /**
     * The full GlobalPay scenario from the README: per-cloud egress savings, a negotiated
     * {@link CustomRateCard} layered over the standard chain, and the 36-month hub TCO with
     * the cabinets / cross-connects / power levers.
     *
     * @param fabric an authenticated Fabric client (never invoked here — compile-check only)
     */
    static void globalPayShowcase(Fabric fabric) {

        // ── Step 1: per-cloud egress economics (standard chain: live → reference) ──

        SavingsEstimate aws = fabric.savingsCalculator()
            .egress(40, DataUnit.TERABYTE)
            .fromCloud(CloudProviderType.AWS).inRegion("us-east-1")
            .viaMetro(MetroCode.DC).bandwidthMbps(5_000)
            .calculate();

        SavingsEstimate azure = fabric.savingsCalculator()
            .egress(25, DataUnit.TERABYTE)
            .fromCloud(CloudProviderType.AZURE).inRegion("eastus")
            .viaMetro(MetroCode.DC).bandwidthMbps(2_000)
            .calculate();

        SavingsEstimate gcp = fabric.savingsCalculator()
            .egress(15, DataUnit.TERABYTE)
            .fromCloud(CloudProviderType.GOOGLE_CLOUD).inRegion("us-east4")
            .viaMetro(MetroCode.DC).bandwidthMbps(1_000)
            .calculate();

        System.out.println(aws.toMarkdown());
        System.out.printf("Net monthly savings — AWS %s, Azure %s, GCP %s%n",
            aws.getNetMonthlySavings(), azure.getNetMonthlySavings(), gcp.getNetMonthlySavings());

        // ── Step 2: negotiated rates over the standard chain ──

        CustomRateCard negotiated = CustomRateCard.builder()
            .currency("USD")
            // the hub's 10G IP_VC at the contracted DC/36-month rate:
            .connectionRate(ConnectionType.IP_VC, 10_000, MetroCode.DC, Term.MONTH_36,
                            new BigDecimal("300.00"))
            .cloudRouterRate("STANDARD", new BigDecimal("950.00"))
            // colocation primitives — per cabinet, per cross-connect, per kW:
            .colocationRate(ColocationItem.CABINET, MetroCode.DC, Term.MONTH_36,
                            new BigDecimal("550.00"), new BigDecimal("500.00"))
            .colocationRate(ColocationItem.CROSS_CONNECT, new BigDecimal("150.00"))
            .colocationRate(ColocationItem.POWER_PER_KW, new BigDecimal("140.00"))
            .build();

        RateCard rates = RateCard.layered(negotiated, RateCard.standardChain(fabric));

        // ── Step 3: the whole hub, over the full term ──

        TcoComparison tco = fabric.tcoComparison()
            .egress(80, DataUnit.TERABYTE)      // consolidated hub: 40 AWS + 25 Azure + 15 GCP
            .fromCloud(CloudProviderType.AWS).inRegion("us-east-1")
            .viaMetro(MetroCode.DC)
            .bandwidthMbps(10_000)              // one shared 10G port
            .connectionType(ConnectionType.IP_VC)
            .includeCloudRouter("STANDARD")
            .cabinets(2)                        // 2 × the per-cabinet quote
            .crossConnects(4)                   // 4 × the per-cross-connect quote
            .powerKw(5.0)                       // 5 × the per-kW quote
            .term(Term.MONTH_36)
            .archetypes(DeploymentArchetype.PUBLIC_CLOUD_INTERNET,
                        DeploymentArchetype.EQUINIX_INTERCONNECT)
            .rateCard(rates)
            .compare();

        // ── Reading the result ──

        CostBreakdown equinix = tco.breakdown(DeploymentArchetype.EQUINIX_INTERCONNECT).orElseThrow();
        equinix.getLineItems().forEach((item, monthly) ->
            System.out.printf("  %-42s %,10.2f%n", item, monthly));
        System.out.printf("Total over term: %,.2f %s%n",
            equinix.getTotalOverTerm(), equinix.getCurrency());

        if (tco.getSavingsOverTermVsBaseline() != null) {   // null when unpriced or cross-currency
            System.out.printf("36-month saving vs. internet egress: %,.2f %s%n",
                tco.getSavingsOverTermVsBaseline(), tco.getCurrency());
        }

        System.out.println(tco.toMarkdown());

        // Provenance: ask the card itself — every quote carries its source and note.
        rates.connection(ConnectionType.IP_VC, 10_000, MetroCode.DC, Term.MONTH_36)
            .ifPresent(q -> System.out.println("connection source: " + q.getSource()));  // CUSTOM
        rates.egress(CloudProviderType.AWS, "us-east-1", EgressPath.PRIVATE, Term.MONTH_36)
            .ifPresent(r -> System.out.println("egress source: " + r.getSource()
                + " — " + r.getNote()));   // REFERENCE — "Direct Connect DTO, contiguous US"
    }
}
