# Cloud-to-cloud paths (Beta)

Everything on this page is Beta. Two cloud providers can be joined by a link the providers build
between themselves, with no Equinix resource on the path. AWS announced general availability of such
links with Google Cloud on 2026-04-14 and with Oracle Cloud on 2026-07-29. The SDK models that link as
a third path next to the public internet and the Equinix Fabric path, prices all three on the same
traffic, and reports the sustained rate at which the lowest-cost path changes. It plans and prices
the native link and does not create it: the SDK calls no AWS, Google Cloud, Oracle or Azure API for
this and has no dependency on a cloud-provider SDK. Naming and scope decisions are recorded in
[ADR 0001](../adr/0001-interconnect-vocabulary.md).

| Surface | Package | Data |
|---|---|---|
| Two-sided TCO and savings comparison, native-link rate lookups | `design.value.tco`, `design.value.savings`, `design.value.ratecard` | Bundled native-link prices retrieved 2026-09-21 |
| Native links on a deployment plan | `design.optimizer.wizard` | Bundled region-pair catalogue retrieved 2026-09-21 |
| Activation-key codec and specification vocabulary | `core.model.multicloud` | Connection Coordinator specification, commit `bbfc763` (2026-09-18) |
| Provider environments, activation-key validation, `activationKey` and `environment` on an access point | `fabric` | Fabric v4 catalogue, Beta operations |

## The provider-to-provider specification

The Connection Coordinator API (`github.com/aws/Interconnect`) is an OpenAPI 3.0.3 specification
maintained by Amazon Web Services and Google. Providers speak it to each other; customers do not.
Its resource hierarchy is `providers/{provider}/environments/{environment}/interconnects/{interconnect}`;
commit `bbfc763` defines 35 operations. An environment is a pair of provider sites with the
connection sizes they support, in Mbps.

The customer sees three steps. At provider A the customer selects an environment, a size and the
account identifier it uses at provider B, and receives an activation key. The customer enters the
key at provider B, which confirms it with provider A. The providers then configure the link and
report it verified. Each provider defines its own customer-facing operations for the first two steps.

## Economics

A native link is billed by each provider as a flat hourly fee by size, with no per-GB charge. The
Equinix path has a lower fixed cost (two Fabric virtual connections, a Cloud Router, each provider's
interconnect port, a cross-connect) and a per-GB private-egress charge at each cloud. The native link
therefore costs more at low utilisation and less at high utilisation.

Bundled native-link prices (`/json/ratecard_multicloud_reference_2026_09.json`, USD, retrieved
2026-09-21):

| Provider side | Size (Mbps) | USD per hour | USD per month at 730 h |
|---|---|---|---|
| AWS, path tier 1 | 10000 | 12.33 | 9,000.90 |
| AWS, path tier 4 | 10000 | 51.78 | 37,799.40 |
| Google Cloud, North America and Europe | 1000 / 5000 / 10000 / 100000 | 3.50 / 17.30 / 19.00 / 146.60 | 2,555.00 / 12,629.00 / 13,870.00 / 107,018.00 |
| Google Cloud, APAC | 1000 / 5000 / 10000 / 100000 | 5.00 / 24.90 / 26.40 / 196.10 | 3,650.00 / 18,177.00 / 19,272.00 / 143,153.00 |
| Google Cloud, South America | 1000 / 5000 / 10000 / 100000 | 7.60 / 38.00 / 46.90 / 299.60 | 5,548.00 / 27,740.00 / 34,237.00 / 218,708.00 |

AWS publishes no other size or tier; Oracle Cloud and Azure native-link prices are not bundled. Both
AWS and Google Cloud state that the native link carries no per-GB data-transfer charge.

Lookup rules:

- The requested size must equal a published size; the card does not round, interpolate or
  extrapolate native-link rates.
- A side with no published rate is unpriced with a reason. The archetype is then partially priced
  and never recommended.
- The AWS path tier (1 for local scope to 5) is a caller input, default 1. AWS assigns the tier from
  the regions the interconnect serves and publishes no path-to-tier table.
- The AWS free tier is applied only with `useAwsFreeTier(true)`.
- Hourly rates convert to monthly at 730 h (`MulticloudLinkQuote.HOURS_PER_MONTH`).

Break-even for symmetric traffic of `g` GB per month in each direction
(`MulticloudPathComparison.breakEvenSustainedMbps(...)`):

```text
g*     = (nativeFixedMonthly - equinixFixedMonthly) / (equinixPerGbSum - nativePerGbSum)     GB each way
result = 2 x g* x 8000 Mb/GB / (730 h x 3600 s/h)                                            Mbps, both directions summed
```

`equinixPerGbSum` is the two clouds' private-egress rates added together. The result is empty when an
input is unpriced, when currencies differ, or when the two cost lines do not cross.

## Comparing three archetypes

`toCloud(...)` makes the TCO comparison two-sided. The example uses only bundled figures, so its
output is reproducible offline.

```java
TcoComparison tco = fabric.tcoComparison()
    .egress(100, DataUnit.TERABYTE)               // AWS to Google Cloud, per month
    .reverseEgress(40, DataUnit.TERABYTE)         // Google Cloud to AWS, per month
    .fromCloud(CloudProviderType.AWS).inRegion("us-east-1")
    .toCloud(CloudProviderType.GOOGLE_CLOUD).toRegion("us-east4")
    .viaMetro(MetroCode.DC)
    .bandwidthMbps(10_000)
    .includeCloudRouter("STANDARD")
    .pathTier(1)
    .term(Term.MONTH_12)
    .archetypes(DeploymentArchetype.PUBLIC_CLOUD_INTERNET,
                DeploymentArchetype.EQUINIX_INTERCONNECT,
                DeploymentArchetype.NATIVE_MULTICLOUD_INTERCONNECT)
    .rateCard(ReferenceRateCard.standard())
    .compare();
```

