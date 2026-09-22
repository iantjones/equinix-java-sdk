# ADR 0001: The word "interconnect"

| | |
|---|---|
| Status | Accepted |
| Date | 2026-09-21 |
| Scope | Type, enum and package naming across `com.eqixiac.equinix.*`; scope of the cloud-to-cloud work |
| Inputs | Connection Coordinator specification, [github.com/aws/Interconnect](https://github.com/aws/Interconnect) commit `bbfc763` (2026-09-18); Fabric v4 catalog fetched 2026-09-21 |

## Context

The SDK added support for native provider-to-provider cloud links in 3.0.0. The open specification
behind those links, the AWS product built on it, and several existing SDK and Equinix surfaces all
use the word "interconnect" for different objects. A type named `Interconnect` would be ambiguous
at every call site.

### Meanings in use

| # | Meaning | Where it appears | What the object is |
|---|---|---|---|
| 1 | `Interconnect` in the Connection Coordinator specification | `connection-coordinator/schemas/interconnect.yaml`, path `providers/{provider}/environments/{environment}/interconnects/{interconnect}` | A redundancy group of channels (physical LAGs) between two providers. Provider-managed. The customer neither selects nor sees it. |
| 2 | Fabric v4 `XF_IC` | Catalog enum `AccessPointType.XF_IC`; examples `InterconnectCreate`, `InterconnectResponse`, `InterconnectSearchResponse` with `href` `/fabric/v4/interconnects/{uuid}` and a `router` of type `IC_ROUTER`. SDK: `fabric.enums.AccessPointType.XF_IC`, `fabric.enums.CloudRouterType.IC_ROUTER`. | A customer attach point in Fabric. The catalog publishes examples and no path or schema (Beta). |
| 3 | Google Cloud Interconnect | SDK: `fabric.model.implementation.cloud.GoogleCloudInterconnectAdapter`; README "Google Cloud Interconnect" | Google's product name for dedicated and partner connectivity into Google Cloud. The adapter carries a pairing key to a Fabric connection. |
| 4 | Equinix Metal interconnection | SDK: `fabric.model.implementation.MetalInterconnection`; `AccessPointType.METAL_NETWORK` | A Metal resource referenced by uuid as a Fabric access-point target. |
| 5 | The TCO "interconnect" | SDK: `design.value.tco.DeploymentArchetype.EQUINIX_INTERCONNECT`; reference-data key `cspInterconnectPort`; `EgressPath.PRIVATE` ("private interconnect") | The Equinix-to-cloud on-ramp: Fabric connections plus the cloud provider's port fee. This is the colocation baseline that a native link is compared against. |
| 6 | AWS Direct Connect `CreateInterconnect` | AWS Direct Connect API (not in this SDK); encountered by users of `AwsDirectConnectAdapter` | A Direct Connect Partner's trunk on which the partner allocates hosted connections. Predates and is unrelated to meaning 7. |
| 7 | AWS Interconnect | AWS product family, 2026: "AWS Interconnect - multicloud" and "AWS Interconnect - last mile" | The customer-facing AWS product built on meaning 1. The customer resource attaches to a Direct Connect gateway. |

Meanings 1 and 7 share a name and differ in kind: the specification's `Interconnect` is invisible to
the customer, and the AWS product resource called an Interconnect is what the customer creates.
Meaning 2 uses the same word for an Equinix-side attach point. Meanings 3 to 6 existed before this
work.

## Decision

### Naming rule

1. No new type, enum or package in `fabric` is named `Interconnect*`.
2. Vocabulary derived from the specification lives in `com.eqixiac.equinix.core.model.multicloud`
   and uses the names `Multicloud*`, `ActivationKey`, `EnvironmentRef`, `ProviderRef`,
   `ProviderSite`, `BandwidthTier`. The specification's connection-type enum is
   `MulticloudConnectionType` because `fabric.enums.ConnectionType` exists.
3. The name `FabricInterconnect` is reserved for the Fabric v4 `XF_IC` resource (meaning 2) and is
   not used until the catalog publishes a path and schema for it.
4. In `design`, the word is allowed only in a qualified compound that names the native link:
   `DeploymentArchetype.NATIVE_MULTICLOUD_INTERCONNECT`, `EgressPath.MULTICLOUD_INTERCONNECT`,
   `PlannedMulticloudInterconnect`. The existing `EQUINIX_INTERCONNECT` and `cspInterconnectPort`
   keep their names and their meaning (5).
5. The specification's `Interconnect`, `Channel`, `MacSecKey`, `Feature` and `FeatureGuidance`
   objects are not modeled in this SDK. They are provider-internal.
6. `AccessPoint.activationKey` is a separate property from `authenticationKey`.
   `getAuthenticationKey()` is not overloaded to carry an activation key. The Safe Mutation Broker's
   `chg-` confirm tokens are not renamed or described as activation keys.
7. Flat-rate native-link prices are not stored under `cspInterconnectPort`. They have their own
   lookup, `RateCard.multicloudLink(MulticloudLinkRequest)`, and their own reference resource,
   `/json/ratecard_multicloud_reference_2026_09.json`.

### Owner decisions of 2026-09-21

| # | Decision | Effect |
|---|---|---|
| 1 | Build the design-layer native-multicloud path now. `CloudToCloudStrategy` defaults to `COMPARE`. | `TcoCalculator` / `SavingsCalculator` gain `toCloud(...)`; the Deployment Wizard attaches `PlannedMulticloudInterconnect` entries with role `ALTERNATIVE` by default. `EQUINIX_ONLY` reproduces the previous plan, pricing and rendered output. |
| 2 | The Java implementation of the specification ships as a separate repository, `connection-coordinator-java` (planned, not published; `com.eqixiac.interconnect:connection-coordinator-java`, base package `com.eqixiac.interconnect.coordinator`; no repository URL exists yet). | It is not a module of this SDK and has no dependency on it. This SDK has no dependency on it. |
| 3 | The Safe Mutation Broker may gain its first UPDATE change type (bandwidth) when a Fabric One API is published. | Approved in advance. Nothing is built until that API exists. |

### Scoping rulings

| Ruling | Reason |
|---|---|
| The SDK is customer-side. It wraps no AWS, Google Cloud, Oracle or Azure customer API and takes no cloud-provider SDK dependency. The native path ends at plan, price and a documented create-then-accept procedure. | The SDK's credentials and transport are Equinix's. Provisioning at a cloud provider needs that provider's identity, SDK and release cadence. |
| No `FabricOne` domain, stub or gateway seam is added before an API is published. | Equinix announced Fabric One on 2026-09-02. No API reference had been published as of 2026-09-21. Test stubs in this repository encode observed API behavior; there is none to observe. |
| The provider-side implementation of the specification lives in the separate repository of decision 2. | The specification is provider-to-provider. Its audience (providers and network operators) differs from this SDK's (Equinix customers). |
| No `ASideType` enum and no `DeploymentWizard.Builder.aSide()` lever in this work. | Those belong to the A-side work that is on hold. A native cloud-to-cloud link has no Equinix A-side. |
| The Terraform export emits no `resource` block and no cloud-provider `provider` block for a native link. It emits a comment block with the documented create-then-accept procedure and one `<name>_destination_account_id` variable. | No cloud-provider Terraform resource for these products was verified. The activation key is never an input of the configuration. |

## Consequences

- A reader can tell from a type name which object it denotes: `Multicloud*` and `ActivationKey` are
  specification vocabulary, `PlannedMulticloudInterconnect` is a plan entry that is never
  provisioned, `EQUINIX_INTERCONNECT` is the Equinix on-ramp, `XF_IC` is a Fabric enum value.
- `core.model.multicloud` imports only `core`, the JDK, Jackson and Lombok. A test
  (`MulticloudPackageContractTest`) enforces it, so `fabric`, `design` and any later domain can
  share the vocabulary without a dependency cycle.
- Everything derived from the specification or from the Fabric v4 Beta surfaces is labeled
  **Beta** in javadoc. Field names, enum values and the activation-key format can change in a minor
  SDK release to follow the specification.
- If the catalog publishes the `XF_IC` resource, it is modeled as `FabricInterconnect` under rule 3
  and this ADR is amended.
- If a Fabric One API is published, a new ADR records its domain name and its relation to
  `core.model.multicloud`.

## Review triggers

| Signal | Where to look |
|---|---|
| A `/fabric/v4/interconnects` path or an `Interconnect*` schema appears | Fabric v4 catalog `openapi.yaml`, diff against the 2026-09-21 copy |
| New values in `ProviderEnvironmentTypeEnum` or `AccessPointType` | same; the SDK's `UNKNOWN` read-side fallback accepts new values without error, so a diff is the only signal |
| Activation-key format or state enums change | `connection-coordinator/schemas/environment.yaml`, `common.yaml` after commit `bbfc763` |
| A Fabric One API reference is published | docs.equinix.com sitemap and API catalog |
