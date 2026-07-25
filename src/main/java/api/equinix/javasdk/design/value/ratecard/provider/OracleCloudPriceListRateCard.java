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

import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.design.value.ratecard.EgressPath;
import api.equinix.javasdk.design.value.ratecard.EgressRate;
import api.equinix.javasdk.design.value.ratecard.PriceQuote;
import api.equinix.javasdk.design.value.ratecard.PriceSource;
import api.equinix.javasdk.design.value.ratecard.RateCard;
import api.equinix.javasdk.design.value.ratecard.Term;
import api.equinix.javasdk.fabric.enums.ConnectionType;
import api.equinix.javasdk.fabric.model.implementation.cloud.CloudProviderType;
import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * A {@link RateCard} that sources Oracle Cloud (OCI) <em>internet data-egress</em> rates from the
 * public <a href="https://www.oracle.com/cloud/price-list/">OCI Price List API</a>
 * ({@code https://apexapps.oracle.com/pls/apex/cetools/api/v1/products/}). The API is
 * unauthenticated, so this adapter needs no credentials.
 *
 * <p>OCI prices outbound data transfer by <em>source geography</em> ("Originating in North America,
 * Europe, and UK" / "APAC, Japan, and South America" / "Middle East and Africa"), not by individual
 * region, so this adapter maps the supplied region to a geography and selects the matching
 * "Outbound Data Transfer" SKU. The per-GB figure is the first paid tier (the generous free
 * allowance, priced at 0, is skipped). It prices {@link EgressPath#INTERNET} for
 * {@link CloudProviderType#ORACLE_CLOUD}; {@link EgressPath#PRIVATE} (FastConnect) is port-based
 * rather than a per-GB egress SKU, so a {@code PRIVATE} lookup returns empty and a layered card
 * falls back to another source. Every rate it returns is tagged {@link PriceSource#PROVIDER_API}.</p>
 *
 * <p>The price list is fetched once and cached for the adapter's lifetime; the adapter is
 * fault-tolerant — any fetch or parse failure yields no rate rather than an exception.</p>
 */
public final class OracleCloudPriceListRateCard implements RateCard {

    public static final String DEFAULT_ENDPOINT =
            "https://apexapps.oracle.com/pls/apex/cetools/api/v1/products/";

    private static final Currency USD = Currency.getInstance("USD");

    /**
     * Safety bound on ORDS "next"-link hops. The cetools products endpoint returns its catalogue
     * in a single response today; this cap only matters if it ever switches to paged collections.
     */
    private static final int MAX_PAGES = 100;

    private final String endpoint;
    private final ProviderPricingHttpClient http;
    private final Map<String, Optional<EgressRate>> cache = new ConcurrentHashMap<>();
    private volatile List<JsonNode> priceItems;
    private volatile boolean fetched;

    private OracleCloudPriceListRateCard(String endpoint) {
        this.endpoint = endpoint;
        this.http = new ProviderPricingHttpClient();
    }

    public static OracleCloudPriceListRateCard create() {
        return new OracleCloudPriceListRateCard(DEFAULT_ENDPOINT);
    }

    /**
     * Creates an adapter against a custom endpoint — for a proxy or testing.
     *
     * @param endpoint the full OCI Price List products URL
     */
    public static OracleCloudPriceListRateCard create(String endpoint) {
        return new OracleCloudPriceListRateCard(endpoint);
    }

    @Override
    public Optional<PriceQuote> connection(ConnectionType type, int bandwidthMbps, MetroCode metro, Term term) {
        return Optional.empty();
    }

    @Override
    public Optional<PriceQuote> cloudRouter(String packageCode, MetroCode metro, Term term) {
        return Optional.empty();
    }

    @Override
    public Optional<EgressRate> egress(CloudProviderType provider, String region, EgressPath path, Term term) {
        if (provider != CloudProviderType.ORACLE_CLOUD || path != EgressPath.INTERNET) {
            return Optional.empty();
        }
        return cache.computeIfAbsent(region == null ? "*" : region, this::resolveInternetEgress);
    }

    @Override
    public PriceSource source() {
        return PriceSource.PROVIDER_API;
    }

    private Optional<EgressRate> resolveInternetEgress(String region) {
        String geography = geographyFor(region);
        for (JsonNode item : priceItems()) {
            String name = item.path("displayName").asText("");
            String lower = name.toLowerCase();
            // The generic OCI egress SKUs (not service-specific ones like "MySQL Database - ...").
            if (!lower.startsWith("outbound data transfer") || !lower.contains(geography)) {
                continue;
            }
            BigDecimal perGb = firstPaidPrice(item);
            if (perGb != null) {
                return Optional.of(EgressRate.of(perGb, USD, PriceSource.PROVIDER_API)
                        .withNote("OCI Price List: " + name));
            }
        }
        return Optional.empty();
    }

    private static String geographyFor(String region) {
        if (region == null || region.isEmpty()) {
            return "north america"; // default to the most common geography
        }
        String r = region.toLowerCase();
        if (r.startsWith("ap-") || r.startsWith("sa-") || r.contains("japan") || r.contains("apac")) {
            return "apac";
        }
        if (r.startsWith("me-") || r.startsWith("af-") || r.contains("middle east") || r.contains("africa")) {
            return "middle east";
        }
        return "north america"; // us-, ca-, eu-, uk-, and anything else
    }

    private static BigDecimal firstPaidPrice(JsonNode item) {
        for (JsonNode loc : item.path("currencyCodeLocalizations")) {
            if (loc.has("currencyCode") && !"USD".equalsIgnoreCase(loc.path("currencyCode").asText())) {
                continue;
            }
            for (JsonNode price : loc.path("prices")) {
                try {
                    BigDecimal value = new BigDecimal(price.path("value").asText("0"));
                    if (value.signum() > 0) {
                        return value;
                    }
                } catch (NumberFormatException ignored) {
                    // skip non-numeric price entries
                }
            }
        }
        return null;
    }

    private List<JsonNode> priceItems() {
        if (!fetched) {
            synchronized (this) {
                if (!fetched) {
                    priceItems = fetchAllItems();
                    fetched = true;
                }
            }
        }
        return priceItems;
    }

    /**
     * Fetches the price-list products, following ORDS pagination when the endpoint returns it.
     * The cetools handler currently serves the whole catalogue in one response (no {@code hasMore}
     * / {@code links.next}), in which case this is a single GET. If it ever paginates, every page
     * is accumulated before the caller matches — so a "not found" is only ever reported after the
     * continuation has been exhausted, never off a truncated first page.
     */
    private List<JsonNode> fetchAllItems() {
        List<JsonNode> collected = new ArrayList<>();
        String url = endpoint + (endpoint.contains("?") ? "&" : "?") + "currencyCode=USD";
        for (int page = 0; url != null && !url.isEmpty() && page < MAX_PAGES; page++) {
            Optional<JsonNode> body = http.getJson(url);
            if (body.isEmpty()) {
                // Fetch failure: stop paginating and match over whatever pages we did retrieve.
                // A found SKU is a real datum; a miss simply yields no rate (a layered card falls
                // back), never a fabricated number.
                break;
            }
            JsonNode root = body.get();
            JsonNode items = root.path("items");
            if (items.isArray()) {
                for (JsonNode item : items) {
                    collected.add(item);
                }
            }
            url = nextLink(root);
        }
        return collected;
    }

    /**
     * The ORDS continuation URL, or {@code null} when the page is the last. A {@code "next"} link
     * is present only while more rows remain; an explicit {@code hasMore:false} is a hard stop.
     * When neither is present the endpoint returned everything in one page.
     */
    private static String nextLink(JsonNode root) {
        JsonNode hasMore = root.get("hasMore");
        if (hasMore != null && hasMore.isBoolean() && !hasMore.booleanValue()) {
            return null;
        }
        for (JsonNode link : root.path("links")) {
            if ("next".equalsIgnoreCase(link.path("rel").asText(""))) {
                String href = link.path("href").asText("");
                if (!href.isEmpty()) {
                    return href;
                }
            }
        }
        return null;
    }
}
