# Metro design for global-interconnect

- Status: Proposed
- Date: 2026-11-16
- Spec: `global-interconnect`, fingerprint `a5f92102787ee30f40ab911fee6c2caf9ba279c5ead65986a92ba8d9b2a9a7da`
- Produced by: equinix-sdk-java development build (no jar manifest version), decision record schema 1

## Context

Private access to three clouds from six sites, with peering and internet breakout.

Sites:

- Ashburn data center: metro DC, data center, weight 3
- Chicago headquarters: metro CH, headquarters, 1200 people
- Dallas operations: metro DA, employee hub, 400 people
- London office: metro LD, primary market, 600 people
- Frankfurt plant: metro FR, manufacturing, 300 people
- Singapore hub: metro SG, branch office, 250 people

Workloads:

- order-api: 1000 Mbps; reaches aws; round trip at most 250 ms from every site; type TRANSACTIONAL
- analytics: 2000 Mbps; reaches aws, azure; type BIG_DATA_ANALYTICS
- inference: 500 Mbps; reaches gcp; pinned to SG; type AI_ML_INFERENCE
- plant-erp: 500 Mbps; reaches azure; pinned to FR; type CUSTOM
- disaster-recovery: 1000 Mbps; reaches aws, azure; type DISASTER_RECOVERY

Clouds:

- aws: AWS, region us-east-1, required, 60,000 GB/month out
- azure: AZURE, region westeurope, required, 30,000 GB/month out
- gcp: GOOGLE_CLOUD, region asia-southeast1, required, 15,000 GB/month out

Peer networks:

- AS13335 (CDN and DNS provider)
- AS20940 (Content delivery provider)
- AS8075 (SaaS productivity provider)

Constraints:

- Must include metros DC, FR
- Must have a metro in each of AMER, EMEA, APAC
- Redundancy at least MULTI_REGION
- Metros per design: 3 to 4
- At least 40% of service must survive the worst single failure
- Monthly cost at most USD 60,000.00

Objectives: explicit weights; weights latency 30%, cost 20%, provider_coverage 20%, resilience 15%, compliance 5%, carbon 10%.

Commercial terms:

- Currency USD
- Term MONTH_12
- Traffic growth 30%/year
- Horizon 36 months

## Decision

Deploy in **TR (Toronto) + FR (Frankfurt) + SG (Singapore) + DC (Ashburn)** (portfolio `DC+FR+SG+TR`, weighted score 90.1 of 100).

DC+FR+SG+TR has the highest weighted score (90.1 of 100) of the 15 feasible design(s) out of 814,385 examined; no feasible design is at least as good on every objective. Weights: latency 30%, cost 20%, provider_coverage 20%, resilience 15%, carbon 10%, compliance 5%. The closest alternative is DC+FR+MA+SG (87.5).

| Objective | Value | Score | Weight | Scale |
|---|---:|---:|---:|---|
| latency | 11.9 ms | 94.1 | 30% | fixed: 0 scores 100, 200 scores 0 |
| cost | USD 16,180.91/month | 89.5 | 20% | ratio to the best option (twice as much scores 50) |
| provider_coverage | 100.0% | 100.0 | 20% | the percentage itself |
| resilience | 82.2% | 82.2 | 15% | the percentage itself |
| compliance | 100.0% | 100.0 | 5% | the percentage itself |
| carbon | 235.25 gCO2e/kWh | 66.4 | 10% | fixed: 0 scores 100, 700 scores 0 |

Placement:

- order-api: runs in TR (site-weighted round trip 45.9 ms), reaching aws; standby in DC
- analytics: runs in TR (site-weighted round trip 45.9 ms), reaching aws, azure; standby in DC
- inference: pinned to SG (site-weighted round trip 177.2 ms), reaching gcp
- plant-erp: pinned to FR (site-weighted round trip 78.2 ms), reaching azure
- disaster-recovery: runs in TR (site-weighted round trip 45.9 ms), reaching aws, azure; standby in DC

Constraints checked:

- required_metros: includes every required metro [DC, FR]
- required_regions: has a metro in every required region [AMER, EMEA, APAC]
- minimum_redundancy: MULTI_REGION met: 4 metros in [AMER, EMEA, APAC]
- metro_count: 4 metros within 3..4
- pinned_workloads: pinned workloads placed: inference@SG, plant-erp@FR
- workload_max_latency: 1 workload latency limit(s) met
- required_clouds: every required cloud is reachable where it is needed
- budget: USD 16180.91/month within budget 60000
- every_metro_used: every metro carries a workload or a cloud connection
- min_survival: 80% survives the worst single failure (minimum 40%)

## Consequences

- Cost: USD 16,180.91 per month (range USD 13,732.42 to USD 18,629.42).
- Resilience: N-1 fails: 6 of all 38 scenarios cause an outage.
- 6 single point(s) of failure, the first: loss of metro FR: workloads down: plant-erp; required clouds lost: azure.
- The decision holds while each weight stays within: latency 0% to 50%; cost 0% to 25%; carbon 7% to 100%; other weights can take any value.
- 7 critical or high finding(s); see appendix D.
- Implementation: 4 Cloud Router(s), 12 provider connection(s), 6 backbone link(s), 58 plan step(s).

