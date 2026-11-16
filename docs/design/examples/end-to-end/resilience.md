# Resilience report: global-interconnect on DC+FR+SG+TR

Spec fingerprint `a5f92102787ee30f40ab911fee6c2caf9ba279c5ead65986a92ba8d9b2a9a7da`; portfolio from the given optimisation result's recommendation; facts as of 2026-11-16T17:06:48.484612Z.

- **N-1:** N-1 fails: 6 of all 38 scenarios cause an outage.
- **N-2:** N-2 fails: 212 of all 639 scenarios cause an outage and 42 degrade service.
- **Single points of failure:** 6
- **Correlated risks:** 7

## Baseline

Service index 100%; sized cloud demand 8000 Mbps.

| Workload | Status | Runs in | Sites | Clouds |
|---|---|---|---|---|
| order-api | full | TR | Ashburn data center: Ashburn data center > TR (17.3 ms); Chicago headquarters: Chicago headquarters > TR (10.2 ms); Dallas operations: Dallas operations > TR (32.1 ms); Frankfurt plant: Frankfurt plant > FR > TR (89.3 ms); London office: London office > DC > TR (90.8 ms); Singapore hub: Singapore hub > SG > TR (210.5 ms) | aws: via conn:aws@TR: TR > on-ramp TR > aws (0.0 ms to the on-ramp) |
| analytics | full | TR | Ashburn data center: Ashburn data center > TR (17.3 ms); Chicago headquarters: Chicago headquarters > TR (10.2 ms); Dallas operations: Dallas operations > TR (32.1 ms); Frankfurt plant: Frankfurt plant > FR > TR (89.3 ms); London office: London office > DC > TR (90.8 ms); Singapore hub: Singapore hub > SG > TR (210.5 ms) | aws: via conn:aws@TR: TR > on-ramp TR > aws (0.0 ms to the on-ramp); azure: via conn:azure@TR: TR > on-ramp TR > azure (0.0 ms to the on-ramp) |
| inference | full | SG | Ashburn data center: Ashburn data center > DC > SG (220.5 ms); Chicago headquarters: Chicago headquarters > TR > SG (220.2 ms); Dallas operations: Dallas operations > TR > SG (242.1 ms); Frankfurt plant: Frankfurt plant > FR > SG (154.5 ms); London office: London office > FR > SG (164.7 ms); Singapore hub: Singapore hub > SG (0.5 ms) | gcp: via conn:gcp@SG: SG > on-ramp SG > gcp (0.0 ms to the on-ramp) |
| plant-erp | full | FR | Ashburn data center: Ashburn data center > DC > FR (86.2 ms); Chicago headquarters: Chicago headquarters > TR > FR (99.0 ms); Dallas operations: Dallas operations > TR > FR (120.9 ms); Frankfurt plant: Frankfurt plant > FR (0.5 ms); London office: London office > FR (10.7 ms); Singapore hub: Singapore hub > FR (154.0 ms) | azure: via conn:azure@FR: FR > on-ramp FR > azure (0.0 ms to the on-ramp) |
| disaster-recovery | full | TR | Ashburn data center: Ashburn data center > TR (17.3 ms); Chicago headquarters: Chicago headquarters > TR (10.2 ms); Dallas operations: Dallas operations > TR (32.1 ms); Frankfurt plant: Frankfurt plant > FR > TR (89.3 ms); London office: London office > DC > TR (90.8 ms); Singapore hub: Singapore hub > SG > TR (210.5 ms) | aws: via conn:aws@TR: TR > on-ramp TR > aws (0.0 ms to the on-ramp); azure: via conn:azure@TR: TR > on-ramp TR > azure (0.0 ms to the on-ramp) |

## Single failures

