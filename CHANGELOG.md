# Changelog

All notable changes to the Equinix Java SDK will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [4.0.0] - 2026-11-12

A major release. It removes deprecated and non-functional surfaces without compatibility shims, adds
bearer-token authentication, the Fabric v4 Beta families, a Messaging Gateway receiver, and a design
layer rebuilt around one input document (`DesignSpec`). [docs/MIGRATING-4.0.md](docs/MIGRATING-4.0.md)
maps every break to its replacement. The sections "Fixes from live validation", "Correctness fixes
from the 2026-11-11 review (P0)" and "Cloud-to-cloud" at the end of this release's notes were
written before the version number was chosen and are part of 4.0.0. The heading `[3.0.0]` below was
added for this release; its content is unchanged.

### Added

#### Product entry points
- `Equinix.crossConnects()` (`CrossConnectApis`) and `Equinix.digitalLoas()` (`DigitalLoaApis`) return
  both API families of each product: the order API under `operations()` and the Fabric v4 Beta API
  under `fabric().beta()`. The clients themselves do not move.

#### Core
- `BearerTokenProvider`, `BearerToken`, `StaticBearerTokenProvider` and `SupplierBearerTokenProvider`:
  sign requests with a token you supply, for example an STS access-policy token. Constructors
  `Equinix(BearerTokenProvider[, EquinixConfig])`, `IAM(BearerTokenProvider[, EquinixConfig])` and
  `EquinixClient(BearerTokenProvider[, EquinixConfig])`, which other facades can delegate to.
- HTTP 401 recovery: the SDK discards the token, obtains a new one once (single-flight across
  threads) and replays the request once. A second 401 is thrown. No replay for the token request,
  unauthenticated endpoints, or a client without a token source.
- `EquinixServiceException.getResponseBodySnippet()`: a truncated, whitespace-collapsed copy of an
  error body that could not be parsed. `ExceptionDetail.toSummaryLine()`.
- An injectable `Clock` for `CircuitBreaker`, `ResourceWaiter`, `EquinixHttpClient` and the core
  `EquinixClient.setClock(...)`; the default is monotonic.
- `DeletableResource.isDeleted()`.

#### Fabric
- `Metrics.search(MetricQuery)`, `forConnection(...)`, `forPort(...)` and `forMetro(...)`;
  `CloudEvents.search(CloudEventQuery[, sort])` and `searchLiveEvents(String...)`
  (`POST /liveEvents/search`).
- Stream, stream-asset and stream-subscription searches; Network Edge device streaming
  (`getDevice`, `attachDevice`, `detachDevice` under `/streams/{id}/devices/{deviceId}`).
- Dynatrace stream sink with the `API_TOKEN` credential type and `apiToken`.
- Access-point `providerIdentifier`, `productOffering`, `bandwidthCommit`, `address` and
  `interconnect`; `ConnectionBuilder.aSideAccessPointInterconnect(...)`; `ConnectionSearchField`.
- Port updates for encapsulation (TPID) and `lagEnabled`; connection `vlanTag(...)` and
  `vlanTags(...)` updates (path not verified live: use a dry run first).
- `GatewayPackage.getVcCountMax()`, `getCrCountMax()`, `getVcBandwidthMax()`; the
  `CONNECTION_PROVIDER_STATUS_UPDATE_REQUEST` change type.

#### Fabric Beta
- `fabric.beta()` returns `FabricBeta`: 82 operations from the Fabric v4 4.34.0 Beta families, one
  client per family: `asns()` (5), `exchangeServices()` (5), `appDomains()`, `appLinks()`,
  `appProcessors()`, `appServices()`, `appSubscriptions()` (42 together), `opticalConnects()` (4),
  `crossConnects()` including install schedules (12), `loas()` including actions, notes and consumers
  (10), `tags()` (`GET /tags/{uuid}`, 1) and `logos()` (3). Endpoints in
  `json/apiParams_FabricBeta.json`.
- `@Beta` annotation; 156 models in `fabric.beta.model`, 91 enums with an `UNKNOWN` fallback in
  `fabric.beta.enums`. Plain read models, mutations on the client, PATCH as `List<PatchOperation>`
  (`application/json` for ASNs, `application/json-patch+json` otherwise). No dry run: the
  specification offers none.
- Specification defects handled: `Asn.type`, `AsnOwnershipChallengeDetails.message` and `signature`,
  and `changeLog` on `AppLinkAttached*` appear in examples but not in the schemas; `CreateAsnRequest`
  requires an undeclared `type`; a change `path` is read as a string.
- Generator in `scripts/fabric-beta/`.

#### Network Edge, operations and other domains
- Network Edge: `DeviceDeleteRequest` and `Device.delete(DeviceDeleteRequest)`; `PublicKeys.delete`,
  `PublicKey.delete`; `PublicKeyJson.projectId`; vendors `SIX_WIND`, `INFOBLOX`, `NETSKOPE`, `THALES`,
  `VYOS`.
- Operations: `OrderHistory.searchPage(...)` and `OrderHistoryPage`;
  `Reports.getReports(duration, statuses, sorts)` and `ReportSort`; `AssetSearchRequest.source` and
  `AssetSearchSource`; `CageCabType.SHARED`, `DEMARCATION`, `UNKNOWN`.
- Internet Access: `InternetAccess.unifiedServices()` and `ipBlocks()` (the Fabric v4 Unified EIA
  clients); `ServiceSearchRequest.state()` and `sort()` with `ServiceSearchSortProperty`.
- IAM: `ProjectIds` (converts a bare project id to the `project:<id>` form IAM paths take).
- STS: `StsScope` and `TokenRequest.scope(StsScope)`.
- EIA v1 price: `CustomerIpBlock.getPrefixLengths()`; `getPrefixLength()` returns the first.

#### Messaging Gateway
- New package `com.eqixiac.equinix.messaging`: sealed `EmgMessage` (`IbxNotification`,
  `NetworkNotification`, `FabricNotification`, `BillingNotification`, `OrderNotification`,
  `ManagedServiceNotification`, `UnknownNotification`), `EmgHeader`, `TicketBody`, enums with
  `UNKNOWN`, a tolerant `EmgMessageParser` (`EmgParseException` only for bodies that are not EMG
  envelopes) and `EmgMessageHandler` for typed dispatch.
- `messaging.transport`: `EmgReceiver` and `EmgDelivery`, and `EmgListener`, which completes a
  message on success, abandons it when the handler fails, dead-letters unparseable bodies and closes
  gracefully.
- `messaging.transport.servicebus.ServiceBusEmgReceiver` (`forQueue`, `forSubscription`, `wrap`),
  which needs the optional `com.azure:azure-messaging-servicebus` 7.18.0.
- Not implemented: EMG Order Management (deprecated 2024-09-30), `client_credentials` EMG
  authentication (deprecated 2024-03-18), signature verification (undocumented) and attachment
  download.

#### Design
- `design.spec`: `DesignSpec` (schema `"1"`), `SiteIntent`, `WorkloadIntent`, `CloudIntent`,
  `PeerIntent`, `DesignConstraints` (including `minSurvivalPct`), `DesignObjectives`,
  `CommercialTerms` (including `exchangeRates`), `DesignSpecIO` for strict YAML and JSON, `validate()`
  with paths and fixes, and `fingerprint()`.
- Metro Optimizer: `MetroOptimizer.forSpec(fabric, spec)`, `forContext(CatalogDesignContext)` and
  `Design.optimizeMetros(spec)`; options `rateCard`, `costModel`, `currencyConverter`,
  `defaultRates`, `solver`, `plugin`, `multicloudEnvironments`. Results carry the spec and its
  fingerprint, `recommended`, the Pareto `frontier`, `constraintVerdicts`, `solverNotes`,
  `solveStatistics`, `sensitivity`, `objectiveScales`, `objectiveUnits`, `costComparability`,
  `latencyCalibration`, `placement`, `dataAsOf` and `dataProvenance`.
- `design.spi` (`Objective`, `ValueScale`, `Constraint`, `CostModel`, `CurrencyConverter`, `Solver`,
  `DesignPlugin` and the data types), `design.objective` (latency, cost, provider_coverage,
  resilience, compliance), `design.constraint` (`Constraints.forSpec`), `design.solve`
  (`ExhaustiveSolver`, `Ranking`, `SensitivityAnalysis`, `WeightProfiles`), `design.placement`
  (`WorkloadPlacer`, `PlacementPlan`), `design.optimizer.context` (`CatalogDesignContext`,
  `FabricCatalogLoader`, `ProviderCatalog`, `LatencyFigure`) and `design.geo.RouteFactorCalibration`
  (fitted on 2026-11-11: 1.73 from 2,140 measured pairs, interquartile range 1.53 to 2.02).
- `design.cost`: `RateCardCostModel` (connections per metro and cloud rounded to the Fabric tier,
  Cloud Router `STANDARD` per metro with a cloud connection, provider connection fee, egress;
  prefetch on 6 threads with a 20 s lookup timeout and reference fallback after 2 timeouts, finding
  `PRICE_LOOKUP_FALLBACK`), `PriceBands`, `CurrencyConverters`, `PinnedExchangeRates`,
  `FrankfurterExchangeRates`, `BundledExchangeRates`, `ConvertingRateCard`, `GrowthPlanner`,
  `GrowthPlan` and `ScenarioBand`.
- Currency: every design is priced and ranked in `commercial.currency` (default USD). Precedence:
  rates pinned in the spec, a supplied converter, live ECB rates from `api.frankfurter.dev` (one GET,
  3 s connect and 5 s overall timeout, band plus or minus 2%), the bundled table
  `/json/fx_reference_2026_11.json` (ECB 2026-11-11, AED at its 3.6725 peg, band plus or minus 5%).
  `CurrencyConverters.bundledOnly()` and `-Dequinix.design.fx.offline=true` keep it offline.
  Findings `COST_CURRENCY_CONVERTED` and `FX_FALLBACK`; `CostLine.conversion`;
  `OptimizationResult.specWithUsedRatesPinned()`.
- Rate cards: `PriceLookup<T>` (exact, next tier up, or unpriced with a reason), `RateCard.*Lookup`
  defaults and `cardName()`, a lookup trail on `LayeredRateCard`,
  `ReferenceRateCard.providerConnection(provider, ConnectionModel, mbps)` with `ConnectionModel`
  `DEDICATED_PORT` and `HOSTED`. `EquinixRateCard` sends `/termLength`.
- Value: `TcoCalculator.forSpec(...)`, `SavingsCalculator.forSpec(...)`, `CostBreakdown.unpriced`
  with reasons and `recommendationNote`.
- `design.resilience`: `DesignGraph` and `DesignGraphBuilder` (Mermaid and table), `FailureScenario`,
  `ScenarioCatalog` (every single failure, up to 2,000 pairs), `FailureSimulator` (verdicts
  `UNAFFECTED`, `SURVIVES`, `DEGRADED`, `OUTAGE`), `ResilienceAnalyzer.forSpec(...)`,
  `ResilienceReport` (blast radius, single points of failure, correlated risks, N-1, N-2,
  re-simulated remediation), `AvailabilityModel` and `AvailabilityAssumptions` (planning assumptions,
  not SLAs), `ResiliencePlugin` (`simulated_resilience` objective, `min_survival` and
  `geo_zones_failover` constraints), `Design.resilience(spec)`.
- `design.sustainability` (Beta): `CarbonObjective` (`carbon`, grid carbon intensity of where
  workloads are placed, fixed scale 0 to 700 gCO2e/kWh), `CarbonIntensityTable` (per metro and per
  country; `bundledReference()` is a country-level table from Ember via Our World in Data, CC BY 4.0,
  retrieved 2026-11-12), `SustainabilityPlugin`. A siting indicator, not an emissions estimate.
- The optimizer adds a classpath plug-in's objective (`carbon`, `simulated_resilience`) when the spec
  weights it; before, a weight on a plug-in objective had no effect unless the plug-in was passed.
- `design.estate`: `EstateAnalyzer.builder(fabric)` and `fromSnapshot(inventory)`,
  `Design.analyzeEstate()`, `EstateScope` (projects, accounts, resource ids or `entireAccount(max)`;
  unscoped refused; 2,000 per type with a `TRUNCATED` gap), `EstateInventory` JSON snapshots,
  `UtilizationProbe` and `UtilizationSnapshot` (p50, p95, peak), seven rules (right-size,
  on-demand to term, consolidate behind a router, idle, single point of failure, capacity risk,
  hygiene), `EstateFinding` with `FinancialImpact`, and `EstateReport` (totals per currency,
  addressable saving counted once per resource, unpriced list, coverage gaps, Markdown and JSON).
  Estate pricing takes `currency`, `currencyConverter` and `defaultRates`.
- `design.plan`: `PlanJson` save and load (plan id is 16 hex characters of a SHA-256 over the
  desired state; integrity check), logical keys, `PlanMarker` (`additionalInfo` keys
  `eqxSdkDeployment`, `eqxSdkKey`, `eqxSdkPlan`), `LiveStateReader`, `DriftDetector` and `PlanDiff`,
  `PlanExecutor` with `execute` and `reconcile`, `Checkpoint`, `InMemoryCheckpointStore`,
  `FileCheckpointStore`, `ExecutionOptions`, `FailurePolicy`, `PlanRollback`, `RollbackReport`,
  `OrphanedResource`, `StepResult` and `StepAction`. `DeploymentPlan.save`, `load`, `withFabric`,
  `diff`, `reconcile`. `PlannedInterconnect` (Fabric One; planned, unpriced, not executable). Wizard
  builder steps `designSpec`, `specFingerprint`, `deploymentName`, `interconnect`, `currency`,
  `currencyConverter`, `defaultRates`. `PlanPricing.lines` and `currencyNotes`.
- `design.record`: `DesignDecisionRecord` (schema `"1"`, nine sections each `INCLUDED`, `SKIPPED` or
  `FAILED` with a reason; `toMarkdown`, `toJson`, `fromJson`, `compare(before, after)` returning
  `DecisionDelta`, `reproducibleSpec()`), `DesignDecisionRecorder.forSpec(...)` and `forContext(...)`
  with fingerprint-checked `.optimization`, `.resilience`, `.plan`, `.estate`, `.peering`, and
  `Design.decisionRecord(spec)`.
- `PeeringIntelligence.forSpec(fabric, spec[, key])`; `PeeringRequest` and the result carry the spec
  fingerprint; `DesignGraphBuilder.peers(result, spec)`.

