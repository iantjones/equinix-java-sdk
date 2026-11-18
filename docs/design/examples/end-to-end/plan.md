# Deployment Plan

Plan id `ac8860df082b481c`, deployment `global-interconnect`.
Derived from design spec `a5f92102787ee30f40ab911fee6c2caf9ba279c5ead65986a92ba8d9b2a9a7da`.
_Generated from optimization computed at 2026-11-18T00:50:20.959712Z_

**Total Resources:** 27

> **DEFERRED TO PROVISIONING** - the live connection endpoint dry-run runs when the plan is applied (needs your authorization keys)
> - Connection 'FCR-DC-to-aws' (aws): endpoints validated structurally; the live endpoint dry-run runs at provisioning once Cloud Router 'FCR-DC' exists - you will need to supply AWS Account ID (12-digit), a VLAN tag
> - Connection 'FCR-DC-to-azure' (azure): endpoints validated structurally; the live endpoint dry-run runs at provisioning once Cloud Router 'FCR-DC' exists - you will need to supply Azure ExpressRoute service key (GUID), a VLAN tag, an Azure peering type
> - Connection 'FCR-FR-to-aws' (aws): endpoints validated structurally; the live endpoint dry-run runs at provisioning once Cloud Router 'FCR-FR' exists - you will need to supply AWS Account ID (12-digit), a VLAN tag
> - Connection 'FCR-FR-to-azure' (azure): endpoints validated structurally; the live endpoint dry-run runs at provisioning once Cloud Router 'FCR-FR' exists - you will need to supply Azure ExpressRoute service key (GUID), a VLAN tag, an Azure peering type
> - Connection 'FCR-SG-to-gcp' (gcp): endpoints validated structurally; the live endpoint dry-run runs at provisioning once Cloud Router 'FCR-SG' exists - you will need to supply GCP pairing key, a VLAN tag
> - Access connection 'FCR-DC-access-primary': not validated, because DC-port-primary is an order to place and has no uuid yet
> - Access connection 'FCR-DC-access-secondary': not validated, because DC-port-secondary is an order to place and has no uuid yet
> - Access connection 'FCR-FR-access-primary': not validated, because FR-ne-primary is an order to place and has no uuid yet
> - Access connection 'FCR-FR-access-secondary': not validated, because FR-ne-secondary is an order to place and has no uuid yet
> - Access connection 'FCR-SG-access-primary': not validated, because SG-ne-primary is an order to place and has no uuid yet
> - Access connection 'FCR-SG-access-secondary': not validated, because SG-ne-secondary is an order to place and has no uuid yet

> **SKIPPED (could not validate now)** - these checks could not be attempted; each reason says why. They do not invalidate the plan.
> - Router dry-run skipped for 'FCR-DC': no billing account number was given. Set accountNumber(...) on the wizard to validate and to execute the plan
> - Router dry-run skipped for 'FCR-FR': no billing account number was given. Set accountNumber(...) on the wizard to validate and to execute the plan
> - Router dry-run skipped for 'FCR-SG': no billing account number was given. Set accountNumber(...) on the wizard to validate and to execute the plan

> **BANDWIDTH ROUNDED UP** - a requirement with no exact service-profile tier was rounded up to the smallest tier that satisfies it (this raises the billed bandwidth)
> - FCR-DC-to-aws: 4000 -> 5000 Mbps (+1000 Mbps billable)
> - FCR-FR-to-aws: 4000 -> 5000 Mbps (+1000 Mbps billable)