| Scenario | Verdict | Survival | Site weight affected | Down | Required clouds lost | Capacity served | Shortfall | Breaks |
|---|---|---|---|---|---|---|---|---|
| loss of metro DC | survives | 100% | 0% | - | - | 100% of 8000 Mbps | - | 0 |
| loss of metro FR | outage | 80% | 100% | plant-erp | azure | 94% of 8000 Mbps | 500 Mbps | 0 |
| loss of metro SG | outage | 80% | 100% | inference | gcp | 94% of 8000 Mbps | 500 Mbps | 0 |
| loss of metro TR | survives | 100% | 0% | - | - | 100% of 8000 Mbps | - | 0 |
| loss of the Cloud Router in DC | survives | 100% | 0% | - | - | 100% of 8000 Mbps | - | 0 |
| loss of the Cloud Router in FR | outage | 80% | 64% | plant-erp | azure | 94% of 8000 Mbps | 500 Mbps | 0 |
| loss of the Cloud Router in SG | outage | 80% | 94% | inference | gcp | 94% of 8000 Mbps | 500 Mbps | 0 |
| loss of the Cloud Router in TR | survives | 100% | 0% | - | - | 100% of 8000 Mbps | - | 0 |
| loss of the aws on-ramp in DC | unaffected | 100% | 0% | - | - | 100% of 8000 Mbps | - | 0 |
| loss of the aws on-ramp in TR | survives | 100% | 0% | - | - | 100% of 8000 Mbps | - | 0 |
| loss of the azure on-ramp in DC | unaffected | 100% | 0% | - | - | 100% of 8000 Mbps | - | 0 |
| loss of the azure on-ramp in FR | survives | 100% | 0% | - | - | 100% of 8000 Mbps | - | 0 |
| loss of the azure on-ramp in TR | survives | 100% | 0% | - | - | 100% of 8000 Mbps | - | 0 |
| loss of the gcp on-ramp in SG | outage | 80% | 0% | inference | gcp | 94% of 8000 Mbps | 500 Mbps | 0 |
| loss of the standby connection aws@DC | unaffected | 100% | 0% | - | - | 100% of 8000 Mbps | - | 0 |
| loss of the connection aws@TR | survives | 100% | 0% | - | - | 100% of 8000 Mbps | - | 0 |
| loss of the standby connection azure@DC | unaffected | 100% | 0% | - | - | 100% of 8000 Mbps | - | 0 |
| loss of the connection azure@FR | survives | 100% | 0% | - | - | 100% of 8000 Mbps | - | 0 |
| loss of the connection azure@TR | survives | 100% | 0% | - | - | 100% of 8000 Mbps | - | 0 |
| loss of the connection gcp@SG | outage | 80% | 0% | inference | gcp | 94% of 8000 Mbps | 500 Mbps | 0 |
| loss of the access circuit Ashburn data center@DC | survives | 100% | 0% | - | - | 100% of 8000 Mbps | - | 0 |
| loss of the access circuit Ashburn data center@TR | survives | 100% | 0% | - | - | 100% of 8000 Mbps | - | 0 |
| loss of the access circuit Chicago headquarters@DC | unaffected | 100% | 0% | - | - | 100% of 8000 Mbps | - | 0 |
| loss of the access circuit Chicago headquarters@TR | survives | 100% | 0% | - | - | 100% of 8000 Mbps | - | 0 |
| loss of the access circuit Dallas operations@DC | unaffected | 100% | 0% | - | - | 100% of 8000 Mbps | - | 0 |
| loss of the access circuit Dallas operations@TR | survives | 100% | 0% | - | - | 100% of 8000 Mbps | - | 0 |
| loss of the access circuit Frankfurt plant@DC | unaffected | 100% | 0% | - | - | 100% of 8000 Mbps | - | 0 |
| loss of the access circuit Frankfurt plant@FR | survives | 100% | 0% | - | - | 100% of 8000 Mbps | - | 0 |
| loss of the access circuit London office@DC | survives | 100% | 0% | - | - | 100% of 8000 Mbps | - | 0 |
| loss of the access circuit London office@FR | survives | 100% | 0% | - | - | 100% of 8000 Mbps | - | 0 |
| loss of the access circuit Singapore hub@FR | survives | 100% | 0% | - | - | 100% of 8000 Mbps | - | 0 |
| loss of the access circuit Singapore hub@SG | survives | 100% | 0% | - | - | 100% of 8000 Mbps | - | 0 |
| loss of the backbone link DC-FR | survives | 100% | 0% | - | - | 100% of 8000 Mbps | - | 0 |
| loss of the backbone link DC-SG | survives | 100% | 0% | - | - | 100% of 8000 Mbps | - | 0 |
| loss of the backbone link DC-TR | survives | 100% | 0% | - | - | 100% of 8000 Mbps | - | 0 |
| loss of the backbone link FR-SG | survives | 100% | 0% | - | - | 100% of 8000 Mbps | - | 0 |
| loss of the backbone link FR-TR | survives | 100% | 0% | - | - | 100% of 8000 Mbps | - | 0 |
| loss of the backbone link SG-TR | survives | 100% | 0% | - | - | 100% of 8000 Mbps | - | 0 |
| outage of cloud region AWS us-east-1 (clouds aws) | outage | 40% | 0% | order-api | aws | 50% of 8000 Mbps | 4000 Mbps | 0 |
| outage of cloud region AZURE westeurope (clouds azure) | outage | 40% | 0% | plant-erp | azure | 56% of 8000 Mbps | 3500 Mbps | 0 |
| outage of cloud region GOOGLE_CLOUD asia-southeast1 (clouds gcp) | outage | 80% | 0% | inference | gcp | 94% of 8000 Mbps | 500 Mbps | 0 |
| loss of the entire Equinix AMER region | outage | 14% | 100% | order-api, analytics, disaster-recovery | aws, azure | 13% of 8000 Mbps | 7000 Mbps | 0 |
| loss of the entire Equinix EMEA region | outage | 80% | 100% | plant-erp | azure | 94% of 8000 Mbps | 500 Mbps | 0 |
| loss of the entire Equinix APAC region | outage | 80% | 100% | inference | gcp | 94% of 8000 Mbps | 500 Mbps | 0 |