#### MCP server
- Design tools on `DesignSpec`: `design_spec_template`, `design_validate_spec`, `design_optimize`,
  `design_analyze_resilience`, `design_growth_and_terms`, `design_get_plan`, `design_plan_drift`,
  `design_decision_record` (paged Markdown of 16,000 characters per page) and
  `design_analyze_estate` (new `estate` toolset; 1 to 10 named projects). Every design tool takes
  `spec` (object, JSON or YAML) or `spec_fingerprint`; results carry the fingerprint and provenance;
  `detail` is `summary`, `standard` or `full`, and capped lists report `*_omitted`.
- Resources `equinix-design://schema/design-spec`, `equinix-design://spec/template`,
  `equinix-design://plans`, `equinix-design://plans/{plan_id}`; prompts `design_from_requirements`,
  `review_existing_estate`, `what_breaks_if`.
- `DesignStore` (60 minutes, 20 entries per kind, `evicted_from_store`) and `EngineRunner` for
  cancellable engine runs.
- `notifications/cancelled` support (`StdioCancellationInterceptor`, `CancellationSignal`,
  `ToolHandler.Cancellable`, `EquinixMcpServer.Builder.stdio(in, out)`).
- `HumanConfirmer` seam (`ServerContext.Builder.humanConfirmer`).
- `UntrustedText`: remote strings are sanitised, capped and listed under a top-level
  `untrusted_data` block.
- `EQUINIX_MCP_PRICING_TIMEOUT_MS` (100 to 120000 ms, default 12000): one live-pricing budget per
  call covering the Equinix rate card as well; the result's `equinix_live_pricing` block groups
  failures by message. `--env-file <path>`.
- `ClaudeDesktopConfigMerger` and the new `connect-claude-desktop.sh`.

### Changed

#### Naming: Customer Portal becomes Operations
- `CustomerPortal` is renamed `Operations`, package `com.eqixiac.equinix.customerportal` is renamed
  `com.eqixiac.equinix.operations`, and `Equinix.customerPortal()` is renamed `Equinix.operations()`.
  "Customer Portal" is an old name (there is one Equinix portal); the class covers the operational product
  APIs (cross connects, Smart Hands, shipments, site access, tickets, billing, reports).
- MCP toolset `portal` is renamed `ops`; tools `portal_list_open_tickets` and
  `portal_get_billing_summary` are renamed `ops_list_open_tickets` and `ops_get_billing_summary`;
  `ServerContext.Builder.customerPortal(...)` is renamed `operations(...)`.
- Resource `apiParams_CustomerPortal.json` is renamed `apiParams_Operations.json`.

#### Core
- Breaking: `ExceptionDetail` is immutable with a builder and reads every service's error format.
  `getStatusCode`, `getMessage`, `getError`, `getFault`, `getStatus`, `getPath`, `getMoreInfo`,
  `getTimestamp` and `getErrorMessageKey` are removed; use `getErrorCode`, `getErrorMessage`,
  `getDetails`, `getProperty`, `getErrorDomain`, `getErrorTitle`, `getDeveloperMessage`, `getHelp`.
  `ExceptionAdditionalInfo` is immutable with a three-argument constructor.
- Breaking: `EquinixServiceException.getMessage()` starts with `code: message (field: ...)`. A
  seven-argument constructor adds `responseBodySnippet`.
- Breaking: the core `EquinixClient.getEquinixCredentialsProvider()` returns `null` for a client
  built with a `BearerTokenProvider`.
- Error bodies parsed per service: Fabric, Network Edge, EIA and Projects arrays, the IAM `error`
  object, STS `error`/`error_description`, Operations and Smart Hands `errors[]`, IBX SmartView
  v1 and v2, the gateway error object and Apigee faults.
- Path parameters are percent-encoded (`:` and `@` kept); a null or blank path parameter throws.
- `Retry-After` as an HTTP date is evaluated against the injectable clock.

#### Fabric
- Breaking: the four cloud adapters take the metro of the provider port as their last argument
  (Fabric answered HTTP 400 `EQ-3142501` without `location.metroCode`). `AzureExpressRouteAdapter.of`
  is `(serviceKey, profileUuid, peeringType, metro)` and sends no `sellerRegion`.
- Breaking: `Ports.list()`, `CloudRouters.commands(...)` and `Networks.getConnections(...)` return
  `List` (the endpoints take no offset).
- Breaking: EIA BGP routing protocols reference an ASN by UUID (`asn(uuid)`); the read side has
  `routingProtocol`, `bgpAuthType` and `asn`.
- Metrics and cloud-event searches check their mandatory filters before sending.
- PATCH content types: ports and service tokens `application/json-patch+json` (specification; not
  verified live), IP blocks `application/json` (specification), networks
  `application/json-patch+json` only (live), route filters and rules accept both (live).
- `Prices.list(filter)` sends the filter only and reads one page; the live service ignores
  pagination on that search.

#### Network Edge, operations and other domains
- Breaking: `Devices.getInterfaceStatistics(...)` takes `Instant` bounds and sends whole-second UTC.
- Breaking: Digital LOA search uses a typed builder that requires a non-empty `filter.and`.
- IBX SmartView v1 hierarchy, legacy and asset calls fail before sending when a required parameter is
  missing. `getCurrentTagPoint` sends `tagId` (the specification says `tagid`; live returned 400 for
  `tagid`).
- EIA service search rejects the `/type` filter before sending (live returned 400 for every value).
- Trouble-ticket types read the live key `troubleticketTypes` and are never null.

#### Design
- Breaking: the optimiser evaluates portfolios of metros. `OptimizationRequest` no longer carries
  constraints, strategy or scoring weights; `OptimizationStrategy` has `profile()` instead of weights.
  Builder: `scoringWeights()`, workload `profile()`, `requiresHighPowerDensity()` and
  `requiresLiquidCooling()` are removed; `monthlyBudget(max)` and `monthlyBudget(BigDecimal, currency)`
  replace the range; `name()`, `weights()`, `geoZones()`, `minMetros()`, `pinnedMetro()`, `region()`,
  `monthlyEgressGb()` and `toSpec()` are added.
- Breaking: `RedundancyTier.N_PLUS_1` is at least two metros with two in one region; `MULTI_METRO` is
  at least two metros in any regions (it no longer forces a cross-region spread); `MULTI_REGION` is
  at least two metros in at least two regions.
- Breaking: required clouds and per-workload `maxLatencyMs` are hard constraints; when nothing is
  feasible the result has the finding `NO_FEASIBLE_DESIGN`. `LatencySensitivity` is descriptive.
- Scores use fixed scales (`ValueScale`): cost as a ratio to the cheapest option, latency anchored at
  0 ms and the spec ceiling or 200 ms, percentages as themselves. With min-max normalisation a 7% cost
  difference had moved a London and New York design to Jakarta and Warsaw; the live re-run on
  2026-11-12 recommends LD and NY (score 89.2).
- Breaking: `ReferenceRateCard` reads `ratecard_reference_2026_11.json` (`asOf()` is `"2026-11"`);
  provider rows are keyed by provider, `ConnectionModel` and bandwidth at 730 h per month, each with a
  source and date. `cspInterconnectPortMonthlyQuote` returns the `HOSTED` fee; Fabric cloud legs are
  priced as `HOSTED`. An AWS 10 Gbps hosted connection is USD 1,810.40 per month in this bundle.
- Breaking: `CustomRateCard.Builder.currency(...)` throws once rates in another currency are
  declared. TCO Markdown marks an incomplete total.
- Breaking: `DiversityScore.distanceKm` and `estimatedRttMs` are nullable `Double`s;
  `estimatedRttMs` is measured where Fabric publishes it, otherwise a calibrated estimate; the fibre
  floor moved to `rttFloorMs`; `rttBasis` and `rttNote` are added.
- Breaking: `DeploymentPlan.rollback(outcome)` returns `RollbackReport`. `execute()` aborts and rolls
  back by default on router, routing-protocol, timeout and terminal failures and throws
  `IllegalStateException` without a Fabric client. `getSourceOptimization()` and
  `PlannedConnection.zSideAuthenticationKey` are not serialised (supply keys through
  `ExecutionInputs`). JSON property names are `aSide...` and `zSide...` (were `aside` and `zside`).
  Connection create bodies include the project and the marker `additionalInfo`. Plan totals are
  converted to the plan currency by default. `toMarkdown()` has a new header.
- Terraform export: Azure `peering_type`; redundancy `group = one(equinix_fabric_connection.<primary>.redundancy).group`;
  service-provider Z-side `location { metro_code }`; required router project; `order { term_length }`;
  marker `additional_info`; read-only `bgp_ipv4.equinix_peer_ip` removed.
- `FabricCatalogLoader.zonesOf(metro)` derives zones from the country code (`EU` for member states,
  `UK` for GB) when the Metros API gives none; 20 of 67 live metros had none on 2026-11-12.
- `DeploymentPlan` was split into `PlanMarkdown`, `PlanPricer` and `PlanExecutor` (1,357 to about
  420 lines).

#### MCP server
- Breaking: `design_optimize_placement` is replaced by `design_optimize`, which takes a `spec`.
  `design_plan_deployment`, `design_estimate_tco`, `design_compare_cloud_egress` and
  `design_analyze_peering` take `spec` or `spec_fingerprint` instead of their own parameters.
  `plan_id` is a 16-hex content hash (was `plan-<n>`). `PlanStore` is replaced by `DesignStore`
  (`ServerContext.designStore()`). `fabric` selects `design` and `estate`.
- Breaking: `ToolRegistration.openWorld` defaults to `true`; `false` only on tools that contact
  nothing (`design_spec_template`, `design_validate_spec`, `design_list_multicloud_environments`,
  `design_get_plan`, `design_export_terraform`). Broker tool titles say the creates are billable;
  `design_plan_deployment` states that it sends dry-run POSTs.
- Breaking: the broker binds the token to the SHA-256 of the compiled request and recompiles at
  confirm (`request_matches_binding`); `confirm_checks.spec_matches_binding` is replaced by
  `not_evicted` and `request_matches_binding`. `ProposalStore.mint` returns `Minted`; `PendingChange`
  gains `requestSha256` and `compiledAt`; `HumanConfirmation.Status.CALL_CANCELLED` is added. Service
  token `expiry_days` counts from the proposal time and is sent as `expirationDateTime`. Evictions
  are reported (`evicted_proposals`, `Outcome.EVICTED`); the TTL is re-checked after the prompt; the
  prompt shows the compiled body; propose returns `compiled_request` and `request_sha256`.
- Breaking: a malformed `.env.local` line stops the server with exit status 3 and names the line and
  key. The parser accepts `export`, quotes, inline comments and a byte order mark.
- `EQUINIX_MCP_ELICIT_TIMEOUT_MS` accepts 100 to 3600000.
- `ops_list_open_tickets` sorts by `CREATED_ON` descending (verified live).
- The connect scripts merge into an existing entry, back up the config and write no secret unless
  `--embed-credentials` (`-EmbedCredentials`) is given.
- Server text describes Equinix's Fabric MCP server as Public Preview at
  `https://mcp.equinix.com/fabric` (Fabric and Network Edge, OAuth 2.1, viewer, operator and
  manager levels, create and update tools, no delete tools).
- MCP Java SDK 2.0.0 to 2.1.0 (`mcp-core`, `mcp-json-jackson2`); protocol versions 2024-11-05 to
  2025-11-25.

### Removed

#### Core
- `EquinixRequest.getEquinixCredentialsProvider()` and `setEquinixCredentialsProvider(...)`.
- The `playgroundHostName` entry of `apiParams_Core.json`.

#### Fabric
- Methods on the endpoints the specification deprecates: `Connections.getStatistics(...)` and
  `getMetrics(...)`, `Ports.getStatistics(...)` and `getMetrics(...)`,
  `Metrics.getMetricsByAssetId(...)`, `CloudEvents.getByAssetId(...)`, and the no-argument and
  filter-only `Metrics.search` and `CloudEvents.search`. Use `Metrics.search(MetricQuery)`,
  `forConnection`, `forPort`, `forMetro` and `CloudEvents.search(CloudEventQuery)`.
- `ConnectionStatistic`, `PortStatistic`, `ConnectionStat`, `PortStat`, `Statistic`,
  `BandwidthUtilization` and their JSON and wrapper types, and the internal port-statistics client.
- Service token `expiry` and `issuerSide` (`withExpiry`, `expiry`, `getExpiry`, `getIssuerSide`); use
  `withExpirationDateTime` and the updater's `expirationDateTime`.
- `AccessPointType.XF_IC` and `CX_PORT` (use `IC`); `hideAssetInfo`; `PortSettings` and
  `Port.getSettings()`; the port `serviceType` and bandwidth builder steps; port purchase-order
  `selectionType`; the `interconnection(...)` builder and `MetalInterconnection` (product end of life
  2026-06-30);
  `ConnectionBuilder.endCustomer(...)`, `aSideCloudProvider(...)` and `zSideAccessPointService(...)`;
  the service-provider Z-side builders without a metro; `fabric.client.RequestBuilder`; `NatType` and
  the `GatewayPackage` NAT, NACL and HA getters.

#### Network Edge, operations and other domains
- Operations Work Visit v1 locations (end of life 2024-01-22) and the `WorkVisitLocation*`
  models: `workVisits().listLocations()` returns `LookupLocation` from Lookup v2.
- `PhonePreferenceToCall.BUSINESS_HOURS`; the `CaseStatus`, `CasePriority` and `MediaType` enums.
- Network Edge `ipType` and `IPAssignment`; `Backup.restoreToDevice(...)` (use `Backup.restore()`).
- `Vendor.VMWWARE` (use `VMWARE`).

#### Design
- `MetroOptimizerEngine`, `ScoringWeights`, `OptimizationConstraints`, `BudgetRange`,
  `LatencyMatrix.worstCase()` and `average()` (use `worstCaseMs()` and `averageMs()`).
- `ReferenceRateCard.cspInterconnectPortMonthly(...)` and `cspInterconnectPortMonthlyExact(...)`
  (use `providerConnection(...)`) and the `ratecard_reference_2026_06.json` bundle.

#### MCP server
- `design_optimize_placement` and the non-spec parameters of the design tools; `PlanStore`.

### Fixed

#### Core
- A token rejected with 401 is replaced and the request replayed once, instead of failing every call
  until expiry.
- Wrappers keep their state after `delete()` returns 204 (18 Fabric wrappers, and Network Edge,
  Operations and IBX SmartView wrappers).

