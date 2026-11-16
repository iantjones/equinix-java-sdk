# Deployment Plan

Plan id `c502b0d151391174`, deployment `global-interconnect`.
Derived from design spec `a5f92102787ee30f40ab911fee6c2caf9ba279c5ead65986a92ba8d9b2a9a7da`.
_Generated from optimization computed at 2026-11-16T17:06:48.479596Z_

**Total Resources:** 58

> **VALIDATION ERRORS** (validated now - structural + router dry-run)
> - Router dry-run validation warning for 'FCR-TR': Error returned by Equinix API (HTTP 400). EQ-3040032: Invalid account number (field: account.accountNumber)
Status Code: 400
URI: https://api.equinix.com/fabric/v4/routers?dryRun=true
Correlation Id: 7684c49a-1832-413a-8981-d18fd8691ab5
  Details: Invalid account number
  Property: account.accountNumber | Reason: invalid project account number
> - Router dry-run validation warning for 'FCR-FR': Error returned by Equinix API (HTTP 400). EQ-3040032: Invalid account number (field: account.accountNumber)
Status Code: 400
URI: https://api.equinix.com/fabric/v4/routers?dryRun=true
Correlation Id: 46324653-1c43-4125-aa56-4f0db7dc082a
  Details: Invalid account number
  Property: account.accountNumber | Reason: invalid project account number
> - Router dry-run validation warning for 'FCR-SG': Error returned by Equinix API (HTTP 400). EQ-3040032: Invalid account number (field: account.accountNumber)
Status Code: 400
URI: https://api.equinix.com/fabric/v4/routers?dryRun=true
Correlation Id: c87511d7-3c87-4c59-8799-08d4acbe9888
  Details: Invalid account number
  Property: account.accountNumber | Reason: invalid project account number
> - Router dry-run validation warning for 'FCR-DC': Error returned by Equinix API (HTTP 400). EQ-3040032: Invalid account number (field: account.accountNumber)
Status Code: 400
URI: https://api.equinix.com/fabric/v4/routers?dryRun=true
Correlation Id: 3df1f6c7-8b2d-44fc-b964-0d6ff0dc8652
  Details: Invalid account number
  Property: account.accountNumber | Reason: invalid project account number

> **DEFERRED TO PROVISIONING** - the live connection endpoint dry-run runs when the plan is applied (needs your authorization keys)
> - Connection 'FCR-TR-to-aws' (aws): endpoints validated structurally; the live endpoint dry-run runs at provisioning once Cloud Router 'FCR-TR' exists - you will need to supply AWS Account ID (12-digit), a VLAN tag
> - Connection 'FCR-TR-to-azure' (azure): endpoints validated structurally; the live endpoint dry-run runs at provisioning once Cloud Router 'FCR-TR' exists - you will need to supply Azure ExpressRoute service key (GUID), a VLAN tag, an Azure peering type
> - Connection 'FCR-TR-to-gcp' (gcp): endpoints validated structurally; the live endpoint dry-run runs at provisioning once Cloud Router 'FCR-TR' exists - you will need to supply GCP pairing key, a VLAN tag
> - Connection 'FCR-FR-to-aws' (aws): endpoints validated structurally; the live endpoint dry-run runs at provisioning once Cloud Router 'FCR-FR' exists - you will need to supply AWS Account ID (12-digit), a VLAN tag
> - Connection 'FCR-FR-to-azure' (azure): endpoints validated structurally; the live endpoint dry-run runs at provisioning once Cloud Router 'FCR-FR' exists - you will need to supply Azure ExpressRoute service key (GUID), a VLAN tag, an Azure peering type
> - Connection 'FCR-FR-to-gcp' (gcp): endpoints validated structurally; the live endpoint dry-run runs at provisioning once Cloud Router 'FCR-FR' exists - you will need to supply GCP pairing key, a VLAN tag
> - Connection 'FCR-SG-to-aws' (aws): endpoints validated structurally; the live endpoint dry-run runs at provisioning once Cloud Router 'FCR-SG' exists - you will need to supply AWS Account ID (12-digit), a VLAN tag
> - Connection 'FCR-SG-to-azure' (azure): endpoints validated structurally; the live endpoint dry-run runs at provisioning once Cloud Router 'FCR-SG' exists - you will need to supply Azure ExpressRoute service key (GUID), a VLAN tag, an Azure peering type
> - Connection 'FCR-SG-to-gcp' (gcp): endpoints validated structurally; the live endpoint dry-run runs at provisioning once Cloud Router 'FCR-SG' exists - you will need to supply GCP pairing key, a VLAN tag
> - Connection 'FCR-DC-to-aws' (aws): endpoints validated structurally; the live endpoint dry-run runs at provisioning once Cloud Router 'FCR-DC' exists - you will need to supply AWS Account ID (12-digit), a VLAN tag
> - Connection 'FCR-DC-to-azure' (azure): endpoints validated structurally; the live endpoint dry-run runs at provisioning once Cloud Router 'FCR-DC' exists - you will need to supply Azure ExpressRoute service key (GUID), a VLAN tag, an Azure peering type
> - Connection 'FCR-DC-to-gcp' (gcp): endpoints validated structurally; the live endpoint dry-run runs at provisioning once Cloud Router 'FCR-DC' exists - you will need to supply GCP pairing key, a VLAN tag