## Alternatives considered

814,385 portfolios examined, 15 feasible, 5 on the Pareto frontier (exhaustive search).

| Option | Score | latency | cost | provider_coverage | resilience | compliance | carbon | Gains vs recommendation | Gives up vs recommendation |
|---|---:|---:|---:|---:|---:|---:|---:|---|---|
| **DC+FR+SG+TR** (chosen) | 90.1 | 11.9 ms | USD 16,180.91/month | 100.0% | 82.2% | 100.0% | 235.25 gCO2e/kWh | - | - |
| DC+FR+SG | 89.4 | 16.0 ms | USD 14,480.63/month | 100.0% | 82.2% | 100.0% | 390.19 gCO2e/kWh | cost USD 14,480.63/month vs USD 16,180.91/month | latency 16.0 ms vs 11.9 ms; carbon 390.19 gCO2e/kWh vs 235.25 gCO2e/kWh |
| CH+DC+FR+SG | 88.8 | 5.4 ms | USD 16,233.91/month | 100.0% | 82.2% | 100.0% | 390.19 gCO2e/kWh | latency 5.4 ms vs 11.9 ms | cost USD 16,233.91/month vs USD 16,180.91/month; carbon 390.19 gCO2e/kWh vs 235.25 gCO2e/kWh |
| AM+DC+FR+SG | 87.5 | 14.9 ms | USD 16,112.13/month | 100.0% | 82.2% | 100.0% | 390.19 gCO2e/kWh | cost USD 16,112.13/month vs USD 16,180.91/month | latency 14.9 ms vs 11.9 ms; carbon 390.19 gCO2e/kWh vs 235.25 gCO2e/kWh |
| DC+FR+MA+SG | 87.5 | 14.8 ms | USD 16,170.71/month | 100.0% | 82.2% | 100.0% | 390.19 gCO2e/kWh | cost USD 16,170.71/month vs USD 16,180.91/month | latency 14.8 ms vs 11.9 ms; carbon 390.19 gCO2e/kWh vs 235.25 gCO2e/kWh |

Rejected by constraint:

| Constraint | Binding | Rejected alone | Chosen without it | Explanation |
|---|---|---:|---|---|
| required_metros | yes | 140 | FR+SG+TR | binding: without it FR+SG+TR would be recommended (140 portfolio(s) rejected by it alone) |
| required_regions | no | 0 | - | rejects no portfolio on its own |
| minimum_redundancy | no | 0 | - | rejects no portfolio on its own |
| metro_count | no | 0 | - | rejects no portfolio on its own |
| pinned_workloads | no | 0 | - | rejects no portfolio on its own |
| workload_max_latency | no | 0 | - | rejects no portfolio on its own |
| required_clouds | no | 0 | - | rejects no portfolio on its own |
| budget | no | 0 | - | rejects no portfolio on its own |
| every_metro_used | no | 50 | - | not binding: it rejects 50 portfolio(s) on its own but none would outrank the recommendation |
| min_survival | no | 0 | - | rejects no portfolio on its own |

Search notes:

- Portfolios rejected, by the first constraint that failed: {required_metros=812240, required_regions=1275, pinned_workloads=805, every_metro_used=50}.

## Evidence

### A. Sensitivity

| Objective | Weight now | Holds from | Holds to | Below, chosen | Above, chosen |
|---|---:|---:|---:|---|---|
| latency | 30% | 0% | 50% | - | CH+DC+FR+SG |
| cost | 20% | 0% | 25% | - | DC+FR+SG |
| provider_coverage | 20% | 0% | 100% | - | - |
| resilience | 15% | 0% | 100% | - | - |
| compliance | 5% | 0% | 100% | - | - |
| carbon | 10% | 7% | 100% | DC+FR+SG | - |

Nearest alternative: DC+FR+MA+SG (score distance 22.2); difference from the recommendation: latency +2.89, cost -10.20, provider_coverage 0.00, resilience 0.00, compliance 0.00, carbon +154.94.

### B. Cost