#### Fabric
- Metrics searches with a page size above 10 were rejected (HTTP 400 `EQ-3045002`, 2026-11-12); the
  SDK pages metrics 10 at a time.

#### Network Edge, operations and other domains
- Operations invoice filter no longer throws a `NullPointerException` when a filter is unset,
  and joins list values with commas.
- Lookup v2 `WORK_VISIT` and `SHIPMENTS` locations were always empty (the response is keyed by
  permission code).
- Order history: typed enums from the live service (`TROUBLE`, `INBOUND_SHIPMENT`,
  `OUTBOUND_SHIPMENT`, `PAST_*` date ranges; 22 statuses).
- IBX SmartView `dcim/v3` power events read the live `{data, pagination}` envelope.
- Network Edge public-key create reads the id from the `Location` header (201 with an empty body).
- `Vendor` "F5 Networks" maps correctly.
- EIA v1 price `ipBlock.prefixLength` is an array live; it is read as an array or an integer.
- Trouble-ticket types were null live.

#### Design
- The budget check compares one currency; costs in other currencies are converted or reported
  unknown, never added.
- Cost line items no longer double-count bandwidth; the phantom USD 500 port and the regional cost
  constants are gone.
- Ties are broken by Pareto optimality, not API order.
- `SpeedOfLightLatency` no longer returns `NaN`.
- Rollback waits for connection deletes to reach `DEPROVISIONED` or 404 before deleting routers and
  reports orphans with their UUIDs; one failure policy applies (no silent continue); routing
  protocols wait for `PROVISIONED`.
- Redundant connection bodies: only the `PRIMARY` sends a priority; the `SECONDARY` sends the
  primary's live group id.
- The Fabric connection search ignored the project filter live (2026-11-12); plan drift and estate
  analysis re-check the project on the client, and checkpointed connections are read by UUID.
- `PortOperation` had no getters (`@Getter` added).

### Security
- Plan JSON never contains cloud authorisation keys.
- MCP tool output is sanitised and remote strings are labelled `untrusted_data`.
- The MCP broker executes only a request whose hash matches the proposal, re-checks expiry after the
  human prompt, and reports evicted proposals.
- The connect scripts write no secret into the Claude Desktop config by default.
- CI runs OSV-Scanner; `-Pdependency-check` runs OWASP dependency-check (fails on CVSS 7 or above).

### Build
- Version 4.0.0. Maven Wrapper 3.9.16 (`./mvnw`). Reproducible builds. GPG signing and Central
  publishing run only with `-Prelease`. The enforcer requires Java 21+, Maven 3.9+, pinned plugins and
  no duplicate classes.
- Upgrades: WireMock 3.13.2, JUnit 5.14.4, Surefire 3.6.1, JaCoCo 0.8.16, maven-source 3.4.0,
  maven-javadoc 3.12.0, SLF4J 2.0.20, Jackson 2.21.8 (BOM), Mockito 5.24.0, maven-compiler 3.14.1,
  MCP Java SDK 2.1.0, Lombok 1.18.48 (1.18.42 fails to compile on JDK 27).
- The bytecode target stays Java 21. The build, both suites, the coverage gate and Javadoc pass on
  JDK 21 and on JDK 27.0.1.
- `.env.local.example` is removed. Create `.env.local` with `EQUINIX_ACCESS_KEY=` and
  `EQUINIX_SECRET_KEY=` lines; the runners and connect scripts say so when the file is missing.
- The shaded MCP server jar excludes Azure, Netty and Qpid classes: 27.1 MB to 12.2 MB. The optional
  `azure-messaging-servicebus` is pinned at 7.18.0 for reactor-core 3.7 alignment with `mcp-core`.
- `-Pcoverage verify` runs the default and WireMock suites and the JaCoCo gate (line 86%,
  instruction 88%, branch 73%, 35% per package).
- Live tiers `integration-readonly`, `integration-dryrun`, `integration-dryrun-existing`,
  `integration-full` and `integration-scenario` (the last two need `-DconfirmDestructive=true`);
  `LiveTierExtension` reports PASS, SKIPPED, SERVER_ERROR or FAIL; `run-live.sh` and `run-live.ps1`
  take the tier and flags; optional `EQUINIX_ACCESS_POLICY_TOKEN`.
- Fixture conformance: `com.eqixiac.equinix.spec` (`OpenApiSubsetValidator`,
  `FixtureSpecConformanceTest`) validates fixtures against 26 bundled specifications;
  `fixture-manifest.json` must list every fixture; `fixture-deviations.json` records 89 deviations;
  `fixture-pending-review.json` lists 231 mismatches as a baseline under review. 13 unused fixtures
  deleted.
- CI: `build.yml` (SHA-pinned actions, coverage, Javadoc, shaded-jar smoke test, OSV-Scanner),
  `live-readonly.yml` (nightly), Dependabot. `scripts/release.sh` and `release.ps1` need a clean tree
  and a `## [version]` section in this file.
- `CONTRIBUTING.md` rewritten.
- Live read-only tier on 2026-11-12, after the fixes above: 184 PASS, 67 SKIPPED, 7 SERVER_ERROR
  (HTTP 5xx from the service), 0 FAIL. The first run of the day had 35 failures.


### Fixes from live validation against the Equinix APIs (2026-11-11)
These were checked with a real account and with read-only calls only (GET, search, and
`dryRun=true` creates that leave no resource), plus public PeeringDB queries.

**Confirmed live**
- **The P0 fixes below work against the live service.**
  - IAM `roles().list()` returns 200 from `access.eqix.equinix.com`; the old host returns 404.
  - Operations finance accounts, reports, attachments and asset search return `page`.
  - An unfiltered Precision Time search returns 200; the old `GET /timeServices` returns 502.
  - Service-profile `ETag` is a quoted value (`"5"`), as `If-Match` expects.
  - STS discovery reads snake_case, and an STS error is `{"error","error_description"}`.
- **A service-provider connection now validates when the metro is set.**
  - Without `zSide.accessPoint.location.metroCode` it is rejected with
    `HTTP 400 EQ-3142501 "Invalid seller metroCode location"`.
  - The same dry run with it returns 200, through the raw API and through
    `zSideCloudProvider(adapter, linkProtocol, "SV")`.
  - The two-argument overloads now document this.
  - The README examples pass a metro.
- `/fabric/v4/agents` and `/agentTemplates` exist (200) although the published spec omits them.

**Fixed**
- **Order history** (`orderHistory().search`): the live service names fields differently from the
  spec, and those fields were silently null.
  - `account.number/name` now also read `accountNumber/accountName`.
  - `orderingContact` also reads `orderingContacts`.
  - Contacts also read `firstName`, `lastName`, `emailAddress`, `workPhone`, `username` and
    `contactUCId`.
  - New `getNotificationContacts()`, `getTechnicalContacts()` and `getOrderSource()`.
- **Metro Optimizer latency units.** Fabric `connectedMetros.avgLatency` is round-trip (live
  SV->DC 65.8 ms against a one-way fibre floor of about 26 ms).
  - The distance fallback was one-way and is now doubled, so measured and estimated latencies
    share one unit.
  - A site with no measurement and no coordinates was a fixed 150 ms guess. It is now *unknown*:
    it never excludes a metro under `maxLatencyMs`, is left out of scoring, and is rendered as
    unknown or `?`.
  - Reports label latencies as round-trip.
- **Live Equinix rate card (`EquinixRateCard`).** Every live price lookup failed with HTTP 400,
  so estimates silently fell back to reference figures.
  - Cause: it scanned `POST /prices/search` by `/type` alone, and the service requires connection
    type, bandwidth and both access-point types (or router package and metro).
  - It now sends one targeted, cached query per lookup: COLO (or CLOUD_ROUTER for `IP_VC`) to SP
    in the requested metro.
  - Lookups with no metro, an unmodelled connection type, or 0 Mbps defer to the next rate card
    without a request.
  - Live result: EVPL 1 Gbps DC is USD 200/month; Cloud Router LAB/STANDARD/ADVANCED in DC is
    0/1500/3000.
  - `PricingConnection.PricingAccessPoint` is now public with getters, so the A/Z metro can be
    matched structurally.
- **Peering Intelligence returned no presence for any network.**
  - PeeringDB ignores `netfac?asn=`, `local_asn=` and `asn__in=`, and returns unrelated rows (for
    example AS1299 for a query for AS15169). The client now resolves each ASN to its network id
    and queries `netfac?net_id=` / `net_id__in=`, dropping rows for other networks. Network
    lookups are cached, so no extra `/net` request is made.
  - The live `/fabric/v4/metros` response has no `ibxs` list, so the IBX->metro bridge was empty
    and every Equinix facility and IX was unresolved. `EquinixIXMapping` now also takes the live
    metro codes, so a name such as `Equinix DC1-DC15,DC21-DC22 - Ashburn` resolves to `DC`
    through its IBX prefix.
- **SavingsCalculator (single cloud)** now nets out the cloud provider's own interconnect port
  fee (reference rate, exact bandwidth tier only), as `TcoEngine` does. New
  `SavingsEstimate.cloudProviderPortMonthlyCost`, which MCP reports as
  `cloud_provider_port_monthly_cost`. Live AWS 1 Gbps DC: net USD 3081/month, not 3300.
  `ReferenceRateCard.cspInterconnectPortMonthlyExact(...)` added.
- **MCP `ops_list_open_tickets` always failed with HTTP 400.**
  - It filtered `productTypes: TROUBLE_TICKET`, which the live service rejects (`EQX-171-1212`).
    It now sends `TROUBLE`, the spec enum value.
  - It requests one row more than it shows, so truncation is reported.
  - A unit test that asserted the wrong value was corrected.
- **MCP schema text:** the `design_optimize_placement` / `design_plan_deployment` site schema no
  longer promises a 150 ms placeholder.
- **MCP version:** the shaded server jar now stamps `Implementation-Version`, so MCP `serverInfo`
  reports `3.0.0` instead of `0.0.0-dev`.

**Fixed from the live read-only tier** (each of these reads failed against the live service)
- **Response parsing no longer fails on fields the SDK does not model.**
  - The shared wire mapper now disables `FAIL_ON_UNKNOWN_PROPERTIES`, the same forward-compatibility
    it already applied to unknown enum values.
  - Without it, models lacking `@JsonIgnoreProperties` failed whole reads, and error bodies with an
    extra key lost their parsed details.
- **Fabric `GET /health`:**
  - `apiServices` is an array live; the spec types it as one object.
  - `HealthStatus.getAllApiServices()` added; `getApiServices()` returns the first entry.
- **Fabric `GET /portPackages` and Network Edge `GET /publicKeys`:** both return a bare array live,
  where the spec documents a `data` envelope. Both shapes are now read.
- **Network Edge device types / allowed interfaces / downloadable images:**
  - `DeviceManagement.LicenseOptions` had package-private, unannotated fields, so the live `"SUB"`
    key failed the read.
  - It is now public, with `@JsonProperty("SUB")`, `@JsonProperty("BYOL")` and getters.
- **IBX SmartView streaming subscriptions:**
  - `MessageType.meteredPower` reused the metered-power *reading* model (string `ibx`).
  - It is now `List<MeteredPowerMessageType>` (account + `ibx` array), the subscription filter the
    service returns.
- **Fabric connection / port `getMetrics(...)` and `Metrics.getMetricsByAssetId(...)`:**
  - These are sent without `limit`; `GET /{asset}/{id}/metrics` rejects any `limit` (HTTP 400
    EQ-3045002 for 100, 50 and 20).
- **Network Edge `DeviceCore.tier`** is an array for some device types live; the spec types it as
  an integer. Both shapes are read: `getTier()` returns the first value and `getTiers()` returns
  all of them.
- **Fabric `Metric.summary`** is an object in the live port-metrics response; the spec types it as
  a string. `getSummary()` returns text, or the JSON text of an object, and `getSummaryNode()`
  returns the raw node.
- **`SavingsEstimate.toMarkdown()`** shows the cloud-provider port line.
- **IBX SmartView asset calls:** they now fail fast with a named message when `accountNo`, `ibx`
  or `classification` is null, instead of throwing a `NullPointerException`; the service requires
  all three.
- **Live test inputs that the services reject:**
  - Fabric price search now uses the mandatory connection filters.
  - EIA v2 service search filters on `/state`; `/type` is not a supported filter property live,
    although the spec lists it.

**Observed, not changed**
- The live order-history product-type enum also contains `INBOUND_SHIPMENT` and
  `OUTBOUND_SHIPMENT`, which the spec lacks.
- `/fabric/v4/streams` returns 403 for this account, and `ne/v1/bgp` returns 403.
- Some Operations endpoints return 5xx intermittently (attachments 504, assets search 500
  with an empty body).
- **IAM project-scoped endpoints need an access-policy token.** `/v1/projects/{project}/roles` and
  `principalPolicies` reject a client-credentials token: with the `project:` prefix they return
  "only supported when using an access-policy auth token", and a bare UUID fails the project-id
  format. Use an STS-issued access-policy token.
- **IBX SmartView v1 calls require `accountNo`.** Hierarchy and legacy environmental calls answer
  HTTP 400 without it.
- **Fabric searches with mandatory filters.** Cloud-events and metrics searches require a
  `subject` (or project/organization) filter.
- **Digital LOA search requires a non-empty `filter.and`.** The `privateBetaAccess` and
  `betaTermsAgreement` endpoints return 404.
- **Live tests that pick resources they cannot use.** Some live-tier tests pick resources they
  cannot read (a route filter of another owner, a deleted connection). Others call operations the
  chosen device does not support (NE ping on a VyOS device). These are test-data issues, not SDK
  defects.

### Correctness fixes from the 2026-11-11 review (P0)
Each fix was checked against the current specifications (Fabric v4 4.34.0, network-edgev1, accessv1,
stsv1 and the Operations specs from `docs.equinix.com/.well-known/api-catalog`) and, where
noted, against the live service. Details: `docs/reviews/2026-11-11-deep-analysis.md`.

**IAM and STS**
- **Fixed: IAM and STS calls were sent to the wrong host.** IAM now goes to `https://access.eqix.equinix.com`
  and STS to `https://sts.eqix.equinix.com`, the hosts their specifications declare.
  `api.equinix.com` answers 404 for both (probed 2026-11-11). The OAuth login stays on `api.equinix.com`.
  - Each functional area can now declare its own `hostName` in its apiParams resource.
  - `EquinixClient.setDomainEndPoint(area, url)` overrides the host for one area.
  - `getEndPointFor(area)` resolves the host in this order: domain override, then
    `setEndPoint(...)`, then the catalogue host, then the default.
