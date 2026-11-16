# End-to-end design example

`EndToEndDesignExample` carries one design from requirements to a deployment decision. It uses every
design engine, then the API clients for the parts the design layer does not plan.

- Source: [`src/test/java/com/eqixiac/equinix/examples/EndToEndDesignExample.java`](../../src/test/java/com/eqixiac/equinix/examples/EndToEndDesignExample.java).
  It is compiled with the tests and has a `main` method.
- Offline test: `EndToEndDesignExampleTest` runs the same flow against a mocked Fabric catalogue and
  the reference prices, with no network.
- Output of a live read-only run on 2026-11-16: [`examples/end-to-end/`](examples/end-to-end/).
  Every figure and excerpt below comes from that run. Prices are design-time estimates, not quotes.
  Availability figures are planning assumptions, not SLAs.

The default run creates nothing. It reads the Fabric catalogue and prices, Network Edge metros and
Internet Access availability. Building the plan also sends one Cloud Router `dryRun=true` request per
metro (see [Step 9](#step-9-build-the-deployment-plan)).

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
- [Findings from building the example](#findings-from-building-the-example)

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
| Cloud on-ramp, IX, private connection or Internet Access per destination | Rules in the example ([Step 6](#step-6-decide-how-each-destination-is-reached)), using `PeeringIntelligence` output for peers |
| Physical ports, cross connects or Network Edge per metro | Rules in the example ([Step 7](#step-7-decide-how-each-metro-is-entered)) |
| Whether to deploy | The gate in the example ([Step 10](#step-10-the-deploy-gate)) |

The design layer has no engine for the last three rows. The example states its rules in code and
prints what would be ordered. It sends no port, cross connect, Network Edge or Internet Access order.

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
| `--peering` | Runs Peering Intelligence, which calls PeeringDB. Set `PEERINGDB_API_KEY` for more than a few calls |
| `EQIXIAC_EXAMPLE_PROJECT`, `EQIXIAC_EXAMPLE_ACCOUNT` | Project and billing account for the plan and for deployment |
| `EQIXIAC_EXAMPLE_ESTATE_PROJECT` | Adds an estate review of that project's deployed Fabric resources |
| `--deploy` with `EQIXIAC_EXAMPLE_DEPLOY=yes` | Allows deployment if the gate passes. Both are needed |

The live run took 147 seconds and examined 814,385 portfolios.

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

Two limits in the spec shaped the scenario:

- A workload latency limit applies to the farthest site in the whole spec. A workload cannot be scoped
  to some sites. With sites on three continents, 250 ms is the smallest round number that any design
  meets.
- A metro counts as used only when a workload or cloud connection lands in it. A metro that would only
  shorten a site's access path is rejected by the `every_metro_used` constraint, so the APAC metro
  needs a pinned workload.

## Step 2: choose metros

```java
OptimizationResult optimization = MetroOptimizer.forSpec(fabric, spec).rateCard(rateCard).run();
DesignOption best = optimization.getRecommended();
```

Result: DC + FR + SG + TR (Toronto), weighted score 90.1 of 100, chosen from 15 feasible designs.

| Design | Weighted | Latency (ms) | Cost (USD/month) | Resilience (%) | Carbon (gCO2e/kWh) |
|---|---:|---:|---:|---:|---:|
| DC+FR+SG+TR (recommended) | 90.1 | 11.9 | 16,180.9 | 82.2 | 235.3 |
| DC+FR+SG | 89.4 | 16.0 | 14,480.6 | 82.2 | 390.2 |
| CH+DC+FR+SG | 88.8 | 5.4 | 16,233.9 | 82.2 | 390.2 |
| AM+DC+FR+SG | 87.5 | 14.9 | 16,112.1 | 82.2 | 390.2 |
| DC+FR+MA+SG | 87.5 | 14.8 | 16,170.7 | 82.2 | 390.2 |

Latency is the site-weighted round trip to the nearest chosen metro. The three required and pinned
metros were fixed by the spec. The optimizer chose whether a fourth pays for itself, and which one.

Sensitivity: the recommendation holds while the carbon weight is at least 7% and the cost weight is at
most 25%. Toronto is in the design because of the 10% carbon weight: the bundled table gives Canada a
lower grid intensity than the United States, and the placement runs `order-api`, `analytics` and
`disaster-recovery` there with DC as standby. Drop the carbon weight below 7% and DC+FR+SG wins.

Placement:

| Workload | Runs in | Standby | Site-weighted round trip (ms) |
|---|---|---|---:|
| order-api | TR | DC | 45.9 |
| analytics | TR | DC | 45.9 |
| inference | SG (pinned) | none | 177.2 |
| plant-erp | FR (pinned) | none | 78.2 |
| disaster-recovery | TR | DC | 45.9 |

Full report: [`optimization.md`](examples/end-to-end/optimization.md).

## Step 3: simulate failures

```java
ResilienceReport resilience = ResilienceAnalyzer.forSpec(fabric, spec)
        .optimization(optimization).rateCard(rateCard).run();
```

| Check | Result |
|---|---|
| N-1 | Fails: 6 of 38 single failures cause an outage |
| N-2 | Fails: 212 of 639 pairs cause an outage, 42 degrade service |

The outages come from the two pinned workloads, which have no standby metro. Remediation the analyzer
re-simulated and confirmed: add a standby Google Cloud connection in DC sized for 500 Mbps. For the loss
of FR or SG it found no candidate metro among the nearest five that removes the outage, because the
workload is pinned.

Full report, with the blast radius, the availability estimate and its assumptions:
[`resilience.md`](examples/end-to-end/resilience.md).

## Step 4: project growth and terms

```java
CatalogDesignContext context = FabricCatalogLoader.load(fabric, spec, null);
GrowthPlan growth = GrowthPlanner.forSpec(spec, portfolio, context, rateCard, ReferenceRateCard.standard()).plan();
```

19 items over 36 months at 30% a year. Example: the DC to AWS connection needs an upgrade from 5,000 to
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

No engine makes this choice. The example's rules (`connectivityFor`):

1. A cloud is reached over a private Fabric connection from the Cloud Router in each metro where the
   optimizer's placement connects it.
2. A peer is reached by public peering at an Internet Exchange (IX) when Peering Intelligence finds it
   at an IX in a chosen metro. Otherwise it is reached over a private Fabric connection when it
   publishes a service profile there. Otherwise it is reached over Internet Access.
3. General internet traffic leaves through Internet Access in each chosen metro that offers it.

| Destination | Reached by | Metros |
|---|---|---|
| aws | Fabric and Cloud Router | DC, TR |
| azure | Fabric and Cloud Router | DC, FR, TR |
| gcp | Fabric and Cloud Router | SG |
| AS13335, AS20940, AS8075 | Undecided | none |
| internet | Internet Access | DC, FR |

The peers are undecided because the live run did not pass `--peering`: Peering Intelligence reads
PeeringDB, which rate-limits anonymous callers. With a result, rule 2 reads
`UnifiedConnectivityView.forMetro(metro)` for each peer (`isHasIxPeering()`, `isHasFabric()`).

Internet Access was offered in DC and FR and not in SG or TR, as read from
`internetAccess().ibxs().availability(ConnectionType.IA_VC)`.

## Step 7: decide how each metro is entered

The design layer plans the Cloud Router in each metro and nothing below it. The example's rules
(`accessFor`):

1. A metro with a site of role `DATA_CENTER` is entered through two Fabric ports in one redundancy
   group, each with its own cross connect from the cage.
2. Any other metro is entered through a redundant pair of Network Edge devices when Network Edge is
   offered there, so that offices reach it over SD-WAN or IPsec without a cage.
3. Otherwise the metro has its Cloud Router only, reached over the backbone.

| Metro | Entered by | Would order |
|---|---|---|
| DC | Physical ports | Two 10G Fabric ports (primary and secondary in one redundancy group), two cross connects on diverse paths, virtual connections from both ports to the Cloud Router |
| FR | Network Edge | A redundant device pair, virtual connections from both devices to the Cloud Router |
| SG | Network Edge | The same |
| TR | Network Edge | The same |

Network Edge availability comes from `networkEdge().setup().listMetros()`. To validate these orders
without placing them, use the dry-run calls the SDK has: `fabric.ports().define()...dryRun().create()`
validates a port order, and `connections().define(...)...dryRun().create()` validates a virtual
connection. Both need a billing account, so the example prints the orders and does not send them.
The SDK has no dry run for cross connect orders or Network Edge device creates.

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
        .plan();
String hcl = new TerraformExporter().export(plan);
String diagram = new TopologyDiagram().toMermaid(plan);
```

| Item | Count |
|---|---:|
| Cloud Routers | 4 |
| Cloud connections | 12 |
| Backbone links (full mesh) | 6 |
| Routing protocols | 36 |

The plan's estimate is USD 10,797.34 a month. It covers the Equinix items only (Cloud Routers,
connections, backbone). The optimizer's USD 16,180.91 also includes the cloud providers' connection
fees and private-path egress, so the two figures differ by design.

`plan()` sent a Cloud Router `dryRun=true` request for each metro. With no billing account given, the
service answered HTTP 400 `EQ-3040032` (invalid account number) four times, and the plan records four
validation errors. Twelve connection checks are deferred until the routers exist and the provider keys
are supplied.

Outputs: [`plan.md`](examples/end-to-end/plan.md), [`plan.json`](examples/end-to-end/plan.json),
[`main.tf`](examples/end-to-end/main.tf), [`topology.mmd`](examples/end-to-end/topology.mmd).

## Step 10: the deploy gate

The example deploys only when every check passes:

| Check | Live run |
|---|---|
| The plan is valid | Fail: 4 errors (no billing account) |
| The estimate is within the budget | Pass: 10,797.34 of 60,000 |
| N-1 passes | Fail |
| Provider inputs are present | Fail: 12 missing (AWS account ids, Azure service keys, Google pairing keys) |
| A project and billing account were given | Fail |
| Deployment was requested with `--deploy` and `EQIXIAC_EXAMPLE_DEPLOY=yes` | Fail |

Result: not deploying, 5 of 6 checks failed. When the gate passes, the example calls
`plan.reconcile(inputs, ExecutionOptions.defaults(), new FileCheckpointStore(dir))`, which creates what
is missing, records a checkpoint after each step, and can be re-run after a failure.

To get this design through the gate: supply the billing account and project, pass the provider keys in
`ExecutionInputs`, and either add standby metros for the two pinned workloads or accept the N-1 result
and remove that check.

## What is not modelled

- Network Edge devices, physical ports, cross connects, Internet Access services and IX membership are
  not part of a `DeploymentPlan`. They are not priced, simulated or deployed by the design layer.
- The resilience simulation covers metros, Cloud Routers, on-ramps, cloud connections, backbone links
  and cloud regions. It does not model the ports, cross connects or Network Edge devices from Step 7.
- The cloud side of each connection (the AWS, Azure or Google Cloud resource) is created with that
  provider's tools.
- The carbon objective is a siting indicator from country-level grid figures. It is not an emissions
  estimate.

## Findings from building the example

| Finding | Effect |
|---|---|
| A workload latency limit applies to every site in the spec | A regional workload cannot have a regional limit |
| A metro that only serves sites counts as unused | A metro cannot be added for access latency alone |
| `plan()` sends router dry runs when no billing account is given | Every plan built without an account records validation errors |
| A standby metro can connect a cloud in a region the spec did not name | In the live plan, Toronto connects AWS `ca-central-1`; the spec names `us-east-1` |
| The plan estimate and the optimizer cost use different scopes | Two monthly figures for one design; the plan's excludes provider fees and egress |
| No engine chooses between IX, a private connection and Internet Access, or between ports and Network Edge | The example decides by rule |