| Item | Metro | Monthly | Range | One-time | Source | Note |
|---|---|---:|---|---:|---|---|
| Fabric IP_VC 5000 Mbps DC to aws | DC | USD 375.00 | 375.00 to 375.00 | 0.00 | live | Equinix price ECX00015.PROD; exact (EQUINIX_LIVE) |
| Provider hosted-connection fee aws 5000 Mbps in DC | DC | USD 1,204.50 | 903.38 to 1,505.63 | 0.00 | reference | AWS Direct Connect hosted connection, port-hour by capacity (all locations except Japan; 1 Gbps and above from select partners only), 5000 Mbps: 1.65 USD/h x 730 h = 1204.50 USD/month; source https://aws.amazon.com/directconnect/pricing/, retrieved 2026-11-12; range +/-25% (REFERENCE) |
| Fabric IP_VC 5000 Mbps DC to azure | DC | USD 375.00 | 375.00 to 375.00 | 0.00 | live | Equinix price ECX00015.PROD; exact (EQUINIX_LIVE) |
| Provider hosted-connection fee azure 5000 Mbps in DC | DC | USD 2,180.00 | 1,635.00 to 2,725.00 | 0.00 | reference | Azure ExpressRoute circuit, Standard Metered Data, Zone 1 (monthly); over Equinix Fabric the circuit is ordered through the service-provider (partner) model, 5000 Mbps: 2180 USD/month; source https://prices.azure.com/api/retail/prices?$filter=serviceName eq 'ExpressRoute' and productName eq 'ExpressRoute' and armRegionName eq 'Zone 1' (Standard Metered Data circuits; pricing page https://azure.microsoft.com/en-us/pricing/details/expressroute/), retrieved 2026-11-12; range +/-25% (REFERENCE) |
| Fabric IP_VC 500 Mbps FR to azure | FR | USD 143.18 | 140.32 to 146.04 | 0.00 | live | Equinix price ECX00015.PROD; exact (EQUINIX_LIVE); converted from EUR 130.0 at 1.10139437 USD/EUR (ECB reference rate via Frankfurter, 2026-11-16, as of 2026-11-16); range widened +/-2% for exchange-rate uncertainty |
| Provider hosted-connection fee azure 500 Mbps in FR | FR | USD 290.00 | 217.50 to 362.50 | 0.00 | reference | Azure ExpressRoute circuit, Standard Metered Data, Zone 1 (monthly); over Equinix Fabric the circuit is ordered through the service-provider (partner) model, 500 Mbps: 290 USD/month; source https://prices.azure.com/api/retail/prices?$filter=serviceName eq 'ExpressRoute' and productName eq 'ExpressRoute' and armRegionName eq 'Zone 1' (Standard Metered Data circuits; pricing page https://azure.microsoft.com/en-us/pricing/details/expressroute/), retrieved 2026-11-12; range +/-25% (REFERENCE) |
| Fabric IP_VC 500 Mbps SG to gcp | SG | USD 151.30 | 148.28 to 154.33 | 0.00 | live | Equinix price ECX00015.PROD; exact (EQUINIX_LIVE); converted from SGD 195.0 at 0.77591558 USD/SGD (ECB reference rate via Frankfurter, 2026-11-16, as of 2026-11-16); range widened +/-2% for exchange-rate uncertainty |
| Provider hosted-connection fee gcp 500 Mbps in SG | SG | USD 126.73 | 95.05 to 158.41 | 0.00 | reference | Google Cloud Partner Interconnect VLAN attachment by capacity (hourly), 500 Mbps: 0.1736 USD/h x 730 h = 126.73 USD/month; source https://cloud.google.com/network-connectivity/docs/interconnect/pricing, retrieved 2026-11-12; range +/-25% (REFERENCE) |
| Fabric IP_VC 5000 Mbps TR to aws | TR | USD 364.02 | 356.74 to 371.30 | 0.00 | live | Equinix price ECX00015.PROD; exact (EQUINIX_LIVE); converted from CAD 520.0 at 0.700035 USD/CAD (ECB reference rate via Frankfurter, 2026-11-16, as of 2026-11-16); range widened +/-2% for exchange-rate uncertainty |
| Provider hosted-connection fee aws 5000 Mbps in TR | TR | USD 1,204.50 | 903.38 to 1,505.63 | 0.00 | reference | AWS Direct Connect hosted connection, port-hour by capacity (all locations except Japan; 1 Gbps and above from select partners only), 5000 Mbps: 1.65 USD/h x 730 h = 1204.50 USD/month; source https://aws.amazon.com/directconnect/pricing/, retrieved 2026-11-12; range +/-25% (REFERENCE) |
| Fabric IP_VC 5000 Mbps TR to azure | TR | USD 364.02 | 356.74 to 371.30 | 0.00 | live | Equinix price ECX00015.PROD; exact (EQUINIX_LIVE); converted from CAD 520.0 at 0.700035 USD/CAD (ECB reference rate via Frankfurter, 2026-11-16, as of 2026-11-16); range widened +/-2% for exchange-rate uncertainty |
| Provider hosted-connection fee azure 5000 Mbps in TR | TR | USD 2,180.00 | 1,635.00 to 2,725.00 | 0.00 | reference | Azure ExpressRoute circuit, Standard Metered Data, Zone 1 (monthly); over Equinix Fabric the circuit is ordered through the service-provider (partner) model, 5000 Mbps: 2180 USD/month; source https://prices.azure.com/api/retail/prices?$filter=serviceName eq 'ExpressRoute' and productName eq 'ExpressRoute' and armRegionName eq 'Zone 1' (Standard Metered Data circuits; pricing page https://azure.microsoft.com/en-us/pricing/details/expressroute/), retrieved 2026-11-12; range +/-25% (REFERENCE) |
| Fabric Cloud Router STANDARD in DC | DC | USD 1,275.00 | 1,275.00 to 1,275.00 | 0.00 | live | Equinix price ECX00014.PROD; exact (EQUINIX_LIVE) |
| Fabric Cloud Router STANDARD in FR | FR | USD 1,198.32 | 1,174.35 to 1,222.28 | 0.00 | live | Equinix price ECX00014.PROD; exact (EQUINIX_LIVE); converted from EUR 1088.0 at 1.10139437 USD/EUR (ECB reference rate via Frankfurter, 2026-11-16, as of 2026-11-16); range widened +/-2% for exchange-rate uncertainty |
| Fabric Cloud Router STANDARD in SG | SG | USD 1,276.38 | 1,250.85 to 1,301.91 | 0.00 | live | Equinix price ECX00014.PROD; exact (EQUINIX_LIVE); converted from SGD 1645.0 at 0.77591558 USD/SGD (ECB reference rate via Frankfurter, 2026-11-16, as of 2026-11-16); range widened +/-2% for exchange-rate uncertainty |
| Fabric Cloud Router STANDARD in TR | TR | USD 1,243.96 | 1,219.08 to 1,268.84 | 0.00 | live | Equinix price ECX00014.PROD; exact (EQUINIX_LIVE); converted from CAD 1777.0 at 0.700035 USD/CAD (ECB reference rate via Frankfurter, 2026-11-16, as of 2026-11-16); range widened +/-2% for exchange-rate uncertainty |
| Data out of aws, 60000 GB/month over the private path | - | USD 1,200.00 | 900.00 to 1,500.00 | 0.00 | reference | Direct Connect data transfer out, contiguous US to US locations; 0.02/GB; range +/-25% (REFERENCE) |
| Data out of azure, 30000 GB/month over the private path | - | USD 750.00 | 562.50 to 937.50 | 0.00 | reference | ExpressRoute Metered Data - Data Transfer Out, Zone 1; 0.025/GB; range +/-25% (REFERENCE) |
| Data out of gcp, 15000 GB/month over the private path | - | USD 279.00 | 209.25 to 348.75 | 0.00 | reference | Cloud Interconnect data transfer, United States/Canada connection location; converted from per-GiB list price (0.02 USD/GiB = 0.0186 per decimal GB); 0.0186/GB; range +/-25% (REFERENCE) |
| **Total** | | **USD 16,180.91** | 13,732.42 to 18,629.42 | 0.00 | |  |

