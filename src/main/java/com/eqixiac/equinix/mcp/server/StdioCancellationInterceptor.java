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
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Connects the client's {@code notifications/cancelled} to the {@link CancellationSignal} of the
 * {@code tools/call} it names.
 *
 * <p>The MCP Java SDK 2.1.0 has no handler for {@code notifications/cancelled} (it logs "No handler
 * registered" and drops it), and a sync tool handler does not see the JSON-RPC id of its own
 * request. This class therefore sits in front of the stdio transport's input stream, one
 * newline-delimited JSON-RPC message per line:</p>
 * <ul>
 *   <li>a {@code tools/call} request: a signal is registered under its id, and the id is written
 *       into the request's {@code params._meta} under {@link #REQUEST_KEY_META} so the tool adapter
 *       can find the signal ({@link #signalFor(Map)}). Any value the client put under that key is
 *       overwritten;</li>
 *   <li>{@code notifications/cancelled}: the named call's signal is cancelled and the line is
 *       consumed (the SDK would only log it);</li>
 *   <li>every other line passes through unchanged, byte for byte.</li>
 * </ul>
 *
 * <p>A cancel for an id that is not in flight (already finished, or never seen) is ignored, as the
 * specification allows. At most {@value #MAX_IN_FLIGHT} signals are held; the oldest is dropped
 * beyond that, so a request the SDK rejects before any handler runs cannot leak an entry.</p>
 */
final class StdioCancellationInterceptor {

    /** The {@code params._meta} key carrying the JSON text of the {@code tools/call} request id. */
    static final String REQUEST_KEY_META = "com.eqixiac.equinix.mcp/request-id";

    static final int MAX_IN_FLIGHT = 256;

    private static final Logger logger = LoggerFactory.getLogger(StdioCancellationInterceptor.class);

    private final ObjectMapper mapper;
    private final Map<String, CancellationSignal> inFlight = new LinkedHashMap<>(16, 0.75f, false) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, CancellationSignal> eldest) {
            return size() > MAX_IN_FLIGHT;
        }
    };

    StdioCancellationInterceptor(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * @param source the raw stdin of the server process
     * @return a stream the stdio transport reads instead of {@code source}
     */
    InputStream wrap(InputStream source) {
        return new LineFilterInputStream(source);
    }

    /**
     * The signal of the call whose {@code _meta} carries {@link #REQUEST_KEY_META}, or a fresh
     * signal that nothing can cancel from the wire when the call did not come through this
     * interceptor.
     *
     * @param meta the call's {@code _meta}, or {@code null}
     * @return the signal; never {@code null}
     */
    CancellationSignal signalFor(Map<String, Object> meta) {
        Object key = meta == null ? null : meta.get(REQUEST_KEY_META);
        if (key != null) {
            synchronized (inFlight) {
                CancellationSignal signal = inFlight.get(String.valueOf(key));
                if (signal != null) {
                    return signal;
                }
            }
        }
        return CancellationSignal.create();
    }

    /**
     * Forgets a finished call.
     *
     * @param meta the call's {@code _meta}, or {@code null}
     */
    void release(Map<String, Object> meta) {
        Object key = meta == null ? null : meta.get(REQUEST_KEY_META);
        if (key != null) {
            synchronized (inFlight) {
                inFlight.remove(String.valueOf(key));
            }
        }
    }

    /** @return the number of calls currently tracked (tests) */
    int inFlightCount() {
        synchronized (inFlight) {
            return inFlight.size();
        }
    }

    /**
     * Applies the rules above to one line.
     *
     * @param line one newline-delimited message
     * @return the line to forward, or {@code null} to consume it
     */
    String filter(String line) {
        if (line.indexOf("tools/call") < 0 && line.indexOf("notifications/cancelled") < 0) {
            return line;
        }
        JsonNode message;
        try {
            message = mapper.readTree(line);
        }
        catch (IOException | RuntimeException e) {
            return line;
        }
        if (message == null || !message.isObject()) {
            return line;
        }
        String method = message.path("method").asText("");
        if ("tools/call".equals(method) && message.hasNonNull("id") && message.path("params").isObject()) {
            String key = message.get("id").toString();
            synchronized (inFlight) {
                inFlight.put(key, CancellationSignal.create());
            }
            ObjectNode params = (ObjectNode) message.get("params");
            JsonNode meta = params.get("_meta");
            ObjectNode metaObject = meta != null && meta.isObject() ? (ObjectNode) meta : params.putObject("_meta");
            metaObject.put(REQUEST_KEY_META, key);
            try {
                return mapper.writeValueAsString(message);
            }
            catch (IOException e) {
                return line;
            }
        }
        if ("notifications/cancelled".equals(method) && !message.has("id")) {
            JsonNode requestId = message.path("params").get("requestId");
            if (requestId != null && !requestId.isNull()) {
                CancellationSignal signal;
                synchronized (inFlight) {
                    signal = inFlight.get(requestId.toString());
                }
                if (signal != null) {
                    JsonNode reason = message.path("params").get("reason");
                    logger.info("Client cancelled tool call {}", UntrustedText.sanitize(requestId.toString(), 80));
                    signal.cancel(reason == null || reason.isNull() ? null : reason.asText());
                }
            }
            return null;
        }
        return line;
    }

    /** Reads the source line by line and serves the filtered lines as UTF-8 bytes. */
    private final class LineFilterInputStream extends InputStream {

        private final BufferedReader reader;
        private byte[] buffer = new byte[0];
        private int position;
        private boolean eof;

        LineFilterInputStream(InputStream source) {
            this.reader = new BufferedReader(new InputStreamReader(source, StandardCharsets.UTF_8));
        }

        private boolean fill() throws IOException {
            while (position >= buffer.length) {
                if (eof) {
                    return false;
                }
                String line = reader.readLine();
                if (line == null) {
                    eof = true;
                    return false;
                }
                String forwarded = filter(line);
                if (forwarded != null) {
                    buffer = (forwarded + "\n").getBytes(StandardCharsets.UTF_8);
                    position = 0;
                }
            }
            return true;
        }

        @Override
        public int read() throws IOException {
            if (!fill()) {
                return -1;
            }
            return buffer[position++] & 0xff;
        }

        @Override
        public int read(byte[] target, int offset, int length) throws IOException {
            if (length == 0) {
                return 0;
            }
            if (!fill()) {
                return -1;
            }
            int count = Math.min(length, buffer.length - position);
            System.arraycopy(buffer, position, target, offset, count);
            position += count;
            return count;
        }

        @Override
        public int available() {
            return buffer.length - position;
        }

        @Override
        public void close() throws IOException {
            reader.close();
        }
    }
}
