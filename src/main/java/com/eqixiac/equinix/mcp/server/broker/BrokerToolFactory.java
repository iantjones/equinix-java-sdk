/*
 * Copyright 2021 Ian Jones. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.eqixiac.equinix.mcp.server.broker;

import com.eqixiac.equinix.design.value.ratecard.PriceQuote;
import com.eqixiac.equinix.design.value.ratecard.RateCard;
import com.eqixiac.equinix.design.value.ratecard.Term;
import com.eqixiac.equinix.fabric.enums.ConnectionType;
import com.eqixiac.equinix.mcp.server.HumanConfirmation;
import com.eqixiac.equinix.mcp.server.ServerContext;
import com.eqixiac.equinix.mcp.server.ToolRegistration;
import com.eqixiac.equinix.mcp.server.Toolset;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The Safe Mutation Broker's two tools — {@code fabric_propose_change} and
 * {@code fabric_confirm_change} — registered through the exact same {@link ToolRegistration}
 * seam as the read-only catalog, in the opt-in {@link Toolset#MUTATE} toolset.
 *
 * <p>The two tools returned by one {@code tools(...)} call share one {@link ProposalStore},
 * so a token minted by that propose tool is confirmable only by its paired confirm tool —
 * i.e. within one server instance. The store-taking overload is the test seam (deterministic
 * clocks, pre-seeded proposals).</p>
 *
 * <h2>Human confirmation at confirm time</h2>
 * <p>{@code fabric_confirm_change} asks the MCP client to show the change to a person before it
 * executes ({@link ServerContext#confirmWithHuman(String)}). The order is fixed: consume the token,
 * verify the spec hash, prompt, execute. The prompt states the change type, the target, the price
 * context the proposal reported, the proposal's age and the spec SHA-256.</p>
 * <table>
 *   <caption>Confirm outcomes by human-confirmation status</caption>
 *   <tr><th>{@code human_confirmation.status}</th><th>Executed</th><th>Token afterwards</th></tr>
 *   <tr><td>{@code accepted}</td><td>yes</td><td>consumed</td></tr>
 *   <tr><td>{@code declined}, {@code cancelled}, {@code timed_out}, {@code failed}</td><td>no</td>
 *       <td>consumed; a new proposal is required</td></tr>
 *   <tr><td>{@code unsupported_by_client}</td><td>yes, without a prompt</td><td>consumed</td></tr>
 * </table>
 * <p>{@code unsupported_by_client} keeps the behaviour the broker had before the prompt existed:
 * a client that did not declare form elicitation cannot be prompted, so approval rests on the
 * calling agent having shown the proposal to a person. The result says so. The prompt is the
 * broker's counterpart of the "Confused Deputy Resolution" requirement in the Connection
 * Coordinator specification ({@code connection-coordinator/docs/Protocols.md}): evidence that the
 * inciting action came from the customer. <b>Beta</b>: that section of the specification is marked
 * work in progress, and the counterpart here is an analogy, not an implementation of it.</p>
 *
 * <h2>Vocabulary</h2>
 * <p>The {@code chg-} confirm token is a process-local lookup key. It is not an activation key:
 * activation keys are issued by cloud providers for native multicloud links and are entered at
 * the other provider. Tool text never calls the token an activation key, so a model has no reason
 * to put one where the other belongs.</p>
 *
 * <p>Annotation honesty: both tools carry {@code readOnlyHint=false} because the pair exists
 * to mutate — but the propose phase itself provisions <em>nothing</em> (its API call is the
 * spec-documented {@code dryRun=true} validation), and {@code destructiveHint} is explicitly
 * {@code false} on both: every change type is a purely additive create, and no delete or
 * update tools exist.</p>
 */
public final class BrokerToolFactory {

    private BrokerToolFactory() {
    }

    /**
     * Builds the broker tool pair over a fresh {@link ProposalStore} (10-minute single-use
     * tokens). Called once per server build, so each server instance gets its own store.
     *
     * @return the propose/confirm registrations, sharing one store
     */
    public static List<ToolRegistration> tools() {
        return tools(new ProposalStore());
    }

    /**
     * Builds the broker tool pair over the given store — the seam tests use for deterministic
     * clocks and pre-seeded proposals.
     *
     * @param store the proposal store both tools share
     * @return the propose/confirm registrations
     */
    public static List<ToolRegistration> tools(ProposalStore store) {
        return List.of(proposeChange(store), confirmChange(store));
    }

