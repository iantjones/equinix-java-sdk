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
 * A {@link RateCard} that sources AWS <em>internet data-egress</em> rates from
 * the public
 * <a href="https://docs.aws.amazon.com/awsaccountbilling/latest/aboutv2/using-price-list-api.html">AWS Price List Bulk API</a>
 * — specifically the {@code AWSDataTransfer} offer file
 * ({@code /offers/v1.0/aws/AWSDataTransfer/current/index.json}). The bulk API is
 * unauthenticated, so this adapter needs no credentials or request signing.
 *
 * <p>It prices only {@link EgressPath#INTERNET} egress for
 * {@link CloudProviderType#AWS} — the {@code AWS Outbound} / {@code External}
 * data-transfer products, resolved by source region. The per-GB figure is the
 * first paid on-demand tier (the headline rate; the 1&nbsp;GB free tier is
 * skipped). {@link EgressPath#PRIVATE} (AWS Direct Connect data transfer) lives in
 * a separate offer and is not modelled here, so a {@code PRIVATE} lookup returns
 * empty and a layered card falls back to another source. Every rate it returns is
 * tagged {@link PriceSource#PROVIDER_API}.</p>
 *
 * <p>The offer file is large; it is fetched once on first use and cached for the
 * adapter's lifetime. The adapter is fault-tolerant — any fetch or parse failure
 * yields no rate rather than an exception. A {@code null} region yields empty,
 * since AWS egress pricing is region-specific.</p>
 */
public final class AwsPriceListRateCard implements RateCard {

    /** The public AWS data-transfer bulk offer file. */
    public static final String DEFAULT_OFFER_URL =
            "https://pricing.us-east-1.amazonaws.com/offers/v1.0/aws/AWSDataTransfer/current/index.json";

    private static final Currency USD = Currency.getInstance("USD");

    private final String offerUrl;
    private final ProviderPricingHttpClient http;
    private final Map<String, Optional<EgressRate>> cache = new ConcurrentHashMap<>();
    private volatile JsonNode offer;
    private volatile boolean offerFetched;

    private AwsPriceListRateCard(String offerUrl) {
        this.offerUrl = offerUrl;
        this.http = new ProviderPricingHttpClient();
    }

    /** Creates an adapter against the public AWS data-transfer bulk offer. */
    public static AwsPriceListRateCard create() {
        return new AwsPriceListRateCard(DEFAULT_OFFER_URL);
    }

    /**
     * Creates an adapter against a custom offer-file URL — for a mirror, a proxy,
     * or testing.
     *
     * @param offerUrl the full URL of the {@code AWSDataTransfer} {@code index.json}
     */
    public static AwsPriceListRateCard create(String offerUrl) {
        return new AwsPriceListRateCard(offerUrl);
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
        if (provider != CloudProviderType.AWS || path != EgressPath.INTERNET
                || region == null || region.isEmpty()) {
            return Optional.empty();
        }
        return cache.computeIfAbsent(region, this::resolveInternetEgress);
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
        String sku = productSku(products, region);
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

    /** Returns the SKU of the internet-egress product for the region, or null. */
    private static String productSku(JsonNode products, String region) {
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
            JsonNode dims = term.path("priceDimensions");
            for (JsonNode dim : dims) {
                String usd = dim.path("pricePerUnit").path("USD").asText("0");
                BigDecimal price;
                try {
                    price = new BigDecimal(usd);
                } catch (NumberFormatException e) {
                    continue;
                }
                if (price.signum() <= 0) {
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
}
