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

package api.equinix.javasdk.core.http;

import api.equinix.javasdk.core.enums.HttpMethod;
import api.equinix.javasdk.core.enums.Protocol;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.core.http.request.EquinixRequest;
import api.equinix.javasdk.core.http.request.RequestFactory;
import api.equinix.javasdk.core.http.response.EquinixResponse;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
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
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The SDK's pooled, timeout-bounded HTTP client. Executes a single {@link EquinixRequest} against
 * the Equinix API with automatic, policy-driven retry of transient failures (see {@link RetryPolicy}),
 * mapping error responses to typed exceptions via {@link ResponseErrorMapper}. One instance is shared
 * across calls and is safe to reuse; {@link #close()} shuts down the owned connection pool.
 *
 * @author ianjones
 */
public class EquinixHttpClient implements Closeable {

    private static final Logger logger = LoggerFactory.getLogger(EquinixHttpClient.class);

    private static final String RETRY_AFTER_HEADER = "Retry-After";

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
     * Overrides the retry policy for transient failures.
     *
     * @param retryPolicy the policy to apply; {@link RetryPolicy#none()} disables retries
     */
    public void setRetryPolicy(RetryPolicy retryPolicy) {
        this.retryPolicy = (retryPolicy != null) ? retryPolicy : RetryPolicy.none();
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
     *
     * @throws api.equinix.javasdk.core.exception.EquinixClientException if any.
     */
    public <T> EquinixResponse<T> executeSingleRequest(EquinixRequest<T> equinixRequest, SingleRequestParams singleRequestParams)
            throws EquinixClientException {

        singleRequestParams.newApacheRequest(requestFactory, equinixRequest);

        logger.info("{} {}", equinixRequest.getHttpMethod(), singleRequestParams.apacheRequest.getURI());

        try {
            logRequestBody(equinixRequest);

            singleRequestParams.apacheResponse = httpClient.execute(singleRequestParams.apacheRequest);

            EquinixResponse<T> equinixResponse = new EquinixResponse<>(equinixRequest, singleRequestParams.apacheRequest, singleRequestParams.apacheResponse);
            equinixResponse.setEquinixRequest(equinixRequest);

            logger.info("Status: {} {}", equinixResponse.getStatusCode(), equinixResponse.getStatusText());

            if(singleRequestParams.apacheResponse.getEntity() != null) {
                equinixResponse.setEntity(singleRequestParams.apacheResponse.getEntity());
                equinixResponse.setContent(equinixResponse.getEntity().getContent());
            }

            if(!isRequestSuccessful(singleRequestParams.apacheResponse)) {
                String path = singleRequestParams.apacheRequest.getURI().toString();

                Map<String, String> responseHeaders = null;
                Header retryAfterHeader = singleRequestParams.apacheResponse.getFirstHeader(RETRY_AFTER_HEADER);
                if (retryAfterHeader != null && retryAfterHeader.getValue() != null) {
                    responseHeaders = new HashMap<>();
                    responseHeaders.put(RETRY_AFTER_HEADER, retryAfterHeader.getValue());
                }

                throw ResponseErrorMapper.toException(
                        equinixResponse.getStatusCode(), path, responseHeaders, readErrorBody(equinixResponse));
            }
            else {
                return equinixResponse;
            }
        }
        catch (IOException ioe) {
            throw new EquinixClientException(ioe);
        }
    }

    /**
     * Logs the request body at DEBUG when enabled. Reads from the request's repeatable
     * {@link org.apache.http.HttpEntity} (not the one-shot content stream), so it is correct on every
     * retry attempt and never consumes the bytes that are actually sent. A logging failure is swallowed.
     */
    private void logRequestBody(EquinixRequest<?> equinixRequest) {
        if (!this.outputRequestJson || !logger.isDebugEnabled() || equinixRequest.getHttpEntity() == null) {
            return;
        }
        try {
            logger.debug("Request body: {}", EntityUtils.toString(equinixRequest.getHttpEntity()));
        }
        catch (IOException bodyLogEx) {
            logger.debug("Could not read request body for logging: {}", bodyLogEx.getMessage());
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
        try {
            return new BufferedReader(new InputStreamReader(equinixResponse.getContent(), StandardCharsets.UTF_8))
                    .lines().collect(Collectors.joining("\n"));
        }
        catch (Exception bodyEx) {
            logger.warn("Could not read error response body: {}", bodyEx.getMessage());
            return null;
        }
    }

    private static class SingleRequestParams {
        HttpRequestBase apacheRequest;
        HttpResponse apacheResponse;

        <T> void newApacheRequest(final RequestFactory httpRequestFactory, final EquinixRequest<T> equinixRequest) {
            apacheRequest = httpRequestFactory.create(equinixRequest);
        }
    }

    /**
     * Executes a request with automatic retry of transient failures per the configured
     * {@link RetryPolicy}. Retries are bounded by the policy's max attempts, gated on retryable status
     * codes / {@code IOException}s <em>and</em> on HTTP-method idempotency (POST is not retried by
     * default, to avoid duplicate creates), and spaced by exponential backoff with full jitter
     * (honoring {@code Retry-After}). Each retry is logged at WARN.
     *
     * @param equinixRequest the request to execute
     * @param <T> the response payload type
     * @return the successful response
     * @throws api.equinix.javasdk.core.exception.EquinixClientException on a non-retryable failure or once retries are exhausted
     */
    public <T> EquinixResponse<T> executeWithRetries(final EquinixRequest<T> equinixRequest) throws EquinixClientException {
        final RetryPolicy policy = this.retryPolicy;
        final HttpMethod method = equinixRequest.getHttpMethod();
        int attempt = 0;
        while (true) {
            try {
                // A fresh SingleRequestParams (and thus a freshly-built Apache request) is used per
                // attempt; the serialized body is a repeatable StringEntity, so the request can be
                // re-sent safely. Method-idempotency gating below prevents re-sending a POST.
                return executeSingleRequest(equinixRequest, new SingleRequestParams());
            } catch (EquinixServiceException ese) {
                Integer status = ese.getStatusCode();
                if (attempt >= policy.getMaxRetries() || status == null
                        || !policy.isRetryableStatus(status) || !policy.isRetryableMethod(method)) {
                    throw ese;
                }
                long backoff = policy.computeBackoffMillis(attempt, retryAfterMillis(ese));
                logger.warn("Retrying {} after HTTP {} (attempt {} of {}), waiting {}ms",
                        method, status, attempt + 1, policy.getMaxRetries(), backoff);
                backoffSleep(backoff);
                attempt++;
            } catch (EquinixClientException ece) {
                if (attempt >= policy.getMaxRetries() || !policy.isRetryOnIoException()
                        || !(ece.getCause() instanceof IOException) || !policy.isRetryableMethod(method)) {
                    throw ece;
                }
                long backoff = policy.computeBackoffMillis(attempt, null);
                logger.warn("Retrying {} after {} (attempt {} of {}), waiting {}ms",
                        method, ece.getCause().getClass().getSimpleName(), attempt + 1, policy.getMaxRetries(), backoff);
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
