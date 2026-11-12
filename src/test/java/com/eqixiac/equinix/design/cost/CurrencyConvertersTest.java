package com.eqixiac.equinix.design.cost;

import com.eqixiac.equinix.design.spec.CommercialTerms;
import com.eqixiac.equinix.design.spi.CurrencyConverter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The converter chain. The live source is always given an injected fetch: no test reaches the internet.
 * The canned body is the shape observed from {@code https://api.frankfurter.dev/v1/latest?base=USD} on 2026-11-12.
 */
class CurrencyConvertersTest {

    private static final String USD_BODY = "{\"amount\":1.0,\"base\":\"USD\",\"date\":\"2026-11-11\",\"rates\":"
            + "{\"EUR\":0.89871,\"GBP\":0.75149,\"SGD\":1.2826,\"JPY\":159.23}}";

    private static final class Recording implements FrankfurterExchangeRates.Fetcher {
        final List<URI> calls = new ArrayList<>();
        final String body;
        final Exception failure;

        Recording(String body, Exception failure) {
            this.body = body;
            this.failure = failure;
        }

        @Override
        public String get(URI uri) throws Exception {
            calls.add(uri);
            if (failure != null) {
                throw failure;
            }
            return body;
        }
    }

    @Test
    @DisplayName("live: one call for the whole table, rates inverted to the target, ECB provenance, +/-2% band")
    void liveSuccess() {
        Recording fetch = new Recording(USD_BODY, null);
        FrankfurterExchangeRates live = new FrankfurterExchangeRates("USD", "https://fx.example/v1/", fetch);
        CurrencyConverter.Rate gbp = live.rate("GBP", "USD").orElseThrow();
        // 1 / 0.75149 = 1.33068970
        assertEquals(new BigDecimal("1.33068970"), gbp.rate());
        assertEquals(CurrencyConverter.Kind.LIVE, gbp.kind());
        assertEquals("ECB reference rate via Frankfurter, 2026-11-11", gbp.source());
        assertEquals(2.0, gbp.bandPercent());
        live.rate("EUR", "USD");
        live.rate("SGD", "USD");
        assertEquals(List.of(URI.create("https://fx.example/v1/latest?base=USD")), fetch.calls, "one call per run");
        assertTrue(live.fallbackNote().isEmpty());
    }

    @Test
    @DisplayName("live failure or timeout: falls back to the bundled table silently, notes why, never retries")
    void failureFallsBack() {
        Recording fetch = new Recording(null, new HttpTimeoutException("request timed out"));
        FrankfurterExchangeRates live = new FrankfurterExchangeRates("USD", null, fetch);
        CurrencyConverters.Chain chain = CurrencyConverters.chain(live, CurrencyConverters.bundledOnly());
        CurrencyConverter.Rate gbp = chain.rate("GBP", "USD").orElseThrow();
        assertEquals(CurrencyConverter.Kind.REFERENCE, gbp.kind());
        assertEquals(new BigDecimal("1.33069435"), gbp.rate(), "ECB 2026-11-11: 1.1127 / 0.83618");
        chain.rate("EUR", "USD");
        assertEquals(1, fetch.calls.size(), "never retried");
        assertEquals(1, chain.notes().size());
        assertTrue(chain.notes().get(0).startsWith("live ECB rates via Frankfurter unavailable (HttpTimeoutException"),
                chain.notes().get(0));
    }

    @Test
    @DisplayName("a currency ECB does not publish (AED) falls back to the bundled table's peg-derived rate")
    void missingCurrencyFallsBack() {
        FrankfurterExchangeRates live = new FrankfurterExchangeRates("USD", null, new Recording(USD_BODY, null));
        CurrencyConverters.Chain chain = CurrencyConverters.chain(live, CurrencyConverters.bundledOnly());
        CurrencyConverter.Rate aed = chain.rate("AED", "USD").orElseThrow();
        assertEquals(CurrencyConverter.Kind.REFERENCE, aed.kind());
        // AED per EUR = 1.1127 x 3.6725; AED -> USD = 1.1127 / (1.1127 x 3.6725) = 1 / 3.6725 = 0.27229408
        assertEquals(new BigDecimal("0.27229408"), aed.rate());
        assertTrue(chain.rate("XAF", "USD").isEmpty(), "absent from every source");
        assertTrue(chain.missingReason("XAF", "USD").startsWith("no exchange rate XAF to USD in any source"));
    }

    @Test
    @DisplayName("a non-USD target asks for that base and converts USD into it")
    void nonUsdTarget() {
        Recording fetch = new Recording("{\"amount\":1.0,\"base\":\"EUR\",\"date\":\"2026-11-11\",\"rates\":"
                + "{\"USD\":1.1127,\"GBP\":0.83618}}", null);
        FrankfurterExchangeRates live = new FrankfurterExchangeRates("EUR", null, fetch);
        assertEquals(new BigDecimal("0.89871484"), live.rate("USD", "EUR").orElseThrow().rate());
        assertEquals(URI.create(FrankfurterExchangeRates.DEFAULT_BASE_URL + "/latest?base=EUR"), fetch.calls.get(0));
    }

    @Test
    @DisplayName("a response of another shape is a failure, not a rate")
    void wrongShapeFallsBack() {
        FrankfurterExchangeRates live = new FrankfurterExchangeRates("USD", null, new Recording("{\"base\":\"EUR\"}", null));
        assertTrue(live.rate("GBP", "USD").isEmpty());
        assertTrue(live.fallbackNote().orElseThrow().contains("unexpected response shape"));
    }

    @Test
    @DisplayName("opt-out: with the offline property the standard chain holds no live source")
    void offlineOptOut() {
        String previous = System.getProperty(CurrencyConverters.OFFLINE_PROPERTY);
        try {
            System.setProperty(CurrencyConverters.OFFLINE_PROPERTY, "true");
            assertFalse(CurrencyConverters.standard("USD").name().contains("Frankfurter"));
            System.setProperty(CurrencyConverters.OFFLINE_PROPERTY, "false");
            assertTrue(CurrencyConverters.standard("USD").name().contains("Frankfurter"),
                    "constructing the chain makes no call: the fetch is lazy");
        } finally {
            System.setProperty(CurrencyConverters.OFFLINE_PROPERTY, previous == null ? "true" : previous);
        }
        assertFalse(CurrencyConverters.bundledOnly().name().contains("Frankfurter"));
    }

    @Test
    @DisplayName("precedence: pinned > supplied > defaults")
    void precedence() {
        CommercialTerms terms = CommercialTerms.builder().currency("USD").exchangeRates(new CommercialTerms.ExchangeRates(
                Map.of("GBP", new BigDecimal("1.27")), "2026-11-12", "sheet", null)).build();
        CurrencyConverter supplied = (from, to) -> java.util.Optional.of(new CurrencyConverter.Rate(from, to,
                new BigDecimal("2"), CurrencyConverter.Kind.SUPPLIED, "feed", "2026-11-12", 1.0));
        CurrencyConverters.Chain chain = CurrencyConverters.forTerms(terms, supplied, CurrencyConverters.bundledOnly());
        assertEquals(CurrencyConverter.Kind.PINNED, chain.rate("GBP", "USD").orElseThrow().kind());
        assertEquals(CurrencyConverter.Kind.SUPPLIED, chain.rate("EUR", "USD").orElseThrow().kind());
        CurrencyConverters.Chain noSupplied = CurrencyConverters.forTerms(terms, null, CurrencyConverters.bundledOnly());
        assertEquals(CurrencyConverter.Kind.REFERENCE, noSupplied.rate("EUR", "USD").orElseThrow().kind());
    }

    @Test
    @DisplayName("bundled table covers every currency the Fabric price search returns plus the ECB set")
    void bundledCoverage() {
        for (String c : List.of("USD", "EUR", "GBP", "JPY", "SGD", "AUD", "HKD", "CAD", "BRL", "CHF", "SEK", "AED",
                "KRW", "INR", "MXN", "CNY")) {
            assertTrue(BundledExchangeRates.standard().currencies().contains(c), c);
        }
        assertEquals("2026-11-11", BundledExchangeRates.standard().asOf());
    }
}
