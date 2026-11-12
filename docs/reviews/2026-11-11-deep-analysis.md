# equinix-java-sdk 3.0.0: deep analysis and release-notes gap review

Date: 2026-11-11. Commit `1cf71599` (main).

## How this review was done

The review was split into seven areas, each read end to end:

1. core, Fabric and Network Edge
2. the other API domains
3. the design layer
4. the MCP server
5. the test environment
6. release notes for Fabric, Fabric One, the MCP server and Network Edge
7. release notes for every other product

The highest-impact claims were then re-checked directly.

**Sources.**

- **Equinix docs index.** `https://docs.equinix.com/AGENTS.md` was used as the entry point to the documentation; the bare `/AGENTS` URL returns 403 to automated fetches. It says to:
  - select pages from `llms.txt`
  - fetch the `.md` variants of pages
  - discover specs from the RFC 9727 catalog at `/.well-known/api-catalog`
- **API specs.** All findings are checked against the current OpenAPI specs from that catalog:
  - Fabric v4 **4.34.0**
  - network-edgev1 1.0.0
  - accessv1, stsv1, and 30 more
- **Equinix's Go SDK specs.** Used for cross-checking, including the patches Equinix applies to its own specs.

**Labels used below.**

- **[V-repro]**: reproduced directly, by code read, spec grep, unauthenticated `curl` or `java` run.
- **[V-spec]**: verified against the spec.
- **[I]**: inferred from code reasoning.

**Build verification.** JDK 21 and Maven 3.9.9 were run from a scratch directory; no Java toolchain is installed on the machine. The full CI pipeline is green:

| Step | Result |
|---|---|
| `mvn clean test` | 1534 tests, 0 failures, 0 skipped |
| `mvn -P wiremock test` | 1295 tests, 0 failures |
| `mvn -P coverage jacoco:check` | passes |

Combined coverage is 87.2% line, 89.3% instruction and 75.4% branch. The gate is set at 75% line and instruction, with no branch limit.

---

## 1. What the SDK is and intends to be

A hand-written, opinionated Java SDK over the Equinix REST APIs (package `com.eqixiac.equinix`). It is in three layers.

**API layer.** Clients per domain:

| Domain | Coverage |
|---|---|
| Fabric v4 | about 197 endpoints |
| Network Edge | 66 endpoints |
| Customer Portal / Colocation | orders, Smart Hands, tickets, shipments, work visits, billing, reports, Digital LOA |
| Other APIs | IBX SmartView, EIA v2, IAM (Access Management), STS, Resource Manager projects |

The API layer also provides:

- fluent builders, `dryRun` support, live wrappers and pagination
- retries with idempotency rules and 429 / `Retry-After` handling
- a circuit breaker
- cloud adapters for AWS, Azure, GCP and Oracle

**Design layer.** Planning and cost tools built on top of the API layer:

- the Metro Optimizer, and a Deployment Wizard that goes from plan through validation, dry-run and execution to Terraform and diagram export
- Peering Intelligence: PeeringDB, resiliency and speed-of-light latency
- the value layer: rate cards, live cloud-egress adapters, TCO and Savings calculators
- a Beta cloud-to-cloud / native-multicloud planner (ActivationKey decoding, AWS Interconnect-multicloud pricing)

**Intelligence MCP Server.** A stdio MCP server, shipped as a shaded jar. It exposes the design, Fabric, portal, Network Edge and IBX tools read-only by default. An opt-in `mutate` toolset is guarded by a **Safe Mutation Broker** with this flow:

1. propose
2. real dry-run
3. single-use token
4. human elicitation
5. execute

**Overall assessment.** The engineering is unusually careful:

- **Field names:** JSON field names match the spec almost exactly. The diff found one real mismatch, on ServiceToken.
- **Endpoint coverage:** coverage of non-Beta spec operations is near-complete.
- **Contract tests:** the WireMock suite asserts real request contracts, with about 900 body assertions.
- **Documentation:** the docs carry dated provenance.

The defects found fall into a few groups:

- a wrong host for IAM and STS
- wire-format details: content types, multipart, PUT vs PATCH, timestamp parsing, `page` vs `pagination`
- broker gating that is weaker than the README claims
- tests whose fixtures were written to match the SDK, so they cannot catch these bugs

---

## 2. Critical and high findings (fix first)