Exchange rates used:

| From | To | Rate | Kind | Source | As of | Band |
|---|---|---:|---|---|---|---:|
| CAD | USD | 0.700035 | LIVE | ECB reference rate via Frankfurter, 2026-11-16 | 2026-11-16 | +/-2% |
| EUR | USD | 1.10139437 | LIVE | ECB reference rate via Frankfurter, 2026-11-16 | 2026-11-16 | +/-2% |
| SGD | USD | 0.77591558 | LIVE | ECB reference rate via Frankfurter, 2026-11-16 | 2026-11-16 | +/-2% |

#### Term and growth

Traffic growth 30.0%/year over 36 months. On-demand total over the horizon: USD 737,663.56.

| Item | Upgrade | Recommended term | Why |
|---|---|---|---|
| Fabric IP_VC DC to aws | month 12 | MONTH_1 | MONTH_1 has the lowest total over 36 months among 4 priced option(s) (ties go to the shorter term) |
| Provider hosted-connection fee aws in DC | month 12 | MONTH_1 | no term axis: priced on demand |
| Fabric IP_VC DC to azure | month 25 | MONTH_1 | MONTH_1 has the lowest total over 36 months among 4 priced option(s) (ties go to the shorter term) |
| Provider hosted-connection fee azure in DC | month 25 | MONTH_1 | no term axis: priced on demand |
| Fabric IP_VC FR to azure | month 2 | MONTH_1 | MONTH_1 has the lowest total over 36 months among 4 priced option(s) (ties go to the shorter term) |
| Provider hosted-connection fee azure in FR | month 2 | MONTH_1 | no term axis: priced on demand |
| Fabric IP_VC SG to gcp | month 2 | MONTH_1 | MONTH_1 has the lowest total over 36 months among 4 priced option(s) (ties go to the shorter term) |
| Provider hosted-connection fee gcp in SG | month 2 | MONTH_1 | no term axis: priced on demand |
| Fabric IP_VC TR to aws | month 12 | MONTH_1 | MONTH_1 has the lowest total over 36 months among 4 priced option(s) (ties go to the shorter term) |
| Provider hosted-connection fee aws in TR | month 12 | MONTH_1 | no term axis: priced on demand |
| Fabric IP_VC TR to azure | month 25 | MONTH_1 | MONTH_1 has the lowest total over 36 months among 4 priced option(s) (ties go to the shorter term) |
| Provider hosted-connection fee azure in TR | month 25 | MONTH_1 | no term axis: priced on demand |
| Fabric Cloud Router STANDARD in DC | none in horizon | MONTH_36 | MONTH_36 has the lowest total over 36 months among 4 priced option(s) (ties go to the shorter term) |
| Fabric Cloud Router STANDARD in FR | none in horizon | MONTH_36 | MONTH_36 has the lowest total over 36 months among 4 priced option(s) (ties go to the shorter term) |
| Fabric Cloud Router STANDARD in SG | none in horizon | MONTH_36 | MONTH_36 has the lowest total over 36 months among 4 priced option(s) (ties go to the shorter term) |
| Fabric Cloud Router STANDARD in TR | none in horizon | MONTH_36 | MONTH_36 has the lowest total over 36 months among 4 priced option(s) (ties go to the shorter term) |
| Data out of aws | none in horizon | MONTH_1 | no term axis: priced on demand |
| Data out of azure | none in horizon | MONTH_1 | no term axis: priced on demand |
| Data out of gcp | none in horizon | MONTH_1 | no term axis: priced on demand |

