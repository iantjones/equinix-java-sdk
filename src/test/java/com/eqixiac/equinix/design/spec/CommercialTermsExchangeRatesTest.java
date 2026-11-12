package com.eqixiac.equinix.design.spec;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommercialTermsExchangeRatesTest {

    private static final String JSON = "{\"name\":\"fx\",\"commercial\":{\"currency\":\"USD\",\"exchangeRates\":"
            + "{\"rates\":{\"GBP\":1.27,\"EUR\":1.08},\"asOf\":\"2026-11-12\",\"source\":\"treasury\"}}}";

    @Test
    @DisplayName("exchangeRates reads strictly from JSON and YAML, round-trips, and is part of the fingerprint")
    void readsAndRoundTrips() {
        DesignSpec spec = DesignSpecIO.fromJson(JSON);
        CommercialTerms.ExchangeRates fx = spec.commercial().exchangeRates();
        assertEquals(new BigDecimal("1.27"), fx.rate("GBP").orElseThrow());
        assertEquals("[EUR, GBP]", fx.rates().keySet().toString(), "sorted");
        assertEquals(0.0, fx.bandPercentOrDefault(), "pinned rates are exact unless a band is stated");
        assertEquals(spec, DesignSpecIO.fromYaml(DesignSpecIO.toYaml(spec)));
        assertEquals(spec.fingerprint(), DesignSpecIO.fromJson(DesignSpecIO.toJson(spec)).fingerprint());
        assertNotEquals(spec.fingerprint(), DesignSpecIO.fromJson(JSON.replace("1.27", "1.28")).fingerprint());
        assertThrows(IllegalArgumentException.class, () -> DesignSpecIO.fromJson(JSON.replace("\"source\"", "\"sauce\"")));
        assertNull(new CommercialTerms(null, null, null, null, null, null).exchangeRates(), "the six-argument form remains");
    }

    @Test
    @DisplayName("rates are validated: ISO codes, positive values, a date and a source")
    void validates() {
        assertThrows(IllegalArgumentException.class,
                () -> new CommercialTerms.ExchangeRates(Map.of("XYZ1", BigDecimal.ONE), "d", "s", null));
        assertThrows(IllegalArgumentException.class,
                () -> new CommercialTerms.ExchangeRates(Map.of("GBP", BigDecimal.ZERO), "d", "s", null));
        assertThrows(IllegalArgumentException.class,
                () -> new CommercialTerms.ExchangeRates(Map.of("GBP", BigDecimal.ONE), " ", "s", null));
        assertThrows(IllegalArgumentException.class,
                () -> new CommercialTerms.ExchangeRates(Map.of("GBP", BigDecimal.ONE), "d", null, null));
        assertThrows(IllegalArgumentException.class,
                () -> new CommercialTerms.ExchangeRates(Map.of("GBP", BigDecimal.ONE), "d", "s", 60.0));
        assertTrue(DesignSpecIO.fromJson(JSON).validate().stream().noneMatch(i -> i.toString().contains("commercial")));
    }
}
