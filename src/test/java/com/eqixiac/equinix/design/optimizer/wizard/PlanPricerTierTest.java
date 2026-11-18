/*
 * Copyright 2026 Ian Jones
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.eqixiac.equinix.design.optimizer.wizard;

import com.eqixiac.equinix.core.model.MetroId;
import com.eqixiac.equinix.design.value.ratecard.PriceQuote;
import com.eqixiac.equinix.design.value.ratecard.PriceSource;
import com.eqixiac.equinix.design.value.ratecard.RateCard;
import com.eqixiac.equinix.design.value.ratecard.Term;
import com.eqixiac.equinix.fabric.enums.ConnectionType;
import com.eqixiac.equinix.core.enums.MetroCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Plan pricing: a connection is priced at the Fabric tier that covers it")
class PlanPricerTierTest {

    /** A card that, like the live price search, has a figure only for a Fabric bandwidth tier. */
    private static final RateCard TIERS_ONLY = new RateCard() {
        @Override
        public Optional<PriceQuote> connection(ConnectionType type, int bandwidthMbps, MetroCode metro, Term term) {
            return bandwidthMbps == 5000
                    ? Optional.of(PriceQuote.monthly(new BigDecimal("375"), Currency.getInstance("USD"),
                            PriceSource.EQUINIX_LIVE).withNote("tier price"))
                    : Optional.empty();
        }

        @Override
        public Optional<PriceQuote> cloudRouter(String packageCode, MetroCode metro, Term term) {
            return Optional.empty();
        }

        @Override
        public PriceSource source() {
            return PriceSource.EQUINIX_LIVE;
        }
    };

    @Test
    @DisplayName("3000 Mbps is priced at the 5000 Mbps tier and the line says so")
    void betweenTiers() {
        PriceQuote quote = PlanPricer.priceConnection(TIERS_ONLY, ConnectionType.IP_VC, 3000, MetroId.of("DC"),
                Term.MONTH_12);
        assertEquals(PriceSource.EQUINIX_LIVE, quote.getSource());
        assertEquals(0, new BigDecimal("375").compareTo(quote.getMonthlyRecurring()));
        assertTrue(quote.getNote().contains("5000 Mbps Fabric tier that covers 3000 Mbps"), quote.getNote());
    }

    @Test
    @DisplayName("a bandwidth on a tier keeps the card's note unchanged")
    void onTier() {
        PriceQuote quote = PlanPricer.priceConnection(TIERS_ONLY, ConnectionType.IP_VC, 5000, MetroId.of("DC"),
                Term.MONTH_12);
        assertEquals("tier price", quote.getNote());
    }
}
