# Contributing to Equinix Java SDK

Thank you for your interest in contributing to the Equinix Java SDK! This document provides guidelines and instructions for contributing.

## Development Setup

### Prerequisites
- Java 21 or later (the build is verified on JDK 21 and JDK 27)
- Nothing else: the Maven Wrapper (`./mvnw`, `mvnw.cmd`) downloads the pinned Maven 3.9.x
  (checksum-verified). A local Maven must be 3.9 or later; the enforcer plugin fails older ones.
- An Equinix developer account, only for the live integration tiers

### Building
```bash
git clone https://github.com/iantjones/equinix-java-sdk.git
cd equinix-java-sdk
./mvnw clean verify -DskipTests     # compiles, packages all jars; no GPG key needed
```
`verify` builds the library jar, the sources and javadoc jars and the runnable shaded
`target/equinix-sdk-java-<version>-mcp-server.jar`. GPG signing and the Central Portal publishing
extension only run with `-Prelease` (see [Releasing](#releasing)).

### Test suites (offline, no credentials)

| Command | What runs |
|---|---|
| `./mvnw test` | Default suite: unit, deserialization, doc-contract and spec-conformance tests. Excludes the `wiremock` tag and every live tier. |
| `./mvnw -Pwiremock test` | WireMock request-contract suite (classes extending `core/WireMockTestBase`, tag `wiremock`). |
| `./mvnw -Pcoverage verify` | Both suites in one build (the profile adds a second surefire execution for `wiremock`), then the combined JaCoCo report and `jacoco:check`. Fails under the floors: bundle line 86%, instruction 88%, branch 73%; every package 35% line; IAM/STS client packages 70% line. Floors sit about one point under measured coverage and only move up. Report: `target/site/jacoco/`. |
| `./mvnw javadoc:javadoc` | Javadoc reference gate (see below). |
| `./mvnw package -DskipTests` then `./mvnw test -Dtest=ShadedJarSmokeTest -Dsurefire.failIfNoSpecifiedTests=false -Dmcp.server.jar.required=true` | Starts the shaded MCP server jar over stdio. Without `-Dmcp.server.jar.required=true` the test skips when no jar was built. |
| `./mvnw -Pdependency-check verify` | OWASP dependency-check (fails on CVSS >= 7). Optional `NVD_API_KEY` environment variable for faster NVD downloads. CI uses OSV-Scanner, which needs no key. |

CI (`.github/workflows/build.yml`) runs exactly these gates on every push and pull request, plus an
OSV-Scanner dependency scan. The build also enforces Java 21+, Maven 3.9+, pinned plugin versions
and no duplicate classes on the runtime classpath; dependency-convergence problems are warnings.

**Javadoc reference gate** (broken `{@link}`/`@see` references FAIL the build - `doclint=reference`
plus `failOnWarnings`). Javadoc runs on raw sources, so Lombok-generated getters do not exist when it
looks: never `{@link #getX()}` a Lombok getter - write `{@code getX()}` instead.

**Mutation testing** (PIT, opt-in - too slow for every build, but **`-Ppit` is part of the release
checklist**; run both before cutting a release):
```bash
# Core report (informational)
./mvnw -P pit test-compile org.pitest:pitest-maven:mutationCoverage
# design.value mutation GATE - fails under the threshold
./mvnw -P pit test-compile org.pitest:pitest-maven:mutationCoverage@design-value-gate
```
The `design-value-gate` execution is scoped to `com.eqixiac.equinix.design.value.*` (the rate-card /
TCO / savings money math) with `mutationThreshold=50`. The threshold is a **ratchet**: when a run
measures a comfortably higher kill rate, raise it to just below the measured value in the same PR - 
never lower it.

### Live integration tiers (real Equinix APIs)

Credentials are read from the **environment only**: `EQUINIX_ACCESS_KEY`, `EQUINIX_SECRET_KEY`
and, optionally, `EQUINIX_ACCESS_POLICY_TOKEN` (an STS-issued access-policy token; the IAM
project-scoped roles and principal-policy reads skip without it). Never pass credentials as `-D`
properties: surefire can copy system properties into its XML reports, and they show in the process
list. The easiest way is `.env.local` (gitignored; a `KEY=value` file you create in the repo root) and the runners, which
export only those keys:

```bash
./run-live.sh                         # readonly (default)
./run-live.sh dryrun
./run-live.sh dryrun-existing
./run-live.sh full --confirm-destructive
./run-live.sh scenario --confirm-destructive
.\run-live.ps1 -Tier dryrun            # PowerShell twin, same tiers and switches (-ConfirmDestructive)
```

| Tier (profile = JUnit tag) | What it does | Opt-in |
|---|---|---|
| `integration-readonly` | GET / list / search only. Zero mutations. | profile |
| `integration-dryrun` | Adds `dryRun=true` **creates** and validate endpoints, which leave nothing behind. | profile |
| `integration-dryrun-existing` | `dryRun=true` **updates** against pre-existing resources (no-op payloads). | profile |
| `integration-full` | Full CRUD on real resources, cleaned up LIFO in `@AfterAll`. | profile **and** `-DconfirmDestructive=true` |
| `integration-scenario` | `scenarios/*ScenarioTest`: multi-step flows that create real resources. | profile **and** `-DconfirmDestructive=true` |

Each profile selects its tag and **excludes every more dangerous tag**: JUnit `@Nested` classes
inherit the outer class's tags, so a nested `integration-full` class inside an
`integration-readonly` class would otherwise be selected by the read-only profile.
`core/LiveTierExtension` re-checks the mode at runtime as a second layer. Use full and scenario
only against a sandbox account.

Every live run writes `target/integration-report.json` and `target/site/integration-report.html`,
classifying each test as **PASS**, **SKIPPED** (with the reason: not entitled, no data, a documented
test-data limitation), **SERVER_ERROR** (the endpoint answered 5xx: reported, does not fail the run)
or **FAIL** (assertion, deserialization error, unexpected 4xx: fails the run). In live tests use the
`IntegrationTestBase` helpers: `requireEntitled(...)` (401/403 skip), `skipWhen(..., predicate,
reason, call)` for a documented, non-SDK limitation, `timedExpectedFailure(...)` for calls that must
be rejected (it fails when the call succeeds). Never skip on a deserialization error or an
unexplained 4xx: that is exactly what the tier exists to catch.

`.github/workflows/live-readonly.yml` runs the read-only tier nightly with the repository secrets
`EQUINIX_ACCESS_KEY` / `EQUINIX_SECRET_KEY` (and optional `EQUINIX_ACCESS_POLICY_TOKEN`) as
environment variables, uploads the report, and is skipped when the secrets are not configured.

### Fixtures must come from the spec or the live service

Every JSON file under `src/test/resources/json/` is checked by
`spec/FixtureSpecConformanceTest` (default suite) against the OpenAPI schema it stands for, in the
specifications vendored under `src/test/resources/specs/` (from
docs.equinix.com/.well-known/api-catalog):

- **Source fixtures from the spec example or a captured live response**, never from what the SDK
  model happens to read. Redact identifiers; keep the shape.
- Add every new fixture to `specs/fixture-manifest.json` with its `spec` and `schema` (a JSON
  pointer such as `#/components/schemas/Port`), or `"validate": false` with a `reason` when no
  Equinix spec describes it (PeeringDB, cloud-provider pricing, an endpoint the spec omits). An
  unmapped fixture fails the test.
- When the **live** service differs from the spec, the fixture follows the live service and the
  difference goes into `specs/fixture-deviations.json` with a `reason` (what was observed) and the
  `date`.
- `specs/fixture-pending-review.json` is the baseline of untriaged violations found by the first
  sweep (2026-11-12). It may only shrink: fix the fixture or move the entry to the deviations file
  with evidence. Stale entries (no longer violated) fail the test, so delete them when you fix one.
- When a spec is updated, replace the vendored copy and re-run the default suite.

### Adding an endpoint

1. Check the operation in the current spec (method, path, parameters, request and response
   schemas, `dryRun` support) and, where possible, its live behaviour.
2. Declare it in `src/main/resources/json/apiParams_<Domain>.json`; `core/http/RequestAssembler`
   resolves it.
3. Add the model (interface + Jackson JSON class; enums with an `UNKNOWN` read fallback), the
   internal client method and the public client method, following the patterns in
   [Architecture](#architecture). Javadoc every public type and method, naming the operation.
4. Add a fixture from the spec example or a live capture, plus its manifest entry.
5. Add a unit/deserialization test and a WireMock test that asserts the request on the wire
   (method, path, query, body) and the parsed response.
6. Add a live test to the domain's `*IntegrationTest` in the right tier (reads in
   `integration-readonly`, dry-run creates in `integration-dryrun`, anything touching an existing
   resource in `integration-dryrun-existing`, real mutations in `integration-full`).
7. Run `./mvnw -Pcoverage verify` and `./mvnw javadoc:javadoc`.

### Releasing

`scripts/release.sh` (POSIX) and `scripts/release.ps1` (PowerShell) require a clean working tree,
a pom version equal to the release version, a free `v<version>` tag and a `## [<version>]` section
in `CHANGELOG.md`; then run the same gates as CI (`clean verify -Pcoverage`, `javadoc:javadoc`,
the shaded-jar smoke test) before tagging, creating the GitHub release and, with
`--deploy --gpg-key ID` / `-DeployMavenCentral -GpgKeyId ID`, running
`./mvnw -Prelease deploy -DskipTests`. `--dry-run` / `-DryRun` stops after the gates. Run the PIT
gate and the live read-only tier before releasing.

## Testing standards

These are the house rules for test strength - added after a review found five systemic blind spots
(implementation-pinning tests, untested cross-path invariants, assumption-encoding stubs, missing
negative paths, and gates that measured execution rather than assertion strength). New code is
expected to follow them; reviewers should ask for each of these by name.

1. **Negative-path tests are required for every cache and every external call.**
   - Every cache (negative caches included) needs a *transient-failure-then-retry* test: the first
     lookup fails with a transient error (timeout/5xx/429), and the test proves a later lookup
     retries rather than serving the failure from the cache forever. See the
     `design.value.ratecard.provider` adapter suites for the pattern.
   - Every external HTTP call needs a *timeout* test (WireMock delay + a client timeout budget),
     asserting the SDK's classified handling - not a hang, not an unclassified crash.

2. **Cross-path consistency invariants get parity tests.** When two code paths must produce the
   same artifact (e.g. a dry-run body and the execute-time body), do not test each path against its
   own expectations - extract a shared builder and add a wire-parity test proving both paths emit
   byte-for-byte the same thing. Canonical example: `RouterBodies` (the wizard's router dry-run and
   Phase-1 create share one body builder, locked by its parity test).

3. **Documented behavioral promises get doc-contract tests.** If javadoc promises behavior
   ("returns a copy, never mutates", "empty means cannot-price, never zero", "throws naming the
   offender"), a test must enforce it and quote the javadoc line it locks - see
   `src/test/java/com/eqixiac/equinix/design/DocContractTest.java`. If a promise is deliberately
   changed, change the javadoc and the test's quote in the same commit. A documented promise
   without a test is a defect.

4. **Live-observed API errors get corpus stubs.** Every real error observed against the live API
   gets a regression test stubbed with the REAL error JSON the API returned, asserting the SDK's
   classified handling (error vs. deferred vs. skipped) - see
   `src/test/java/com/eqixiac/equinix/design/optimizer/wizard/LiveErrorCorpusWireMockTest.java`
   (EQ-3040013, EQ-3040063, EQ-3142539, EQ-3142501). Add to the corpus whenever a new live error is
   observed; never trim or "clean up" the observed body.

5. **Stubs must encode observed reality, not assumptions.** A WireMock stub is a claim about how
   the live API behaves. Capture real responses (or the documented catalog spec) and encode those;
   a stub invented from what the author *believes* the API does will happily pass a test while the
   live API rejects the SDK. When a live run contradicts a stub, the stub is the bug - fix the stub
   first, then the code.

6. **Assert strength, not execution.** Coverage says code ran; it does not say anything was
   checked. The javadoc reference gate and the PIT ratchet above exist to measure assertion
   strength - treat a surviving mutant in code you touched as a missing assertion, not as noise.

## How to Contribute

### Reporting Bugs
- Use the [Bug Report](https://github.com/iantjones/equinix-java-sdk/issues/new?template=bug_report.md) template
- Include SDK version, Java version, and the affected domain
- Provide a minimal code example that reproduces the issue

### Requesting Features
- Use the [Feature Request](https://github.com/iantjones/equinix-java-sdk/issues/new?template=feature_request.md) template
- Reference the Equinix API documentation where applicable
- Describe the desired API usage with code examples

### Submitting Changes

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/my-feature`)
3. Follow the existing code patterns (see Architecture below)
4. Add unit tests for new functionality, following the [Testing standards](#testing-standards)
5. Ensure `./mvnw -Pcoverage verify` passes (both suites and the coverage gate) and
   `./mvnw javadoc:javadoc` passes (reference gate)
6. Submit a pull request with a clear description

## Architecture

The SDK follows a layered architecture for each resource:

### Full (Mutable) Resource Pattern
```
Model Interface          - Public API contract (getters)
  -> JSON Model          - Jackson-annotated POJO implementing the interface
  -> Wrapper             - @Delegate wrapper adding mutability methods
  -> Public Interface    - Client interface (list, getByUuid, define)
  -> Public Impl         - Lazy-initialized implementation
  -> Internal Client     - Low-level HTTP client interface
  -> Internal Client Impl - Builds and executes HTTP requests
  -> Operator/Creator    - Fluent builder for create/update operations
```

### Read-Only Resource Pattern
```
Model Interface          - Public API contract (getters)
  -> JSON Model          - Implements interface directly (no Wrapper)
  -> Public Interface    - Client interface (list, getByUuid)
  -> Public Impl         - Lazy-initialized implementation
  -> Internal Client     - Low-level HTTP client interface
  -> Internal Client Impl - Builds and executes HTTP requests
```

### Key Files
- Entry points: `Fabric.java`, `NetworkEdge.java`, `Operations.java`, `IBXSmartView.java`,
  `InternetAccess.java`, `Projects.java`, `IAM.java`, `STS.java`, plus the `Equinix.java` session,
  in `com.eqixiac.equinix`
- Value-add modules: `com.eqixiac.equinix.design.*` (Metro Optimizer, Deployment Wizard, Peering
  Intelligence, Savings/TCO, `design.geo` speed-of-light latency) - the engines depend on the
  narrow `FabricGateway` interface, which makes them straightforward to unit-test against a stub
  gateway
- Config wiring: `*Config.java` + `*ConfigImpl.java` per domain
- API endpoints: `src/main/resources/json/apiParams_*.json`
- Base classes: `ClientBase.java`, `PageableBase.java`

## Code Style

- Use Lombok annotations (`@Getter`, `@Setter`, `@NoArgsConstructor`)
- Use `@JsonProperty` for all Jackson field mappings
- Enums implement `APIParam` with `@JsonCreator fromString()` returning `UNKNOWN` on failure
- All files include the Apache 2.0 license header
- Use fluent builder patterns for resource creation

## License

By contributing, you agree that your contributions will be licensed under the Apache License 2.0.
