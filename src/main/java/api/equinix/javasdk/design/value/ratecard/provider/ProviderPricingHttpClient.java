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

package api.equinix.javasdk.design.value.ratecard.provider;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.util.EntityUtils;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * A minimal, fault-tolerant HTTP/JSON GET client shared by the public cloud
 * pricing adapters (Azure Retail Prices, AWS Price List, GCP Cloud Billing
 * Catalog). It mirrors the transport choices of the rest of the SDK — Apache
 * HttpClient plus Jackson — and the lightweight style of the PeeringDB client
 * in the peering-intelligence module.
 *
 * <p>Every call is best-effort: any non-200 status, transport failure, or parse
 * error yields {@link Optional#empty()} rather than throwing, so a pricing
 * adapter built on it degrades gracefully (its rate card simply produces no
 * price and a layered card falls back to another source).</p>
 *
 * <p>Graceful degradation only works if a call can actually <em>fail</em>: every
 * request runs under hard connect / socket / connection-request timeouts, so an
 * unresponsive pricing endpoint surfaces as an empty result within seconds rather
 * than hanging the caller forever. The defaults keep a single worst-case GET
 * comfortably inside the MCP layer's per-lookup hard timeout
 * ({@code EQUINIX_MCP_PRICING_TIMEOUT_MS}, default 12&nbsp;s), which wraps whole
 * rate-card lookups from the outside.</p>
 *
 * <p>Some pricing endpoints (GCP Billing Catalog) carry an API key as a query-string
 * parameter; log lines never echo a raw URL — credential-bearing query parameters are
 * {@linkplain #redactCredentials(String) redacted} first.</p>
 */
@Slf4j
final class ProviderPricingHttpClient {

    /** Maximum time to establish a TCP connection to a pricing endpoint. */
    static final int DEFAULT_CONNECT_TIMEOUT_MS = 5_000;

    /** Maximum inactivity between response packets while reading a pricing payload. */
    static final int DEFAULT_SOCKET_TIMEOUT_MS = 10_000;

    /** Maximum wait to lease a connection from the pool. */
    static final int DEFAULT_CONNECTION_REQUEST_TIMEOUT_MS = 2_000;

    private static final int MAX_POOLED_CONNECTIONS = 8;
    private static final int MAX_POOLED_CONNECTIONS_PER_ROUTE = 4;

    /**
     * Query-string parameter names (lower-case) whose values are credentials and must never
     * reach a log line. Covers the Google-style {@code key}, generic API-key spellings, and
     * common token/signature/secret parameters.
     */
    private static final Set<String> CREDENTIAL_QUERY_PARAMS = Set.of(
            "key", "api-key", "api_key", "apikey",
            "token", "access_token", "auth", "authorization",
            "sig", "signature", "secret", "client_secret", "password");

    private final CloseableHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final RequestConfig requestConfig;

    ProviderPricingHttpClient() {
        this(DEFAULT_CONNECT_TIMEOUT_MS, DEFAULT_SOCKET_TIMEOUT_MS, DEFAULT_CONNECTION_REQUEST_TIMEOUT_MS);
    }

    /**
     * Seam for tests and callers that need non-default timeouts.
     *
     * @param connectTimeoutMs           maximum time to establish the TCP connection
     * @param socketTimeoutMs            maximum inactivity between response packets
     * @param connectionRequestTimeoutMs maximum wait to lease a pooled connection
     */
    ProviderPricingHttpClient(int connectTimeoutMs, int socketTimeoutMs, int connectionRequestTimeoutMs) {
        this.requestConfig = RequestConfig.custom()
                .setConnectTimeout(connectTimeoutMs)
                .setSocketTimeout(socketTimeoutMs)
                .setConnectionRequestTimeout(connectionRequestTimeoutMs)
                .build();
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(MAX_POOLED_CONNECTIONS);
        connectionManager.setDefaultMaxPerRoute(MAX_POOLED_CONNECTIONS_PER_ROUTE);
        this.httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .setConnectionManager(connectionManager)
                .build();
        this.objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
    }

    /**
     * @return the request configuration every GET runs under (exposed so tests can assert the
     *         timeout guarantees without opening a socket)
     */
    RequestConfig requestConfig() {
        return requestConfig;
    }

    Optional<JsonNode> getJson(String url) {
        return getJson(url, null);
    }

    Optional<JsonNode> getJson(String url, Map<String, String> headers) {
        HttpGet request = new HttpGet(url);
        request.setHeader("Accept", "application/json");
        request.setHeader("User-Agent", "equinix-java-sdk/value-realization");
        if (headers != null) {
            headers.forEach(request::setHeader);
        }

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            int status = response.getStatusLine().getStatusCode();
            String body = response.getEntity() == null ? "" : EntityUtils.toString(response.getEntity());
            if (status != 200) {
                log.debug("Provider pricing GET {} returned HTTP {}", redactCredentials(url), status);
                return Optional.empty();
            }
            return Optional.of(objectMapper.readTree(body));
        } catch (Exception e) {
            log.debug("Provider pricing GET {} failed; adapter will yield no price", redactCredentials(url), e);
            return Optional.empty();
        }
    }

    /**
     * Returns the URL with the value of every credential-bearing query parameter (API keys,
     * tokens, signatures, …) replaced by {@code REDACTED}, so the URL is safe to log. URLs
     * without a query string are returned unchanged.
     *
     * @param url the request URL (may be {@code null})
     * @return the redacted URL, or {@code null} when {@code url} was {@code null}
     */
    static String redactCredentials(String url) {
        if (url == null) {
            return null;
        }
        int queryStart = url.indexOf('?');
        if (queryStart < 0) {
            return url;
        }
        String query = url.substring(queryStart + 1);
        String fragment = "";
        int fragmentStart = query.indexOf('#');
        if (fragmentStart >= 0) {
            fragment = query.substring(fragmentStart);
            query = query.substring(0, fragmentStart);
        }

        StringBuilder redacted = new StringBuilder(url.length())
                .append(url, 0, queryStart + 1);
        String[] pairs = query.split("&", -1);
        for (int i = 0; i < pairs.length; i++) {
            if (i > 0) {
                redacted.append('&');
            }
            String pair = pairs[i];
            int eq = pair.indexOf('=');
            if (eq > 0 && CREDENTIAL_QUERY_PARAMS.contains(pair.substring(0, eq).toLowerCase(Locale.ROOT))) {
                redacted.append(pair, 0, eq + 1).append("REDACTED");
            } else {
                redacted.append(pair);
            }
        }
        return redacted.append(fragment).toString();
    }
}