| # | Sev | Area | Finding | Evidence |
|---|---|---|---|---|
| C1 | **Critical** | IAM / STS | **Every IAM (~33) and STS (10) call goes to `api.equinix.com`, which does not serve them (404).** The spec hosts are `access.eqix.equinix.com` and `sts.eqix.equinix.com`. There is no per-domain base-URL override, and the OAuth mint must stay on `api.equinix.com`. | [V-repro] `api.equinix.com/v1/roles` returns 404, while `access.eqix.equinix.com/v1/roles` returns 401. `api.equinix.com/.well-known/openid-configuration` returns 404, while the `sts.eqix...` one returns 200. `apiParams_Core.json:3`, `IAM.java:67-76`, `STS.java:51-61` |
| H1 | High | STS | **The token exchange sends camelCase form fields** (`grantType`, `subjectToken`...). The server only parses RFC 8693 `grant_type` / `subject_token`. The spec schema is wrong and the SDK followed it. | [V-spec, live probe] `sts/model/json/creators/TokenRequest.java:97-106` |
| H2 | High | STS | **Response models expect camelCase, but the server returns snake_case** (`jwks_uri`, `token_endpoint`, and very likely `access_token` / `expires_in`). Fields are silently null. | [V-repro] the live discovery doc is snake_case. `OpenIdConfigurationJson.java:37-52`, `StsTokenJson.java:33-43` |
| H3 | High | STS | **Every STS call first forces a client-id/secret login.** The spec marks `/v1/token`, `/jwks` and discovery as unauthenticated. This defeats workload identity federation. | `EquinixClient.java:370-383`, `STS.java:94-150` |
| H4 | High | MCP broker | **Mutations run without any human check when the client lacks elicitation.** `fabric_confirm_change` proceeds on `UNSUPPORTED_BY_CLIENT`, so a model or prompt injection can propose and confirm in one turn. The README claim "humans confirm - enforced by the server" is overstated. | [V-repro] `BrokerToolFactory.java:322`, `ElicitationSupport.java:202` |
| H5 | High | Customer Portal | **Four list calls silently stop at page 1.** The affected calls are attachments, finance accounts, reportCenter reports and assets search. The spec returns the pagination block under `page`, but `Page.java` only reads `pagination`. The fixtures use `pagination`, so the tests pass. | [V-repro] `core/http/response/Page.java:60` has no alias |
| H6 | High | Fabric | **Connection create cannot set `accessPoint.location.metroCode`.** Every Colo/VD/FCR-to-SP example in the spec sends it for AWS, Azure, GCP and Oracle, but `SimpleAccessPoint` has no `location`. Cloud connection creates are likely rejected or misrouted. | [V-repro] the field is absent. Whether the API enforces it is unverified. |
| H7 | High | Fabric | **`PrecisionTimes.list()` calls `GET /fabric/v4/timeServices`, which does not exist.** The spec has only POST create and `POST /timeServices/search`, and the SDK has no search. | [V-repro] `apiParams_Fabric.json:633` |
| H8 | High | Fabric | **Stream alert-rule update uses PATCH; the spec defines only PUT** (body `AlertRulePutRequest`). | [V-repro] `apiParams_Fabric.json:879` |
| H9 | High | Network Edge | **License-file upload sends JSON `{"file":...}`; the spec requires `multipart/form-data`.** `FilesClientImpl` already does multipart correctly. The test asserts the wrong body. | [V-repro] `DeviceClientImpl.java:159,166` |
| H10 | High | Tests / security | **Live credentials are written in plaintext into surefire `TEST-*.xml` `<properties>`.** They are passed as system properties, and 57 entries were found in the current reports. | `pom.xml:405-481`, `run-live.ps1:14` |

## 3. Medium findings

### Core / Fabric / Network Edge

- **Timestamp parsing (M-T) [V-repro].** `DATE_TIME_FORMAT` (`Constants.java:159`) fails on 4, 5, 7, 8 and 9 fractional digits and on `+00:00` offsets. Parsed offsets are dropped. The spec's own example `2025-02-04T00:20:54.511118575Z` fails, and one bad timestamp fails the whole response. Fix: use `DateTimeFormatterBuilder` with `appendFraction(NANO_OF_SECOND,0,9,true)` plus `appendOffset("+HH:MM","Z")` and normalise to UTC.
- **ServiceToken `changelog` [V-repro].** The spec spells it lowercase `changelog`; the SDK maps `changeLog` with no alias, so it is always null. The fixtures copy the SDK's spelling (`ServiceTokenJson.java:91`).
- **`XF_IC` vs `IC` [V-repro].** Spec 4.34.0's `AccessPointType` ends `...APP_LINK, IC`, and `XF_IC` appears nowhere. Real `IC` values decode to `UNKNOWN`, and ADR-0001 is out of date. Also add the `/aSide/accessPoint/interconnect/uuid` search field and the `providerIdentifier`, `productOffering` and `bandwidthCommit` access-point properties.
- **PATCH content types disagree with the spec.**
  - Ports and service tokens send `application/json`; the spec says `json-patch+json`.
  - IP blocks send `json-patch+json`; the spec says `application/json`.
  - Verify these live before changing, because the code comments claim the current values were deliberate.
- **`PUT /serviceProfiles/{id}`** never sends the required `If-Match` header.
- **A 401 is never recovered.** The token is not cleared, re-minted and replayed, so a revoked or skewed token fails every call for up to about an hour (`EquinixClient.java:330-353`).
- **`loadAll()` has no stall guard.** It stops only on server `total`. An empty page, or an endpoint that ignores `offset`, loops forever. Nine spec endpoints return `pagination` but take no `offset`.
- **`CloudRouterWrapper.delete()`** nulls its backing JSON on a 204 response, so a later `refresh()` throws a NullPointerException. Sixteen other wrappers share the pattern.
- **`ExceptionDetail` is not `@JsonIgnoreProperties(ignoreUnknown=true)`.** Any extra error field wipes the parsed error details.
- **NE `updateLicenseToken`** always throws on the spec's 202 empty-body response, even though the server applied the update.
- **Azure adapter** sends the peering location as `sellerRegion`, which none of the spec examples do.

### Other domains