    // ── fabric_propose_change ───────────────────────────────────────────────

    private static ToolRegistration proposeChange(ProposalStore store) {
        return ToolRegistration.builder()
                .name("fabric_propose_change")
                .title("Propose a Fabric change (real dry run, nothing provisioned)")
                .description("Phase 1 of the Safe Mutation Broker's two-phase protocol: agents propose, "
                        + "dry-run results decide, humans confirm. Takes a typed create specification and "
                        + "executes the REAL spec-documented dry run (dryRun=true on the actual Fabric v4 "
                        + "endpoint): the Equinix API validates the exact payload a confirm would send, but "
                        + "NOTHING is provisioned by this call. Returns the validated entity, a rate-card "
                        + "price context where estimable (connection bandwidth; honestly 'unpriced' "
                        + "otherwise), and a single-use confirm_token (valid " + ProposalStore.DEFAULT_TTL.toMinutes()
                        + " minutes, this server process only) bound to the SHA-256 of the exact spec. "
                        + "Present the validation and price context to a human, then call "
                        + "fabric_confirm_change with the token to execute exactly this spec — any change "
                        + "to the spec requires a new proposal. Only creates exist: no update and no "
                        + "delete change types.")
                .inputSchema(objectSchema(propsOf(
                                "change_type", enumSchema("The kind of create to propose.", ChangeType.ids()),
                                "spec", specSchema()),
                        "change_type", "spec"))
                .outputSchema(looseObjectSchema("The dry-run validation (echoed entity), price context, "
                        + "confirm_token, expiry, and the spec's SHA-256 binding."))
                .readOnly(false)
                .destructive(false)
                .idempotent(false)
                .toolset(Toolset.MUTATE)
                .handler((args, ctx) -> handlePropose(store, args, ctx))
                .build();
    }

    private static ObjectNode handlePropose(ProposalStore store, JsonNode args, ServerContext ctx) {
        ChangeType changeType = ChangeType.fromId(requireString(args, "change_type"));
        JsonNode spec = args.get("spec");
        if (spec == null || !spec.isObject() || spec.isEmpty()) {
            throw new IllegalArgumentException("'spec' is required: the typed create specification for "
                    + changeType.id() + " (see the tool description for its fields).");
        }

        String canonical = SpecHash.canonicalize(spec, ctx.objectMapper());
        String sha256 = SpecHash.sha256Hex(canonical);

        // The real, spec-backed dry run: the API validates the exact create payload.
        ObjectNode validation = ChangeCompiler.execute(changeType, spec, ctx, true);

        ObjectNode priceContext = priceContext(changeType, spec, ctx);

        // Token minted only after the dry run passed — invalid specs never earn a token. The price
        // summary is stored with it so the confirm prompt repeats the figure shown here.
        PendingChange minted = store.mint(changeType, canonical, sha256, priceSummary(priceContext));

        ObjectNode payload = ctx.objectMapper().createObjectNode();
        payload.put("phase", "dry_run");
        payload.put("change_type", changeType.id());
        payload.set("validation", validation);
        payload.put("dry_run_note", "Validated by the live Equinix API with dryRun=true — nothing was "
                + "provisioned by this call.");
        payload.set("price_context", priceContext);
        payload.put("confirm_token", minted.token());
        payload.put("expires_in_seconds", store.ttl().toSeconds());
        payload.put("spec_sha256", sha256);
        payload.put("token_scope", "Single-use, held only in this server process's memory, and bound to "
                + "the SHA-256 of this exact spec. Any change to the spec invalidates it — re-propose "
                + "instead.");
        payload.put("next_step", "Have a human review the validation and price context, then call "
                + "fabric_confirm_change with confirm_token to execute exactly this spec. When the client "
                + "supports MCP elicitation, fabric_confirm_change prompts the user itself and executes "
                + "only on an explicit accept.");
        return payload;
    }

    /** One line of the price context for the confirm prompt: the figures when priced, else the note. */
    private static String priceSummary(ObjectNode priceContext) {
        if (!priceContext.path("priced").asBoolean(false)) {
            return priceContext.path("note").asText("unpriced");
        }
        String currency = priceContext.path("currency").asText("USD");
        return currency + " " + priceContext.path("monthly_recurring").asText() + " monthly recurring, "
                + currency + " " + priceContext.path("non_recurring").asText() + " non-recurring ("
                + priceContext.path("price_source").asText() + "). " + priceContext.path("basis").asText("");
    }