> **PROFILE CHOICE** - more than one service profile can carry these connections; the tightest-fitting default is used, and the alternatives are available to choose
> - FCR-DC-to-azure: default 0de4e413-edd7-4325-912f-7c8a4428e156; options: 0de4e413-edd7-4325-912f-7c8a4428e156 @ 3000 Mbps, a1390b22-bbe0-4e93-ad37-85beef9d254d @ 3000 Mbps
> - FCR-FR-to-aws: default de55ca94-df3b-4680-845f-6f7093f76bef; options: de55ca94-df3b-4680-845f-6f7093f76bef @ 5000 Mbps, ed48616a-c182-40b4-a462-a78ba812e961 @ 5000 Mbps
> - FCR-SG-to-gcp: default b97dacca-abe6-4e8c-8be1-fcdd1be1defe; options: b97dacca-abe6-4e8c-8be1-fcdd1be1defe @ 500 Mbps, bd4570e2-d792-4a00-87f5-3bde040cdcd7 @ 500 Mbps

## Required Customer Inputs (before provisioning)

| Connection | Provider | Authorization key | VLAN | Azure peering |
|------------|----------|-------------------|------|---------------|
| FCR-DC-to-aws | aws | AWS Account ID (12-digit) | required | - |
| FCR-DC-to-azure | azure | Azure ExpressRoute service key (GUID) | required | required |
| FCR-FR-to-aws | aws | AWS Account ID (12-digit) | required | - |
| FCR-FR-to-azure | azure | Azure ExpressRoute service key (GUID) | required | required |
| FCR-SG-to-gcp | gcp | GCP pairing key | required | - |

## Cloud Routers

| Name | Metro | Package |
|------|-------|---------|
| FCR-DC | DC | STANDARD |
| FCR-FR | FR | STANDARD |
| FCR-SG | SG | STANDARD |

## Provider Connections