- **Invoice filter NullPointerException [V-repro].** The builder NPEs unless all four filters are set (`customerportal/client/RequestBuilder.java:72-80`), so a date-only query crashes.
- **Order-history account fields** likely map to the wrong JSON names. Equinix's Go SDK patches them to `accountNumber` / `accountName`.
- **Path parameters are not URL-encoded** (`RequestAssembler.java:244-263`). An empty UUID collapses the URL to the collection path.
- **README examples that do not compile or would not work:**
  - `crossConnects().list()`, `troubleTickets().list()` and `workVisits().list()`
  - the STS `.scope("roles")` example

### Design layer

- **Latency units are mixed in the optimizer.** It compares measured round-trip latency (`avgLatency`) with a computed one-way estimate, and a fixed 150 ms placeholder for "no coordinates" leaks into the `maxLatencyMs` filter and results (`MetroOptimizerEngine.java:1382-1427`).
- **Wizard rollback can orphan billable Cloud Routers.** It deletes routers without waiting for the asynchronous connection deletes to finish (`DeploymentPlan.java:1321-1357`).
- **SavingsCalculator (single-cloud) is inconsistent with TCO.** It omits the cloud-provider port and cross-connect that TCO includes, about $219 a month for AWS 1G. Partial results still populate `netMonthly` with fabricated values.
- **Budget check mixes currencies.** It compares totals with no currency check (`MetroOptimizerEngine.java:2955`).
- **Cost line items double-count bandwidth** (`:879-896`). For example, a total of 1000 is itemised as 1500.
- **Google reference port data is wrong.**
  - It uses 720 hours a month where the rest of the SDK uses 730.
  - It has only a 10G row, so every Google leg, even a 50 Mbps one, is priced as 10G Dedicated.
  - This skews cloud-to-cloud break-even calculations.
- **Terraform export drops the Azure `peering_type`.**
- **PeeringDB `netfac?asn=` filter.** `netfac` has `local_asn`, not `asn`, and the single-ASN path does not filter on the client side. This needs a direct curl comparison of the two filters to confirm (confidence low-to-medium).
- **`MULTI_METRO` forces a cross-region spread.** The README does not document this.
- **Tie-breaking and cost scoring.** Ties are broken by API order, and the "COST" strategy is really a fixed regional constant, contrary to the README.

**Verified correct in the design layer:**

- ActivationKey decoding against the aws/Interconnect schema, including redaction
- AWS Interconnect-multicloud rates ($12.33/h and $51.78/h) and the free tier
- AWS Direct Connect port and data-transfer rates
- Azure Zone 1 egress
- break-even maths and unit conversions
- `PlanValidator` gating

### MCP server

- **Elicitation timeout has no effect.** `requestTimeout` is never set on the MCP SDK builder, so its 10 s default overrides `EQUINIX_MCP_ELICIT_TIMEOUT_MS` (5 minutes). Humans get about 10 s to confirm, and results report `failed` instead of `timed_out`.
- **The confirm prompt is incomplete and can be spoofed.**
  - It omits notification emails and project/PO fields.
  - It omits the access-grant fields for service tokens.
  - A model-supplied `name` can contain newlines that forge prompt lines.
- **`ChangeCompiler` silently drops known keys with the wrong type,** and truncates decimals. The SHA-256 and the human-reviewed spec therefore include values that are never sent.
- **Debug logging writes request bodies, including the OAuth `client_secret`,** to stderr. Hosts persist stderr.
- **The MCP Java SDK is pinned at 2.0.0** (2.1.0 makes stdio sends thread-safe). The server negotiates up to protocol 2025-11-25 only, while the current spec revision is 2026-07-28 (stateless, multi-round-trip requests replacing `elicitation/create`).
- **`connect-claude-desktop.ps1`** replaces the whole server entry. That silently drops `EQUINIX_SANDBOX` and `TOOLSETS`, so a sandbox user is switched to production, and it writes the secret into the config in plaintext.
- **Smaller issues:**
  - the README's "no data leaves the machine except api.equinix.com" ignores the cloud pricing APIs and PeeringDB
  - `spec_matches_binding` always passes
  - eviction of the 21st proposal is silent
  - `.env.local` parsing keeps quotes and `export` prefixes
  - `notifications/cancelled` is not handled

**Verified correct in the MCP server:**

- `mutate` is opt-in at three layers
- the dry-run is real
- tokens are single-use with 96-bit entropy and replay detection
- nothing but JSON-RPC is written to stdout
- `isError` results and `structuredContent` are used correctly

### Test environment

- **Error fixtures are Fabric-shaped for every domain.** The real IAM `{"error":{...}}`, STS `{error, errorDescription}` and Smart Hands `{"errors":[string]}` shapes are untested. `ResponseErrorMapper` loses their code and message, but the tests only assert the exception type.
- **Fixtures written from the SDK, not the spec**, hide real bugs:
  - H5 (`page` vs `pagination`)
  - ServiceToken `changeLog`
  - the router type `CLOUD_ROUTER` (should be `XF_ROUTER`)
  - the license-file JSON body
  - the `updateLicenseToken` body
  - 14 unused fixtures
  - Recommendation: add a default-suite test that validates every fixture against its spec schema. A JSON Schema validator is already on the classpath through the MCP SDK.
