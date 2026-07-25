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
import java.math.RoundingMode;
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
 * The per-GiB figure is the first paid pricing tier (the free tier is skipped),
 * computed from {@code unitPrice.units + nanos}. Every rate it returns is tagged
 * {@link PriceSource#PROVIDER_API}.</p>
 *
 * <p>Internet-egress selection is <em>deterministic</em>: many egress SKUs contain the word
 * "egress", so a match must carry the "internet" marker (not merely "not-interconnect"), and when
 * several qualify the adapter prefers the representative base rate — the standard, worldwide meter
 * over the pricier Premium-Tier and destination-qualified (China/Australia) variants — rather than
 * whichever happened to appear first in the catalogue.</p>
 *
 * <p>GCP meters egress <em>per gibibyte</em> ({@code GiBy}), but the savings engine multiplies the
 * rate by a <em>decimal</em>-gigabyte volume (1&nbsp;TB = 1000&nbsp;GB). To keep the two consistent
 * the adapter converts $/GiB&nbsp;&rarr;&nbsp;$/GB by dividing by 1.073741824 (1&nbsp;GiB = 1.073741824&nbsp;GB),
 * yielding a slightly lower per-GB number; the returned note records the original per-GiB figure.</p>
 *
 * <p>This is an opt-in, pluggable source intended for a
 * {@link RateCard#layered(RateCard...)} chain. It is fault-tolerant — a missing key,
 * network error, or unrecognised response yields no rate rather than an exception.
 * The SKU catalogue is fetched once (following pagination) and cached for the
 * adapter's lifetime.</p>
 */
public final class GcpBillingCatalogRateCard implements RateCard {

    public static final String DEFAULT_ENDPOINT = "https://cloudbilling.googleapis.com";

    public static final String COMPUTE_ENGINE_SERVICE = "6F81-5844-456A";

    private static final Currency USD = Currency.getInstance("USD");
    private static final int MAX_PAGES = 25;

    /** 1 GiB = 1.073741824 GB (decimal). GCP prices per GiB; the engine costs per decimal GB. */
    private static final BigDecimal GIB_PER_GB = new BigDecimal("1.073741824");

    /** Decimal places retained when converting the per-GiB rate to a per-GB rate. */
    private static final int PRICE_SCALE = 10;

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
        // Deterministic pick across ALL matching SKUs — never "the first one in catalogue order".
        // Higher representativeness score wins; ties break to the lower per-GiB rate, then to the
        // lexicographically-lower skuId, so the choice is stable regardless of catalogue ordering.
        JsonNode chosen = null;
        BigDecimal chosenPerGib = null;
        int chosenScore = Integer.MIN_VALUE;
        for (JsonNode sku : skus()) {
            if (!matchesPath(sku, path) || !matchesRegion(sku, region)) {
                continue;
            }
            BigDecimal perGib = firstPaidTier(sku);
            if (perGib == null) {
                continue;
            }
            int score = representativeScore(sku, path);
            if (chosen == null
                    || score > chosenScore
                    || (score == chosenScore && perGib.compareTo(chosenPerGib) < 0)
                    || (score == chosenScore && perGib.compareTo(chosenPerGib) == 0
                            && skuId(sku).compareTo(skuId(chosen)) < 0)) {
                chosen = sku;
                chosenPerGib = perGib;
                chosenScore = score;
            }
        }
        if (chosen == null) {
            return Optional.empty();
        }
        // $/GiB → $/GB: GCP meters per gibibyte, the engine costs per decimal gigabyte.
        BigDecimal perGb = chosenPerGib.divide(GIB_PER_GB, PRICE_SCALE, RoundingMode.HALF_UP);
        return Optional.of(EgressRate.of(perGb, USD, PriceSource.PROVIDER_API)
                .withNote("GCP Billing Catalog: " + chosen.path("description").asText("egress")
                        + " (" + chosenPerGib.toPlainString() + "/GiB ÷ 1.073741824 = per GB)"));
    }

    private static boolean matchesPath(JsonNode sku, EgressPath path) {
        String descriptor = (sku.path("description").asText("") + " "
                + sku.path("category").path("resourceGroup").asText("")).toLowerCase();
        if (!descriptor.contains("egress")) {
            return false;
        }
        boolean interconnect = descriptor.contains("interconnect")
                || descriptor.contains("inter region") || descriptor.contains("inter-region");
        if (path == EgressPath.PRIVATE) {
            return interconnect;
        }
        // INTERNET: an internet-egress SKU specifically. Requiring the "internet" marker (rather
        // than "any egress that isn't interconnect") avoids classifying, e.g., an inter-zone or
        // service-specific egress SKU as internet egress.
        return descriptor.contains("internet") && !interconnect;
    }

    /**
     * Ranks how representative a matching SKU is of the headline egress rate, so a deterministic
     * winner can be chosen. Only meaningful for {@link EgressPath#INTERNET}, where several variants
     * (Premium-Tier, China/Australia destinations) coexist with the standard worldwide meter; the
     * standard one scores highest. {@link EgressPath#PRIVATE} SKUs all score equally (0) — their
     * winner is decided by the price/skuId tie-break.
     */
    private static int representativeScore(JsonNode sku, EgressPath path) {
        if (path != EgressPath.INTERNET) {
            return 0;
        }
        String desc = sku.path("description").asText("").toLowerCase();
        int score = 0;
        // Prefer the base internet-egress rate over the pricier Premium-Tier variant.
        if (!desc.contains("premium")) {
            score += 4;
        }
        // Prefer the general/worldwide rate over destination-qualified SKUs, which are billed at a
        // higher, non-representative rate.
        if (!desc.contains("china") && !desc.contains("australia")) {
            score += 2;
        }
        return score;
    }

    private static String skuId(JsonNode sku) {
        return sku.path("skuId").asText(sku.path("description").asText(""));
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
