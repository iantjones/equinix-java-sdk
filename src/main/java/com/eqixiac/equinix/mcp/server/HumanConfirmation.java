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

import java.util.Locale;

/**
 * The result of asking the person at the MCP client to approve one action through a form
 * elicitation ({@link ServerContext#confirmWithHuman(String)}).
 *
 * <p>The MCP client is the only party in a tool call that can attest a person saw a prompt and
 * answered it. A tool argument cannot: the calling model writes every argument. This type records
 * what the client reported.</p>
 *
 * <table>
 *   <caption>Statuses and what the caller may do</caption>
 *   <tr><th>Status</th><th>Observed</th><th>Approval given</th></tr>
 *   <tr><td>{@link Status#ACCEPTED}</td><td>the client returned {@code accept} and the form's
 *       {@code confirm} field was {@code true}</td><td>yes</td></tr>
 *   <tr><td>{@link Status#DECLINED}</td><td>the client returned {@code decline}, or returned
 *       {@code accept} without {@code confirm = true}</td><td>no</td></tr>
 *   <tr><td>{@link Status#CANCELLED}</td><td>the client returned {@code cancel} (the prompt was
 *       dismissed)</td><td>no</td></tr>
 *   <tr><td>{@link Status#TIMED_OUT}</td><td>no answer within
 *       {@link ServerContext#elicitTimeoutMillis()} ms</td><td>no</td></tr>
 *   <tr><td>{@link Status#FAILED}</td><td>the round trip threw, was interrupted, or returned no
 *       result</td><td>no</td></tr>
 *   <tr><td>{@link Status#UNSUPPORTED_BY_CLIENT}</td><td>no client exchange is bound, or the client
 *       did not declare form elicitation at initialize; no prompt was sent</td><td>not asked</td></tr>
 * </table>
 *
 * <p>Limit: an {@code accept} proves the client reported an acceptance. A client that answers
 * elicitations without showing them to a person defeats the check; the server cannot detect
 * that.</p>
 *
 * @param status what the client reported
 * @param detail a one-sentence description of the outcome, suitable for a tool payload
 */
public record HumanConfirmation(Status status, String detail) {

    /**
     * @return {@code true} only for {@link Status#ACCEPTED}
     */
    public boolean accepted() {
        return status == Status.ACCEPTED;
    }

    /**
     * @return {@code true} when no prompt could be sent because the client cannot service a form
     *         elicitation
     */
    public boolean unsupportedByClient() {
        return status == Status.UNSUPPORTED_BY_CLIENT;
    }

    /** What the client reported for one confirmation prompt. */
    public enum Status {

        /** {@code accept} with {@code confirm = true}. */
        ACCEPTED,

        /** {@code decline}, or {@code accept} without {@code confirm = true}. */
        DECLINED,

        /** {@code cancel}: the prompt was dismissed without a decision. */
        CANCELLED,

        /** No answer within the elicitation timeout. */
        TIMED_OUT,

        /** The round trip threw, was interrupted, or returned no result. */
        FAILED,

        /** The client cannot service a form elicitation; nothing was sent. */
        UNSUPPORTED_BY_CLIENT;

        /**
         * @return the lower-case payload spelling, for example {@code unsupported_by_client}
         */
        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
