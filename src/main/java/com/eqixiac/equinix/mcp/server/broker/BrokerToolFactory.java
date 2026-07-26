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
import com.eqixiac.equinix.mcp.server.ServerContext;
import com.eqixiac.equinix.mcp.server.ToolRegistration;
import com.eqixiac.equinix.mcp.server.Toolset;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

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

        // Token minted only after the dry run passed — invalid specs never earn a token.
        PendingChange minted = store.mint(changeType, canonical, sha256);

        ObjectNode payload = ctx.objectMapper().createObjectNode();
        payload.put("phase", "dry_run");
        payload.put("change_type", changeType.id());
        payload.set("validation", validation);
        payload.put("dry_run_note", "Validated by the live Equinix API with dryRun=true — nothing was "
                + "provisioned by this call.");
        payload.set("price_context", priceContext(changeType, spec, ctx));
        payload.put("confirm_token", minted.token());
        payload.put("expires_in_seconds", store.ttl().toSeconds());
        payload.put("spec_sha256", sha256);
        payload.put("token_scope", "Single-use, held only in this server process's memory, and bound to "
                + "the SHA-256 of this exact spec. Any change to the spec invalidates it — re-propose "
                + "instead.");
        payload.put("next_step", "Have a human review the validation and price context, then call "
                + "fabric_confirm_change with confirm_token to execute exactly this spec.");
        return payload;
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
                        + "this only after a human has reviewed and approved the proposal. Tokens are "
                        + "single-use (consumed on the first attempt, success or not), expire after "
                        + ProposalStore.DEFAULT_TTL.toMinutes() + " minutes, and exist only in this server "
                        + "process; on any token error, re-propose with fabric_propose_change.")
                .inputSchema(objectSchema(propsOf(
                                "confirm_token", stringSchema("The single-use token returned by "
                                        + "fabric_propose_change.")),
                        "confirm_token"))
                .outputSchema(looseObjectSchema("The created entity (uuid, name, state) and the executed "
                        + "spec's SHA-256."))
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
                    + "fabric_propose_change again and confirm with the fresh token.");
            case EXPIRED -> throw new IllegalArgumentException("confirm_token '" + token + "' has expired: "
                    + "proposals are confirmable for " + ttlMinutes + " minutes. Nothing was executed. "
                    + "Call fabric_propose_change again to mint a fresh proposal.");
            case REPLAYED -> throw new IllegalArgumentException("confirm_token '" + token + "' was already "
                    + "used: tokens are consumed on the first confirm attempt, whether or not it "
                    + "succeeded. Nothing was executed by this call. If the change is still wanted, call "
                    + "fabric_propose_change again.");
            case CONSUMED -> {
                // Fall through to execution below.
            }
        }
        PendingChange change = consumption.change();

        // Defense in depth: the stored spec must still match the SHA-256 the token was bound to.
        String recomputed = SpecHash.sha256Hex(change.canonicalSpec());
        if (!recomputed.equals(change.specSha256())) {
            throw new IllegalStateException("Proposal integrity check failed: the stored spec no longer "
                    + "matches the SHA-256 its confirm token was bound to. Nothing was executed. Call "
                    + "fabric_propose_change again.");
        }

        JsonNode spec = ctx.objectMapper().readTree(change.canonicalSpec());
        // The real create — same compiler, same endpoint, no dryRun parameter.
        ObjectNode created = ChangeCompiler.execute(change.changeType(), spec, ctx, false);

        ObjectNode payload = ctx.objectMapper().createObjectNode();
        payload.put("phase", "executed");
        payload.put("change_type", change.changeType().id());
        payload.set("result", created);
        payload.put("spec_sha256", change.specSha256());
        payload.put("note", "Executed the exact proposed spec via the real create endpoint (no dryRun "
                + "parameter). The confirm token is now consumed.");
        return payload;
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
