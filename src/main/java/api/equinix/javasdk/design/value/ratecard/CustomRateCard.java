package api.equinix.javasdk.design.value.ratecard;

import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.fabric.enums.ConnectionType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A {@link RateCard} whose prices are supplied by the caller — the way to feed
 * the cost models <em>real</em> figures such as negotiated contract rates,
 * rather than published list pricing or heuristics. It is constructed fluently,
 * mirroring how workloads, sites, and constraints are declared elsewhere in the
 * design API:
 *
 * <pre>{@code
 * CustomRateCard rates = CustomRateCard.builder()
 *     .currency("USD")
 *     .connectionRate(ConnectionType.EVPL_VC, 1_000, new BigDecimal("250.00"))
 *     .connectionRate(ConnectionType.EVPL_VC, 10_000, new BigDecimal("1800.00"), new BigDecimal("500.00"))
 *     .cloudRouterRate("STANDARD", new BigDecimal("285.00"))
 *     .defaultConnectionRate(new BigDecimal("400.00"))   // fallback for unlisted bandwidths
 *     .build();
 * }</pre>
 *
 * <p>Lookups resolve to the exact {@code (type, bandwidth)} entry when present,
 * otherwise to the declared default, otherwise {@link Optional#empty()} so a
 * layered card can defer to another source. Every quote it returns is tagged
 * {@link PriceSource#CUSTOM}.</p>
 *
 * <p>Metro and term are accepted by the lookup methods for interface
 * compatibility but are not yet used to differentiate custom rates; per-metro
 * and per-term granularity is a planned enhancement.</p>
 */
public final class CustomRateCard implements RateCard {

    private final Currency currency;
    private final Map<String, PriceQuote> connectionRates;
    private final Map<String, PriceQuote> routerRates;
    private final PriceQuote defaultConnection;
    private final PriceQuote defaultRouter;

    private CustomRateCard(Builder b) {
        this.currency = b.currency;
        this.connectionRates = new HashMap<>();
        this.routerRates = new HashMap<>();

        for (ConnEntry e : b.connectionEntries) {
            connectionRates.put(connKey(e.type, e.bandwidthMbps),
                    PriceQuote.of(e.monthly, e.setup, currency, PriceSource.CUSTOM));
        }
        for (RouterEntry e : b.routerEntries) {
            routerRates.put(e.packageCode,
                    PriceQuote.of(e.monthly, e.setup, currency, PriceSource.CUSTOM));
        }
        this.defaultConnection = b.defaultConnectionMonthly == null ? null
                : PriceQuote.of(b.defaultConnectionMonthly, b.defaultConnectionSetup, currency, PriceSource.CUSTOM);
        this.defaultRouter = b.defaultRouterMonthly == null ? null
                : PriceQuote.of(b.defaultRouterMonthly, b.defaultRouterSetup, currency, PriceSource.CUSTOM);
    }

    /** Starts a new custom rate-card builder. */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Optional<PriceQuote> connection(ConnectionType type, int bandwidthMbps, MetroCode metro, Term term) {
        PriceQuote exact = connectionRates.get(connKey(type, bandwidthMbps));
        if (exact != null) {
            return Optional.of(exact);
        }
        return Optional.ofNullable(defaultConnection);
    }

    @Override
    public Optional<PriceQuote> cloudRouter(String packageCode, MetroCode metro, Term term) {
        PriceQuote exact = packageCode == null ? null : routerRates.get(packageCode);
        if (exact != null) {
            return Optional.of(exact);
        }
        return Optional.ofNullable(defaultRouter);
    }

    @Override
    public PriceSource source() {
        return PriceSource.CUSTOM;
    }

    private static String connKey(ConnectionType type, int bandwidthMbps) {
        return (type == null ? "ANY" : type.name()) + "|" + bandwidthMbps;
    }

    // ── Builder ──

    /** Fluent builder for {@link CustomRateCard}. */
    public static final class Builder {

        private Currency currency = Currency.getInstance("USD");
        private final List<ConnEntry> connectionEntries = new ArrayList<>();
        private final List<RouterEntry> routerEntries = new ArrayList<>();
        private BigDecimal defaultConnectionMonthly;
        private BigDecimal defaultConnectionSetup = BigDecimal.ZERO;
        private BigDecimal defaultRouterMonthly;
        private BigDecimal defaultRouterSetup = BigDecimal.ZERO;

        /** Sets the currency for all rates on this card. Defaults to USD. */
        public Builder currency(Currency currency) {
            this.currency = currency;
            return this;
        }

        /** Sets the currency by ISO 4217 code (e.g. {@code "USD"}, {@code "EUR"}). */
        public Builder currency(String currencyCode) {
            this.currency = Currency.getInstance(currencyCode);
            return this;
        }

        /** Declares a monthly-only rate for a connection of the given type and bandwidth. */
        public Builder connectionRate(ConnectionType type, int bandwidthMbps, BigDecimal monthly) {
            return connectionRate(type, bandwidthMbps, monthly, BigDecimal.ZERO);
        }

        /** Declares a monthly + one-time setup rate for a connection of the given type and bandwidth. */
        public Builder connectionRate(ConnectionType type, int bandwidthMbps, BigDecimal monthly, BigDecimal setup) {
            connectionEntries.add(new ConnEntry(type, bandwidthMbps, monthly, setup));
            return this;
        }

        /** Fallback monthly rate used for any connection without an exact {@code (type, bandwidth)} entry. */
        public Builder defaultConnectionRate(BigDecimal monthly) {
            return defaultConnectionRate(monthly, BigDecimal.ZERO);
        }

        /** Fallback monthly + setup rate used for any connection without an exact entry. */
        public Builder defaultConnectionRate(BigDecimal monthly, BigDecimal setup) {
            this.defaultConnectionMonthly = monthly;
            this.defaultConnectionSetup = setup;
            return this;
        }

        /** Declares a monthly-only rate for a Cloud Router package. */
        public Builder cloudRouterRate(String packageCode, BigDecimal monthly) {
            return cloudRouterRate(packageCode, monthly, BigDecimal.ZERO);
        }

        /** Declares a monthly + one-time setup rate for a Cloud Router package. */
        public Builder cloudRouterRate(String packageCode, BigDecimal monthly, BigDecimal setup) {
            routerEntries.add(new RouterEntry(packageCode, monthly, setup));
            return this;
        }

        /** Fallback monthly rate used for any Cloud Router package without an exact entry. */
        public Builder defaultCloudRouterRate(BigDecimal monthly) {
            return defaultCloudRouterRate(monthly, BigDecimal.ZERO);
        }

        /** Fallback monthly + setup rate used for any Cloud Router package without an exact entry. */
        public Builder defaultCloudRouterRate(BigDecimal monthly, BigDecimal setup) {
            this.defaultRouterMonthly = monthly;
            this.defaultRouterSetup = setup;
            return this;
        }

        /** Builds the immutable {@link CustomRateCard}. */
        public CustomRateCard build() {
            return new CustomRateCard(this);
        }
    }

    private static final class ConnEntry {
        final ConnectionType type;
        final int bandwidthMbps;
        final BigDecimal monthly;
        final BigDecimal setup;

        ConnEntry(ConnectionType type, int bandwidthMbps, BigDecimal monthly, BigDecimal setup) {
            this.type = type;
            this.bandwidthMbps = bandwidthMbps;
            this.monthly = monthly;
            this.setup = setup;
        }
    }

    private static final class RouterEntry {
        final String packageCode;
        final BigDecimal monthly;
        final BigDecimal setup;

        RouterEntry(String packageCode, BigDecimal monthly, BigDecimal setup) {
            this.packageCode = packageCode;
            this.monthly = monthly;
            this.setup = setup;
        }
    }
}
