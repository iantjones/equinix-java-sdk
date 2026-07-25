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

import java.util.Map;
import java.util.function.Function;

/**
 * Test doubles for the MCP {@link McpSyncServerExchange}. The real exchange delegates to a live async
 * session; here we subclass it with a {@code null} delegate and override only the two methods the
 * elicitation path touches — {@link McpSyncServerExchange#getClientCapabilities()} and
 * {@link McpSyncServerExchange#createElicitation}. That lets a handler under test believe it is talking
 * to a client that does (or does not) support elicitation, and answer the prompt however the test wants.
 */
final class StubExchanges {

    private StubExchanges() {
    }

    /** Client capabilities declaring form-elicitation support (an empty {@code elicitation: {}}). */
    static McpSchema.ClientCapabilities elicitationCapable() {
        return McpSchema.ClientCapabilities.builder().elicitation().build();
    }

    /** Client capabilities with no elicitation declared. */
    static McpSchema.ClientCapabilities noElicitation() {
        return McpSchema.ClientCapabilities.builder().build();
    }

    /**
     * An exchange with the given capabilities that answers any elicitation via {@code answer}.
     *
     * @param capabilities the client capabilities to advertise (may be {@code null})
     * @param answer produces the elicit result for a request (may throw to simulate a transport error)
     * @return the stub exchange
     */
    static McpSyncServerExchange stub(McpSchema.ClientCapabilities capabilities,
                                      Function<McpSchema.ElicitRequest, McpSchema.ElicitResult> answer) {
        return new McpSyncServerExchange(null) {
            @Override
            public McpSchema.ClientCapabilities getClientCapabilities() {
                return capabilities;
            }

            @Override
            public McpSchema.ElicitResult createElicitation(McpSchema.ElicitRequest request) {
                if (answer == null) {
                    throw new IllegalStateException("no elicitation answer configured for this stub");
                }
                return answer.apply(request);
            }
        };
    }

    /** An elicitation-capable exchange that ACCEPTs, returning the given {@code choice} value. */
    static McpSyncServerExchange accepts(String choiceId) {
        Map<String, Object> content = Map.of(ElicitationSupport.CHOICE_FIELD, choiceId);
        return stub(elicitationCapable(), request -> new McpSchema.ElicitResult(
                McpSchema.ElicitResult.Action.ACCEPT, content));
    }

    /** An elicitation-capable exchange that the user DECLINEs. */
    static McpSyncServerExchange declines() {
        return stub(elicitationCapable(),
                request -> new McpSchema.ElicitResult(McpSchema.ElicitResult.Action.DECLINE, null));
    }

    /** An exchange that does not support elicitation (must never actually be prompted). */
    static McpSyncServerExchange unsupported() {
        return stub(noElicitation(), request -> {
            throw new AssertionError("createElicitation must not be called when unsupported");
        });
    }
}