> **BANDWIDTH ROUNDED UP** - a requirement with no exact service-profile tier was rounded up to the smallest tier that satisfies it (this raises the billed bandwidth)
> - FCR-TR-to-aws: 4000 -> 5000 Mbps (+1000 Mbps billable)
> - FCR-DC-to-aws: 4000 -> 5000 Mbps (+1000 Mbps billable)

> **PROFILE CHOICE** - more than one service profile can carry these connections; the tightest-fitting default is used, and the alternatives are available to choose
> - FCR-TR-to-gcp: default b97dacca-abe6-4e8c-8be1-fcdd1be1defe; options: b97dacca-abe6-4e8c-8be1-fcdd1be1defe @ 1000 Mbps, bd4570e2-d792-4a00-87f5-3bde040cdcd7 @ 1000 Mbps
> - FCR-FR-to-aws: default de55ca94-df3b-4680-845f-6f7093f76bef; options: de55ca94-df3b-4680-845f-6f7093f76bef @ 1000 Mbps, ed48616a-c182-40b4-a462-a78ba812e961 @ 1000 Mbps
> - FCR-FR-to-gcp: default 6aa4ceca-e532-4b61-a932-b8a5868712fc; options: 6aa4ceca-e532-4b61-a932-b8a5868712fc @ 1000 Mbps, 76013c6d-b9c4-4762-9708-324875b399b3 @ 1000 Mbps, b97dacca-abe6-4e8c-8be1-fcdd1be1defe @ 1000 Mbps, bd4570e2-d792-4a00-87f5-3bde040cdcd7 @ 1000 Mbps
> - FCR-SG-to-aws: default de55ca94-df3b-4680-845f-6f7093f76bef; options: de55ca94-df3b-4680-845f-6f7093f76bef @ 1000 Mbps, ed48616a-c182-40b4-a462-a78ba812e961 @ 1000 Mbps
> - FCR-SG-to-gcp: default b97dacca-abe6-4e8c-8be1-fcdd1be1defe; options: b97dacca-abe6-4e8c-8be1-fcdd1be1defe @ 500 Mbps, bd4570e2-d792-4a00-87f5-3bde040cdcd7 @ 500 Mbps
> - FCR-DC-to-azure: default 0de4e413-edd7-4325-912f-7c8a4428e156; options: 0de4e413-edd7-4325-912f-7c8a4428e156 @ 3000 Mbps, a1390b22-bbe0-4e93-ad37-85beef9d254d @ 3000 Mbps
> - FCR-DC-to-gcp: default b97dacca-abe6-4e8c-8be1-fcdd1be1defe; options: b97dacca-abe6-4e8c-8be1-fcdd1be1defe @ 1000 Mbps, bd4570e2-d792-4a00-87f5-3bde040cdcd7 @ 1000 Mbps

## Required Customer Inputs (before provisioning)

| Connection | Provider | Authorization key | VLAN | Azure peering |
|------------|----------|-------------------|------|---------------|
| FCR-TR-to-aws | aws | AWS Account ID (12-digit) | required | - |
| FCR-TR-to-azure | azure | Azure ExpressRoute service key (GUID) | required | required |
| FCR-TR-to-gcp | gcp | GCP pairing key | required | - |
| FCR-FR-to-aws | aws | AWS Account ID (12-digit) | required | - |
| FCR-FR-to-azure | azure | Azure ExpressRoute service key (GUID) | required | required |
| FCR-FR-to-gcp | gcp | GCP pairing key | required | - |
| FCR-SG-to-aws | aws | AWS Account ID (12-digit) | required | - |
| FCR-SG-to-azure | azure | Azure ExpressRoute service key (GUID) | required | required |
| FCR-SG-to-gcp | gcp | GCP pairing key | required | - |
| FCR-DC-to-aws | aws | AWS Account ID (12-digit) | required | - |
| FCR-DC-to-azure | azure | Azure ExpressRoute service key (GUID) | required | required |
| FCR-DC-to-gcp | gcp | GCP pairing key | required | - |

