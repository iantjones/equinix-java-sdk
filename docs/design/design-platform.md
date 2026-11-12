# Design platform reference

This page documents `com.eqixiac.equinix.design` in release 4.0.0. The [README](../../README.md#design-platform)
has the summary. All latency on this page is round-trip milliseconds. Prices are live Equinix list
prices, dated reference figures copied from public pages, or estimates; every cost line states which.
None of them is a quote. Availability figures are planning assumptions, not SLAs.

## Contents

- [Conventions](#conventions)
- [DesignSpec](#designspec)
- [Metro Optimizer](#metro-optimizer)
- [Cost model, rate cards and currency](#cost-model-rate-cards-and-currency)
- [Growth and commitment terms](#growth-and-commitment-terms)
- [TCO and egress savings](#tco-and-egress-savings)
- [Resilience simulation](#resilience-simulation)
- [Estate analysis](#estate-analysis)
- [Plan lifecycle](#plan-lifecycle)
- [Peering Intelligence](#peering-intelligence)
- [Design Decision Record](#design-decision-record)
- [Extension points](#extension-points)

## Conventions

- Every engine takes a `DesignSpec`, records `spec.fingerprint()` (SHA-256 of the canonical JSON) on
  its result, and rejects a supplied result computed for another fingerprint.
- Engines are read-only. The only calls that create, change or delete resources are
  `DeploymentPlan.execute(...)`, `reconcile(...)` and `rollback(...)`.
- An unknown value stays unknown. A missing latency is not scored as zero or as a placeholder, and
  a missing price leaves the line unpriced with a reason instead of contributing zero.
- `Design.over(fabric)` (or `eq.design()` on a session) is the facade: `optimizeMetros(spec)`,
  `resilience(spec)`, `decisionRecord(spec)`, `analyzeEstate()`, `deploymentWizard(result)`,
  `peeringIntelligence()`, `savingsCalculator()` and `tcoComparison()`. Each engine also has a
  static entry point, shown below.

## DesignSpec

`DesignSpec` (schema version `"1"`) is a record read from YAML or JSON with `DesignSpecIO`. Reading is
strict: an unknown property is an error. `spec.validate()` returns `SpecIssue`s with a severity, a
dotted path and a fix; an error blocks every engine, and a warning means part of a result will be
unknown.

```yaml
schemaVersion: "1"
name: ld-ny-aws            # required; a plan made from this spec uses it as its deployment name
description: Two offices reaching AWS us-east-1 over Equinix Fabric
sites:                     # where users are; drives the latency objective
  - label: London office
    metro: LD              # Fabric metro code, or latitude + longitude instead
    role: HEADQUARTERS
  - label: New York office
    metro: NY
    weight: 1.0            # optional relative importance (> 0)
clouds:
  - id: aws-use1
    provider: AWS          # AWS, AZURE, GOOGLE_CLOUD, ORACLE_CLOUD, IBM_CLOUD, ALIBABA_CLOUD
    region: us-east-1
    monthlyEgressGb: 5000  # optional; prices cloud egress
workloads:
  - label: analytics
    type: GENERAL_COMPUTE
    bandwidthMbps: 1000    # sustained Mbps; sizes and prices the cloud connection
    clouds: [aws-use1]
    # maxLatencyMs: 80     # optional hard ceiling, round-trip ms from every site
    # pinnedMetro: NY
peers:
  - asn: 13335
    label: Cloudflare
constraints:
  maxMetros: 2             # default 3
  # minimumRedundancy: MULTI_METRO
  # maxLatencyMs: 100
  # geoZones: [EU]
  # minSurvivalPct: 99
objectives:
  profile: balanced
  # weights: {latency: 0.5, cost: 0.3}
commercial:
  currency: USD
  # budgetMaxMonthly: 5000
  # term: MONTH_12         # MONTH_1 (default), MONTH_12, MONTH_24, MONTH_36
  # annualGrowthRate: 0.25
  # horizonMonths: 36
  # exchangeRates:
  #   rates: {GBP: 1.27}
  #   asOf: "2026-11-12"
  #   source: treasury sheet
```

The same template is served by the MCP tool `design_spec_template` and the resource
`equinix-design://spec/template`; the JSON Schema is the resource
`equinix-design://schema/design-spec`.

Specs are records with builders, so a variation is a copy:

```java
DesignSpec strict = spec.toBuilder()
        .constraints(spec.constraints().toBuilder()
                .minimumRedundancy(RedundancyTier.MULTI_REGION)
                .minSurvivalPct(99.0)
                .build())
        .build();
String yaml = DesignSpecIO.toYaml(strict);
```

`MetroOptimizer.builder(fabric)` (and `fabric.optimizeMetros()`) remains as a fluent way to build a
spec; `toSpec()` returns the spec it would run.

## Metro Optimizer

```java
OptimizationResult result = MetroOptimizer.forSpec(fabric, spec).run();
DesignOption best = result.getRecommended();          // null when nothing is feasible
List<DesignOption> frontier = result.getFrontier();   // Pareto-optimal options, best score first
SensitivityReport sensitivity = result.getSensitivity();
System.out.println(result.toMarkdown());
```

### Search

The optimiser reads the Fabric metro and service-profile catalogues once (`FabricCatalogLoader`,
up to 100 pages each), then the `ExhaustiveSolver` evaluates every portfolio of 1 to `maxMetros`
metros (default 3). A live run on 2026-11-12 over 67 metros examined 2,278 portfolios for a
two-metro spec. Pricing runs in two passes: constraints that do not use cost reject portfolios
first, then the remaining ones are priced in a batch.

### Objectives and scoring

Five built-in objectives: `latency`, `cost`, `provider_coverage`, `resilience` and `compliance`.
Two more come from bundled plug-ins: `simulated_resilience` (`ResiliencePlugin`) and `carbon`
(`SustainabilityPlugin`, Beta). The optimizer adds each one when the spec gives it a weight, without
the caller passing a plug-in, and leaves it out otherwise.

#### Carbon (Beta)

`carbon` is the grid carbon intensity of the metros the workloads are placed in, weighted by
bandwidth, in gCO2e/kWh. Smaller is better. It scores on a fixed scale: 0 scores 100 and 700 scores 0
(an SDK assumption, roughly a coal-dominated grid).

```yaml
objectives:
  weights:
    carbon: 0.15
```

By default it reads a table bundled with the SDK (`CarbonIntensityTable.bundledReference()`,
resource `json/carbon_intensity_reference_2026_11.json`): national annual average carbon intensity
of electricity generation published by Ember and processed by Our World in Data, CC BY 4.0,
retrieved 2026-11-12, latest year published per country (2024 or 2025). To use your own figures per
metro or per country, pass `new SustainabilityPlugin(new CarbonIntensityTable(...))` to
`MetroOptimizer.plugin(...)`; a metro figure wins over its country's.

Limits to know before relying on it:

- It is a siting indicator, not an emissions estimate. It models no power draw of ports, routers or
  colocation.
- The bundled figures are per country, so two metros in the same country always tie.
- The figures are location-based. An operator's renewable energy purchases are not reflected.
- A metro with no figure makes the value unknown; nothing is filled in.
- Beta: the objective, its scale and the bundled table may change in a minor release.

Each objective's values are turned into 0 to 100 scores on a fixed scale, so one option's score does
not depend on which other options happened to be evaluated:

| Objective | Scale |
|---|---|
| `cost` | Ratio to the cheapest feasible option: equal scores 100, twice as much scores 50 |
| `latency` | Anchored: 0 ms scores 100, the spec's `maxLatencyMs` (200 ms when unset) scores 0 |
| `provider_coverage`, `resilience`, `compliance` | The percentage itself |

`OptimizationResult.getObjectiveScales()` and `getObjectiveUnits()` describe the scales used. Named
profiles set the weights:

| Profile | latency | provider_coverage | cost | resilience | compliance |
|---|---:|---:|---:|---:|---:|
| `balanced` (default) | 0.30 | 0.25 | 0.20 | 0.15 | 0.10 |
| `latency_first` | 0.50 | 0.20 | 0.10 | 0.10 | 0.10 |
| `cost_first` | 0.15 | 0.15 | 0.45 | 0.15 | 0.10 |
| `redundancy_first` | 0.20 | 0.15 | 0.15 | 0.40 | 0.10 |
| `provider_coverage_first` | 0.20 | 0.45 | 0.15 | 0.10 | 0.10 |

`objectives.weights` overrides individual weights; the result is normalised to sum to 1. Ties go to
the Pareto-optimal option. The weighted score ranks the feasible options; the frontier lists every
option that no other option beats on all objectives.

### Constraints

Constraints are hard: a portfolio that fails one is infeasible. Each has an id that appears in
verdicts and in the decision record.

| Id | Source in the spec |
|---|---|
| `required_metros`, `excluded_metros` | `constraints.requiredMetros`, `excludedMetros` |
| `required_regions`, `excluded_regions` | `constraints.requiredRegions`, `excludedRegions` |
| `compliance_zones` | `constraints.complianceZones` |
| `geo_zones` | `constraints.geoZones`: every metro inside these Fabric geo zones |
| `minimum_redundancy` | `constraints.minimumRedundancy` |
| `max_site_latency` | `constraints.maxLatencyMs`: site to serving metro |
| `workload_max_latency` | `workloads[].maxLatencyMs` |
| `metro_count` | `constraints.minMetros`, `maxMetros` |
| `required_clouds` | Clouds marked required, and clouds a workload references |
| `pinned_workloads` | `workloads[].pinnedMetro` |
| `budget` | `commercial.budgetMaxMonthly`, in the spec currency |
| `min_survival` | `constraints.minSurvivalPct`, checked by failure simulation (see [Resilience simulation](#resilience-simulation)) |
| `every_metro_used` | Always: every metro carries a workload or a cloud connection |

Redundancy tiers: `NONE` is one metro; `N_PLUS_1` is at least two metros, two of them in the same
region; `MULTI_METRO` is at least two metros in any regions; `MULTI_REGION` is at least two metros
spanning at least two regions.

When nothing is feasible the result carries the finding `NO_FEASIBLE_DESIGN` and
`getSolverNotes()` says which constraint rejected what. `getConstraintVerdicts()` holds the verdicts
for the recommendation.

Geo zones come from the Metros API. On 2026-11-12, 20 of 67 live metros (including Dublin and
Singapore) had no geo scopes or zones; `FabricCatalogLoader.zonesOf(metro)` then derives zones from
the ISO country code (`EU` for member states, `UK` for GB).

### Sensitivity

`SensitivityReport` gives, per objective, the weight range over which the recommendation holds and
which option takes over below and above it; the nearest alternative and its distance; and, for each
constraint, whether it is binding (what would be recommended without it) and how many portfolios
it rejects alone.

### Latency

Measured latency is Fabric's `connectedMetros.avgLatency`, which is round-trip. A site in the metro
it names is 0.5 ms from it. Other pairs are estimated as the fibre floor (speed of light in fibre
over the great-circle distance) times a route factor fitted from the measured pairs
(`RouteFactorCalibration`). On 2026-11-11 the overall factor was 1.73 from 2,140 measured pairs
(interquartile range 1.53 to 2.02), with per-region-pair factors from 1.49 (AMER to APAC) to 2.03
(EMEA to EMEA); the fallback when fewer than 5 pairs exist is 1.75. Each figure is labelled
measured, estimated, assumed or unknown (`LatencyFigure`), and `getLatencyCalibration()` reports
the factor used.

### Placement

`getPlacement()` returns a `PlacementPlan`: each workload runs in one metro, and in a design of two
or more metros its cloud connections are duplicated at full size in a standby metro.

## Cost model, rate cards and currency

### Rate cards

A `RateCard` answers price lookups for Equinix connections, Cloud Routers, colocation items, cloud
egress and native multicloud links. Each lookup returns a `PriceLookup` that is exact, the next
tier up, or unpriced with a reason; nothing is extrapolated.

| Card | Source |
|---|---|
| `EquinixRateCard.of(fabric)` | Live Fabric price search (`api.equinix.com`), one query per lookup, cached; sends `/termLength`. A live lookup during this release returned USD 1,500, 1,275 and 1,125 per month for a Cloud Router `STANDARD` in DC at 1, 12 and 36 months |
| `ReferenceRateCard.standard()` | Bundled `ratecard_reference_2026_11.json`, 730 hours per month, with a source and date on every row |
| `CustomRateCard` | Your own figures in one currency |
| `RateCard.layered(...)` | First card with an answer wins |
| `RateCard.standardChain(fabric)` | `EquinixRateCard` then `ReferenceRateCard` |
| `AwsPriceListRateCard`, `AzureRetailPricesRateCard`, `OracleCloudPriceListRateCard`, `GcpBillingCatalogRateCard` | Live cloud price lists for egress; Google needs an API key |

Provider connection fees are keyed by provider, `ConnectionModel` (`DEDICATED_PORT` or `HOSTED`) and
bandwidth: `ReferenceRateCard.providerConnection(CloudProviderType.AWS, ConnectionModel.HOSTED, 1000)`.
Fabric cloud legs are priced as `HOSTED`.

Price bands: a live Equinix price is exact; reference and composite figures carry a band of plus or
minus 25%; estimates plus or minus 50% (`PriceBands`).

### RateCardCostModel

`RateCardCostModel` prices a portfolio: one connection per metro and cloud (default `IP_VC`, rounded up
to the next Fabric tier from 50 Mbps to 100 Gbps), a Cloud Router `STANDARD` per metro that carries
a cloud connection, the provider's connection fee, and cloud egress. It prefetches prices on 6
threads with a 20 s timeout per lookup and switches to the reference card after 2 timeouts, which
adds a `PRICE_LOOKUP_FALLBACK` finding. A live run on 2026-11-12 took 10.7 s with prefetching and
48.1 s without, with identical results.

```java
RateCardCostModel model = RateCardCostModel.forSpec(spec, RateCard.standardChain(fabric))
        .prefetch(6, Duration.ofSeconds(20), 2)
        .build();
OptimizationResult result = MetroOptimizer.forSpec(fabric, spec).costModel(model).run();
```

Customer ports, colocation space and power, and site access circuits are not priced. Data into a
cloud is not charged by the providers and is not priced.

### Currency conversion

Every design is priced and ranked in `commercial.currency` (default USD). A line in another currency
is converted by the first source that has the rate:

1. Rates pinned in the spec (`commercial.exchangeRates`). They are part of the fingerprint and exact
   unless the spec states a band.
2. A converter you supply (`.currencyConverter(...)` on the optimiser, wizard, estate analyser and
   value calculators, or `DesignPlugin.currencyConverter()`).
3. Live ECB reference rates from `https://api.frankfurter.dev/v1/latest?base=<currency>`: one GET per
   converter, made on the first conversion that needs it, 3 s connect and 5 s overall timeout, never
   retried, band plus or minus 2%.
4. The bundled table `/json/fx_reference_2026_11.json`: ECB reference rates of 2026-11-11, with AED
   from its USD peg of 3.6725; band plus or minus 5%.

A currency missing from every source leaves the line unpriced. Offline use:
`-Dequinix.design.fx.offline=true` or `CurrencyConverters.bundledOnly()` removes step 3;
`.defaultRates(CurrencyConverters.none())` turns conversion off, so mixed-currency costs stay
unknown. Converted lines carry their `conversion` details; the findings `COST_CURRENCY_CONVERTED`
and `FX_FALLBACK` are informational. `result.specWithUsedRatesPinned()` returns the spec with the
rates the run used pinned, for a reproducible re-run. The Fabric prices API rejects a `/currency`
filter with HTTP 400, so prices arrive in the metro's local currency: live runs during this release
returned GBP in London and EUR in Frankfurt and Amsterdam.

## Growth and commitment terms

`GrowthPlanner` projects monthly traffic for each priced item as today's traffic times
`(1 + annualGrowthRate)^((month - 1) / 12)`, picks the next Fabric tier at or above it, reports the
upgrade month, and compares on demand against 12, 24 and 36 month terms with break-even and a
recommended term. `scenarios(low, high, MonteCarlo)` runs low and high growth and a seeded Monte
Carlo. Provider fees and egress have no term axis and are priced on demand.

```java
CatalogDesignContext context = FabricCatalogLoader.load(fabric, spec, model);
GrowthPlan growth = GrowthPlanner.forSpec(spec, result.getRecommended().portfolio(), context,
        RateCard.standardChain(fabric), ReferenceRateCard.standard()).plan();
System.out.println(growth.toMarkdown());
```

The decision record and the MCP tool `design_growth_and_terms` run this for you.

## TCO and egress savings

`TcoCalculator.forSpec(fabric, spec, cloudId, portfolio).compare()` compares deployment archetypes
(`PUBLIC_CLOUD_INTERNET`, `ON_PREM`, `EQUINIX_INTERCONNECT` and, for two clouds,
`NATIVE_MULTICLOUD_INTERCONNECT`) for one of the spec's clouds.
`SavingsCalculator.forSpec(fabric, spec, cloudId, portfolio).calculate()` compares a cloud's egress
over the internet with private interconnect, net of the provider's connection fee. `CostBreakdown`
lists unpriced items with reasons; a total is called complete only when every item is priced, and
the Markdown marks an incomplete total. The default comparison currency is USD.

For traffic between two clouds the calculators and the wizard also price the providers' native
cloud-to-cloud link (Beta) and report the break-even rate against the Equinix path; see
[cloud-to-cloud.md](cloud-to-cloud.md).

## Resilience simulation

```java
ResilienceReport report = ResilienceAnalyzer.forSpec(fabric, spec).optimization(result).run();
ResilienceAnalyzer.forSpec(fabric, spec).portfolio(MetroPortfolio.ofCodes("DC", "NY")).run();
System.out.println(report.toMarkdown());
```

The analyser builds a `DesignGraph` (sites, metros, Cloud Routers, on-ramps, cloud connections,
backbone links, cloud regions), then `FailureSimulator` simulates every single failure and up to
2,000 failure pairs (`ResilienceOptions.maxPairScenarios`). Each scenario is path, latency,
capacity and zone aware and ends `UNAFFECTED`, `SURVIVES`, `DEGRADED` or `OUTAGE`.

The report gives the blast radius ranked worst first, single points of failure, correlated risks
(redundant elements that share a failure domain), an N-1 verdict over single-element failures, an
N-2 verdict over the simulated pairs, and remediation that is re-simulated where possible. Cloud-region
and Equinix-region losses are shared-fate events: reported and ranked, not counted in N-1.

Availability per workload composes assumed per-element availabilities with independent failures
(`AvailabilityAssumptions.defaults()`): metro 0.9999, Cloud Router 0.9999, cloud connection 0.9995,
cloud on-ramp 0.9995, backbone link 0.9999, site access 0.999, cloud region 0.9999. These are round
figures chosen for comparison, not Equinix or cloud-provider SLAs; replace them with your
contractual figures. `toMarkdown()` includes a Mermaid diagram of the graph.

`constraints.minSurvivalPct` (percent, 0 to 100) makes the optimiser reject portfolios whose worst
single-element failure leaves less than that share of service. `ResiliencePlugin` adds the
`simulated_resilience` objective and the `geo_zones_failover` constraint.

Limits: failures are of whole elements, not partial degradation; backbone capacity is unknown and
does not constrain the simulation; a second-router remediation is not re-simulated.

## Estate analysis

`EstateAnalyzer` reviews what is already deployed. It is read-only and must be scoped:
`EstateScope.projects(...)`, `accounts(...)`, `resources(...)` or `entireAccount(maxPerType)`; an
unscoped call is refused. Each resource type is capped at 2,000 items, with a `TRUNCATED` gap when
the cap is hit.

```java
EstateReport report = EstateAnalyzer.builder(fabric)
        .scope(EstateScope.projects(projectId))
        .window(Duration.ofDays(30))
        .currency("USD")
        .analyze();
System.out.println(report.toMarkdown());
```

Utilisation comes from Fabric metrics `equinix.fabric.connection.bandwidth_tx.usage` and
`bandwidth_rx.usage` (bit/s, hourly): p50, p95 and peak as a percentage of provisioned bandwidth.
An unknown utilisation is never treated as zero. Seven rules produce findings in the categories
`RIGHT_SIZE_DOWN`, `RIGHT_SIZE_UP`, `ON_DEMAND_TO_TERM`, `CONSOLIDATE_BEHIND_ROUTER`,
`IDLE_OR_UNUSED`, `SINGLE_POINT_OF_FAILURE`, `CAPACITY_RISK` and `HYGIENE`. Each finding's
financial impact is priced, unpriced or none; reference prices carry a band and estimates plus or
minus 35% by default (`EstateThresholds`). The report totals per currency in the reporting currency,
counts the addressable saving once per resource, lists unpriced findings separately and never adds
currencies together.

`collect()` returns an `EstateInventory` you can save as JSON and analyse later with
`EstateAnalyzer.fromSnapshot(inventory)`.

The Fabric connection search ignored the `/project/projectId` filter in a live test on 2026-11-12
(86 connections from 14 other projects), so the analyser re-checks scope on the client. The port
search with a project filter returned HTTP 400 `EQ-3046106`. Port and Network Edge prices are not in
the rate cards, the reference card has no term axis, and Network Edge devices carry no project, so
they cannot be scoped.

## Plan lifecycle

```java
DeploymentPlan plan = fabric.deploymentWizard(result)
        .designSpec(spec)
        .projectId(projectId)
        .accountNumber(accountNumber)
        .notifications("ops@example.com")
        .plan();
plan.save(Path.of("plan.json"));

DeploymentPlan loaded = DeploymentPlan.load(Path.of("plan.json")).withFabric(fabric);
PlanDiff diff = loaded.diff();
System.out.println(diff.toMarkdown());

ExecutionInputs inputs = ExecutionInputs.builder()
        .authenticationKey("FCR-DC-to-aws", "123456789012")
        .vlanTag("FCR-DC-to-aws", 1001)
        .build();
DeploymentOutcome outcome = loaded.reconcile(inputs, ExecutionOptions.defaults(),
        new FileCheckpointStore(Path.of("checkpoints")));
String hcl = new TerraformExporter().export(loaded);
```

- Identity: `planId` is the first 16 hex characters of a SHA-256 over the canonical desired state.
  `load` checks the integrity of the file. Resources have logical keys such as `router/DC`,
  `connection/DC/aws` and `backbone/DC-NY`.
- Markers: connections the plan creates carry `additionalInfo` keys `eqxSdkDeployment`, `eqxSdkKey` and
  `eqxSdkPlan`; routers are found by exact name in the project. The plan JSON never contains cloud
  authorisation keys; supply them through `ExecutionInputs`.
- Drift: `diff()` reads live state and reports each resource as `MISSING`, `PRESENT_MATCHING`,
  `PRESENT_DIFFERENT` (with a patchable flag), `UNREADABLE` or `NOT_EXECUTABLE`, and lists unmanaged
  resources. Connections are re-checked against the project on the client.
- Execution: `execute(...)` and `reconcile(...)` create what is missing, checkpoint each step
  (`FileCheckpointStore` writes atomically; `InMemoryCheckpointStore` for tests) and resume from the
  checkpoint. `ExecutionOptions` sets the failure policy, `dryRun`, `applyUpdates` and timeouts
  (provision 5 min, delete 10 min, poll 5 s). The default policy `ABORT_AND_ROLLBACK` stops on a
  router, routing-protocol, timeout or terminal failure and rolls back; `CONTINUE_ON_ERROR` keeps
  going. Routing protocols are created once the connection is provisioned.
- Rollback: `rollback(outcome)` returns a `RollbackReport`. It waits for connection deletes to reach
  `DEPROVISIONED` or 404 before deleting routers, and reports orphaned resources with their UUIDs.
- Terraform: `TerraformExporter.export(plan)` writes Equinix provider HCL with routers, connections
  and routing protocols in dependency order and one input variable per authorisation key and VLAN.
  It writes Azure `peering_type`, the redundancy group of the primary connection, the service-provider
  Z-side `location.metro_code`, `order.term_length` and the marker `additional_info`.
- Fabric One interconnects (`PlannedInterconnect`) are planned, unpriced and `NOT_EXECUTABLE`: no
  public Fabric One API existed when this was written.

Live checks on 2026-11-12: drift read in a test project took about 1 s; an `IP_VC` dry run with the
marker in `additionalInfo` returned 200 and echoed it. A Cloud Router dry run needs
`account.accountNumber` (HTTP 400 `EQ-3040032` without it) and was not verified.

## Peering Intelligence

```java
PeeringIntelligenceResult peers = PeeringIntelligence.forSpec(fabric, spec)
        .includeAll()
        .analyze();
```

It reads PeeringDB (`www.peeringdb.com/api`) for each ASN in `spec.peers()`, resolves facilities to
Fabric metros through live metro codes, and reports presence per metro, peering policies, capacity
and resiliency. The PeeringDB key is resolved from the explicit argument, then
`EquinixConfig.peeringDbApiKey`, then the `PEERINGDB_API_KEY` environment variable, then anonymous
access, which PeeringDB throttles. Diversity scores report `distanceKm` and `estimatedRttMs` as
nullable values: measured round-trip when Fabric publishes it, a calibrated estimate otherwise, with
the fibre floor in `rttFloorMs` and the basis in `rttBasis`.

## Design Decision Record

`DesignDecisionRecorder` runs the optimiser, the resilience analysis, the growth and value models
where the spec gives enough input, and a deployment plan, then writes one record. The catalogue is
read once and one rate card prices every section.

```java
DesignDecisionRecord record = Design.over(fabric).decisionRecord(spec)
        .optimization(result)            // optional: reuse a result for this spec
        .record();
Files.writeString(Path.of("decision.md"), record.toMarkdown());
Files.writeString(Path.of("decision.json"), record.toJson());

DecisionDelta delta = DesignDecisionRecord.compare(previous, record);
DesignSpec again = record.reproducibleSpec();     // the spec with the run's exchange rates pinned
```

The record has nine sections (context, decision, alternatives, sensitivity, cost, resilience, risks,
provenance, implementation). Each is `INCLUDED`, `SKIPPED` or `FAILED` with a reason, so a record is
always produced. Supplied results (`.optimization`, `.resilience`, `.plan`, `.estate`, `.peering`)
are checked against the spec fingerprint. A live run on 2026-11-12 took 17.7 s. An excerpt of its
Markdown:

```markdown
## Decision

Deploy in **LD (London) + NY (New York)** (portfolio `LD+NY`, weighted score 89.2 of 100).

LD+NY has the highest weighted score (89.2 of 100) of the 1,081 feasible design(s) out of 2,278
examined; no feasible design is at least as good on every objective. Weights: latency 30%,
provider_coverage 25%, cost 20%, resilience 15%, compliance 10%. The closest alternative is
AM+NY (89.1).

| Objective | Value | Score | Weight | Scale |
|---|---:|---:|---:|---|
| latency | 0.5 ms | 99.8 | 30% | fixed: 0 scores 100, 200 scores 0 |
| cost | USD 3,865.12/month | 46.6 | 20% | ratio to the best option (twice as much scores 50) |
| provider_coverage | 100.0% | 100.0 | 25% | the percentage itself |
| resilience | 100.0% | 100.0 | 15% | the percentage itself |
| compliance | 100.0% | 100.0 | 10% | the percentage itself |

## Consequences

- Cost: USD 3,865.12 per month (range USD 3,660.52 to USD 4,069.75).
- Resilience: N-1 passes: every one of all 13 scenarios survives at full service.
- The decision holds while each weight stays within: latency 18% to 100%; cost 0% to 33%;
  resilience 5% to 100%; other weights can take any value.
- Implementation: 2 Cloud Router(s), 2 provider connection(s), 1 backbone link(s), 11 plan step(s).
```

In that run two of the six cost lines were London prices in GBP converted at the bundled reference
rate, so the cost section carried a `COST_CURRENCY_CONVERTED` finding and a plus or minus 5% band on
those lines.

## Extension points

`com.eqixiac.equinix.design.spi` holds the interfaces the engines are built on: `Objective`
(with a `ValueScale`), `Constraint`, `CostModel`, `CurrencyConverter`, `Solver`,
`CandidateGenerator` and `DesignPlugin`, which bundles objectives, constraints and a converter.
`MetroOptimizer.plugin(...)` installs a plug-in; `ResiliencePlugin` and `SustainabilityPlugin` are
the bundled examples. An objective from a plug-in on the classpath is added automatically when the
spec weights it; constraints from plug-ins stay opt-in through `plugin(...)`.
