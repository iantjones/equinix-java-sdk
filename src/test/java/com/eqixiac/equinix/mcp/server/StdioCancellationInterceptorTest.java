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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("StdioCancellationInterceptor - notifications/cancelled reaches the matching tools/call")
class StdioCancellationInterceptorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String CALL = "{\"jsonrpc\":\"2.0\",\"id\":\"abc\",\"method\":\"tools/call\","
            + "\"params\":{\"name\":\"x\",\"arguments\":{},\"_meta\":{\"progressToken\":5}}}";

    @Test
    @DisplayName("a tools/call is tagged with its id; a cancel trips that call's signal and is consumed")
    void cancelTripsTheMatchingCall() throws Exception {
        StdioCancellationInterceptor interceptor = new StdioCancellationInterceptor(MAPPER);
        JsonNode tagged = MAPPER.readTree(interceptor.filter(CALL));
        JsonNode meta = tagged.get("params").get("_meta");
        assertEquals("\"abc\"", meta.get(StdioCancellationInterceptor.REQUEST_KEY_META).asText());
        assertEquals(5, meta.get("progressToken").asInt(), "existing _meta is kept");

        Map<String, Object> callMeta = Map.of(StdioCancellationInterceptor.REQUEST_KEY_META, "\"abc\"");
        CancellationSignal signal = interceptor.signalFor(callMeta);
        assertSame(signal, interceptor.signalFor(callMeta), "the adapter finds the same signal");
        assertFalse(signal.isCancelled());

        // A cancel for a numeric id 'abc' is a different id and does nothing.
        assertNull(interceptor.filter("{\"jsonrpc\":\"2.0\",\"method\":\"notifications/cancelled\","
                + "\"params\":{\"requestId\":7}}"));
        assertFalse(signal.isCancelled());

        assertNull(interceptor.filter("{\"jsonrpc\":\"2.0\",\"method\":\"notifications/cancelled\","
                + "\"params\":{\"requestId\":\"abc\",\"reason\":\"user stopped it\"}}"),
                "the notification is consumed (the SDK has no handler for it)");
        assertTrue(signal.isCancelled());
        assertEquals("user stopped it", signal.reason());

        interceptor.release(callMeta);
        assertEquals(0, interceptor.inFlightCount());
        assertNotSame(signal, interceptor.signalFor(callMeta), "a released call is forgotten");
    }

    @Test
    @DisplayName("other messages pass through byte for byte, including malformed lines")
    void otherLinesPassThrough() {
        StdioCancellationInterceptor interceptor = new StdioCancellationInterceptor(MAPPER);
        for (String line : List.of(
                "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\",\"params\":{}}",
                "{\"jsonrpc\":\"2.0\",\"id\":2,\"result\":{\"action\":\"accept\"}}",
                "not json mentioning tools/call",
                "[{\"jsonrpc\":\"2.0\",\"method\":\"notifications/cancelled\"}]")) {
            assertEquals(line, interceptor.filter(line));
        }
    }

    @Test
    @DisplayName("the wrapped stream serves filtered lines and drops consumed ones")
    void wrappedStream() throws Exception {
        StdioCancellationInterceptor interceptor = new StdioCancellationInterceptor(MAPPER);
        String input = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"ping\"}\n"
                + "{\"jsonrpc\":\"2.0\",\"method\":\"notifications/cancelled\",\"params\":{\"requestId\":9}}\n"
                + "{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"ping\",\"params\":{\"note\":\"é\"}}\n";
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                interceptor.wrap(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8))),
                StandardCharsets.UTF_8));
        assertEquals("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"ping\"}", reader.readLine());
        assertEquals("{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"ping\",\"params\":{\"note\":\"é\"}}",
                reader.readLine());
        assertNull(reader.readLine());
    }

    @Test
    @DisplayName("commit() orders against cancel(): after commit a cancel is only recorded")
    void commitOrdering() {
        CancellationSignal before = CancellationSignal.create();
        before.cancel(null);
        assertFalse(before.commit(), "a cancelled call may not send its irreversible request");

        CancellationSignal after = CancellationSignal.create();
        assertTrue(after.commit());
        after.cancel("late");
        assertFalse(after.isCancelled(), "the call carries on to report what it sent");
        assertTrue(after.cancelRequestedAfterCommit());

        CancellationSignal none = CancellationSignal.none();
        none.cancel("ignored");
        assertFalse(none.isCancelled());
    }
}