- **Fixed: STS token exchange.**
  - The form body now uses RFC 8693 field names (`grant_type`, `subject_token`, `subject_token_type`).
    The live service rejects the camelCase names in the spec's `TokenRequest` schema.
  - `StsToken` and `OpenIdConfiguration` also read the snake_case names the service returns
    (`access_token`, `expires_in`, `jwks_uri`, `token_endpoint`, ...).
- **Changed: unauthenticated STS operations send no credentials.** The token exchange,
  `accessPoliciesGranted`, JWKS and OpenID discovery no longer trigger an Equinix login and are sent
  without a bearer header.
  - Resources are flagged `"unauthenticated": true` in apiParams.
  - New `STS.withoutCredentials()` supports workload identity federation with no client secret.

**Pagination**
- **Fixed: four Operations lists stopped after the first page.** `GET /v1/attachments`,
  `GET /v1/finance/accounts`, `GET /v1/reportCenter/reports` and `POST /v1/assets/search` return
  their paging block as `page`, which was not read. `Page.pagination` now reads `page` as an alias.
- **Fixed: `loadAll()` / `next()` could loop forever** on `PaginatedList` and `PaginatedFilteredList`.
  Paging now stops when the server returns an empty page, or echoes back the offset it was already
  at, while `total` still promises more. A repeated page is not appended twice.

**Timestamps**
- **Fixed: some wire timestamps failed the whole response.** The parser accepted only 1, 2, 3 or
  6 fractional-second digits and no `+HH:MM` offset, so the spec's own example
  `2025-02-04T00:20:54.511118575Z` failed.
- **Changed: any 0 to 9 fractional digits and `Z`, `+HH`, `+HHMM` or `+HH:MM` offsets are now accepted.**
  A value that carries an offset or zone is normalised to UTC.

**Fabric**
- **Changed: `PrecisionTimes.list()` now calls `POST /fabric/v4/timeServices/search`.** The spec has
  no `GET /timeServices`.
  - New `PrecisionTimes.search(filter, sort)`; the spec requires a `/project/projectId` filter.
  - The internal `PrecisionTimeClient` now extends `PageablePost`.
- **Fixed: stream alert-rule update used `PATCH`.** It now uses `PUT /streams/{id}/alertRules/{id}`;
  the spec defines no PATCH on that path.
- **Added:** `AccessPointType.IC`, the value the 4.34.0 `AccessPointType` enum lists.
- **Deprecated, then removed later in this release:** `AccessPointType.XF_IC`, which is absent from 4.34.0. See Removed.
- **Added: `location.metroCode` on access points.** Every service-provider connection example in
  the spec sends it.
  - `AccessPointBuilder.metro(String | MetroCode)`.
  - `ConnectionBuilder.zSideCloudProvider(adapter, linkProtocol, metroCode)`.
  - `zSideAccessPointServiceProfile(uuid, linkProtocol, metroCode)`.
  - `CloudProviderConnectionAdapter.getMetroCode()` (`default`, returns `null`).
- **Fixed: `ServiceToken.changeLog` was always null.** It now also reads the spec's lowercase
  `changelog`.
- **Fixed: service-profile replace now sends the required `If-Match` header.**
  `PUT /serviceProfiles/{id}` takes its value from the `ETag` of a preceding GET. There is also a new
  overload that takes an explicit `If-Match` value.

**Network Edge**
- **Fixed: license-file uploads sent a JSON body.** `postLicenseFile(...)` (both forms) now sends
  `multipart/form-data` with a binary `file` part, the only content type the spec accepts.
  `metroCode`, `deviceTypeCode` and `licenseType` are now required, as in the spec.
- **Fixed: `updateLicenseToken(...)` failed on an empty `202 Accepted`.** It now returns `null`
  instead of a deserialization error. More broadly, map-shaped responses with an empty body map to
  an empty map.

**Intelligence MCP Server**
- **Changed (security): `fabric_confirm_change` fails closed** when the client did not declare
  elicitation, so no human prompt can be shown. It previously executed.
  `EQUINIX_MCP_ALLOW_UNCONFIRMED_MUTATIONS=true` restores the old behaviour for hosts whose
  tool-approval prompt is the human check.
- **Fixed: the confirmation prompt timed out after about 10 s whatever
  `EQUINIX_MCP_ELICIT_TIMEOUT_MS` said.** The MCP SDK's default request timeout overrode it; the
  server now sets the SDK request timeout above the elicitation timeout.
- **Changed: the confirmation prompt lists every field the create will send.** That includes
  notification emails, project and purchase-order IDs, VLANs and service-token access-grant
  settings. Values are rendered as escaped JSON literals.
- **Changed: the broker rejects mistyped spec values instead of dropping them.**
  - Known fields with the wrong JSON type, fractional or out-of-range integers, and strings
    containing control characters are rejected.
  - The spec a human approves, and the SHA-256 the token binds, now match what executes.
- **Fixed (security): debug request logging no longer writes credentials.**
  `client_secret`, passwords and access, refresh and subject tokens are masked in JSON and form
  bodies.

**Test environment**
- **Changed: live-tier credentials come from environment variables** (`EQUINIX_ACCESS_KEY`,
  `EQUINIX_SECRET_KEY`), no longer `-DaccessKey/-DsecretKey` system properties, which surefire
  copied into `TEST-*.xml`.
  - The integration profiles set `enablePropertiesElement=false`.
  - `run-live.ps1` no longer passes the keys on the command line.
  - Added `run-live.sh`.
  - A class skipped for missing credentials no longer fails with a NullPointerException in
    `@AfterAll`.
- **Changed: WireMock fixtures follow the specs.** That covers Operations `page`, ServiceToken
  `changelog`, multipart license upload and the STS RFC 8693 form and snake_case discovery.