| Name | Type | A-Side (Metro) | Z-Side (Provider) | Bandwidth | Workload Breakdown |
|------|------|----------------|-------------------|-----------|--------------------|
| FCR-DC-to-aws | IP_VC | FCR-DC (DC) | aws [us-east-1] | 5000 Mbps (rounded up from 4000) | order-api: 1000 Mbps, analytics: 2000 Mbps, disaster-recovery: 1000 Mbps |
| FCR-DC-to-azure | IP_VC | FCR-DC (DC) | azure | 3000 Mbps | analytics: 2000 Mbps, disaster-recovery: 1000 Mbps |
| FCR-FR-to-aws | IP_VC | FCR-FR (FR) | aws [eu-central-1; the design names us-east-1, reached over the provider's network] | 5000 Mbps (rounded up from 4000) | order-api: 1000 Mbps, analytics: 2000 Mbps, disaster-recovery: 1000 Mbps |
| FCR-FR-to-azure | IP_VC | FCR-FR (FR) | azure | 3500 Mbps | plant-erp: 500 Mbps, analytics: 2000 Mbps, disaster-recovery: 1000 Mbps |
| FCR-SG-to-gcp | IP_VC | FCR-SG (SG) | gcp [asia-southeast1] | 500 Mbps | inference: 500 Mbps |

## Backbone Links

| Name | Metro A | Metro Z | Bandwidth | Topology |
|------|---------|---------|-----------|----------|
| FCR-DC-to-FR | DC | FR | 10000 Mbps | FULL_MESH |
| FCR-DC-to-SG | DC | SG | 10000 Mbps | FULL_MESH |
| FCR-FR-to-SG | FR | SG | 10000 Mbps | FULL_MESH |

## Metro access

| Metro | Entered by | Rule |
|---|---|---|
| DC | Physical ports | the site 'Ashburn data center' is a data center in this metro and the inventory has no usable port here |
| FR | Network Edge | no data center site here and Network Edge is offered |
| SG | Network Edge | no data center site here and Network Edge is offered |

### Access items

| Name | Kind | Metro | Source | UUID | IBX | Speed | Pair | Note |
|---|---|---|---|---|---|---|---|---|
| DC-port-primary | Fabric port | DC | to order | - | - | 10000 Mbps | PRIMARY | order a 10G port in one redundancy group with priority PRIMARY |
| DC-xc-primary | cross connect | DC | to order | - | - | - | PRIMARY | from the cage to DC-port-primary |
| DC-port-secondary | Fabric port | DC | to order | - | - | 10000 Mbps | SECONDARY | order a 10G port in one redundancy group with priority SECONDARY |
| DC-xc-secondary | cross connect | DC | to order | - | - | - | SECONDARY | from the cage to DC-port-secondary on a diverse path |
| DC-internet-access | Internet Access service | DC | to order | - | - | - | - | general internet traffic and peers with no presence in the design leave here |
| FR-ne-primary | Network Edge device | FR | to order | - | - | - | PRIMARY | create a redundant device pair (SD-WAN or router image); this is the PRIMARY device |
| FR-ne-secondary | Network Edge device | FR | to order | - | - | - | SECONDARY | create a redundant device pair (SD-WAN or router image); this is the SECONDARY device |
| FR-internet-access | Internet Access service | FR | to order | - | - | - | - | general internet traffic and peers with no presence in the design leave here |
| SG-ne-primary | Network Edge device | SG | to order | - | - | - | PRIMARY | create a redundant device pair (SD-WAN or router image); this is the PRIMARY device |
| SG-ne-secondary | Network Edge device | SG | to order | - | - | - | SECONDARY | create a redundant device pair (SD-WAN or router image); this is the SECONDARY device |

### Access links

| Name | Metro | Lands on | Endpoint UUID | Bandwidth | Pair |
|---|---|---|---|---|---|
| DC-access-primary | DC | DC-port-primary | after the order | 10000 Mbps | PRIMARY |
| DC-access-secondary | DC | DC-port-secondary | after the order | 10000 Mbps | SECONDARY |
| FR-access-primary | FR | FR-ne-primary | after the order | 10000 Mbps | PRIMARY |
| FR-access-secondary | FR | FR-ne-secondary | after the order | 10000 Mbps | SECONDARY |
| SG-access-primary | SG | SG-ne-primary | after the order | 500 Mbps | PRIMARY |
| SG-access-secondary | SG | SG-ne-secondary | after the order | 500 Mbps | SECONDARY |

Notes:

- no project id was given, so existing Fabric ports were not read
- no account number was given, so existing Network Edge devices were not read

### Access connections

| Name | Type | A-Side (Cloud Router) | Z-Side | Bandwidth | Created by execution |
|------|------|-----------------------|--------|-----------|----------------------|
| FCR-DC-access-primary | IP_VC | FCR-DC (DC) | DC-port-primary (to order) | 10000 Mbps | no: the DC-port-primary does not exist yet |
| FCR-DC-access-secondary | IP_VC | FCR-DC (DC) | DC-port-secondary (to order) | 10000 Mbps | no: the DC-port-secondary does not exist yet |
| FCR-FR-access-primary | IP_VC | FCR-FR (FR) | FR-ne-primary (to order) | 10000 Mbps | no: the FR-ne-primary does not exist yet |
| FCR-FR-access-secondary | IP_VC | FCR-FR (FR) | FR-ne-secondary (to order) | 10000 Mbps | no: the FR-ne-secondary does not exist yet |
| FCR-SG-access-primary | IP_VC | FCR-SG (SG) | SG-ne-primary (to order) | 500 Mbps | no: the SG-ne-primary does not exist yet |
| FCR-SG-access-secondary | IP_VC | FCR-SG (SG) | SG-ne-secondary (to order) | 500 Mbps | no: the SG-ne-secondary does not exist yet |

## Routing Protocols

| Connection | Type | Customer Peer | Equinix Peer | ASN | BFD |
|------------|------|---------------|--------------|-----|-----|
| FCR-DC-to-aws | DIRECT | - | 10.100.0.1/30 | - | off |
| FCR-DC-to-aws | BGP | 10.100.0.2/30 | 10.100.0.1/30 | 64512 | 300ms |
| FCR-DC-to-azure | DIRECT | - | 10.100.1.1/30 | - | off |
| FCR-DC-to-azure | BGP | 10.100.1.2/30 | 10.100.1.1/30 | 64512 | 300ms |
| FCR-FR-to-aws | DIRECT | - | 10.100.2.1/30 | - | off |
| FCR-FR-to-aws | BGP | 10.100.2.2/30 | 10.100.2.1/30 | 64512 | 300ms |
| FCR-FR-to-azure | DIRECT | - | 10.100.3.1/30 | - | off |
| FCR-FR-to-azure | BGP | 10.100.3.2/30 | 10.100.3.1/30 | 64512 | 300ms |
| FCR-SG-to-gcp | DIRECT | - | 10.100.4.1/30 | - | off |
| FCR-SG-to-gcp | BGP | 10.100.4.2/30 | 10.100.4.1/30 | 64512 | 300ms |
| FCR-DC-to-FR | DIRECT | - | 10.100.5.1/30 | - | off |
| FCR-DC-to-FR | BGP | 10.100.5.2/30 | 10.100.5.1/30 | 64512 | 300ms |
| FCR-DC-to-SG | DIRECT | - | 10.100.6.1/30 | - | off |
| FCR-DC-to-SG | BGP | 10.100.6.2/30 | 10.100.6.1/30 | 64512 | 300ms |
| FCR-FR-to-SG | DIRECT | - | 10.100.7.1/30 | - | off |
| FCR-FR-to-SG | BGP | 10.100.7.2/30 | 10.100.7.1/30 | 64512 | 300ms |

## Cost Estimate

| Category | Monthly Cost |
|----------|-------------:|
| Cloud Routers | $3746.98 |
| Provider Connections | $1604.60 |
| Backbone Links | $1467.03 |
| **Total Monthly** | **$6818.61** |
| Provider fees and data egress (billed by the cloud providers; not created by this plan; reference figures) | $9124.73 |
| **Design total monthly** | **$15943.34** |
| Metro access connections (created by this plan) | $2236.66 |
| **Design total with metro access** | **$18180.00** |
| Setup (one-time) | $0.00 |

_Based on live Equinix Fabric pricing. Actual costs may vary based on contract terms, volume discounts, and promotional offers. 6 of 11 line(s) converted to USD: EUR at ECB reference rate via Frankfurter, 2026-11-17; SGD at ECB reference rate via Frankfurter, 2026-11-17; each converted line keeps its original amount, the rate and its date._

Not priced:

- Fabric port DC-port-primary: no rate card prices it; the cost is unknown, not zero
- cross connect DC-xc-primary: no rate card has a cross connect figure for DC; the cost is unknown, not zero
- Fabric port DC-port-secondary: no rate card prices it; the cost is unknown, not zero
- cross connect DC-xc-secondary: no rate card has a cross connect figure for DC; the cost is unknown, not zero
- Internet Access service DC-internet-access: no rate card prices it; the cost is unknown, not zero
- Network Edge device FR-ne-primary: no rate card prices it; the cost is unknown, not zero
- Network Edge device FR-ne-secondary: no rate card prices it; the cost is unknown, not zero
- Internet Access service FR-internet-access: no rate card prices it; the cost is unknown, not zero
- Network Edge device SG-ne-primary: no rate card prices it; the cost is unknown, not zero
- Network Edge device SG-ne-secondary: no rate card prices it; the cost is unknown, not zero

## Execution

Resources will be created in this order:

1. Cloud Routers (3)
2. Provider Connections (5)
3. Backbone Links (3)
4. Routing Protocols (16)

Call `plan.dryRun()` to validate, `plan.diff()` to compare with live state, `plan.reconcile(...)` to create only what is missing (resumable from a checkpoint), or `plan.execute(...)` to provision everything in one run.

After the Cloud Routers, execution creates the 0 access connection(s) whose port or device has a uuid. 6 wait for an order: put the uuid of the port or device on the plan, then run again.

Execution orders no port, cross connect, Network Edge device or Internet Access service. It uses the ones the plan names by uuid.