    /**
     * A rate-card estimate where one is honestly derivable — a connection's bandwidth prices
     * against the layered live-Equinix-then-reference chain — and an explicit
     * {@code priced: false} everywhere else. A pricing failure never fails the proposal.
     */
    private static ObjectNode priceContext(ChangeType changeType, JsonNode spec, ServerContext ctx) {
        ObjectNode node = ctx.objectMapper().createObjectNode();
        if (changeType != ChangeType.CONNECTION_CREATE) {
            node.put("priced", false);
            node.put("note", "unpriced: no rate card models " + changeType.id()
                    + " (the rate-card machinery prices connections and cloud routers).");
            return node;
        }
        try {
            ConnectionType type = ChangeCompiler.connectionType(spec);
            int bandwidth = spec.path("bandwidth_mbps").asInt();
            Optional<PriceQuote> quote = RateCard.standardChain(ctx.fabric())
                    .connection(type, bandwidth, null, Term.MONTH_1);
            if (quote.isEmpty()) {
                node.put("priced", false);
                node.put("note", "unpriced: neither live Equinix pricing nor the bundled reference "
                        + "figures could price a " + bandwidth + " Mbps " + type + " connection.");
                return node;
            }
            PriceQuote price = quote.get();
            node.put("priced", true);
            node.put("monthly_recurring", price.getMonthlyRecurring());
            node.put("non_recurring", price.getNonRecurring());
            node.put("currency", price.getCurrency() == null ? "USD" : price.getCurrency().getCurrencyCode());
            node.put("price_source", String.valueOf(price.getSource()));
            if (price.getNote() != null) {
                node.put("note", price.getNote());
            }
            node.put("basis", "Rate-card estimate for a " + bandwidth + " Mbps " + type
                    + " connection (metro-agnostic, month-to-month) — an indication for human review, "
                    + "not a quote.");
        }
        catch (RuntimeException e) {
            node.removeAll();
            node.put("priced", false);
            node.put("note", "unpriced: the price lookup failed (" + e.getMessage()
                    + "). The proposal itself is unaffected.");
        }
        return node;
    }

    // ── fabric_confirm_change ───────────────────────────────────────────────

    private static ToolRegistration confirmChange(ProposalStore store) {
        return ToolRegistration.builder()
                .name("fabric_confirm_change")
                .title("Confirm and execute a proposed Fabric change")
                .description("Phase 2 of the Safe Mutation Broker: executes a change previously validated "
                        + "by fabric_propose_change, identified ONLY by its confirm_token. The stored spec "
                        + "— never a re-supplied one — is re-verified against its SHA-256 binding and then "
                        + "executed for real via the same Fabric v4 create endpoint WITHOUT dryRun. Call "
                        + "this only after a human has reviewed and approved the proposal. HUMAN "
                        + "CONFIRMATION: when the connected client declared MCP form elicitation, this "
                        + "tool prompts the user with the change type, target, price context, proposal "
                        + "age and spec SHA-256 BEFORE executing, and executes ONLY when the user accepts "
                        + "with confirm=true. Decline, cancel, no answer within the elicitation timeout, "
                        + "or a failed prompt execute NOTHING and return executed=false with "
                        + "human_confirmation.status = declined | cancelled | timed_out | failed. When the "
                        + "client did not declare elicitation, no prompt can be sent: the change executes "
                        + "as before and the result reports human_confirmation.status = "
                        + "unsupported_by_client, meaning the server did not verify a human approval. "
                        + "Every result carries confirm_checks (proposal_exists, not_expired, "
                        + "not_previously_used, spec_matches_binding). Tokens are "
                        + "single-use (consumed on the first attempt, success or not — a declined or "
                        + "unanswered prompt also consumes the token), expire after "
                        + ProposalStore.DEFAULT_TTL.toMinutes() + " minutes, and exist only in this server "
                        + "process; on any token error or non-accepted prompt, re-propose with "
                        + "fabric_propose_change. The confirm_token is a process-local lookup key, not a "
                        + "cloud-provider activation key; never enter it in a cloud console.")
                .inputSchema(objectSchema(propsOf(
                                "confirm_token", stringSchema("The single-use token returned by "
                                        + "fabric_propose_change.")),
                        "confirm_token"))
                .outputSchema(looseObjectSchema("phase ('executed' or 'not_executed'), executed, the created "
                        + "entity (uuid, name, state) when executed, the spec's SHA-256, "
                        + "human_confirmation {status, detail}, confirm_checks, and token_state."))
                .readOnly(false)
                .destructive(false)
                .idempotent(false)
                .toolset(Toolset.MUTATE)
                .handler((args, ctx) -> handleConfirm(store, args, ctx))
                .build();
    }

