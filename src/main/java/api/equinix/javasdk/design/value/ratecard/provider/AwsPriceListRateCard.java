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
 * <p>The offer files are large; each is fetched once on first use and cached for the adapter's
 * lifetime. The adapter is fault-tolerant — any fetch or parse failure yields no rate rather than
 * an exception. A {@code null} region yields empty, since AWS egress pricing is region-specific.</p>
 */
public final class AwsPriceListRateCard implements RateCard {

    /** The public AWS data-transfer bulk offer file (internet egress). */
    public static final String DEFAULT_OFFER_URL =
            "https://pricing.us-east-1.amazonaws.com/offers/v1.0/aws/AWSDataTransfer/current/index.json";

    /** The public AWS Direct Connect bulk offer file (private-path egress). */
    public static final String DEFAULT_DIRECT_CONNECT_OFFER_URL =
            "https://pricing.us-east-1.amazonaws.com/offers/v1.0/aws/AWSDirectConnect/current/index.json";

    private static final Currency USD = Currency.getInstance("USD");

    private final String offerUrl;
    private final String directConnectOfferUrl;
    private final ProviderPricingHttpClient http;
    private final Map<String, Optional<EgressRate>> cache = new ConcurrentHashMap<>();
    private volatile JsonNode offer;
    private volatile boolean offerFetched;
    private volatile JsonNode dxOffer;
    private volatile boolean dxOfferFetched;

    private AwsPriceListRateCard(String offerUrl, String directConnectOfferUrl) {
        this.offerUrl = offerUrl;
        this.directConnectOfferUrl = directConnectOfferUrl;
        this.http = new ProviderPricingHttpClient();
    }

    /** Creates an adapter against the public AWS data-transfer + Direct Connect bulk offers. */
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
        return cache.computeIfAbsent(path.name() + "|" + region, k ->
                path == EgressPath.INTERNET ? resolveInternetEgress(region) : resolvePrivateEgress(region));
    }

    @Override
    public PriceSource source() {
        return PriceSource.PROVIDER_API;
    }

    private Optional<EgressRate> resolveInternetEgress(String region) {
        JsonNode root = offer();
        if (root == null) {
            return Optional.empty();
        }
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

    private Optional<EgressRate> resolvePrivateEgress(String region) {
        JsonNode root = dxOffer();
        if (root == null) {
            return Optional.empty();
        }
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

    /** Returns the SKU of the internet-egress product for the region, or null. */
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

    /** The lowest-volume paid on-demand tier's per-GB USD rate for a SKU's terms, or null. */
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

    /** The minimum positive per-GB USD rate across a SKU's on-demand price dimensions, or null. */
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

    /** Parses a positive USD per-unit price from a price dimension, or null. */
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

    private JsonNode offer() {
        if (!offerFetched) {
            synchronized (this) {
                if (!offerFetched) {
                    offer = http.getJson(offerUrl).orElse(null);
                    offerFetched = true;
                }
            }
        }
        return offer;
    }

    private JsonNode dxOffer() {
        if (!dxOfferFetched) {
            synchronized (this) {
                if (!dxOfferFetched) {
                    dxOffer = http.getJson(directConnectOfferUrl).orElse(null);
                    dxOfferFetched = true;
                }
            }
        }
        return dxOffer;
    }
}