## Cloud Routers

| Name | Metro | Package |
|------|-------|---------|
| FCR-TR | TR | STANDARD |
| FCR-FR | FR | STANDARD |
| FCR-SG | SG | STANDARD |
| FCR-DC | DC | STANDARD |

## Provider Connections

| Name | Type | A-Side (Metro) | Z-Side (Provider) | Bandwidth | Workload Breakdown |
|------|------|----------------|-------------------|-----------|--------------------|
| FCR-TR-to-aws | IP_VC | FCR-TR (TR) | aws [ca-central-1] | 5000 Mbps (rounded up from 4000) | order-api: 1000 Mbps, analytics: 2000 Mbps, disaster-recovery: 1000 Mbps |
| FCR-TR-to-azure | IP_VC | FCR-TR (TR) | azure | 3000 Mbps | analytics: 2000 Mbps, disaster-recovery: 1000 Mbps |
| FCR-TR-to-gcp | IP_VC | FCR-TR (TR) | gcp [northamerica-northeast2] | 1000 Mbps | default: 1000 Mbps |
| FCR-FR-to-aws | IP_VC | FCR-FR (FR) | aws [eu-central-1] | 1000 Mbps | default: 1000 Mbps |
| FCR-FR-to-azure | IP_VC | FCR-FR (FR) | azure | 500 Mbps | plant-erp: 500 Mbps |
| FCR-FR-to-gcp | IP_VC | FCR-FR (FR) | gcp [europe-west3] | 1000 Mbps | default: 1000 Mbps |
| FCR-SG-to-aws | IP_VC | FCR-SG (SG) | aws [ap-southeast-1] | 1000 Mbps | default: 1000 Mbps |
| FCR-SG-to-azure | IP_VC | FCR-SG (SG) | azure | 1000 Mbps | default: 1000 Mbps |
| FCR-SG-to-gcp | IP_VC | FCR-SG (SG) | gcp [asia-southeast1] | 500 Mbps | inference: 500 Mbps |
| FCR-DC-to-aws | IP_VC | FCR-DC (DC) | aws [us-east-1] | 5000 Mbps (rounded up from 4000) | order-api: 1000 Mbps, analytics: 2000 Mbps, disaster-recovery: 1000 Mbps |
| FCR-DC-to-azure | IP_VC | FCR-DC (DC) | azure | 3000 Mbps | analytics: 2000 Mbps, disaster-recovery: 1000 Mbps |
| FCR-DC-to-gcp | IP_VC | FCR-DC (DC) | gcp [us-east4] | 1000 Mbps | default: 1000 Mbps |

## Backbone Links

| Name | Metro A | Metro Z | Bandwidth | Topology |
|------|---------|---------|-----------|----------|
| FCR-TR-to-FR | TR | FR | 10000 Mbps | FULL_MESH |
| FCR-TR-to-SG | TR | SG | 10000 Mbps | FULL_MESH |
| FCR-TR-to-DC | TR | DC | 10000 Mbps | FULL_MESH |
| FCR-FR-to-SG | FR | SG | 10000 Mbps | FULL_MESH |
| FCR-FR-to-DC | FR | DC | 10000 Mbps | FULL_MESH |
| FCR-SG-to-DC | SG | DC | 10000 Mbps | FULL_MESH |

## Routing Protocols