## Blast radius (worst first)

| # | Scenario | Verdict | Service lost | Site weight affected | Shortfall | Down |
|---|---|---|---|---|---|---|
| 1 | loss of the entire Equinix AMER region | outage | 86% | 100% | 7000 Mbps | order-api, analytics, disaster-recovery |
| 2 | outage of cloud region AWS us-east-1 (clouds aws) | outage | 60% | 0% | 4000 Mbps | order-api |
| 3 | outage of cloud region AZURE westeurope (clouds azure) | outage | 60% | 0% | 3500 Mbps | plant-erp |
| 4 | loss of the entire Equinix APAC region | outage | 20% | 100% | 500 Mbps | inference |
| 5 | loss of the entire Equinix EMEA region | outage | 20% | 100% | 500 Mbps | plant-erp |
| 6 | loss of metro FR | outage | 20% | 100% | 500 Mbps | plant-erp |
| 7 | loss of metro SG | outage | 20% | 100% | 500 Mbps | inference |
| 8 | loss of the Cloud Router in SG | outage | 20% | 94% | 500 Mbps | inference |
| 9 | loss of the Cloud Router in FR | outage | 20% | 64% | 500 Mbps | plant-erp |
| 10 | outage of cloud region GOOGLE_CLOUD asia-southeast1 (clouds gcp) | outage | 20% | 0% | 500 Mbps | inference |
| 11 | loss of the connection gcp@SG | outage | 20% | 0% | 500 Mbps | inference |
| 12 | loss of the gcp on-ramp in SG | outage | 20% | 0% | 500 Mbps | inference |
| 13 | loss of the access circuit Ashburn data center@DC | survives | 0% | 0% | - | - |
| 14 | loss of the access circuit Ashburn data center@TR | survives | 0% | 0% | - | - |
| 15 | loss of the access circuit Chicago headquarters@TR | survives | 0% | 0% | - | - |

## Single points of failure

- **loss of metro FR** (`metro:FR`): workloads down: plant-erp; required clouds lost: azure
- **loss of metro SG** (`metro:SG`): workloads down: inference; required clouds lost: gcp
- **loss of the Cloud Router in FR** (`router:FR`): workloads down: plant-erp; required clouds lost: azure
- **loss of the Cloud Router in SG** (`router:SG`): workloads down: inference; required clouds lost: gcp
- **loss of the gcp on-ramp in SG** (`onramp:gcp@SG`): workloads down: inference; required clouds lost: gcp
- **loss of the connection gcp@SG** (`conn:gcp@SG`): workloads down: inference; required clouds lost: gcp

## Correlated risks

- medium: workload 'order-api': primary TR and standby DC are both in AMER (scenario `equinix-region:AMER`)
- medium: workload 'analytics': primary TR and standby DC are both in AMER (scenario `equinix-region:AMER`)
- medium: workload 'disaster-recovery': primary TR and standby DC are both in AMER (scenario `equinix-region:AMER`)
- medium: every connection to aws (DC, TR) is in AMER (scenario `equinix-region:AMER`)
- medium: site 'Ashburn data center': both access circuits land in AMER (scenario `equinix-region:AMER`)
- medium: site 'Chicago headquarters': both access circuits land in AMER (scenario `equinix-region:AMER`)
- medium: site 'Dallas operations': both access circuits land in AMER (scenario `equinix-region:AMER`)

## Pairs (N-2)

