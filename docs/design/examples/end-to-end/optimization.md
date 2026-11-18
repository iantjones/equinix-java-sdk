# Metro Optimization Report

_Spec `global-interconnect`, fingerprint `a5f92102787e`; generated 2026-11-18T00:50:20.959712Z_

## Summary

Recommended design: Ashburn (DC) + Frankfurt (FR) + Singapore (SG) - 3 metro(s), weighted score 89.4/100.  
  latency: site-weighted round trip 16.0 ms; worst case Dallas operations 37.7 ms via DC (measured)  
  cost: USD 15943.34/month (range 13586.31-18300.41) over 19 line(s)  
  provider_coverage: 100% of requested clouds reachable  
  resilience: 82% survives the worst single-metro loss (losing SG leaves 100% of site weight served, 2/3 required clouds connected, 4/5 workloads running); worst region loss (APAC) leaves 82%  
  compliance: no compliance or sovereignty zone stated  
  carbon: Beta: 390 gCO2e/kWh, load-weighted over [DC, SG, FR] (Ember (2026), Yearly Electricity Data, with processing by Our World in Data (carbon-intensity-electricity), CC BY 4.0, retrieved 2026-11-12 (bundled reference, Beta), 2024 to 2025 annual averages (latest year published per country), location-based: national annual average carbon intensity of electricity generation)  
Frontier: 5 Pareto-optimal option(s). Searched 814385 portfolio(s) of 3-4 metros from 67 candidates (15 feasible) in 19472 ms, exhaustively.  
Unmeasured latencies are estimated as 1.73 x the straight-fibre round-trip floor (median of 2140 Fabric-measured metro pairs, interquartile 1.52-2.01; 5 short or uncoordinated pair(s) left out); per region pair: AMER/AMER 1.81 (374 pairs), AMER/APAC 1.48 (420 pairs), AMER/EMEA 1.63 (644 pairs), APAC/APAC 1.95 (104 pairs), APAC/EMEA 2.00 (345 pairs), EMEA/EMEA 2.01 (253 pairs).  
Computed in 23501 ms.

## Recommendation and why

**Ashburn (DC) + Frankfurt (FR) + Singapore (SG)** has the highest weighted score among the feasible designs and no other design is at least as good on every objective.

- **latency**: site-weighted round trip 16.0 ms; worst case Dallas operations 37.7 ms via DC (measured) (score 92.0)
- **cost**: USD 15943.34/month (range 13586.31-18300.41) over 19 line(s) (score 100.0)
- **provider_coverage**: 100% of requested clouds reachable (score 100.0)
- **resilience**: 82% survives the worst single-metro loss (losing SG leaves 100% of site weight served, 2/3 required clouds connected, 4/5 workloads running); worst region loss (APAC) leaves 82% (score 82.2)
- **compliance**: no compliance or sovereignty zone stated (score 100.0)
- **carbon**: Beta: 390 gCO2e/kWh, load-weighted over [DC, SG, FR] (Ember (2026), Yearly Electricity Data, with processing by Our World in Data (carbon-intensity-electricity), CC BY 4.0, retrieved 2026-11-12 (bundled reference, Beta), 2024 to 2025 annual averages (latest year published per country), location-based: national annual average carbon intensity of electricity generation) (score 44.3)

## Trade-off frontier

| Design | Weighted | latency | cost | provider_coverage | resilience | compliance | carbon |
|---|---:|---:|---:|---:|---:|---:|---:|
| **DC+FR+SG** (recommended) | 89.4 | 16.0 | 15943.3 | 100.0 | 82.2 | 100.0 | 390.2 |
| CH+DC+FR+SG | 87.6 | 5.4 | 19197.9 | 100.0 | 82.2 | 100.0 | 390.2 |
| DC+FR+SG+TR | 86.7 | 11.9 | 19111.6 | 100.0 | 82.2 | 100.0 | 390.2 |
| AM+DC+FR+SG | 86.3 | 14.9 | 18972.9 | 100.0 | 82.2 | 100.0 | 390.2 |
| DC+FR+MA+SG | 86.2 | 14.8 | 19087.8 | 100.0 | 82.2 | 100.0 | 390.2 |