Result with the 4.0.0 reference data (USD; one-time charges 0.00):

| Archetype | Monthly | Total over 12 months | Line items |
|---|---:|---:|---|
| `PUBLIC_CLOUD_INTERNET` | 13,472.00 | 161,664.00 | 100,000 GB x 0.09 (AWS) + 40,000 GB x 0.1118 (Google Cloud) |
| `EQUINIX_INTERCONNECT` (recommended) | 8,477.20 | 101,726.40 | private egress 2,000.00 + 744.00; two Fabric connections 2 x 350.00; Cloud Router 1,200.00; provider ports 1,810.40 (AWS) + 1,722.80 (Google Cloud); cross-connect 300.00 |
| `NATIVE_MULTICLOUD_INTERCONNECT` | 22,870.90 | 274,450.80 | 12.33 x 730 h (AWS) + 19.00 x 730 h (Google Cloud), no data-transfer charge |

140 TB per month is a sustained 426 Mbps summed over both directions. `SavingsCalculator` with the same
inputs reports a break-even of 2,750.4 Mbps:
2 x ((22,870.90 - 5,433.20) / 0.0386) GB x 8000 / 2,628,000 s. Below it the Equinix path costs less
per month; above it the native link does. The figure moves with every input: a negotiated Fabric rate,
another Cloud Router package or a Google Cloud region outside North America changes it.

AWS publishes no 1000 Mbps rate, so a 1 Gbps comparison reports the native archetype as partially
priced until you supply the rate, for example with
`CustomRateCard.builder().currency("USD").multicloudLinkHourlyRate(CloudProviderType.AWS, 1_000, 1, rate)`
layered in front of `ReferenceRateCard.standard()`.

## Native links on a deployment plan

`DeploymentWizard.Builder.cloudToCloudStrategy(...)` takes `COMPARE` (default), `EQUINIX_ONLY`,
`NATIVE_WHEN_AVAILABLE` or `NATIVE_ONLY`. Under `COMPARE` the Equinix connections are planned and each
cloud pair with a catalogue environment gets one `PlannedMulticloudInterconnect` with role
`ALTERNATIVE`. `EQUINIX_ONLY` plans the Equinix path only. `NATIVE_WHEN_AVAILABLE` omits the Equinix
connections where the replacement rule allows and carries the link as a `REPLACEMENT`. `NATIVE_ONLY`
treats a pair without a usable environment as a validation error.

```java
DeploymentPlan plan = fabric.deploymentWizard(result)
    .cloudToCloudStrategy(CloudToCloudStrategy.COMPARE)
    .notifications("noc@example.com")
    .plan();
for (PlannedMulticloudInterconnect link : plan.multicloudLinksOrEmpty()) {
    System.out.println(link.getName() + ": " + link.describe() + ", role " + link.getRole());
    link.getCreateThenAcceptRecipe().forEach(step -> System.out.println("  " + step));
}
```

`plan.execute()` sends no request for a native link; each link is reported once in
`DeploymentOutcome.getInformational()`. The Terraform export writes links as comment blocks with the
provider procedure and a `<name>_destination_account_id` variable, and adds no resource for them.

## Activation keys

`ActivationKey.decode(String)` (package `core.model.multicloud`) reads the specification's
base64-encoded JSON key and returns `ActivationKey.V1` (cleartext fields), `ActivationKey.V2Encrypted`
(only `destinationEnvironmentUri()` is readable; the SDK does not decrypt) or `ActivationKey.Opaque`.
`toString()` and Jackson serialisation never include key material; `encode()`,
`V1.sharedConnectionUuid()`, `V1.destinationAccountId()` and `Opaque.raw()` are the only accessors
that return it. The specification publishes no example key, and the format changed between commits
`8173027` and `bbfc763`.

Two Beta Fabric operations on an `IC_PROFILE` service profile are wired:
`serviceProfiles().getEnvironments(profileUuid)` (`GET .../serviceProfiles/{id}/environments`, all
pages) and `serviceProfiles().validateActivationKey(profileUuid, environmentUuid, key)` (`POST
.../environments/{id}/actions`, type `VALIDATE_ACTIVATION_KEY`). The catalogue publishes no response
example, so every getter of the response can be `null`. Access points accept `activationKey` and
`environment`; the four cloud adapters send no activation key. Send such a connection with
`dryRun()` first.

## MCP tools

| Tool | Engine |
|---|---|
| `design_compare_cloud_to_cloud` | `SavingsCalculator` with `toCloud(...)`: three paths and `break_even_sustained_mbps` |
| `design_list_multicloud_environments` | `MulticloudEnvironmentCatalog` |
| `design_estimate_tco` with `peer_cloud_id` | `TcoCalculator` with `toCloud(...)` |
| `design_plan_deployment` with `deployment.cloud_to_cloud_strategy` | The wizard strategy above; links returned as `native_multicloud_links` |

## Limits

- The SDK is customer-side and does not implement the provider-to-provider specification.
- No Equinix Fabric One API had been published as of 2026-09-21; the SDK has no Fabric One client.
- Bundled prices and the region-pair catalogue are dated copies of provider pages. An absent pair or
  size means it is not in this copy, not that it is not offered.
- The break-even assumes symmetric traffic and list prices; discounts, commitments and the AWS free
  tier change it.
- The live provider price-list adapters return no native-link or multicloud-egress rates.