#### TCO and savings

Compared for cloud `aws`.

- The private path saves USD 1,345.50 per month net at 60,000 GB/month.
- TCO over MONTH_12: cheapest archetype EQUINIX_INTERCONNECT, USD 1,345.50 per month below PUBLIC_CLOUD_INTERNET.

### C. Resilience

Analysed portfolio: `DC+FR+SG+TR`.

- N-1 FAIL: N-1 fails: 6 of all 38 scenarios cause an outage.
- N-2 FAIL: N-2 fails: 212 of all 639 scenarios cause an outage and 42 degrade service.

Single points of failure:

- loss of metro FR: workloads down: plant-erp; required clouds lost: azure
- loss of metro SG: workloads down: inference; required clouds lost: gcp
- loss of the Cloud Router in FR: workloads down: plant-erp; required clouds lost: azure
- loss of the Cloud Router in SG: workloads down: inference; required clouds lost: gcp
- loss of the gcp on-ramp in SG: workloads down: inference; required clouds lost: gcp
- loss of the connection gcp@SG: workloads down: inference; required clouds lost: gcp

Correlated risks:

- workload 'order-api': primary TR and standby DC are both in AMER (MEDIUM)
- workload 'analytics': primary TR and standby DC are both in AMER (MEDIUM)
- workload 'disaster-recovery': primary TR and standby DC are both in AMER (MEDIUM)
- every connection to aws (DC, TR) is in AMER (MEDIUM)
- site 'Ashburn data center': both access circuits land in AMER (MEDIUM)
- site 'Chicago headquarters': both access circuits land in AMER (MEDIUM)
- site 'Dallas operations': both access circuits land in AMER (MEDIUM)

Estimated availability. These are computed from planning assumptions, not SLAs: SDK planning assumptions (round figures chosen 2026-11-12 for comparison only; NOT Equinix or cloud-provider SLAs: Equinix publishes SLAs per product, replace these with your contractual figures).

| Workload | Availability | Downtime per year | Composition |
|---|---:|---:|---|
| order-api | 99.9898% | 54 min | parallel([metro TR x router TR x conn aws@TR x on-ramp], [metro DC x router DC x conn aws@DC x on-ramp]) x cloud region(s) [AWS us-east-1] x site access 0.999999 |
| analytics | 99.9794% | 108 min | parallel([metro TR x router TR x conn aws@TR x on-ramp x conn azure@TR x on-ramp], [metro DC x router DC x conn aws@DC x on-ramp x conn azure@DC x on-ramp]) x cloud region(s) [AWS us-east-1, AZURE westeurope] x site access 0.999999 |
| inference | 99.8700% | 684 min | [metro SG x router SG x conn gcp@SG x on-ramp] x cloud region(s) [GOOGLE_CLOUD asia-southeast1] x site access 0.999999 |
| plant-erp | 99.8700% | 684 min | [metro FR x router FR x conn azure@FR x on-ramp] x cloud region(s) [AZURE westeurope] x site access 0.999999 |
| disaster-recovery | 99.9794% | 108 min | parallel([metro TR x router TR x conn aws@TR x on-ramp x conn azure@TR x on-ramp], [metro DC x router DC x conn aws@DC x on-ramp x conn azure@DC x on-ramp]) x cloud region(s) [AWS us-east-1, AZURE westeurope] x site access 0.999999 |

Per-element availability assumed: BACKBONE_LINK 0.99990, CLOUD_CONNECTION 0.99950, CLOUD_ONRAMP 0.99950, CLOUD_REGION 0.99990, CLOUD_ROUTER 0.99990, METRO 0.99990, SITE_ACCESS 0.99900.

Remediation:

- Add a standby gcp connection in DC sized for 500 Mbps (DC already reaches gcp; re-simulated: the scenario no longer hurts)
- Add a metro to fail over to (no candidate among the nearest 5 removed loss of metro FR when re-simulated) (the design has no metro to fail over to)
- Add a metro to fail over to (no candidate among the nearest 5 removed loss of metro SG when re-simulated) (the design has no metro to fail over to)
- Add a metro to fail over to (no candidate among the nearest 5 removed loss of the entire Equinix AMER region when re-simulated) (every metro of the design is in AMER)
- Add a metro to fail over to (no candidate among the nearest 5 removed loss of the entire Equinix APAC region when re-simulated) (every metro of the design is in APAC)
- Add a metro to fail over to (no candidate among the nearest 5 removed loss of the entire Equinix EMEA region when re-simulated) (every metro of the design is in EMEA)
- Add a second Cloud Router in FR and duplicate its connections on it, or give the affected workloads a standby metro (the router carries every cloud and inter-metro path of FR (not re-simulated: a twin router changes the design shape))
- Add a second Cloud Router in SG and duplicate its connections on it, or give the affected workloads a standby metro (the router carries every cloud and inter-metro path of SG (not re-simulated: a twin router changes the design shape))
- Add a second region of the affected cloud as another cloud in the spec and connect to it: outage of cloud region AWS us-east-1 (clouds aws) is outside anything the Equinix design can protect (a cloud-provider region outage takes every on-ramp to it)
- Add a second region of the affected cloud as another cloud in the spec and connect to it: outage of cloud region AZURE westeurope (clouds azure) is outside anything the Equinix design can protect (a cloud-provider region outage takes every on-ramp to it)
- Add a second region of the affected cloud as another cloud in the spec and connect to it: outage of cloud region GOOGLE_CLOUD asia-southeast1 (clouds gcp) is outside anything the Equinix design can protect (a cloud-provider region outage takes every on-ramp to it)
- Give workload 'inference' a standby metro (it has none: pinned, or no second metro reaches all its clouds) (it cannot fail over)
- Give workload 'plant-erp' a standby metro (it has none: pinned, or no second metro reaches all its clouds) (it cannot fail over)

```mermaid
flowchart LR
    n0(["Ashburn data center"])
    n1(["Chicago headquarters"])
    n2(["Dallas operations"])
    n3(["Frankfurt plant"])
    n4(["London office"])
    n5(["Singapore hub"])
    n20(("aws (AWS us-east-1)"))
    n21(("azure (AZURE westeurope)"))
    n22(("gcp (GOOGLE_CLOUD asia-southeast1)"))
    n23[["analytics"]]
    n24[["disaster-recovery"]]
    n25[["inference"]]
    n26[["order-api"]]
    n27[["plant-erp"]]
    subgraph m_DC["DC"]
        n6["DC Ashburn"]
        n10{{"Cloud Router DC"}}
        n14[/"aws on-ramp DC"/]
        n16[/"azure on-ramp DC"/]
    end
    subgraph m_FR["FR"]
        n7["FR Frankfurt"]
        n11{{"Cloud Router FR"}}
        n17[/"azure on-ramp FR"/]
    end
    subgraph m_SG["SG"]
        n8["SG Singapore"]
        n12{{"Cloud Router SG"}}
        n19[/"gcp on-ramp SG"/]
    end
    subgraph m_TR["TR"]
        n9["TR Toronto"]
        n13{{"Cloud Router TR"}}
        n15[/"aws on-ramp TR"/]
        n18[/"azure on-ramp TR"/]
    end
    n0 ---|"0.5 ms"| n6
    n0 -.-|"17.3 ms / standby"| n9
    n1 -.-|"16.8 ms / standby"| n6
    n1 ---|"10.2 ms"| n9
    n2 -.-|"37.7 ms / standby"| n6
    n2 ---|"32.1 ms"| n9
    n3 -.-|"85.7 ms / standby"| n6
    n3 ---|"0.5 ms"| n7
    n4 -.-|"73.5 ms / standby"| n6
    n4 ---|"10.7 ms"| n7
    n5 -.-|"154.0 ms / standby"| n7
    n5 ---|"0.5 ms"| n8
    n6 --- n10
    n7 --- n11
    n8 --- n12
    n9 --- n13
    n10 ---|"85.7 ms"| n11
    n10 ---|"220.0 ms"| n12
    n10 ---|"17.3 ms"| n13
    n11 ---|"154.0 ms"| n12
    n11 ---|"88.8 ms"| n13
    n12 ---|"210.0 ms"| n13
    n10 -.-|"4000 Mbps / standby"| n14
    n13 ---|"4000 Mbps"| n15
    n10 -.-|"3000 Mbps / standby"| n16
    n11 ---|"500 Mbps"| n17
    n13 ---|"3000 Mbps"| n18
    n12 ---|"500 Mbps"| n19
    n14 --- n20
    n15 --- n20
    n16 --- n21
    n17 --- n21
    n18 --- n21
    n19 --- n22
    n23 -.-|"2000 Mbps / standby"| n6
    n23 ---|"2000 Mbps"| n9
    n24 -.-|"1000 Mbps / standby"| n6
    n24 ---|"1000 Mbps"| n9
    n25 ---|"500 Mbps"| n8
    n26 -.-|"1000 Mbps / standby"| n6
    n26 ---|"1000 Mbps"| n9
    n27 ---|"500 Mbps"| n7
```