N-2 fails: 212 of all 639 scenarios cause an outage and 42 degrade service.

| # | Scenario | Verdict | Service lost | Site weight affected | Shortfall | Down |
|---|---|---|---|---|---|---|
| 1 | loss of metro DC AND loss of metro TR | outage | 86% | 100% | 7000 Mbps | order-api, analytics, disaster-recovery |
| 2 | loss of metro DC AND loss of the Cloud Router in TR | outage | 86% | 100% | 7000 Mbps | order-api, analytics, disaster-recovery |
| 3 | loss of metro TR AND loss of the Cloud Router in DC | outage | 86% | 70% | 7000 Mbps | order-api, analytics, disaster-recovery |
| 4 | loss of the Cloud Router in DC AND loss of the Cloud Router in TR | outage | 86% | 70% | 7000 Mbps | order-api, analytics, disaster-recovery |
| 5 | loss of the standby connection aws@DC AND loss of the connection aws@TR | outage | 60% | 0% | 4000 Mbps | order-api |
| 6 | loss of metro DC AND loss of the connection aws@TR | outage | 60% | 0% | 4000 Mbps | order-api |
| 7 | loss of metro DC AND loss of the aws on-ramp in TR | outage | 60% | 0% | 4000 Mbps | order-api |
| 8 | loss of metro TR AND loss of the standby connection aws@DC | outage | 60% | 0% | 4000 Mbps | order-api |
| 9 | loss of metro TR AND loss of the aws on-ramp in DC | outage | 60% | 0% | 4000 Mbps | order-api |
| 10 | loss of the aws on-ramp in DC AND loss of the connection aws@TR | outage | 60% | 0% | 4000 Mbps | order-api |

## Availability estimate

Estimates under the assumptions below, not guarantees: Equinix publishes SLAs per product and the contractual figures are the ones that apply.

| Workload | Core | End to end | Downtime / year | Composition |
|---|---|---|---|---|
| order-api | 99.98986% | 99.98976% | 54 min | parallel([metro TR x router TR x conn aws@TR x on-ramp], [metro DC x router DC x conn aws@DC x on-ramp]) x cloud region(s) [AWS us-east-1] x site access 0.999999 |
| analytics | 99.97952% | 99.97942% | 108 min | parallel([metro TR x router TR x conn aws@TR x on-ramp x conn azure@TR x on-ramp], [metro DC x router DC x conn aws@DC x on-ramp x conn azure@DC x on-ramp]) x cloud region(s) [AWS us-east-1, AZURE westeurope] x site access 0.999999 |
| inference | 99.87006% | 99.86996% | 684 min | [metro SG x router SG x conn gcp@SG x on-ramp] x cloud region(s) [GOOGLE_CLOUD asia-southeast1] x site access 0.999999 |
| plant-erp | 99.87006% | 99.86996% | 684 min | [metro FR x router FR x conn azure@FR x on-ramp] x cloud region(s) [AZURE westeurope] x site access 0.999999 |
| disaster-recovery | 99.97952% | 99.97942% | 108 min | parallel([metro TR x router TR x conn aws@TR x on-ramp x conn azure@TR x on-ramp], [metro DC x router DC x conn aws@DC x on-ramp x conn azure@DC x on-ramp]) x cloud region(s) [AWS us-east-1, AZURE westeurope] x site access 0.999999 |

Assumed availability per element (SDK planning assumptions (round figures chosen 2026-11-12 for comparison only; NOT Equinix or cloud-provider SLAs: Equinix publishes SLAs per product, replace these with your contractual figures)):

- metro: 99.9900%
- cloud router: 99.9900%
- cloud connection: 99.9500%
- cloud onramp: 99.9500%
- backbone link: 99.9900%
- site access: 99.9000%
- cloud region: 99.9900%

## Remediation

