# MCP server reference

The SDK jar contains an MCP server, `equinix-sdk-java-intelligence`, in
`com.eqixiac.equinix.mcp.server`. This page is the reference for release 4.0.0; the
[README](../README.md#mcp-server) has the summary. The server is a community project, unrelated to
the Fabric MCP server Equinix hosts. All latency in tool results is round-trip milliseconds.

## Contents

- [Running it](#running-it)
- [Settings](#settings)
- [Toolsets](#toolsets)
- [Tool arguments](#tool-arguments)
- [Result conventions](#result-conventions)
- [Resources and prompts](#resources-and-prompts)
- [Safe Mutation Broker](#safe-mutation-broker)
- [Cancellation](#cancellation)
- [Untrusted text](#untrusted-text)
- [Embedding](#embedding)
- [Security guidance](#security-guidance)

## Running it

```bash
./mvnw -q package -DskipTests
java -jar target/equinix-sdk-java-4.0.0-mcp-server.jar --toolsets design,ibx --env-file /path/to/.env.local
```

The host starts the process and speaks JSON-RPC on its stdin and stdout. The server has no network
listener. stdout carries only JSON-RPC; every diagnostic, including the startup line, goes to stderr.
The server negotiates MCP protocol versions 2024-11-05, 2025-03-26, 2025-06-18 and 2025-11-25
through the bundled MCP Java SDK 2.1.0.

Claude Desktop: `./connect-claude-desktop.sh` (macOS and Linux) or `.\connect-claude-desktop.ps1`
(Windows) registers the server. The script:

- merges an `equinix` entry into `claude_desktop_config.json`, keeping an existing entry's `env`,
  extra arguments, Java command and other fields, and updating only the jar path;
- backs the config up to `claude_desktop_config.json.bak-<timestamp>` first;
- writes no secret by default: the entry gets `cwd` set to the repository and
  `--env-file <repo>/.env.local`, and credentials embedded by an older script version are removed;
- copies the credentials into the config's `env` block only with `--embed-credentials`
  (`-EmbedCredentials`);
- refuses to run while Claude Desktop is running, because it rewrites its config on exit.

Other hosts take the same command line: `java -jar <jar> [--toolsets ...] [--env-file ...]`, with
the settings below in the host's `env` block.

## Settings

Credentials are read from the process environment, then from the file named by `--env-file`, then
from `.env.local` in the working directory. Only the two credential keys are read from the file;
every other setting must be a real environment variable.

| Variable | Default | Meaning |
|---|---|---|
| `EQUINIX_ACCESS_KEY`, `EQUINIX_SECRET_KEY` | none | Client credentials. Missing: exit status 2 |
| `EQUINIX_MCP_TOOLSETS` (or `--toolsets`) | every toolset except `mutate` | Comma-separated toolset ids |
| `EQUINIX_SANDBOX` | `false` | `true` targets the sandbox environment |
| `EQUINIX_MCP_LOG_LEVEL` | `info` | slf4j-simple level, written to stderr |
| `EQUINIX_PEERINGDB_KEY` | none | PeeringDB key for `design_analyze_peering`; anonymous access otherwise |
| `GCP_BILLING_API_KEY` | none | Enables live Google Cloud egress prices |
| `EQUINIX_MCP_PRICING_TIMEOUT_MS` | 12000 | Live-pricing budget of one tool call, shared by every lookup in it, Equinix included. Accepted 100 to 120000; other values are clipped or replaced with a warning |
| `EQUINIX_MCP_ELICIT_TIMEOUT_MS` | 300000 | Hard timeout of one prompt to a person (account and service-profile choices in `design_plan_deployment`, confirmation in `fabric_confirm_change`). Accepted 100 to 3600000 |
| `EQUINIX_MCP_ALLOW_UNCONFIRMED_MUTATIONS` | `false` | `true` lets `fabric_confirm_change` execute on a client that cannot show a prompt |

`.env.local` syntax: `KEY=value`, an optional `export ` prefix, single-quoted literal values,
double-quoted values with `\"`, `\\`, `\n`, `\r` and `\t` escapes, and a `#` comment after
whitespace. A UTF-8 byte order mark is ignored. Any other line stops the server with exit status 3
and a message that names the line number and key and never the value.

The pricing budget starts at the first live price lookup of a call. When it runs out, the remaining
lookups use reference prices and the result's `equinix_live_pricing` block says how many lookups
failed and why, grouped by message. In testing on 2026-11-12 the first live Fabric price search
alone took more than 12 s, so with the default every price for an LD and NY design came from the
reference card (USD 3,181.80 per month); with 120000 the same call used live prices (USD 3,865.12
per month) and took 45.7 s. Set 60000 to 120000 when live prices matter.

## Toolsets

| Id | Tools |
|---|---|
| `design` | The 16 `design_*` tools except `design_analyze_estate`. Also enables the resources and prompts |
| `estate` | `design_analyze_estate`: reads the account's deployed Fabric inventory and metrics |
| `fabric` | `design` and `estate` |
| `ops` | `ops_list_open_tickets`, `ops_get_billing_summary` |
| `ne` | `ne_list_devices` |
| `ibx` | `ibx_get_environmentals`, `ibx_list_power_events` |
| `mutate` | `fabric_propose_change`, `fabric_confirm_change`. Never selected by default |

The default selection is every id except `mutate`: 22 tools. `estate` is separate from `design` so a
deployment can serve design tools without giving the model read access to the account's inventory.

## Tool arguments

The design tools that list `detail` below accept `summary`, `standard` or `full`.

| Tool | Arguments (required first, then optional) |
|---|---|
| `design_spec_template` | optional `format` (`yaml` or `json`), `include_schema` |
| `design_validate_spec` | `spec`; optional `format` |
| `design_optimize` | `spec`; optional `detail` |
| `design_analyze_resilience` | `spec` or `spec_fingerprint`; optional `portfolio`, `failures`, `max_pair_scenarios`, `detail` |
| `design_growth_and_terms` | `spec` or `spec_fingerprint`; optional `portfolio`, `scenarios`, `detail` |
| `design_estimate_tco` | `spec` or `spec_fingerprint`; optional `cloud_id`, `portfolio`, `peer_cloud_id`, `path_tier`, `archetypes`, `power_kw`, `on_prem_transit_per_mbps_month`, `on_prem_hardware_monthly`, `on_prem_cross_connect_monthly`, `on_prem_power_per_kw_month` |
| `design_compare_cloud_egress` | `spec` or `spec_fingerprint`; optional `cloud_id`, `portfolio` |
| `design_estimate_latency` | `from`, `to` (metro codes or IBX codes) |
| `design_compare_cloud_to_cloud` | `a`, `z`, `bandwidth_mbps`; optional `sustained_mbps`, `term_months`, `path_tier`, `use_aws_free_tier`, `metro_code`, `cloud_router_package` |
| `design_list_multicloud_environments` | optional `a`, `z` |
| `design_analyze_peering` | `spec` or `spec_fingerprint`; optional `customer_asn`, `detail` |
| `design_plan_deployment` | `spec` or `spec_fingerprint`; optional `deployment`, `detail` |
| `design_get_plan` | `plan_id`; optional `detail` |
| `design_plan_drift` | `plan_id`; optional `detail` |
| `design_export_terraform` | `plan_id` |
| `design_decision_record` | `spec` or `spec_fingerprint`; optional `title`, `status`, `build_plan`, `offset` |
| `design_analyze_estate` | `project_ids` (1 to 10); optional `window_days`, `max_resources_per_type` (default 500, at most 2000), `detail`, `finding_ids` |
| `ops_list_open_tickets` | optional `ibxs`, `include_closed`, `limit` |
| `ops_get_billing_summary` | none |
| `ne_list_devices` | optional `limit` |
| `ibx_get_environmentals` | `ibx`; optional `type`, `zone`, `limit` |
| `ibx_list_power_events` | `ibxs`; optional `statuses`, `limit` |
| `fabric_propose_change` | `change_type` (`connection_create`, `network_create`, `service_token_create`), `spec` |
| `fabric_confirm_change` | `confirm_token` |

`spec` is a DesignSpec as a JSON object or as a JSON or YAML string. `spec_fingerprint` names a
`design_optimize` result stored in this process; the tool then reuses that run's spec and
recommendation. The `on_prem_*` and `power_kw` arguments of `design_estimate_tco` describe the
on-premises comparison.

`design_plan_deployment` sends Fabric dry-run POSTs only and provisions nothing. It returns a
`plan_id` (16 hex characters, a hash of the plan content) and the plan document; `design_get_plan`,
`design_plan_drift` and `design_export_terraform` take that id. `design_plan_drift` needs the plan to
have a project id. `design_decision_record` returns Markdown in pages of 16,000 characters; follow
`markdown_next_offset` with `offset`.

## Result conventions

- Design results carry `spec_fingerprint` and a `provenance` block naming the sources used.
- `detail` caps lists at 5 (`summary`), 15 (`standard`) or 50 (`full`) entries; a capped list
  reports how many were left out in a `*_omitted` field.
- Costs state their price basis (live, reference, estimate or composite) and range, and unknown
  values are reported as unknown.
- Optimisation results and plans are kept in an in-process store for 60 minutes, at most 20 of each
  kind. A result that names an evicted or expired entry says so in `evicted_from_store`.

## Resources and prompts

With the `design` toolset (or `fabric`) the server serves:

| Resource | Content |
|---|---|
| `equinix-design://schema/design-spec` | The DesignSpec JSON Schema |
| `equinix-design://spec/template` | The commented starter spec (YAML) |
| `equinix-design://plans` | The index of plans stored in this process |
| `equinix-design://plans/{plan_id}` | One stored plan document |

| Prompt | Arguments | Walks the model through |
|---|---|---|
| `design_from_requirements` | `requirements`, optional `currency` | Template, validation, optimisation, resilience, optionally growth, plan, Terraform and decision record |
| `review_existing_estate` | `project_id` | `design_analyze_estate` for one project and a prioritised action list (only with `estate` or `fabric`) |
| `what_breaks_if` | `failure`, optional `spec_fingerprint` | `design_analyze_resilience` for one failure |

## Safe Mutation Broker

The `mutate` toolset creates Fabric connections, networks and service tokens. It never deletes.

1. `fabric_propose_change` compiles the `spec` into the exact create request, runs the Fabric dry
   run, and returns `compiled_request`, `request_sha256`, the price context and a single-use
   `confirm_token` that expires after 10 minutes. The store holds 20 proposals; minting a 21st
   evicts the oldest, and the result lists `evicted_proposals`.
2. `fabric_confirm_change` consumes the token, recompiles the stored spec and checks that the new
   request hashes to the bound `request_sha256`, asks a person through MCP elicitation with every
   field of the request shown, checks the expiry again after the answer, and then sends the create.

Every confirm result carries `confirm_checks` (`proposal_exists`, `not_evicted`, `not_expired`,
`not_previously_used`, `request_matches_binding`) and `human_confirmation.status`:

| Status | Executed | Token afterwards |
|---|---|---|
| `accepted` | yes | consumed |
| `declined`, `cancelled`, `timed_out`, `failed` | no | consumed; propose again |
| `unsupported_by_client` | no, unless `EQUINIX_MCP_ALLOW_UNCONFIRMED_MUTATIONS=true` | consumed |

A client that did not declare form elicitation cannot be prompted, so by default the server refuses
to execute. Set `EQUINIX_MCP_ALLOW_UNCONFIRMED_MUTATIONS=true` only when the host shows its own
per-tool approval; the result then reports that no server-side check ran. Service-token expiry is
counted from the proposal time and sent as `expirationDateTime`. A service-provider Z-side needs
its metro code. The tool titles state that the creates are billable.

## Cancellation

The server honours `notifications/cancelled`. A running design engine is stopped and the call
returns a cancellation error. For `fabric_confirm_change`, a cancel before the token is consumed
leaves the token usable; a cancel during the prompt abandons the prompt and sends no create; a
cancel that arrives after the create was sent cannot undo it, and the result reports
`executed: true` with `cancellation.requested_after_send: true`.

## Untrusted text

Strings that come from remote systems (resource names, descriptions, error messages, PeeringDB
records) are sanitised: control and Unicode format characters are removed (newlines and tabs become a space)
and each field is capped at 512 characters
(20,000 for a whole document such as a decision-record page, 2,000 for a remote error). The result
lists the JSON paths of these fields in a top-level `untrusted_data` block with a note that they are
data, not instructions. The server's `instructions` tell the model the same.

## Embedding

```java
EquinixMcpServer server = EquinixMcpServer.builder()
        .credentials(accessKey, secretKey)          // or .session(existingEquinix)
        .toolsets(Toolset.DESIGN, Toolset.IBX)
        .build();                                   // registers tools and serves stdio
```

Embedding needs the optional `mcp-core` and `mcp-json-jackson2` dependencies.
`Builder.additionalTools(ToolRegistration...)` registers your own tools through the same
`ToolRegistration` type the built-ins use; `Builder.stdio(in, out)` and
`Builder.transportProvider(...)` replace the transport. `EquinixMcpServer.catalog(toolsets)` lists
the static catalogue without starting a server.

## Security guidance

- Create a separate API application in the Equinix Developer Portal for agent use, so the agent has
  its own audit trail and a key you can revoke alone.
- Grant it only the entitlements the selected toolsets need. `EQUINIX_MCP_TOOLSETS=design,ibx`
  serves nothing else.
- Enable `mutate` only where a person reviews every proposal, and try it with `EQUINIX_SANDBOX=true`
  first.
- Keep credentials in `.env.local` (ignored by git) or per-user host configuration, not in a shared
  repository, and rotate them like any other credential.
- The server contacts the hosts listed in the [README](../README.md#hosts-the-sdk-contacts) and
  nothing else.