### D. Risks and findings

| Severity | Source | Category | Finding | Action |
|---|---|---|---|---|
| HIGH | resilience | N1_FAIL | N-1 fails: 6 of all 38 scenarios cause an outage. | Apply the remediations in the resilience section |
| HIGH | resilience | SINGLE_POINT_OF_FAILURE | loss of metro FR: workloads down: plant-erp; required clouds lost: azure | - |
| HIGH | resilience | SINGLE_POINT_OF_FAILURE | loss of metro SG: workloads down: inference; required clouds lost: gcp | - |
| HIGH | resilience | SINGLE_POINT_OF_FAILURE | loss of the Cloud Router in FR: workloads down: plant-erp; required clouds lost: azure | - |
| HIGH | resilience | SINGLE_POINT_OF_FAILURE | loss of the Cloud Router in SG: workloads down: inference; required clouds lost: gcp | - |
| HIGH | resilience | SINGLE_POINT_OF_FAILURE | loss of the connection gcp@SG: workloads down: inference; required clouds lost: gcp | - |
| HIGH | resilience | SINGLE_POINT_OF_FAILURE | loss of the gcp on-ramp in SG: workloads down: inference; required clouds lost: gcp | - |
| MEDIUM | resilience | CORRELATED_RISK | every connection to aws (DC, TR) is in AMER | - |
| MEDIUM | resilience | CORRELATED_RISK | site 'Ashburn data center': both access circuits land in AMER | - |
| MEDIUM | resilience | CORRELATED_RISK | site 'Chicago headquarters': both access circuits land in AMER | - |
| MEDIUM | resilience | CORRELATED_RISK | site 'Dallas operations': both access circuits land in AMER | - |
| MEDIUM | resilience | CORRELATED_RISK | workload 'analytics': primary TR and standby DC are both in AMER | - |
| MEDIUM | resilience | CORRELATED_RISK | workload 'disaster-recovery': primary TR and standby DC are both in AMER | - |
| MEDIUM | resilience | CORRELATED_RISK | workload 'order-api': primary TR and standby DC are both in AMER | - |
| LOW | resilience | N2_FAIL | N-2 fails: 212 of all 639 scenarios cause an outage and 42 degrade service. | - |
| INFO | optimizer | COST_CURRENCY_CONVERTED | 7 of 19 line(s) converted to USD: CAD at ECB reference rate via Frankfurter, 2026-11-16; EUR at ECB reference rate via Frankfurter, 2026-11-16; SGD at ECB reference rate via Frankfurter, 2026-11-16. Not a risk: pin the rates (OptimizationResult.specWithUsedRatesPinned()) to reproduce the run | - |
| INFO | optimizer | NATIVE_MULTICLOUD_ALTERNATIVE | Workload 'analytics' depends on Amazon Web Services and Microsoft Azure. The multicloud environment catalog (as of 2026-09-21) lists 4 environment(s) for the pair, none matched to a region in this spec. A native provider-to-provider link carries traffic between the two clouds without an Equinix metro. Informational: not a risk | Plan with the Deployment Wizard's default CloudToCloudStrategy.COMPARE to price both paths |
| INFO | optimizer | NATIVE_MULTICLOUD_ALTERNATIVE | Workload 'disaster-recovery' depends on Amazon Web Services and Microsoft Azure. The multicloud environment catalog (as of 2026-09-21) lists 4 environment(s) for the pair, none matched to a region in this spec. A native provider-to-provider link carries traffic between the two clouds without an Equinix metro. Informational: not a risk | Plan with the Deployment Wizard's default CloudToCloudStrategy.COMPARE to price both paths |

### E. Assumptions and provenance

Site to metro round trips of the recommendation: 21 measured by Fabric, 3 estimated, 0 unknown.

Unmeasured round trips are the fibre floor x 1.73 (overall, fitted from 2,140 measured pairs, interquartile range 1.52 to 2.01).

As of:

- Fabric catalogue (metros, latencies, service profiles) read 2026-11-16T17:06:14.485904Z
- Exchange rate EUR to USD: ECB reference rate via Frankfurter, 2026-11-16, as of 2026-11-16
- Exchange rate SGD to USD: ECB reference rate via Frankfurter, 2026-11-16, as of 2026-11-16
- Exchange rate CAD to USD: ECB reference rate via Frankfurter, 2026-11-16, as of 2026-11-16
- Reference rate card as of 2026-11
- Resilience analysis facts gathered 2026-11-16T17:06:48.484612Z
- Record generated 2026-11-16T17:07:59.291341Z

Data notes:

- The Fabric metro catalogue was read to its last page (67 items).
- The Fabric service profile catalogue was read to its last page (436 items).

Assumptions:

