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
 *
 * <h3>Native multicloud links</h3>
 * <p><b>Beta.</b> {@code multicloudLinkRate(...)} and {@code multicloudLinkHourlyRate(...)}
 * declare what one provider charges for its side of a native provider-to-provider link, keyed
 * by {@code (provider, bandwidthMbps, pathTier)}. Bandwidth matches exactly. A lookup tries the
 * entry for the requested path tier, then the tier-agnostic entry. Region, peer provider and
 * term are not axes on this card. A side with no declared entry is left empty, so a layered
 * chain can take it from a later card. The card does not model the AWS free tier: declare a
 * zero rate for the covered size to express it; a request that opts in to the free tier while
 * this card prices the AWS side gets the declared rate and a quote note saying the tier was not
 * applied. The per-GB charge on the link is declared
 * separately with {@code egressRate(provider, EgressPath.MULTICLOUD_INTERCONNECT, perGb)};
 * without it, a card used on its own leaves the link's data-transfer cost unknown and the
 * engines report the native path as partially priced.</p>
 */
public final class CustomRateCard implements RateCard {

    private static final String WILDCARD = "*";

    private final Currency currency;
    private final Map<String, PriceQuote> connectionRates;
    private final Map<String, PriceQuote> routerRates;
    private final Map<String, EgressRate> egressRates;
    private final Map<String, PriceQuote> colocationRates;
    private final Map<String, PriceQuote> multicloudRates;
    private final PriceQuote defaultConnection;
    private final PriceQuote defaultRouter;