    private static ObjectNode handleConfirm(ProposalStore store, JsonNode args, ServerContext ctx)
            throws Exception {
        String token = requireString(args, "confirm_token");
        ProposalStore.Consumption consumption = store.consume(token);
        long ttlMinutes = store.ttl().toMinutes();
        switch (consumption.outcome()) {
            case UNKNOWN -> throw new IllegalArgumentException("confirm_token '" + token + "' is unknown "
                    + "in this server process. Tokens are minted by fabric_propose_change, live only in "
                    + "this process's memory for " + ttlMinutes + " minutes, and are single-use. Call "
                    + "fabric_propose_change again and confirm with the fresh token. "
                    + ConfirmChecks.unknown().describe());
            case EXPIRED -> throw new IllegalArgumentException("confirm_token '" + token + "' has expired: "
                    + "proposals are confirmable for " + ttlMinutes + " minutes. Nothing was executed. "
                    + "Call fabric_propose_change again to mint a fresh proposal. "
                    + ConfirmChecks.expired().describe());
            case REPLAYED -> throw new IllegalArgumentException("confirm_token '" + token + "' was already "
                    + "used: tokens are consumed on the first confirm attempt, whether or not it "
                    + "succeeded. Nothing was executed by this call. If the change is still wanted, call "
                    + "fabric_propose_change again. " + ConfirmChecks.replayed().describe());
            case CONSUMED -> {
                // Fall through to the integrity check below.
            }
        }
        PendingChange change = consumption.change();

        // Defense in depth: the stored spec must still match the SHA-256 the token was bound to.
        String recomputed = SpecHash.sha256Hex(change.canonicalSpec());
        if (!recomputed.equals(change.specSha256())) {
            throw new IllegalStateException("Proposal integrity check failed: the stored spec no longer "
                    + "matches the SHA-256 its confirm token was bound to. Nothing was executed. Call "
                    + "fabric_propose_change again. " + ConfirmChecks.bindingMismatch().describe());
        }
        ConfirmChecks checks = ConfirmChecks.allPassed();

        JsonNode spec = ctx.objectMapper().readTree(change.canonicalSpec());

        // Human confirmation. The token is already consumed, so every outcome below leaves it
        // consumed: the single-use rule is "consumed on the first attempt", and a prompt that was
        // declined, dismissed or never answered is an attempt.
        HumanConfirmation confirmation = ctx.confirmWithHuman(
                confirmationPrompt(change, spec, store.ageOf(change), store.ttl()));

        ObjectNode payload = ctx.objectMapper().createObjectNode();
        if (!confirmation.accepted() && !confirmation.unsupportedByClient()) {
            payload.put("phase", "not_executed");
            payload.put("executed", false);
            payload.put("change_type", change.changeType().id());
            payload.put("spec_sha256", change.specSha256());
            payload.set("human_confirmation", humanConfirmationNode(ctx, confirmation));
            payload.set("confirm_checks", checks.toNode(ctx.objectMapper()));
            payload.put("token_state", "consumed");
            payload.put("note", "Nothing was sent to the Equinix API: the create runs only after the user "
                    + "accepts the confirmation prompt. The confirm token is single-use and was consumed by "
                    + "this attempt.");
            payload.put("next_step", "If the change is still wanted, call fabric_propose_change again and "
                    + "confirm with the fresh token.");
            return payload;
        }

        // The real create — same compiler, same endpoint, no dryRun parameter.
        ObjectNode created = ChangeCompiler.execute(change.changeType(), spec, ctx, false);

        payload.put("phase", "executed");
        payload.put("executed", true);
        payload.put("change_type", change.changeType().id());
        payload.set("result", created);
        payload.put("spec_sha256", change.specSha256());
        payload.set("human_confirmation", humanConfirmationNode(ctx, confirmation));
        payload.set("confirm_checks", checks.toNode(ctx.objectMapper()));
        payload.put("token_state", "consumed");
        payload.put("note", "Executed the exact proposed spec via the real create endpoint (no dryRun "
                + "parameter). The confirm token is now consumed.");
        return payload;
    }

