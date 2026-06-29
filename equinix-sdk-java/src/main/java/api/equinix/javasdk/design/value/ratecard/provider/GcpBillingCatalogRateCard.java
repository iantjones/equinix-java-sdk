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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A {@link RateCard} that sources Google Cloud <em>data-egress</em> rates from the
 * <a href="https://cloud.google.com/billing/docs/reference/rest/v1/services.skus/list">Cloud Billing Catalog API</a>
 * ({@code https://cloudbilling.googleapis.com/v1/services/{service}/skus}). Unlike the
 * Azure and AWS adapters this API requires an API key, supplied at construction.
 *
 * <p>It lists the SKUs of the Compute Engine service and selects the network-egress
 * ones: {@link EgressPath#INTERNET} maps to "Network Internet Egress" SKUs and
 * {@link EgressPath#PRIVATE} to "Interconnect"/"Network Inter-Region" egress SKUs.
 * The per-GB figure is the first paid pricing tier (the free tier is skipped),
 * computed from {@code unitPrice.units + nanos}. Every rate it returns is tagged
 * {@link PriceSource#PROVIDER_API}.</p>
 *
 * <p>This is an opt-in, pluggable source intended for a
 * {@link RateCard#layered(RateCard...)} chain. It is fault-tolerant — a missing key,
 * network error, or unrecognised response yields no rate rather than an exception.
 * The SKU catalogue is fetched once (following pagination) and cached for the
 * adapter's lifetime.</p>
 */
public final class GcpBillingCatalogRateCard implements RateCard {

    /** The public Cloud Billing Catalog endpoint root. */
    public static final String DEFAULT_ENDPOINT = "https://cloudbilling.googleapis.com";

    /** Google's Compute Engine service id, under which network-egress SKUs are published. */
    public static final String COMPUTE_ENGINE_SERVICE = "6F81-5844-456A";

    private static final Currency USD = Currency.getInstance("USD");
    private static final int MAX_PAGES = 25;

    private final String endpoint;
    private final String apiKey;
    private final ProviderPricingHttpClient http;
    private final Map<String, Optional<EgressRate>> cache = new ConcurrentHashMap<>();
    private volatile List<JsonNode> skus;

    private GcpBillingCatalogRateCard(String endpoint, String apiKey) {
        this.endpoint = endpoint;
        this.apiKey = apiKey;
        this.http = new ProviderPricingHttpClient();
    }

    /**
     * Creates an adapter against the public Cloud Billing Catalog endpoint.
     *
     * @param apiKey a Google API key authorised for the Cloud Billing Catalog API
     */
    public static GcpBillingCatalogRateCard create(String apiKey) {
        return new GcpBillingCatalogRateCard(DEFAULT_ENDPOINT, apiKey);
    }

    /**
     * Creates an adapter against a custom endpoint root — for a proxy or testing.
     *
     * @param apiKey   a Google API key authorised for the Cloud Billing Catalog API
     * @param endpoint the endpoint root (e.g. {@code https://cloudbilling.googleapis.com})
     */
    public static GcpBillingCatalogRateCard create(String apiKey, String endpoint) {
        return new GcpBillingCatalogRateCard(endpoint, apiKey);
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
        if (provider != CloudProviderType.GOOGLE_CLOUD || path == null
                || apiKey == null || apiKey.isEmpty()) {
            return Optional.empty();
        }
        String key = path.name() + "|" + (region == null ? "*" : region);
        return cache.computeIfAbsent(key, k -> resolve(region, path));
    }

    @Override
    public PriceSource source() {
        return PriceSource.PROVIDER_API;
    }

    private Optional<EgressRate> resolve(String region, EgressPath path) {
        for (JsonNode sku : skus()) {
            if (!matchesPath(sku, path) || !matchesRegion(sku, region)) {
                continue;
            }
            BigDecimal perGb = firstPaidTier(sku);
            if (perGb != null) {
                return Optional.of(EgressRate.of(perGb, USD, PriceSource.PROVIDER_API)
                        .withNote("GCP Billing Catalog: " + sku.path("description").asText("egress")));
            }
        }
        return Optional.empty();
    }

    private static boolean matchesPath(JsonNode sku, EgressPath path) {
        String descriptor = (sku.path("description").asText("") + " "
                + sku.path("category").path("resourceGroup").asText("")).toLowerCase();
        boolean egress = descriptor.contains("egress");
        if (!egress) {
            return false;
        }
        if (path == EgressPath.PRIVATE) {
            return descriptor.contains("interconnect") || descriptor.contains("inter region")
                    || descriptor.contains("inter-region");
        }
        // INTERNET: an egress SKU that is not an interconnect/inter-region one.
        return descriptor.contains("internet")
                || !(descriptor.contains("interconnect") || descriptor.contains("inter region")
                        || descriptor.contains("inter-region"));
    }

    private static boolean matchesRegion(JsonNode sku, String region) {
        if (region == null || region.isEmpty()) {
            return true;
        }
        JsonNode regions = sku.path("serviceRegions");
        if (!regions.isArray()) {
            return false;
        }
        for (JsonNode r : regions) {
            String name = r.asText("");
            if (region.equals(name) || "global".equals(name)) {
                return true;
            }
        }
        return false;
    }

    /** The first paid pricing tier's per-GB USD price (units + nanos), or null. */
    private static BigDecimal firstPaidTier(JsonNode sku) {
        for (JsonNode info : sku.path("pricingInfo")) {
            JsonNode tiers = info.path("pricingExpression").path("tieredRates");
            for (JsonNode tier : tiers) {
                JsonNode unitPrice = tier.path("unitPrice");
                long units;
                try {
                    units = Long.parseLong(unitPrice.path("units").asText("0"));
                } catch (NumberFormatException e) {
                    units = 0L;
                }
                long nanos = unitPrice.path("nanos").asLong(0L);
                BigDecimal price = BigDecimal.valueOf(units)
                        .add(BigDecimal.valueOf(nanos).movePointLeft(9));
                if (price.signum() > 0) {
                    return price;
                }
            }
        }
        return null;
    }

    private List<JsonNode> skus() {
        List<JsonNode> local = skus;
        if (local == null) {
            synchronized (this) {
                local = skus;
                if (local == null) {
                    local = fetchSkus();
                    skus = local;
                }
            }
        }
        return local;
    }

    private List<JsonNode> fetchSkus() {
        List<JsonNode> collected = new ArrayList<>();
        String pageToken = null;
        for (int page = 0; page < MAX_PAGES; page++) {
            StringBuilder url = new StringBuilder(endpoint)
                    .append("/v1/services/").append(COMPUTE_ENGINE_SERVICE).append("/skus")
                    .append("?key=").append(URLEncoder.encode(apiKey, StandardCharsets.UTF_8))
                    .append("&currencyCode=USD&pageSize=5000");
            if (pageToken != null && !pageToken.isEmpty()) {
                url.append("&pageToken=").append(URLEncoder.encode(pageToken, StandardCharsets.UTF_8));
            }

            Optional<JsonNode> root = http.getJson(url.toString());
            if (root.isEmpty()) {
                break;
            }
            JsonNode skuArray = root.get().path("skus");
            if (skuArray.isArray()) {
                for (JsonNode sku : skuArray) {
                    collected.add(sku);
                }
            }
            pageToken = root.get().path("nextPageToken").asText("");
            if (pageToken.isEmpty()) {
                break;
            }
        }
        return collected;
    }
}
