package com.eqixiac.equinix.design.value.ratecard;

import com.eqixiac.equinix.core.enums.MetroCode;
import com.eqixiac.equinix.fabric.enums.ConnectionType;
import com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A {@link RateCard} whose prices are supplied by the caller — e.g. negotiated
 * contract rates — instead of published list pricing or built-in heuristics. It is
 * constructed fluently,
 * mirroring how workloads, sites, and constraints are declared elsewhere in the
 * design API:
 *
 * <pre>{@code
 * CustomRateCard rates = CustomRateCard.builder()
 *     .currency("USD")
 *     .connectionRate(ConnectionType.EVPL_VC, 1_000, new BigDecimal("250.00"))
 *     .connectionRate(ConnectionType.EVPL_VC, 10_000, new BigDecimal("1800.00"), new BigDecimal("500.00"))
 *     // a metro- and term-specific override (e.g. a negotiated Singapore 36-month rate):
 *     .connectionRate(ConnectionType.EVPL_VC, 10_000, MetroCode.SG, Term.MONTH_36,
 *                     new BigDecimal("1600.00"), new BigDecimal("0.00"))
 *     .cloudRouterRate("STANDARD", new BigDecimal("285.00"))
 *     .defaultConnectionRate(new BigDecimal("400.00"))   // fallback for unlisted bandwidths
 *     .build();
 * }</pre>
 *
 * <h3>Granularity &amp; resolution order</h3>
 * <p>Rates may be declared at several levels of specificity — with or without a metro,
 * a term, and (for connections) a concrete type. A lookup for a given
 * {@code (type, bandwidth, metro, term)} resolves to the <em>most specific</em>
 * declared entry, trying in order:</p>
 * <ol>
 *   <li>exact metro <em>and</em> term;</li>
 *   <li>exact metro, any term;</li>
 *   <li>any metro, exact term;</li>
 *   <li>any metro, any term (the metro/term-agnostic {@code connectionRate(type, bandwidth, …)} entry);</li>
 *   <li>a type-agnostic entry (declared with a {@code null} type, meaning "any connection type"),
 *       probed at the same four levels of metro/term specificity;</li>
 *   <li>the declared default, otherwise {@link Optional#empty()} so a layered card can defer.</li>
 * </ol>
 * <p>Cloud-router rates resolve the same way over {@code (packageCode, metro, term)},
 * and colocation rates over {@code (item, metro, term)}. Egress rates are keyed by
 * provider + path only — the {@code region} and {@code term} lookup parameters are
 * ignored on this card. Declaring the same key twice is
 * <em>last-declaration-wins</em>: the later call silently replaces the earlier rate.
 * Every quote this card returns is tagged {@link PriceSource#CUSTOM}.</p>
 */
public final class CustomRateCard implements RateCard {

    private static final String WILDCARD = "*";

    private final Currency currency;
    private final Map<String, PriceQuote> connectionRates;
    private final Map<String, PriceQuote> routerRates;
    private final Map<String, EgressRate> egressRates;
    private final Map<String, PriceQuote> colocationRates;
    private final PriceQuote defaultConnection;
    private final PriceQuote defaultRouter;

    private CustomRateCard(Builder b) {
        this.currency = b.currency;
        this.connectionRates = new HashMap<>();
        this.routerRates = new HashMap<>();
        this.egressRates = new HashMap<>();
        this.colocationRates = new HashMap<>();

        for (ConnEntry e : b.connectionEntries) {
            connectionRates.put(connKey(e.type, e.bandwidthMbps, e.metro, e.term),
                    PriceQuote.of(e.monthly, e.setup, currency, PriceSource.CUSTOM));
        }
        for (RouterEntry e : b.routerEntries) {
            routerRates.put(routerKey(e.packageCode, e.metro, e.term),
                    PriceQuote.of(e.monthly, e.setup, currency, PriceSource.CUSTOM));
        }
        for (EgressEntry e : b.egressEntries) {
            egressRates.put(egressKey(e.provider, e.path),
                    EgressRate.of(e.perGb, currency, PriceSource.CUSTOM));
        }
        for (ColoEntry e : b.colocationEntries) {
            colocationRates.put(coloKey(e.item, e.metro, e.term),
                    PriceQuote.of(e.monthly, e.setup, currency, PriceSource.CUSTOM));
        }
        this.defaultConnection = b.defaultConnectionMonthly == null ? null
                : PriceQuote.of(b.defaultConnectionMonthly, b.defaultConnectionSetup, currency, PriceSource.CUSTOM);
        this.defaultRouter = b.defaultRouterMonthly == null ? null
                : PriceQuote.of(b.defaultRouterMonthly, b.defaultRouterSetup, currency, PriceSource.CUSTOM);
    }

    /**
     * Starts a new custom rate card.
     *
     * @return a fresh {@link Builder} (currency defaults to USD)
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Optional<PriceQuote> connection(ConnectionType type, int bandwidthMbps, MetroCode metro, Term term) {
        for (String key : connKeyCandidates(type, bandwidthMbps, metro, term)) {
            PriceQuote match = connectionRates.get(key);
            if (match != null) {
                return Optional.of(match);
            }
        }
        return Optional.ofNullable(defaultConnection);
    }

    @Override
    public Optional<PriceQuote> cloudRouter(String packageCode, MetroCode metro, Term term) {
        for (String key : routerKeyCandidates(packageCode, metro, term)) {
            PriceQuote match = routerRates.get(key);
            if (match != null) {
                return Optional.of(match);
            }
        }
        return Optional.ofNullable(defaultRouter);
    }

    @Override
    public Optional<EgressRate> egress(CloudProviderType provider, String region, EgressPath path, Term term) {
        if (provider == null || path == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(egressRates.get(egressKey(provider, path)));
    }

    @Override
    public Optional<PriceQuote> colocation(ColocationItem item, MetroCode metro, Term term) {
        if (item == null) {
            return Optional.empty();
        }
        for (String key : coloKeyCandidates(item, metro, term)) {
            PriceQuote match = colocationRates.get(key);
            if (match != null) {
                return Optional.of(match);
            }
        }
        return Optional.empty();
    }

    @Override
    public PriceSource source() {
        return PriceSource.CUSTOM;
    }

    // ── Keys ──

    private static String connKey(ConnectionType type, int bandwidthMbps, MetroCode metro, Term term) {
        return (type == null ? "ANY" : type.name()) + "|" + bandwidthMbps
                + "|" + (metro == null ? WILDCARD : metro.name())
                + "|" + (term == null ? WILDCARD : term.name());
    }

    private static List<String> connKeyCandidates(ConnectionType type, int bandwidthMbps, MetroCode metro, Term term) {
        List<String> keys = new ArrayList<>(8);
        keys.add(connKey(type, bandwidthMbps, metro, term));
        keys.add(connKey(type, bandwidthMbps, metro, null));
        keys.add(connKey(type, bandwidthMbps, null, term));
        keys.add(connKey(type, bandwidthMbps, null, null));
        if (type != null) {
            // A rate declared with a null ConnectionType is stored under the ANY type key; probe
            // those variants too so the type-agnostic entry is reachable from a concrete-type
            // lookup — after the concrete-type candidates (a typed entry is more specific and
            // wins) but before the declared default.
            keys.add(connKey(null, bandwidthMbps, metro, term));
            keys.add(connKey(null, bandwidthMbps, metro, null));
            keys.add(connKey(null, bandwidthMbps, null, term));
            keys.add(connKey(null, bandwidthMbps, null, null));
        }
        return keys;
    }

    private static String routerKey(String packageCode, MetroCode metro, Term term) {
        return (packageCode == null ? "ANY" : packageCode)
                + "|" + (metro == null ? WILDCARD : metro.name())
                + "|" + (term == null ? WILDCARD : term.name());
    }

    private static List<String> routerKeyCandidates(String packageCode, MetroCode metro, Term term) {
        List<String> keys = new ArrayList<>(4);
        keys.add(routerKey(packageCode, metro, term));
        keys.add(routerKey(packageCode, metro, null));
        keys.add(routerKey(packageCode, null, term));
        keys.add(routerKey(packageCode, null, null));
        return keys;
    }

    private static String egressKey(CloudProviderType provider, EgressPath path) {
        return provider.name() + "|" + path.name();
    }

    private static String coloKey(ColocationItem item, MetroCode metro, Term term) {
        return item.name()
                + "|" + (metro == null ? WILDCARD : metro.name())
                + "|" + (term == null ? WILDCARD : term.name());
    }

    private static List<String> coloKeyCandidates(ColocationItem item, MetroCode metro, Term term) {
        List<String> keys = new ArrayList<>(4);
        keys.add(coloKey(item, metro, term));
        keys.add(coloKey(item, metro, null));
        keys.add(coloKey(item, null, term));
        keys.add(coloKey(item, null, null));
        return keys;
    }

    // ── Builder ──

    /**
     * Fluent builder for a {@link CustomRateCard}. All declared amounts share one
     * currency — default USD; whatever {@link #currency(String)} value is in force at
     * {@link #build()} time stamps <em>every</em> entry, regardless of declaration
     * order. Shorthand overloads without a metro/term declare "any metro, any term"
     * entries and overloads without a setup amount declare a zero NRC; at lookup time
     * the most specific declared entry wins (see the class javadoc), and re-declaring
     * an identical key replaces the earlier rate (last declaration wins).
     */
    public static final class Builder {

        private Currency currency = Currency.getInstance("USD");
        private final List<ConnEntry> connectionEntries = new ArrayList<>();
        private final List<RouterEntry> routerEntries = new ArrayList<>();
        private final List<EgressEntry> egressEntries = new ArrayList<>();
        private final List<ColoEntry> colocationEntries = new ArrayList<>();
        private BigDecimal defaultConnectionMonthly;
        private BigDecimal defaultConnectionSetup = BigDecimal.ZERO;
        private BigDecimal defaultRouterMonthly;
        private BigDecimal defaultRouterSetup = BigDecimal.ZERO;

        /**
         * Sets the currency every declared amount is expressed in (default USD). The value in
         * force at {@link #build()} time applies to all entries.
         *
         * @param currency the currency of all declared amounts
         * @return this builder for method chaining
         */
        public Builder currency(Currency currency) {
            this.currency = currency;
            return this;
        }

        /**
         * Sets the currency by ISO&nbsp;4217 code, e.g. {@code "USD"} or {@code "EUR"}.
         *
         * @param currencyCode the ISO 4217 currency code
         * @return this builder for method chaining
         * @throws IllegalArgumentException if the code is not a supported ISO 4217 code
         */
        public Builder currency(String currencyCode) {
            this.currency = Currency.getInstance(currencyCode);
            return this;
        }

        /**
         * Declares a metro/term-agnostic monthly-only connection rate ({@code null} type
         * means "any connection type"; setup is zero).
         */
        public Builder connectionRate(ConnectionType type, int bandwidthMbps, BigDecimal monthly) {
            return connectionRate(type, bandwidthMbps, null, null, monthly, BigDecimal.ZERO);
        }

        /**
         * Declares a metro/term-agnostic monthly + one-time setup connection rate
         * ({@code null} type means "any connection type").
         */
        public Builder connectionRate(ConnectionType type, int bandwidthMbps, BigDecimal monthly, BigDecimal setup) {
            return connectionRate(type, bandwidthMbps, null, null, monthly, setup);
        }

        /**
         * Declares a metro- and term-specific monthly-only rate for a connection. A {@code null}
         * metro or term means "any" for that axis, so this also expresses metro-only or term-only
         * overrides.
         */
        public Builder connectionRate(ConnectionType type, int bandwidthMbps, MetroCode metro, Term term,
                                      BigDecimal monthly) {
            return connectionRate(type, bandwidthMbps, metro, term, monthly, BigDecimal.ZERO);
        }

        /**
         * Declares a metro- and term-specific monthly + one-time setup rate for a connection. A
         * {@code null} metro or term means "any" for that axis. More specific entries win over
         * less specific ones at lookup time (see {@link CustomRateCard}).
         */
        public Builder connectionRate(ConnectionType type, int bandwidthMbps, MetroCode metro, Term term,
                                      BigDecimal monthly, BigDecimal setup) {
            connectionEntries.add(new ConnEntry(type, bandwidthMbps, metro, term, monthly, setup));
            return this;
        }

        /**
         * Declares the fallback monthly connection rate (zero setup) returned when no declared
         * connection entry matches a lookup. Without a default, unmatched lookups return empty
         * so a layered card can defer.
         */
        public Builder defaultConnectionRate(BigDecimal monthly) {
            return defaultConnectionRate(monthly, BigDecimal.ZERO);
        }

        /**
         * Declares the fallback monthly + one-time setup connection rate returned when no
         * declared connection entry matches a lookup.
         */
        public Builder defaultConnectionRate(BigDecimal monthly, BigDecimal setup) {
            this.defaultConnectionMonthly = monthly;
            this.defaultConnectionSetup = setup;
            return this;
        }

        /**
         * Declares a metro/term-agnostic monthly-only rate for a Cloud Router package
         * (setup is zero).
         */
        public Builder cloudRouterRate(String packageCode, BigDecimal monthly) {
            return cloudRouterRate(packageCode, null, null, monthly, BigDecimal.ZERO);
        }

        /** Declares a metro/term-agnostic monthly + one-time setup rate for a Cloud Router package. */
        public Builder cloudRouterRate(String packageCode, BigDecimal monthly, BigDecimal setup) {
            return cloudRouterRate(packageCode, null, null, monthly, setup);
        }

        /**
         * Declares a metro- and term-specific monthly-only rate for a Cloud Router package. A
         * {@code null} metro or term means "any" for that axis.
         */
        public Builder cloudRouterRate(String packageCode, MetroCode metro, Term term, BigDecimal monthly) {
            return cloudRouterRate(packageCode, metro, term, monthly, BigDecimal.ZERO);
        }

        /**
         * Declares a metro- and term-specific monthly + one-time setup rate for a Cloud Router
         * package. A {@code null} metro or term means "any" for that axis. More specific entries
         * win over less specific ones at lookup time.
         */
        public Builder cloudRouterRate(String packageCode, MetroCode metro, Term term, BigDecimal monthly,
                                       BigDecimal setup) {
            routerEntries.add(new RouterEntry(packageCode, metro, term, monthly, setup));
            return this;
        }

        /**
         * Declares the fallback monthly Cloud Router rate (zero setup) returned when no
         * declared router entry matches a lookup.
         */
        public Builder defaultCloudRouterRate(BigDecimal monthly) {
            return defaultCloudRouterRate(monthly, BigDecimal.ZERO);
        }

        /**
         * Declares the fallback monthly + one-time setup Cloud Router rate returned when no
         * declared router entry matches a lookup.
         */
        public Builder defaultCloudRouterRate(BigDecimal monthly, BigDecimal setup) {
            this.defaultRouterMonthly = monthly;
            this.defaultRouterSetup = setup;
            return this;
        }

        /**
         * Declares a per-GB data-egress rate for a cloud provider over a given path.
         * Provide both {@link EgressPath#INTERNET} and {@link EgressPath#PRIVATE} rates
         * for a provider to drive the egress savings calculation. Egress entries have no
         * region or term axis on this card — a declared rate answers every
         * {@code egress(provider, region, path, term)} lookup for its provider + path,
         * whatever region and term are requested.
         *
         * @param provider the cloud provider the data leaves
         * @param path     internet vs. private interconnect
         * @param perGb    the price per decimal (SI) GB of egress
         * @return this builder for method chaining
         */
        public Builder egressRate(CloudProviderType provider, EgressPath path, BigDecimal perGb) {
            egressEntries.add(new EgressEntry(provider, path, perGb));
            return this;
        }

        /**
         * Declares a metro/term-agnostic monthly rate for an Equinix colocation primitive
         * (per the unit named on {@link ColocationItem} — per cabinet, per kW, or per cross-connect).
         */
        public Builder colocationRate(ColocationItem item, BigDecimal monthly) {
            return colocationRate(item, null, null, monthly, BigDecimal.ZERO);
        }

        /**
         * Declares a metro/term-agnostic monthly + one-time setup rate for an Equinix
         * colocation primitive (per the unit named on {@link ColocationItem}).
         */
        public Builder colocationRate(ColocationItem item, BigDecimal monthly, BigDecimal setup) {
            return colocationRate(item, null, null, monthly, setup);
        }

        /**
         * Declares a metro- and term-specific monthly rate for a colocation primitive. A
         * {@code null} metro or term means "any" for that axis; more specific entries win.
         */
        public Builder colocationRate(ColocationItem item, MetroCode metro, Term term, BigDecimal monthly) {
            return colocationRate(item, metro, term, monthly, BigDecimal.ZERO);
        }

        /**
         * Declares a metro- and term-specific monthly + one-time setup rate for a colocation
         * primitive. A {@code null} metro or term means "any" for that axis.
         */
        public Builder colocationRate(ColocationItem item, MetroCode metro, Term term, BigDecimal monthly,
                                      BigDecimal setup) {
            colocationEntries.add(new ColoEntry(item, metro, term, monthly, setup));
            return this;
        }

        /**
         * Builds the immutable rate card. Every declared entry is stamped with the builder's
         * final currency and tagged {@link PriceSource#CUSTOM}.
         *
         * @return the built rate card
         */
        public CustomRateCard build() {
            return new CustomRateCard(this);
        }
    }

    private static final class ConnEntry {
        final ConnectionType type;
        final int bandwidthMbps;
        final MetroCode metro;
        final Term term;
        final BigDecimal monthly;
        final BigDecimal setup;

        ConnEntry(ConnectionType type, int bandwidthMbps, MetroCode metro, Term term,
                  BigDecimal monthly, BigDecimal setup) {
            this.type = type;
            this.bandwidthMbps = bandwidthMbps;
            this.metro = metro;
            this.term = term;
            this.monthly = monthly;
            this.setup = setup;
        }
    }

    private static final class RouterEntry {
        final String packageCode;
        final MetroCode metro;
        final Term term;
        final BigDecimal monthly;
        final BigDecimal setup;

        RouterEntry(String packageCode, MetroCode metro, Term term, BigDecimal monthly, BigDecimal setup) {
            this.packageCode = packageCode;
            this.metro = metro;
            this.term = term;
            this.monthly = monthly;
            this.setup = setup;
        }
    }

    private static final class EgressEntry {
        final CloudProviderType provider;
        final EgressPath path;
        final BigDecimal perGb;

        EgressEntry(CloudProviderType provider, EgressPath path, BigDecimal perGb) {
            this.provider = provider;
            this.path = path;
            this.perGb = perGb;
        }
    }

    private static final class ColoEntry {
        final ColocationItem item;
        final MetroCode metro;
        final Term term;
        final BigDecimal monthly;
        final BigDecimal setup;

        ColoEntry(ColocationItem item, MetroCode metro, Term term, BigDecimal monthly, BigDecimal setup) {
            this.item = item;
            this.metro = metro;
            this.term = term;
            this.monthly = monthly;
            this.setup = setup;
        }
    }
}
