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

package com.eqixiac.equinix.design.value;

import com.eqixiac.equinix.core.enums.MetroCode;
import com.eqixiac.equinix.design.value.ratecard.CustomRateCard;
import com.eqixiac.equinix.design.value.ratecard.EquinixRateCard;
import com.eqixiac.equinix.design.value.ratecard.MulticloudLinkQuote;
import com.eqixiac.equinix.design.value.ratecard.MulticloudLinkRequest;
import com.eqixiac.equinix.design.value.ratecard.PriceQuote;
import com.eqixiac.equinix.design.value.ratecard.PriceSource;
import com.eqixiac.equinix.design.value.ratecard.RateCard;
import com.eqixiac.equinix.design.value.ratecard.ReferenceRateCard;
import com.eqixiac.equinix.design.value.ratecard.Term;
import com.eqixiac.equinix.fabric.enums.ConnectionType;
import com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@code RateCard.multicloudLink} across the card implementations: the default-empty seam, the
 * {@link CustomRateCard} levers for negotiated rates, and the per-side precedence of
 * {@code RateCard.layered(...)}.
 */
class MulticloudRateCardLayeringTest {

    private static MulticloudLinkRequest tenGig() {
        return tenGig(1);
    }

    private static MulticloudLinkRequest tenGig(int tier) {
        return MulticloudLinkRequest.builder()
                .providerA(CloudProviderType.AWS).regionA("us-east-1")
                .providerZ(CloudProviderType.GOOGLE_CLOUD).regionZ("us-east4")
                .bandwidthMbps(10_000).pathTier(tier).build();
    }