- (optimizer) All latencies are round trip. A site in the metro it names is assumed 0.5 ms from it; measured pairs use Fabric's connectedMetros.avgLatency; others are estimated: Unmeasured latencies are estimated as 1.73 x the straight-fibre round-trip floor (median of 2140 Fabric-measured metro pairs, interquartile 1.52-2.01; 5 short or uncoordinated pair(s) left out); per region pair: AMER/AMER 1.81 (374 pairs), AMER/APAC 1.48 (420 pairs), AMER/EMEA 1.63 (644 pairs), APAC/APAC 1.95 (104 pairs), APAC/EMEA 2.00 (345 pairs), EMEA/EMEA 2.01 (253 pairs).
- (optimizer) Each workload runs in one metro; in a design of two or more metros its cloud connections are duplicated at full size in a standby metro (active/standby).
- (optimizer) Cloud connections are Fabric virtual connections from a Cloud Router in each metro that carries one; customer ports, colocation space and power, and site access circuits are not priced.
- (optimizer) Data sent into a cloud is not charged by the providers; only data leaving a cloud is priced.
- (optimizer) A metro that carries no workload and no cloud connection is not a design option (every_metro_used).
- (resilience) One Fabric Cloud Router per metro carries cloud and inter-metro traffic (as the Deployment Wizard plans it); a router is a single element unless the design adds a second.
- (resilience) Inter-metro backbone capacity is not provisioned by the spec: it is left unknown and does not constrain the simulation.
- (resilience) A cloud connection is sized as its primary workloads' bandwidth plus 100% of its standby workloads' bandwidth.
- (resilience) Each site has access circuits into its 2 nearest portfolio metro(s) by round trip (the spec does not state site access); access capacity is unknown.
- (resilience) All latencies are round-trip ms: Fabric measurements where published, calibrated estimates otherwise; an unknown latency is never held against a path.
- (resilience) Failures are of whole elements; partial degradation (packet loss, brown-outs) is not modelled.
- (resilience) N-1 counts single-element failures (metro, Cloud Router, on-ramp, connection); cloud-region and Equinix-region losses are shared-fate events, reported and ranked but not counted in N-1.
- (resilience) Availability figures compose assumed per-element availabilities assuming independent failures (SDK planning assumptions (round figures chosen 2026-11-12 for comparison only; NOT Equinix or cloud-provider SLAs: Equinix publishes SLAs per product, replace these with your contractual figures)).
- (resilience) Availability figures: SDK planning assumptions (round figures chosen 2026-11-12 for comparison only; NOT Equinix or cloud-provider SLAs: Equinix publishes SLAs per product, replace these with your contractual figures).
- (growth) Month n carries today's traffic x (1 + 0.3)^((n - 1)/12); capacity is the next Fabric tier at or above the projected Mbps.
- (growth) A term renews at the same term to the horizon; an upgrade within a term is billed at that term's price for the new tier; months committed beyond the horizon are counted at the last month's price.
- (growth) Provider hosted-connection fees and egress have no term axis and are priced on demand.
- (growth) One-time charges fall in month 1; scenario totals are priced on demand (1-month prices).
- (value) 4000 Mbps of workloads to 'aws' rounded up to the 5000 Mbps Fabric tier.
- (value) The portfolio DC+FR+SG+TR has several metros; the connection is priced in DC.

### F. Implementation and reproduction

| Plan | Value |
|---|---|
| Plan id | `0178573252529411` |
| Deployment name | global-interconnect |
| Steps | 58 (58 executable) |
| Cloud Routers / connections / backbone links | 4 / 12 / 6 |
| Monthly estimate, Equinix items | USD 10,797.34 (composite) |
| One-time estimate | USD 0.00 |
| Validation | failed: Cloud Router 'FCR-TR' (TR) has no notification email: Fabric requires at least one notification recipient to create a Cloud Router. Supply deployment.notifications.; Cloud Router 'FCR-FR' (FR) has no notification email: Fabric requires at least one notification recipient to create a Cloud Router. Supply deployment.notifications.; Cloud Router 'FCR-SG' (SG) has no notification email: Fabric requires at least one notification recipient to create a Cloud Router. Supply deployment.notifications.; Cloud Router 'FCR-DC' (DC) has no notification email: Fabric requires at least one notification recipient to create a Cloud Router. Supply deployment.notifications. |

Built by the recorder from the recommendation, priced with Layered[EquinixRateCard > ReferenceRateCard], without deployment inputs (project, account, notification addresses) and without live validation; build it with DeploymentWizard and run dryRun() before executing.

To reproduce: run spec fingerprint `a5f92102787ee30f40ab911fee6c2caf9ba279c5ead65986a92ba8d9b2a9a7da` with SDK development build (no jar manifest version), with these exchange rates pinned in `commercial.exchangeRates`: CAD 0.700035, EUR 1.10139437, GBP 1.31983581, SGD 0.77591558 (as of 2026-11-16). The pinned spec has fingerprint `b059d25b74433e27dd0150a31c016218980decfddf051bfa3a9871da1c202ae3` (`DesignDecisionRecord.reproducibleSpec()`).
