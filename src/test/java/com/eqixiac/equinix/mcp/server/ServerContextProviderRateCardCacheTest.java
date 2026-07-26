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

package com.eqixiac.equinix.mcp.server;

import com.eqixiac.equinix.core.enums.MetroCode;
import com.eqixiac.equinix.design.value.ratecard.PriceQuote;
import com.eqixiac.equinix.design.value.ratecard.PriceSource;
import com.eqixiac.equinix.design.value.ratecard.RateCard;
import com.eqixiac.equinix.design.value.ratecard.Term;
import com.eqixiac.equinix.fabric.enums.ConnectionType;
import com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that {@link ServerContext#providerRateCard(CloudProviderType)} builds each live pricing
 * adapter once per context and reuses it — the adapters memoize multi-megabyte offer/SKU
 * catalogues internally, so a fresh adapter per tool call would defeat that caching entirely
 * (every {@code design_compare_cloud_egress} invocation would refetch the catalogue).
 */
class ServerContextProviderRateCardCacheTest {

    private static RateCard stubCard() {
        return new RateCard() {
            @Override
            public Optional<PriceQuote> connection(ConnectionType type, int bandwidthMbps,
                                                   MetroCode metro, Term term) {
                return Optional.empty();
            }

            @Override
            public Optional<PriceQuote> cloudRouter(String packageCode, MetroCode metro, Term term) {
                return Optional.empty();
            }

            @Override
            public PriceSource source() {
                return PriceSource.PROVIDER_API;
            }
        };
    }

    @Test
    @DisplayName("the factory is invoked once per provider; repeated lookups reuse the same adapter instance")
    void factoryInvokedOncePerProvider() {
        AtomicInteger creations = new AtomicInteger();
        ServerContext context = ServerContext.builder()
                .environment(Map.of())
                .providerRateCardFactory((provider, ctx) -> {
                    creations.incrementAndGet();
                    return Optional.of(stubCard());
                })
                .build();

        RateCard first = context.providerRateCard(CloudProviderType.AWS).orElseThrow();
        RateCard second = context.providerRateCard(CloudProviderType.AWS).orElseThrow();

        assertSame(first, second, "repeated calls must reuse the SAME adapter, or its internal caches never help");
        assertEquals(1, creations.get(), "the factory runs once per provider, not once per call");

        context.providerRateCard(CloudProviderType.AZURE).orElseThrow();
        context.providerRateCard(CloudProviderType.AZURE).orElseThrow();
        assertEquals(2, creations.get(), "a different provider gets its own (single) adapter");
    }

    @Test
    @DisplayName("an empty factory result (no adapter available) is cached too")
    void emptyFactoryResultIsCached() {
        AtomicInteger creations = new AtomicInteger();
        ServerContext context = ServerContext.builder()
                .environment(Map.of())
                .providerRateCardFactory((provider, ctx) -> {
                    creations.incrementAndGet();
                    return Optional.empty();
                })
                .build();

        assertTrue(context.providerRateCard(CloudProviderType.GOOGLE_CLOUD).isEmpty());
        assertTrue(context.providerRateCard(CloudProviderType.GOOGLE_CLOUD).isEmpty());
        assertEquals(1, creations.get());
    }

    @Test
    @DisplayName("the default factory reuses one live adapter per provider for the context's lifetime")
    void defaultFactoryReusesAdapters() {
        ServerContext context = ServerContext.builder()
                .environment(Map.of())
                .build();

        assertSame(context.providerRateCard(CloudProviderType.AWS).orElseThrow(),
                context.providerRateCard(CloudProviderType.AWS).orElseThrow());
        assertSame(context.providerRateCard(CloudProviderType.AZURE).orElseThrow(),
                context.providerRateCard(CloudProviderType.AZURE).orElseThrow());
        assertSame(context.providerRateCard(CloudProviderType.ORACLE_CLOUD).orElseThrow(),
                context.providerRateCard(CloudProviderType.ORACLE_CLOUD).orElseThrow());
        assertTrue(context.providerRateCard(CloudProviderType.GOOGLE_CLOUD).isEmpty(),
                "no GCP billing key in this environment, so no GCP adapter");
    }

    @Test
    @DisplayName("separate contexts do not share adapters")
    void separateContextsDoNotShareAdapters() {
        ServerContext one = ServerContext.builder().environment(Map.of()).build();
        ServerContext two = ServerContext.builder().environment(Map.of()).build();

        assertNotSame(one.providerRateCard(CloudProviderType.AWS).orElseThrow(),
                two.providerRateCard(CloudProviderType.AWS).orElseThrow(),
                "adapter reuse is per context, not process-global");
    }
}