    private CustomRateCard(Builder b) {
        this.currency = b.currency;
        this.connectionRates = new HashMap<>();
        this.routerRates = new HashMap<>();
        this.egressRates = new HashMap<>();
        this.colocationRates = new HashMap<>();
        this.multicloudRates = new HashMap<>();

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
        for (MulticloudEntry e : b.multicloudEntries) {
            PriceQuote quote = PriceQuote.of(e.monthly, e.setup, currency, PriceSource.CUSTOM);
            if (e.hourly != null) {
                quote = quote.withNote("custom hourly rate " + MulticloudLinkQuote.formatRate(e.hourly) + " "
                        + currency.getCurrencyCode()
                        + "/h x " + MulticloudLinkQuote.HOURS_PER_MONTH + " h/month = " + e.monthly.toPlainString()
                        + " " + currency.getCurrencyCode() + "/month");
            }
            multicloudRates.put(multicloudKey(e.provider, e.bandwidthMbps, e.pathTier), quote);
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

    /**
     * {@inheritDoc}
     *
     * <p><b>Beta.</b> Prices each side from the declared {@code multicloudLinkRate(...)} /
     * {@code multicloudLinkHourlyRate(...)} entries: exact bandwidth, the requested path tier
     * first, then the tier-agnostic entry. Returns empty when neither side has an entry. The
     * request's region and term are ignored. The free-tier flag does not change the price: when
     * it is set and this card prices the AWS side, the quote carries a note saying the declared
     * rate applies and the free tier was not modelled, so a caller who opted in is not left to
     * assume it was applied.</p>
     */
    @Override
    public Optional<MulticloudLinkQuote> multicloudLink(MulticloudLinkRequest request) {
        if (request == null) {
            return Optional.empty();
        }
        PriceQuote sideA = resolveMulticloudSide(request.getProviderA(), request);
        PriceQuote sideZ = resolveMulticloudSide(request.getProviderZ(), request);
        if (sideA == null && sideZ == null) {
            return Optional.empty();
        }
        MulticloudLinkQuote.MulticloudLinkQuoteBuilder quote = MulticloudLinkQuote.builder()
                .providerA(request.getProviderA()).regionA(request.getRegionA())
                .providerZ(request.getProviderZ()).regionZ(request.getRegionZ())
                .bandwidthMbps(request.getBandwidthMbps())
                .pathTier(request.getPathTier())
                .sideA(sideA)
                .sideAUnpricedReason(sideA != null ? null : undeclaredReason(request.getProviderA(), request))
                .sideZ(sideZ)
                .sideZUnpricedReason(sideZ != null ? null : undeclaredReason(request.getProviderZ(), request));
        PriceQuote awsSide = request.getProviderA() == CloudProviderType.AWS ? sideA
                : request.getProviderZ() == CloudProviderType.AWS ? sideZ : null;
        if (request.isUseAwsFreeTier() && awsSide != null) {
            quote.note("The AWS free tier was requested but not applied: the AWS side is priced from this card's "
                    + "declared custom rate (" + awsSide.getMonthlyRecurring().toPlainString() + " "
                    + currency.getCurrencyCode() + "/month at " + request.getBandwidthMbps()
                    + " Mbps), and CustomRateCard does not model the free tier. Declare a zero rate for the covered "
                    + "size to express it.");
        }
        return Optional.of(quote.build());
    }

    private PriceQuote resolveMulticloudSide(CloudProviderType provider, MulticloudLinkRequest request) {
        PriceQuote tiered = multicloudRates.get(
                multicloudKey(provider, request.getBandwidthMbps(), request.getPathTier()));
        return tiered != null ? tiered
                : multicloudRates.get(multicloudKey(provider, request.getBandwidthMbps(), null));
    }

    private static String undeclaredReason(CloudProviderType provider, MulticloudLinkRequest request) {
        return "no custom multicloud link rate declared for " + provider + " at "
                + request.getBandwidthMbps() + " Mbps (path tier " + request.getPathTier() + ")";
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

    private static String multicloudKey(CloudProviderType provider, int bandwidthMbps, Integer pathTier) {
        return provider.name() + "|" + bandwidthMbps + "|" + (pathTier == null ? WILDCARD : pathTier.toString());
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
        private final List<MulticloudEntry> multicloudEntries = new ArrayList<>();
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
         * Declares the monthly charge one provider bills for its side of a native multicloud
         * link of the given size, at any path tier, with no one-time charge. <b>Beta.</b>
         *
         * @param provider      the provider billing this side
         * @param bandwidthMbps the link size in Mbps; matched exactly at lookup
         * @param monthly       the monthly recurring charge in the card's currency
         * @return this builder for method chaining
         * @throws IllegalArgumentException if the provider or amount is null, the bandwidth is not
         *                                  positive, or the amount is negative
         */
        public Builder multicloudLinkRate(CloudProviderType provider, int bandwidthMbps, BigDecimal monthly) {
            return multicloudLinkRate(provider, bandwidthMbps, null, monthly, BigDecimal.ZERO);
        }

        /**
         * Declares the monthly and one-time charge one provider bills for its side of a native
         * multicloud link. <b>Beta.</b>
         *
         * @param provider      the provider billing this side
         * @param bandwidthMbps the link size in Mbps; matched exactly at lookup
         * @param pathTier      the AWS connectivity-scope tier (1-5) the rate applies to, or
         *                      {@code null} for any tier; a tier-specific entry wins over the
         *                      tier-agnostic one
         * @param monthly       the monthly recurring charge in the card's currency
         * @param setup         the one-time charge in the card's currency ({@code null} = zero)
         * @return this builder for method chaining
         * @throws IllegalArgumentException if the provider or monthly amount is null, the
         *                                  bandwidth is not positive, the tier is outside 1-5,
         *                                  or an amount is negative
         */
        public Builder multicloudLinkRate(CloudProviderType provider, int bandwidthMbps, Integer pathTier,
                                          BigDecimal monthly, BigDecimal setup) {
            multicloudEntries.add(new MulticloudEntry(provider, bandwidthMbps, pathTier, null,
                    requireAmount(monthly, "monthly"), setup == null ? BigDecimal.ZERO : requireAmount(setup, "setup")));
            return this;
        }

        /**
         * Declares one provider's side of a native multicloud link as a flat hourly rate, at any
         * path tier. The card converts it to a monthly charge at
         * {@link MulticloudLinkQuote#HOURS_PER_MONTH} hours per month and records the conversion
         * in the quote's note. <b>Beta.</b>
         *
         * @param provider      the provider billing this side
         * @param bandwidthMbps the link size in Mbps; matched exactly at lookup
         * @param hourly        the hourly rate in the card's currency
         * @return this builder for method chaining
         * @throws IllegalArgumentException under the same conditions as
         *                                  {@link #multicloudLinkRate(CloudProviderType, int, Integer, BigDecimal, BigDecimal)}
         */
        public Builder multicloudLinkHourlyRate(CloudProviderType provider, int bandwidthMbps, BigDecimal hourly) {
            return multicloudLinkHourlyRate(provider, bandwidthMbps, null, hourly);
        }

        /**
         * Declares one provider's side of a native multicloud link as a flat hourly rate for a
         * specific path tier ({@code null} = any tier). Converted to monthly at
         * {@link MulticloudLinkQuote#HOURS_PER_MONTH} hours per month. <b>Beta.</b>
         *
         * @param provider      the provider billing this side
         * @param bandwidthMbps the link size in Mbps; matched exactly at lookup
         * @param pathTier      the AWS connectivity-scope tier (1-5), or {@code null} for any tier
         * @param hourly        the hourly rate in the card's currency
         * @return this builder for method chaining
         * @throws IllegalArgumentException under the same conditions as
         *                                  {@link #multicloudLinkRate(CloudProviderType, int, Integer, BigDecimal, BigDecimal)}
         */
        public Builder multicloudLinkHourlyRate(CloudProviderType provider, int bandwidthMbps, Integer pathTier,
                                                BigDecimal hourly) {
            BigDecimal checked = requireAmount(hourly, "hourly");
            multicloudEntries.add(new MulticloudEntry(provider, bandwidthMbps, pathTier, checked,
                    MulticloudLinkQuote.monthlyFromHourly(checked), BigDecimal.ZERO));
            return this;
        }

        private static BigDecimal requireAmount(BigDecimal value, String name) {
            if (value == null || value.signum() < 0) {
                throw new IllegalArgumentException("multicloud link " + name
                        + " amount must be a non-negative number: " + value);
            }
            return value;
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

    private static final class MulticloudEntry {
        final CloudProviderType provider;
        final int bandwidthMbps;
        final Integer pathTier;
        final BigDecimal hourly;
        final BigDecimal monthly;
        final BigDecimal setup;

        MulticloudEntry(CloudProviderType provider, int bandwidthMbps, Integer pathTier,
                        BigDecimal hourly, BigDecimal monthly, BigDecimal setup) {
            if (provider == null) {
                throw new IllegalArgumentException("multicloud link provider must not be null");
            }
            if (bandwidthMbps <= 0) {
                throw new IllegalArgumentException("multicloud link bandwidthMbps must be positive: " + bandwidthMbps);
            }
            if (pathTier != null && (pathTier < MulticloudLinkQuote.MIN_PATH_TIER
                    || pathTier > MulticloudLinkQuote.MAX_PATH_TIER)) {
                throw new IllegalArgumentException("multicloud link pathTier must be between "
                        + MulticloudLinkQuote.MIN_PATH_TIER + " and " + MulticloudLinkQuote.MAX_PATH_TIER
                        + " (or null for any tier): " + pathTier);
            }
            this.provider = provider;
            this.bandwidthMbps = bandwidthMbps;
            this.pathTier = pathTier;
            this.hourly = hourly;
            this.monthly = monthly;
            this.setup = setup;
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
