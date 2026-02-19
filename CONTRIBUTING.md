# Contributing to Equinix Java SDK

Thank you for your interest in contributing to the Equinix Java SDK! This document provides guidelines and instructions for contributing.

## Development Setup

### Prerequisites
- Java 14 or later
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

**Integration tests** (requires Equinix API credentials):
```bash
mvn test -Pintegration -Dauth.access=YOUR_CLIENT_ID -Dauth.secret=YOUR_CLIENT_SECRET
```

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
4. Add unit tests for new functionality
5. Ensure `mvn clean compile` passes with no errors
6. Ensure `mvn test` passes (unit tests)
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
- Entry points: `Fabric.java`, `CustomerPortal.java`, etc. in `api.equinix.javasdk`
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