- Add a standby gcp connection in DC sized for 500 Mbps **(re-simulated: removes `onramp:gcp@SG`, `conn:gcp@SG`)**: DC already reaches gcp; re-simulated: the scenario no longer hurts
- Add a metro to fail over to (no candidate among the nearest 5 removed loss of metro FR when re-simulated) (addresses `metro:FR`): the design has no metro to fail over to
- Add a metro to fail over to (no candidate among the nearest 5 removed loss of metro SG when re-simulated) (addresses `metro:SG`): the design has no metro to fail over to
- Add a metro to fail over to (no candidate among the nearest 5 removed loss of the entire Equinix AMER region when re-simulated) (addresses `equinix-region:AMER`): every metro of the design is in AMER
- Add a metro to fail over to (no candidate among the nearest 5 removed loss of the entire Equinix APAC region when re-simulated) (addresses `equinix-region:APAC`): every metro of the design is in APAC
- Add a metro to fail over to (no candidate among the nearest 5 removed loss of the entire Equinix EMEA region when re-simulated) (addresses `equinix-region:EMEA`): every metro of the design is in EMEA
- Add a second Cloud Router in FR and duplicate its connections on it, or give the affected workloads a standby metro (addresses `router:FR`): the router carries every cloud and inter-metro path of FR (not re-simulated: a twin router changes the design shape)
- Add a second Cloud Router in SG and duplicate its connections on it, or give the affected workloads a standby metro (addresses `router:SG`): the router carries every cloud and inter-metro path of SG (not re-simulated: a twin router changes the design shape)
- Add a second region of the affected cloud as another cloud in the spec and connect to it: outage of cloud region AWS us-east-1 (clouds aws) is outside anything the Equinix design can protect (addresses `cloud-region:AWS/us-east-1`): a cloud-provider region outage takes every on-ramp to it
- Add a second region of the affected cloud as another cloud in the spec and connect to it: outage of cloud region AZURE westeurope (clouds azure) is outside anything the Equinix design can protect (addresses `cloud-region:AZURE/westeurope`): a cloud-provider region outage takes every on-ramp to it
- Add a second region of the affected cloud as another cloud in the spec and connect to it: outage of cloud region GOOGLE_CLOUD asia-southeast1 (clouds gcp) is outside anything the Equinix design can protect (addresses `cloud-region:GOOGLE_CLOUD/asia-southeast1`): a cloud-provider region outage takes every on-ramp to it
- Give workload 'inference' a standby metro (it has none: pinned, or no second metro reaches all its clouds) (addresses `metro:SG`): it cannot fail over
- Give workload 'plant-erp' a standby metro (it has none: pinned, or no second metro reaches all its clouds) (addresses `metro:FR`): it cannot fail over

## Design graph

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

