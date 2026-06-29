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
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;

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
 */
final class ProviderPricingHttpClient {

    private static final Logger log = LoggerFactory.getLogger(ProviderPricingHttpClient.class);

    private final CloseableHttpClient httpClient;
    private final ObjectMapper objectMapper;

    ProviderPricingHttpClient() {
        this.httpClient = HttpClients.createDefault();
        this.objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
    }

    /** Issues a GET and parses the body as a JSON tree, or empty on any failure. */
    Optional<JsonNode> getJson(String url) {
        return getJson(url, null);
    }

    /** Issues a GET with the supplied request headers and parses the body as a JSON tree, or empty on any failure. */
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
                log.debug("Provider pricing GET {} returned HTTP {}", url, status);
                return Optional.empty();
            }
            return Optional.of(objectMapper.readTree(body));
        } catch (Exception e) {
            log.debug("Provider pricing GET {} failed; adapter will yield no price", url, e);
            return Optional.empty();
        }
    }
}