| Connection | Type | Customer Peer | Equinix Peer | ASN | BFD |
|------------|------|---------------|--------------|-----|-----|
| FCR-TR-to-aws | DIRECT | - | 10.100.0.1/30 | - | off |
| FCR-TR-to-aws | BGP | 10.100.0.2/30 | 10.100.0.1/30 | 64512 | 300ms |
| FCR-TR-to-azure | DIRECT | - | 10.100.1.1/30 | - | off |
| FCR-TR-to-azure | BGP | 10.100.1.2/30 | 10.100.1.1/30 | 64512 | 300ms |
| FCR-TR-to-gcp | DIRECT | - | 10.100.2.1/30 | - | off |
| FCR-TR-to-gcp | BGP | 10.100.2.2/30 | 10.100.2.1/30 | 64512 | 300ms |
| FCR-FR-to-aws | DIRECT | - | 10.100.3.1/30 | - | off |
| FCR-FR-to-aws | BGP | 10.100.3.2/30 | 10.100.3.1/30 | 64512 | 300ms |
| FCR-FR-to-azure | DIRECT | - | 10.100.4.1/30 | - | off |
| FCR-FR-to-azure | BGP | 10.100.4.2/30 | 10.100.4.1/30 | 64512 | 300ms |
| FCR-FR-to-gcp | DIRECT | - | 10.100.5.1/30 | - | off |
| FCR-FR-to-gcp | BGP | 10.100.5.2/30 | 10.100.5.1/30 | 64512 | 300ms |
| FCR-SG-to-aws | DIRECT | - | 10.100.6.1/30 | - | off |
| FCR-SG-to-aws | BGP | 10.100.6.2/30 | 10.100.6.1/30 | 64512 | 300ms |
| FCR-SG-to-azure | DIRECT | - | 10.100.7.1/30 | - | off |
| FCR-SG-to-azure | BGP | 10.100.7.2/30 | 10.100.7.1/30 | 64512 | 300ms |
| FCR-SG-to-gcp | DIRECT | - | 10.100.8.1/30 | - | off |
| FCR-SG-to-gcp | BGP | 10.100.8.2/30 | 10.100.8.1/30 | 64512 | 300ms |
| FCR-DC-to-aws | DIRECT | - | 10.100.9.1/30 | - | off |
| FCR-DC-to-aws | BGP | 10.100.9.2/30 | 10.100.9.1/30 | 64512 | 300ms |
| FCR-DC-to-azure | DIRECT | - | 10.100.10.1/30 | - | off |
| FCR-DC-to-azure | BGP | 10.100.10.2/30 | 10.100.10.1/30 | 64512 | 300ms |
| FCR-DC-to-gcp | DIRECT | - | 10.100.11.1/30 | - | off |
| FCR-DC-to-gcp | BGP | 10.100.11.2/30 | 10.100.11.1/30 | 64512 | 300ms |
| FCR-TR-to-FR | DIRECT | - | 10.100.12.1/30 | - | off |
| FCR-TR-to-FR | BGP | 10.100.12.2/30 | 10.100.12.1/30 | 64512 | 300ms |
| FCR-TR-to-SG | DIRECT | - | 10.100.13.1/30 | - | off |
| FCR-TR-to-SG | BGP | 10.100.13.2/30 | 10.100.13.1/30 | 64512 | 300ms |
| FCR-TR-to-DC | DIRECT | - | 10.100.14.1/30 | - | off |
| FCR-TR-to-DC | BGP | 10.100.14.2/30 | 10.100.14.1/30 | 64512 | 300ms |
| FCR-FR-to-SG | DIRECT | - | 10.100.15.1/30 | - | off |
| FCR-FR-to-SG | BGP | 10.100.15.2/30 | 10.100.15.1/30 | 64512 | 300ms |
| FCR-FR-to-DC | DIRECT | - | 10.100.16.1/30 | - | off |
| FCR-FR-to-DC | BGP | 10.100.16.2/30 | 10.100.16.1/30 | 64512 | 300ms |
| FCR-SG-to-DC | DIRECT | - | 10.100.17.1/30 | - | off |
| FCR-SG-to-DC | BGP | 10.100.17.2/30 | 10.100.17.1/30 | 64512 | 300ms |

## Cost Estimate

| Category | Monthly Cost |
|----------|-------------:|
| Cloud Routers | $4993.66 |
| Provider Connections | $2907.47 |
| Backbone Links | $2896.21 |
| **Total Monthly** | **$10797.34** |
| Setup (one-time) | $0.00 |

_Some or all figures are heuristic or reference estimates (live Fabric pricing was unavailable for part of this plan). Actual costs may vary; contact your Equinix account team for precise quotes. 17 of 22 line(s) converted to USD: CAD at ECB reference rate via Frankfurter, 2026-11-16; EUR at ECB reference rate via Frankfurter, 2026-11-16; SGD at ECB reference rate via Frankfurter, 2026-11-16; each converted line keeps its original amount, the rate and its date._

## Execution

Resources will be created in this order:

1. Cloud Routers (4)
2. Provider Connections (12)
3. Backbone Links (6)
4. Routing Protocols (36)

Call `plan.dryRun()` to validate, `plan.diff()` to compare with live state, `plan.reconcile(...)` to create only what is missing (resumable from a checkpoint), or `plan.execute(...)` to provision everything in one run.