| Edge | Kind | From | To | Role | Capacity | Round trip |
|---|---|---|---|---|---|---|
| access:Ashburn data center@DC | site_access | Ashburn data center | DC Ashburn | primary | unknown | 0.5 ms (intra-metro) |
| access:Ashburn data center@TR | site_access | Ashburn data center | TR Toronto | standby | unknown | 17.3 ms (measured) |
| access:Chicago headquarters@DC | site_access | Chicago headquarters | DC Ashburn | standby | unknown | 16.8 ms (measured) |
| access:Chicago headquarters@TR | site_access | Chicago headquarters | TR Toronto | primary | unknown | 10.2 ms (measured) |
| access:Dallas operations@DC | site_access | Dallas operations | DC Ashburn | standby | unknown | 37.7 ms (measured) |
| access:Dallas operations@TR | site_access | Dallas operations | TR Toronto | primary | unknown | 32.1 ms (measured) |
| access:Frankfurt plant@DC | site_access | Frankfurt plant | DC Ashburn | standby | unknown | 85.7 ms (measured) |
| access:Frankfurt plant@FR | site_access | Frankfurt plant | FR Frankfurt | primary | unknown | 0.5 ms (intra-metro) |
| access:London office@DC | site_access | London office | DC Ashburn | standby | unknown | 73.5 ms (measured) |
| access:London office@FR | site_access | London office | FR Frankfurt | primary | unknown | 10.7 ms (measured) |
| access:Singapore hub@FR | site_access | Singapore hub | FR Frankfurt | standby | unknown | 154.0 ms (measured) |
| access:Singapore hub@SG | site_access | Singapore hub | SG Singapore | primary | unknown | 0.5 ms (intra-metro) |
| attach:DC | router_attachment | DC Ashburn | Cloud Router DC |  | unknown | 0.0 ms (assumed) |
| attach:FR | router_attachment | FR Frankfurt | Cloud Router FR |  | unknown | 0.0 ms (assumed) |
| attach:SG | router_attachment | SG Singapore | Cloud Router SG |  | unknown | 0.0 ms (assumed) |
| attach:TR | router_attachment | TR Toronto | Cloud Router TR |  | unknown | 0.0 ms (assumed) |
| backbone:DC-FR | backbone | Cloud Router DC | Cloud Router FR |  | unknown | 85.7 ms (measured) |
| backbone:DC-SG | backbone | Cloud Router DC | Cloud Router SG |  | unknown | 220.0 ms (measured) |
| backbone:DC-TR | backbone | Cloud Router DC | Cloud Router TR |  | unknown | 17.3 ms (measured) |
| backbone:FR-SG | backbone | Cloud Router FR | Cloud Router SG |  | unknown | 154.0 ms (measured) |
| backbone:FR-TR | backbone | Cloud Router FR | Cloud Router TR |  | unknown | 88.8 ms (measured) |
| backbone:SG-TR | backbone | Cloud Router SG | Cloud Router TR |  | unknown | 210.0 ms (measured) |
| conn:aws@DC | cloud_connection | Cloud Router DC | aws on-ramp DC | standby | 4000 Mbps | unknown |
| conn:aws@TR | cloud_connection | Cloud Router TR | aws on-ramp TR | primary | 4000 Mbps | unknown |
| conn:azure@DC | cloud_connection | Cloud Router DC | azure on-ramp DC | standby | 3000 Mbps | unknown |
| conn:azure@FR | cloud_connection | Cloud Router FR | azure on-ramp FR | primary | 500 Mbps | unknown |
| conn:azure@TR | cloud_connection | Cloud Router TR | azure on-ramp TR | primary | 3000 Mbps | unknown |
| conn:gcp@SG | cloud_connection | Cloud Router SG | gcp on-ramp SG | primary | 500 Mbps | unknown |
| provider:aws@DC | provider_link | aws on-ramp DC | aws (AWS us-east-1) |  | unknown | unknown |
| provider:aws@TR | provider_link | aws on-ramp TR | aws (AWS us-east-1) |  | unknown | unknown |
| provider:azure@DC | provider_link | azure on-ramp DC | azure (AZURE westeurope) |  | unknown | unknown |
| provider:azure@FR | provider_link | azure on-ramp FR | azure (AZURE westeurope) |  | unknown | unknown |
| provider:azure@TR | provider_link | azure on-ramp TR | azure (AZURE westeurope) |  | unknown | unknown |
| provider:gcp@SG | provider_link | gcp on-ramp SG | gcp (GOOGLE_CLOUD asia-southeast1) |  | unknown | unknown |
| host:analytics@DC | workload_hosting | analytics | DC Ashburn | standby | 2000 Mbps | unknown |
| host:analytics@TR | workload_hosting | analytics | TR Toronto | primary | 2000 Mbps | unknown |
| host:disaster-recovery@DC | workload_hosting | disaster-recovery | DC Ashburn | standby | 1000 Mbps | unknown |
| host:disaster-recovery@TR | workload_hosting | disaster-recovery | TR Toronto | primary | 1000 Mbps | unknown |
| host:inference@SG | workload_hosting | inference | SG Singapore | primary | 500 Mbps | unknown |
| host:order-api@DC | workload_hosting | order-api | DC Ashburn | standby | 1000 Mbps | unknown |
| host:order-api@TR | workload_hosting | order-api | TR Toronto | primary | 1000 Mbps | unknown |
| host:plant-erp@FR | workload_hosting | plant-erp | FR Frankfurt | primary | 500 Mbps | unknown |

## Assumptions

- One Fabric Cloud Router per metro carries cloud and inter-metro traffic (as the Deployment Wizard plans it); a router is a single element unless the design adds a second.
- Inter-metro backbone capacity is not provisioned by the spec: it is left unknown and does not constrain the simulation.
- A cloud connection is sized as its primary workloads' bandwidth plus 100% of its standby workloads' bandwidth.
- Each site has access circuits into its 2 nearest portfolio metro(s) by round trip (the spec does not state site access); access capacity is unknown.
- All latencies are round-trip ms: Fabric measurements where published, calibrated estimates otherwise; an unknown latency is never held against a path.
- Failures are of whole elements; partial degradation (packet loss, brown-outs) is not modelled.
- N-1 counts single-element failures (metro, Cloud Router, on-ramp, connection); cloud-region and Equinix-region losses are shared-fate events, reported and ranked but not counted in N-1.
- Availability figures compose assumed per-element availabilities assuming independent failures (SDK planning assumptions (round figures chosen 2026-11-12 for comparison only; NOT Equinix or cloud-provider SLAs: Equinix publishes SLAs per product, replace these with your contractual figures)).

## Timings

- catalogue load: 7355 ms
- graph: 2 ms
- single failures (44): 12 ms
- pairs (639): 118 ms
- remediation: 19 ms
- availability: 1 ms
