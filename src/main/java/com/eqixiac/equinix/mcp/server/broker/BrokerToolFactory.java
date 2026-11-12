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
import com.eqixiac.equinix.design.value.ratecard.Term;
import com.eqixiac.equinix.fabric.enums.ConnectionType;
import com.eqixiac.equinix.mcp.server.CancellationSignal;
import com.eqixiac.equinix.mcp.server.HumanConfirmation;
import com.eqixiac.equinix.mcp.server.ServerContext;
import com.eqixiac.equinix.mcp.server.ToolHandler;
import com.eqixiac.equinix.mcp.server.ToolRegistration;
import com.eqixiac.equinix.mcp.server.Toolset;
import com.eqixiac.equinix.mcp.server.UntrustedText;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The Safe Mutation Broker's two tools - {@code fabric_propose_change} and
 * {@code fabric_confirm_change} - registered through the exact same {@link ToolRegistration}
 * seam as the read-only catalog, in the opt-in {@link Toolset#MUTATE} toolset.
 *
 * <p>The two tools returned by one {@code tools(...)} call share one {@link ProposalStore},
 * so a token minted by that propose tool is confirmable only by its paired confirm tool - 
 * i.e. within one server instance. The store-taking overload is the test seam (deterministic
 * clocks, pre-seeded proposals).</p>
 *
 * <h2>Human confirmation at confirm time</h2>
 * <p>{@code fabric_confirm_change} asks the MCP client to show the change to a person before it
 * executes ({@link ServerContext#confirmWithHuman(String)}). The order is fixed: consume the token,
 * recompile the stored spec and compare the compiled request's SHA-256 with the one the token is
 * bound to, prompt, re-check the TTL, execute. The prompt states the change type, the target, the
 * price context the proposal reported, the proposal's age, the request SHA-256 and every field.</p>
 *
 * <h2>What the token is bound to</h2>
 * <p>The propose phase compiles the spec into the exact create request ({@code ChangeCompiler}) and
 * binds the token to the SHA-256 of that request: the change type and the canonical JSON body the
 * HTTP layer serializes. The confirm phase compiles the stored spec again and executes only when the
 * new request hashes to the bound value, so any drift between what the dry run validated and what
 * would be sent (a different SDK mapping, a time-relative field, a tampered store) stops the
 * create.</p>
 *
 * <h2>Cancellation</h2>
 * <p>A client {@code notifications/cancelled} stops {@code fabric_confirm_change} at any point before
 * the create request is sent: before the token is consumed (the token stays usable), during the
 * human prompt (the prompt is abandoned) or after it. Once the create has been sent the call
 * finishes and its result says it executed, with {@code cancellation.requested_after_send = true}
 * when a cancel arrived too late.</p>
 * <table>
 *   <caption>Confirm outcomes by human-confirmation status</caption>
 *   <tr><th>{@code human_confirmation.status}</th><th>Executed</th><th>Token afterwards</th></tr>
 *   <tr><td>{@code accepted}</td><td>yes</td><td>consumed</td></tr>
 *   <tr><td>{@code declined}, {@code cancelled}, {@code timed_out}, {@code failed}</td><td>no</td>
 *       <td>consumed; a new proposal is required</td></tr>
 *   <tr><td>{@code unsupported_by_client}</td><td>no, unless
 *       {@code EQUINIX_MCP_ALLOW_UNCONFIRMED_MUTATIONS=true}</td><td>consumed</td></tr>
 * </table>
 * <p>{@code unsupported_by_client} fails closed by default: a client that did not declare form
 * elicitation cannot be prompted, so the server cannot verify a human approval and refuses to
 * execute. An operator whose host shows its own per-tool approval can opt in with
 * {@code EQUINIX_MCP_ALLOW_UNCONFIRMED_MUTATIONS=true}; the result then reports that no
 * server-side check ran. The prompt is the
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
 * to mutate - but the propose phase itself provisions <em>nothing</em> (its API call is the
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
     * Builds the broker tool pair over the given store - the seam tests use for deterministic
     * clocks and pre-seeded proposals.
     *
     * @param store the proposal store both tools share
     * @return the propose/confirm registrations
     */
    public static List<ToolRegistration> tools(ProposalStore store) {
        return List.of(proposeChange(store), confirmChange(store));
    }

    // fabric_propose_change

    private static ToolRegistration proposeChange(ProposalStore store) {
        return ToolRegistration.builder()
                .name("fabric_propose_change")
                .title("Propose a billable Fabric create (dry run only; nothing provisioned)")
                .description("Phase 1 of the Safe Mutation Broker's two-phase protocol: agents propose, "
                        + "dry-run results decide, humans confirm. Takes a typed create specification and "
                        + "executes the REAL spec-documented dry run (dryRun=true on the actual Fabric v4 "
                        + "endpoint): the Equinix API validates the exact payload a confirm would send, but "
                        + "NOTHING is provisioned by this call. Returns the validated entity, a rate-card "
                        + "price context where estimable (connection bandwidth; honestly 'unpriced' "
                        + "otherwise), the exact compiled create request, and a single-use confirm_token "
                        + "(valid " + ProposalStore.DEFAULT_TTL.toMinutes() + " minutes, this server process "
                        + "only) bound to the SHA-256 of that compiled request. The change it proposes "
                        + "creates BILLABLE Equinix resources when confirmed. Present the validation and "
                        + "price context to a human, then call fabric_confirm_change with the token to "
                        + "execute exactly this request - any change to the spec requires a new proposal. "
                        + "At most " + ProposalStore.DEFAULT_CAPACITY + " proposals are pending at once: "
                        + "proposing beyond that evicts the oldest, reported under evicted_proposals. Only "
                        + "creates exist: no update and no delete change types.")
                .inputSchema(objectSchema(propsOf(
                                "change_type", enumSchema("The kind of create to propose.", ChangeType.ids()),
                                "spec", specSchema()),
                        "change_type", "spec"))
                .outputSchema(looseObjectSchema("The dry-run validation (echoed entity), price context, "
                        + "compiled_request, confirm_token, expiry, request_sha256 binding, spec_sha256, and "
                        + "evicted_proposals when capacity forced an eviction."))
                .readOnly(false)
                .destructive(false)
                .idempotent(false)
                .openWorld(true)
                .toolset(Toolset.MUTATE)
                .handler((ToolHandler.Cancellable) (args, ctx, cancel) -> handlePropose(store, args, ctx, cancel))
                .build();
    }

    private static ObjectNode handlePropose(ProposalStore store, JsonNode args, ServerContext ctx,
                                            CancellationSignal cancel) {
        ChangeType changeType = ChangeType.fromId(requireString(args, "change_type"));
        JsonNode spec = args.get("spec");
        if (spec == null || !spec.isObject() || spec.isEmpty()) {
            throw new IllegalArgumentException("'spec' is required: the typed create specification for "
                    + changeType.id() + " (see the tool description for its fields).");
        }

        String canonical = SpecHash.canonicalize(spec, ctx.objectMapper());
        String sha256 = SpecHash.sha256Hex(canonical);

        // Compile once: the token is bound to the hash of exactly this request, and the dry run sends
        // exactly this request.
        Instant compiledAt = store.now();
        ChangeCompiler.Compiled compiled = ChangeCompiler.compile(changeType, spec, ctx, compiledAt);

        // The real, spec-backed dry run: the API validates the exact create payload.
        cancel.throwIfCancelled("before the dry run was sent");
        ObjectNode validation = compiled.dryRun();

        ObjectNode priceContext = priceContext(changeType, spec, ctx);

        // Token minted only after the dry run passed - invalid specs never earn a token. The price
        // summary is stored with it so the confirm prompt repeats the figure shown here.
        ProposalStore.Minted minted = store.mint(changeType, canonical, sha256, compiled.requestSha256(),
                compiledAt, priceSummary(priceContext));

        ObjectNode payload = ctx.objectMapper().createObjectNode();
        payload.put("phase", "dry_run");
        payload.put("change_type", changeType.id());
        payload.set("validation", validation);
        payload.put("dry_run_note", "Validated by the live Equinix API with dryRun=true - nothing was "
                + "provisioned by this call.");
        payload.set("price_context", priceContext);
        payload.set("compiled_request", compiled.requestBody());
        payload.put("confirm_token", minted.change().token());
        payload.put("expires_in_seconds", store.ttl().toSeconds());
        payload.put("request_sha256", compiled.requestSha256());
        payload.put("spec_sha256", sha256);
        payload.put("token_scope", "Single-use, held only in this server process's memory, and bound to "
                + "the SHA-256 of the compiled request above. The confirm phase recompiles the stored spec "
                + "and executes only if it compiles to the same request. Any change to the spec needs a "
                + "new proposal.");
        if (!minted.evicted().isEmpty()) {
            ArrayNode evicted = payload.putArray("evicted_proposals");
            for (PendingChange old : minted.evicted()) {
                ObjectNode entry = evicted.addObject();
                entry.put("confirm_token", old.token());
                entry.put("change_type", old.changeType().id());
                entry.put("minted_at", old.mintedAt().toString());
            }
            payload.put("eviction_note", "At most " + store.capacity() + " proposals can be pending. "
                    + minted.evicted().size() + " older proposal(s) listed under evicted_proposals were "
                    + "evicted to make room and can no longer be confirmed; re-propose them if they are "
                    + "still wanted.");
        }
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
     * A rate-card estimate where one is honestly derivable - a connection's bandwidth prices
     * against the layered live-Equinix-then-reference chain - and an explicit
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
            // Live Equinix pricing under the call's pricing budget, falling back to reference figures.
            Optional<PriceQuote> quote = ctx.budgetedEquinixRateCard()
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
                UntrustedText.put(node, "note", price.getNote());
            }
            node.put("basis", "Rate-card estimate for a " + bandwidth + " Mbps " + type
                    + " connection (metro-agnostic, month-to-month) - an indication for human review, "
                    + "not a quote.");
        }
        catch (RuntimeException e) {
            node.removeAll();
            node.put("priced", false);
            UntrustedText.put(node, "note", "unpriced: the price lookup failed (" + e.getMessage()
                    + "). The proposal itself is unaffected.");
        }
        return node;
    }

    // fabric_confirm_change

    private static ToolRegistration confirmChange(ProposalStore store) {
        return ToolRegistration.builder()
                .name("fabric_confirm_change")
                .title("Create billable Fabric resources (executes a proposed change)")
                .description("Phase 2 of the Safe Mutation Broker: executes a change previously validated "
                        + "by fabric_propose_change, identified ONLY by its confirm_token. EXECUTING CREATES "
                        + "BILLABLE EQUINIX RESOURCES (a connection, network or service token) on the "
                        + "operator's account. The stored spec - never a re-supplied one - is recompiled, "
                        + "and the create runs only when the compiled request still hashes to the "
                        + "request_sha256 the token is bound to; it is then sent to the same Fabric v4 "
                        + "create endpoint WITHOUT dryRun. Call this only after a human has reviewed and "
                        + "approved the proposal. HUMAN CONFIRMATION: when the connected client declared "
                        + "MCP form elicitation, this tool prompts the user with the change type, target, "
                        + "price context, proposal age, request SHA-256 and every field BEFORE executing, "
                        + "and executes ONLY when the user accepts with confirm=true and the proposal is "
                        + "still within its TTL when the answer arrives. Decline, cancel, no answer within "
                        + "the elicitation timeout, a failed prompt, or expiry while the prompt was open "
                        + "execute NOTHING and return executed=false with human_confirmation.status = "
                        + "declined | cancelled | timed_out | failed | call_cancelled. When the client did "
                        + "not declare elicitation, no prompt can be sent and nothing executes "
                        + "(human_confirmation.status = unsupported_by_client) unless the operator set "
                        + "EQUINIX_MCP_ALLOW_UNCONFIRMED_MUTATIONS=true. Cancelling this tool call "
                        + "(notifications/cancelled) before the create is sent stops it; once sent, the "
                        + "result says it executed. "
                        + "Every result carries confirm_checks (proposal_exists, not_evicted, not_expired, "
                        + "not_previously_used, request_matches_binding). Tokens are "
                        + "single-use (consumed on the first attempt, success or not - a declined or "
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
                        + "entity (uuid, name, state) when executed, request_sha256, spec_sha256, "
                        + "human_confirmation {status, detail}, confirm_checks, token_state, and "
                        + "cancellation when the client cancelled the call."))
                .readOnly(false)
                .destructive(false)
                .idempotent(false)
                .openWorld(true)
                .toolset(Toolset.MUTATE)
                .handler((ToolHandler.Cancellable) (args, ctx, cancel) -> handleConfirm(store, args, ctx, cancel))
                .build();
    }

    private static ObjectNode handleConfirm(ProposalStore store, JsonNode args, ServerContext ctx,
                                            CancellationSignal cancel) throws Exception {
        String token = requireString(args, "confirm_token");
        // A call cancelled before it starts leaves the token unused.
        cancel.throwIfCancelled("before the confirm token was used (the token is still valid)");
        ProposalStore.Consumption consumption = store.consume(token);
        long ttlMinutes = store.ttl().toMinutes();
        switch (consumption.outcome()) {
            case UNKNOWN -> throw new IllegalArgumentException("confirm_token '" + token + "' is unknown "
                    + "in this server process. Tokens are minted by fabric_propose_change, live only in "
                    + "this process's memory for " + ttlMinutes + " minutes, and are single-use. Call "
                    + "fabric_propose_change again and confirm with the fresh token. "
                    + ConfirmChecks.unknown().describe());
            case EVICTED -> throw new IllegalArgumentException("confirm_token '" + token + "' was evicted: "
                    + "at most " + store.capacity() + " proposals can be pending, and this one was the "
                    + "oldest when a newer proposal was made (that propose call reported it under "
                    + "evicted_proposals). Nothing was executed. Call fabric_propose_change again. "
                    + ConfirmChecks.evicted().describe());
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

        // Integrity: recompile the stored spec at the proposal's reference instant and execute only
        // if it produces exactly the request the token is bound to (and the dry run validated).
        JsonNode spec = ctx.objectMapper().readTree(change.canonicalSpec());
        ChangeCompiler.Compiled compiled;
        try {
            compiled = ChangeCompiler.compile(change.changeType(), spec, ctx, change.compiledAt());
        }
        catch (IllegalArgumentException e) {
            throw new IllegalStateException("Proposal integrity check failed: the stored spec no longer "
                    + "compiles (" + e.getMessage() + "). Nothing was executed. Call fabric_propose_change "
                    + "again. " + ConfirmChecks.bindingMismatch().describe(), e);
        }
        if (!compiled.requestSha256().equals(change.requestSha256())
                || !SpecHash.sha256Hex(change.canonicalSpec()).equals(change.specSha256())) {
            throw new IllegalStateException("Proposal integrity check failed: the stored spec now compiles "
                    + "to a request whose SHA-256 (" + compiled.requestSha256() + ") differs from the one "
                    + "the confirm token was bound to (" + change.requestSha256() + "), so what would be "
                    + "sent is not what the dry run validated. Nothing was executed. Call "
                    + "fabric_propose_change again. " + ConfirmChecks.bindingMismatch().describe());
        }
        ConfirmChecks checks = ConfirmChecks.allPassed();

        // Human confirmation. The token is already consumed, so every outcome below leaves it
        // consumed: the single-use rule is "consumed on the first attempt", and a prompt that was
        // declined, dismissed or never answered is an attempt. A cancel during the prompt abandons it.
        HumanConfirmation confirmation = cancel.isCancelled()
                ? new HumanConfirmation(HumanConfirmation.Status.CALL_CANCELLED,
                "the client cancelled the tool call before the confirmation prompt was sent")
                : ctx.confirmWithHuman(confirmationPrompt(change, spec, compiled.requestBody(),
                        store.ageOf(change), store.ttl()));

        // Fail closed: execute only on an explicit human accept. A client without elicitation gets
        // no prompt, so it executes only when the operator opted in with
        // EQUINIX_MCP_ALLOW_UNCONFIRMED_MUTATIONS=true.
        boolean humanApproved = confirmation.accepted()
                || (confirmation.unsupportedByClient() && ctx.allowUnconfirmedMutations());
        if (!humanApproved) {
            String note;
            if (confirmation.unsupportedByClient()) {
                note = "Nothing was sent to the Equinix API: this MCP client did not declare elicitation "
                        + "support, so no confirmation prompt could be shown to a human, and the server refuses "
                        + "to execute unconfirmed changes. Use a client that supports elicitation, or have the "
                        + "operator set " + ServerContext.ENV_ALLOW_UNCONFIRMED_MUTATIONS + "=true to rely on "
                        + "the host's tool-approval prompt instead. The confirm token was consumed.";
            }
            else if (confirmation.status() == HumanConfirmation.Status.CALL_CANCELLED) {
                note = "Nothing was sent to the Equinix API: the client cancelled this tool call before the "
                        + "create was sent. The confirm token was consumed.";
            }
            else {
                note = "Nothing was sent to the Equinix API: the create runs only after the user "
                        + "accepts the confirmation prompt. The confirm token is single-use and was consumed by "
                        + "this attempt.";
            }
            return notExecuted(ctx, change, confirmation, checks, note, cancel);
        }

        // The answer may arrive after the proposal's TTL: the human approved a proposal that is no
        // longer current, so nothing executes.
        if (store.isExpired(change)) {
            ConfirmChecks expiredDuringPrompt = new ConfirmChecks(true, true, false, true, true);
            // (proposal_exists, not_evicted, not_expired=false, not_previously_used, request_matches_binding)
            return notExecuted(ctx, change, confirmation, expiredDuringPrompt, "Nothing was sent to the "
                    + "Equinix API: the proposal's " + store.ttl().toMinutes() + "-minute TTL elapsed "
                    + "before the confirmation answer arrived, so the approval is for an expired proposal. "
                    + "The confirm token was consumed.", cancel);
        }

        // Point of no return. commit() fails if the client cancelled first; after it, a cancel is
        // only recorded and the result reports the create that was sent.
        if (!cancel.commit()) {
            return notExecuted(ctx, change, new HumanConfirmation(HumanConfirmation.Status.CALL_CANCELLED,
                    "the client cancelled the tool call after the approval and before the create was sent"),
                    checks, "Nothing was sent to the Equinix API: the client cancelled this tool call before "
                    + "the create was sent. The confirm token was consumed.", cancel);
        }

        // The real create - the request just verified against the binding, no dryRun parameter.
        ObjectNode created;
        try {
            created = compiled.create();
        }
        catch (RuntimeException e) {
            throw new IllegalStateException("The create request for " + change.changeType().id()
                    + " WAS SENT to the Equinix API and failed: " + e.getMessage() + " Check the Fabric "
                    + "portal before re-proposing; the confirm token was consumed.", e);
        }

        ObjectNode payload = ctx.objectMapper().createObjectNode();
        payload.put("phase", "executed");
        payload.put("executed", true);
        payload.put("change_type", change.changeType().id());
        payload.set("result", created);
        payload.put("request_sha256", change.requestSha256());
        payload.put("spec_sha256", change.specSha256());
        payload.set("human_confirmation", humanConfirmationNode(ctx, confirmation));
        payload.set("confirm_checks", checks.toNode(ctx.objectMapper()));
        payload.put("token_state", "consumed");
        payload.put("note", "Executed the exact proposed request via the real create endpoint (no dryRun "
                + "parameter). The created resource is billable. The confirm token is now consumed.");
        if (cancel.cancelRequestedAfterCommit()) {
            ObjectNode cancellation = payload.putObject("cancellation");
            cancellation.put("requested_after_send", true);
            cancellation.put("note", "The client cancelled this tool call after the create request was "
                    + "sent. The create was not undone: the resource above exists.");
        }
        return payload;
    }

    private static ObjectNode notExecuted(ServerContext ctx, PendingChange change, HumanConfirmation confirmation,
                                          ConfirmChecks checks, String note, CancellationSignal cancel) {
        ObjectNode payload = ctx.objectMapper().createObjectNode();
        payload.put("phase", "not_executed");
        payload.put("executed", false);
        payload.put("change_type", change.changeType().id());
        payload.put("request_sha256", change.requestSha256());
        payload.put("spec_sha256", change.specSha256());
        payload.set("human_confirmation", humanConfirmationNode(ctx, confirmation));
        payload.set("confirm_checks", checks.toNode(ctx.objectMapper()));
        payload.put("token_state", "consumed");
        payload.put("note", note);
        if (cancel.isCancelled()) {
            ObjectNode cancellation = payload.putObject("cancellation");
            cancellation.put("requested_after_send", false);
            cancellation.put("note", "The client cancelled this tool call; nothing was sent.");
        }
        payload.put("next_step", "If the change is still wanted, call fabric_propose_change again and "
                + "confirm with the fresh token.");
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
     * the proposal's age against its TTL, the request binding, every field of the compiled request
     * body (exactly what is sent), and what each answer does.
     */
    static String confirmationPrompt(PendingChange change, JsonNode spec, JsonNode requestBody, Duration age,
                                     Duration ttl) {
        return "Approve execution of a Fabric change proposed through fabric_propose_change.\n"
                + "Change type: " + change.changeType().id() + "\n"
                + "Target: " + describeTarget(change.changeType(), spec) + "\n"
                + "Price context: " + (change.priceSummary() == null
                ? "none was recorded with the proposal" : change.priceSummary()) + "\n"
                + "Proposal age: " + age.toSeconds() + " s (proposals expire " + ttl.toSeconds()
                + " s after the dry run)\n"
                + "Request SHA-256: " + change.requestSha256() + "\n"
                + "Every field of the request that will be sent:\n" + describeAllFields(requestBody)
                + "Accepting with confirm=true sends the real create request to the Equinix Fabric API. "
                + "Declining, dismissing or not answering sends nothing. The confirm token is single-use "
                + "and is already consumed, so a new fabric_propose_change is needed after any answer "
                + "other than accept.";
    }

    /**
     * Every leaf of a JSON document (the compiled request body) as one {@code path = value} line, so
     * the human sees all of what the create carries (notification emails, project and purchase-order identifiers, VLANs, and
     * a service token's access-grant settings), not only the summary line. Values are rendered as
     * JSON literals with control characters escaped, so no spec value can start a new prompt line.
     */
    static String describeAllFields(JsonNode spec) {
        StringBuilder out = new StringBuilder();
        appendLeaves(out, "", spec);
        return out.toString();
    }

    private static void appendLeaves(StringBuilder out, String path, JsonNode node) {
        if (node.isObject()) {
            List<String> names = new java.util.ArrayList<>();
            node.fieldNames().forEachRemaining(names::add);
            java.util.Collections.sort(names);
            for (String name : names) {
                appendLeaves(out, path.isEmpty() ? name : path + "." + name, node.get(name));
            }
        } else if (node.isArray()) {
            if (node.isEmpty()) {
                out.append("  ").append(path).append(" = []\n");
            }
            for (int i = 0; i < node.size(); i++) {
                appendLeaves(out, path + "[" + i + "]", node.get(i));
            }
        } else {
            // JsonNode.toString() renders a JSON literal: strings are quoted and \n, \r, \t and
            // other control characters are escaped.
            out.append("  ").append(path).append(" = ").append(node.toString()).append("\n");
        }
    }

    /** A one-line description of what the stored spec creates, read from the spec's own fields. */
    private static String describeTarget(ChangeType changeType, JsonNode spec) {
        String name = spec.path("name").isMissingNode() ? "(unnamed)" : spec.path("name").toString();
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
     * The redemption predicates of a confirm attempt, in evaluation order. {@code null} means the
     * predicate was not evaluated because an earlier one failed. They correspond to the checks the
     * Connection Coordinator specification requires of {@code ConfirmActivationKey} (the resource
     * still exists, the key was not already used, the request matches the recorded intent), applied
     * here to a process-local confirm token.
     *
     * @param proposalExists the token was minted by this server process and is still remembered
     * @param notEvicted the proposal was not evicted at capacity before this attempt
     * @param notExpired the proposal's TTL had not elapsed, both when the token was presented and when
     *        the human's answer arrived
     * @param notPreviouslyUsed no earlier confirm attempt consumed the token
     * @param requestMatchesBinding recompiling the stored spec produced the request whose SHA-256 the
     *        token was bound to
     */
    record ConfirmChecks(Boolean proposalExists, Boolean notEvicted, Boolean notExpired,
                         Boolean notPreviouslyUsed, Boolean requestMatchesBinding) {

        static ConfirmChecks unknown() {
            return new ConfirmChecks(false, null, null, null, null);
        }

        static ConfirmChecks evicted() {
            return new ConfirmChecks(true, false, null, null, null);
        }

        static ConfirmChecks expired() {
            return new ConfirmChecks(true, true, false, true, null);
        }

        static ConfirmChecks replayed() {
            return new ConfirmChecks(true, true, null, false, null);
        }

        static ConfirmChecks bindingMismatch() {
            return new ConfirmChecks(true, true, true, true, false);
        }

        static ConfirmChecks allPassed() {
            return new ConfirmChecks(true, true, true, true, true);
        }

        ObjectNode toNode(ObjectMapper mapper) {
            ObjectNode node = mapper.createObjectNode();
            node.put("proposal_exists", proposalExists);
            node.put("not_evicted", notEvicted);
            node.put("not_expired", notExpired);
            node.put("not_previously_used", notPreviouslyUsed);
            node.put("request_matches_binding", requestMatchesBinding);
            return node;
        }

        String describe() {
            return "confirm_checks: proposal_exists=" + text(proposalExists) + ", not_evicted="
                    + text(notEvicted) + ", not_expired=" + text(notExpired) + ", not_previously_used="
                    + text(notPreviouslyUsed) + ", request_matches_binding=" + text(requestMatchesBinding) + ".";
        }

        private static String text(Boolean value) {
            return value == null ? "not_evaluated" : value.toString();
        }
    }

    // local schema/argument helpers
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
                + "devices; metro_code, e.g. 'SV', required for a service-profile z_side). For network_create: {type: EVPLAN|EPLAN|IPWAN|EVPTREE|EPTREE, name, "
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
