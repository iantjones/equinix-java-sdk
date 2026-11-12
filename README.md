# Equinix Java SDK

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java](https://img.shields.io/badge/Java-21%2B-orange.svg)](https://openjdk.java.net/)
[![Maven Central](https://img.shields.io/maven-central/v/com.eqixiac.equinix/equinix-sdk-java.svg)](https://central.sonatype.com/artifact/com.eqixiac.equinix/equinix-sdk-java)

A Java 21 client for the Equinix REST APIs: Fabric (v4, including the Beta families under
`fabric.beta()`), Network Edge, the data center operations APIs (cross connects, Smart Hands,
shipments, site access, tickets, billing), Internet Access, IBX SmartView, Projects, IAM and STS, plus a typed receiver for Equinix Messaging Gateway notifications. On top of the clients sits a
design layer that reads the live Fabric catalogue and prices to choose metros for a `DesignSpec`,
simulate failures, plan and execute a deployment, analyse an existing estate and write a Design
Decision Record. The same jar runs as a local MCP server over stdio that exposes the design layer
and a few cross-domain reads to an MCP host. This is an independent community project. It is not
an Equinix product, Equinix does not endorse or support it, and Equinix trademarks belong to
Equinix, Inc.; the name is used only to identify the APIs the SDK calls.

## Contents

- [Install](#install)
- [Authentication](#authentication)
- [Quick start](#quick-start)
- [API coverage](#api-coverage)
  - [Fabric](#fabric)
  - [Fabric Beta](#fabric-beta)
  - [Network Edge](#network-edge)
  - [Data center operations](#data-center-operations)
  - [Cross Connect and Digital LOA](#cross-connect-and-digital-loa)
  - [Internet Access](#internet-access)
  - [IBX SmartView](#ibx-smartview)
  - [IAM, STS and Projects](#iam-sts-and-projects)
  - [Messaging Gateway](#messaging-gateway)
- [Paging, errors and retries](#paging-errors-and-retries)
- [Design platform](#design-platform)
- [MCP server](#mcp-server)
- [Hosts the SDK contacts](#hosts-the-sdk-contacts)
- [Testing](#testing)
- [Limitations and known gaps](#limitations-and-known-gaps)
- [Versioning, migration and licence](#versioning-migration-and-licence)

Longer reference pages: [design platform](docs/design/design-platform.md),
[cloud-to-cloud paths](docs/design/cloud-to-cloud.md), [MCP server](docs/mcp-server.md), [migrating from 3.0.0](docs/MIGRATING-4.0.md),
[CHANGELOG](CHANGELOG.md), [CONTRIBUTING](CONTRIBUTING.md).

## Install

Java 21 or later (the build is verified on JDK 21 and JDK 27). Maven coordinates:

```xml
<dependency>
    <groupId>com.eqixiac.equinix</groupId>
    <artifactId>equinix-sdk-java</artifactId>
    <version>4.0.0</version>
</dependency>
```

These dependencies are declared `<optional>`, so they do not reach your build unless you add them:

| Dependency | Needed for |
|---|---|
| `io.modelcontextprotocol.sdk:mcp-core` and `mcp-json-jackson2` (2.1.0) | Embedding the MCP server in your own process. The runnable server jar bundles them. |
| `com.azure:azure-messaging-servicebus` (7.18.0) | `ServiceBusEmgReceiver`, the Azure Service Bus binding of the Messaging Gateway receiver |
| `org.slf4j:slf4j-simple` (2.0.20) | Log output from the runnable server jar; library users bring their own SLF4J binding |

Building from source needs only a JDK: `./mvnw clean verify -DskipTests` downloads the pinned
Maven, builds the library jar, the sources and Javadoc jars and the runnable
`target/equinix-sdk-java-4.0.0-mcp-server.jar`. GPG signing runs only with `-Prelease`.
[CONTRIBUTING.md](CONTRIBUTING.md) has the full build and test reference.

## Authentication

### Client credentials

Every domain client accepts OAuth2 client credentials from an application created in the Equinix
Developer Portal. The SDK fetches a token on the first call and refreshes it before expiry.

```java
BasicEquinixCredentials credentials = new BasicEquinixCredentials(
        System.getenv("EQUINIX_ACCESS_KEY"), System.getenv("EQUINIX_SECRET_KEY"));
Fabric fabric = new Fabric(credentials);
```

To rotate a secret without rebuilding the client, pass an `EquinixCredentialsProvider`. The SDK calls
it on each authentication.

```java
EquinixCredentialsProvider provider = () -> new BasicEquinixCredentials(vault.clientId(), vault.clientSecret());
Fabric fabric = new Fabric(provider);
```

### Sessions

Each `new Fabric(...)`, `new NetworkEdge(...)` and so on owns its own token and connection pool. An
`Equinix` session owns one of each and shares them across every domain and the design facade:

```java
try (Equinix eq = new Equinix(credentials)) {
    Fabric fabric = eq.fabric();
    NetworkEdge edge = eq.networkEdge();
    Design design = eq.design();
}
```

`EquinixConfig` holds the construction-time options: `sandbox` (targets
`https://sandboxapi.equinix.com`), `autoLoadMetros`, `enrichMetroRegistry`, `retryPolicy`,
`circuitBreaker` and `peeringDbApiKey`. Every client constructor has an overload that takes it.

```java
Fabric fabric = new Fabric(credentials, EquinixConfig.builder()
        .sandbox(true)
        .retryPolicy(RetryPolicy.none())
        .build());
```

### Bearer tokens

`BearerTokenProvider` replaces client credentials with a token you supply. `Equinix` and `IAM` have
constructors that take one; the other domain clients are reached through the session.

```java
try (Equinix eq = new Equinix(BearerTokenProvider.of(token))) {
    eq.fabric().connections().search();
}
```

`BearerTokenProvider.of(String)` always returns the same token. `BearerTokenProvider.from(Supplier<BearerToken>)`
asks the supplier on the first call, shortly before a known expiry (`BearerToken(value, expiresAt)`),
and after an HTTP 401. Calls are single-flight: concurrent requests that need a token cause one call.

### Access-policy tokens for IAM project-scoped calls

Organisation-level IAM calls such as `iam.roles().list()` accept the client-credentials token. The
project-scoped endpoints (`/v1/projects/{projectId}/...`: access policies, permission sets,
principal policies, policy masks, effective permissions, project roles, resource types) answered
HTTP 401 to a client-credentials token in live tests on 2026-11-11 and need an STS-issued
access-policy token. Exchange an OIDC ID token at STS, then build an `IAM` with the result:

```java
StsToken sts = STS.withoutCredentials().tokens().generate(new TokenRequest()
        .grantType("urn:ietf:params:oauth:grant-type:token-exchange")
        .subjectToken(idToken)
        .subjectTokenType("urn:ietf:params:oauth:token-type:id_token")
        .scope(StsScope.principalPolicy(projectId)));

IAM projectIam = new IAM(BearerTokenProvider.of(sts.getAccessToken()));
projectIam.principalPolicies().list(ProjectIds.of(projectId));
```

`StsScope` builds the scopes STS accepted live: `accessPolicyErn(...)`, `accessPolicy(projectId, policyId)`,
`roleAssignments(organizationId)`, `projectPolicies(projectId)` and `principalPolicy(projectId)`.
IAM paths take the `project:<id>` form; `ProjectIds.of(...)` converts a bare Resource Manager
project id. The token exchange, JWKS and OpenID discovery are unauthenticated, so
`STS.withoutCredentials()` needs no Equinix secret. OIDC provider management needs credentials.

### Hosts per domain

IAM requests go to `https://access.eqix.equinix.com` and STS requests to
`https://sts.eqix.equinix.com`; `api.equinix.com` answers 404 for both. Every other domain, and the
OAuth token request, uses `https://api.equinix.com`. Override a domain host with
`getEquinixClient().setDomainEndPoint("IAM", url)`.

## Quick start

```java
import com.eqixiac.equinix.Equinix;
import com.eqixiac.equinix.Fabric;
import com.eqixiac.equinix.core.auth.BasicEquinixCredentials;
import com.eqixiac.equinix.fabric.model.Connection;

public class ListConnections {
    public static void main(String[] args) throws Exception {
        BasicEquinixCredentials credentials = new BasicEquinixCredentials(
                System.getenv("EQUINIX_ACCESS_KEY"), System.getenv("EQUINIX_SECRET_KEY"));
        try (Equinix eq = new Equinix(credentials)) {
            Fabric fabric = eq.fabric();
            for (Connection c : fabric.connections().search()) {
                System.out.println(c.getName() + " " + c.getState());
            }
        }
    }
}
```

## API coverage

| Domain | Entry point | Accessors |
|---|---|---|
| Fabric v4 | `eq.fabric()` or `new Fabric(...)` | `connections`, `ports`, `serviceTokens`, `serviceProfiles`, `cloudRouters`, `routingProtocols`, `routeFilters`, `routeFilterRules`, `routeAggregations`, `routeAggregationRules`, `networks`, `streams`, `streamSubscriptions`, `streamAlertRules`, `streamAssets`, `precisionTimes`, `metrics`, `cloudEvents`, `prices`, `marketplaceSubscriptions`, `eiaServices`, `ipBlocks`, `portPackages`, `agents`, `agentTemplates`, `companyProfiles`, `tags`, `metros`, `metroRegistry`, `health`, `beta` |
| Network Edge | `eq.networkEdge()` | `devices`, `aclTemplates`, `vpns`, `bgpPeerings`, `deviceLinks`, `publicKeys`, `backups`, `setup` |
| Data center operations | `eq.operations()` or `new Operations(...)` | `orders`, `orderHistory`, `crossConnects`, `troubleTickets`, `troubleTicketOrders`, `workVisits`, `smartHandsRequests`, `shipments`, `secureCabinets`, `invoices`, `billingAccounts`, `billingAccountsSearch`, `quotes`, `assets`, `attachments`, `reports`, `notifications`, `unifiedNotifications`, `supportCases`, `supportPlans`, `digitalLoas`, `lookups` |
| Internet Access | `eq.internetAccess()` | `services` (EIA v2), `unifiedServices` and `ipBlocks` (Fabric v4), `ibxs`, `prices`, `accounts`, `termsAndConditions`, `operationalUnits`, `signaturePolicies`, `productConfigurations`, `purchaseOrders`, `orders`, `cages`, `cabinets`, `patchPanels`, `connectionServices` |
| IBX SmartView | `eq.ibxSmartView()` | `environmentals`, `powerEvents`, `systemAlerts`, `streamingSubscriptions`, `smartViewAssets`, `hierarchy`, `legacyEnvironmentals`, `legacyPower` |
| IAM | `eq.iam()` or `new IAM(...)` | `roles`, `roleAssignments`, `accessPolicies`, `permissionSets`, `principalPolicies`, `policyMasks`, `effectivePermissions`, `resourceTypes` |
| STS | `new STS(...)` or `STS.withoutCredentials()` | `tokens`, `oidcProviders`, `discovery` |
| Projects | `eq.projects()` | `projects` (Resource Manager v2, read-only) |
| Cross Connect, Digital LOA | `eq.crossConnects()`, `eq.digitalLoas()` | Each returns both API families for the product: the order API from `operations()` and the Fabric v4 Beta API from `fabric().beta()` |
| Messaging Gateway | `com.eqixiac.equinix.messaging` | `EmgMessageParser`, `EmgMessageHandler`, `EmgListener`, `ServiceBusEmgReceiver` |

Endpoints are declared in `src/main/resources/json/apiParams_*.json`. Where the live service and
the OpenAPI specification disagree, the SDK reads both shapes and sends what the live service
accepts; the Javadoc of the affected method describes the difference.

### Fabric

Search, create with a dry run, and read metrics:

```java
PaginatedFilteredList<Connection> mine = fabric.connections().search(
        Filter.filter().and().equals("/project/projectId", projectId),
        Sort.sort().desc("/changeLog/updatedDateTime"));

Connection validated = fabric.connections()
        .define(ConnectionType.EVPL_VC)
        .name("dc-to-aws")
        .bandwidth(1000)
        .aSideAccessPointPort(portUuid, LinkProtocol.dot1q().vlanTag(1001).create())
        .zSideCloudProvider(AwsDirectConnectAdapter.of("123456789012", "us-east-1", profileUuid, "DC"))
        .notification("ops@example.com")
        .dryRun()                       // validates; creates nothing
        .create();

LocalDateTime to = LocalDateTime.now(ZoneOffset.UTC);
List<Metric> tx = fabric.metrics().forConnection(connectionUuid, to.minusHours(6), to,
        "equinix.fabric.connection.bandwidth_tx.usage");
```

The four cloud adapters (`AwsDirectConnectAdapter`, `AzureExpressRouteAdapter`,
`GoogleCloudInterconnectAdapter`, `OracleFastConnectAdapter`) take the Equinix metro of the
provider port as their last argument. Fabric rejected a service-provider Z-side without
`location.metroCode` with HTTP 400 `EQ-3142501` on 2026-11-11. Azure takes the ExpressRoute service
key, the service profile, the peering type and the metro:
`AzureExpressRouteAdapter.of(serviceKey, profileUuid, PeeringType.PRIVATE, "SV")`.

Behaviour to know about:

- `fabric.metrics().search(MetricQuery)` and `fabric.cloudEvents().search(CloudEventQuery)` check
  the mandatory filters before sending. Metrics need one subject and a metric name; cloud events
  need a subject, project or organisation. The service rejected metric searches with a page size
  above 10 on 2026-11-12, so the SDK pages metrics 10 at a time.
- `ports().list()`, `cloudRouters().commands(...)` and `networks().getConnections(...)` return a
  `List`: the endpoints take no offset. Use `search(...)` to page.
- `prices().list(filter)` sends the filter only and reads one page, because the live service
  ignores pagination on that search.
- The connection search ignored the `/project/projectId` filter in a live test on 2026-11-12 and
  returned connections from other projects. Code that must stay inside a project should check
  `getProject()` on each result. The design layer does this itself.
- Wrappers keep their state after `delete()` returns 204; `DeletableResource.isDeleted()` reports it.
- `fabric.metroRegistry()` is a cached snapshot of every metro from the Metros API. A metro missing
  from the `MetroCode` enum reads as `MetroCode.UNKNOWN` and keeps its code in `metroId()`.

### Fabric Beta

`fabric.beta()` returns `FabricBeta`: 82 operations from the Fabric v4 4.34.0 Beta families, one
client per family. Types are annotated `@Beta`.

| Client | Operations | Covers |
|---|---:|---|
| `asns()` | 5 | ASN registration and ownership challenge |
| `exchangeServices()` | 5 | IX services |
| `appDomains()`, `appLinks()`, `appProcessors()`, `appServices()`, `appSubscriptions()` | 42 | Application Connect |
| `opticalConnects()` | 4 | Optical Metro Connect |
| `crossConnects()` | 12 | v4 cross connects and install schedules |
| `loas()` | 10 | Letters of authorisation, actions, notes, consumers |
| `tags()` | 1 | `GET /tags/{uuid}` |
| `logos()` | 3 | Create, download, delete |

```java
PaginatedFilteredList<Asn> asns = fabric.beta().asns().search(
        Filter.filter().and().equals("/project/projectId", projectId),
        Sort.sort().desc("/changeLog/updatedDateTime"));
CrossConnectResponse cc = fabric.beta().crossConnects().getByUuid(crossConnectId);
var domains = fabric.beta().appLinks().listAttachedAppDomains(linkId, new AttachmentQuery().states("ATTACHED"));
```

The Beta families have no dry run in the specification, so the SDK offers none. Cross connects,
LOAs and optical connects are orders for physical work. Read models are plain data classes with no
wrapper layer; mutations are methods on the client, and PATCH takes `List<PatchOperation>`. Access
needs an entitlement on the account. In a read-only run on 2026-11-12 every family answered, with
intermittent HTTP 500s and some searches taking more than 45 s; the cross-connect schedule search
returned 400 without a filter and 500 with the specification's example filter.

### Network Edge

```java
for (Device d : eq.networkEdge().devices().list()) {
    System.out.println(d.getName());
}
```

`devices().getInterfaceStatistics(uuid, interfaceId, start, end)` takes `Instant` bounds and sends
whole-second UTC. `devices().define(name)` is the device create builder; `Device.delete(DeviceDeleteRequest)`
carries the deletion options. Public-key create returns the id from the `Location` header (the
service answers 201 with an empty body). The `/ne/v1/bgp*` and `/ne/v1/vpn*` endpoints the SDK
calls are absent from the published Network Edge specification but answered live on 2026-11-11.

### Data center operations

`Operations` (called `CustomerPortal` before 4.0.0) groups the product APIs for running a deployment
inside an IBX: cross connects, Smart Hands, shipments, work visits (site access), trouble tickets
and support cases, orders and quotes, invoices and billing accounts, assets, reports, notifications
and lookups. Equinix publishes them as separate APIs; they share one host and one credential.

Trouble tickets, cross connects and work visits have no list endpoint; their history comes from
order history:

```java
List<? extends OrderHistoryItem> tickets = eq.operations().orderHistory().search(
        OrderHistorySearchRequest.builder()
                .filters(OrderHistorySearchRequest.Filters.builder()
                        .productTypes(OrderHistoryProductType.TROUBLE)
                        .fromDate(LocalDate.now().minusDays(30))
                        .build())
                .build());
tickets.forEach(o -> System.out.println(o.getOrderNumber() + " " + o.getOrderStatus()));
```

`orderHistory().searchPage(...)` returns the page envelope as well. Work visit locations come from
the Lookup v2 API (`workVisits().listLocations()` returns `LookupLocation`); the v1 locations
endpoint reached end of life on 2024-01-22 and is gone. Digital LOA search needs a non-empty
`filter.and`, which the typed builder enforces.

### Cross Connect and Digital LOA

Each of these two products has two unrelated API families. The session facade returns both, so
neither is hidden under another domain:

```java
CrossConnectApis xc = eq.crossConnects();
xc.orderApi();    // crossconnects v2: order, update, deinstall (same client as eq.operations().crossConnects())
xc.fabricApi();   // Fabric v4, Beta: cross connect records and install schedules (eq.fabric().beta().crossConnects())

DigitalLoaApis loa = eq.digitalLoas();
loa.diloaApi();   // Digital LOA v1 (eq.operations().digitalLoas())
loa.fabricApi();  // Fabric v4, Beta: LOAs with actions, notes and consumers (eq.fabric().beta().loas())
```

The hubs add no behaviour and the clients keep their own packages. The Fabric v4 families are Beta
and need the account to be entitled to them.

### Internet Access

`services()` is EIA v2. Since 2026-07 the Unified EIA API lives in Fabric v4, and
`internetAccess().unifiedServices()` and `ipBlocks()` return the same clients as
`fabric.eiaServices()` and `fabric.ipBlocks()`:

```java
PaginatedFilteredList<EiaService> services = eq.internetAccess().unifiedServices().search();
```

IP blocks are standalone and survive service termination. `ServiceSearchRequest.sort()` worked live
on `/name`, `/state` and `/type` only; the `/type` filter returned 400 for every value and the SDK
rejects it before sending.

### IBX SmartView

```java
for (SensorReading r : eq.ibxSmartView().environmentals().list("DC2")) {
    System.out.println(r);
}
```

v1 hierarchy, legacy and asset calls fail fast when `accountNo` or another required parameter is
missing, because the service answers those requests with an error. Power events read the live
`dcim/v3` envelope. Streaming subscriptions deliver API+ data, a paid entitlement.

### IAM, STS and Projects

See [Authentication](#access-policy-tokens-for-iam-project-scoped-calls) for the token rules. IAM
list calls page with an opaque `pageToken`:

```java
RoleList page = eq.iam().roles().list();
for (Project p : eq.projects().projects().list()) {
    System.out.println(p.getProjectId() + " " + p.getProjectName());
}
```

### Messaging Gateway

`com.eqixiac.equinix.messaging` parses Equinix Messaging Gateway (EMG) notifications into a sealed
`EmgMessage` hierarchy (`IbxNotification`, `NetworkNotification`, `FabricNotification`,
`BillingNotification`, `OrderNotification`, `ManagedServiceNotification`, `UnknownNotification`) and
dispatches them to an `EmgMessageHandler`. `EmgListener` drives any `EmgReceiver`;
`ServiceBusEmgReceiver` binds it to Azure Service Bus and needs the optional
`azure-messaging-servicebus` dependency.

```java
EmgMessageHandler handler = new EmgMessageHandler() {
    @Override
    public void onIbxNotification(IbxNotification n) {
        if (n.body().isServiceImpacting()) {
            System.out.println(n.id() + " " + n.category() + " " + n.body().startDateTime());
        }
    }
};
EmgReceiver receiver = ServiceBusEmgReceiver.forQueue(
        System.getenv("EMG_CONNECTION_STRING"), System.getenv("EMG_QUEUE"));
try (EmgListener listener = new EmgListener(receiver, handler)) {
    listener.run();   // blocks: completes on success, abandons on handler error, dead-letters non-EMG bodies
}
```

The connection string and queue name come from the Equinix portal (portal.equinix.com): Developer Settings, Messaging
Gateway, the subscription, Configuration, Copy YAML. Treat the connection string as a secret.
Service Bus delivers at least once, so handlers must be idempotent: de-duplicate on `id()`. The
package does not implement EMG Order Management (deprecated 2024-09-30), `client_credentials` EMG
authentication (deprecated 2024-03-18), signature verification (not documented) or attachment
download.

## Paging, errors and retries

### Paging

List calls return `PaginatedList<T>` or `PaginatedFilteredList<T>`, an `Iterable` over the loaded
page with lazy access to the rest. It is not a `java.util.List`.

```java
PaginatedFilteredList<Connection> page = fabric.connections().search();
List<Connection> all = page.loadAll().toList();     // every page, then a List snapshot
```

`loadAll()` and `next()` stop on an empty page or a repeated offset, so an endpoint that ignores
`offset` cannot loop forever. IAM lists and some operations lists use token or page envelopes
instead; their return types say so.

### Errors

API errors are `EquinixServiceException` subclasses; client-side and transport failures are
`EquinixClientException`. Both extend `BaseException`.

| HTTP status | Exception |
|---|---|
| 401 | `EquinixAuthenticationException` |
| 403 | `EquinixAuthorizationException` |
| 404 | `EquinixNotFoundException` |
| 409 | `EquinixConflictException` |
| 429 | `EquinixRateLimitException` |
| 5xx | `EquinixServerException` |

```java
try {
    fabric.connections().getByUuid(uuid);
} catch (EquinixNotFoundException e) {
    System.err.println("not found, correlation id " + e.getCorrelationId());
} catch (EquinixServiceException e) {
    System.err.println(e.getStatusCode() + " " + e.getMessage());
    for (ExceptionDetail d : e.getExceptionDetails()) {
        System.err.println(d.getErrorCode() + " " + d.getErrorMessage() + " " + d.getProperty());
    }
    System.err.println(e.getResponseBodySnippet());   // set only when the body could not be parsed
}
```

The error parser reads the body format of each service: Fabric, Network Edge, EIA and Projects
arrays, the IAM `error` object, the STS `error`/`error_description` pair, operations and Smart
Hands `errors[]`, IBX SmartView v1 and v2, the gateway error object and Apigee faults. `getMessage()`
starts with `code: message (field: ...)`. `ExceptionDetail` is immutable; `toSummaryLine()` renders
one detail. Every request carries an `X-Correlation-Id`, returned by `getCorrelationId()`; quote it
when raising an issue with Equinix support.

### Retries, circuit breaker and 401 replay

`RetryPolicy.defaultPolicy()` retries 429, 500, 502, 503 and 504 and transient `IOException`s up to
3 times, with exponential backoff from 500 ms capped at 20 s and full jitter, and honours
`Retry-After` (seconds or an HTTP date). 5xx and I/O retries apply only to idempotent methods:
`POST` and `PATCH` are not retried unless the policy sets `retryNonIdempotentMethods`. A 429 is
retried for every method, because the server did not process the request. `RetryPolicy.none()`
disables retries. `EquinixConfig.circuitBreaker(...)` adds an opt-in breaker that opens after
consecutive failures and throws `CircuitOpenException` while open.

After an HTTP 401 the SDK discards the token, obtains a new one once (single-flight across threads)
and replays the request once. A second 401 is thrown as `EquinixAuthenticationException`. There is
no replay for the token request itself, for unauthenticated endpoints, or when the client has no
token source.

## Design platform

The design layer (`com.eqixiac.equinix.design`, facade `Design.over(fabric)` or `eq.design()`) plans
networks from one input document, the `DesignSpec`. Every engine reads the spec, records its
fingerprint (SHA-256 of the canonical JSON) in its result and reports where each figure came from.
The engines are read-only except `DeploymentPlan.execute(...)` and `reconcile(...)`. Latency is
round-trip milliseconds throughout. Prices are live list prices, dated reference figures or
estimates, and each line says which; none is a quote. Availability figures are planning
assumptions, not SLAs.

[docs/design/design-platform.md](docs/design/design-platform.md) is the full reference and
[docs/design/cloud-to-cloud.md](docs/design/cloud-to-cloud.md) covers native cloud-to-cloud links
(Beta). A summary:

```yaml
schemaVersion: "1"
name: ld-ny-aws
sites:
  - label: London office
    metro: LD
    role: HEADQUARTERS
  - label: New York office
    metro: NY
clouds:
  - id: aws-use1
    provider: AWS
    region: us-east-1
    monthlyEgressGb: 5000
workloads:
  - label: analytics
    type: GENERAL_COMPUTE
    bandwidthMbps: 1000
    clouds: [aws-use1]
constraints:
  maxMetros: 2
objectives:
  profile: balanced
commercial:
  currency: USD
```

```java
DesignSpec spec = DesignSpecIO.read(Path.of("design.yaml"));
OptimizationResult result = MetroOptimizer.forSpec(fabric, spec).run();
System.out.println(result.toMarkdown());

ResilienceReport resilience = ResilienceAnalyzer.forSpec(fabric, spec).optimization(result).run();
DesignDecisionRecord record = Design.over(fabric).decisionRecord(spec).optimization(result).record();
Files.writeString(Path.of("decision.md"), record.toMarkdown());
```

| Engine | Entry point | What it does |
|---|---|---|
| Spec | `DesignSpec`, `DesignSpecIO` | YAML or JSON document; `validate()` returns issues with a path and a fix; unknown properties are errors |
| Optimizer | `MetroOptimizer.forSpec(fabric, spec).run()` | Scores every metro portfolio up to `maxMetros` on latency, cost, provider coverage, resilience and compliance; returns the recommendation, the Pareto frontier, constraint verdicts, sensitivity and workload placement |
| Cost and currency | `RateCardCostModel`, `CurrencyConverters` | Prices every design in the spec currency; pinned rates, then a supplied converter, then live ECB rates from `api.frankfurter.dev`, then a bundled table; `-Dequinix.design.fx.offline=true` or `CurrencyConverters.bundledOnly()` keeps it offline |
| Growth and terms | `GrowthPlanner.forSpec(...)` | Monthly traffic growth, upgrade months, on-demand against 12, 24 and 36 month terms with break-even, seeded Monte Carlo scenarios |
| TCO and savings | `TcoCalculator.forSpec(...)`, `SavingsCalculator.forSpec(...)` | Fabric against internet and on-premises archetypes; cloud egress over the internet against private interconnect; unpriced lines listed with reasons |
| Resilience | `ResilienceAnalyzer.forSpec(fabric, spec)` | Simulates every single failure and up to 2,000 failure pairs; blast radius, single points of failure, correlated risks, N-1 and N-2 verdicts, remediation re-simulated |
| Estate | `EstateAnalyzer.builder(fabric).scope(...)` | Read-only review of deployed Fabric resources in named projects: seven rules, priced findings, coverage gaps |
| Plan lifecycle | `fabric.deploymentWizard(result)`, `DeploymentPlan` | Plan, save and load with an integrity check, drift against live state, checkpointed execute and reconcile, rollback, Terraform export |
| Peering | `PeeringIntelligence.forSpec(fabric, spec)` | PeeringDB presence of the spec's peers at the candidate metros |
| Decision record | `DesignDecisionRecorder.forSpec(fabric, spec)` | Runs the engines and writes one ADR-style Markdown or JSON record with nine sections |
| Cloud-to-cloud (Beta) | `tcoComparison().toCloud(...)`, `savingsCalculator().toCloud(...)`, `cloudToCloudStrategy(...)` | Prices the internet, Fabric and native provider-to-provider paths between two clouds and reports the break-even rate |

## MCP server

The runnable jar `target/equinix-sdk-java-4.0.0-mcp-server.jar` is an MCP server
(`equinix-sdk-java-intelligence`) that an MCP host starts as a subprocess and talks to over stdio.
It exposes the design engines and a few operations, Network Edge and IBX SmartView reads as
tools, under the client credentials you give it. It has no network listener. Built on the MCP Java
SDK 2.1.0, it negotiates protocol versions 2024-11-05 through 2025-11-25.

Equinix runs its own Fabric MCP server. As described in Equinix documentation read on 2026-11-11, it
is in Public Preview at `https://mcp.equinix.com/fabric`, is hosted by Equinix, signs users in with
OAuth 2.1 through the Equinix portal login, grants viewer, operator or manager permissions at consent,
and offers roughly 127 to 135 Fabric and Network Edge tools that mirror the API, with create and
update tools and no delete tools. This server is unrelated to it: it runs locally under API client
credentials, and its tools wrap SDK engines (optimisation, resilience, pricing, planning, estate
analysis) rather than single endpoints. The two can be registered in the same host.

### Connect

```bash
printf 'EQUINIX_ACCESS_KEY=\nEQUINIX_SECRET_KEY=\n' > .env.local   # then fill in both values
./mvnw -q package -DskipTests
./connect-claude-desktop.sh           # Windows: .\connect-claude-desktop.ps1
```

The connect scripts merge an `equinix` entry into Claude Desktop's config (keeping any existing
`env`, arguments and command), back the file up first, and by default write no secret: the entry
points `cwd` and `--env-file` at the repository's `.env.local`. `--embed-credentials`
(`-EmbedCredentials` in PowerShell) copies the keys into the config instead. For any other host,
run `java -jar target/equinix-sdk-java-4.0.0-mcp-server.jar [--toolsets design,ibx] [--env-file path]`.

The server reads `EQUINIX_ACCESS_KEY` and `EQUINIX_SECRET_KEY` from its environment, then from the
`--env-file` file or `.env.local` in its working directory. A malformed `.env.local` line stops the
server with exit status 3 and a message naming the line and key, never the value. Missing
credentials exit with status 2. stdout carries only JSON-RPC; all logging goes to stderr.

### Toolsets and tools

`EQUINIX_MCP_TOOLSETS` or `--toolsets` selects toolsets by id: `design`, `estate`, `fabric`
(`design` plus `estate`), `ops`, `ne`, `ibx`, `mutate`. The default is every toolset except
`mutate`: 22 tools, all read-only. `--toolsets design` serves 16.

| Tool | Toolset | Contacts remote hosts | What it returns |
|---|---|---|---|
| `design_spec_template` | design | no | A commented starter spec (YAML or JSON) and its fingerprint |
| `design_validate_spec` | design | no | Issues with severity, path and fix; the normalised spec |
| `design_optimize` | design | yes | Recommendation, frontier, sensitivity, placement, cost with price basis |
| `design_analyze_resilience` | design | yes | N-1 and N-2 verdicts, single points of failure, remediation, per-failure detail |
| `design_growth_and_terms` | design | yes | Traffic growth, upgrade months, term comparison and break-even |
| `design_estimate_tco` | design | yes | TCO of deployment archetypes for one of the spec's clouds |
| `design_compare_cloud_egress` | design | yes | Internet egress against private interconnect for one cloud |
| `design_estimate_latency` | design | yes | Round-trip latency between two metros or IBXs, measured or estimated |
| `design_compare_cloud_to_cloud` | design | yes | Internet, Fabric and native multicloud link paths between two clouds (Beta) |
| `design_list_multicloud_environments` | design | no | Native multicloud region pairs from the bundled catalogue (Beta) |
| `design_analyze_peering` | design | yes | PeeringDB presence for the spec's peers |
| `design_plan_deployment` | design | yes | A deployment plan, its `plan_id` and plan document; sends dry-run POSTs only |
| `design_get_plan` | design | no | A stored plan |
| `design_plan_drift` | design | yes | Stored plan against live Fabric state |
| `design_export_terraform` | design | no | Terraform for a stored plan |
| `design_decision_record` | design | yes | The Design Decision Record, paged Markdown of 16,000 characters per page |
| `design_analyze_estate` | estate | yes | Estate findings for 1 to 10 named projects |
| `ops_list_open_tickets` | ops | yes | Open trouble tickets, newest first |
| `ops_get_billing_summary` | ops | yes | Billing summary |
| `ne_list_devices` | ne | yes | Network Edge devices |
| `ibx_get_environmentals` | ibx | yes | IBX environmental readings |
| `ibx_list_power_events` | ibx | yes | IBX power events |
| `fabric_propose_change` | mutate | yes | Dry-run of a billable Fabric create, a single-use token bound to the request |
| `fabric_confirm_change` | mutate | yes | Executes a proposed create after a human confirms |

Design tools take `spec` (a JSON object or a JSON or YAML string) or `spec_fingerprint` (a stored
`design_optimize` result); `design_optimize` and `design_validate_spec` need `spec`. Results carry
the fingerprint and provenance. Resources: `equinix-design://schema/design-spec`,
`equinix-design://spec/template`, `equinix-design://plans` and `equinix-design://plans/{plan_id}`.
Prompts: `design_from_requirements`, `review_existing_estate` (with the `estate` toolset) and
`what_breaks_if`.

### Safety

- The Safe Mutation Broker (`mutate`) is never on by default. It creates connections, networks and
  service tokens in two phases. `fabric_propose_change` compiles the request, runs the Fabric dry
  run and returns a token bound to the SHA-256 of the compiled request, valid for 10 minutes.
  `fabric_confirm_change` recompiles, checks the hash, asks a person through MCP elicitation with
  every field shown, re-checks the expiry and then sends the create. A client without elicitation
  gets a refusal unless `EQUINIX_MCP_ALLOW_UNCONFIRMED_MUTATIONS=true`. There are no delete tools.
- `notifications/cancelled` stops a running tool. A cancelled confirm sends no create; a cancel that
  arrives after the create was sent is reported as `cancellation.requested_after_send`.
- Strings from remote systems (resource names, error messages, PeeringDB text) are sanitised,
  capped at 512 characters per field, and listed under a top-level `untrusted_data` block so the
  model can treat them as data.
- `EQUINIX_MCP_PRICING_TIMEOUT_MS` (default 12000, accepted 100 to 120000) is the live-pricing budget
  of one tool call, shared by all its lookups. When it runs out the tool uses reference prices and
  says so in `equinix_live_pricing`. A first live price lookup took more than 12 s in testing on
  2026-11-12; 60000 to 120000 gives live prices for a two-metro design.
  `EQUINIX_MCP_ELICIT_TIMEOUT_MS` (default 300000) bounds each prompt to a person.

[docs/mcp-server.md](docs/mcp-server.md) has the full settings list, tool arguments, output shapes and
security guidance.

## Hosts the SDK contacts

| Host | When |
|---|---|
| `api.equinix.com` (`sandboxapi.equinix.com` in sandbox mode) | OAuth token, every domain except IAM and STS |
| `access.eqix.equinix.com` | IAM |
| `sts.eqix.equinix.com` | STS |
| `api.frankfurter.dev` | Design cost conversion, when a price is in another currency than the spec's and no pinned or supplied rate covers it; one `GET /v1/latest?base=<currency>` per converter. Not contacted with `-Dequinix.design.fx.offline=true` or `CurrencyConverters.bundledOnly()` |
| `www.peeringdb.com` | Peering Intelligence (`PeeringIntelligence`, `design_analyze_peering`) |
| `pricing.us-east-1.amazonaws.com` | AWS price list: `AwsPriceListRateCard`, and the MCP tools `design_compare_cloud_egress` and `design_compare_cloud_to_cloud` |
| `prices.azure.com` | Azure retail prices: `AzureRetailPricesRateCard` and the same two MCP tools |
| `apexapps.oracle.com` | Oracle Cloud price list: `OracleCloudPriceListRateCard` and the same two MCP tools |
| `cloudbilling.googleapis.com` | Google Cloud Billing Catalog: `GcpBillingCatalogRateCard`, and the same two MCP tools only when `GCP_BILLING_API_KEY` is set |
| Your Azure Service Bus namespace | `ServiceBusEmgReceiver` |

The library contacts the cloud price lists only when your code creates those rate cards. Nothing
else is contacted; the MCP server sends no telemetry.

## Testing

| Command | What runs |
|---|---|
| `./mvnw test` | Default suite: unit, deserialisation, documentation-contract and fixture-conformance tests; no network |
| `./mvnw -Pwiremock test` | WireMock request-contract suite against a local HTTP server |
| `./mvnw -Pcoverage verify` | Both suites, then the JaCoCo gate |
| `./run-live.sh [tier]` | A live tier against the real APIs with credentials from `.env.local` |

At the time of writing the default suite runs 2,045 tests and the WireMock suite
1,462 tests.

Live tiers, each selected by a Maven profile: `integration-readonly` (GET, list and search only),
`integration-dryrun` (`dryRun=true` creates and validate calls), `integration-dryrun-existing`
(`dryRun=true` updates against existing resources), and `integration-full` and
`integration-scenario`, which create and delete real resources and need `-DconfirmDestructive=true`
(`run-live.sh full --confirm-destructive`). The report goes to `target/integration-report.json`
and `target/site/integration-report.html` with a PASS, SKIPPED, SERVER_ERROR or FAIL verdict per
test. The optional `EQUINIX_ACCESS_POLICY_TOKEN` enables the IAM project-scoped reads.

Every JSON fixture is listed in `src/test/resources/specs/fixture-manifest.json` and validated against
one of 26 bundled OpenAPI specifications; documented deviations live in `fixture-deviations.json`.
[CONTRIBUTING.md](CONTRIBUTING.md) explains the tiers, the fixture rules and the release gates.

## Limitations and known gaps

- Fixtures under review: `src/test/resources/specs/fixture-pending-review.json` lists 231 fixture
  mismatches against the specifications, grouped by owner. They are recorded as a baseline, not yet
  resolved; a fixture on that list may still mirror the SDK rather than the service.
- Live read-only tier on 2026-11-12: the first run had 7 failures. Those were fixed and covered by
  WireMock tests built from the live shapes (metrics page size, EIA v1 price `prefixLength` as an
  array, trouble-ticket types, SmartView `tagId`). The re-run gave 184 PASS, 67 SKIPPED,
  7 SERVER_ERROR (HTTP 5xx from the service) and 0 FAIL.
- Not verified live: PATCH content types for ports, service tokens and IP blocks (sent as the
  specification states); route aggregations (403 on the test account); stream operations (403);
  service-profile replace with `If-Match`; connection `vlanTag`/`vlanTags` updates (use a dry run
  first); BGP `customerPeerIp` with a `/30` suffix; Cloud Router dry-run create, which needs a billing
  account number; Network Edge BGP, VPN and public-key deletes.
- Entitlements: the Fabric Beta families, IBX SmartView API+ streaming and `getCurrentTagPoint`
  (403 without the entitlement), route aggregations and streams need account entitlements the test
  account lacked.
- Beta: everything under `fabric.beta()`, provider environments and activation-key validation,
  `core.model.multicloud`, native cloud-to-cloud planning and pricing, and the two MCP tools for it
  can change in a minor release. The Fabric Beta services returned intermittent HTTP 500s in testing.
  The `carbon` design objective is Beta too: it uses country-level grid intensity and is a siting
  indicator, not an emissions estimate (see [design platform](docs/design/design-platform.md)).
- Fabric searches: the connection search ignored the project filter live (see [Fabric](#fabric));
  the port search with a project filter returned 400 `EQ-3046106`. The filter DSL has no nested `or`,
  `LIKE` or `ILIKE`.
- Design data: 20 of 67 live metros had no geo scopes or zones on 2026-11-12; the catalogue loader
  derives zones from the country code instead. `/routerPackages` reported `vcCountMax` for 1 of 5
  packages. Port and Network Edge prices are not in the rate cards, so estate findings on them are
  unpriced, and the reference card has no term axis. Network Edge devices cannot be scoped to a
  project for estate analysis.
- Optimiser run time depends on live price lookups: a two-site, one-cloud spec took 10.7 s with the
  prefetching cost model on 2026-11-12, against 48.1 s before; 2,278 portfolios were examined.
- Messaging Gateway: no order management, signature verification or attachment download.
- MCP: protocol 2026-07-28 is not supported by any released MCP Java SDK at the time of writing.
- Bearer-token constructors exist on `Equinix` and `IAM` only.

## Versioning, migration and licence

The SDK follows [Semantic Versioning](https://semver.org/spec/v2.0.0.html); types annotated `@Beta`
are exempt. 4.0.0 removes deprecated and non-functional surfaces without compatibility shims.
[docs/MIGRATING-4.0.md](docs/MIGRATING-4.0.md) lists each break with its replacement, and
[CHANGELOG.md](CHANGELOG.md) has the full list of changes.

Licensed under the [Apache License, Version 2.0](LICENSE). Contributions: see
[CONTRIBUTING.md](CONTRIBUTING.md).
