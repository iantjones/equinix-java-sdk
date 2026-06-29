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
import java.util.Currency;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A {@link RateCard} that sources <em>cloud-egress</em> rates from the public
 * <a href="https://learn.microsoft.com/azure/cost-management-billing/manage/manage-pricing-tools">Azure Retail Prices API</a>
 * ({@code https://prices.azure.com/api/retail/prices}). The API is unauthenticated,
 * so this adapter needs no credentials.
 *
 * <p>It prices only data egress for {@link CloudProviderType#AZURE}; connection and
 * cloud-router lookups return {@link Optional#empty()} (those are Equinix-side costs).
 * It maps {@link EgressPath#INTERNET} to the {@code Bandwidth} service (data transfer
 * out to the internet) and {@link EgressPath#PRIVATE} to the {@code ExpressRoute}
 * service (metered egress over a dedicated interconnect) — the two sides of the
 * value-realization savings story. Every rate it returns is tagged
 * {@link PriceSource#PROVIDER_API}.</p>
 *
 * <p>The adapter is an opt-in, pluggable source: compose it into a precedence chain
 * with {@link RateCard#layered(RateCard...)} (e.g. caller-supplied → Equinix live →
 * provider APIs → bundled reference). It is deliberately fault-tolerant — a network
 * error, throttling, or an unrecognised response shape yields no rate rather than an
 * exception, so a layered card falls back to the next source.</p>
 *
 * <pre>{@code
 * RateCard rates = RateCard.layered(
 *     EquinixRateCard.of(fabric),
 *     AzureRetailPricesRateCard.create(),
 *     ReferenceRateCard.standard());
 * }</pre>
 */
public final class AzureRetailPricesRateCard implements RateCard {

    /** The public Azure Retail Prices endpoint. */
    public static final String DEFAULT_ENDPOINT = "https://prices.azure.com/api/retail/prices";

    private static final Currency USD = Currency.getInstance("USD");

    private final String endpoint;
    private final ProviderPricingHttpClient http;
    private final Map<String, Optional<EgressRate>> cache = new ConcurrentHashMap<>();

    private AzureRetailPricesRateCard(String endpoint) {
        this.endpoint = endpoint;
        this.http = new ProviderPricingHttpClient();
    }

    /** Creates an adapter against the public Azure Retail Prices endpoint. */
    public static AzureRetailPricesRateCard create() {
        return new AzureRetailPricesRateCard(DEFAULT_ENDPOINT);
    }

    /**
     * Creates an adapter against a custom Retail Prices endpoint — for an Azure
     * sovereign/government cloud, a proxy, or testing.
     *
     * @param endpoint the full {@code .../api/retail/prices} URL
     */
    public static AzureRetailPricesRateCard create(String endpoint) {
        return new AzureRetailPricesRateCard(endpoint);
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
        if (provider != CloudProviderType.AZURE || path == null) {
            return Optional.empty();
        }
        String key = path.name() + "|" + (region == null ? "*" : region);
        return cache.computeIfAbsent(key, k -> fetch(region, path));
    }

    @Override
    public PriceSource source() {
        return PriceSource.PROVIDER_API;
    }

    private Optional<EgressRate> fetch(String region, EgressPath path) {
        String serviceName = path == EgressPath.INTERNET ? "Bandwidth" : "ExpressRoute";
        StringBuilder filter = new StringBuilder("serviceName eq '").append(serviceName).append("'");
        if (region != null && !region.isEmpty()) {
            filter.append(" and armRegionName eq '").append(region).append("'");
        }
        String url = endpoint + "?currencyCode='USD'&$filter="
                + URLEncoder.encode(filter.toString(), StandardCharsets.UTF_8);

        Optional<JsonNode> root = http.getJson(url);
        if (root.isEmpty()) {
            return Optional.empty();
        }

        JsonNode items = root.get().get("Items");
        if (items == null || !items.isArray()) {
            return Optional.empty();
        }

        // Prefer a meter that names egress/outbound; fall back to the first priced GB meter.
        JsonNode best = null;
        for (JsonNode item : items) {
            double price = item.path("retailPrice").asDouble(0d);
            String unit = item.path("unitOfMeasure").asText("").toLowerCase();
            if (price <= 0d || !unit.contains("gb")) {
                continue;
            }
            String descriptor = (item.path("meterName").asText("") + " "
                    + item.path("productName").asText("")).toLowerCase();
            boolean egressMeter = descriptor.contains("out") || descriptor.contains("egress")
                    || descriptor.contains("internet");
            if (egressMeter) {
                best = item;
                break;
            }
            if (best == null) {
                best = item;
            }
        }
        if (best == null) {
            return Optional.empty();
        }

        BigDecimal perGb = new BigDecimal(best.path("retailPrice").asText("0"));
        String meter = best.path("meterName").asText(serviceName);
        return Optional.of(EgressRate.of(perGb, USD, PriceSource.PROVIDER_API)
                .withNote("Azure Retail Prices: " + meter));
    }
}
