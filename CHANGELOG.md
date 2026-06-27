# Changelog

All notable changes to the Equinix Java SDK will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.0.0] - 2026-06-26

A major hardening and correctness release. **Breaking:** fictional/misattributed resources
were removed and the value-add engines moved to dedicated top-level modules.

### Added
- **Fluent `update()` across all 13 mutable Fabric resources** (Network, CloudRouter, Stream,
  StreamSubscription, PrecisionTime, RouteFilter, RouteFilterRule, RouteAggregation,
  RouteAggregationRule, RoutingProtocol). Resources whose API uses RFC 6902 JSON Patch send a
  typed operations array with `application/json-patch+json`; full-body resources use a seeded
  builder. Exposed on each model/wrapper, e.g. `network.update().name("x").save()`.
- **Automatic retry/backoff** (`RetryPolicy`): retries 429/500/502/503/504 and transient IO
  errors with exponential backoff + full jitter, honoring `Retry-After` (both delta-seconds and
  HTTP-date forms). **Idempotent methods only by default** — POST is not retried, so a transient
  failure after a create cannot duplicate the resource; opt in with `retryNonIdempotentMethods`.
  Each retry is logged at WARN. Configurable via `EquinixClient.setRetryPolicy(...)`; on by default.
- **Forward-compatible enum deserialization**: unknown API enum values no longer crash a
  response (map to a default/`null`/`UNKNOWN`).
- **Fail-fast endpoint validation**: an unknown apiParams endpoint now throws a clear error
  instead of silently dispatching a malformed request.
- **WireMock coverage** for 30+ previously-untested resources (request-contract `verify(...)`).
- **Fabric Connection validation**: `Connections.validate(FilterPropertyList)` →
  `POST /connections/validate` (validate a provider auth key or VLAN availability before creating).
- **Fabric Metrics API**: `Fabric.metrics().search(...)` (`POST /metrics/search`) plus per-asset
  `Connections.getMetrics(...)` / `Ports.getMetrics(...)` (`GET /{uuid}/metrics`) — the successor to
  the deprecated `/stats` statistics endpoints.
- **Customer Portal Orders actions**: `getNegotiations`/`replyNegotiation`/`addNote`/`cancel` on
  `Orders` (the real `colocations/v2` order sub-actions).
- **Customer Portal SmartHands** typed order builders: 12 typed creates
  (`createEquipmentInstall`, `createShipmentUnpack`, …) plus `listTypes()`/`listLocations()`.
- **Typed async waiter** (`ResourceWaiter`): poll a resource until a target state
  (`PROVISIONING`→`PROVISIONED`), with timeout/failure conditions.
- **IaC / Terraform export** (`design.export.TerraformExporter`): turn a `DeploymentPlan`
  into Equinix Terraform-provider HCL (cloud routers, connections, routing protocols).
- **Topology diagrams** (`design.export.TopologyDiagram`): GitHub-renderable Mermaid graphs
  from a `DeploymentPlan` or `OptimizationResult`.
- **Async / virtual-thread client facade** (`core.async.EquinixAsync`): run any SDK call on a
  Java 21 virtual thread, returning a `CompletableFuture`, without mirroring the API surface.

### Changed
- **Modules extracted out of `fabric.*`:** Metro Optimizer + Deployment Wizard + Peering
  Intelligence → `api.equinix.javasdk.design.*`; MCP bridge → `api.equinix.javasdk.mcp.*`.
  `Fabric.optimizeMetros()/deploymentWizard()/peeringIntelligence()/mcp()` remain as accessors.
- **`FabricGateway` interface** — the value-add design engines now depend on a narrow read/build
  interface (`metros()`/`serviceProfiles()`/`cloudRouters()`/`connections()`/`routingProtocols()`/
  `prices()`) rather than the concrete `Fabric`; `Fabric implements FabricGateway`, so existing
  callers are unaffected.
- **Pagination is now `Iterable`, not `List`** (breaking): `PaginatedList<T>` and
  `PaginatedFilteredList<T>` implement `Iterable<T>` and expose `stream()`/`get(int)`/`size()`/
  `isEmpty()`/`toList()` alongside `hasNextPage()`/`next()`/`loadAll()`/`getPagination()`, instead
  of extending `ArrayList`. A server-returned page is no longer a mutable collection (matching the
  AWS/Google/Stripe SDK idiom). Migration: `new ArrayList<>(page)` → `new ArrayList<>(page.toList())`;
  iteration, `stream()`, `get(int)` and `size()` are unchanged.
- **`RouteAggregations.list()` → `search()`** (breaking): route aggregations are exposed via
  `POST /routeAggregations/search` (the only endpoint the API provides), returning a
  `PaginatedFilteredList`; the old `list()` targeted a non-existent GET path and threw at runtime.
- **JSON Patch support in core**: per-request content-type + `PatchOperation` model + `patchOne`.
- **Connection `update()`** exposed (`PATCH /connections/{uuid}`, JSON Patch) — previously
  plumbed but unreachable.
- **Customer Portal Orders → EIA-correct shape** (breaking): base path corrected to
  `colocations/v2/orders`; the fictional `Orders.list()`/`create()` (no such ops in `ordersv2`;
  listing lives in `OrderHistory`) were removed.
- **Customer Portal SmartHands reshape** (breaking): the generic `list/get/create/update` CRUD
  (which the API does not provide) is replaced by the 12 typed order builders + `listTypes`/
  `listLocations`; `SmartHands`/`SmartHandsType`/`SmartHandsStatus` removed.
- **InternetAccess → EIA v2** (breaking): collapsed to the single nested
  `POST /internetAccess/v2/services` create (`ServiceRequest` with nested routing-protocol/IP-block
  body); the prior 5-op v1 CRUD shape is gone; enums renamed to `ServiceTypeV2`/`ServiceState`.

### Deprecated
- **`Connections.getStatistics(...)` / `Ports.getStatistics(...)`** (the `/stats` endpoints, marked
  deprecated upstream) → use `getMetrics(...)` / `Fabric.metrics().search(...)`. The `/stats` wiring
  remains for back-compat.

### Fixed
- **~20 endpoint-name mismatches** where client code referenced endpoints absent from
  `apiParams` (silent runtime failures) — notably Fabric PrecisionTime (entirely non-functional),
  Stream/StreamSubscription/RouteAggregation/RoutingProtocol updates, the NetworkEdge BGP delete
  and PublicKey delete (missing endpoints added), CloudEvents/RouteFilterRule listing, and the
  Customer Portal WorkVisit/TroubleTicket create names.
- **Stream-subscriptions path** (`/streamSubscriptions` → `/subscriptions`, spec-verified).
- Operator builders that initialized accumulator lists with an immutable `singletonList` then
  mutated them (`UnsupportedOperationException`); `ResellerWrapper` getters returning `null`
  and defeating `@Delegate`; `MetrosImpl.list(presence)` ignoring its argument.
- Corrected the unverifiable "60+ MCP tools" claim; removed the dead `PeeringIntelligence
  .withMcpEnrichment()` no-op.

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

### Added - Comprehensive Test Suite
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
- Metro Optimizer — intelligent placement engine with 12-phase scoring pipeline
- Deployment Wizard — optimization-to-execution deployment planning
- Peering Intelligence — PeeringDB integration with presence matrices and resiliency analysis
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
- Comprehensive exception hierarchy (401/403/404/409/429/5xx)
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
