# Contributing to Equinix Java SDK

Thank you for your interest in contributing to the Equinix Java SDK! This document provides guidelines and instructions for contributing.

## Development Setup

### Prerequisites
- Java 21 or later
- Maven 3.6+
- An Equinix developer account (for integration tests)

### Building
```bash
git clone https://github.com/iantjones/equinix-java-sdk.git
cd equinix-java-sdk
mvn clean compile
```

### Running Tests

**Unit tests** (no credentials required):
```bash
mvn test
```

**WireMock tests** (no credentials required — full request/response cycles against a local stub server):
```bash
mvn test -Pwiremock
```

**Coverage gate** (JaCoCo line/branch thresholds, fails the build if unmet):
```bash
mvn -Pcoverage verify
```

**Integration tests** (requires Equinix API credentials; tiers: `integration-readonly` → `integration-dryrun` → `integration-full`):
```bash
mvn test -Pintegration-readonly -DaccessKey=YOUR_CLIENT_ID -DsecretKey=YOUR_CLIENT_SECRET
```

**Javadoc reference gate** (broken `{@link}`/`@see` references FAIL the build — `doclint=reference`
plus `failOnWarnings`). Javadoc runs on raw sources, so Lombok-generated getters do not exist when it
looks: never `{@link #getX()}` a Lombok getter — write `{@code getX()}` instead:
```bash
mvn javadoc:javadoc
```

**Mutation testing** (PIT, opt-in — too slow for every build, but **`-Ppit` is part of the release
checklist**; run both before cutting a release):
```bash
# Core report (informational)
mvn -P pit test-compile org.pitest:pitest-maven:mutationCoverage
# design.value mutation GATE — fails under the threshold
mvn -P pit test-compile org.pitest:pitest-maven:mutationCoverage@design-value-gate
```
The `design-value-gate` execution is scoped to `api.equinix.javasdk.design.value.*` (the rate-card /
TCO / savings money math) with `mutationThreshold=50`. The threshold is a **ratchet**: when a run
measures a comfortably higher kill rate, raise it to just below the measured value in the same PR —
never lower it.

## Testing standards

These are the house rules for test strength — added after a review found five systemic blind spots
(implementation-pinning tests, untested cross-path invariants, assumption-encoding stubs, missing
negative paths, and gates that measured execution rather than assertion strength). New code is
expected to follow them; reviewers should ask for each of these by name.

1. **Negative-path tests are required for every cache and every external call.**
   - Every cache (negative caches included) needs a *transient-failure-then-retry* test: the first
     lookup fails with a transient error (timeout/5xx/429), and the test proves a later lookup
     retries rather than serving the failure from the cache forever. See the
     `design.value.ratecard.provider` adapter suites for the pattern.
   - Every external HTTP call needs a *timeout* test (WireMock delay + a client timeout budget),
     asserting the SDK's classified handling — not a hang, not an unclassified crash.

2. **Cross-path consistency invariants get parity tests.** When two code paths must produce the
   same artifact (e.g. a dry-run body and the execute-time body), do not test each path against its
   own expectations — extract a shared builder and add a wire-parity test proving both paths emit
   byte-for-byte the same thing. Canonical example: `RouterBodies` (the wizard's router dry-run and
   Phase-1 create share one body builder, locked by its parity test).

3. **Documented behavioral promises get doc-contract tests.** If javadoc promises behavior
   ("returns a copy, never mutates", "empty means cannot-price, never zero", "throws naming the
   offender"), a test must enforce it and quote the javadoc line it locks — see
   `src/test/java/api/equinix/javasdk/design/DocContractTest.java`. If a promise is deliberately
   changed, change the javadoc and the test's quote in the same commit. A documented promise
   without a test is a defect.

4. **Live-observed API errors get corpus stubs.** Every real error observed against the live API
   gets a regression test stubbed with the REAL error JSON the API returned, asserting the SDK's
   classified handling (error vs. deferred vs. skipped) — see
   `src/test/java/api/equinix/javasdk/design/optimizer/wizard/LiveErrorCorpusWireMockTest.java`
   (EQ-3040013, EQ-3040063, EQ-3142539, EQ-3142501). Add to the corpus whenever a new live error is
   observed; never trim or "clean up" the observed body.

5. **Stubs must encode observed reality, not assumptions.** A WireMock stub is a claim about how
   the live API behaves. Capture real responses (or the documented catalog spec) and encode those;
   a stub invented from what the author *believes* the API does will happily pass a test while the
   live API rejects the SDK. When a live run contradicts a stub, the stub is the bug — fix the stub
   first, then the code.

6. **Assert strength, not execution.** Coverage says code ran; it does not say anything was
   checked. The javadoc reference gate and the PIT ratchet above exist to measure assertion
   strength — treat a surviving mutant in code you touched as a missing assertion, not as noise.

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
5. Ensure `mvn clean compile` passes with no errors
6. Ensure `mvn test` passes (unit tests) and `mvn javadoc:javadoc` passes (reference gate)
7. Submit a pull request with a clear description

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
- Entry points: `Fabric.java`, `NetworkEdge.java`, `CustomerPortal.java`, `IBXSmartView.java`,
  `InternetAccess.java`, `Projects.java`, `IAM.java`, `STS.java`, plus the `Equinix.java` session,
  in `api.equinix.javasdk`
- Value-add modules: `api.equinix.javasdk.design.*` (Metro Optimizer, Deployment Wizard, Peering
  Intelligence, Savings/TCO, `design.geo` speed-of-light latency) — the engines depend on the
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
