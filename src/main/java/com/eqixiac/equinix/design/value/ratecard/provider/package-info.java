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

/**
 * Live cloud-provider pricing adapters: {@code RateCard} implementations that source
 * current data-egress list prices from the providers' public pricing APIs —
 * {@link com.eqixiac.equinix.design.value.ratecard.provider.AwsPriceListRateCard} (AWS
 * Price List bulk offers),
 * {@link com.eqixiac.equinix.design.value.ratecard.provider.AzureRetailPricesRateCard}
 * (Azure Retail Prices),
 * {@link com.eqixiac.equinix.design.value.ratecard.provider.GcpBillingCatalogRateCard}
 * (GCP Cloud Billing Catalog; the only one needing an API key), and
 * {@link com.eqixiac.equinix.design.value.ratecard.provider.OracleCloudPriceListRateCard}
 * (OCI Price List). Every rate they return is tagged
 * {@link com.eqixiac.equinix.design.value.ratecard.PriceSource#PROVIDER_API}: live and
 * accurate for the provider side, but public list pricing, not negotiated rates.
 *
 * <p>The adapters are opt-in — the default value-model chain does not include them.
 * Compose them explicitly, e.g. {@code RateCard.layered(EquinixRateCard.of(fabric),
 * AzureRetailPricesRateCard.create(), ReferenceRateCard.standard())}. They price
 * egress only; connection and Cloud Router lookups return empty (those are
 * Equinix-side costs). Rates are per decimal (SI) gigabyte — GCP's per-GiB list
 * prices are converted, with the original recorded in the rate's note.</p>
 *
 * <p>All adapters share one operational contract (via a common HTTP client): hard
 * connect/read timeouts so an unresponsive endpoint degrades to "no rate" within
 * seconds; fault tolerance — any fetch, parse, or pagination failure yields empty
 * rather than throwing, so a layered chain falls back; success-only memoization —
 * catalogues are fetched lazily and cached for the adapter's lifetime, but a failed
 * or truncated fetch is never cached, so a transient outage is retried instead of
 * pinning "no rate" forever; and credential redaction — API keys never reach log
 * lines.</p>
 *
 * @see com.eqixiac.equinix.design.value.ratecard.RateCard
 * @see com.eqixiac.equinix.design.value.ratecard.EgressRate
 */
package com.eqixiac.equinix.design.value.ratecard.provider;