    private static ObjectNode humanConfirmationNode(ServerContext ctx, HumanConfirmation confirmation) {
        ObjectNode node = ctx.objectMapper().createObjectNode();
        node.put("status", confirmation.status().id());
        node.put("detail", confirmation.unsupportedByClient()
                ? confirmation.detail() + ". The change executed without a server-side check that a "
                + "human approved it; approval rests on the calling agent having shown the proposal to "
                + "the user."
                : confirmation.detail());
        return node;
    }

    /**
     * The text shown to the approver: change type, target, the price context the proposal reported,
     * the proposal's age against its TTL, the spec binding, and what each answer does.
     */
    static String confirmationPrompt(PendingChange change, JsonNode spec, Duration age, Duration ttl) {
        return "Approve execution of a Fabric change proposed through fabric_propose_change.\n"
                + "Change type: " + change.changeType().id() + "\n"
                + "Target: " + describeTarget(change.changeType(), spec) + "\n"
                + "Price context: " + (change.priceSummary() == null
                ? "none was recorded with the proposal" : change.priceSummary()) + "\n"
                + "Proposal age: " + age.toSeconds() + " s (proposals expire " + ttl.toSeconds()
                + " s after the dry run)\n"
                + "Spec SHA-256: " + change.specSha256() + "\n"
                + "Accepting with confirm=true sends the real create request to the Equinix Fabric API. "
                + "Declining, dismissing or not answering sends nothing. The confirm token is single-use "
                + "and is already consumed, so a new fabric_propose_change is needed after any answer "
                + "other than accept.";
    }

    /** A one-line description of what the stored spec creates, read from the spec's own fields. */
    private static String describeTarget(ChangeType changeType, JsonNode spec) {
        String name = "'" + spec.path("name").asText("(unnamed)") + "'";
        return switch (changeType) {
            case CONNECTION_CREATE -> spec.path("type").asText("?") + " connection " + name + ", "
                    + spec.path("bandwidth_mbps").asText("?") + " Mbps, A-side " + describeSide(spec.path("a_side"))
                    + ", Z-side " + describeSide(spec.path("z_side"));
            case NETWORK_CREATE -> spec.path("type").asText("?") + " network " + name + ", scope "
                    + spec.path("scope").asText("?")
                    + (spec.hasNonNull("metro_code") ? ", metro " + spec.path("metro_code").asText() : "");
            case SERVICE_TOKEN_CREATE -> spec.path("type").asText("?") + " service token " + name
                    + ", issuer side " + spec.path("issuer_side").asText("?") + ", access point "
                    + describeSide(spec.path("access_point"));
        };
    }

    private static final List<String> ENDPOINT_KEYS = List.of("port_uuid", "service_profile_uuid",
            "cloud_router_uuid", "network_uuid", "service_token_uuid", "virtual_device_uuid");

    /** The endpoint identifier of a connection side or token access point, as {@code key=value}. */
    private static String describeSide(JsonNode side) {
        for (String key : ENDPOINT_KEYS) {
            if (side.hasNonNull(key)) {
                return key + "=" + side.path(key).asText();
            }
        }
        return "(no endpoint identifier)";
    }

