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
 * Thrown by a handler that stops because the client cancelled its tool call with
 * {@code notifications/cancelled}. The server turns it into a tool error result that says nothing
 * irreversible was sent.
 */
public final class ToolCallCancelledException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @param stage where the handler stopped, for example "before the dry run"
     * @param reason the client's reason, or {@code null}
     */
    public ToolCallCancelledException(String stage, String reason) {
        super("The client cancelled this tool call (" + (reason == null || reason.isBlank()
                ? "no reason given" : "reason: " + UntrustedText.sanitize(reason, 200)) + "); it stopped "
                + stage + ". Nothing irreversible was sent.");
    }
}