- **The live scenario tier can never run.** The `integration-scenario` tag has no profile and is excluded by default, leaving about 1,600 lines of dead tests.
- **Tags do not separate destructive tests from read-only ones.** `@Nested` `integration-full` classes inherit `integration-readonly`; only a runtime double opt-in protects them.
- **No live tier runs in CI or on a schedule.** `run-live.sh`, referenced in the example env file, does not exist.
- **The release script runs only `mvn test`** before an irreversible `deploy -DskipTests` with `autoPublish=true`. It skips the WireMock, coverage, javadoc and PIT gates.
- **CI never builds or smoke-starts the shaded MCP jar.**
- **The coverage gate is about 12 points below actual** and has no branch limit. CONTRIBUTING's `mvn -Pcoverage verify` neither checks coverage nor works without GPG.
- **Missing tests:**
  - pagination stall
  - concurrent re-auth
  - 401 recovery
  - non-Fabric error bodies
  - shaded-jar startup
- **Flaky timing tests** use a 50 ms circuit-breaker cooldown; switch them to an injected clock.
- **Dependencies:**

  | Dependency | Current | Latest | Note |
  |---|---|---|---|
  | WireMock | 3.5.4 | 3.13.x | bundles a Jetty with known CVEs |
  | MCP SDK | 2.0.0 | 2.1.0 | |
  | JUnit | 5.10.3 | | |
  | surefire | 3.3.1 | | |
  | jacoco | 0.8.13 | 0.8.16 | 0.8.13 cannot instrument JDK 25 classes |

  The build also has no Maven wrapper, no enforcer, no Dependabot, and its GitHub Actions are not SHA-pinned.

---

## 4. Release notes: what is new and what changed (as of 2026-11-11)

The API release-notes pages lag behind:

- Fabric API notes stop at R2026.1 (2026-02-16).
- Network Edge API notes stop at R2024.5.
- Customer Portal API notes stop at 2024-09.

Product release notes and the specs are more current, so later changes were inferred from the spec diff.

### Fabric (API v4, spec 4.34.0)

- **R2026.1 (Feb 2026):**
  - Fabric Cloud Router term/package update through `PATCH /routers/{id}`
  - metro-latency metrics (64×64)
  - metrics carry name, state and type
  - geoScopes UK, AU, CH and BR
- **R2025.5 (Oct 2025):**
  - on-demand-to-term connection upgrade
  - the `equinixASN` field removed from BGP create, and route `age` removed
  - stream count fields
  - alert-rule `metricSelector` and `OUTLIER` detection
- **Product releases 2026.2-2026.5:**
  - Dynatrace sink with an `API_TOKEN` credential
  - NE device streaming (`/streams/{id}/devices/{deviceId}`)
  - project-level active alerts (likely `POST /liveEvents/search`)
  - Digital LOA tokens (Beta `/loas*`)
  - Geo Zones GA
  - EVPL VLAN edit
  - TPID selection
  - 50G multipoint, 100G connections, 400G ports
  - immediate connection delete
  - E-Tree
- **Fabric Cloud Router:**
  - IPv6 on IP-WAN
  - policy limit raised from 32 to 100 rules
  - route aggregation on IP-WAN
  - ping and traceroute
  - BASIC package
  - IBM DL 2.0 and Alibaba
- **Beta families in the spec:**
  - ASNs, IX Services, Application Connect (39 operations)
  - Optical Metro Connects, v4 Cross Connects, LOAs
  - Port Packages, Tags, Logos
  - Internet Access and IP Blocks
- **Deprecated in the spec:**
  - `/{asset}/{id}/metrics` and `/cloudevents` (replaced by the `/search` endpoints)
  - `/ports/{id}/stats` and `/connections/{id}/stats`
  - `ServiceToken.expiry` and `issuerSide`
  - EIA routing-protocol `customerAsn`, `customerAsnRange` and `bgpAuthKey`
  - several port fields

### Fabric One

**Preview.** A managed, metro-scoped, any-to-any routed "Fabric Interconnect" backed by an Equinix-managed Cloud Router.

- Lab package only: 3 Gbps, free, non-production, 66 metros.
- Endpoints: AWS Interconnect (1G, Ashburn and Silicon Valley, activation-key flow), AWS Direct Connect, Azure ExpressRoute, Google Partner Interconnect, Oracle FastConnect and Fabric ports.
- **Portal-only: there is no public Fabric One API.** Its only traces in the spec are `IC`, `IC_ROUTER`, `IC_PROFILE`, `IC_ENV`, activation-key validation and the `interconnect/uuid` search field.

### Equinix MCP servers

- **Fabric MCP server:** hosted at `https://mcp.equinix.com/fabric`, in **Public Preview**.
  - **Auth:** OAuth 2.1 (dynamic client registration or client ID metadata documents) with a Customer Portal browser login.
  - **Permissions:** you pick `fabricViewer`, `fabricOperator` or `fabricManager` at consent, revocable through AI Consent Management (IAM, Aug 2026).
  - **Tools:** about 127-135, Fabric plus about 21 Network Edge tools, including create and update tools but **no delete tools**. There is no server-side dry-run or confirm step.
  - **Clients:** Claude Desktop and Claude Code, Cursor, VS Code, Codex, Gemini CLI and OpenCode.