    /**
     * The four redemption predicates of a confirm attempt, in evaluation order. {@code null} means
     * the predicate was not evaluated because an earlier one failed. They correspond to the checks
     * the Connection Coordinator specification requires of {@code ConfirmActivationKey} (the
     * resource still exists, the key was not already used, the request matches the recorded
     * intent), applied here to a process-local confirm token.
     *
     * @param proposalExists the token was minted by this server process and is still remembered
     * @param notExpired the proposal's TTL had not elapsed
     * @param notPreviouslyUsed no earlier confirm attempt consumed the token
     * @param specMatchesBinding the stored spec still hashes to the SHA-256 the token was bound to
     */
    record ConfirmChecks(Boolean proposalExists, Boolean notExpired, Boolean notPreviouslyUsed,
                         Boolean specMatchesBinding) {

        static ConfirmChecks unknown() {
            return new ConfirmChecks(false, null, null, null);
        }

        static ConfirmChecks expired() {
            return new ConfirmChecks(true, false, true, null);
        }

        static ConfirmChecks replayed() {
            return new ConfirmChecks(true, null, false, null);
        }

        static ConfirmChecks bindingMismatch() {
            return new ConfirmChecks(true, true, true, false);
        }

        static ConfirmChecks allPassed() {
            return new ConfirmChecks(true, true, true, true);
        }

        ObjectNode toNode(ObjectMapper mapper) {
            ObjectNode node = mapper.createObjectNode();
            node.put("proposal_exists", proposalExists);
            node.put("not_expired", notExpired);
            node.put("not_previously_used", notPreviouslyUsed);
            node.put("spec_matches_binding", specMatchesBinding);
            return node;
        }

        String describe() {
            return "confirm_checks: proposal_exists=" + text(proposalExists) + ", not_expired="
                    + text(notExpired) + ", not_previously_used=" + text(notPreviouslyUsed)
                    + ", spec_matches_binding=" + text(specMatchesBinding) + ".";
        }

        private static String text(Boolean value) {
            return value == null ? "not_evaluated" : value.toString();
        }
    }

    // ── local schema/argument helpers ───────────────────────────────────────
    // (The core package's Schemas/Args are package-private by design; the broker keeps its own
    // minimal equivalents rather than widening that surface.)

    private static Map<String, Object> specSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("description", "The typed create specification; unknown fields are rejected. "
                + "For connection_create: {type (e.g. 'EVPL_VC'), name, bandwidth_mbps, "
                + "notification_emails: [..], a_side: {..}, z_side: {..}, purchase_order_number?, "
                + "project_id?} where each side gives exactly one of port_uuid / service_profile_uuid / "
                + "cloud_router_uuid / network_uuid / service_token_uuid / virtual_device_uuid, plus "
                + "link_protocol {type: dot1q|qinq|untagged, vlan_tag / vlan_c_tag+vlan_s_tag} for "
                + "port, service-profile, and virtual-device endpoints (interface_id? for virtual "
                + "devices). For network_create: {type: EVPLAN|EPLAN|IPWAN|EVPTREE|EPTREE, name, "
                + "scope: LOCAL|REGIONAL|GLOBAL, notification_emails: [..], project_id?, metro_code?}. "
                + "For service_token_create: {issuer_side: a_side|z_side, type: VC_TOKEN|EPL_TOKEN, "
                + "name, access_point: {port_uuid|virtual_device_uuid|network_uuid, interface_id?, "
                + "link_protocol? (dot1q|qinq)}, description?, expiry_days?, connection_type?, "
                + "allow_remote_connection?, allow_custom_bandwidth?, bandwidth_limit_mbps?, "
                + "supported_bandwidths_mbps?, project_id?, notification_emails?}.");
        schema.put("additionalProperties", true);
        return schema;
    }

    private static Map<String, Object> objectSchema(Map<String, Object> properties, String... required) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        if (required.length > 0) {
            schema.put("required", new ArrayList<>(Arrays.asList(required)));
        }
        schema.put("additionalProperties", false);
        return schema;
    }

    private static Map<String, Object> propsOf(Object... namesAndSchemas) {
        Map<String, Object> properties = new LinkedHashMap<>();
        for (int i = 0; i < namesAndSchemas.length; i += 2) {
            properties.put((String) namesAndSchemas[i], namesAndSchemas[i + 1]);
        }
        return properties;
    }

    private static Map<String, Object> stringSchema(String description) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "string");
        schema.put("description", description);
        return schema;
    }

    private static Map<String, Object> enumSchema(String description, String... values) {
        Map<String, Object> schema = stringSchema(description);
        schema.put("enum", new ArrayList<>(Arrays.asList(values)));
        return schema;
    }

    private static Map<String, Object> looseObjectSchema(String description) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("description", description);
        schema.put("additionalProperties", true);
        return schema;
    }

    private static String requireString(JsonNode args, String field) {
        JsonNode node = args.get(field);
        if (node == null || node.isNull() || !node.isTextual() || node.asText().trim().isEmpty()) {
            throw new IllegalArgumentException("'" + field + "' is required and must be a non-empty string.");
        }
        return node.asText().trim();
    }
}
