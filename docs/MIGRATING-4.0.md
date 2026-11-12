# Migrating from 3.0.0 to 4.0.0

4.0.0 removes deprecated and non-functional surfaces without compatibility shims. A method that
called an operation the specification marks deprecated, or that the live service rejected, is
deleted rather than deprecated. Update the dependency, compile, and work through the compiler
errors with this page. [CHANGELOG.md](../CHANGELOG.md) has the complete list of changes; this page
covers the breaks a caller is most likely to meet, organised by what you used.

```xml
<dependency>
    <groupId>com.eqixiac.equinix</groupId>
    <artifactId>equinix-sdk-java</artifactId>
    <version>4.0.0</version>
</dependency>
```

## Contents

- [Authentication and errors](#authentication-and-errors)
- [Fabric](#fabric)
- [Network Edge](#network-edge)
- [Data center operations (was Customer Portal)](#data-center-operations-was-customer-portal)
- [Metro Optimizer](#metro-optimizer)
- [Rate cards and value calculators](#rate-cards-and-value-calculators)
- [Deployment Wizard and plans](#deployment-wizard-and-plans)
- [Peering Intelligence](#peering-intelligence)
- [MCP server](#mcp-server)
- [Build](#build)
- [Behaviour changes without a signature change](#behaviour-changes-without-a-signature-change)

## Authentication and errors

### If you called IAM project-scoped endpoints with client credentials

They answered HTTP 401 ("Project arg only supported when using an access-policy auth token") in
live tests on 2026-11-11. Use an STS access-policy token through the new `BearerTokenProvider`:

```java
// 3.0.0: no way to supply the token; the call failed with 401
IAM iam = new IAM(credentials);
iam.roles().listByProject(projectId);

// 4.0.0
IAM projectIam = new IAM(BearerTokenProvider.of(accessPolicyToken));
projectIam.roles().listByProject(ProjectIds.of(projectId));

// a token that rotates: the supplier is asked again before expiry and after a 401
IAM rotating = new IAM(BearerTokenProvider.from(
        () -> new BearerToken(currentToken(), Instant.now().plusSeconds(3600))));
```

`Equinix` also takes a `BearerTokenProvider`; other domain clients are reached through the session.
`STS` builds scopes with `TokenRequest.scope(StsScope)`. The README section
[Access-policy tokens](../README.md#access-policy-tokens-for-iam-project-scoped-calls) shows the exchange.

### If you read `ExceptionDetail` fields

`ExceptionDetail` is immutable and models the union of every service's error format. The getters
`getStatusCode`, `getMessage`, `getError`, `getFault`, `getStatus`, `getPath`, `getMoreInfo`,
`getTimestamp` and `getErrorMessageKey` are gone.

```java
// 3.0.0
d.getErrorCode(); d.getMessage(); d.getError(); d.getStatusCode();

// 4.0.0
d.getErrorCode(); d.getErrorMessage(); d.getDetails(); d.getProperty();
d.getErrorDomain(); d.getErrorTitle(); d.getDeveloperMessage(); d.getHelp();
d.toSummaryLine();                       // "code: message (field: ...)"
e.getStatusCode();                       // the HTTP status stays on EquinixServiceException
e.getResponseBodySnippet();              // when the body could not be parsed
```

`ExceptionAdditionalInfo` has a three-argument constructor and no setters.
`EquinixServiceException.getMessage()` now starts with `code: message (field: ...)`; code that
parsed the old message text needs to read `getExceptionDetails()` instead. The seven-argument
constructor adds `responseBodySnippet`.

### If you set credentials on a request or read them from the core client

`EquinixRequest.getEquinixCredentialsProvider()` and `setEquinixCredentialsProvider(...)` are gone;
credentials belong to the client. The core `EquinixClient.getEquinixCredentialsProvider()` returns
`null` for a client built with a `BearerTokenProvider`.

## Fabric

### If you read connection or port statistics

`/connections/{id}/stats`, `/ports/{id}/stats`, `/{asset}/{id}/metrics` and `/cloudevents` are
deprecated in the Fabric v4 specification; their methods and models are removed
(`Connections.getStatistics`, `Connections.getMetrics`, `Ports.getStatistics`, `Ports.getMetrics`,
`Metrics.getMetricsByAssetId`, `CloudEvents.getByAssetId`, `ConnectionStatistic`, `PortStatistic` and
the `*Statistic*` implementation types).

```java
// 3.0.0
ConnectionStatistic stats = fabric.connections().getStatistics(uuid, from, to);
List<Metric> m = fabric.connections().getMetrics(uuid, "equinix.fabric.connection.bandwidth_tx.usage", from, to);
List<CloudEvent> events = fabric.cloudEvents().getByAssetId("connections", uuid);

// 4.0.0
List<Metric> m = fabric.metrics().forConnection(uuid, from, to, "equinix.fabric.connection.bandwidth_tx.usage");
List<Metric> p = fabric.metrics().forPort(portUuid, from, to, "equinix.fabric.port.bandwidth_rx.usage");
fabric.metrics().search(MetricQuery.forConnection(uuid)
        .names("equinix.fabric.connection.bandwidth_tx.usage").between(from, to));
fabric.cloudEvents().search(CloudEventQuery.forAsset("connections", uuid).after(from));
```

The no-argument and filter-only `Metrics.search` and `CloudEvents.search` overloads are removed: the
service requires a subject and a metric name for metrics, and a subject, project or organisation for
cloud events. `MetricQuery` and `CloudEventQuery` build valid filters, and the SDK checks a raw
`FilterPropertyList` before sending it. `CloudEvents.searchLiveEvents(String...)` is new.

### If you used the cloud adapters or a service-profile Z-side

Fabric rejected a service-provider Z-side without `location.metroCode` (HTTP 400 `EQ-3142501`, live
on 2026-11-11). Every adapter now takes the metro of the provider port as its last argument, and
the service-profile Z-side builders without a metro are removed.

```java
// 3.0.0
AwsDirectConnectAdapter.of("123456789012", "us-east-1", profileUuid);
GoogleCloudInterconnectAdapter.of(pairingKey, "us-east4", profileUuid);
OracleFastConnectAdapter.of(virtualCircuitOcid, "us-ashburn-1", profileUuid);
AzureExpressRouteAdapter.of(serviceKey, peeringLocation, profileUuid, PeeringType.PRIVATE);
builder.zSideAccessPointServiceProfile(profileUuid, linkProtocol);

// 4.0.0
AwsDirectConnectAdapter.of("123456789012", "us-east-1", profileUuid, "DC");
GoogleCloudInterconnectAdapter.of(pairingKey, "us-east4", profileUuid, "DC");
OracleFastConnectAdapter.of(virtualCircuitOcid, "us-ashburn-1", profileUuid, "DC");
AzureExpressRouteAdapter.of(serviceKey, profileUuid, PeeringType.PRIVATE, "DC");
builder.zSideAccessPointServiceProfile(profileUuid, linkProtocol, "DC");
```

The Azure adapter no longer sends a `sellerRegion`; the service key identifies the circuit. The
constructors that wrap a provider SDK object take the metro as their last argument as well.
`ConnectionBuilder.aSideCloudProvider(...)` and `zSideAccessPointService(...)` are removed.

### If you created or updated service tokens with an expiry

`ServiceToken.expiry` and `issuerSide` are deprecated in the specification and removed.

```java
// 3.0.0
fabric.serviceTokens().define(Side.A_Side).withExpiry(30);
fabric.serviceTokens().update(uuid).expiry(60).save();

// 4.0.0
fabric.serviceTokens().define(Side.A_Side).withExpirationDateTime(LocalDateTime.now().plusDays(30));
fabric.serviceTokens().update(uuid).expirationDateTime(LocalDateTime.now().plusDays(60)).save();
```

`ServiceToken.getExpiry()`, `getIssuerSide()` and the builder's `hideAssetInfo(...)` are removed.

### If you iterated ports, router commands or network connections as a `PaginatedList`

`ports().list()`, `cloudRouters().commands(routerId)` and `networks().getConnections(networkId)`
return `List<T>`. The endpoints take no offset; live `GET /ports` returns every port. Use
`ports().search(...)` to page.

### If you used other removed Fabric types and members

| Removed | Use instead |
|---|---|
| `AccessPointType.XF_IC`, `CX_PORT` | `AccessPointType.IC`; an unknown value reads as `UNKNOWN` |
| `Port.getSettings()`, `PortSettings` | Port fields on `Port`; `PortOperator` updates for encapsulation (TPID) and `lagEnabled` |
| Port `serviceType` and bandwidth builder steps | Not in the specification's port create |
| `GatewayPackage.getNatType()`, `getNaclsMax()`, `getNaclRulesMax()`, `getHaSupported()`, `NatType` | `getVcCountMax()`, `getCrCountMax()`, `getVcBandwidthMax()` |
| `AccessPointBuilder.hideAssetInfo(...)`, `interconnection(...)`, `MetalInterconnection` | None: the product behind them reached end of life on 2026-06-30 |
| `ConnectionBuilder.endCustomer(...)` | None: not in the connection create schema |
| `com.eqixiac.equinix.fabric.client.RequestBuilder` | The typed search and filter builders |
| `selectionType` on port purchase orders | `type` |

EIA BGP routing protocols reference an ASN by UUID (`asn(uuid)`); the read side exposes
`routingProtocol`, `bgpAuthType` and `asn`.

## Network Edge

| 3.0.0 | 4.0.0 |
|---|---|
| `devices().getInterfaceStatistics(uuid, interfaceId, String start, String end)` | Same method with `Instant start, Instant end`; sent as whole-second UTC |
| `Backup.restoreToDevice(...)` | `Backup.restore()`: the restore endpoint is keyed by the backup UUID only |
| `ipType` builder steps and the `IPAssignment` enum | Removed |
| `Vendor.VMWWARE` | `Vendor.VMWARE` |

```java
// 4.0.0
InterfaceStats stats = ne.devices().getInterfaceStatistics(deviceUuid, "2",
        Instant.now().minus(Duration.ofHours(1)), Instant.now());
```

`Device.delete(DeviceDeleteRequest)`, `PublicKeys.delete`, `PublicKey.delete` and the vendors
`SIX_WIND`, `INFOBLOX`, `NETSKOPE`, `THALES` and `VYOS` are new.

## Data center operations (was Customer Portal)

The class, its package and every name derived from it are renamed. "Customer Portal" is an old
name; there is one Equinix portal (portal.equinix.com), and these are the operational product APIs (cross connects, Smart
Hands, shipments, site access, tickets, billing).

| 3.0.0 | 4.0.0 |
|---|---|
| `CustomerPortal` | `Operations` |
| package `com.eqixiac.equinix.customerportal` (and sub-packages) | `com.eqixiac.equinix.operations` |
| `Equinix.customerPortal()` | `Equinix.operations()` |
| MCP toolset `portal` | `ops` |
| MCP tools `portal_list_open_tickets`, `portal_get_billing_summary` | `ops_list_open_tickets`, `ops_get_billing_summary` |
| `ServerContext.Builder.customerPortal(...)` | `ServerContext.Builder.operations(...)` |
| no product-level entry point for Cross Connect or Digital LOA | `Equinix.crossConnects()` and `Equinix.digitalLoas()` return both API families (`CrossConnectApis`, `DigitalLoaApis`) |

A search and replace of `customerportal` to `operations`, `CustomerPortal` to `Operations` and
`customerPortal(` to `operations(` covers imports and call sites.

Other changes in this area:

| 3.0.0 | 4.0.0 |
|---|---|
| `workVisits().listLocations(...)` returning `WorkVisitLocation` (Work Visit v1 locations, end of life 2024-01-22) | `workVisits().listLocations()` or `listLocations(ibxs, details)` returning `LookupLocation`, from Lookup v2 |
| `WorkVisitLocation`, `WorkVisitLocationAccount`, `WorkVisitLocationCage`, `WorkVisitLocationCabinet` | `LookupLocation` |
| `CaseStatus`, `CasePriority`, `MediaType` | Removed: absent from the specifications |
| `PhonePreferenceToCall.BUSINESS_HOURS` | `MY_BUSINESS_HOURS` or the other values the specification lists |
| Digital LOA search with an empty filter | The typed builder requires a non-empty `filter.and`; the service rejected the empty form |

`crossConnects().list()`, `troubleTickets().list()` and `workVisits().list()` never existed; earlier
README examples showed them. Read their history with `orderHistory().search(...)` and an
`OrderHistoryProductType`. IBX SmartView v1 hierarchy, legacy and asset calls now fail before sending
when a required parameter such as `accountNo` is missing.

## Metro Optimizer

The optimiser was rebuilt around `DesignSpec` and an exhaustive portfolio solver. The 3.0.0 engine
and its configuration types are removed: `MetroOptimizerEngine`, `ScoringWeights`,
`OptimizationConstraints`, `BudgetRange`, and `LatencyMatrix.worstCase()` and `average()` (use
`worstCaseMs()` and `averageMs()`, which return `OptionalDouble`). `OptimizationRequest` no longer
carries constraints, strategy or scoring weights, and `OptimizationStrategy` has `profile()` instead
of weights.

### If you used the fluent builder

The builder remains and now builds a `DesignSpec` (`toSpec()` returns it). Its changed steps:

| 3.0.0 | 4.0.0 |
|---|---|
| `.scoringWeights(ScoringWeights.builder()...)` | `.weights(Map<String, Double>)` keyed by objective id (`latency`, `cost`, `provider_coverage`, `resilience`, `compliance`), or `.strategy(...)` for a named profile |
| `.constraints().monthlyBudget(min, max)`, `monthlyBudget(BudgetRange)` | `.monthlyBudget(max)` (USD) or `.monthlyBudget(BigDecimal max, String currency)`; the budget constraint is a ceiling only |
| `.addWorkload(...).profile(WorkloadProfile...)` | `type(...)`, `latencySensitivity(...)`, `bandwidthMbps(...)`, `maxLatencyToleranceMs(...)`, `pinnedMetro(...)` |
| `.requiresHighPowerDensity()`, `.requiresLiquidCooling()` | Removed: the catalogue has no power or cooling data. Pin the workload to a metro known to meet the requirement |
| (none) | `name(...)`, `geoZones(...)`, `minMetros(...)`, `pinnedMetro(...)`, provider `region(...)` and `monthlyEgressGb(...)` |

```java
// 4.0.0, builder form
OptimizationResult result = fabric.optimizeMetros()
        .addSite("New York HQ").nearestMetro("NY").role(SiteRole.HEADQUARTERS).headcount(500).done()
        .constraints().monthlyBudget(150_000).redundancy(RedundancyTier.MULTI_REGION).maxMetros(5).done()
        .strategy(OptimizationStrategy.BALANCED)
        .weights(Map.of("latency", 0.35, "cost", 0.15))
        .optimize();
```

### Moving to a DesignSpec

A spec can be written as YAML (see [the design platform reference](design/design-platform.md#designspec))
or built in code:

```java
DesignSpec spec = DesignSpec.builder()
        .name("ny-ld")
        .site(SiteIntent.builder().label("New York HQ").metro("NY").role(SiteRole.HEADQUARTERS).build())
        .site(SiteIntent.builder().label("London office").metro("LD").build())
        .cloud(CloudIntent.builder().id("aws").provider(CloudProviderType.AWS).region("us-east-1").required(true).build())
        .workload(WorkloadIntent.builder().label("api").type(WorkloadType.TRANSACTIONAL).bandwidthMbps(2000)
                .maxLatencyMs(25.0).clouds(List.of("aws")).build())
        .constraints(DesignConstraints.builder().maxMetros(3).minimumRedundancy(RedundancyTier.MULTI_REGION).build())
        .objectives(new DesignObjectives("balanced", Map.of("latency", 0.35, "cost", 0.15)))
        .commercial(CommercialTerms.builder().currency("USD").budgetMaxMonthly(new BigDecimal("150000")).build())
        .build();
OptimizationResult result = MetroOptimizer.forSpec(fabric, spec).run();
```

### Semantics that changed

- Results: `getRecommended()`, `getFrontier()`, `getSensitivity()`, `getPlacement()`,
  `getConstraintVerdicts()` and `getSpecFingerprint()` are new. `getRecommendations()`,
  `getTopology()` and `getCostEstimate()` restate the recommendation for the wizard.
- Latency is round-trip everywhere. 3.0.0 mixed a one-way estimate with round-trip measurements and
  used 150 ms where it had no data; an unknown latency is now unknown.
- `RedundancyTier.MULTI_METRO` means at least two metros in any regions. In 3.0.0 it forced a
  cross-region spread; use `MULTI_REGION` for that. `N_PLUS_1` means at least two metros, two of
  them in the same region.
- Required clouds and per-workload `maxLatencyMs` are hard constraints. When nothing satisfies them
  the result has no recommendation and a `NO_FEASIBLE_DESIGN` finding. `LatencySensitivity` is
  descriptive and does not filter.
- The budget is compared in one currency. Costs in other currencies are converted (see
  [Currency conversion](design/design-platform.md#currency-conversion)); 3.0.0 compared totals in
  different currencies directly.
- Scores use fixed scales (cost as a ratio to the cheapest option, latency anchored at 0 ms and the
  spec ceiling or 200 ms), so a score no longer depends on the other options evaluated.

## Rate cards and value calculators

| 3.0.0 | 4.0.0 |
|---|---|
| `ReferenceRateCard.cspInterconnectPortMonthly(provider, mbps)` (rounded up to a dedicated port) | `providerConnection(provider, ConnectionModel, mbps)`: exact, next tier up, or unpriced with a reason |
| `ReferenceRateCard.cspInterconnectPortMonthlyExact(provider, mbps)` | `providerConnection(provider, model, mbps)` with an exact tier |
| `cspInterconnectPortMonthlyQuote(provider, mbps)`: a dedicated-port figure | The same method now returns the `HOSTED` fee, which is what a Fabric cloud leg pays |
| Bundled `ratecard_reference_2026_06.json`, `asOf()` `"2026-06"` | `ratecard_reference_2026_11.json`, `asOf()` `"2026-11"`, a source and date on every row |
| `CustomRateCard.Builder.currency(...)` after rates were declared | Throws `IllegalStateException` when it would change the currency of declared rates |

```java
// 4.0.0
PriceLookup<PriceQuote> fee = ReferenceRateCard.standard()
        .providerConnection(CloudProviderType.AWS, ConnectionModel.HOSTED, 1000);
if (fee.isPriced()) {
    System.out.println(fee.value().getMonthlyRecurring() + " from " + fee.answeredBy());
} else {
    System.out.println(fee.unpricedReason());
}
```

Reference figures changed with the new bundle; code or tests that pinned 3.0.0 figures need the
new ones (an AWS 10 Gbps hosted connection is USD 1,810.40 per month in the 2026-11 bundle).
`CostBreakdown` lists unpriced items with reasons, and the TCO Markdown marks an incomplete total.
`TcoCalculator.forSpec(...)` and `SavingsCalculator.forSpec(...)` build a calculation from a spec.

## Deployment Wizard and plans

| 3.0.0 | 4.0.0 |
|---|---|
| `plan.rollback(outcome)` returning `List<ProvisioningError>` | Returns a `RollbackReport`: the removed resources and the orphaned ones, with their UUIDs |
| `execute()` | Aborts and rolls back by default on a router, routing-protocol, timeout or terminal failure (`FailurePolicy.ABORT_AND_ROLLBACK`); `ExecutionOptions` with `CONTINUE_ON_ERROR` keeps going. Throws `IllegalStateException` without a Fabric client |
| `PlannedConnection.zSideAuthenticationKey` serialised with the plan | Not serialised; supply keys through `ExecutionInputs` |
| `getSourceOptimization()` serialised | Not serialised |
| JSON properties `aside...` and `zside...` | `aSide...` and `zSide...` |

```java
// 4.0.0
DeploymentOutcome outcome = plan.execute(ExecutionInputs.none());
RollbackReport report = plan.rollback(outcome);
DeploymentOutcome keepGoing = plan.execute(ExecutionInputs.none(),
        ExecutionOptions.builder().failurePolicy(FailurePolicy.CONTINUE_ON_ERROR).build());
```

Connection create bodies now include the project and the marker keys `eqxSdkDeployment`, `eqxSdkKey`
and `eqxSdkPlan` in `additionalInfo`. Plan totals are converted to the plan currency by default.
The Terraform output changed: Azure `peering_type`, the redundancy group taken from the primary
connection, service-provider Z-side `location.metro_code`, `order.term_length`, the required router
project, the marker `additional_info`, and no read-only `bgp_ipv4.equinix_peer_ip`. Review the diff
before applying generated Terraform over an existing state.

## Peering Intelligence

`DiversityScore.distanceKm` and `estimatedRttMs` are nullable `Double`s instead of `double` with `NaN`.
`estimatedRttMs` is measured round-trip latency when Fabric publishes it and a calibrated estimate
otherwise; the old fibre floor moved to `rttFloorMs`, and `rttBasis` and `rttNote` say which.

## MCP server

### Tools

| 3.0.0 | 4.0.0 |
|---|---|
| `design_optimize_placement` with `sites`, `workloads`, `require_clouds`, `prefer_clouds`, `strategy`, `constraints` | `design_optimize` with `spec` (a DesignSpec object or a JSON or YAML string) |
| `design_plan_deployment`, `design_estimate_tco`, `design_compare_cloud_egress`, `design_analyze_peering` with per-tool parameters | The same names with `spec` or `spec_fingerprint` |
| `plan_id` of the form `plan-<n>` | 16 hex characters, a hash of the plan content |
| (none) | `design_spec_template`, `design_validate_spec`, `design_analyze_resilience`, `design_growth_and_terms`, `design_get_plan`, `design_plan_drift`, `design_decision_record`, `design_analyze_estate` |

`design_estimate_latency`, `design_export_terraform`, the operations, Network Edge and IBX tools and the
two broker tools keep their names. `design_analyze_estate` is in a new `estate` toolset; `fabric`
selects `design` and `estate`. A default launch serves 22 tools. Client prompts or scripts that
called `design_optimize_placement` must send a spec: call `design_spec_template` for a starting
point.

### Behaviour

- `fabric_confirm_change` checks `request_matches_binding` (the recompiled request hashes to the
  value bound at proposal time) and `not_evicted`; `spec_matches_binding` is gone. A client without
  elicitation gets a refusal unless `EQUINIX_MCP_ALLOW_UNCONFIRMED_MUTATIONS=true`.
- Service-token expiry in a proposal is counted from the proposal time.
- Remote strings in results are sanitised and listed under `untrusted_data`.
- A malformed `.env.local` line stops the server with exit status 3. In 3.0.0 quotes and `export`
  prefixes were kept as part of the value.
- `connect-claude-desktop.ps1` merges into an existing entry instead of replacing it and writes no
  secret unless you pass `-EmbedCredentials`. `connect-claude-desktop.sh` is new.

### Embedding

| 3.0.0 | 4.0.0 |
|---|---|
| `ServerContext.planStore()`, `PlanStore` | `ServerContext.designStore()`, `DesignStore` |
| `ToolRegistration` `openWorld` default `false` | Default `true`; set `openWorld(false)` for a tool that contacts nothing |
| `ProposalStore.mint(...)` returning `PendingChange` | Returns `Minted` (the change and any evicted proposals); `PendingChange` gains `requestSha256` and `compiledAt` |
| `HumanConfirmation.Status` | Adds `CALL_CANCELLED` |
| (none) | `ToolHandler.Cancellable`, `ServerContext.Builder.humanConfirmer(...)`, `EquinixMcpServer.Builder.stdio(in, out)` |

## Build

| 3.0.0 | 4.0.0 |
|---|---|
| `mvn ...` with a locally installed Maven | `./mvnw ...` (Maven Wrapper; a local Maven must be 3.9 or later) |
| GPG signing bound to the normal build (`verify` failed without a key) | GPG signing and Central publishing only with `-Prelease` |
| `-Pcoverage` | Runs the default and WireMock suites in one build, then the coverage gate |
| Shaded MCP jar of about 27 MB | About 12 MB: Azure, Netty and Qpid classes are excluded |

The live tiers read credentials from environment variables only (`EQUINIX_ACCESS_KEY`,
`EQUINIX_SECRET_KEY`, optional `EQUINIX_ACCESS_POLICY_TOKEN`), never `-D` properties. New tiers:
`integration-dryrun-existing` and `integration-scenario`. See [CONTRIBUTING.md](../CONTRIBUTING.md).

## Behaviour changes without a signature change

- After an HTTP 401 the SDK obtains a new token once and replays the request once. A call that
  failed after a token revocation can now succeed.
- Path parameters are percent-encoded (`:` and `@` are kept); a null or blank path parameter throws
  before sending.
- A `Retry-After` header given as an HTTP date is honoured.
- Wrappers keep their data after `delete()` returns 204; `isDeleted()` reports it.
- Fabric `prices().list(filter)` sends only the filter and reads one page.
- Fabric PATCH content types: ports and service tokens send `application/json-patch+json`, IP
  blocks `application/json`, networks `application/json-patch+json`; route filters and rules accept
  both.
- Metrics searches page 10 at a time; the service rejected larger pages.
- EIA v1 price `CustomerIpBlock` reads `prefixLength` as an integer or an array
  (`getPrefixLengths()`, and `getPrefixLength()` for the first).
- IBX SmartView `getCurrentTagPoint` sends `tagId`, the name the live service accepts.