### Cloud-to-cloud: native multicloud links, activation keys, Fabric v4 environment operations (Beta)
Every item in this section is **Beta**. Sources: the Connection Coordinator specification
([github.com/aws/Interconnect](https://github.com/aws/Interconnect), commit `bbfc763`, 2026-09-18),
the Fabric v4 catalog fetched 2026-09-21, and provider pricing and documentation pages read
2026-09-21. The SDK plans and prices a native provider-to-provider link. It does not create one:
it calls no AWS, Google Cloud, Oracle or Azure API and adds no cloud-provider SDK dependency.
Naming and scope: [ADR 0001](docs/adr/0001-interconnect-vocabulary.md).

**Added: `com.eqixiac.equinix.core.model.multicloud`** (new package; imports only `core`, the
JDK, Jackson and Lombok, enforced by `MulticloudPackageContractTest`):
- `ActivationKey`: sealed interface over `V1` (cleartext), `V2Encrypted` (envelope readable,
  ciphertext not exposed, no decryption) and `Opaque` (any input this release cannot interpret).
  `decode(String)` throws only for a null or blank argument. It accepts the RFC 4648 section 4 and
  section 5 alphabets, with or without padding, and ignores ASCII whitespace. `encode()` on a
  decoded key returns the issued string and does not re-serialize it. `toString()` on every variant
  omits `sharedConnectionUuid`, `destinationAccountId`, the ciphertext and the encoded form. The
  variants have no JavaBean getters and the type declares a Jackson serializer that writes `{}`,
  so any `ObjectMapper` (including one with the default `FAIL_ON_EMPTY_BEANS`) serializes a key
  as `{}`.
- `ProviderRef`, `ProviderSite`, `EnvironmentRef`, `BandwidthTier` (`coveringTier(int)` rounds a
  request up to the next listed size, Mbps).
- Wire enums with an `UNKNOWN` read-side fallback: `AdminState`, `VerificationState`,
  `ProvisioningState`, `MulticloudConnectionType`, `EnvironmentVisibility`.

**Added: Fabric v4 Beta surfaces** (catalog fetched 2026-09-21):
- `ServiceProfiles.getEnvironments(serviceProfileUuid)`:
  `GET /fabric/v4/serviceProfiles/{serviceProfileId}/environments`. Follows `offset`/`limit`
  pagination to the last page; a failure on any page propagates and no partial list is returned.
- `ServiceProfiles.validateActivationKey(serviceProfileUuid, environmentUuid, String)` and the
  `ActivationKeyDetails` overload: `POST .../environments/{environmentId}/actions` with
  `type = VALIDATE_ACTIVATION_KEY`. Returns `EnvironmentActionResponse`; `isKeyUnused()` is true
  only for state `INACTIVE`. The catalog publishes no response example, so which response
  properties the service populates is unverified.
- Both methods are `default` on the `ServiceProfiles` interface (the default throws
  `UnsupportedOperationException`), so existing implementations of the interface still compile.
- New models: `EnvironmentActionResponse`, `ActivationKeyDetails`, `EnvironmentActionRequest`,
  `ProviderEnvironmentRef`; enums `EnvironmentActionType`, `EnvironmentActionState`.
- `ProviderEnvironment` gains `supportedBandwidths` (Mbps), `metros`, `supportedFeatures`,
  `changeLog`. `MetroRef` reads `metroCode` as an alias of `code`, because the catalog example
  publishes both shapes for a `metros` item.
- `SimpleAccessPoint.AccessPointBuilder.activationKey(String)`, `.environment(String)`,
  `.environment(ProviderEnvironment)`; `ConnectionBuilder.zSideAccessPoint(SimpleAccessPoint)`.
  `activationKey` is a separate wire property from `authenticationKey`; neither is derived from
  the other. The catalog publishes no connection-create example that sets either new property.
- `CloudProviderConnectionAdapter.getActivationKey()`: `default`, returns `null`. The four
  built-in adapters do not override it, so the requests they build are unchanged.
- Read-side enum values: `AccessPointType.XF_IC`, `CloudRouterType.IC_ROUTER`,
  `ConnectionType.GW_VC`, `ConnectionType.IPX_VC`. The catalog publishes examples and no path or
  schema for the `XF_IC` resource.

**Added: value layer** (`design.value`):
- `RateCard.multicloudLink(MulticloudLinkRequest)` returns `Optional<MulticloudLinkQuote>`: one
  `PriceQuote` per provider side. `default` method returning empty, so existing `RateCard`
  implementations need no change. `LayeredRateCard` resolves each side independently; the earliest
  card that prices a side supplies it.
- `EgressPath.MULTICLOUD_INTERCONNECT`: the per-GB rate on a native link. The reference card
  returns 0 for AWS and Google Cloud (each with source URL and retrieval date) and empty for every
  other provider. The live provider-API adapters return empty for this path.
- `ReferenceRateCard` loads a second bundle, `/json/ratecard_multicloud_reference_2026_09.json`
  (`multicloudAsOf()`), holding only figures published on the providers' pricing pages on
  2026-09-21: AWS Interconnect - multicloud 10000 Mbps at path tier 1 (12.33 USD/h) and tier 4
  (51.78 USD/h); Google Partner Cross-Cloud Interconnect transport at 1000, 5000, 10000 and
  100000 Mbps in four transport locations. Lookups match the published size exactly and do not
  round, interpolate or extrapolate. A side without a published rate is empty with a reason; AWS
  1000 Mbps, Oracle Cloud and Azure are unpriced. Hourly rates convert at 730 h per month
  (`MulticloudLinkQuote.HOURS_PER_MONTH`). These rates are not stored under `cspInterconnectPort`.
- `CustomRateCard.Builder.multicloudLinkRate(...)` and `multicloudLinkHourlyRate(...)`, keyed by
  provider, bandwidth (exact match) and optional path tier.
- `TcoCalculator.Builder` and `SavingsCalculator.Builder`: `toCloud(CloudProviderType)`,
  `toRegion(String)`, `pathTier(int)` (1-5, default 1, an input), `useAwsFreeTier(boolean)`
  (default false), `reverseEgress(double, DataUnit)` (defaults to the forward volume). Lever
  conflicts are rejected at `compare()` / `calculate()` with an `IllegalArgumentException` naming
  the lever.
- `DeploymentArchetype.NATIVE_MULTICLOUD_INTERCONNECT`. It joins the default archetype set only
  when `toCloud(...)` is set, and `ON_PREM` then leaves it: the on-prem inputs carry no cloud
  egress, so a two-sided comparison cannot rank them against the other archetypes; an `ON_PREM`
  requested explicitly with a peer cloud is reported unpriced with that reason. With a peer cloud
  the internet, Equinix and native archetypes price traffic in both directions. A partially priced
  archetype is never recommended.
- `CostBreakdown.getProvenance()`, `TcoComparison.getTrafficNote()`,
  `SavingsEstimate.getMulticloudComparison()`, `SavingsEstimate.breakEvenSustainedMbps()`,
  `MulticloudPathComparison` (three path totals and
  `breakEvenSustainedMbps(nativeFixed, perGbPathFixed, perGbSumPerGbPath, perGbSumNative)`, Mbps,
  both directions summed, symmetric traffic). The break-even is returned whenever the two cost
  lines cross: both differences positive (native cheaper above the rate) or both negative (Equinix
  cheaper above it); `isNativeCheaperAboveBreakEven()` and the static
  `nativeCheaperAboveBreakEven(nativeFixed, perGbPathFixed)` give the sense, and
  `dominatesAtEveryVolume(...)` names the path that costs the same or less at every volume when
  they do not cross. The four per-GB rates are currency-checked before they are summed, whatever
  the declared volume. The comparison currency is the one the Equinix and native paths share when
  both are priced, so an internet path in another currency is withheld without withholding the
  break-even.
- `CustomRateCard.multicloudLink(...)` adds a quote note when `useAwsFreeTier` is set and the card
  prices the AWS side: the declared rate applies and the free tier is not modelled.
- `ActivationKey` declares a Jackson serializer that writes `{}`, so a default `ObjectMapper`
  (with `FAIL_ON_EMPTY_BEANS`) serializes a key as `{}` instead of throwing.
- Without `toCloud(...)` the TCO and savings Markdown output is byte-identical to the previous
  build (`SingleCloudOutputUnchangedTest`, golden files under `src/test/resources/golden/`).

**Added: optimizer and Deployment Wizard** (`design.optimizer`):
- `DeploymentWizard.Builder.cloudToCloudStrategy(CloudToCloudStrategy)` with values
  `EQUINIX_ONLY`, `NATIVE_WHEN_AVAILABLE`, `NATIVE_ONLY`, `COMPARE`. Default `COMPARE`: the Equinix
  connections are planned as before and each cloud pair with a catalog environment gets a
  `PlannedMulticloudInterconnect` with role `ALTERNATIVE`. `EQUINIX_ONLY` reproduces the previous
  plan, pricing and rendered output (`EquinixOnlyOutputUnchangedTest`). Also
  `multicloudEnvironments(MulticloudEnvironmentCatalog)` and `multicloudPathTier(int)`.
- `DeploymentPlan.getMulticloudLinks()` (nullable) and `multicloudLinksOrEmpty()`;
  `MulticloudLinkRole` (`ALTERNATIVE`, `REPLACEMENT`, `UNAVAILABLE`); `MulticloudLinkPricing`
  (native two-sided quote, Equinix fixed monthly cost for the same flow, break-even rate with
  `nativeCheaperAboveBreakEven` and the per-GB rates' `perGbCurrency`). The recommendation states
  which path costs less on each side of the break-even; when the fixed costs and the per-GB rates
  are not in one currency it states that no comparison is made. `plan()` and `reprice(plan)`
  resolve the rate card once and price the links and the plan from that instance (a live card
  fetches the price catalogue once).
- `PlanValueAssessment` / `PlanValueRealization`: a plan that depends on `REPLACEMENT` links
  subtracts `PlanPricing.getNativeReplacementMonthlyCost()` from the net
  (`getNativeLinkMonthlyCost()`, `getNativeLinkCurrency()`), prices egress from a cloud reached
  only through such a link at `EgressPath.MULTICLOUD_INTERCONNECT` (`ProviderEgressSaving.getPath()`),
  and withholds the aggregates when the fee is unpriced (`isNativeLinkUnpriced()`).
- `MulticloudEnvironmentCatalog` (`standard()`, `empty()`, `of(...)`, `load(InputStream)`,
  `with(...)`, `find(...)`, `regionsFor(...)`), `MulticloudEnvironment`,
  `MulticloudEnvironmentStatus`. Bundled data `/json/multicloud_environments_2026_09.json`, read
  2026-09-21: 8 AWS and Google Cloud region pairs (GA), AWS `us-east-1` with Oracle `us-ashburn-1`
  (GA), 4 AWS and Azure pairs (Preview, 1000 Mbps).
- `PlanPricing` native-link fields (`nativeAlternativeMonthlyCost`,
  `nativeReplacementMonthlyCost`, `perMulticloudLinkCost`, `unpricedMulticloudLinks` and the
  per-currency maps). None is part of `monthlyTotal`. `PlanPricing` is now
  `@Builder(toBuilder = true)`.
- `PlanValidator`: a native link is listed under skipped validations with a reason; an
  `UNAVAILABLE` entry under `NATIVE_ONLY` is a Layer-1 error.
- `DeploymentPlan.execute()` sends no request for a native link.
  `DeploymentOutcome.getInformational()` holds one non-recoverable entry per link
  (`resourceType "MulticloudInterconnect"`); it does not affect `isFullySuccessful()`.
- `MetroOptimizer.Builder.multicloudEnvironments(...)` and the informational finding
  `NATIVE_MULTICLOUD_ALTERNATIVE` (severity `INFO`, no score deduction).
- `TerraformExporter`: per native link, one comment block with the create-then-accept procedure
  and one `<name>_destination_account_id` variable (no default for a `REPLACEMENT` link,
  `default = null` otherwise, none for an `UNAVAILABLE` entry). No `resource` block and no
  cloud-provider `provider` block is emitted; the number of `resource` blocks equals
  `totalResourceCount()`. The header comment names the Equinix connections each `REPLACEMENT` link
  stands in for. Plan-supplied text written into a comment has its line breaks replaced by spaces,
  so it cannot end the comment (this also applies to the existing "not resolved" comments).
  Provider procedures are copies of AWS, Google Cloud and Oracle pages read 2026-09-21, printed
  with their source URLs. `TopologyDiagram` draws a
  native link as a dashed bidirectional edge between the two cloud nodes, never through a Cloud
  Router; its label is HTML-escaped like node labels. An `UNAVAILABLE` entry is not drawn.

**Added: Intelligence MCP Server tools** (`design` toolset; the catalog goes from 14 to 16 tools,
7 to 9 of them `design_*`; all read-only):
- `design_compare_cloud_to_cloud`: inputs `a` and `z` (cloud, region), `bandwidth_mbps`, optional
  `sustained_mbps` (both directions summed, 0 to 2 x `bandwidth_mbps`), `term_months`,
  `path_tier`, `use_aws_free_tier`, `metro_code`, `cloud_router_package`. Returns the
  `public_internet`, `equinix_fabric` and `native_multicloud_link` paths with per-side price
  source, `break_even_sustained_mbps` with `cheaper_above_break_even` (`native_multicloud_link` or
  `equinix_fabric`), `unpriced_components` and the catalog environment for the region pair. An
  unpriced figure is `null` with a reason.
- `design_list_multicloud_environments`: the bundled environment catalog, filtered by cloud and
  region; at most 50 entries per call.
- `design_estimate_tco` accepts `peer_cloud`, `peer_region`, `path_tier` and returns
  `traffic_note` and per-breakdown provenance when `peer_cloud` is set.
- `design_plan_deployment` accepts `deployment.cloud_to_cloud_strategy` (default `compare`) and
  `deployment.multicloud_path_tier`, and returns `native_multicloud_links` when a flow matched
  (each link's `pricing` carries `per_gb_currency` and, with a break-even,
  `cheaper_above_break_even`). A plan without cloud-to-cloud flows has an unchanged payload.
- `ServerContext.multicloudEnvironments()` (defaults to `MulticloudEnvironmentCatalog.standard()`;
  injectable through the builder). `TimeoutRateCard` applies the per-lookup timeout to
  `multicloudLink(...)`.

**Changed: Safe Mutation Broker** (`mcp.server.broker`, `mutate` toolset):
- `fabric_confirm_change` now consumes the token, verifies the spec hash, sends an MCP form
  elicitation to the client (change type, target, price context, proposal age, spec SHA-256) and
  executes only on an accept with `confirm = true`. `declined`, `cancelled`, `timed_out`
  (`EQUINIX_MCP_ELICIT_TIMEOUT_MS`, default 300000 ms) and `failed` execute nothing; the token
  stays consumed. A client that did not declare form elicitation is not prompted and the change
  executes as before, reported as `human_confirmation.status = unsupported_by_client`.
- Every confirm result and token error carries `confirm_checks` (`proposal_exists`,
  `not_expired`, `not_previously_used`, `spec_matches_binding`); confirm results also carry
  `human_confirmation {status, detail}` and `token_state`.
- New public type `mcp.server.HumanConfirmation` and
  `ServerContext.confirmWithHuman(String)`. Limit: `accepted` proves the client reported an
  acceptance, not that a person read the prompt.
- The `chg-` confirm token keeps its name and is documented as a process-local lookup key, not an
  activation key.

**Documentation:**
- README section "Design: Cloud-to-Cloud" and wizard subsection "Cloud-to-Cloud Flows: Native
  Multicloud Links (Beta)". Their Java examples are compiled with the test tree in
  `ReadmeCloudToCloudShowcase`. For 4.0.0 this material moved to
  [docs/design/cloud-to-cloud.md](docs/design/cloud-to-cloud.md), with figures recomputed from the
  2026-11 reference data.
- `docs/adr/0001-interconnect-vocabulary.md`: the seven meanings of "interconnect" in and around
  the SDK, the naming rule, the owner decisions of 2026-09-21 and the scoping rulings.

**Not included:** an implementation of the provider-to-provider specification (planned as the
separate artifact `com.eqixiac.interconnect:connection-coordinator-java`); any Equinix Fabric One
surface (no API reference published as of 2026-09-21); pricing for AWS sizes other than
10000 Mbps, for Oracle Cloud and for Azure.

## [3.0.0] - 2026-07-26

### MCP client removed; embedded Intelligence MCP Server added (breaking)
The SDK's MCP story is inverted: it no longer *consumes* Equinix's MCP server - it *is* one.

**Removed - the entire MCP client layer** (the root `Mcp` JSON-RPC client, `Fabric.mcp()` /
`Equinix.mcp()`, `McpClientConfig`, `McpException`, the `mcp.auth`/`mcp.bridge`/`mcp.model`
packages, the bundled `mcp-catalog.json`, and their test suites - 46 files). The honest
rationale, from a 2026-07-20 audit of the live service (`WWW-Authenticate` on
`mcp.equinix.com/fabric`, its protected-resource metadata, and the `as.equinix.com`
authorization-server metadata) and Equinix's MCP documentation:
- **It duplicated the SDK.** The remote Fabric MCP server's ~109 tools are 1:1 mirrors of
  Fabric REST operations this SDK already covers natively, with types, retries, and pagination.
  A Java program holding this SDK gains nothing by calling the same endpoints through a
  JSON-RPC detour.
- **Its auth model is structurally wrong for a headless library.** The live server accepts only
  OAuth 2.1 bearer tokens from `as.equinix.com` (authorization-code + refresh grants, PKCE,
  browser-based user consent - no `client_credentials` grant), so the SDK's own
  client-credentials tokens can never authenticate to it. A library cannot reasonably pop a
  browser; the private-beta service is designed for interactive MCP hosts, not SDKs.

**Added - the embedded Equinix Intelligence MCP Server** (`com.eqixiac.equinix.mcp.server`), a
*community* server (not affiliated with Equinix, unrelated to their private-beta Fabric MCP
server) that MCP hosts (Claude Desktop/Code, Cursor, VS Code) launch over **stdio only**,
executing through the SDK's typed clients and design engines under the operator's own
client-credentials (`EQUINIX_ACCESS_KEY`/`EQUINIX_SECRET_KEY`, or `.env.local`):
- **14 tools, engine-first by design** - a hard-capped catalog where every tool embeds engine
  logic or cross-domain reach, never mirroring the official Fabric catalog: 7 design tools
  (placement optimization, plan-only deployment planning, latency, TCO, live cloud-egress
  comparison, peering analysis, Terraform export), plus Customer Portal (open tickets, billing
  summary), Network Edge (device inventory), and IBX SmartView (environmentals, power events)
  reads. Toolsets are selectable via `EQUINIX_MCP_TOOLSETS`/`--toolsets`.
- **Safe Mutation Broker, strictly opt-in** (`mutate` toolset, off by default): two-phase
  creates only - `fabric_propose_change` runs the *real* spec-documented `dryRun=true`
  validation and mints a single-use, SHA-256-spec-bound, 10-minute confirm token;
  `fabric_confirm_change` executes only the stored spec. No update tools at launch, no delete
  tools ever. *Agents propose, dry-run diffs decide, humans confirm - enforced by the server,
  not the prompt.*
- **Packaging** - `mvn package` now also attaches a self-contained runnable jar
  (`equinix-sdk-java-<version>-mcp-server.jar`, maven-shade classifier `mcp-server`,
  `Main-Class: com.eqixiac.equinix.mcp.server.EquinixMcpServerMain`); the primary library
  artifact is untouched. The `io.modelcontextprotocol.sdk` dependencies (`mcp-core`,
  `mcp-json-jackson2`) and the bundled `slf4j-simple` binding are `<optional>`, so consumers'
  dependency trees stay clean - locked by a new architecture test (`DependencyHygieneTest`).

### Every spec-documented dry-run is now in the SDK
fabricv4 documents nine `dryRun=true` query-param operations; the SDK exposed two. The other
seven now have their own client surfaces (the spec audit also resolved the `$ref`'d shared params and
confirmed tags/searches carry **no** dry-run despite grep hits):
- **Fluent `.dryRun()` on updaters** - `connection.update()...dryRun().save()` and
  `serviceTokens().update(uuid)...dryRun().save()` (PATCH + `dryRun=true`, returns the simulated
  post-update entity).
- **Fluent `.dryRun()` on creators** - port create, cloudRouter create, network create
  (validated request echoed back without uuid/state; port dry-run create validates a real
  procurement order without placing it).
- **`ports().delete(uuid, dryRun)`** overload (returns the port that *would* be deleted) and
  `PortUpdater.dryRun()` - whose dry-run response is a paginated envelope unlike the real
  update's bare entity (spec quirk, handled + documented).
- **Regression locks**: every dry-run surface has a WireMock pair - one test proving
  `dryRun=true` reaches the wire on the exact endpoint, one proving the default path omits it - 
  so a future param-drop can never silently turn a dry-run into a real mutation. +6 live
  `integration-dryrun` tests use param-drop-safe payloads (no-op renames; cleanup-registered
  creates); port create/delete and EIA delete are deliberately WireMock-only because an
  accidental real execution would be unrecoverable.
- Verified-correct, unchanged: EIA v2 update/delete `dryRun` flags (already wired to spec);
  EIA v2 create has no dry-run in the spec; v1 delete dry-run intentionally unexposed
  (superseded by v2). The suspected NetworkEdge downtime-notifications wrong-shape was
  re-audited against the spec and refuted - no fix needed.

### Live integration tier made catalog-complete
All 33 API-catalog OpenAPI specs were swept operation-by-operation and every one of the 280
operations verified safe to run against a production account (GET reads, POST searches, and
spec-documented `dryRun`/validate mechanisms - 198 side-effectful lookalikes such as quote
requests and report-generation jobs were explicitly rejected) now has a live test:
- **+219 `integration-readonly` tests** (Fabric 104-op sweep, NetworkEdge 32, InternetAccess 25,
  IBX SmartView 25, CustomerPortal 54, Projects/IAM/STS 25) with live discovery chains for
  item-GET ids and optional `-D` overrides where the API offers no discovery path.
- **+4 `integration-dryrun` tests** - Fabric `POST /connections/validate`, routing-protocol
  validate, dry-run connection create (two-port EVPL over live-discovered free VLANs), and
  NetworkEdge backup restore-feasibility.
- **Fail-loud contract** - new `IntegrationTestBase.requireEntitled(...)` skips a live test only
  on 401/403 (credential lacks the product); deserialization errors, unmapped enums, and 5xx now
  FAIL instead of silently skipping (43 existing tests migrated). A green readonly run certifies
  the models against API reality, not just against the specs.
- **Legacy live suites consolidated** - the six pre-tier `*Test.java` live files (Fabric,
  NetworkEdge, CustomerPortal, IBXSmartView, InternetAccess, Projects) were folded into their
  `*IntegrationTest` successors and deleted; the obsolete `-Pintegration` Maven profile was
  retired (use `-Pintegration-readonly/-dryrun/-full`). Fixed a latent JUnit bug where static
  `@Nested` classes in `FabricIntegrationTest` could be discovered standalone and NPE without
  their outer `@BeforeAll`.
- New `IAMIntegrationTest`, `STSIntegrationTest`, `InternetAccessIntegrationTest`; EIA BGP
  service create moved behind the `integration-full` double opt-in with registered cleanup.

### Core redesign (breaking)
The core module was reviewed file-by-file (121 files, 123 findings, adversarially verified) and
every deferred redesign was executed:
- **Request model unified** - the `content`/`httpEntity`/`objectToSerialize` triple is one
  `RequestBody` abstraction; Apache `HttpEntity` no longer appears in any public core type; the
  dead `Request<T>` interface and `originalRequest` field are gone; `EquinixResponse` hides Apache
  internals (no `getHttpResponse`/`getEntity`), returns an unmodifiable case-insensitive header map.
- **`Page<T,S>` collapsed to `Page<J>`** (~200 sites); `ClientBase`'s legacy
  `buildRequest`/`buildRequestWithPathParams` String+TypeReference overloads removed (fluent
  builder + Class-typed forms remain); dead paged `TypeReference` fields deleted.
- **`Utils` split** into `RequestAssembler`/`ResponseHandler`/`ParameterMapper`/
  `SerializationHelper`; `Constants.objectMapper`/`JSON_CONVERTOR` are private (use
  `Constants.mapper()`/`converter()`); unresolved `{$token}` URI placeholders now fail fast naming
  the token and endpoint; `ResourceClientBase` can derive endpoint names by convention.
- **Fixed (highlights of 43 verified findings)** - multi-page `fabric.prices()` searches no longer
  throw `ClassCastException` (POST-search bodies share a `PaginatedPostBody` contract);
  `EquinixServiceException.getMessage()` no longer discards the constructor message and omits
  null fields; error-path HTTP responses are always closed; the `Host` header carries non-default
  ports; UTC is stamped correctly (no literal `Z` on local time) and date parsing is
  locale-fixed; `Page.items`/empty-response NPEs guarded; pagination state advances only after a
  successful fetch; `BandwidthDeserializer`/`OperationalStatusDeserializer` are forward-compatible
  instead of failing whole responses; `IPAddress.parse` validates literals strictly (no DNS
  lookups); `OAuthToken` uses a UTC clock and lost its public setters; the dead
  `OAuthRefreshClient`/refresh-grant machinery and orphaned `@SerializeOperation` filter machinery
  are removed; `RetryPolicy` no longer treats PATCH as idempotent by default.
- **New capabilities** - always-on per-request `X-Correlation-Id` (one id per logical request
  across retries, carried on `EquinixServiceException.getCorrelationId()` and in logs); opt-in
  `CircuitBreaker` via `EquinixConfig.circuitBreaker(...)` (consecutive-failure open, half-open
  probe, `CircuitOpenException`); typed `withX(IPAddress)` overloads on the NetworkEdge
  BGP/VPN/Device and Fabric route-aggregation builders.
- **Quality gates** - JaCoCo bundle floor raised 0.48 -> **0.75** line+instruction (combined
  coverage is 81%), a global 0.30 per-package floor added (plus the existing 0.70 IAM/STS client
  floors), and an opt-in `pit` profile adds PIT mutation testing over core.

### Added
- **Shipments v1 typed orders + locations** - `POST /v1/orders/shipment/{inbound,outbound,pendingStorage}`
  with per-variant fluent builders, plus shipment and work-visit `locations` lookups (`shipmentsv1`/
  `workvisitv1`). Work-order resources now combine v1 and v2 APIs to complete functionality; the
  API version is visible only in the request URL.
- **Prices `metroConnect` payload** - `METRO_CONNECT_PRODUCT` price entries now deserialize their
  metroConnect detail (built from the fabricv4 prices/search examples).
- `PublicKey.getKeyType()` (network-edgev1 `PublicKeyResponse.keyType`).

### Fixed
- **`APIParam` has a real contract now** - the interface's old `String toString();` declaration
  enforced nothing (every object has one); it now declares `paramValue()` (defaulting to
  `toString()`), the parameter-building helpers call it, and a new architecture test
  (`ApiParamContractTest`) scans every `APIParam` enum on the classpath asserting the query form
  equals the `@JsonValue` body form. Its first run caught a live wire bug: NetworkEdge
  `DeviceManagementType` sent `EQUINIX_CONFIGURED` (underscore) in query parameters but
  `EQUINIX-CONFIGURED` (the spec's hyphenated form) in bodies - fixed to one wire form.
- **POST-search pagination unified (one advance path, five call sites fixed)** - 
  `PaginatedList` and `PaginatedFilteredList` now share a single paging-advance dispatch:
  a `PaginatedPostRequest` whose body implements the (now behavioral) `PaginatedPostBody`
  contract (`seekPage(offset, limit)`) pages by re-pointing the body's own pagination shape;
  a `PaginatedRequest` pages via `offset`/`limit` query parameters; anything else fails fast
  with a clear unsupported-pagination error instead of a `ClassCastException` on page 2.
  Concretely: CustomerPortal `assets().search(...)` and IBX SmartView
  `systemAlerts().searchPost(...)` no longer explode on page 2; the basv2
  `BillingAccountSearchRequest` (top-level body `offset`/`limit`) and SmartView `SearchRequest`
  (body `pagination` member) now implement the contract per their specs; the EIA
  `services().search(...)` and `prices().search(...)` searches - whose specs paginate via
  query parameters, not the body - now page by advancing the query offset while re-sending
  the same filter body (multi-page was previously impossible by design). All five call sites
  are regression-locked with two-page wire tests.
- **Server-clamped page limits no longer skip records** - the paging pipeline used to advance
  the offset by the caller-requested page size; a server that clamps the limit (e.g. Fabric
  caps `limit` at 100, so `limit=500` returns 100 rows) silently skipped every record in
  between. Page 2 is now requested from the SERVER-reported pagination
  (`next offset = server offset + server limit`, with the server-honored limit), on both the
  query-parameter and POST-body paths; regression-locked on the wire (requested `limit=500`
  clamped to 100 -> page 2 asks for `offset=100`, not 500).
- **Every paginated list honored `offset` only when it was a multiple of `limit`** - the core
  pagination seeding quantized caller offsets to page boundaries (e.g. `offset=5, limit=100` -> 0),
  silently shifting the requested window on every PAGINATED endpoint in every domain (found via the
  IBX SystemAlerts search). Offsets now pass through exactly; regression-locked on the wire.

### Changed
- **Lombok adoption + over-use sweep** (149-item inventory, applied SDK-wide): 92 client facade
  impls now use `@RequiredArgsConstructor` (generated signatures verified byte-identical; the 122
  `super(...)`-calling impls correctly keep explicit constructors); `@Slf4j` replaces all manual
  loggers; utility classes are `final` with private constructors; `@Data` removed from all 15
  Jackson wire models (now `@Getter`, with explicit `@JsonProperty` where binding relied on
  setters); class-level `@Setter` removed from 52 read-only JSON models (IAM/IA/STS); every
  `@AllArgsConstructor` with adjacent same-typed parameters replaced by an explicit order-pinned
  constructor or `@Builder` (66 positional call sites migrated to named builder form - 
  eliminating the silent argument-transposition bug class); `RetryPolicy` gains a fluent
  `@Builder`.
- **Request-side builders are fully enum-typed** (fabric): port `connectivitySourceType`, service-token
  virtual-device `type`, Cloud Router `PackageRef`/`withPackage`/`changePackage`
  (`GatewayPackageCode`), marketplace-subscription + notification refs, stream alert-rule `type`,
  stream-subscription sink `credential.type` (String overload removed) and new
  `StreamSubscriptionSinkFormat` for `sink.format`; `Redundancy` now carries the spec's `enabled`
  member (constructor signature changed).
- **Client method naming aligned to the SDK-wide convention** (breaking renames):
  `STSOidcProviders.page(...)` -> `list(...)`, `patch(...)` -> `update(...)`;
  `IAMResourceTypes.pageResourceTypeActions(...)` -> `listResourceTypeActions(...)`;
  `DigitalLoas.patch(...)` -> `update(...)`, `findOrganizations(...)` -> `listOrganizations(...)`.
  The convention across all domains is `list` / `search` / `getByUuid` (or `getBy<Key>`) /
  `define`-or-`create` / `update` / `delete`; javadoc keeps citing the specs' operationIds.

### Removed
- **The nine single-implementation `*Config` interfaces** (`CoreConfig`, `FabricConfig`,
  `NetworkEdgeConfig`, ...) - each had exactly one implementation and one consumer (its own facade),
  no test doubles, no polymorphism; the facades now reference the `*ConfigImpl` wiring classes
  directly. The `Config` superclass stays: it is the polymorphic seam through which every internal
  client (`ClientBase`) reaches the shared transport.
- **CustomerPortal Resellers** - no specification exists anywhere in the API catalog (all 37 slugs
  checked); the resource was unverifiable fiction.
- **Fabric bulk connection create (`createBatch`) and the top-N port-statistics listing** - neither
  operation exists in fabricv4 (`POST /connections/bulk` has no path; only
  `GET /ports/{portId}/stats` is defined).
- **CrossConnect/Shipment/WorkVisit read models** - the v2 order APIs return a Location header only
  and no spec version defines resource reads (order status flows through Orders/OrderHistory).
- Fictional fields absent from the fabricv4/network-edgev1 schemas *and* examples: Fabric
  `Stream.enabled` (response side), `MarketplaceSubscription.type`, port-create `name`,
  `GatewayPackageCode.LIMITED`, and ~11 NetworkEdge response fields (each re-verified individually;
  spec-attested fields like `cvpId` and the NE request-side options were kept with citations).
- **`PasswordEquinixCredentials` and the `PASSWORD` grant type** - the OAuth2 resource-owner
  password grant reached end-of-life in January 2025 per the Equinix API release notes; the token
  endpoint only supports client credentials. Use `BasicEquinixCredentials` (or a custom
  `EquinixCredentialsProvider`).
- **`RoutingProtocolBuilder.withEquinixAsn(...)`** - Fabric release R2025.5 removed `equinixASN`
  from the BGP routing-protocol create request; the platform assigns the ASN and returns it on the
  response (`RoutingProtocol.getEquinixAsn()` is unchanged).

### Fixed
- **Exhaustive model-fidelity pass against the full API catalog** - every SDK model was diffed
  property-by-property (including nested objects, array items, request bodies, and enum value sets)
  against all 33 authoritative OpenAPI specs; 322 of 358 verified discrepancies fixed across 171
  files (+92 new model classes). Highlights: Connection/Port/Change status enums gained their
  missing spec values (`DRAFT`, `CANCELLED`, `PROVISIONED`, ...) with forward-compat `UNKNOWN`
  fallbacks; service tokens can now be created with `name`/`description`/`expirationDateTime`/
  `project` and virtual-device / network access-point selectors (`VXLAN`/`EVPN_VXLAN` link
  protocols included); `Order` supports `termLength` for term-discounted pricing; PrecisionTime
  create/read gained the `package`/`connections`/`ipv4`/MD5/PTP configuration family; Port
  responses expose their full schema (package, project, order, notifications, interface, LOAs, ...);
  Stream alert rules/subscriptions gained `metricSelector`/`resourceSelector`/`eventSelector` and
  the detection-method model; marketplace subscriptions gained `entitlements`/`trial`/`metroCodes`;
  misnamed wire mappings (silent nulls) were corrected (e.g. CloudEvent `id`); 4-byte-unsafe
  `Integer` ASN/bandwidth fields retyped to `Long`. The 36 remaining findings are deliberate holds
  (breaking public-API removals awaiting owner sign-off, or spec-vs-live-API conflicts) - see the
  session report.

### Added
- **Cross-source metro-registry enrichment** - `EquinixConfig.enrichMetroRegistry(true)` makes the
  metro registry the SDK's most complete location directory: alongside Fabric's metro-level data it
  pulls the EIA per-IBX catalogue (the only Equinix API with per-data-center coordinates; both EIA
  connection types unioned) over the same client transport. New `MetroRegistry.ibx(String)`,
  `ibxDetails(String)` and `isEnriched()` expose the merged records - ready for
  `SpeedOfLightLatency` IBX-to-IBX math without wiring up the InternetAccess domain yourself. The
  fetch is best-effort: an EIA outage never fails the registry load. Works on a standalone `Fabric`
  and flows through an `Equinix` session to `eq.metroRegistry()`. `MetroRegistry.refresh()` re-reads
  both sources at runtime and swaps the snapshot atomically in place, so existing references see
  new metros/IBXes immediately (`Fabric.reloadMetroRegistry()` now delegates to it).
- **SpeedOfLightLatency Metro/mixed overloads** - `millisBetween`/`distanceKm` now accept
  `(Metro, Metro)`, `(Ibx, Metro)` and `(Metro, Ibx)` in addition to `(Ibx, Ibx)`, normalizing the
  Fabric and EIA coordinate types internally; missing coordinates throw naming the offending
  IBX/metro.
- **Fabric Metro model completed against the spec** - `country`, `equinixAsn`,
  `localVCBandwidthMax`, `services[]`, `geoScopes[]`, `geoZones[]` and
  `connectedMetros[].remoteVCBandwidthMax` now deserialize (previously silently dropped).
- **PeeringDB API key option.** The Peering Intelligence entry points now resolve
  the PeeringDB credential in a documented order: an explicit `peeringIntelligence("key")` argument,
  then the new `EquinixConfig.peeringDbApiKey` option (works on a standalone `Fabric` and on an
  `Equinix` session, flowing through `eq.design()`), then the `PEERINGDB_API_KEY` environment
  variable, then anonymous access. Previously the key could only be passed as a method argument.

## [2.0.1] - 2026-07-01

Patch release superseding 2.0.0, which was unusable as a jar dependency.

### Fixed
- **Client init crashed when the SDK was consumed from the published jar** - classpath resources
  (`apiParams_*.json`) were resolved via `Path.of(url.toURI())`, which throws
  `FileSystemNotFoundException` for `jar:file:...!/` entries. Resources are now read through
  `getResourceAsStream`, which works from both a jar and loose files.
- `OptimizationResult.toJson()` returned an `{"error": ...}` blob for every engine result: it relied
  on Jackson module auto-discovery to serialize its `Instant` field, but `jackson-datatype-jsr310`
  was not on the classpath. The module is now a declared dependency and registered explicitly.
- Pinned `commons-codec` to 1.17.1, overriding the 1.11 pulled in transitively by httpclient
  (WS-2019-0379).

### Testing
- Exhaustive endpoint coverage across every domain (~520 new WireMock tests asserting URL, verb,
  query params, and request bodies), including the Design module's wizard engine
  (plan/execute/dryRun/rollback), the PeeringDB client HTTP paths, and the optimizer renderers.

## [2.0.0] - 2026-06-26

A major hardening and correctness release. **Breaking:** fictional/misattributed resources
were removed and the value-add engines moved to dedicated top-level modules.

### Full Equinix API catalog coverage
The SDK was audited against the **complete** Equinix API catalog (all published OpenAPI specs
at `docs.equinix.com/api-catalog`) and brought to spec-accurate coverage across every domain:
- **New domains:** **IAM** (`accessv1` - roles, role assignments, access policies + grants,
  permission sets, principal policies, policy masks, effective permissions, resource types) and
  **STS** (`stsv1` - token issuance, OIDC provider lifecycle + suspend/resume, JWKS/OpenID discovery).
- **New Fabric families:** IP Blocks, Agents + Agent Templates, Company Profiles + Tags, Stream
  Alert Rules, Stream Assets, Port Packages. Plus connection advertised/received-route search,
  Cloud Router route search + diagnostic commands + validate, Routing Protocol BGP actions,
  network connections, Service Profile/Token update + actions, Port search + VLANs, and Precision
  Time service packages.
- **Restored** (resources wrongly deleted as "fictional" before the full catalog was reachable):
  Support Cases (`supportv2`), Unified Notifications (`unifiednotificationv2`), Digital LOAs (`diloav1`).
- **Reshaped onto the real API:** Projects -> read-only `getprojectsv2` (`/resourceManager/v2/projects`);
  Secure Cabinets / Cross Connects / Shipments / Work Visits -> the `/colocations/v2/orders/*` order model;
  Order History -> `POST /v1/retrieve-orders`; Lookups -> `/colocations/v2/*`; Reports -> `/v1/reportCenter`;
  Notifications -> v1 IBX/network search; Assets -> `POST /v1/assets/search`; Cloud Events -> `POST /cloudevents/search`.
- **Internet Access:** completed the EIA v2 service lifecycle (get/update/delete/search + IBX
  availability) and added EIA v1 price search.
- **IBX SmartView:** added the `dcim/v3` Power Events resource (events + alert configurations).
- **Network Edge:** added device soft-reboot, RMA, device-level ACL, and generic file upload.

### Removed (fictional surface - not in the catalog)
- Fabric **Fabric Gateways** (renamed to Cloud Routers; no `/fabric/v4/gateways` path) and the
  fictional `MarketplaceSubscriptions.list()` / `RoutingProtocols` subnet-validate.
- Network Edge **DNS lookup** and the standalone **SSH Users** resource (SSH users are an embedded
  device sub-resource), plus Public Key get-by-id/delete (the API exposes only list + create).
- IBX SmartView fictional standalone **Power** resource (real power data is `power/v1/current`).

### Added
- **`Equinix` session - one client, many domains**: `new Equinix(creds)` owns a single OAuth token
  and connection pool and vends every domain over it - `eq.fabric()`, `eq.networkEdge()`,
  `eq.customerPortal()`, `eq.ibxSmartView()`, `eq.internetAccess()`, `eq.projects()`, `eq.iam()`,
  `eq.sts()`, `eq.design()`, `eq.mcp()` - so multi-domain apps no longer authenticate and pool per
  domain. Clients are lazily created + cached; a session-obtained domain's `close()` is a no-op
  (the session owns and closes the shared core). The standalone `new Fabric(creds)` constructors are
  unchanged.
- **`Design` facade**: `Design.over(fabric)` (or `eq.design()`) groups the value-add engines - 
  `optimizeMetros()`, `deploymentWizard(...)`, `peeringIntelligence([key])`, `savingsCalculator()`,
  `tcoComparison()` - in one discoverable root-level entry, reusing an existing Fabric client's
  transport (it is built from a `FabricGateway`, not credentials).
- **Fluent `update()` across all 13 mutable Fabric resources** (Network, CloudRouter, Stream,
  StreamSubscription, PrecisionTime, RouteFilter, RouteFilterRule, RouteAggregation,
  RouteAggregationRule, RoutingProtocol). Resources whose API uses RFC 6902 JSON Patch send a
  typed operations array with `application/json-patch+json`; full-body resources use a seeded
  builder. Exposed on each model/wrapper, e.g. `network.update().name("x").save()`.
- **Automatic retry/backoff** (`RetryPolicy`): retries 429/500/502/503/504 and transient IO
  errors with exponential backoff + full jitter, honoring `Retry-After` (both delta-seconds and
  HTTP-date forms). **Idempotent methods only by default** - POST is not retried, so a transient
  failure after a create cannot duplicate the resource; opt in with `retryNonIdempotentMethods`.
  Each retry is logged at WARN. Configurable via `EquinixClient.setRetryPolicy(...)`; on by default.
- **Token lifecycle**: authentication is now lazy (on the first API call) and the token is
  transparently re-acquired once it expires, single-flight under a lock so concurrent calls don't
  stampede the token endpoint. The explicit `authenticate()` still forces a fresh token.
- **Authentication options - password grant + pluggable credentials**: `PasswordEquinixCredentials`
  adds the OAuth2 resource-owner password grant (`grant_type=password`, with `user_name`/
  `user_password` and an optional `password_encoding`) for accounts that still require it (Equinix
  deprecates it in favour of client credentials). New `EquinixCredentialsProvider` interface,
  consulted on **every** authentication, lets credentials be resolved or rotated at runtime (e.g.
  from a secrets manager) without rebuilding the client; provider-accepting constructors were added
  to `Equinix` and every domain client (`new Fabric(provider)`, ...). **Breaking:**
  `EquinixStaticCredentialsProvider` no longer extends Apache HttpClient's `BasicCredentialsProvider`
  (it now implements `EquinixCredentialsProvider`); its vestigial `setCredentials(AuthScope, ...)`
  overload was removed.
- **Forward-compatible enum deserialization**: unknown API enum values no longer crash a
  response (map to a default/`null`/`UNKNOWN`).
- **`EquinixConfig` - unified construction options + metro auto-load**: a builder passed at
  construction (`new Fabric(creds, EquinixConfig.builder()...build())`, likewise `Equinix` and every
  domain) that consolidates the previously-scattered options - `sandbox`, `retryPolicy` - and adds
  `autoLoadMetros` (default `true`): an explicit `authenticate()` (or `Equinix.authenticate()`) now
  eagerly loads the metro catalogue (`metroRegistry()`) up front, best-effort, so it is ready before
  first use; set `autoLoadMetros(false)` to keep it lazy. The plain `(creds)` / `(creds, sandbox)`
  constructors remain and are equivalent to a config differing only in `sandbox`.
- **Forward-compatible metros - `MetroId` + live `MetroRegistry`**: a metro Equinix adds after this
  SDK was built is now fully usable without an enum entry. `MetroId` (`core.model`) is a normalized
  metro-code value object that names any metro (`MetroId.of("ZZ")`, `MetroId.of(MetroCode.SV)`,
  `asMetroCode()`/`isKnown()`); `MetroCode` gains `lookup(String)`/`fromCode(String)`. The `Metro`
  model now preserves the exact wire code: `metro.metroId()` returns the real code even when
  `metro.getCode()` is `UNKNOWN` (the raw code is no longer discarded), and `Metro.getIbxs()` exposes
  a metro's IBX data centers. `fabric.metroRegistry()` is a cached, refreshable snapshot of every
  metro (and its IBXs) from the Metros API, keyed by `MetroId` - the authoritative, always-current
  set - with `get`/`contains`/`all`/`ibxs`. `Metros.getByMetroCode(String)`/`getByMetroId(MetroId)`
  overloads fetch an unlisted metro directly (the create builders' `inMetro(String)` already did).
  **Minor breaking:** new methods `metroId()` and `getIbxs()` on the public `Metro` interface (affects
  only code that implements `Metro` itself, not callers).
- **Metro Optimizer is `MetroId`-keyed end to end**: the optimizer/wizard scoring graph
  (`metroMap`/`latencyMap`/`providerMetroMap`) and every result/input type now key by `MetroId`
  instead of `MetroCode`, so brand-new metros (absent from the enum) are ranked distinctly rather
  than colliding on `UNKNOWN`. `ConnectedMetro` gains a forward-compatible `metroId()`; the site and
  constraint builders accept `String`/`MetroId` (e.g. `nearestMetro("ZZ")`, `requireMetro("ZZ")`)
  alongside `MetroCode`; result types (`MetroRecommendation`, `MetroCostBreakdown`, `LatencyMatrix`,
  `ProviderConnectivityMap`, `DeploymentTopology`, ...) expose `MetroId`. Rate-card pricing remains
  `MetroCode`-keyed, so an unlisted metro prices via the card's fallback. (Breaking: the optimizer
  model types' metro accessors are now `MetroId`-typed.)
- **Peering Intelligence is `MetroId`-keyed end to end**: the IX/facility->metro bridge
  (`EquinixIXMapping`), the analysis engine, the presence matrix, the resiliency assessment, and
  every result/request model now key by `MetroId` built from the live `Metro.metroId()` - so a
  metro that is live in Fabric but not yet in the enum binds its IBXs and stays distinct instead of
  collapsing to `UNKNOWN`. The IBX-name bridge's metro-prefix fallback is gated on the live metro
  set (a new metro's prefix resolves; a stray token does not). The `PeeringIntelligence` builder
  keeps `customerMetros(MetroCode...)` and adds `MetroId.../String...` overloads. (Breaking: peering
  result/model metro accessors are now `MetroId`-typed.)
- **Speed-of-light latency calculator** (`design.geo.SpeedOfLightLatency`): estimates fibre latency
  from geographic distance - great-circle haversine × speed of light in fibre (`c / n`, ~4.9 µs/km
  one-way). Configurable refractive index and route-inflation factor; **round-trip (RTT) by default**,
  one-way optional. Computed **IBX-to-IBX**: `millisBetween(Ibx, Ibx)` / `distanceKm(Ibx, Ibx)` use
  each Equinix data center's own `geoCoordinates` (from `internetAccess.ibxs()`), so two IBXes in the
  same metro have a real, non-zero latency rather than collapsing to a metro centroid. Surfaced in
  peering: the diversity distance and `DiversityScore.estimatedRttMs` are now grounded in the centroid
  of each metro's actual Equinix IBX data centers (PeeringDB facility coordinates), falling back to the
  Fabric metro centroid only when no facility coordinates are available; the diversity explanation
  quotes the "~X ms RTT floor".
- **Fail-fast endpoint validation**: an unknown apiParams endpoint now throws a clear error
  instead of silently dispatching a malformed request.
- **`CustomerRoute` interface** (`internetaccess.model`): a shared read-only view of the EIA v1
  customer-route shape (`importPolicy` + `prefixLength`) now implemented by both
  `DedicatedPortCustomerRoute` and `VirtualConnectionCustomerRoute`. Additive (existing getters
  unchanged). This was the only model-consolidation found safe to apply without a public-surface
  change in a full model-layer near-duplicate audit; the remaining candidates are genuinely distinct
  wire contracts or would re-type public getters (a 2.0 break) and were left as-is.
- **WireMock coverage** for 30+ previously-untested resources (request-contract `verify(...)`).
- **Fabric Connection validation**: `Connections.validate(FilterPropertyList)` ->
  `POST /connections/validate` (validate a provider auth key or VLAN availability before creating).
- **Fabric Metrics API**: `Fabric.metrics().search(...)` (`POST /metrics/search`) plus per-asset
  `Connections.getMetrics(...)` / `Ports.getMetrics(...)` (`GET /{uuid}/metrics`) - the successor to
  the deprecated `/stats` statistics endpoints.
- **Customer Portal Orders actions**: `getNegotiations`/`replyNegotiation`/`addNote`/`cancel` on
  `Orders` (the real `colocations/v2` order sub-actions).
- **Customer Portal SmartHands** typed order builders: 12 typed creates
  (`createEquipmentInstall`, `createShipmentUnpack`, ...) plus `listTypes()`/`listLocations()`.
- **Typed async waiter** (`ResourceWaiter`): poll a resource until a target state
  (`PROVISIONING`->`PROVISIONED`), with timeout/failure conditions.
- **IaC / Terraform export** (`design.export.TerraformExporter`): turn a `DeploymentPlan`
  into Equinix Terraform-provider HCL (cloud routers, connections, routing protocols).
- **Topology diagrams** (`design.export.TopologyDiagram`): GitHub-renderable Mermaid graphs
  from a `DeploymentPlan` or `OptimizationResult`.
- **Async / virtual-thread client facade** (`core.async.EquinixAsync`): run any SDK call on a
  Java 21 virtual thread, returning a `CompletableFuture`, without mirroring the API surface.
- **Value realization - cost / savings / TCO modeling** (`design.value`, `fabric.savingsCalculator()`,
  `fabric.tcoComparison()`, `plan.valueRealization()`): quantify the cost case for interconnection.
  - **Layered, provenance-tagged rate cards** (`design.value.ratecard.RateCard`, combined in a
    precedence chain via `RateCard.layered(...)`): `EquinixRateCard` reads **live `fabric.prices()`**
    (narrowed server-side by price `/type` to the connection + gateway product families);
    `CustomRateCard` takes caller-supplied rates fluently with **per-metro / per-term granularity**
    and most-specific-match resolution, including **Equinix colocation primitives** (cabinet, power
    per kW, cross-connect) so the physical-infrastructure side of a colo-vs-cloud comparison uses
    real figures; the TCO calculator also accepts arbitrary `additionalLineItem(...)`s (compute,
    storage, ...) folded into every priced archetype. `ReferenceRateCard` ships dated indicative figures; and the
    `design.value.ratecard.provider` adapters source **live cloud-egress rates** from the public
    Azure Retail Prices, AWS Price List (data-transfer **and** Direct Connect, so both INTERNET and
    PRIVATE paths), GCP Cloud Billing Catalog, and Oracle Cloud (OCI) Price List APIs (opt-in,
    fault-tolerant; parsers verified against the live Azure/AWS responses). Every quote carries a
    `PriceSource` (`EQUINIX_LIVE` / `CUSTOM` / `PROVIDER_API` / `REFERENCE` / `ESTIMATE` / `COMPOSITE`).
  - **Savings calculator**: public-internet vs private-interconnect egress savings (net / annual /
    first-year, break-even GB, payback) -> `SavingsEstimate`.
  - **TCO comparison**: Public Cloud (internet) vs On-Prem vs Equinix Interconnect, recommending the
    cheapest with savings vs baseline; on-prem inputs overridable.
  - **Plan value realization**: per-provider egress savings netted against a `DeploymentPlan`'s real
    interconnect cost (no double-counting).
  - The Metro Optimizer's cost estimate now uses live rate-card pricing (heuristic fallback tagged
    `ESTIMATE`) and exposes per-metro + aggregate `PriceSource` provenance on `CostEstimate` /
    `MetroCostBreakdown`.

### Changed
- **Consistent response-model naming - `*Ref` / `*Summary` / full (breaking)**: the unevenly-applied
  `Minimal*` / `Basic*` / `Simplified*` prefixes are replaced by one convention - a bare embedded
  reference (uuid/href, or code-keyed identity) is `*Ref`; a reference plus a few list/display fields
  is `*Summary`; the complete resource carries no prefix; nested value objects keep descriptive
  names. 18 renames, e.g. `MinimalPort`->`PortRef`, `MinimalMetro`->`MetroRef`, `BasicMetro`->
  `MetroSummary`, `BasicPort`->`PortSummary`, `SimplifiedNetwork`->`NetworkSummary`, `SimplifiedRouter`->
  `CloudRouterSummary`, and the two mislabeled abstract bases `BasicOrderInfo`->`OrderRef` /
  `BasicChangeInfo`->`ChangeRef`. (`MinimalLocation`->`LocationCode`, a metro-code value object;
  `BasicEquinixCredentials` is unrelated to the scheme and unchanged.) Wire contracts are untouched - 
  these are pure type renames.
- **Peering Intelligence metro bridge is now fully live (no hardcoded city table)**: the
  PeeringDB->Equinix-metro mapping previously relied on a ~60-entry hardcoded `city->MetroCode` table
  and a hardcoded `metro->region` switch. Both are gone. Each Equinix facility name carries its IBX
  code (`LA4`, `SV5`, `DC11`, ...) whose prefix is the metro, so facilities are resolved by reading the
  IBX code from the PeeringDB name and looking it up in the **live IBX->metro map** built from
  `fabric.metros().getIbxs()` (exact hit wins; metro prefix is the fallback). Each resolved facility
  seeds a city->metro map through which PeeringDB IXes (city-only) resolve - so city aliases (a
  "San Jose" exchange in the `SV` facilities) map correctly with zero static data. Region comes from
  the live `metro.getRegion()`; the geographic-**distance** measure (haversine over live coordinates)
  is the primary diversity signal, with region as a secondary descriptor. New metros/IBXs are bridged
  automatically as Equinix adds them. (`EquinixIXMapping` now takes the live IBX->metro map at
  construction; its static `resolveMetroFromCity`/`getCityToMetroMap` are removed.)
- **Modules extracted out of `fabric.*`:** Metro Optimizer + Deployment Wizard + Peering
  Intelligence -> `com.eqixiac.equinix.design.*`; MCP bridge -> `com.eqixiac.equinix.mcp.*`.
  `Fabric.optimizeMetros()/deploymentWizard()/peeringIntelligence()/mcp()` remain as accessors.
- **Root-level `Mcp` client**: the standalone MCP JSON-RPC client (`mcp.McpClient`) is promoted to
  `com.eqixiac.equinix.Mcp` - `new Mcp(creds)` - so every credential-constructed client sits in
  the root package alongside `Fabric`, `NetworkEdge`, `IAM`, etc. The Fabric-integration bridge
  stays behind `Fabric.mcp()`; supporting types remain in `com.eqixiac.equinix.mcp.*`.
- **`FabricGateway` interface** - the value-add design engines now depend on a narrow read/build
  interface (`metros()`/`serviceProfiles()`/`cloudRouters()`/`connections()`/`routingProtocols()`/
  `prices()`) rather than the concrete `Fabric`; `Fabric implements FabricGateway`, so existing
  callers are unaffected.
- **Pagination is now `Iterable`, not `List`** (breaking): `PaginatedList<T>` and
  `PaginatedFilteredList<T>` implement `Iterable<T>` and expose `stream()`/`get(int)`/`size()`/
  `isEmpty()`/`toList()` alongside `hasNextPage()`/`next()`/`loadAll()`/`getPagination()`, instead
  of extending `ArrayList`. A server-returned page is no longer a mutable collection (matching the
  AWS/Google/Stripe SDK idiom). Migration: `new ArrayList<>(page)` -> `new ArrayList<>(page.toList())`;
  iteration, `stream()`, `get(int)` and `size()` are unchanged.
- **`RouteAggregations.list()` -> `search()`** (breaking): route aggregations are exposed via
  `POST /routeAggregations/search` (the only endpoint the API provides), returning a
  `PaginatedFilteredList`; the old `list()` targeted a non-existent GET path and threw at runtime.
- **JSON Patch support in core**: per-request content-type + `PatchOperation` model + `patchOne`.
- **Connection `update()`** exposed (`PATCH /connections/{uuid}`, JSON Patch) - previously
  plumbed but unreachable.
- **Customer Portal Orders -> EIA-correct shape** (breaking): base path corrected to
  `colocations/v2/orders`; the fictional `Orders.list()`/`create()` (no such ops in `ordersv2`;
  listing lives in `OrderHistory`) were removed.
- **Customer Portal SmartHands reshape** (breaking): the generic `list/get/create/update` CRUD
  (which the API does not provide) is replaced by the 12 typed order builders + `listTypes`/
  `listLocations`; `SmartHands`/`SmartHandsType`/`SmartHandsStatus` removed.
- **InternetAccess -> EIA v2** (breaking): collapsed to the single nested
  `POST /internetAccess/v2/services` create (`ServiceRequest` with nested routing-protocol/IP-block
  body); the prior 5-op v1 CRUD shape is gone; enums renamed to `ServiceTypeV2`/`ServiceState`.

### Deprecated
- **`Connections.getStatistics(...)` / `Ports.getStatistics(...)`** (the `/stats` endpoints, marked
  deprecated upstream) -> use `getMetrics(...)` / `Fabric.metrics().search(...)`. The `/stats` wiring
  remains for back-compat.

### Fixed
- **~20 endpoint-name mismatches** where client code referenced endpoints absent from
  `apiParams` (silent runtime failures) - notably Fabric PrecisionTime (entirely non-functional),
  Stream/StreamSubscription/RouteAggregation/RoutingProtocol updates, the NetworkEdge BGP delete
  and PublicKey delete (missing endpoints added), CloudEvents/RouteFilterRule listing, and the
  Customer Portal WorkVisit/TroubleTicket create names.
- **Stream-subscriptions path** (`/streamSubscriptions` -> `/subscriptions`, spec-verified).
- Operator builders that initialized accumulator lists with an immutable `singletonList` then
  mutated them (`UnsupportedOperationException`); `ResellerWrapper` getters returning `null`
  and defeating `@Delegate`; `MetrosImpl.list(presence)` ignoring its argument.
- Corrected the unverifiable "60+ MCP tools" claim; removed the dead `withMcpEnrichment(McpBridge)`
  levers on `PeeringIntelligence.Builder` and `MetroOptimizer.Builder` - both fetched MCP data the
  engines then discarded. (`DeploymentWizard.Builder.withMcpValidation(...)` is unaffected; it
  remains a real, wired MCP integration.)

### Removed (breaking)
- **Fictional/misattributed surface**: the entire `Messaging` domain (covered by Fabric Stream
  Subscriptions + Customer Portal Notifications); InternetAccess `RoutingConfigs`/`Ports` (no
  EIA v2 counterpart); Customer Portal `SupportCases`/`DigitalLOAs`/`UnifiedNotifications`/
  `BillingCredits`; assorted dead code (`ModelHelper` classes, unused `serviceManager` fields).

## [1.3.0] - 2026-03-25

### Added - MCP Bridge Module
- **MCP Client** (`fabric.mcp`): JSON-RPC 2.0 client for Equinix MCP servers with OAuth2 token management, automatic retry with backoff, and error mapping to the SDK exception hierarchy
- **McpBridge**: High-level facade with typed sub-bridges for metros, connections, Cloud Routers, and observability
- **McpMetroBridge**: Typed `McpMetro` objects from `get_metro` and `list_metro` MCP tools
- **McpConnectionBridge**: Typed `McpConnectionValidation` and `McpConnection` from connection validation and search tools
- **McpCloudRouterBridge**: Typed `McpCloudRouter` objects with router package lookup and diagnostic commands
- **McpObservabilityBridge**: Typed `McpMetrics` and `McpStream` objects for live telemetry access
- **McpClientConfig**: Configurable endpoints, timeouts, and retry policy with sensible defaults
- **McpTokenManager**: Standalone OAuth2 token lifecycle with 5-minute pre-expiry refresh
- **McpException**: MCP-specific exception extending `EquinixClientException` with JSON-RPC error codes
- **Fabric.mcp()**: Lazy-initialized accessor for the MCP bridge, following the existing pattern
- **Fabric.mcp(McpClientConfig)**: Overload accepting custom MCP configuration

### Added - MCP Integration with Existing Modules
- **MetroOptimizer.Builder.withMcpEnrichment(McpBridge)**: Optional real-time metro data enrichment via MCP during optimization
- **DeploymentWizard.Builder.withMcpValidation(McpBridge)**: Optional connection validation via MCP before deployment planning
- **PeeringIntelligence.Builder.withMcpEnrichment(McpBridge)**: Optional live connection state enrichment for peering analysis
- All MCP integrations are fully optional with graceful fallback when MCP is unavailable

### Added - Test Suite
- WireMock-based unit tests for all 7 API domains (Fabric, Network Edge, Customer Portal, IBX SmartView, Internet Access, Projects, Messaging)
- MCP Bridge WireMock tests covering initialization, tool listing, typed bridge responses, and error handling
- Core infrastructure tests: OAuth2 authentication, resiliency patterns, error mapping
- JSON fixtures for all test scenarios (`src/test/resources/json/`)
- Test profiles: `mvn test -Pwiremock` for API simulation tests, `mvn test -Pintegration` for live tests

### Changed
- Upgraded Java source/target from 14 to 21
- Updated README with MCP Bridge documentation, WireMock test instructions, and Java 21 badge

## [1.2.0] - 2026-03-15

### Added
- Metro Optimizer - intelligent placement engine with 12-phase scoring pipeline
- Deployment Wizard - optimization-to-execution deployment planning
- Peering Intelligence - PeeringDB integration with presence matrices and resiliency analysis
- Cloud Provider SDK Interoperability adapters (AWS, Azure, GCP, Oracle)

## [1.1.0] - 2026-02-19

### Added - New Domains
- **Internet Access**: Services, Ports, and Routing Configurations
- **Projects**: Project management
- **Messaging**: Notification subscriptions and events

### Added - Fabric Expansion
- Cloud Routers with router packages
- Streams and Stream Subscriptions for real-time data
- Route Filters and Route Filter Rules
- Networks (EVPLAN, EPLAN, IPWAN)
- Route Aggregations and Route Aggregation Rules
- Routing Protocols
- Precision Time services
- Marketplace Subscriptions
- Fabric Gateway packages
- Cloud Events
- Health endpoint
- **Dry Run** validation on Connection and ServiceToken builders (`dryRun()`)
- Prices with filtering
- Connection and Port statistics

### Added - Customer Portal Expansion
- Invoices (summaries and details)
- Resellers and reseller customers
- Cross-Connects (full CRUD)
- Orders (full CRUD)
- Trouble Tickets (full CRUD)
- Work Visits (full CRUD)
- Smart Hands requests (full CRUD)
- Shipments (full CRUD)
- Notifications
- Assets
- Support Cases (full CRUD)
- Quotes (full CRUD)
- Support Plans
- Order History
- Lookups (locations, patch panels)
- Attachments (full CRUD)
- Reports
- Digital LOAs (full CRUD)
- Secure Cabinets (full CRUD)
- Unified Notifications
- Billing Credits

### Added - IBX SmartView Expansion
- Streaming Subscriptions (full CRUD with AWS IoT, Azure Event Hub, Webhook, REST channels)
- System Alerts (search with filtering)
- Hierarchy APIs (location and power hierarchy)
- Smart View Assets (list, details, search, tag points)
- Legacy Environmental APIs (current readings, trending data)
- Legacy Power APIs (current readings, trending data)
- New enums: LevelType, AssetClassification, ChannelType, StreamingMessageType, AlertStatus, DataPoint

### Added - Core Improvements
- Fluent `EquinixRequestBuilder` replacing verbose request construction
- Exception hierarchy (401/403/404/409/429/5xx)
- `@Delegate` pattern on all wrapper classes
- Per-resource `defaultVersion` override in apiParams JSON
- `overrideUriFormat` for legacy SmartView URI patterns
- Rich-object builder overloads (22 methods across 13 operator files)
- `ResourceRef` base class eliminating model duplication

### Changed
- Upgraded Jackson from 2.9.9 to 2.17.2
- Upgraded JUnit from 5.5 to 5.10.3
- Upgraded HttpClient to 4.5.14
- Upgraded Lombok to 1.18.34
- Upgraded maven-javadoc-plugin from 2.9.1 to 3.6.3
- Removed Guava and Ebean dependencies
- Moved domain-specific enums from core to fabric package
- Added Mockito 5.11.0 and WireMock 3.5.4 for unit testing

### Fixed
- `Utils.addRequestParams()` now respects per-resource-group `defaultVersion` in apiParams JSON
- CustomerPortal v1 endpoints (SmartHands, Assets, OrderHistory, etc.) now correctly resolve to `/v1/` instead of `/v2/`
- `Account.accountNumber` getter shadowing bug in NetworkEdge
- `DeviceJson` field shadowing from parent class

### Removed
- Removed dead code from core module
- Removed unused enums from core package (6 deleted)

## [1.0.0] - 2021-01-01

### Added
- Initial release with Fabric and Network Edge support
- Fabric: Connections, Ports, Service Tokens, Service Profiles, Metros
- Network Edge: Devices, SSH Users, ACL Templates, VPNs, BGP Peerings, Device Links, Public Keys, Backups, Setup
- IBX SmartView: Environmental sensor readings, Power readings
- Core: OAuth2 authentication, pagination, exception handling
