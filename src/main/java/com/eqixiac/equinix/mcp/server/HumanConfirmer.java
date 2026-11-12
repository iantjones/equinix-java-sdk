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


package com.eqixiac.equinix.mcp.server;

/**
 * How the server asks the person at the MCP client to approve one action. This is the only seam
 * the Safe Mutation Broker's human check goes through ({@link ServerContext#confirmWithHuman(String)}),
 * so replacing the delivery mechanism is a change to one implementation of this interface.
 *
 * <h2>Protocol revisions</h2>
 * <p>The default, {@link #elicitation()}, sends a server-to-client {@code elicitation/create} form
 * request and blocks the tool call until the answer arrives. That is how every protocol revision the
 * bundled MCP Java SDK (2.1.0) negotiates works: 2024-11-05, 2025-03-26, 2025-06-18 and 2025-11-25
 * (form elicitation exists from 2025-06-18; an older client simply declares no elicitation and the
 * broker fails closed).</p>
 *
 * <p>Revision 2026-07-28 is stateless and replaces {@code elicitation/create} with multi-round-trip
 * requests: the tool call returns an {@code InputRequiredResult} carrying the form and a sealed
 * {@code requestState}, and the client retries the same {@code tools/call} with
 * {@code inputResponses}. No GA release of the Java SDK implements it (2.1.0 is the latest GA on
 * 2026-11-12; 2.2.0-M2 carries an experimental {@code io.modelcontextprotocol.modern} server). When
 * a GA SDK does, the swap is:</p>
 * <ol>
 *   <li>an implementation of this interface that, on the first call, returns a "pending" outcome
 *       carrying the form, and on the retry reads the answer from {@code inputResponses};</li>
 *   <li>in {@code fabric_confirm_change}, the token is consumed on the retry that carries the answer
 *       (not on the first round trip), and the compiled-request hash is sealed into
 *       {@code requestState} so the retry is bound to the same request;</li>
 *   <li>the server is built on the SDK's stateless server for clients that negotiate 2026-07-28,
 *       keeping this blocking implementation for older clients.</li>
 * </ol>
 */
@FunctionalInterface
public interface HumanConfirmer {

    /**
     * Asks for approval of one action.
     *
     * @param context the server context of the current call (bound exchange, timeouts)
     * @param message the prompt: what will be executed and what each answer does
     * @param cancellation the current call's signal; a cancel abandons the prompt
     * @return what the client reported; never {@code null}. Only
     *         {@link HumanConfirmation.Status#ACCEPTED} is an approval.
     */
    HumanConfirmation confirm(ServerContext context, String message, CancellationSignal cancellation);

    /**
     * @return the default confirmer: an MCP form elicitation ({@code elicitation/create}) over the
     *         exchange bound to the current call, bounded by
     *         {@link ServerContext#elicitTimeoutMillis()}
     */
    static HumanConfirmer elicitation() {
        return (context, message, cancellation) -> ElicitationSupport.confirm(
                context.currentExchange(), message, context.elicitTimeoutMillis(), cancellation);
    }
}
