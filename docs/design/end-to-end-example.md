# End-to-end design example

`EndToEndDesignExample` carries one design from requirements to a deployment decision. It uses every
design engine, then the API clients for the parts the design layer does not plan.

- Source: [`src/test/java/com/eqixiac/equinix/examples/EndToEndDesignExample.java`](../../src/test/java/com/eqixiac/equinix/examples/EndToEndDesignExample.java).
  It is compiled with the tests and has a `main` method.
- Offline test: `EndToEndDesignExampleTest` runs the same flow against a mocked Fabric catalogue and
  the reference prices, with no network.
- Output of a live read-only run on 2026-11-17: [`examples/end-to-end/`](examples/end-to-end/).
  Every figure and excerpt below comes from that run. Prices are design-time estimates, not quotes.
  Availability figures are planning assumptions, not SLAs.

The default run creates nothing. It reads the Fabric catalogue and prices, Network Edge metros and
Internet Access availability. When a project and a billing account are given it also reads that
project's Fabric ports and that account's Network Edge devices. Building the plan also sends one Cloud Router `dryRun=true` request per
metro when a billing account is given (see [Step 9](#step-9-build-the-deployment-plan)).

## Contents

- [The scenario](#the-scenario)
- [What decides what](#what-decides-what)
- [Run it](#run-it)
- [Step 1: write the spec](#step-1-write-the-spec)
- [Step 2: choose metros](#step-2-choose-metros)
- [Step 3: simulate failures](#step-3-simulate-failures)
- [Step 4: project growth and terms](#step-4-project-growth-and-terms)
- [Step 5: price the egress](#step-5-price-the-egress)
- [Step 6: decide how each destination is reached](#step-6-decide-how-each-destination-is-reached)
- [Step 7: decide how each metro is entered](#step-7-decide-how-each-metro-is-entered)
- [Step 8: write the decision record](#step-8-write-the-decision-record)
- [Step 9: build the deployment plan](#step-9-build-the-deployment-plan)
- [Step 10: the deploy gate](#step-10-the-deploy-gate)
- [What is not modelled](#what-is-not-modelled)
- [Engine changes that came from the example](#engine-changes-that-came-from-the-example)

## The scenario

| Input | Value |
|---|---|
| Sites | A cage in Ashburn (DC), offices in Chicago, Dallas, London and Singapore, a plant in Frankfurt |
| Clouds | AWS `us-east-1` (60,000 GB/month out), Azure `westeurope` (30,000), Google Cloud `asia-southeast1` (15,000) |
| Workloads | `order-api` 1 Gbps with a 250 ms round-trip limit; `analytics` 2 Gbps; `inference` 500 Mbps pinned to SG; `plant-erp` 500 Mbps (custom type) pinned to FR; `disaster-recovery` 1 Gbps |
| Peers | AS13335, AS20940, AS8075 |
| Constraints | Metros DC and FR required; a metro in AMER, EMEA and APAC; multi-region redundancy; 3 to 4 metros; at least 40% survival of the worst single failure |
| Objectives | latency 30%, cost 20%, provider coverage 20%, resilience 15%, compliance 5%, carbon 10% (Beta) |
| Commercial | USD, budget 60,000/month, 12-month term, 30% annual growth, 36-month horizon |

## What decides what

| Decision | Made by |
|---|---|
| Metros, workload placement, standby metros | `MetroOptimizer` |
| Cost, currency conversion, growth, commitment terms, egress value | `RateCardCostModel`, `GrowthPlanner`, `SavingsCalculator`, `TcoCalculator` |
| What a failure takes down, and what fixes it | `ResilienceAnalyzer` |
| Cloud Routers, cloud connections, backbone links, BGP | `DeploymentWizard` |
| Cloud on-ramp, IX, private connection or Internet Access per destination | `ConnectivityPlanner` ([Step 6](#step-6-decide-how-each-destination-is-reached)), using `PeeringIntelligence` output for peers |
| Physical ports, cross connects or Network Edge per metro, existing or to order | `ConnectivityPlanner` and `AccessPlanner` ([Step 7](#step-7-decide-how-each-metro-is-entered)) |
| Access connections from each Cloud Router to those ports and devices | `DeploymentWizard` |
| Whether to deploy | The gate in the example ([Step 10](#step-10-the-deploy-gate)) |

The plan lists the ports, cross connects, Network Edge devices and Internet Access services, and the
failure simulation fails each port, cross connect, device and access connection. The SDK orders none of
them: a port or device that exists is used by UUID, and one that does not is listed as an order to place.

## Run it

```bash
./mvnw -q test-compile dependency:build-classpath -Dmdep.outputFile=target/cp.txt
```

```bash
java -cp "target/classes:target/test-classes:$(cat target/cp.txt)" com.eqixiac.equinix.examples.EndToEndDesignExample target/end-to-end-example
```

| Setting | Effect |
|---|---|
| `EQUINIX_ACCESS_KEY`, `EQUINIX_SECRET_KEY` | Required |
| First argument | Output directory; default `target/end-to-end-example` |
| `PEERINGDB_API_KEY` | When set, the program runs Peering Intelligence, which calls PeeringDB, and the peers get a decision. Without it the step is skipped, because anonymous PeeringDB access allows about 20 requests a minute |
| `EQIXIAC_EXAMPLE_PROJECT`, `EQIXIAC_EXAMPLE_ACCOUNT` | Project and billing account for the plan and for deployment. The project's Fabric ports and the account's Network Edge devices are read and used before an order is planned |
| `EQIXIAC_EXAMPLE_PORTS` | Ports to use, named by UUID: `DC=uuid1,uuid2;FR=uuid3`. The first is the primary |
| `EQIXIAC_EXAMPLE_ESTATE_PROJECT` | Adds an estate review of that project's deployed Fabric resources |
| `--deploy` with `EQIXIAC_EXAMPLE_DEPLOY=yes` | Allows deployment if the gate passes. Both are needed |

In the live run the optimizer examined 814,385 portfolios in 16 seconds. No project or account was
given, so no existing port or device was read.

In your own code, run Peering Intelligence and give its result to the planner. The PeeringDB key is
the second argument of `PeeringIntelligence.builder(fabric, apiKey)`; without it the SDK reads
`PEERINGDB_API_KEY`, then falls back to anonymous access.

## Step 1: write the spec

One `DesignSpec` feeds every engine. `validate()` returns issues with a path; `requireValid()` throws
on an error.

```java
DesignSpec spec = DesignSpec.builder()
        .name("global-interconnect")
        .site(SiteIntent.builder().label("Ashburn data center").metro("DC").role(SiteRole.DATA_CENTER)
                .weight(3.0).build())
        .site(SiteIntent.builder().label("Chicago headquarters").metro("CH").role(SiteRole.HEADQUARTERS)
                .headcount(1200).build())
        // ... four more sites
        .cloud(CloudIntent.builder().id("aws").provider(CloudProviderType.AWS).region("us-east-1")
                .monthlyEgressGb(60_000d).build())
        // ... azure, gcp
        .workload(WorkloadIntent.builder().label("order-api").type(WorkloadType.TRANSACTIONAL)
                .bandwidthMbps(1000).latencySensitivity(LatencySensitivity.CRITICAL).maxLatencyMs(250.0)
                .clouds(List.of("aws")).build())
        .workload(WorkloadIntent.builder().label("plant-erp").type(WorkloadType.CUSTOM)
                .bandwidthMbps(500).pinnedMetro("FR").clouds(List.of("azure")).build())
        // ... analytics, inference (pinned to SG), disaster-recovery
        .peer(PeerIntent.builder().asn(13335L).label("CDN and DNS provider").build())
        .constraints(DesignConstraints.builder()
                .requiredRegions(List.of(Region.AMER, Region.EMEA, Region.APAC))
                .requiredMetros(List.of("DC", "FR"))
                .minimumRedundancy(RedundancyTier.MULTI_REGION)
                .minMetros(3).maxMetros(4)
                .minSurvivalPct(40.0)
                .build())
        .objectives(new DesignObjectives(null, weights))      // includes carbon: 0.10
        .commercial(CommercialTerms.builder().currency("USD").budgetMaxMonthly(new BigDecimal("60000"))
                .term(Term.MONTH_12).annualGrowthRate(0.30).horizonMonths(36).build())
        .build();
spec.requireValid();
```

The spec as YAML: [`spec.yaml`](examples/end-to-end/spec.yaml).

Two spec options exist that this scenario does not use:

- `WorkloadIntent.sites` limits a workload's latency limit and placement to the sites it names. Without
  it the limit applies to the farthest site in the spec, which is why `order-api` has a 250 ms limit.
- `DesignConstraints.allowAccessMetros` lets the optimizer add a metro that carries no workload and
  only shortens a site's path. It is off by default, so here every chosen metro runs a workload.

## Step 2: choose metros

```java
OptimizationResult optimization = MetroOptimizer.forSpec(fabric, spec).rateCard(rateCard).run();
DesignOption best = optimization.getRecommended();
```

Result: DC + FR + SG, weighted score 89.4 of 100, chosen from 15 feasible designs.

| Design | Weighted | Latency (ms) | Cost (USD/month) | Resilience (%) | Carbon (gCO2e/kWh) |
|---|---:|---:|---:|---:|---:|
| DC+FR+SG (recommended) | 89.4 | 16.0 | 15,943.3 | 82.2 | 390.2 |
| CH+DC+FR+SG | 87.6 | 5.4 | 19,197.9 | 82.2 | 390.2 |
| DC+FR+SG+TR | 86.7 | 11.9 | 19,111.6 | 82.2 | 390.2 |
| AM+DC+FR+SG | 86.3 | 14.9 | 18,972.9 | 82.2 | 390.2 |
| DC+FR+MA+SG | 86.2 | 14.8 | 19,087.8 | 82.2 | 390.2 |

Latency is the site-weighted round trip to the nearest chosen metro. The three required and pinned
metros were fixed by the spec. The optimizer chose whether a fourth pays for itself: none does at these
weights. The cost includes the full-mesh backbone between Cloud Routers, so a fourth metro adds a
router, its connections and three backbone links.

Sensitivity: the recommendation holds while the latency weight is at most 48% and the cost weight is at
least 10%. Outside either bound CH+DC+FR+SG takes over.

Placement:

| Workload | Runs in | Standby | Site-weighted round trip (ms) |
|---|---|---|---:|
| order-api | DC | FR | 49.2 |
| analytics | DC | FR | 49.2 |
| inference | SG (pinned) | none | 177.2 |
| plant-erp | FR (pinned) | none | 78.2 |
| disaster-recovery | DC | FR | 49.2 |

The optimizer places a workload first in a metro that reaches each cloud in the region the spec names.
DC reaches AWS `us-east-1`. The standby, FR, reaches AWS through `eu-central-1`. The report states this
as a `CLOUD_REGION_NOT_LOCAL` finding: traffic to `us-east-1` continues over the provider's network,
which is not modelled or priced.

Full report: [`optimization.md`](examples/end-to-end/optimization.md).

## Step 3: simulate failures

```java
ResilienceReport resilience = ResilienceAnalyzer.forSpec(fabric, spec)
        .optimization(optimization).rateCard(rateCard)
        .access(connectivity.accessPlan())       // from Step 7: ports, cross connects, devices
        .run();
```

| Check | Result |
|---|---|
| N-1 | Fails: 6 of 45 single failures cause an outage, 2 degrade service |
| N-2 | Fails: 245 of 915 pairs cause an outage, 74 degrade service |

The 45 single failures include each planned port, cross connect, Network Edge device and access
connection. None of them causes an outage, because every metro is entered through a redundant pair.
The outages come from the two pinned workloads, which have no standby metro. Remediation the analyzer
re-simulated and confirmed: add a standby Google Cloud connection in DC sized for 500 Mbps, raise the
Azure connection in DC from 3,000 to 3,500 Mbps, and add a second, redundant Azure connection in FR. For the loss
of FR or SG it found no candidate metro among the nearest five that removes the outage, because the
workload is pinned.

Full report, with the blast radius, the availability estimate and its assumptions:
[`resilience.md`](examples/end-to-end/resilience.md).

## Step 4: project growth and terms

```java
CatalogDesignContext context = FabricCatalogLoader.load(fabric, spec, null);
GrowthPlan growth = GrowthPlanner.forSpec(spec, portfolio, context, rateCard, ReferenceRateCard.standard()).plan();
```

16 items over 36 months at 30% a year. Example: the DC to AWS connection needs an upgrade from 5,000 to
10,000 Mbps in month 12. For virtual connections the live prices were the same at every term, so the
planner recommends month to month (ties go to the shorter term). Provider-side fees have no term axis
and are priced on demand. Full plan: [`growth.md`](examples/end-to-end/growth.md).

## Step 5: price the egress

```java
SavingsEstimate savings = SavingsCalculator.forSpec(fabric, spec, "aws", portfolio).rateCard(rateCard).calculate();
TcoComparison tco = TcoCalculator.forSpec(fabric, spec, "aws", portfolio).rateCard(rateCard).compare();
```

For AWS, 60,000 GB/month through DC:

| Line | USD/month |
|---|---:|
| Egress over the public internet | 5,400.00 |
| Egress over the private path | 1,200.00 |
| Equinix interconnect | -1,650.00 |
| AWS hosted-connection fee (reference) | -1,204.50 |
| Net saving | 1,345.50 |

Break-even is 40,779 GB/month. The TCO comparison recommends the Equinix interconnect archetype. Full
output: [`value.md`](examples/end-to-end/value.md).

## Step 6: decide how each destination is reached

```java
ConnectivityPlan connectivity = ConnectivityPlanner.forSpec(spec).optimization(optimization)
        .peering(peering)                                   // null when Peering Intelligence was not run
        .availability(ProductAvailability.read(equinix))    // Network Edge and Internet Access metros
        .inventory(AccessInventory.read(equinix, projectId, accountNumber))   // existing ports and devices
        .plan();
```

`ConnectivityPlanner` applies these rules to each destination:

1. A cloud is reached over a private Fabric connection from the Cloud Router in each metro where the
   optimizer's placement connects it.
2. A peer is reached by public peering at an Internet Exchange (IX) when Peering Intelligence finds it
   at an IX in a chosen metro. Otherwise it is reached over a private Fabric connection when it
   publishes a service profile there. Otherwise it is reached over Internet Access.
3. General internet traffic leaves through Internet Access in each chosen metro that offers it.

| Destination | Reached by | Metros |
|---|---|---|
| aws | Fabric and Cloud Router | DC, FR |
| azure | Fabric and Cloud Router | DC, FR |
| gcp | Fabric and Cloud Router | SG |
| AS13335, AS20940, AS8075 | Undecided | none |
| internet | Internet Access | DC, FR |

The peers are undecided because `PEERINGDB_API_KEY` was not set, so the live run did not run Peering
Intelligence.
With a result, rule 2 reads
`UnifiedConnectivityView.forMetro(metro)` for each peer (`isHasIxPeering()`, `isHasFabric()`).

Internet Access was offered in DC and FR and not in SG, as read from
`internetAccess().ibxs().availability(ConnectionType.IA_VC)`.

## Step 7: decide how each metro is entered

The same `ConnectivityPlan` holds the access of each metro (`connectivity.accessPlan()`). For each metro
the first rule that applies wins:

1. Ports you name with `usePorts(metro, uuids...)` are used. A UUID the inventory does not hold is
   assumed to exist.
2. Network Edge devices you name with `useDevices(metro, uuids...)` are used, in the same way.
3. Usable ports of the inventory in the metro, with room for the access connection, are used: a pair in
   one redundancy group, else two ports on different Fabric devices, else one port and an order for
   the second.
4. A metro with a site of role `DATA_CENTER` and no usable port gets an order for two ports in one
   redundancy group, each with a cross connect.
5. Usable Network Edge devices of the inventory in the metro are used.
6. A metro where Network Edge is offered gets an order for a redundant device pair.
7. Otherwise the metro has its Cloud Router only.

The live run read no inventory, so every item is an order:

| Metro | Entered by | Items | Access connections to the Cloud Router |
|---|---|---|---|
| DC | Physical ports | Two 10G ports to order (primary and secondary in one redundancy group), two cross connects to order, one Internet Access service to order | Two, 10,000 Mbps each |
| FR | Network Edge | Two devices to order, one Internet Access service to order | Two, 10,000 Mbps each |
| SG | Network Edge | Two devices to order | Two, 500 Mbps each |

An access connection is sized as the cloud bandwidth the placement lands in the metro, rounded up to a
Fabric tier: 8,000 Mbps in DC and 7,500 Mbps in FR give 10,000; 500 Mbps in SG gives 500.

With existing ports the result differs. The offline test gives the planner two ports in Ashburn, in one
redundancy group and in two IBXs. The planner uses them, plans no port order and no cross connect for DC,
and the plan's two DC access connections name the port UUIDs, so execution can create them:

```java
AccessInventory inventory = AccessInventory.of(List.of(
        new ExistingPort("port-uuid-a", "ashburn-cage-a", "DC", "DC2", "fabric-device-1", "cage",
                RedundancyPriority.PRIMARY, 10_000, 10_000, "ACTIVE"),
        new ExistingPort("port-uuid-b", "ashburn-cage-b", "DC", "DC6", "fabric-device-2", "cage",
                RedundancyPriority.SECONDARY, 10_000, 10_000, "ACTIVE")), List.of());
```

Two existing ports in the same IBX produce a finding, and the simulation then reports the loss of that
IBX as a shared-fate scenario. Network Edge availability comes from `networkEdge().setup().listMetros()`.
Full output: [`access.md`](examples/end-to-end/access.md).

## Step 8: write the decision record

```java
DesignDecisionRecord record = Design.over(fabric).decisionRecord(spec).rateCard(rateCard)
        .optimization(optimization).resilience(resilience).record();
Files.writeString(path, record.toMarkdown());
```

All nine sections were included. The record states the decision, each objective's value, score, weight
and scale, the alternatives, the sensitivity, every cost line with its source, the resilience findings
and how to reproduce the run. Result: [`decision-record.md`](examples/end-to-end/decision-record.md).

## Step 9: build the deployment plan

```java
DeploymentPlan plan = Design.over(fabric).deploymentWizard(optimization)
        .designSpec(spec).deploymentName("global-interconnect").rateCard(rateCard)
        .backboneTopology(BackboneTopology.FULL_MESH).bandwidthStrategy(BandwidthStrategy.PER_WORKLOAD)
        .customerAsn(64512L).withBFD(true, 300).notifications("noc@example.com")
        .access(connectivity.accessPlan())
        .plan();
String hcl = new TerraformExporter().export(plan);
String diagram = new TopologyDiagram().toMermaid(plan);
```

| Item | Count |
|---|---:|
| Cloud Routers | 3 |
| Cloud connections | 5 |
| Backbone links (full mesh) | 3 |
| Routing protocols | 16 |
| Access items (ports, cross connects, devices, Internet Access) | 10, all to order |
| Access connections | 6, none created until its port or device exists |

The plan connects a cloud only in a metro where the placement uses it: AWS and Azure in DC and FR,
Google Cloud in SG. The FR to AWS row reads `aws [eu-central-1; the design names us-east-1, reached over
the provider's network]`.

| Cost row in the plan | USD/month |
|---|---:|
| Cloud Routers | 3,746.98 |
| Provider connections | 1,604.60 |
| Backbone links | 1,467.03 |
| Total monthly | 6,818.61 |
| Provider fees and data egress (billed by the cloud providers, reference figures) | 9,124.73 |
| Design total monthly | 15,943.34 |
| Metro access connections | 2,236.66 |
| Design total with metro access | 18,180.00 |

The design total equals the optimizer's cost for DC+FR+SG in [Step 2](#step-2-choose-metros); the
optimizer does not price metro access. Six of the eleven Equinix lines were converted to USD from EUR
or SGD at the ECB reference rate of 2026-11-17. The ports, cross connects, Network Edge devices and
Internet Access services are listed as not priced: no rate card has a figure for them, and the cost is
unknown, not zero.

No billing account was given, so the three Cloud Router dry runs were skipped and the plan is valid
with no errors. Each skipped check is listed in the plan with the reason. Five cloud connection checks
are deferred until the routers exist and the provider keys are supplied. With an account, `plan()`
sends a Cloud Router `dryRun=true` request for each metro.

Execution creates an access connection when its port or device has a UUID: A-side the Cloud Router,
Z-side the port with the VLAN tag you pass in `ExecutionInputs`, or the device. It orders no port, cross
connect, Network Edge device or Internet Access service. To finish this plan, place the orders, then
build the plan again with `usePorts` and `useDevices`.

Outputs: [`plan.md`](examples/end-to-end/plan.md), [`plan.json`](examples/end-to-end/plan.json),
[`main.tf`](examples/end-to-end/main.tf), [`topology.mmd`](examples/end-to-end/topology.mmd),
[`access.md`](examples/end-to-end/access.md).

## Step 10: the deploy gate

The example deploys only when every check passes:

| Check | Live run |
|---|---|
| The plan is valid | Pass: 0 errors |
| The estimate is within the budget | Pass: 6,818.61 of 60,000 |
| N-1 passes | Fail |
| Provider inputs are present | Fail: 5 missing (AWS account ids, Azure service keys, a Google pairing key) |
| A project and billing account were given | Fail |
| Deployment was requested with `--deploy` and `EQIXIAC_EXAMPLE_DEPLOY=yes` | Fail |

Result: not deploying, 4 of 6 checks failed. When the gate passes, the example calls
`plan.reconcile(inputs, ExecutionOptions.defaults(), new FileCheckpointStore(dir))`, which creates what
is missing, records a checkpoint after each step, and can be re-run after a failure.

To get this design through the gate: supply the billing account and project, pass the provider keys in
`ExecutionInputs`, and either add standby metros for the two pinned workloads or accept the N-1 result
and remove that check.

## What is not modelled

- IX membership is not a plan item.
- Colocation is not a plan item. A cage is a site of role `DATA_CENTER` in the spec; cabinets and power
  appear only as cost lines in the TCO comparison; the IBX of an existing port is a shared-fate scenario
  in the failure simulation. Cages, cabinets and power are not failure elements.
- The optimizer's cost does not include metro access. The plan shows it on separate rows.
- Fabric ports, Network Edge devices and Internet Access services have no price in the rate cards.
- The SDK places no order for a port, cross connect, Network Edge device or Internet Access service
  from a plan. The create body of an access connection follows the Fabric v4 specification and has not
  been sent to the live service.
- The cloud side of each connection (the AWS, Azure or Google Cloud resource) is created with that
  provider's tools.
- The carbon objective is a siting indicator from country-level grid figures. It is not an emissions
  estimate.

## Engine changes that came from the example

The first version of this example exposed seven gaps in the design layer. Each is closed in 4.1.0; see the [CHANGELOG](../../CHANGELOG.md).

| Gap | Now |
|---|---|
| A workload latency limit applied to every site in the spec | `WorkloadIntent.sites` scopes a workload to named sites |
| A metro that only serves sites counted as unused | `DesignConstraints.allowAccessMetros` permits it; the report marks it `ACCESS_METRO` |
| `plan()` recorded validation errors when no billing account was given | The router dry run is a skipped check with the reason |
| A standby metro could connect a cloud in a region the spec did not name, without saying so | Placement prefers the named region; a different region is a `CLOUD_REGION_NOT_LOCAL` finding and is written on the plan row |
| The plan estimate and the optimizer cost differed for one design | The optimizer prices the backbone; the plan shows provider fees and egress and a design total |
| No engine chose between IX, a private connection and Internet Access, or between ports and Network Edge | `ConnectivityPlanner` |
| Ports, cross connects and Network Edge devices were not on the plan or in the failure simulation | `AccessPlanner`, `DeploymentWizard.Builder.access(...)`, `ResilienceAnalyzer.access(...)` |
