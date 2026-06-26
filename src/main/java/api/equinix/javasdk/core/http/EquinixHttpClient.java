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

import api.equinix.javasdk.core.enums.Protocol;
import api.equinix.javasdk.core.exception.*;
import api.equinix.javasdk.core.http.request.EquinixRequest;
import api.equinix.javasdk.core.http.request.RequestFactory;
import api.equinix.javasdk.core.http.response.EquinixResponse;
import api.equinix.javasdk.core.internal.Constants;
import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.io.EmptyInputStream;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>EquinixHttpClient class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class EquinixHttpClient implements Closeable {

    private static final Logger logger = LoggerFactory.getLogger(EquinixHttpClient.class);

    /** Maximum time (ms) to wait for a connection to be established. */
    private static final int DEFAULT_CONNECT_TIMEOUT_MS = 10_000;
    /** Maximum time (ms) to wait for data between packets once connected. */
    private static final int DEFAULT_SOCKET_TIMEOUT_MS = 60_000;
    /** Maximum time (ms) to wait for a connection lease from the pool. */
    private static final int DEFAULT_CONNECTION_REQUEST_TIMEOUT_MS = 5_000;
    /** Maximum total pooled connections across all routes. */
    private static final int DEFAULT_MAX_TOTAL_CONNECTIONS = 50;
    /** Maximum pooled connections per route (the Equinix API host). */
    private static final int DEFAULT_MAX_CONNECTIONS_PER_ROUTE = 20;

    private final CloseableHttpClient httpClient;
    private final RequestFactory requestFactory = new RequestFactory();
    private final Protocol protocol = Protocol.HTTPS;
    public Boolean outputRequestJson = true;

    /** Automatic retry behavior for transient failures; defaults to {@link RetryPolicy#defaultPolicy()}. */
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
     * <p>Constructor for EquinixHttpClient.</p>
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
     * <p>Setter for the field <code>outputRequestJson</code>.</p>
     *
     * @param outputRequestJson a {@link java.lang.Boolean} object.
     */
    public void setOutputRequestJson(Boolean outputRequestJson) {
        this.outputRequestJson = outputRequestJson;
    }

    /**
     * <p>executeSingleRequest.</p>
     *
     * @param equinixRequest a {@link api.equinix.javasdk.core.http.request.EquinixRequest} object.
     * @param singleRequestParams a {@link api.equinix.javasdk.core.http.EquinixHttpClient.SingleRequestParams} object.
     * @param <T> a T object.
     * @return a {@link api.equinix.javasdk.core.http.response.EquinixResponse} object.
     * @throws api.equinix.javasdk.core.exception.EquinixClientException if any.
     */
    public <T> EquinixResponse<T> executeSingleRequest(EquinixRequest<T> equinixRequest, SingleRequestParams singleRequestParams)
            throws EquinixClientException {

        singleRequestParams.newApacheRequest(requestFactory, equinixRequest);

        logger.info(equinixRequest.getHttpMethod() + " " + singleRequestParams.apacheRequest.getURI());

        try {
            if(this.outputRequestJson) {
                if(equinixRequest.getContent() != null) {
                    String requestJson = new BufferedReader(
                            new InputStreamReader(equinixRequest.getContent(), StandardCharsets.UTF_8)).lines()
                            .collect(Collectors.joining());
                    logger.info(requestJson);
                }
            }

            singleRequestParams.apacheResponse = httpClient.execute(singleRequestParams.apacheRequest);

            EquinixResponse<T> equinixResponse = new EquinixResponse<>(equinixRequest, singleRequestParams.apacheRequest, singleRequestParams.apacheResponse);
            equinixResponse.setEquinixRequest(equinixRequest);

            logger.info("Status: " + equinixResponse.getStatusCode() + " " + equinixResponse.getStatusText());

            if(singleRequestParams.apacheResponse.getEntity() != null) {
                equinixResponse.setEntity(singleRequestParams.apacheResponse.getEntity());
                equinixResponse.setContent(equinixResponse.getEntity().getContent());
            }

            if(!isRequestSuccessful(singleRequestParams.apacheResponse)) {
                EquinixServiceException ese = createServiceException(equinixResponse.getStatusCode());
                ese.setStatusCode(equinixResponse.getStatusCode());
                ese.setPath(singleRequestParams.apacheRequest.getURI().toString());

                org.apache.http.Header retryAfterHeader = singleRequestParams.apacheResponse.getFirstHeader("Retry-After");
                if (retryAfterHeader != null && retryAfterHeader.getValue() != null) {
                    java.util.Map<String, String> responseHeaders = new java.util.HashMap<>();
                    responseHeaders.put("Retry-After", retryAfterHeader.getValue());
                    ese.setHttpHeaders(responseHeaders);
                }

                if(equinixResponse.getContent() != null && !(equinixResponse.getContent() instanceof EmptyInputStream)) {
                    try {
                        String errorBody = new BufferedReader(
                                new InputStreamReader(equinixResponse.getContent(), StandardCharsets.UTF_8)).lines()
                                .collect(Collectors.joining("\n"));

                        if(errorBody != null && !errorBody.isBlank()) {
                            try {
                                ArrayList<ExceptionDetail> exceptionDetails = Constants.objectMapper.readValue(
                                        errorBody, new TypeReference<ArrayList<ExceptionDetail>>(){});
                                ese.setExceptionDetails(exceptionDetails);
                            }
                            catch (Exception arrayEx) {
                                try {
                                    ExceptionDetail singleDetail = Constants.objectMapper.readValue(
                                            errorBody, new TypeReference<ExceptionDetail>(){});
                                    ArrayList<ExceptionDetail> details = new ArrayList<>();
                                    details.add(singleDetail);
                                    ese.setExceptionDetails(details);
                                }
                                catch (Exception singleEx) {
                                    ExceptionDetail rawDetail = new ExceptionDetail();
                                    ArrayList<ExceptionDetail> details = new ArrayList<>();
                                    details.add(rawDetail);
                                    ese.setExceptionDetails(details);
                                    logger.warn("Could not parse error response body: " + errorBody);
                                }
                            }
                        }
                    }
                    catch (Exception bodyEx) {
                        logger.warn("Could not read error response body: " + bodyEx.getMessage());
                    }
                }

                throw ese;
            }
            else {
                return equinixResponse;
            }
        }
        catch (IOException ioe) {
            throw new EquinixClientException(ioe);
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
     * <p>executeHelper.</p>
     *
     * @param equinixRequest a {@link api.equinix.javasdk.core.http.request.EquinixRequest} object.
     * @param <T> a T object.
     * @return a {@link api.equinix.javasdk.core.http.response.EquinixResponse} object.
     * @throws api.equinix.javasdk.core.exception.EquinixClientException if any.
     */
    public <T> EquinixResponse<T> executeHelper(final EquinixRequest<T> equinixRequest) throws EquinixClientException {
        final RetryPolicy policy = this.retryPolicy;
        int attempt = 0;
        while (true) {
            try {
                // A fresh SingleRequestParams (and thus a freshly-built Apache request) is used per
                // attempt; the serialized body is a repeatable StringEntity, so the request can be
                // re-sent safely.
                return executeSingleRequest(equinixRequest, new SingleRequestParams());
            } catch (EquinixServiceException ese) {
                Integer status = ese.getStatusCode();
                if (attempt >= policy.getMaxRetries() || status == null || !policy.isRetryableStatus(status)) {
                    throw ese;
                }
                backoffSleep(policy.computeBackoffMillis(attempt, retryAfterMillis(ese)));
                attempt++;
            } catch (EquinixClientException ece) {
                if (attempt >= policy.getMaxRetries() || !policy.isRetryOnIoException()
                        || !(ece.getCause() instanceof IOException)) {
                    throw ece;
                }
                backoffSleep(policy.computeBackoffMillis(attempt, null));
                attempt++;
            }
        }
    }

    /** Parses a {@code Retry-After} value (delta-seconds form) from the exception into millis, or null. */
    private Long retryAfterMillis(EquinixServiceException ese) {
        if (ese.getHttpHeaders() == null) {
            return null;
        }
        String value = ese.getHttpHeaders().get("Retry-After");
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(value.trim()) * 1000L;
        } catch (NumberFormatException e) {
            // HTTP-date form is not honored here; fall back to computed backoff.
            return null;
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

    private EquinixServiceException createServiceException(int statusCode) {
        switch (statusCode) {
            case 401:
                return new EquinixAuthenticationException("Authentication failed (HTTP 401).");
            case 403:
                return new EquinixAuthorizationException("Authorization denied (HTTP 403).");
            case 404:
                return new EquinixNotFoundException("Resource not found (HTTP 404).");
            case 409:
                return new EquinixConflictException("Resource conflict (HTTP 409).");
            case 429:
                return new EquinixRateLimitException("Rate limit exceeded (HTTP 429).");
            default:
                if (statusCode >= 500) {
                    return new EquinixServerException("Server error (HTTP " + statusCode + ").");
                }
                return new EquinixServiceException("Error returned by Equinix API (HTTP " + statusCode + ").");
        }
    }

    private boolean isRequestSuccessful(HttpResponse response) {
        int status = response.getStatusLine().getStatusCode();
        return (status / 100 == HttpStatus.SC_OK / 100);
    }

    /**
     * <p>Getter for the field <code>protocol</code>.</p>
     *
     * @return a {@link api.equinix.javasdk.core.enums.Protocol} object.
     */
    public Protocol getProtocol() {
        return protocol;
    }

    @Override
    public void close() throws IOException {
        httpClient.close();
    }
}
