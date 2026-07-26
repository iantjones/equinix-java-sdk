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

package com.eqixiac.equinix.core.http;

import com.eqixiac.equinix.core.enums.HttpMethod;
import com.eqixiac.equinix.core.enums.Protocol;
import com.eqixiac.equinix.core.exception.*;
import com.eqixiac.equinix.core.http.request.EquinixRequest;
import com.eqixiac.equinix.core.http.request.RequestFactory;
import com.eqixiac.equinix.core.http.response.EquinixResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.io.EmptyInputStream;
import org.apache.http.util.EntityUtils;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * The SDK's pooled, timeout-bounded HTTP client. Executes a single {@link EquinixRequest} against
 * the Equinix API with automatic, policy-driven retry of transient failures (see {@link RetryPolicy}),
 * mapping error responses to typed exceptions via {@link ResponseErrorMapper}. One instance is shared
 * across calls and is safe to reuse; {@link #close()} shuts down the owned connection pool.
 *
 * <p>Every logical request is tagged with a generated correlation id (a UUID), sent to the API as an
 * {@code X-Correlation-Id} header (shared by all retry attempts of that request), included in the
 * request/retry log lines, and carried on any resulting
 * {@link com.eqixiac.equinix.core.exception.EquinixServiceException} — so a failure in application
 * logs, the SDK's logs, and Equinix's server-side logs can be tied together by one id.</p>
 *
 * <p>An opt-in {@link CircuitBreaker} (see {@link #setCircuitBreaker(CircuitBreaker)}) is consulted
 * per attempt, alongside the retry policy; when open it fails fast with a
 * {@link com.eqixiac.equinix.core.exception.CircuitOpenException} without touching the network.</p>
 *
 * @author ianjones
 */
@Slf4j
public class EquinixHttpClient implements Closeable {

    private static final String RETRY_AFTER_HEADER = "Retry-After";
    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    private static final int DEFAULT_CONNECT_TIMEOUT_MS = 10_000;
    private static final int DEFAULT_SOCKET_TIMEOUT_MS = 60_000;
    private static final int DEFAULT_CONNECTION_REQUEST_TIMEOUT_MS = 5_000;
    private static final int DEFAULT_MAX_TOTAL_CONNECTIONS = 50;
    private static final int DEFAULT_MAX_CONNECTIONS_PER_ROUTE = 20;

    private final CloseableHttpClient httpClient;
    private final RequestFactory requestFactory = new RequestFactory();
    private final Protocol protocol = Protocol.HTTPS;
    private volatile boolean outputRequestJson = true;

    private volatile RetryPolicy retryPolicy = RetryPolicy.defaultPolicy();

    /**
     * The opt-in circuit breaker; {@code null} (the default) disables breaking entirely.
     */
    private volatile CircuitBreaker circuitBreaker;

    /**
     * Overrides the retry policy for transient failures.
     *
     * @param retryPolicy the policy to apply; {@link RetryPolicy#none()} disables retries
     */
    public void setRetryPolicy(RetryPolicy retryPolicy) {
        this.retryPolicy = (retryPolicy != null) ? retryPolicy : RetryPolicy.none();
    }

    /**
     * Enables (or, with {@code null}, disables) the opt-in circuit breaker. When set, every request
     * attempt asks the breaker for permission first: after the configured number of consecutive
     * service failures (5xx or transport {@code IOException}) the breaker opens and requests fail
     * fast with a {@link com.eqixiac.equinix.core.exception.CircuitOpenException} until the cooldown
     * elapses and a probe succeeds. Disabled by default.
     *
     * @param circuitBreaker the breaker to consult, or {@code null} to disable
     */
    public void setCircuitBreaker(CircuitBreaker circuitBreaker) {
        this.circuitBreaker = circuitBreaker;
    }

    /**
     *
     * <p>Builds a connection-pooled, timeout-bounded HTTP client. Without explicit
     * timeouts a stalled server can block a calling thread indefinitely; without a
     * pooled connection manager every concurrent call would open (and leak) its own
     * socket. The pool and timeouts below are sensible defaults for typical SDK use.</p>
     */
    public EquinixHttpClient() {
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(DEFAULT_MAX_TOTAL_CONNECTIONS);
        connectionManager.setDefaultMaxPerRoute(DEFAULT_MAX_CONNECTIONS_PER_ROUTE);

        RequestConfig requestConfig = RequestConfig.custom()
                .setCookieSpec(CookieSpecs.STANDARD)
                .setConnectTimeout(DEFAULT_CONNECT_TIMEOUT_MS)
                .setSocketTimeout(DEFAULT_SOCKET_TIMEOUT_MS)
                .setConnectionRequestTimeout(DEFAULT_CONNECTION_REQUEST_TIMEOUT_MS)
                .build();

        // Connection manager is owned by this client (not shared), so httpClient.close()
        // also shuts the pool down — see close().
        httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .build();
    }

    /**
     * Enables or disables DEBUG-level logging of request bodies.
     *
     * @param outputRequestJson {@code true} to log request bodies (at DEBUG), {@code false} to suppress
     */
    public void setOutputRequestJson(boolean outputRequestJson) {
        this.outputRequestJson = outputRequestJson;
    }

    /**
     * Executes one HTTP attempt. On an error status the response is fully drained (for the error
     * body) and then <em>closed</em> before the typed exception is thrown, so the pooled connection
     * is always released — even when reading the error body fails mid-stream. On the success path
     * the open response is returned for the caller's handler to consume.
     *
     * @throws com.eqixiac.equinix.core.exception.EquinixClientException if any.
     */
    private <T> EquinixResponse<T> executeSingleRequest(EquinixRequest<T> equinixRequest, SingleRequestParams singleRequestParams,
            String correlationId) throws EquinixClientException {

        singleRequestParams.newApacheRequest(requestFactory, equinixRequest);
        singleRequestParams.apacheRequest.setHeader(CORRELATION_ID_HEADER, correlationId);

        log.debug("{} {} [cid={}]", equinixRequest.getHttpMethod(), singleRequestParams.apacheRequest.getURI(), correlationId);

        try {
            logRequestBody(singleRequestParams.apacheRequest);

            singleRequestParams.apacheResponse = httpClient.execute(singleRequestParams.apacheRequest);

            EquinixResponse<T> equinixResponse = captureResponse(singleRequestParams.apacheResponse);

            log.debug("Status: {} {} [cid={}]", equinixResponse.getStatusCode(), equinixResponse.getStatusText(), correlationId);

            if(!isRequestSuccessful(singleRequestParams.apacheResponse)) {
                try {
                    String path = singleRequestParams.apacheRequest.getURI().toString();

                    Map<String, String> responseHeaders = null;
                    Header retryAfterHeader = singleRequestParams.apacheResponse.getFirstHeader(RETRY_AFTER_HEADER);
                    if (retryAfterHeader != null && retryAfterHeader.getValue() != null) {
                        responseHeaders = new HashMap<>();
                        responseHeaders.put(RETRY_AFTER_HEADER, retryAfterHeader.getValue());
                    }

                    throw ResponseErrorMapper.toException(
                            equinixResponse.getStatusCode(), path, responseHeaders, readErrorBody(equinixResponse), correlationId);
                }
                finally {
                    // The error body has been read (or failed to read); release the connection back
                    // to the pool unconditionally. A failed mid-stream read otherwise leaks the
                    // pooled connection until the route pool is exhausted.
                    closeQuietly(singleRequestParams.apacheResponse);
                }
            }
            else {
                return equinixResponse;
            }
        }
        catch (IOException ioe) {
            closeQuietly(singleRequestParams.apacheResponse);
            throw new EquinixClientException(ioe);
        }
    }

    /**
     * Closes an Apache response (releasing its pooled connection), swallowing any close failure.
     */
    private void closeQuietly(CloseableHttpResponse response) {
        if (response == null) {
            return;
        }
        try {
            response.close();
        }
        catch (IOException closeEx) {
            log.debug("Could not close HTTP response cleanly: {}", closeEx.getMessage());
        }
    }

    /**
     * Captures the transport-independent snapshot of an Apache response that the rest of the SDK
     * consumes: status line, case-insensitive header map, and the body stream with its declared
     * length. This is the only place where the raw Apache response crosses into the SDK's
     * response model.
     */
    private static <T> EquinixResponse<T> captureResponse(HttpResponse apacheResponse) throws IOException {
        StatusLine statusLine = apacheResponse.getStatusLine();

        Map<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (Header header : apacheResponse.getAllHeaders()) {
            headers.put(header.getName(), header.getValue());
        }

        HttpEntity entity = apacheResponse.getEntity();
        return new EquinixResponse<>(statusLine.getStatusCode(), statusLine.getReasonPhrase(), headers,
                entity != null ? entity.getContent() : null,
                entity != null ? entity.getContentLength() : 0L);
    }

    /**
     * Logs the request body at DEBUG when enabled. Reads from the built request's repeatable
     * entity (not a one-shot stream), so it is correct on every retry attempt and never consumes
     * the bytes that are actually sent. A logging failure is swallowed.
     */
    private void logRequestBody(HttpRequestBase apacheRequest) {
        if (!this.outputRequestJson || !log.isDebugEnabled()
                || !(apacheRequest instanceof HttpEntityEnclosingRequestBase enclosingRequest)
                || enclosingRequest.getEntity() == null
                || !enclosingRequest.getEntity().isRepeatable()) {
            return;
        }
        try {
            log.debug("Request body: {}", EntityUtils.toString(enclosingRequest.getEntity()));
        }
        catch (IOException bodyLogEx) {
            log.debug("Could not read request body for logging: {}", bodyLogEx.getMessage());
        }
    }

    /**
     * Reads the raw error response body as a UTF-8 string, returning {@code null} when there is no
     * body (or it cannot be read). The structured parsing of this body is delegated to
     * {@link ResponseErrorMapper}.
     */
    private String readErrorBody(EquinixResponse<?> equinixResponse) {
        if (equinixResponse.getContent() == null || equinixResponse.getContent() instanceof EmptyInputStream) {
            return null;
        }
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(equinixResponse.getContent(), StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
        catch (Exception bodyEx) {
            log.warn("Could not read error response body: {}", bodyEx.getMessage());
            return null;
        }
    }

    private static class SingleRequestParams {
        HttpRequestBase apacheRequest;
        CloseableHttpResponse apacheResponse;

        <T> void newApacheRequest(final RequestFactory httpRequestFactory, final EquinixRequest<T> equinixRequest) {
            apacheRequest = httpRequestFactory.create(equinixRequest);
        }
    }

    /**
     * Executes a request with automatic retry of transient failures per the configured
     * {@link RetryPolicy}. Retries are bounded by the policy's max attempts, gated on retryable status
     * codes / {@code IOException}s <em>and</em> — for 5xx/IO failures — on HTTP-method idempotency
     * (POST and PATCH are not retried by default, to avoid duplicate side effects; a 429 is exempt
     * from that gate because the server explicitly did not process the request), and spaced by
     * exponential backoff with full jitter (honoring {@code Retry-After}). Each retry is logged at
     * WARN. A request-body serialization failure ({@code JsonProcessingException}) is a
     * deterministic client-side bug: it is thrown immediately, never retried, and never recorded on
     * the circuit breaker.
     *
     * <p>A single correlation id (UUID) is generated per logical request and shared by all of its
     * retry attempts, sent as {@code X-Correlation-Id}. When a {@link CircuitBreaker} is configured,
     * each attempt first asks the breaker for permission (an open circuit fails fast with
     * {@link com.eqixiac.equinix.core.exception.CircuitOpenException}) and its outcome is reported
     * back: 5xx responses and transport {@code IOException}s count as failures, any completed
     * exchange below 500 counts as healthy.</p>
     *
     * @param equinixRequest the request to execute
     * @param <T> the response payload type
     * @return the successful response
     * @throws com.eqixiac.equinix.core.exception.EquinixClientException on a non-retryable failure or once retries are exhausted
     */
    public <T> EquinixResponse<T> executeWithRetries(final EquinixRequest<T> equinixRequest) throws EquinixClientException {
        final RetryPolicy policy = this.retryPolicy;
        final CircuitBreaker breaker = this.circuitBreaker;
        final HttpMethod method = equinixRequest.getHttpMethod();
        final String correlationId = UUID.randomUUID().toString();
        int attempt = 0;
        while (true) {
            if (breaker != null) {
                breaker.acquire();
            }
            try {
                // A fresh SingleRequestParams (and thus a freshly-built Apache request) is used per
                // attempt; the serialized body is a repeatable StringEntity, so the request can be
                // re-sent safely. Method-idempotency gating below prevents re-sending a POST/PATCH.
                EquinixResponse<T> equinixResponse = executeSingleRequest(equinixRequest, new SingleRequestParams(), correlationId);
                if (breaker != null) {
                    breaker.recordSuccess();
                }
                return equinixResponse;
            } catch (EquinixServiceException ese) {
                Integer status = ese.getStatusCode();
                if (breaker != null) {
                    // 5xx means the service is failing; any other completed exchange means it is up.
                    if (status != null && status >= 500) {
                        breaker.recordFailure();
                    } else {
                        breaker.recordSuccess();
                    }
                }
                if (attempt >= policy.getMaxRetries() || status == null
                        || !policy.isRetryable(status, method)) {
                    throw ese;
                }
                long backoff = policy.computeBackoffMillis(attempt, retryAfterMillis(ese));
                log.warn("Retrying {} after HTTP {} (attempt {} of {}), waiting {}ms [cid={}]",
                        method, status, attempt + 1, policy.getMaxRetries(), backoff, correlationId);
                backoffSleep(backoff);
                attempt++;
            } catch (EquinixClientException ece) {
                if (ece.getCause() instanceof JsonProcessingException) {
                    // Request-body serialization failed. JsonProcessingException extends
                    // IOException, but this is a deterministic client-side bug, not a transport
                    // failure: the service was never contacted and a re-attempt can never succeed.
                    // Throw immediately — no retry, and nothing recorded on the breaker.
                    throw ece;
                }
                boolean transportFailure = ece.getCause() instanceof IOException;
                if (breaker != null && transportFailure) {
                    breaker.recordFailure();
                }
                if (attempt >= policy.getMaxRetries() || !policy.isRetryOnIoException()
                        || !transportFailure || !policy.isRetryableMethod(method)) {
                    throw ece;
                }
                long backoff = policy.computeBackoffMillis(attempt, null);
                log.warn("Retrying {} after {} (attempt {} of {}), waiting {}ms [cid={}]",
                        method, ece.getCause().getClass().getSimpleName(), attempt + 1, policy.getMaxRetries(), backoff, correlationId);
                backoffSleep(backoff);
                attempt++;
            }
        }
    }

    /**
     * Parses a {@code Retry-After} value from the exception into millis, or {@code null} if absent /
     * unparseable. Both RFC&nbsp;7231 forms are honored: delta-seconds (e.g. {@code "30"}) and an
     * HTTP-date (e.g. {@code "Wed, 21 Oct 2026 07:28:00 GMT"}), the latter computed relative to now.
     */
    private Long retryAfterMillis(EquinixServiceException ese) {
        if (ese.getHttpHeaders() == null) {
            return null;
        }
        String value = ese.getHttpHeaders().get(RETRY_AFTER_HEADER);
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        try {
            return Long.parseLong(trimmed) * 1000L;
        } catch (NumberFormatException notDeltaSeconds) {
            try {
                ZonedDateTime retryAt = ZonedDateTime.parse(trimmed, DateTimeFormatter.RFC_1123_DATE_TIME);
                return Math.max(0L, retryAt.toInstant().toEpochMilli() - Instant.now().toEpochMilli());
            } catch (Exception notHttpDate) {
                // Neither form parsed; fall back to computed backoff.
                return null;
            }
        }
    }

    private void backoffSleep(long millis) {
        if (millis <= 0) {
            return;
        }
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new EquinixClientException("Interrupted while waiting to retry a request.", ie);
        }
    }


    private boolean isRequestSuccessful(HttpResponse response) {
        int status = response.getStatusLine().getStatusCode();
        return (status / 100 == HttpStatus.SC_OK / 100);
    }

    public Protocol getProtocol() {
        return protocol;
    }

    @Override
    public void close() throws IOException {
        httpClient.close();
    }
}