## Sensitivity

- **latency** weight 30%: the recommendation holds from 0% to 48%; above, CH+DC+FR+SG takes over.
- **cost** weight 20%: the recommendation holds from 10% to 100%; below, CH+DC+FR+SG takes over.
- **provider_coverage** weight 20%: the recommendation holds from 0% to 100%.
- **resilience** weight 15%: the recommendation holds from 0% to 100%.
- **compliance** weight 5%: the recommendation holds from 0% to 100%.
- **carbon** weight 10%: the recommendation holds from 0% to 100%.
- Nearest alternative on the frontier: **AM+DC+FR+SG** (latency -1.1, cost +3029.5, provider_coverage +0.0, resilience +0.0, compliance +0.0, carbon +0.0 vs the recommendation).

## Constraints

| Constraint | Verdict | Binding |
|---|---|---|
| required_metros | met: includes every required metro [DC, FR] | binding: without it FR+SG+TR would be recommended (140 portfolio(s) rejected by it alone) |
| required_regions | met: has a metro in every required region [AMER, EMEA, APAC] | rejects no portfolio on its own |
| minimum_redundancy | met: MULTI_REGION met: 3 metros in [AMER, EMEA, APAC] | rejects no portfolio on its own |
| metro_count | met: 3 metros within 3..4 | rejects no portfolio on its own |
| pinned_workloads | met: pinned workloads placed: inference@SG, plant-erp@FR | rejects no portfolio on its own |
| workload_max_latency | met: 1 workload latency limit(s) met | rejects no portfolio on its own |
| required_clouds | met: every required cloud is reachable where it is needed | rejects no portfolio on its own |
| budget | met: USD 15943.34/month within budget 60000 | rejects no portfolio on its own |
| every_metro_used | met: every metro carries a workload or a cloud connection | binding: without it DC+FR+HO+SG would be recommended (50 portfolio(s) rejected by it alone) |
| min_survival | met: 80% survives the worst single failure (minimum 40%) | rejects no portfolio on its own |

## Workload placement

