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

package com.eqixiac.equinix.design.value.ratecard.provider;

import com.eqixiac.equinix.core.enums.MetroCode;
import com.eqixiac.equinix.design.value.ratecard.EgressPath;
import com.eqixiac.equinix.design.value.ratecard.EgressRate;
import com.eqixiac.equinix.design.value.ratecard.PriceQuote;
import com.eqixiac.equinix.design.value.ratecard.PriceSource;
import com.eqixiac.equinix.design.value.ratecard.RateCard;
import com.eqixiac.equinix.design.value.ratecard.Term;
import com.eqixiac.equinix.fabric.enums.ConnectionType;
import com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType;
import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A {@link RateCard} that sources AWS data-egress rates from the public
 * <a href="https://docs.aws.amazon.com/awsaccountbilling/latest/aboutv2/using-price-list-api.html">AWS Price List Bulk API</a>.
 * Both bulk offers it reads are unauthenticated, so this adapter needs no credentials or request
 * signing. Every rate it returns is tagged {@link PriceSource#PROVIDER_API}.
 *
 * <ul>
 *   <li>{@link EgressPath#INTERNET} → the {@code AWSDataTransfer} offer's {@code AWS Outbound} /
 *       {@code External} products, resolved by source region. The per-GB figure is the first paid
 *       on-demand tier (the headline rate; the free-tier dimension, if any, is skipped).</li>
 *   <li>{@link EgressPath#PRIVATE} → the {@code AWSDirectConnect} offer's {@code Data Transfer}
 *       products with an {@code *Outbound} transfer type for the region. The per-GB figure is the
 *       <em>lowest</em> positive on-demand rate (the best-case / local Direct Connect egress rate —
 *       the savings-relevant figure).</li>
 * </ul>
 *
 * <p>The offer files are large; each is fetched once on first <em>success</em> and cached for the
 * adapter's lifetime — a failed fetch is never memoized, so a transient outage is retried on the
 * next lookup rather than pinning "no rate" forever. The adapter is fault-tolerant — any fetch or
 * parse failure yields no rate rather than an exception. A {@code null} region yields empty, since
 * AWS egress pricing is region-specific.</p>
 */
public final class AwsPriceListRateCard implements RateCard {

    /**
     * The public {@code AWSDataTransfer} bulk-offer URL (internet-egress rates); the default
     * data-transfer source for {@link #create()} and {@link #create(String)}.
     */
    public static final String DEFAULT_OFFER_URL =
            "https://pricing.us-east-1.amazonaws.com/offers/v1.0/aws/AWSDataTransfer/current/index.json";

    /**
     * The public {@code AWSDirectConnect} bulk-offer URL (private-egress rates); the default
     * Direct Connect source unless overridden via {@link #create(String, String)}.
     */
    public static final String DEFAULT_DIRECT_CONNECT_OFFER_URL =
            "https://pricing.us-east-1.amazonaws.com/offers/v1.0/aws/AWSDirectConnect/current/index.json";

    private static final Currency USD = Currency.getInstance("USD");

    private final String offerUrl;
    private final String directConnectOfferUrl;
    private final ProviderPricingHttpClient http;
    private final Map<String, Optional<EgressRate>> cache = new ConcurrentHashMap<>();
    private final Object offerLock = new Object();
    private final Object dxOfferLock = new Object();
    private volatile JsonNode offer;
    private volatile JsonNode dxOffer;

    private AwsPriceListRateCard(String offerUrl, String directConnectOfferUrl) {
        this.offerUrl = offerUrl;
        this.directConnectOfferUrl = directConnectOfferUrl;
        this.http = new ProviderPricingHttpClient();
    }

    /**
     * Creates an adapter over the public AWS bulk-offer URLs ({@link #DEFAULT_OFFER_URL} /
     * {@link #DEFAULT_DIRECT_CONNECT_OFFER_URL}). No credentials or request signing required;
     * the offers are fetched lazily on first lookup.
     *
     * @return a new AWS pricing adapter
     */
    public static AwsPriceListRateCard create() {
        return new AwsPriceListRateCard(DEFAULT_OFFER_URL, DEFAULT_DIRECT_CONNECT_OFFER_URL);
    }

    /**
     * Creates an adapter with a custom data-transfer offer URL (Direct Connect uses the default) —
     * for a mirror, a proxy, or testing.
     *
     * @param offerUrl the full URL of the {@code AWSDataTransfer} {@code index.json}
     */
    public static AwsPriceListRateCard create(String offerUrl) {
        return new AwsPriceListRateCard(offerUrl, DEFAULT_DIRECT_CONNECT_OFFER_URL);
    }

    /**
     * Creates an adapter with custom data-transfer and Direct Connect offer URLs.
     *
     * @param offerUrl              the full URL of the {@code AWSDataTransfer} {@code index.json}
     * @param directConnectOfferUrl the full URL of the {@code AWSDirectConnect} {@code index.json}
     */
    public static AwsPriceListRateCard create(String offerUrl, String directConnectOfferUrl) {
        return new AwsPriceListRateCard(offerUrl, directConnectOfferUrl);
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
        if (provider != CloudProviderType.AWS || path == null || region == null || region.isEmpty()) {
            return Optional.empty();
        }
        String key = path.name() + "|" + region;
        Optional<EgressRate> cached = cache.get(key);
        if (cached != null) {
            return cached;
        }
        // Fetch the (shared, single-flight) offer OUTSIDE the result map: a multi-megabyte GET
        // must never run inside ConcurrentHashMap's mapping function holding a bin lock.
        JsonNode root = path == EgressPath.INTERNET ? offer() : dxOffer();
        if (root == null) {
            // Transient fetch failure: yield no rate but do NOT cache it — the next call retries.
            return Optional.empty();
        }
        Optional<EgressRate> resolved = path == EgressPath.INTERNET
                ? resolveInternetEgress(root, region)
                : resolvePrivateEgress(root, region);
        Optional<EgressRate> raced = cache.putIfAbsent(key, resolved);
        return raced != null ? raced : resolved;
    }

    @Override
    public PriceSource source() {
        return PriceSource.PROVIDER_API;
    }

    private static Optional<EgressRate> resolveInternetEgress(JsonNode root, String region) {
        JsonNode products = root.get("products");
        JsonNode onDemand = root.path("terms").path("OnDemand");
        if (products == null || !onDemand.isObject()) {
            return Optional.empty();
        }

        // The "AWS Outbound" → "External" (internet) data-transfer product for this region.
        String sku = internetSku(products, region);
        if (sku == null) {
            return Optional.empty();
        }

        BigDecimal perGb = firstPaidTier(onDemand.path(sku));
        if (perGb == null) {
            return Optional.empty();
        }
        return Optional.of(EgressRate.of(perGb, USD, PriceSource.PROVIDER_API)
                .withNote("AWS Price List: data transfer out to internet, " + region));
    }

    private static Optional<EgressRate> resolvePrivateEgress(JsonNode root, String region) {
        JsonNode products = root.get("products");
        JsonNode onDemand = root.path("terms").path("OnDemand");
        if (products == null || !onDemand.isObject()) {
            return Optional.empty();
        }

        // The lowest positive Direct Connect *Outbound data-transfer rate from this region.
        BigDecimal best = null;
        for (Iterator<Map.Entry<String, JsonNode>> it = products.fields(); it.hasNext(); ) {
            Map.Entry<String, JsonNode> entry = it.next();
            JsonNode p = entry.getValue();
            JsonNode attrs = p.path("attributes");
            if (!"Data Transfer".equals(p.path("productFamily").asText(""))
                    || !region.equals(attrs.path("fromRegionCode").asText(""))
                    || !attrs.path("transferType").asText("").contains("Outbound")) {
                continue;
            }
            String sku = p.path("sku").asText(entry.getKey());
            BigDecimal rate = minPositiveRate(onDemand.path(sku));
            if (rate != null && (best == null || rate.compareTo(best) < 0)) {
                best = rate;
            }
        }
        if (best == null) {
            return Optional.empty();
        }
        return Optional.of(EgressRate.of(best, USD, PriceSource.PROVIDER_API)
                .withNote("AWS Price List: Direct Connect data transfer out, " + region));
    }

    private static String internetSku(JsonNode products, String region) {
        for (Iterator<Map.Entry<String, JsonNode>> it = products.fields(); it.hasNext(); ) {
            Map.Entry<String, JsonNode> entry = it.next();
            JsonNode attrs = entry.getValue().path("attributes");
            if ("AWS Outbound".equals(attrs.path("transferType").asText(""))
                    && "External".equals(attrs.path("toLocation").asText(""))
                    && region.equals(attrs.path("fromRegionCode").asText(""))) {
                return entry.getValue().path("sku").asText(entry.getKey());
            }
        }
        return null;
    }

    private static BigDecimal firstPaidTier(JsonNode skuTerms) {
        if (!skuTerms.isObject()) {
            return null;
        }
        BigDecimal best = null;
        long bestBegin = Long.MAX_VALUE;
        for (JsonNode term : skuTerms) {
            for (JsonNode dim : term.path("priceDimensions")) {
                BigDecimal price = usd(dim);
                if (price == null) {
                    continue;
                }
                long begin = parseBegin(dim.path("beginRange").asText("0"));
                if (begin < bestBegin) {
                    bestBegin = begin;
                    best = price;
                }
            }
        }
        return best;
    }

    private static BigDecimal minPositiveRate(JsonNode skuTerms) {
        if (!skuTerms.isObject()) {
            return null;
        }
        BigDecimal best = null;
        for (JsonNode term : skuTerms) {
            for (JsonNode dim : term.path("priceDimensions")) {
                BigDecimal price = usd(dim);
                if (price != null && (best == null || price.compareTo(best) < 0)) {
                    best = price;
                }
            }
        }
        return best;
    }

    private static BigDecimal usd(JsonNode dim) {
        try {
            BigDecimal price = new BigDecimal(dim.path("pricePerUnit").path("USD").asText("0"));
            return price.signum() > 0 ? price : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static long parseBegin(String beginRange) {
        try {
            return Long.parseLong(beginRange.trim());
        } catch (NumberFormatException e) {
            return Long.MAX_VALUE; // "Inf" and the like sort last
        }
    }

    /**
     * The data-transfer offer, fetched single-flight and memoized on <em>success only</em>:
     * a failed fetch leaves the field {@code null} so the next lookup retries, rather than a
     * transient outage being remembered as "no offer" for the adapter's lifetime.
     *
     * @return the offer root, or {@code null} when the fetch failed
     */
    private JsonNode offer() {
        JsonNode local = offer;
        if (local == null) {
            synchronized (offerLock) {
                local = offer;
                if (local == null) {
                    local = http.getJson(offerUrl).orElse(null);
                    offer = local;
                }
            }
        }
        return local;
    }

    /**
     * The Direct Connect offer; same success-only memoization contract as {@link #offer()}.
     *
     * @return the offer root, or {@code null} when the fetch failed
     */
    private JsonNode dxOffer() {
        JsonNode local = dxOffer;
        if (local == null) {
            synchronized (dxOfferLock) {
                local = dxOffer;
                if (local == null) {
                    local = http.getJson(directConnectOfferUrl).orElse(null);
                    dxOffer = local;
                }
            }
        }
        return local;
    }
}
