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
import java.util.Currency;
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

    /** The public OCI Price List products endpoint. */
    public static final String DEFAULT_ENDPOINT =
            "https://apexapps.oracle.com/pls/apex/cetools/api/v1/products/";

    private static final Currency USD = Currency.getInstance("USD");

    private final String endpoint;
    private final ProviderPricingHttpClient http;
    private final Map<String, Optional<EgressRate>> cache = new ConcurrentHashMap<>();
    private volatile JsonNode priceList;
    private volatile boolean fetched;

    private OracleCloudPriceListRateCard(String endpoint) {
        this.endpoint = endpoint;
        this.http = new ProviderPricingHttpClient();
    }

    /** Creates an adapter against the public OCI Price List endpoint. */
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
        JsonNode root = priceList();
        if (root == null) {
            return Optional.empty();
        }
        JsonNode items = root.path("items");
        if (!items.isArray()) {
            return Optional.empty();
        }

        String geography = geographyFor(region);
        for (JsonNode item : items) {
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

    /** Maps an OCI region (or null) to the source geography used in the egress SKU names. */
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

    /** The first positive per-GB price across an item's USD localization, or null. */
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

    private JsonNode priceList() {
        if (!fetched) {
            synchronized (this) {
                if (!fetched) {
                    priceList = http.getJson(endpoint + "?currencyCode=USD").orElse(null);
                    fetched = true;
                }
            }
        }
        return priceList;
    }
}