- **order-api**: runs in DC (site-weighted round trip 49.2 ms), reaching aws; standby in FR (FR reaches aws through another region than us-east-1; traffic to us-east-1 continues over the provider's network, which is not modelled or priced)
- **analytics**: runs in DC (site-weighted round trip 49.2 ms), reaching aws, azure; standby in FR (FR reaches aws through another region than us-east-1; traffic to us-east-1 continues over the provider's network, which is not modelled or priced)
- **inference**: pinned to SG (site-weighted round trip 177.2 ms), reaching gcp
- **plant-erp**: pinned to FR (site-weighted round trip 78.2 ms), reaching azure
- **disaster-recovery**: runs in DC (site-weighted round trip 49.2 ms), reaching aws, azure; standby in FR (FR reaches aws through another region than us-east-1; traffic to us-east-1 continues over the provider's network, which is not modelled or priced)

## Cost

| Item | Metro | Monthly | Range | Source |
|---|---|---:|---|---|
| Fabric IP_VC 5000 Mbps DC to aws | DC | USD 375.0 | 375.0-375.0 | EQUINIX_LIVE |
| Provider hosted-connection fee aws 5000 Mbps in DC | DC | USD 1204.50 | 903.38-1505.63 | REFERENCE |
| Fabric IP_VC 5000 Mbps DC to azure | DC | USD 375.0 | 375.0-375.0 | EQUINIX_LIVE |
| Provider hosted-connection fee azure 5000 Mbps in DC | DC | USD 2180 | 1635.00-2725.00 | REFERENCE |
| Fabric IP_VC 5000 Mbps FR to aws | FR | USD 351.65 | 344.62-358.68 | EQUINIX_LIVE |
| Provider hosted-connection fee aws 5000 Mbps in FR | FR | USD 1204.50 | 903.38-1505.63 | REFERENCE |
| Fabric IP_VC 5000 Mbps FR to azure | FR | USD 351.65 | 344.62-358.68 | EQUINIX_LIVE |
| Provider hosted-connection fee azure 5000 Mbps in FR | FR | USD 2180 | 1635.00-2725.00 | REFERENCE |
| Fabric IP_VC 500 Mbps SG to gcp | SG | USD 151.30 | 148.28-154.33 | EQUINIX_LIVE |
| Provider hosted-connection fee gcp 500 Mbps in SG | SG | USD 126.73 | 95.05-158.41 | REFERENCE |
| Fabric Cloud Router STANDARD in DC | DC | USD 1275.0 | 1275.0-1275.0 | EQUINIX_LIVE |
| Fabric Cloud Router STANDARD in FR | FR | USD 1195.60 | 1171.69-1219.52 | EQUINIX_LIVE |
| Fabric Cloud Router STANDARD in SG | SG | USD 1276.38 | 1250.85-1301.91 | EQUINIX_LIVE |
| Fabric backbone IP_VC 10000 Mbps DC to FR | DC | USD 500.0 | 500.0-500.0 | EQUINIX_LIVE |
| Fabric backbone IP_VC 10000 Mbps DC to SG | DC | USD 500.0 | 500.0-500.0 | EQUINIX_LIVE |
| Fabric backbone IP_VC 10000 Mbps FR to SG | FR | USD 467.03 | 457.69-476.37 | EQUINIX_LIVE |
| Data out of aws, 60000 GB/month over the private path | - | USD 1200.00 | 900.00-1500.00 | REFERENCE |
| Data out of azure, 30000 GB/month over the private path | - | USD 750.00 | 562.50-937.50 | REFERENCE |
| Data out of gcp, 15000 GB/month over the private path | - | USD 279.00 | 209.25-348.75 | REFERENCE |
| **Total** | | **USD 15943.34** | 13586.31-18300.41 | |

## Latency (round trip)

```
Metro | Ashburn data center | Chicago headquarters | Dallas operations | London office | Frankfurt plant | Singapore hub
------+---------------------+----------------------+-------------------+---------------+-----------------+--------------
DC    |                0.5* |                 16.8 |              37.7 |          73.5 |            85.7 |         220.0
FR    |                85.7 |                100.0 |             123.0 |          10.7 |            0.5* |         154.0
SG    |               220.0 |                202.0 |             195.0 |         163.0 |           154.0 |          0.5*

Round-trip ms. * = estimated from distance (no measured Fabric latency); ? = unknown (no coordinates)
```

## Findings

- **[HIGH] PLACEMENT_GAP**: 'order-api': FR reaches aws through another region than us-east-1; traffic to us-east-1 continues over the provider's network, which is not modelled or priced
- **[HIGH] PLACEMENT_GAP**: 'analytics': FR reaches aws through another region than us-east-1; traffic to us-east-1 continues over the provider's network, which is not modelled or priced
- **[HIGH] PLACEMENT_GAP**: 'disaster-recovery': FR reaches aws through another region than us-east-1; traffic to us-east-1 continues over the provider's network, which is not modelled or priced
- **[INFO] NATIVE_MULTICLOUD_ALTERNATIVE**: Workload 'analytics' depends on Amazon Web Services and Microsoft Azure. The multicloud environment catalog (as of 2026-09-21) lists 4 environment(s) for the pair, none matched to a region in this spec. A native provider-to-provider link carries traffic between the two clouds without an Equinix metro. Informational: not a risk _Plan with the Deployment Wizard's default CloudToCloudStrategy.COMPARE to price both paths_
- **[INFO] NATIVE_MULTICLOUD_ALTERNATIVE**: Workload 'disaster-recovery' depends on Amazon Web Services and Microsoft Azure. The multicloud environment catalog (as of 2026-09-21) lists 4 environment(s) for the pair, none matched to a region in this spec. A native provider-to-provider link carries traffic between the two clouds without an Equinix metro. Informational: not a risk _Plan with the Deployment Wizard's default CloudToCloudStrategy.COMPARE to price both paths_
- **[INFO] COST_CURRENCY_CONVERTED**: 6 of 19 line(s) converted to USD: EUR at ECB reference rate via Frankfurter, 2026-11-17; SGD at ECB reference rate via Frankfurter, 2026-11-17. Not a risk: pin the rates (OptimizationResult.specWithUsedRatesPinned()) to reproduce the run
- **[MEDIUM] CLOUD_REGION_NOT_LOCAL**: FR reaches aws through another region than us-east-1; traffic to us-east-1 continues over the provider's network, which is not modelled or priced (workload 'order-api'). _Add a metro whose on-ramp serves the named region, drop the region from the spec to accept any on-ramp, or plan the provider-side path (for AWS, a Direct Connect gateway) yourself_

## Method and assumptions

Every set of 3 to 4 metros the constraints admit was evaluated on latency, cost, provider coverage, resilience and compliance (exhaustive search). Each objective is scored 0-100 on a fixed scale (cost by ratio to the cheapest design, latency against the latency ceiling or 200 ms, percentages as they are) and combined with the spec's weights; the Pareto frontier lists every design no other beats on all objectives. Hard limits (including any max-latency bound) reject designs outright; a limit that could not be evaluated is reported as NOT applied.

- All latencies are round trip. A site in the metro it names is assumed 0.5 ms from it; measured pairs use Fabric's connectedMetros.avgLatency; others are estimated: Unmeasured latencies are estimated as 1.73 x the straight-fibre round-trip floor (median of 2140 Fabric-measured metro pairs, interquartile 1.52-2.01; 5 short or uncoordinated pair(s) left out); per region pair: AMER/AMER 1.81 (374 pairs), AMER/APAC 1.48 (420 pairs), AMER/EMEA 1.63 (644 pairs), APAC/APAC 1.95 (104 pairs), APAC/EMEA 2.00 (345 pairs), EMEA/EMEA 2.01 (253 pairs).
- Each workload runs in one metro; in a design of two or more metros its cloud connections are duplicated at full size in a standby metro (active/standby).
- Cloud connections are Fabric virtual connections from a Cloud Router in each metro that carries one; customer ports, colocation space and power, and site access circuits are not priced.
- Data sent into a cloud is not charged by the providers; only data leaving a cloud is priced.
- A metro that carries no workload and no cloud connection is not a design option (every_metro_used).

## Data provenance

- Facts gathered 2026-11-18T00:49:57.477464Z
- The Fabric metro catalogue was read to its last page (67 items).
- The Fabric service profile catalogue was read to its last page (436 items).
- Cloud 'aws' names region us-east-1: 41 metro(s) offer the provider through other regions only (AM, BA, BG, CH, CL, CN, DB, DX, FR, HE, HK, IL, JK, JN, KL, LA, LD, LS, MA, MB, MD, ME, ML, MT, MU, MX, OS, PA, PE, RJ, SE, SG, SK, SL, SP, SV, SY, TR, TY, WA, ZH). Workloads are placed in a metro that serves the named region when one is in the design; a connection planned elsewhere states the region it uses.
- Cloud 'gcp' names region asia-southeast1: 47 metro(s) offer the provider through other regions only (AM, AT, BA, BG, CA, CH, CL, CN, DA, DC, DX, FR, GV, HE, HK, JK, JN, LA, LD, LS, MA, MB, MD, ME, MI, ML, MT, MU, MX, NY, OS, PA, PE, PH, RJ, SE, SK, SL, SO, SP, ST, SV, SY, TR, TY, WA, ZH). Workloads are placed in a metro that serves the named region when one is in the design; a connection planned elsewhere states the region it uses.
- Unmeasured latencies are estimated as 1.73 x the straight-fibre round-trip floor (median of 2140 Fabric-measured metro pairs, interquartile 1.52-2.01; 5 short or uncoordinated pair(s) left out); per region pair: AMER/AMER 1.81 (374 pairs), AMER/APAC 1.48 (420 pairs), AMER/EMEA 1.63 (644 pairs), APAC/APAC 1.95 (104 pairs), APAC/EMEA 2.00 (345 pairs), EMEA/EMEA 2.01 (253 pairs).
