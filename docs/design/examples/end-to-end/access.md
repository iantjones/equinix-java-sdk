## How each destination is reached

| Destination | Reached by | Metros | Rule |
|---|---|---|---|
| aws | Fabric and Cloud Router | DC, FR | private on-ramp where the placement connects it |
| azure | Fabric and Cloud Router | DC, FR | private on-ramp where the placement connects it |
| gcp | Fabric and Cloud Router | SG | private on-ramp where the placement connects it |
| CDN and DNS provider (AS13335) | Undecided | none | no Peering Intelligence result for this peer |
| Content delivery provider (AS20940) | Undecided | none | no Peering Intelligence result for this peer |
| SaaS productivity provider (AS8075) | Undecided | none | no Peering Intelligence result for this peer |
| internet | Internet Access | DC, FR | Internet Access in each metro of the design that offers it |

## How each metro is entered

| Metro | Entered by | Rule | Would order |
|---|---|---|---|
| DC | Physical ports | the site 'Ashburn data center' is a data center in this metro and the inventory has no usable port here | order Fabric port DC-port-primary: order a 10G port in one redundancy group with priority PRIMARY; order cross connect DC-xc-primary: from the cage to DC-port-primary; order Fabric port DC-port-secondary: order a 10G port in one redundancy group with priority SECONDARY; order cross connect DC-xc-secondary: from the cage to DC-port-secondary on a diverse path; order Internet Access service DC-internet-access: general internet traffic and peers with no presence in the design leave here; virtual connection DC-access-primary, 10000 Mbps, from the Cloud Router in DC to DC-port-primary; virtual connection DC-access-secondary, 10000 Mbps, from the Cloud Router in DC to DC-port-secondary |
| FR | Network Edge | no data center site here and Network Edge is offered | order Network Edge device FR-ne-primary: create a redundant device pair (SD-WAN or router image); this is the PRIMARY device; order Network Edge device FR-ne-secondary: create a redundant device pair (SD-WAN or router image); this is the SECONDARY device; order Internet Access service FR-internet-access: general internet traffic and peers with no presence in the design leave here; virtual connection FR-access-primary, 10000 Mbps, from the Cloud Router in FR to FR-ne-primary; virtual connection FR-access-secondary, 10000 Mbps, from the Cloud Router in FR to FR-ne-secondary |
| SG | Network Edge | no data center site here and Network Edge is offered | order Network Edge device SG-ne-primary: create a redundant device pair (SD-WAN or router image); this is the PRIMARY device; order Network Edge device SG-ne-secondary: create a redundant device pair (SD-WAN or router image); this is the SECONDARY device; virtual connection SG-access-primary, 500 Mbps, from the Cloud Router in SG to SG-ne-primary; virtual connection SG-access-secondary, 500 Mbps, from the Cloud Router in SG to SG-ne-secondary |

These choices are rules applied to the design. Nothing is ordered.

## Access items by metro

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

### Access connections to the Cloud Router

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

Notes:

- No Peering Intelligence result was given, so the peers are undecided
- no project id was given, so existing Fabric ports were not read
- no account number was given, so existing Network Edge devices were not read
