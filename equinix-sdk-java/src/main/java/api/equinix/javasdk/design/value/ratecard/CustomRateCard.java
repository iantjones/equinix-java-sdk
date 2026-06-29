package api.equinix.javasdk.design.value.ratecard;

import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.fabric.enums.ConnectionType;
import api.equinix.javasdk.fabric.model.implementation.cloud.CloudProviderType;

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
 *     // a metro- and term-specific override (e.g. a negotiated Singapore 36-month rate):
 *     .connectionRate(ConnectionType.EVPL_VC, 10_000, MetroCode.SG, Term.MONTH_36,
 *                     new BigDecimal("1600.00"), new BigDecimal("0.00"))
 *     .cloudRouterRate("STANDARD", new BigDecimal("285.00"))
 *     .defaultConnectionRate(new BigDecimal("400.00"))   // fallback for unlisted bandwidths
 *     .build();
 * }</pre>
 *
 * <h3>Granularity &amp; resolution order</h3>
 * <p>Rates may be declared at four levels of specificity. A lookup for a given
 * {@code (type, bandwidth, metro, term)} resolves to the <em>most specific</em>
 * declared entry, trying in order:</p>
 * <ol>
 *   <li>exact metro <em>and</em> term;</li>
 *   <li>exact metro, any term;</li>
 *   <li>any metro, exact term;</li>
 *   <li>any metro, any term (the metro/term-agnostic {@code connectionRate(type, bandwidth, …)} entry);</li>
 *   <li>the declared default, otherwise {@link Optional#empty()} so a layered card can defer.</li>
 * </ol>
 * <p>Cloud-router rates resolve the same way over {@code (packageCode, metro, term)}.
 * Every quote this card returns is tagged {@link PriceSource#CUSTOM}.</p>
 */
public final class CustomRateCard implements RateCard {

    private static final String WILDCARD = "*";

    private final Currency currency;
    private final Map<String, PriceQuote> connectionRates;
    private final Map<String, PriceQuote> routerRates;
    private final Map<String, EgressRate> egressRates;
    private final PriceQuote defaultConnection;
    private final PriceQuote defaultRouter;

    private CustomRateCard(Builder b) {
        this.currency = b.currency;
        this.connectionRates = new HashMap<>();
        this.routerRates = new HashMap<>();
        this.egressRates = new HashMap<>();

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
    public PriceSource source() {
        return PriceSource.CUSTOM;
    }

    // ── Keys ──

    private static String connKey(ConnectionType type, int bandwidthMbps, MetroCode metro, Term term) {
        return (type == null ? "ANY" : type.name()) + "|" + bandwidthMbps
                + "|" + (metro == null ? WILDCARD : metro.name())
                + "|" + (term == null ? WILDCARD : term.name());
    }

    /** Candidate connection keys from most to least specific, so the closest declared rate wins. */
    private static List<String> connKeyCandidates(ConnectionType type, int bandwidthMbps, MetroCode metro, Term term) {
        List<String> keys = new ArrayList<>(4);
        keys.add(connKey(type, bandwidthMbps, metro, term));
        keys.add(connKey(type, bandwidthMbps, metro, null));
        keys.add(connKey(type, bandwidthMbps, null, term));
        keys.add(connKey(type, bandwidthMbps, null, null));
        return keys;
    }

    private static String routerKey(String packageCode, MetroCode metro, Term term) {
        return (packageCode == null ? "ANY" : packageCode)
                + "|" + (metro == null ? WILDCARD : metro.name())
                + "|" + (term == null ? WILDCARD : term.name());
    }

    /** Candidate router keys from most to least specific. */
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

    // ── Builder ──

    /** Fluent builder for {@link CustomRateCard}. */
    public static final class Builder {

        private Currency currency = Currency.getInstance("USD");
        private final List<ConnEntry> connectionEntries = new ArrayList<>();
        private final List<RouterEntry> routerEntries = new ArrayList<>();
        private final List<EgressEntry> egressEntries = new ArrayList<>();
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

        /** Declares a metro/term-agnostic monthly-only rate for a connection of the given type and bandwidth. */
        public Builder connectionRate(ConnectionType type, int bandwidthMbps, BigDecimal monthly) {
            return connectionRate(type, bandwidthMbps, null, null, monthly, BigDecimal.ZERO);
        }

        /** Declares a metro/term-agnostic monthly + one-time setup rate for a connection of the given type and bandwidth. */
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

        /** Fallback monthly rate used for any connection without a more specific entry. */
        public Builder defaultConnectionRate(BigDecimal monthly) {
            return defaultConnectionRate(monthly, BigDecimal.ZERO);
        }

        /** Fallback monthly + setup rate used for any connection without a more specific entry. */
        public Builder defaultConnectionRate(BigDecimal monthly, BigDecimal setup) {
            this.defaultConnectionMonthly = monthly;
            this.defaultConnectionSetup = setup;
            return this;
        }

        /** Declares a metro/term-agnostic monthly-only rate for a Cloud Router package. */
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

        /** Fallback monthly rate used for any Cloud Router package without a more specific entry. */
        public Builder defaultCloudRouterRate(BigDecimal monthly) {
            return defaultCloudRouterRate(monthly, BigDecimal.ZERO);
        }

        /** Fallback monthly + setup rate used for any Cloud Router package without a more specific entry. */
        public Builder defaultCloudRouterRate(BigDecimal monthly, BigDecimal setup) {
            this.defaultRouterMonthly = monthly;
            this.defaultRouterSetup = setup;
            return this;
        }

        /**
         * Declares a per-GB data-egress rate for a cloud provider over a given path.
         * Provide both {@link EgressPath#INTERNET} and {@link EgressPath#PRIVATE} rates
         * for a provider to drive the egress savings calculation.
         *
         * @param provider the cloud provider the data leaves
         * @param path     internet vs. private interconnect
         * @param perGb    the price per GB of egress
         * @return this builder for method chaining
         */
        public Builder egressRate(CloudProviderType provider, EgressPath path, BigDecimal perGb) {
            egressEntries.add(new EgressEntry(provider, path, perGb));
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
}
