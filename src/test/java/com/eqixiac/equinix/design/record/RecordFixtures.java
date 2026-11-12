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


package com.eqixiac.equinix.design.record;

import com.eqixiac.equinix.core.enums.MetroCode;
import com.eqixiac.equinix.core.enums.Region;
import com.eqixiac.equinix.design.cost.CurrencyConverters;
import com.eqixiac.equinix.design.optimizer.context.CatalogDesignContext;
import com.eqixiac.equinix.design.spec.CloudIntent;
import com.eqixiac.equinix.design.spec.CommercialTerms;
import com.eqixiac.equinix.design.spec.DesignConstraints;
import com.eqixiac.equinix.design.spec.DesignSpec;
import com.eqixiac.equinix.design.spec.SiteIntent;
import com.eqixiac.equinix.design.spec.WorkloadIntent;
import com.eqixiac.equinix.design.spi.CurrencyConverter;
import com.eqixiac.equinix.design.value.ratecard.CustomRateCard;
import com.eqixiac.equinix.design.value.ratecard.RateCard;
import com.eqixiac.equinix.design.value.ratecard.ReferenceRateCard;
import com.eqixiac.equinix.design.value.ratecard.Term;
import com.eqixiac.equinix.fabric.enums.ConnectionType;
import com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

/**
 * A small offline world for decision-record tests. Round trips are Fabric's live figures where Fabric published them
 * on 2026-11-11 (DC-NY 6.3 ms, DC-LD 73.4 ms); NY-LD and the prices are synthetic test values.
 *
 * <ul>
 *   <li>Sites: HQ in DC (weight 3), London office in LD (weight 1).</li>
 *   <li>Clouds: aws (AWS us-east-1, 20,000 GB/month out) and azure (eastus).</li>
 *   <li>Workloads: api (1000 Mbps to aws), batch (500 Mbps to aws and azure).</li>
 *   <li>Constraints: LD required, at most 2 metros. LD reaches aws only, so batch needs a second metro.</li>
 *   <li>Prices: London connections and its Cloud Router in GBP (converted at a supplied 1.25 USD/GBP), the rest
 *       in USD; provider fees and egress from the SDK's reference table.</li>
 * </ul>
 */
final class RecordFixtures {

    static final Instant NOW = Instant.parse("2026-11-12T09:30:00Z");

    private RecordFixtures() {
    }

    static DesignSpec.DesignSpecBuilder specBuilder() {
        return DesignSpec.builder().name("ddr-test").description("Interconnect for the payments platform.")
                .site(SiteIntent.builder().label("HQ").metro("DC").weight(3.0).build())
                .site(SiteIntent.builder().label("London office").metro("LD").weight(1.0).build())
                .cloud(CloudIntent.builder().id("aws").provider(CloudProviderType.AWS).region("us-east-1")
                        .monthlyEgressGb(20_000d).build())
                .cloud(CloudIntent.builder().id("azure").provider(CloudProviderType.AZURE).region("eastus").build())
                .workload(WorkloadIntent.builder().label("api").bandwidthMbps(1000).clouds(List.of("aws")).build())
                .workload(WorkloadIntent.builder().label("batch").bandwidthMbps(500).clouds(List.of("aws", "azure"))
                        .build())
                .constraints(DesignConstraints.builder().requiredMetros(List.of("LD")).maxMetros(2).build())
                .commercial(CommercialTerms.builder().annualGrowthRate(0.4).horizonMonths(24).build());
    }

    static DesignSpec spec() {
        return specBuilder().build();
    }

    static CatalogDesignContext context(DesignSpec spec) {
        return CatalogDesignContext.builder(spec)
                .metro("DC", Region.AMER, 38.9, -77.4, "US").metro("NY", Region.AMER, 40.7, -74.0, "US")
                .metro("LD", Region.EMEA, 51.5, -0.1, "UK")
                .measuredLatency("DC", "NY", 6.3).measuredLatency("DC", "LD", 73.4)
                .reachable("DC", "aws", "azure").reachable("NY", "aws", "azure").reachable("LD", "aws")
                .asOf(Instant.parse("2026-11-12T08:00:00Z"))
                .build();
    }

    /** London in GBP (metro-specific rows), everything else in USD; provider fees and egress from the reference. */
    static RateCard card() {
        CustomRateCard.Builder gbp = CustomRateCard.builder().currency("GBP");
        for (Term t : Term.values()) {
            gbp.connectionRate(ConnectionType.IP_VC, 1000, MetroCode.LD, t, BigDecimal.valueOf(160))
                    .connectionRate(ConnectionType.IP_VC, 2000, MetroCode.LD, t, BigDecimal.valueOf(260))
                    .cloudRouterRate("STANDARD", MetroCode.LD, t, BigDecimal.valueOf(t == Term.MONTH_1 ? 1200 : 1000));
        }
        CustomRateCard.Builder usd = CustomRateCard.builder().currency("USD");
        int[][] rows = {{500, 150}, {1000, 200}, {2000, 320}, {5000, 600}};
        for (int[] r : rows) {
            usd.connectionRate(ConnectionType.IP_VC, r[0], BigDecimal.valueOf(r[1]));
        }
        usd.cloudRouterRate("STANDARD", BigDecimal.valueOf(1500));
        return RateCard.layered(gbp.build(), usd.build(), ReferenceRateCard.standard());
    }

    /** 1 GBP = 1.25 USD, as a caller-supplied rate. */
    static CurrencyConverter gbpToUsd() {
        return (from, to) -> "GBP".equals(from) && "USD".equals(to)
                ? Optional.of(new CurrencyConverter.Rate("GBP", "USD", new BigDecimal("1.25"),
                CurrencyConverter.Kind.SUPPLIED, "test treasury sheet", "2026-11-12", 0))
                : Optional.empty();
    }

    static DesignDecisionRecorder recorder(DesignSpec spec) {
        return DesignDecisionRecorder.forContext(context(spec)).rateCard(card()).currencyConverter(gbpToUsd())
                .defaultRates(CurrencyConverters.none()).clock(Clock.fixed(NOW, ZoneOffset.UTC));
    }
}