- **Also available:**
  - the `equinix-fabric` skills plugin (`equinix/equinix-agent-plugins`)
  - an experimental Docs MCP server
- **SDK drift:** the README and the server's runtime `instructions` still say "private beta" and "Fabric-only".

### Network Edge

- **2026 product releases:**
  - Meraki vMX, Netskope, 6WIND, NIOS-X, Thales vHSE and Prisma AIRS
  - FIPS
  - DLG term length
  - operational status and power-on
  - 25G connections to EIA
  - zero-network deploys
  - Geo Zones
  - new metros: Johannesburg, and Kamloops replacing Vancouver
- **The published spec is still `/ne/v1` 1.0.0 and has no v2 endpoints.** Most 2026 features are not in it.
- **The SDK's `/ne/v1/bgp*` and `/vpn*` endpoints are not in the published spec.**

### Other products

- **Experience updates (2026):**
  - product codes are being simplified: `SERV.SH`, `IC.EFP`, `IC.EIA`, `COLO.SC`, `IC.CC` and others, through the end of 2026
  - billing moves from Billing Agreements to order-level billing (Order #, CAN/BAN)
  - **Trouble Tickets are being folded into Support Cases**, with a new "Service Restoration" order type
- **EIA:**
  - 2026-05: IP Blocks become standalone and survive service termination
  - 2026-07: the **Unified EIA API** is available in Fabric v4 at `/fabric/v4/internetAccessServices` and `/ipBlocks`
- **Precision Time:** Draft and Cancelled states (Oct 2025).
- **Smart View:** System Alerts v2, and `dcim/v3/powerEvents` (spec only).
- **Messaging Gateway:** an AMQP/AsyncAPI service; the SDK has no client for it.
- **Equinix Metal:** EOL on **2026-06-30**, with the console going offline 2027-01-01.
- **Other end-of-life events:**
  - Marketplace moved into the Fabric Marketplace (2026-06-30)
  - Ethernet Exchange EOL 2026-01-31
  - Equinix Connect EOL extended "until further notice" (2026-10-28)
- **Auth:** the password grant is deprecated, with EOL in January 2025. The SDK sends only `client_credentials`, which is correct. STS / OIDC token exchange is Limited Availability on `sts.eqix.equinix.com`.

### Gap summary

| Status | Items |
|---|---|
| **Supported** | FCR package/term · on-demand-to-term · geoScopes · BGP fields (MED, AS-override, routesMax) · route filters and aggregation · ping/traceroute · provider environments and `validateActivationKey` · `GW_VC`/`IPX_VC` · `IC_PROFILE`/`IC_ENV` · Precision Time states · Access Management (39/39 operations) · Colocation v2 · Billing v1/v2 · Smart View v2/dcim v3 · EIA v2 · all 54 NE spec operations |
| **Broken or mismatched** | C1/H1-H3 (IAM/STS) · H7 (timeServices list) · H8 (alert-rule PUT) · `XF_IC`->`IC` · PATCH content types · ServiceToken `changelog` |
| **Uses deprecated** | asset metrics, cloudevents and stats endpoints · service-token `expiry` · EIA `customerAsn`/`bgpAuthKey` · `GET /v1/orders/workvisit/locations` (use Lookup v2 `/colocations/v2/locations`) · Metal types |
| **Missing (GA)** | Dynatrace sink and `API_TOKEN` · stream, streamAsset and streamSubscription search · NE device streaming · `liveEvents/search` · `timeServices/search` · typed TPID/VLAN updates · access-point `providerIdentifier` · NE `Vendor` values (Infoblox, 6WIND, Thales, Netskope...) · refresh-token endpoint · Messaging Gateway |
| **Missing (Beta)** | about 91 Fabric operations: ASNs, IX, Application Connect, Optical, v4 Cross Connects, LOAs, `GET /tags/{id}`, `POST /logos` |
| **In the SDK but not in the published spec** | `/fabric/v4/agents*`, `/agentTemplates*`, NE `/bgp*` and `/vpn*` |
| **Documentation drift** | MCP comparison section · EIA Unified API reachable only through `Fabric`, not `InternetAccess` · no Support Case migration guidance · CHANGELOG has no `[3.0.0]` section |

---

## 5. Prioritised remediation plan

**P0: correctness bugs that break real calls**

1. **Fix IAM and STS.** Add a per-domain base URL (`access.eqix...` and `sts.eqix...`, overridable). Use snake_case STS form fields and response aliases. Allow unauthenticated STS operations without Equinix credentials.
2. **Fix pagination.** Add `@JsonAlias("page")` on `Page.pagination`, and add a stall guard (empty page or offset not advancing) to `loadAll()`.
3. **Fix the timestamp parser:** variable fractional digits, `+HH:MM` offsets, and normalisation to UTC.
4. **Fix these Fabric operations:**
   - `PrecisionTimes`: use `POST /timeServices/search`
   - alert-rule update: use PUT
   - `AccessPointType`: add `IC`, and keep `XF_IC` as a tolerated alias
   - `SimpleAccessPoint`: add `location.metroCode`
   - ServiceToken: add `@JsonAlias("changelog")`
   - service-profile PUT: send `If-Match`
5. **Fix Network Edge.** Send the license upload as multipart, and handle the 202 empty response in `updateLicenseToken`.
6. **Close the MCP broker gaps:**
   - fail closed when the client has no elicitation support
   - set `requestTimeout`
   - type `ChangeCompiler` strictly and hash the compiled request
   - put every field that will execute in the confirm prompt, and strip control characters
   - redact `client_secret` from debug logs
7. **Remove secrets from the surefire XML** (`enablePropertiesElement=false`, or read credentials from environment variables).

**P1: reliability and accuracy**

8. **Core resilience:**
   - re-mint and replay once on 401
   - make `ExceptionDetail` ignore unknown fields
   - parse error bodies per domain
   - URL-encode path parameters
   - make wrappers safe after `delete()`
   - make the invoice filter builder null-safe
9. **Design layer:**
   - put all latency in one unit
   - remove the 150 ms placeholder
   - wait for connection deletes during rollback
   - include port and cross-connect in Savings
   - check currency in the budget check
   - fix the line-item double count
   - fix the Google reference row (730 h, partner pricing by size)
   - add the Azure `peering_type` to the Terraform export
   - fix the PeeringDB `local_asn` filter
10. **Tests:**
    - add a spec-schema validation test for every fixture, then fix the fixtures it flags
    - replace error fixtures with per-domain ones
    - add a scenario profile
    - add a scheduled read-only live job
    - make the release script run the full CI gate set
    - add a shaded-jar smoke test
    - raise the coverage floor and add a branch limit

**P2: currency with the release notes**

11. Deprecate the asset-id metrics, cloudevents and stats methods and service-token `expiry`. Add an ASN client and migrate EIA routing-protocol fields to `bgpAuthType` plus the ASN reference.
12. Add Dynatrace and `API_TOKEN`, stream searches, NE device streaming, `liveEvents/search`, typed TPID/VLAN updates, `providerIdentifier`, and the NE vendor values.
13. Mark `WorkVisits.listLocations` deprecated and point it to Lookup v2. Add Support Case migration guidance, and soft-deprecate Trouble Tickets.
14. Surface `Fabric.eiaServices()` and `ipBlocks()` from the `InternetAccess` client, and document that IP Blocks survive service deletion.
15. Bump the MCP SDK to 2.1+ and plan for MCP protocol 2026-07-28. Bump WireMock, JUnit, surefire and jacoco.
16. Update the README's MCP comparison and data-egress claims, fix the README examples, and add the CHANGELOG `[3.0.0]` section.

**P3: Beta and optional work**

17. Beta families behind `@Beta`: ASNs, IX, Application Connect, Optical, Cross Connects and LOAs.
18. A Messaging Gateway module.
19. Track the catalog for a Fabric One public API.
20. Prepare for the end-2026 product-code cut-over.

## 6. Unverified, needs a live account or confirmation from Equinix

- whether the API enforces `location.metroCode` on service-provider connection create
- the correct PATCH content types for ports, service tokens and IP blocks
- the snake_case shape of the STS token response
- order-history account field names
- the PeeringDB `asn` filter behaviour
- whether `/agents` and the NE `/bgp` and `/vpn` endpoints still exist
- the Fabric One and MCP launch dates
- whether `liveEvents` is the active-alerts API
- the wire value for `SERVICE_RESTORATION`

---

## 7. Live validation results (2026-11-11)

Run against a real account. These were the only kinds of request sent:

- GET and search calls
- `dryRun=true` creates, which leave no resource behind
- public PeeringDB queries
- the MCP server over stdio, with only its read-only toolsets enabled

The following were deliberately not sent:

- updates, deletes or real creates
- dry-run updates or deletes on existing resources
- the dry-run test tier

| Item | Result |
|---|---|
| IAM host | `api.equinix.com/v1/roles` 404; `access.eqix.equinix.com` 200 (79 roles via SDK) |
| Customer Portal `page` envelope | finance accounts, reports, attachments, assets search all return `page` |
| `location.metroCode` on SP z-side | **required**: without it HTTP 400 EQ-3142501; with it dry run 200 (raw and SDK) |
| Precision Time | `GET /timeServices` 502; unfiltered `POST /timeServices/search` 200 (586 services) |
| Service-profile ETag | `"5"` (quoted) |
| `/fabric/v4/agents`, `/agentTemplates` | exist (200) despite absence from spec |
| NE `/ne/v1/vpn` / `/ne/v1/bgp` | 200 / 403 (permission) |
| Order history | live field names differ from spec (`accountName/accountNumber`, `orderingContacts`, `notificationContacts[]`) -> **fixed** |
| STS | discovery snake_case; errors `{"error","error_description"}`; scope must be an access-policy ERN or `roleassignments:/projectpolicies:/principalpolicy:` |
| Fabric `avgLatency` | round-trip (SV->DC 65.8 ms) -> optimizer mixed units -> **fixed** |
| Live rate card | every lookup HTTP 400 (type-only scan) -> **fixed** (targeted queries; EVPL 1G DC = USD 200) |
| PeeringDB `netfac?asn=` | ignored by PeeringDB (unrelated rows) -> **fixed** (`net_id`) |
| Fabric `/metros` `ibxs` | absent live -> peering presence was always empty -> **fixed** (resolve by live metro codes) |
| Savings vs TCO | single-cloud savings omitted the provider port -> **fixed** (AWS 1G: net 3081 not 3300) |
| MCP `portal_list_open_tickets` | HTTP 400 (`TROUBLE_TICKET`) -> **fixed** (`TROUBLE`) |
| MCP stdout / secrets | stdout pure JSON-RPC; secret absent from stderr even at debug (`"client_secret":"***"`) |
| MCP `serverInfo.version` | was `0.0.0-dev` -> **fixed** (3.0.0) |
| MCP `design_plan_deployment` | correctly refuses to guess among 13 billable accounts without elicitation |

**Not re-verified live:** Peering Intelligence end to end (PeeringDB throttled anonymous calls
for about an hour, so the fixes are covered by unit tests built from the live response shapes),
the STS token-exchange success shape, and the PATCH content types for ports and service tokens.

### Live read-only tier (274 tests)

| Run | Failures + errors | Skipped |
|---|---|---|
| Before the fixes | 51 | 84 |
| After the fixes | 35 | 84 |

None of the remaining 35 are response-parsing or request-shape defects in the SDK:

- **IAM project-scoped calls** need an STS access-policy token: roles by project and principal policies.
- **Test inputs the services reject:**
  - Customer Portal and IBX tests send no IBX `accountNo`.
  - Digital LOA search sends an empty `filter.and`.
  - Cloud-events and metrics searches are missing the mandatory `subject` filter.
  - Tests pick a route filter or connection they cannot read.
  - NE ping and interface stats target a device type that does not support them.
- **Server-side 5xx and 404s:**
  - assets search
  - network changes
  - ports search
  - Digital LOA organizations, and the Digital LOA private-beta endpoints (404)
  - NE device pricing
  - IAM principal policies

Offline suites after all fixes:

| Suite | Result |
|---|---|
| Default | 1571 tests, 0 failures |
| WireMock | 1313 tests, 0 failures |
| Coverage gate | passes |
| Javadoc gate | passes |

---

## 8. Resolution in 4.0.0

This section records what release 4.0.0 (2026-11-12) did with each finding above. "Fixed" means the
code changed and, where stated, the change was checked against the live service. "Open" means the
finding still stands. The live checks were read-only: GET and search calls and `dryRun=true` creates.
[`CHANGELOG.md`](../../CHANGELOG.md) has the details and [`MIGRATING-4.0.md`](../MIGRATING-4.0.md) the
replacements for removed API. Offline test counts at the time of writing are in the README
[Testing](../../README.md#testing) section.

### Critical and high findings

| # | Status | Resolution |
|---|---|---|
| C1 | Fixed, live | IAM and STS use their own hosts; `setDomainEndPoint(area, url)` overrides one area |
| H1, H2 | Fixed, live for discovery | RFC 8693 form fields; snake_case responses read. The success shape of the token exchange is still unverified |
| H3 | Fixed | Unauthenticated STS operations send no credentials; `STS.withoutCredentials()` |
| H4 | Fixed | `fabric_confirm_change` fails closed without elicitation unless `EQUINIX_MCP_ALLOW_UNCONFIRMED_MUTATIONS=true`; the token is bound to the SHA-256 of the compiled request |
| H5 | Fixed, live | `page` read as an alias of `pagination` |
| H6 | Fixed, live | `location.metroCode` on access points; the four cloud adapters require a metro (HTTP 400 `EQ-3142501` without it) |
| H7 | Fixed, live | `PrecisionTimes.list()` calls `POST /timeServices/search` |
| H8 | Fixed | Alert-rule update uses PUT |
| H9 | Fixed | License upload is multipart |
| H10 | Fixed | Live-tier credentials come from environment variables only |

### Medium findings

| Area | Finding | Status |
|---|---|---|
| Core | Timestamp parsing | Fixed: 0 to 9 fractional digits and offsets, normalised to UTC |
| Core | 401 never recovered | Fixed: one token refresh and one replay, single-flight |
| Core | `loadAll()` stall | Fixed: stops on an empty page or a repeated offset |
| Core | Wrappers after `delete()` | Fixed: 18 Fabric wrappers and the Network Edge, Customer Portal and IBX SmartView wrappers keep their state; `isDeleted()` |
| Core | `ExceptionDetail` and error bodies | Fixed: unknown fields ignored, every service's error format parsed, unparseable bodies kept as a snippet |
| Core | Path parameters not encoded | Fixed |
| Core | Flaky timing tests | Fixed: injectable `Clock` in the circuit breaker, waiter and HTTP client |
| Fabric | ServiceToken `changelog` | Fixed |
| Fabric | `XF_IC` against `IC` | Fixed: `IC` added; `XF_IC` and `CX_PORT` removed |
| Fabric | Missing access-point properties and search field | Fixed: `providerIdentifier`, `productOffering`, `bandwidthCommit`, `interconnect`, `ConnectionSearchField` |
| Fabric | PATCH content types | Changed to the specification; networks (`json-patch+json` only) and route filters and rules (both accepted) verified live. Ports, service tokens and IP blocks remain unverified live: open |
| Fabric | Service-profile `If-Match` | Fixed; not verified live (needs a port) |
| Fabric | Azure `sellerRegion` | Fixed: not sent |
| Network Edge | `updateLicenseToken` 202 | Fixed |
| Other | Invoice filter NPE | Fixed |
| Other | Order-history field names | Fixed, live |
| Other | README examples that did not compile | Fixed: README rewritten; its Java examples were compiled against the 4.0.0 tree |
| Design | Mixed latency units, 150 ms placeholder | Fixed: round-trip throughout; unknown stays unknown |
| Design | Rollback orphaning routers | Fixed: waits for connection deletes; orphans reported |
| Design | Savings omitted the provider port | Fixed, live (AWS 1 Gbps in DC: USD 3,081 net, not 3,300) |
| Design | Budget across currencies | Fixed: one comparison currency, conversion with stated sources |
| Design | Double-counted bandwidth | Fixed |
| Design | Google reference row | Fixed: reference data rebuilt with provider rows by connection model and bandwidth at 730 h |
| Design | Terraform Azure `peering_type` | Fixed |
| Design | PeeringDB `asn` filter | Fixed: queries by `net_id`; not re-verified end to end live |
| Design | `MULTI_METRO` forcing cross-region | Fixed: `MULTI_METRO` is any regions, `MULTI_REGION` spans two |
| Design | API-order ties, regional cost constants | Fixed: Pareto ties, rate-card costs |
| MCP | Elicitation timeout ignored | Fixed: `requestTimeout` set from `EQUINIX_MCP_ELICIT_TIMEOUT_MS` |
| MCP | Incomplete, spoofable prompt | Fixed: every field shown as an escaped JSON literal; strings with control characters rejected |
| MCP | `ChangeCompiler` dropping mistyped values | Fixed: rejected |
| MCP | Secrets in debug logs | Fixed |
| MCP | MCP Java SDK 2.0.0 | Fixed: 2.1.0. Protocol 2026-07-28 is not in a released SDK: open |
| MCP | Connect script replaced the entry and wrote the secret | Fixed: merge, backup, no secret by default; `connect-claude-desktop.sh` added |
| MCP | Egress claim, `spec_matches_binding`, silent eviction, `.env.local` parsing, cancellation | Fixed: host list documented, `request_matches_binding`, evictions reported, strict parser, `notifications/cancelled` honoured |
| Tests | Fixtures written from the SDK | Changed: every fixture is validated against 26 bundled specifications; 89 deviations recorded; 231 mismatches listed in `fixture-pending-review.json` as a baseline: open |
| Tests | Scenario tier never runs | Fixed: `integration-scenario` profile with a double opt-in |
| Tests | No scheduled live tier | Fixed: nightly `live-readonly.yml` |
| Tests | Release script skipped gates | Fixed: the release scripts run the CI gates before tagging |
| Tests | No shaded-jar test | Fixed: `ShadedJarSmokeTest` in CI |
| Tests | Coverage floor and branch limit | Fixed: line 86%, instruction 88%, branch 73%, 35% per package |
| Tests | Dependencies, wrapper, enforcer, Dependabot, SHA pinning | Fixed |

### Remediation plan items P2 and P3

| Item | Status |
|---|---|
| 11. Deprecated metrics, cloud-events and stats methods; service-token `expiry`; ASN client; EIA routing-protocol fields | Done: removed rather than deprecated; ASNs under `fabric.beta().asns()`; EIA BGP takes `asn(uuid)` and reads `bgpAuthType` |
| 12. Dynatrace, stream searches, NE device streaming, `liveEvents/search`, TPID and VLAN updates, `providerIdentifier`, NE vendors | Done. VLAN update path not verified live |
| 13. Work Visit locations, Support Case guidance | Done: Lookup v2 locations; `TroubleTickets` Javadoc describes the move to Support Cases |
| 14. EIA and IP Blocks on `InternetAccess` | Done: `unifiedServices()` and `ipBlocks()` |
| 15. MCP SDK and test dependency upgrades | Done |
| 16. README MCP comparison, egress claims, examples; CHANGELOG `[3.0.0]` | Done |
| 17. Fabric Beta families | Done: 82 operations under `fabric.beta()` |
| 18. Messaging Gateway | Done: receiver for Azure Service Bus; no order management or signature verification |
| 19. Fabric One public API | Open: no public API found |
| 20. Product-code cut-over | Open |

### Section 6 items

| Item | Status |
|---|---|
| `location.metroCode` enforced | Confirmed live: required |
| PATCH content types | Networks, route filters and rules confirmed; ports, service tokens and IP blocks open |
| STS token-response shape | Open |
| Order-history field names | Confirmed and fixed |
| PeeringDB filter | Fixed by `net_id`; end-to-end live check open |
| `/agents`, NE `/bgp` and `/vpn` | Confirmed to exist (200; NE BGP 403 for lack of permission) |
| `liveEvents` as the active-alerts API | `CloudEvents.searchLiveEvents` added; needs exact subjects; whether it is the active-alerts API is unconfirmed |
| `SERVICE_RESTORATION` wire value | Open |

### Live read-only tier after 4.0.0

On 2026-11-12 the read-only tier reported 178 PASS, 66 SKIPPED, 7 SERVER_ERROR and 7 FAIL, against 35
failures before. Four defects it exposed were then fixed and covered by WireMock tests from the live
shapes: the metrics page size (at most 10), the EIA v1 price `prefixLength` returned as an array, the
trouble-ticket types key, and the SmartView `tagId` parameter. The tier was not re-run in full after
those fixes.
