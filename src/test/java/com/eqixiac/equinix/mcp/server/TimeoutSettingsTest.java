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

import com.eqixiac.equinix.design.value.ratecard.RateCard;
import com.eqixiac.equinix.design.value.ratecard.Term;
import com.eqixiac.equinix.fabric.enums.ConnectionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Timeout settings are range-checked; the pricing timeout is one budget per tool call")
class TimeoutSettingsTest {

    private static ServerContext with(String variable, String value) {
        return ServerContext.builder().environment(Map.of(variable, value)).build();
    }

    @ParameterizedTest(name = "{0}={1} -> {2}")
    @CsvSource({
            "EQUINIX_MCP_PRICING_TIMEOUT_MS, 250, 250",
            "EQUINIX_MCP_PRICING_TIMEOUT_MS, 5, 100",
            "EQUINIX_MCP_PRICING_TIMEOUT_MS, 99999999999, 120000",
            "EQUINIX_MCP_PRICING_TIMEOUT_MS, -3, 12000",
            "EQUINIX_MCP_PRICING_TIMEOUT_MS, 12s, 12000",
            "EQUINIX_MCP_ELICIT_TIMEOUT_MS, 200, 200",
            "EQUINIX_MCP_ELICIT_TIMEOUT_MS, 0, 300000",
            "EQUINIX_MCP_ELICIT_TIMEOUT_MS, 1, 100",
            "EQUINIX_MCP_ELICIT_TIMEOUT_MS, 86400000, 3600000",
            "EQUINIX_MCP_ELICIT_TIMEOUT_MS, 9223372036854775807, 3600000",
            "EQUINIX_MCP_ELICIT_TIMEOUT_MS, five minutes, 300000"})
    void boundedSettings(String variable, String value, long expected) {
        ServerContext ctx = with(variable, value);
        long actual = variable.equals(ServerContext.ENV_PRICING_TIMEOUT_MS)
                ? ctx.pricingTimeoutMillis() : ctx.elicitTimeoutMillis();
        assertEquals(expected, actual);
    }

    @Test
    @Timeout(10)
    @DisplayName("ten lookups against a stalled source wait one budget in total, not ten")
    void budgetIsPerCall() throws Exception {
        ServerContext ctx = with(ServerContext.ENV_PRICING_TIMEOUT_MS, "300");
        AtomicInteger started = new AtomicInteger();
        RateCard stalled = new RateCard() {
            @Override
            public Optional<com.eqixiac.equinix.design.value.ratecard.PriceQuote> connection(
                    ConnectionType type, int bandwidthMbps, com.eqixiac.equinix.core.enums.MetroCode metro, Term term) {
                started.incrementAndGet();
                try {
                    Thread.sleep(5_000);
                }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return Optional.empty();
            }

            @Override
            public Optional<com.eqixiac.equinix.design.value.ratecard.PriceQuote> cloudRouter(
                    String packageCode, com.eqixiac.equinix.core.enums.MetroCode metro, Term term) {
                return Optional.empty();
            }

            @Override
            public com.eqixiac.equinix.design.value.ratecard.PriceSource source() {
                return com.eqixiac.equinix.design.value.ratecard.PriceSource.REFERENCE;
            }
        };
        long began = System.nanoTime();
        TimeoutRateCard guarded = ctx.withCall(null, CancellationSignal.create(), () -> {
            TimeoutRateCard card = new TimeoutRateCard("Test", stalled, ctx.pricingBudget());
            for (int i = 0; i < 10; i++) {
                card.connection(ConnectionType.EVPL_VC, 1000, null, Term.MONTH_1);
            }
            return card;
        });
        long elapsedMs = (System.nanoTime() - began) / 1_000_000;
        assertTrue(elapsedMs < 1_500, "one 300 ms budget for the whole call, took " + elapsedMs + " ms");
        assertEquals(1, started.get(), "once the budget is spent, later lookups are skipped");
        assertEquals(10, guarded.failures().size());
        assertTrue(guarded.failures().get(9).contains("budget of 300 ms was spent"), guarded.failures().toString());
    }

    @Test
    @DisplayName("a cancelled call has no pricing budget left")
    void cancelledCallHasNoBudget() throws Exception {
        ServerContext ctx = with(ServerContext.ENV_PRICING_TIMEOUT_MS, "5000");
        CancellationSignal signal = CancellationSignal.create();
        signal.cancel(null);
        long remaining = ctx.withCall(null, signal, () -> ctx.pricingBudget().remainingMillis());
        assertEquals(0, remaining);
    }

    @Test
    @DisplayName("the pricing budget starts at the first lookup, so work before pricing does not spend it")
    void budgetStartsAtFirstLookup() throws Exception {
        ServerContext ctx = with(ServerContext.ENV_PRICING_TIMEOUT_MS, "300");
        long remaining = ctx.withCall(null, CancellationSignal.create(), () -> {
            PricingBudget budget = ctx.pricingBudget();
            Thread.sleep(400);
            return budget.remainingMillis();
        });
        assertTrue(remaining > 200, "an unused budget is still whole after 400 ms of other work: " + remaining);
    }

    @Test
    @Timeout(10)
    @DisplayName("repeated pricing failures are reported once each, with a count")
    void failuresAreGrouped() {
        ServerContext ctx = with(ServerContext.ENV_PRICING_TIMEOUT_MS, "100");
        PricingBudget budget = ctx.pricingBudget();
        RateCard failing = new RateCard() {
            @Override
            public Optional<com.eqixiac.equinix.design.value.ratecard.PriceQuote> connection(
                    ConnectionType type, int bandwidthMbps, com.eqixiac.equinix.core.enums.MetroCode metro, Term term) {
                throw new IllegalStateException("price search refused");
            }

            @Override
            public Optional<com.eqixiac.equinix.design.value.ratecard.PriceQuote> cloudRouter(
                    String packageCode, com.eqixiac.equinix.core.enums.MetroCode metro, Term term) {
                return Optional.empty();
            }

            @Override
            public com.eqixiac.equinix.design.value.ratecard.PriceSource source() {
                return com.eqixiac.equinix.design.value.ratecard.PriceSource.EQUINIX_LIVE;
            }
        };
        TimeoutRateCard card = new TimeoutRateCard("Equinix", failing, budget);
        for (int i = 0; i < 20; i++) {
            card.connection(ConnectionType.EVPL_VC, 1000, null, Term.MONTH_1);
        }
        com.fasterxml.jackson.databind.node.ObjectNode payload = ctx.objectMapper().createObjectNode();
        TimeoutRateCard.reportEquinix(payload, java.util.List.of(card), budget);
        com.fasterxml.jackson.databind.JsonNode block = payload.get("equinix_live_pricing");
        assertEquals(20, block.get("lookups_failed").asInt());
        assertEquals(1, block.get("failures").size(), block.toString());
        assertEquals(20, block.get("failures").get(0).get("count").asInt());
    }
}
