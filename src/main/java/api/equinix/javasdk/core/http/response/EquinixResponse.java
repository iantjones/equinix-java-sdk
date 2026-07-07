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

package api.equinix.javasdk.core.http.response;

import lombok.Getter;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 * Immutable view of a single HTTP exchange, exposing exactly what response handling needs: the
 * status line, a read-only case-insensitive snapshot of the response headers, and the body stream
 * with its declared length. No transport (Apache HttpClient) type appears in this class's API —
 * the HTTP client captures these values from the raw transport response at construction.
 *
 * <p>Note that {@code getContent()} exposes the one-shot connection-backed body stream: once the
 * SDK's response handling has consumed it (the normal case), it is exhausted/closed and cannot be
 * re-read. Reading the stream to end-of-stream (or calling {@link #drainQuietly()}) releases the
 * pooled connection. The header map keeps a single value per header name (last one wins for
 * repeated headers such as {@code Set-Cookie}).</p>
 *
 * <p>The type parameter {@code T} threads the operation's model type from the request through to
 * the response handlers; it carries no state here.</p>
 *
 * @author ianjones
 */
@Getter
public class EquinixResponse<T> {

    private final int statusCode;
    private final String statusText;

    /**
     * The response body stream, or {@code null} when the response has no body. One-shot and
     * connection-backed: reading it to end-of-stream releases the pooled connection.
     */
    private final InputStream content;

    /**
     * The declared body length in bytes: {@code 0} for an explicitly empty body, negative when
     * unknown (e.g. chunked transfer encoding).
     */
    private final long contentLength;

    private final Map<String, String> headers;

    /**
     * Captures a response snapshot. Internal — constructed by the SDK's HTTP client from the raw
     * transport response.
     *
     * @param statusCode the HTTP status code
     * @param statusText the HTTP reason phrase (may be {@code null})
     * @param headers the response headers (copied into a case-insensitive read-only map)
     * @param content the body stream, or {@code null} when the response has no body
     * @param contentLength the declared body length; negative when unknown
     */
    public EquinixResponse(int statusCode, String statusText, Map<String, String> headers,
                           InputStream content, long contentLength) {
        this.statusCode = statusCode;
        this.statusText = statusText;
        this.content = content;
        this.contentLength = contentLength;

        Map<String, String> headerSnapshot = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        if (headers != null) {
            headerSnapshot.putAll(headers);
        }
        this.headers = Collections.unmodifiableMap(headerSnapshot);
    }

    /**
     * @return {@code true} for a 2xx status
     */
    public boolean isSuccessful() {
        return statusCode / 100 == 2;
    }

    /**
     * Best-effort consumes the body stream to end-of-stream and closes it, so the underlying
     * pooled connection is released back to the connection manager. Used by response handlers
     * that do not otherwise read the body; releasing a connection must never fail the call, so
     * all errors are swallowed.
     */
    public void drainQuietly() {
        if (content == null) {
            return;
        }
        try (InputStream in = content) {
            in.transferTo(OutputStream.nullOutputStream());
        }
        catch (Exception ignored) {
            // best effort — never let connection release break the call
        }
    }
}