    private static void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual), "expected " + expected + " but was " + actual);
    }

    // ── The seam ──

    @Test
    void aCardThatDoesNotOverrideTheSeamReturnsEmpty() {
        RateCard minimal = new RateCard() {
            @Override
            public Optional<PriceQuote> connection(ConnectionType type, int bandwidthMbps, MetroCode metro, Term term) {
                return Optional.empty();
            }

            @Override
            public Optional<PriceQuote> cloudRouter(String packageCode, MetroCode metro, Term term) {
                return Optional.empty();
            }

            @Override
            public PriceSource source() {
                return PriceSource.ESTIMATE;
            }
        };

        assertTrue(minimal.multicloudLink(tenGig()).isEmpty());
        assertTrue(EquinixRateCard.of(null).multicloudLink(tenGig()).isEmpty(),
                "Equinix sells no part of a native link; the live card does not price it");
    }

    // ── CustomRateCard ──

    @Test
    void customMonthlyRatePricesOneSideAndLeavesTheOtherEmptyWithAReason() {
        CustomRateCard card = CustomRateCard.builder()
                .multicloudLinkRate(CloudProviderType.AWS, 10_000, new BigDecimal("8000.00"))
                .build();

        MulticloudLinkQuote quote = card.multicloudLink(tenGig()).orElseThrow();

        PriceQuote aws = quote.getSideA().orElseThrow();
        assertMoney("8000.00", aws.getMonthlyRecurring());
        assertEquals(PriceSource.CUSTOM, aws.getSource());
        assertTrue(quote.getSideZ().isEmpty());
        assertTrue(quote.getSideZUnpricedReason().contains("GOOGLE_CLOUD"), quote.getSideZUnpricedReason());
        assertTrue(quote.getSideZUnpricedReason().contains("10000 Mbps"), quote.getSideZUnpricedReason());
    }

    @Test
    void customHourlyRateConvertsAt730HoursAndRecordsTheConversion() {
        CustomRateCard card = CustomRateCard.builder().currency("EUR")
                .multicloudLinkHourlyRate(CloudProviderType.GOOGLE_CLOUD, 10_000, new BigDecimal("15.5"))
                .build();

        PriceQuote google = card.multicloudLink(tenGig()).orElseThrow().getSideZ().orElseThrow();

        assertMoney("11315.00", google.getMonthlyRecurring()); // 15.50 x 730
        assertEquals("EUR", google.getCurrency().getCurrencyCode());
        assertEquals("custom hourly rate 15.50 EUR/h x 730 h/month = 11315.00 EUR/month", google.getNote());
    }

    @Test
    void customTierSpecificRateWinsOverTheTierAgnosticRate() {
        CustomRateCard card = CustomRateCard.builder()
                .multicloudLinkRate(CloudProviderType.AWS, 10_000, new BigDecimal("8000"))
                .multicloudLinkRate(CloudProviderType.AWS, 10_000, 4, new BigDecimal("30000"), new BigDecimal("500"))
                .build();

        PriceQuote tierFour = card.multicloudLink(tenGig(4)).orElseThrow().getSideA().orElseThrow();
        assertMoney("30000", tierFour.getMonthlyRecurring());
        assertMoney("500", tierFour.getNonRecurring());
        assertMoney("8000", card.multicloudLink(tenGig(1)).orElseThrow().getSideA().orElseThrow().getMonthlyRecurring());
        assertMoney("8000", card.multicloudLink(tenGig(3)).orElseThrow().getSideA().orElseThrow().getMonthlyRecurring());
    }

    @Test
    void customBandwidthMatchIsExactAndACardWithNoEntryForEitherSideIsEmpty() {
        CustomRateCard card = CustomRateCard.builder()
                .multicloudLinkRate(CloudProviderType.AWS, 1_000, new BigDecimal("900"))
                .build();

        assertTrue(card.multicloudLink(tenGig()).isEmpty(), "a 1 Gbps entry does not price a 10 Gbps link");
        assertTrue(CustomRateCard.builder().build().multicloudLink(tenGig()).isEmpty());
        assertTrue(card.multicloudLink(null).isEmpty());
    }

    @Test
    void customBuilderFailsFastOnInvalidMulticloudRates() {
        CustomRateCard.Builder b = CustomRateCard.builder();
        assertThrows(IllegalArgumentException.class,
                () -> b.multicloudLinkRate(null, 10_000, BigDecimal.ONE));
        assertThrows(IllegalArgumentException.class,
                () -> b.multicloudLinkRate(CloudProviderType.AWS, 0, BigDecimal.ONE));
        assertThrows(IllegalArgumentException.class,
                () -> b.multicloudLinkRate(CloudProviderType.AWS, 10_000, null));
        assertThrows(IllegalArgumentException.class,
                () -> b.multicloudLinkRate(CloudProviderType.AWS, 10_000, new BigDecimal("-1")));
        assertThrows(IllegalArgumentException.class,
                () -> b.multicloudLinkRate(CloudProviderType.AWS, 10_000, 6, BigDecimal.ONE, BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class,
                () -> b.multicloudLinkRate(CloudProviderType.AWS, 10_000, 1, BigDecimal.ONE, new BigDecimal("-5")));
        assertThrows(IllegalArgumentException.class,
                () -> b.multicloudLinkHourlyRate(CloudProviderType.AWS, 10_000, null));
        assertThrows(IllegalArgumentException.class,
                () -> b.multicloudLinkHourlyRate(CloudProviderType.AWS, 10_000, 0, BigDecimal.ONE));
    }

    // ── Layering: per-side precedence ──

    @Test
    void layeredTakesEachSideFromTheEarliestCardThatPricesIt() {
        // Negotiated AWS side on the custom card; the Google side comes from the reference card.
        CustomRateCard negotiated = CustomRateCard.builder()
                .multicloudLinkHourlyRate(CloudProviderType.AWS, 10_000, new BigDecimal("11.00"))
                .build();

        MulticloudLinkQuote quote = RateCard.layered(negotiated, ReferenceRateCard.standard())
                .multicloudLink(tenGig()).orElseThrow();

        PriceQuote aws = quote.getSideA().orElseThrow();
        PriceQuote google = quote.getSideZ().orElseThrow();
        assertMoney("8030.00", aws.getMonthlyRecurring()); // 11.00 x 730, not the reference 9000.90
        assertEquals(PriceSource.CUSTOM, aws.getSource());
        assertMoney("13870.00", google.getMonthlyRecurring());
        assertEquals(PriceSource.REFERENCE, google.getSource());
        assertMoney("21900.00", quote.combinedMonthly().orElseThrow());
        assertNull(quote.getSideZUnpricedReason(), "the custom card's 'not declared' reason is cleared once filled");
    }

    @Test
    void layeredStopsConsultingCardsOnceBothSidesArePriced() {
        List<String> consulted = new ArrayList<>();
        RateCard first = recording("first", consulted, ReferenceRateCard.standard());
        RateCard second = recording("second", consulted, ReferenceRateCard.standard());

        RateCard.layered(first, second).multicloudLink(tenGig());

        assertEquals(List.of("first"), consulted, "the first card priced both sides");
    }

    @Test
    void layeredKeepsEveryCardsReasonForASideNoCardPrices() {
        CustomRateCard googleOnly = CustomRateCard.builder()
                .multicloudLinkRate(CloudProviderType.GOOGLE_CLOUD, 1_000, new BigDecimal("2000"))
                .build();
        MulticloudLinkRequest oneGig = MulticloudLinkRequest.builder()
                .providerA(CloudProviderType.AWS).providerZ(CloudProviderType.GOOGLE_CLOUD)
                .bandwidthMbps(1_000).build();

        MulticloudLinkQuote quote = RateCard.layered(googleOnly, ReferenceRateCard.standard())
                .multicloudLink(oneGig).orElseThrow();

        assertTrue(quote.getSideA().isEmpty(), "neither card prices a 1 Gbps AWS side");
        assertTrue(quote.getSideAUnpricedReason().contains("no custom multicloud link rate declared"),
                quote.getSideAUnpricedReason());
        assertTrue(quote.getSideAUnpricedReason().contains("no published AWS Interconnect - multicloud rate"),
                quote.getSideAUnpricedReason());
        assertMoney("2000", quote.getSideZ().orElseThrow().getMonthlyRecurring());
    }

    @Test
    void layeredOfCardsThatAllReturnEmptyIsEmpty() {
        assertTrue(RateCard.layered(CustomRateCard.builder().build(), EquinixRateCard.of(null))
                .multicloudLink(tenGig()).isEmpty());
    }

    @Test
    void standardChainPricesTheNativeLinkFromTheReferenceCard() {
        MulticloudLinkQuote quote = RateCard.standardChain(null).multicloudLink(tenGig()).orElseThrow();
        assertMoney("22870.90", quote.combinedMonthly().orElseThrow());
    }

    private static RateCard recording(String name, List<String> consulted, RateCard delegate) {
        return new RateCard() {
            @Override
            public Optional<PriceQuote> connection(ConnectionType type, int bandwidthMbps, MetroCode metro, Term term) {
                return delegate.connection(type, bandwidthMbps, metro, term);
            }

            @Override
            public Optional<PriceQuote> cloudRouter(String packageCode, MetroCode metro, Term term) {
                return delegate.cloudRouter(packageCode, metro, term);
            }

            @Override
            public Optional<MulticloudLinkQuote> multicloudLink(MulticloudLinkRequest request) {
                consulted.add(name);
                return delegate.multicloudLink(request);
            }

            @Override
            public PriceSource source() {
                return delegate.source();
            }
        };
    }
}
