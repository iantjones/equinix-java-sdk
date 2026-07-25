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

package api.equinix.javasdk.mcp.server;

import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A small, reusable bridge to MCP <em>elicitation</em>: the server-initiated
 * "ask the user to pick one" prompt. A tool handler that reaches a genuine decision point (which
 * billing account, which service profile) calls {@link #chooseOne} with the live client
 * {@link McpSyncServerExchange} and a list of {@link Option}s, and gets back a typed {@link Outcome}
 * it can act on <em>or fall back from</em>.
 *
 * <h3>Degrades, never dictates</h3>
 * <p>Elicitation is a capability the <strong>client</strong> declares at initialize time (it is not a
 * server capability — the server discovers it off the negotiated {@link McpSchema.ClientCapabilities}
 * on the exchange). So this helper is defensive by construction:</p>
 * <ul>
 *   <li>no exchange (a direct call, or a transport that supplied none), or a client that did not
 *       declare form elicitation &rarr; {@link Status#UNSUPPORTED}: the caller falls back (a structured
 *       {@code choice_required} result, or the already-valid default);</li>
 *   <li>the user accepts and picks an option &rarr; {@link Status#PICKED} with that option;</li>
 *   <li>the user declines or cancels &rarr; {@link Status#DECLINED}: the caller uses its default;</li>
 *   <li>the round-trip times out or errors &rarr; {@link Status#FAILED}: treated like a decline.</li>
 * </ul>
 *
 * <h3>Never blocks forever</h3>
 * <p>The SDK's {@code createElicitation} blocks until the client answers. A stalled or silent client
 * must never wedge a tool call, so the round-trip runs on a daemon worker under a hard timeout; on
 * timeout the call returns {@link Status#FAILED} and the caller proceeds. The timeout is generous (a
 * human may be at the other end) but bounded — see {@link ServerContext#elicitTimeoutMillis()}.</p>
 */
final class ElicitationSupport {

    /** The form field the single-select schema collects the pick into. */
    static final String CHOICE_FIELD = "choice";

    private static final Logger logger = LoggerFactory.getLogger(ElicitationSupport.class);
    private static final AtomicInteger POOL_SEQUENCE = new AtomicInteger();

    /** A shared daemon pool that carries the blocking {@code createElicitation} off the caller's thread. */
    private static final ExecutorService ELICIT_EXECUTOR = Executors.newCachedThreadPool(daemonFactory());

    private ElicitationSupport() {
    }

    /**
     * Whether the negotiated client on {@code exchange} can service a <em>form</em> elicitation. A
     * {@code null} exchange, a client that advertised no capabilities, or one that declared elicitation
     * without form support all read as {@code false}. Per spec, an empty {@code elicitation: {}} object
     * means form support.
     *
     * @param exchange the client exchange (may be {@code null})
     * @return {@code true} when a form elicitation can be sent
     */
    static boolean supportsForm(McpSyncServerExchange exchange) {
        if (exchange == null) {
            return false;
        }
        McpSchema.ClientCapabilities capabilities;
        try {
            capabilities = exchange.getClientCapabilities();
        }
        catch (RuntimeException e) {
            return false;
        }
        if (capabilities == null || capabilities.elicitation() == null) {
            return false;
        }
        McpSchema.ClientCapabilities.Elicitation elicitation = capabilities.elicitation();
        // elicitation: {} (both null) is equivalent to form support; an explicit url-only declaration is not.
        return elicitation.form() != null || elicitation.url() == null;
    }

    /**
     * Prompts the user, through the client, to pick exactly one of {@code options}, using this
     * context's elicitation timeout.
     *
     * @param ctx the owning server context (for the elicitation timeout)
     * @param exchange the live client exchange (may be {@code null})
     * @param message the human-readable prompt
     * @param options the options to choose from (must be non-empty)
     * @return the typed outcome
     */
    static Outcome chooseOne(ServerContext ctx, McpSyncServerExchange exchange, String message,
                            List<Option> options) {
        return chooseOne(exchange, message, options, ctx.elicitTimeoutMillis());
    }

    /**
     * Prompts the user, through the client, to pick exactly one of {@code options}.
     *
     * @param exchange the live client exchange (may be {@code null})
     * @param message the human-readable prompt
     * @param options the options to choose from (must be non-empty)
     * @param timeoutMillis the hard timeout for the round-trip
     * @return the typed outcome — {@link Status#UNSUPPORTED} when the client cannot elicit, so the
     *         caller can fall back cleanly
     */
    static Outcome chooseOne(McpSyncServerExchange exchange, String message, List<Option> options,
                            long timeoutMillis) {
        if (options == null || options.isEmpty()) {
            throw new IllegalArgumentException("chooseOne needs at least one option");
        }
        if (!supportsForm(exchange)) {
            return new Outcome(Status.UNSUPPORTED, null,
                    "the client did not declare elicitation support");
        }

        McpSchema.ElicitRequest request = McpSchema.ElicitFormRequest
                .builder(message, singleSelectSchema(message, options))
                .build();

        McpSchema.ElicitResult result;
        Future<McpSchema.ElicitResult> future = ELICIT_EXECUTOR.submit(() -> exchange.createElicitation(request));
        try {
            result = future.get(timeoutMillis, TimeUnit.MILLISECONDS);
        }
        catch (TimeoutException e) {
            future.cancel(true);
            logger.warn("Elicitation timed out after {} ms; falling back", timeoutMillis);
            return new Outcome(Status.FAILED, null, "the elicitation timed out after " + timeoutMillis + " ms");
        }
        catch (ExecutionException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            logger.warn("Elicitation failed: {}", cause.toString());
            return new Outcome(Status.FAILED, null, "the elicitation failed: " + cause.getMessage());
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            future.cancel(true);
            return new Outcome(Status.FAILED, null, "the elicitation was interrupted");
        }

        if (result == null || result.action() == null) {
            return new Outcome(Status.FAILED, null, "the client returned no elicitation result");
        }
        switch (result.action()) {
            case ACCEPT:
                Option picked = match(options, result.content());
                if (picked == null) {
                    return new Outcome(Status.DECLINED, null,
                            "the user accepted but returned no resolvable selection");
                }
                return new Outcome(Status.PICKED, picked, "the user selected '" + picked.label() + "'");
            case DECLINE:
                return new Outcome(Status.DECLINED, null, "the user declined the prompt");
            case CANCEL:
            default:
                return new Outcome(Status.DECLINED, null, "the user dismissed the prompt");
        }
    }

    /** Matches the accepted form content back to one of the offered options by its id. */
    private static Option match(List<Option> options, Map<String, Object> content) {
        if (content == null) {
            return null;
        }
        Object raw = content.get(CHOICE_FIELD);
        if (raw == null) {
            return null;
        }
        String chosen = String.valueOf(raw).trim();
        for (Option option : options) {
            if (option.id().equals(chosen)) {
                return option;
            }
        }
        return null;
    }

    /**
     * A restricted single-select form schema: one required {@code choice} string field whose allowed
     * values are the option ids, each paired with a human-readable title (the SDK's modern titled
     * single-select shape — {@code type: string} + {@code oneOf: [{const, title}]}).
     */
    private static Map<String, Object> singleSelectSchema(String message, List<Option> options) {
        List<Map<String, Object>> oneOf = new ArrayList<>(options.size());
        for (Option option : options) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("const", option.id());
            entry.put("title", option.title());
            oneOf.add(entry);
        }
        Map<String, Object> choice = new LinkedHashMap<>();
        choice.put("type", "string");
        choice.put("title", "Selection");
        choice.put("description", message);
        choice.put("oneOf", oneOf);

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put(CHOICE_FIELD, choice);

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", List.of(CHOICE_FIELD));
        return schema;
    }

    private static ThreadFactory daemonFactory() {
        return runnable -> {
            Thread thread = new Thread(runnable, "mcp-elicit-" + POOL_SEQUENCE.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }

    /** The status of an elicitation attempt. */
    enum Status {

        /** The user accepted and picked an option — {@link Outcome#option()} is set. */
        PICKED,

        /** The user declined or cancelled — the caller should use its default. */
        DECLINED,

        /** The client cannot elicit (no exchange, or no form-elicitation capability). */
        UNSUPPORTED,

        /** The round-trip timed out or errored — treated like a decline. */
        FAILED
    }

    /**
     * One selectable choice: a machine id (the value returned on accept), a human-readable label, and
     * an optional one-line detail folded into the option's display title.
     *
     * @param id the machine-readable value (a billing account number, a service-profile uuid)
     * @param label the concise human-readable label
     * @param detail an optional extra clause for the display title, or {@code null}
     */
    record Option(String id, String label, String detail) {

        Option(String id, String label) {
            this(id, label, null);
        }

        /** The display title shown to the user: {@code label}, with {@code detail} appended when present. */
        String title() {
            return detail == null || detail.isBlank() ? label : label + " — " + detail;
        }
    }

    /**
     * The typed result of {@link #chooseOne}. Callers branch on {@link #picked()} to use the selection,
     * and otherwise fall back — {@link #status()} and {@link #detail()} distinguish <em>why</em> for the
     * payload (unsupported vs declined vs failed).
     *
     * @param status the outcome status
     * @param option the picked option (non-{@code null} only when {@link Status#PICKED})
     * @param detail a human/LLM-readable explanation of the outcome
     */
    record Outcome(Status status, Option option, String detail) {

        /** @return {@code true} when the user picked an option. */
        boolean picked() {
            return status == Status.PICKED;
        }
    }
}
