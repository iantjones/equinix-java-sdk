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

package api.equinix.javasdk.design.value;

import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.design.value.ratecard.CustomRateCard;
import api.equinix.javasdk.design.value.ratecard.PriceQuote;
import api.equinix.javasdk.design.value.ratecard.PriceSource;
import api.equinix.javasdk.design.value.ratecard.RateCard;
import api.equinix.javasdk.design.value.ratecard.Term;
import api.equinix.javasdk.fabric.enums.ConnectionType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the value-realization rate-card foundation: {@link PriceQuote}
 * arithmetic, {@link CustomRateCard} lookup/fallback semantics, and
 * {@link RateCard#layered} precedence.
 */
class RateCardTest {

    private static final Currency USD = Currency.getInstance("USD");

    @Test
    void priceQuote_aggregatesAndPreservesSource() {
        PriceQuote a = PriceQuote.of(new BigDecimal("250.00"), new BigDecimal("500.00"), USD, PriceSource.CUSTOM);
        PriceQuote b = PriceQuote.monthly(new BigDecimal("300.00"), USD, PriceSource.CUSTOM);

        PriceQuote sum = a.plus(b);

        assertEquals(new BigDecimal("550.00"), sum.getMonthlyRecurring());
        assertEquals(new BigDecimal("500.00"), sum.getNonRecurring());
        assertEquals(PriceSource.CUSTOM, sum.getSource(), "same-source addition keeps the source");
    }

    @Test
    void priceQuote_mixedSourcesBecomeComposite() {
        PriceQuote custom = PriceQuote.monthly(new BigDecimal("100"), USD, PriceSource.CUSTOM);
        PriceQuote live = PriceQuote.monthly(new BigDecimal("100"), USD, PriceSource.EQUINIX_LIVE);

        assertEquals(PriceSource.COMPOSITE, custom.plus(live).getSource());
    }

    @Test
    void priceQuote_rejectsCurrencyMismatch() {
        PriceQuote usd = PriceQuote.monthly(new BigDecimal("100"), USD, PriceSource.CUSTOM);
        PriceQuote eur = PriceQuote.monthly(new BigDecimal("100"), Currency.getInstance("EUR"), PriceSource.CUSTOM);

        assertThrows(IllegalArgumentException.class, () -> usd.plus(eur));
    }

    @Test
    void priceQuote_termAndAnnualTotals() {
        PriceQuote q = PriceQuote.of(new BigDecimal("100"), new BigDecimal("500"), USD, PriceSource.CUSTOM);

        assertEquals(new BigDecimal("1700"), q.annualizedTotal(), "12 × 100 + 500");
        assertEquals(new BigDecimal("4100"), q.totalOverTerm(Term.MONTH_36), "36 × 100 + 500");
    }

    @Test
    void customRateCard_exactMatchWins() {
        CustomRateCard card = CustomRateCard.builder()
                .currency("USD")
                .connectionRate(ConnectionType.EVPL_VC, 10_000, new BigDecimal("1800.00"), new BigDecimal("500.00"))
                .defaultConnectionRate(new BigDecimal("400.00"))
                .build();

        PriceQuote q = card.connection(ConnectionType.EVPL_VC, 10_000, MetroCode.DC, Term.MONTH_12).orElseThrow();

        assertEquals(new BigDecimal("1800.00"), q.getMonthlyRecurring());
        assertEquals(new BigDecimal("500.00"), q.getNonRecurring());
        assertEquals(PriceSource.CUSTOM, q.getSource());
    }

    @Test
    void customRateCard_fallsBackToDefault() {
        CustomRateCard card = CustomRateCard.builder()
                .connectionRate(ConnectionType.EVPL_VC, 10_000, new BigDecimal("1800.00"))
                .defaultConnectionRate(new BigDecimal("400.00"))
                .build();

        PriceQuote q = card.connection(ConnectionType.EVPL_VC, 50, MetroCode.SV, Term.MONTH_12).orElseThrow();

        assertEquals(new BigDecimal("400.00"), q.getMonthlyRecurring(), "unlisted bandwidth uses the default");
    }

    @Test
    void customRateCard_emptyWhenNoMatchAndNoDefault() {
        CustomRateCard card = CustomRateCard.builder()
                .cloudRouterRate("STANDARD", new BigDecimal("285.00"))
                .build();

        assertTrue(card.connection(ConnectionType.EVPL_VC, 1_000, null, Term.MONTH_12).isEmpty(),
                "no rate and no default means empty, not zero");
        assertTrue(card.cloudRouter("LARGE", null, Term.MONTH_12).isEmpty());
        assertEquals(new BigDecimal("285.00"),
                card.cloudRouter("STANDARD", null, Term.MONTH_12).orElseThrow().getMonthlyRecurring());
    }

    @Test
    void layeredRateCard_firstResolvingCardWins() {
        CustomRateCard primary = CustomRateCard.builder()
                .connectionRate(ConnectionType.EVPL_VC, 1_000, new BigDecimal("250.00"))
                .build();
        CustomRateCard fallback = CustomRateCard.builder()
                .defaultConnectionRate(new BigDecimal("999.00"))
                .build();

        RateCard layered = RateCard.layered(primary, fallback);

        // primary has the exact 1000 Mbps rate
        assertEquals(new BigDecimal("250.00"),
                layered.connection(ConnectionType.EVPL_VC, 1_000, null, Term.MONTH_12).orElseThrow().getMonthlyRecurring());
        // primary cannot price 5000 Mbps (no default) → falls through to the fallback card
        assertEquals(new BigDecimal("999.00"),
                layered.connection(ConnectionType.EVPL_VC, 5_000, null, Term.MONTH_12).orElseThrow().getMonthlyRecurring());
        assertEquals(PriceSource.COMPOSITE, layered.source());
    }

    @Test
    void layeredRateCard_emptyWhenNoCardResolves() {
        RateCard layered = RateCard.layered(
                CustomRateCard.builder().cloudRouterRate("STANDARD", new BigDecimal("285")).build());

        Optional<PriceQuote> q = layered.connection(ConnectionType.EVPL_VC, 1_000, null, Term.MONTH_12);
        assertTrue(q.isEmpty());
    }
}
