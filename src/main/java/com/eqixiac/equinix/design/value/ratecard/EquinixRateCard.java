package com.eqixiac.equinix.design.value.ratecard;

import com.eqixiac.equinix.FabricGateway;
import com.eqixiac.equinix.core.enums.MetroCode;
import com.eqixiac.equinix.core.http.response.PaginatedFilteredList;
import com.eqixiac.equinix.fabric.enums.ChargeFrequency;
import com.eqixiac.equinix.fabric.enums.ConnectionType;
import com.eqixiac.equinix.fabric.enums.PriceType;
import com.eqixiac.equinix.fabric.model.Pricing;
import com.eqixiac.equinix.fabric.model.implementation.Charge;
import com.eqixiac.equinix.fabric.model.implementation.PricingConnection;
import com.eqixiac.equinix.fabric.model.implementation.PricingGateway;
import com.eqixiac.equinix.fabric.model.implementation.filter.FilterPropertyList;
import com.eqixiac.equinix.fabric.model.implementation.filter.FilterType;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Optional;

/**
 * A {@link RateCard} backed by the live Equinix Fabric Pricing API
 * ({@code fabric.prices()}); the authoritative source of Equinix-side connection
 * and Cloud Router prices for the cost, savings, and TCO models.
 *
 * <p>Every lookup is one targeted {@code POST /fabric/v4/prices/search}. The live service
 * rejects a catalogue scan by {@code /type} alone (HTTP 400: {@code /connection/type},
 * {@code /connection/bandwidth} and both access-point types are mandatory for connections, and
 * {@code /router/package/code} and {@code /router/location/metroCode} for Cloud Routers; observed
 * 2026-11-11). A connection is queried as {@code COLO} (or {@code CLOUD_ROUTER} for
 * {@code IP_VC}) on the A-side to a service provider ({@code SP}) on the Z-side, both in the
 * requested metro, which is the Fabric-to-cloud connection the design models price. A lookup with
 * no metro, or for a connection type this card does not map, cannot be priced live and returns
 * empty so a layered card falls back. Rows are matched on their structured descriptor (type,
 * bandwidth, access-point metro, router package).</p>
 *
 * <p>Both lookups honour the request's remaining axes strictly. When a metro is
 * requested, a row of another metro is never returned - the lookup yields empty
 * instead, so a layered card can consult a lower layer that may hold a genuine
 * figure for that metro. The live catalogue is also term-scoped, so rows whose
 * {@code termLength} matches the requested {@link Term} are preferred; when only
 * a different-term row exists, it is returned with a note naming the
 * substitution (never silently), and a row without a {@code termLength} is
 * term-neutral.</p>
 *
 * <p>The card is fault-tolerant: if the catalogue cannot be
 * fetched (network error, unauthenticated client, endpoint unavailable), it
 * yields {@link Optional#empty()} for every lookup so callers fall back to
 * another rate card or a heuristic rather than failing, and it reports the
 * failure through {@link #isLiveSourceUnavailable()}. Every quote it does
 * produce is tagged {@link PriceSource#EQUINIX_LIVE}.</p>
 *
 * <p>Each query's result is cached for the card's lifetime <em>once it succeeds</em>. A failed
 * query is <em>not</em> cached and is retried on the next lookup, so a transient
 * {@code 429}/{@code 5xx}/auth error never permanently poisons the card. A lookup that finds no matching <em>or no priced</em> row
 * simply defers to the fallback - never a wrong or phantom-zero number.</p>
 */
@Slf4j
public final class EquinixRateCard implements RateCard {

    private static final Currency DEFAULT_CURRENCY = Currency.getInstance("USD");

    private final FabricGateway fabric;

    /** Successful query results keyed by the query; a failed query is not cached, so it is retried. */
    private final java.util.concurrent.ConcurrentMap<String, List<Pricing>> queryCache =
            new java.util.concurrent.ConcurrentHashMap<>();

    /** {@code true} when the most recent fetch attempt failed (some type-scoped query errored). */
    private volatile boolean liveSourceUnavailable;

    private EquinixRateCard(FabricGateway fabric) {
        this.fabric = fabric;
    }

    /**
     * Creates a live rate card over the given authenticated Fabric client. The
     * price catalogue is fetched lazily on the first lookup and cached for the
     * lifetime of the card.
     *
     * @param fabric the authenticated Fabric client (or any {@link FabricGateway})
     * @return a live Equinix rate card
     */
    public static EquinixRateCard of(FabricGateway fabric) {
        return new EquinixRateCard(fabric);
    }

    @Override
    public Optional<PriceQuote> connection(ConnectionType type, int bandwidthMbps, MetroCode metro, Term term) {
        return connectionLookup(type, bandwidthMbps, metro, term).toOptional();
    }

    /**
     * {@inheritDoc}
     *
     * <p>The reason names the cause: no metro, a bandwidth of 0, an unmapped connection type, the
     * live source unreachable, or no priced row for the request.</p>
     */
    @Override
    public PriceLookup<PriceQuote> connectionLookup(ConnectionType type, int bandwidthMbps, MetroCode metro,
                                                    Term term) {
        String what = (type == null ? "EVPL_VC" : type) + " " + bandwidthMbps + " Mbps" + (metro == null ? "" : " in " + metro);
        if (metro == null) {
            return PriceLookup.unpriced("live Fabric pricing needs a metro (" + what + ")");
        }
        if (bandwidthMbps <= 0) {
            return PriceLookup.unpriced("live Fabric pricing cannot price a bandwidth of " + bandwidthMbps + " Mbps");
        }
        if (accessPointTypes(type) == null) {
            return PriceLookup.unpriced("live Fabric pricing is not mapped for connection type " + type);
        }
        // The live catalogue is term-scoped: the same circuit appears once per commitment term at
        // different rates, so a lookup must prefer the row whose termLength matches the requested
        // term - otherwise a 1-month estimate silently picks up a 36-month discounted figure
        // tagged authoritative. A row that names no termLength is term-neutral and acceptable as
        // a match; a row with an explicitly different termLength is only usable as a labelled
        // last resort (its note names the substitution so the caller can see the mismatch).
        PriceQuote termFallback = null;
        Integer termFallbackLength = null;
        List<Pricing> rows = connectionRows(type, bandwidthMbps, metro, term);
        boolean failed = rows == null;
        for (Pricing p : failed ? List.<Pricing>of() : rows) {
            if (p.getType() != PriceType.VIRTUAL_CONNECTION_PRODUCT) {
                continue;
            }
            PricingConnection c = p.getConnection();
            if (c == null || c.getBandwidth() == null || c.getBandwidth() != bandwidthMbps) {
                continue;
            }
            if (!connectionTypeMatches(p, c, type)) {
                continue;
            }
            // When a metro was requested, a row of another metro is never an acceptable answer:
            // a Tokyo estimate must not be priced at Silicon Valley rates, and returning one
            // (tagged EQUINIX_LIVE) would also block a layered fallback card that might hold a
            // genuine figure for the metro. Match as a whole token, never a 2-letter substring.
            if (metro != null && !matchesMetro(p, metro)) {
                continue;
            }
            // Skip a row that carries no charge we can price (null/empty/unmapped charges):
            // defer to the fallback rather than emit a phantom $0 quote tagged authoritative.
            Optional<PriceQuote> quote = toQuote(p);
            if (quote.isEmpty()) {
                continue;
            }
            if (termMatches(p, term)) {
                return PriceLookup.priced(quote.get(), cardName());
            }
            if (termFallback == null) {
                termFallback = quote.get();
                termFallbackLength = p.getTermLength();
            }
        }
        // No term-matched row: surface the other-term row only with an explicit substitution
        // label, or empty when nothing matched at all (the layered card consults the next layer).
        if (termFallback == null) {
            return PriceLookup.unpriced(noRowReason(what, term, failed));
        }
        return PriceLookup.priced(withTermSubstitutionNote(termFallback, termFallbackLength, term), cardName());
    }

    @Override
    public Optional<PriceQuote> cloudRouter(String packageCode, MetroCode metro, Term term) {
        return cloudRouterLookup(packageCode, metro, term).toOptional();
    }

    /** {@inheritDoc} The reason names the cause as for {@link #connectionLookup}. */
    @Override
    public PriceLookup<PriceQuote> cloudRouterLookup(String packageCode, MetroCode metro, Term term) {
        String what = "Cloud Router " + packageCode + (metro == null ? "" : " in " + metro);
        if (metro == null || packageCode == null || packageCode.isBlank()) {
            return PriceLookup.unpriced("live Fabric pricing needs a metro and a package code (" + what + ")");
        }
        // Cloud Router rows carry a structured descriptor (router package code + location), so the
        // match is structural: exact package-code equality (a substring match would let STANDARD
        // false-match a NONSTANDARD row) and the row's own metroCode against the requested metro
        // (the first priced row of ANY metro is not an answer - an SG lookup must not return a DC
        // price tagged authoritative). No structural match yields empty so a layered card can
        // consult the next layer. Term rows are preferred exactly as in connection().
        PriceQuote termFallback = null;
        Integer termFallbackLength = null;
        List<Pricing> rows = routerRows(packageCode, metro, term);
        boolean failed = rows == null;
        for (Pricing p : failed ? List.<Pricing>of() : rows) {
            if (p.getType() != PriceType.CLOUD_ROUTER_PRODUCT) {
                continue;
            }
            if (!routerPackageMatches(p, packageCode)) {
                continue;
            }
            if (!routerMetroMatches(p, metro)) {
                continue;
            }
            // Defer past a router row with no priced charge instead of returning a phantom $0.
            Optional<PriceQuote> quote = toQuote(p);
            if (quote.isEmpty()) {
                continue;
            }
            if (termMatches(p, term)) {
                return PriceLookup.priced(quote.get(), cardName());
            }
            if (termFallback == null) {
                termFallback = quote.get();
                termFallbackLength = p.getTermLength();
            }
        }
        if (termFallback == null) {
            return PriceLookup.unpriced(noRowReason(what, term, failed));
        }
        return PriceLookup.priced(withTermSubstitutionNote(termFallback, termFallbackLength, term), cardName());
    }

    @Override
    public PriceSource source() {
        return PriceSource.EQUINIX_LIVE;
    }

    /**
     * Whether the most recent attempt to fetch the live price catalogue failed
     * (network error, unauthenticated client, {@code 429}/{@code 5xx}). When
     * {@code true}, an empty lookup result means "the live source could not be
     * reached", <em>not</em> "no such price"; the next lookup retries the fetch.
     * A successful fetch - even one that legitimately returns no rows - clears
     * this. Lets callers distinguish an empty catalogue from a failed fetch.
     *
     * @return {@code true} if the last catalogue fetch failed and rows may be missing
     */
    public boolean isLiveSourceUnavailable() {
        return liveSourceUnavailable;
    }

    /**
     * Why a lookup found nothing. Decided by this lookup's own query (not the card-wide flag), so concurrent
     * lookups give the same reasons as sequential ones.
     */
    private static String noRowReason(String what, Term term, boolean queryFailed) {
        if (queryFailed) {
            return "the live Fabric price search could not be reached (" + what + "); it is retried on the next lookup";
        }
        return "the live Fabric price search returned no priced row for " + what + (term == null ? "" : " at " + term);
    }

    // Queries

    /**
     * The access-point types a connection of {@code type} is priced with: A-side, then Z-side.
     * {@code null} for a type this card does not model.
     */
    private static String[] accessPointTypes(ConnectionType type) {
        if (type == null || type == ConnectionType.EVPL_VC) {
            return new String[]{"COLO", "SP"};
        }
        if (type == ConnectionType.IP_VC) {
            return new String[]{"CLOUD_ROUTER", "SP"};
        }
        return null;
    }

    private List<Pricing> connectionRows(ConnectionType type, int bandwidthMbps, MetroCode metro, Term term) {
        String[] sides = accessPointTypes(type);
        if (metro == null || sides == null || bandwidthMbps <= 0) {
            // The live API cannot price this lookup (it rejects a missing metro and a bandwidth
            // of 0 with HTTP 400); defer to the next layer without sending a request.
            return List.of();
        }
        ConnectionType effective = type == null ? ConnectionType.EVPL_VC : type;
        FilterPropertyList filter = new FilterPropertyList(FilterType.AND)
                .equals("/type", PriceType.VIRTUAL_CONNECTION_PRODUCT.name())
                .equals("/connection/type", effective.name())
                .in("/connection/bandwidth", List.of((Object) String.valueOf(bandwidthMbps)))
                .equals("/connection/aSide/accessPoint/type", sides[0])
                .equals("/connection/aSide/accessPoint/location/metroCode", metro.name())
                .equals("/connection/zSide/accessPoint/type", sides[1])
                .equals("/connection/zSide/accessPoint/location/metroCode", metro.name());
        withTerm(filter, term);
        return query("vc|" + effective + "|" + bandwidthMbps + "|" + metro + "|" + term, filter);
    }

    private List<Pricing> routerRows(String packageCode, MetroCode metro, Term term) {
        if (metro == null || packageCode == null || packageCode.isBlank()) {
            return List.of();
        }
        FilterPropertyList filter = new FilterPropertyList(FilterType.AND)
                .equals("/type", PriceType.CLOUD_ROUTER_PRODUCT.name())
                .equals("/router/location/metroCode", metro.name())
                .in("/router/package/code", List.of((Object) packageCode));
        withTerm(filter, term);
        return query("fcr|" + packageCode + "|" + metro + "|" + term, filter);
    }

    /**
     * Scopes a search to the requested term. Without {@code /termLength} the live service returns only
     * the on-demand (termLength 1) row; with it, the row for that term. {@code /termLength} is not in
     * the spec's list of price-search properties but the live service accepts it and rejects a value
     * other than 1/12/24/36 (observed 2026-11-12: Cloud Router STANDARD in DC is 1500 USD at 1 month,
     * 1275 at 12 and 1125 at 36; IP_VC 1 Gbps in DC is 200 USD at every term).
     */
    private static void withTerm(FilterPropertyList filter, Term term) {
        if (term != null) {
            filter.equals("/termLength", String.valueOf(term.months()));
        }
    }

    /** @return the rows, or {@code null} when the query failed (not cached, retried on the next lookup) */
    private List<Pricing> query(String key, FilterPropertyList filter) {
        List<Pricing> cached = queryCache.get(key);
        if (cached != null) {
            return cached;
        }
        try {
            PaginatedFilteredList<Pricing> page = fabric.prices().list(filter);
            // A null page is a genuine empty result, distinct from a failed query below.
            List<Pricing> rows = page == null ? new ArrayList<>() : new ArrayList<>(page.loadAll().toList());
            queryCache.put(key, rows);
            liveSourceUnavailable = false;
            return rows;
        } catch (RuntimeException e) {
            liveSourceUnavailable = true;
            log.warn("Could not fetch Equinix prices ({}) from the live Fabric Pricing API; the lookup defers "
                    + "to the next rate card and is retried next time: {}", key, e.getMessage());
            return null;
        }
    }

    // Matching

    private static boolean connectionTypeMatches(Pricing p, PricingConnection c, ConnectionType type) {
        if (type == null) {
            return true;
        }
        if (c.getType() == type) {
            return true;
        }
        return mentions(p, type.name());
    }

    /**
     * Whether a price row's commitment term is compatible with the requested {@link Term}.
     * A row that names no {@code termLength} is term-neutral (it carries no evidence of a
     * mismatch) and a caller that requested no term accepts any row; only an explicitly
     * different {@code termLength} is a mismatch - usable solely as a labelled fallback via
     * {@link #withTermSubstitutionNote}.
     */
    private static boolean termMatches(Pricing p, Term term) {
        return term == null || p.getTermLength() == null || p.getTermLength() == term.months();
    }

    /**
     * Labels a quote resolved from a row of a <em>different</em> commitment term than requested,
     * so a term-substituted figure is never mistaken for a term-matched one.
     */
    private static PriceQuote withTermSubstitutionNote(PriceQuote quote, Integer rowTermLength, Term requested) {
        String base = quote.getNote() == null ? "" : quote.getNote();
        return quote.withNote(base + " (termLength " + rowTermLength
                + " substituted for requested " + requested.months() + ")");
    }

    /**
     * Structural router-package match: the row's {@code router/package/code} must equal the
     * requested package code exactly (case-insensitive on the enum name). A {@code null} request
     * matches any package; a row without a structured package can never satisfy a package-specific
     * request - a substring match over code/name is not safe ({@code STANDARD} is a substring of
     * {@code NONSTANDARD}).
     */
    private static boolean routerPackageMatches(Pricing p, String packageCode) {
        if (packageCode == null) {
            return true;
        }
        PricingGateway gateway = p.getGateway();
        if (gateway == null || gateway.getGatewayPackage() == null || gateway.getGatewayPackage().getCode() == null) {
            return false;
        }
        return gateway.getGatewayPackage().getCode().name().equalsIgnoreCase(packageCode);
    }

    /**
     * Structural router-metro match: the row's {@code router/location/metroCode} must equal the
     * requested metro. A {@code null} request matches any metro; a row without a structured
     * location can never satisfy a metro-specific request.
     */
    private static boolean routerMetroMatches(Pricing p, MetroCode metro) {
        if (metro == null) {
            return true;
        }
        PricingGateway gateway = p.getGateway();
        return gateway != null && gateway.getLocation() != null && gateway.getLocation().getMetroCode() == metro;
    }

    /**
     * Whether a price row belongs to the requested metro. The metro code is matched
     * against a whole delimited token of the row's {@code code}/{@code name}, never a
     * bare substring: a metro code is only two letters (e.g. {@code AT}, {@code DE},
     * {@code LA}) and a plain {@code contains} false-positives inside unrelated words
     * ("Gateway" contains "at", "model" contains "de"), which would return a wrong-metro
     * quote tagged authoritative and suppress the correct fallback.
     */
    private static boolean matchesMetro(Pricing p, MetroCode metro) {
        String code = metro.name();
        PricingConnection c = p.getConnection();
        if (c != null) {
            for (PricingConnection.PricingAccessPoint side : java.util.Arrays.asList(c.getASide(), c.getZSide())) {
                if (side != null && side.getAccessPoint() != null && side.getAccessPoint().getLocation() != null
                        && side.getAccessPoint().getLocation().getMetroCode() != null) {
                    // A structured descriptor is authoritative: match on it alone.
                    return metro.equals(side.getAccessPoint().getLocation().getMetroCode());
                }
            }
        }
        return containsToken(p.getCode(), code) || containsToken(p.getName(), code);
    }

    /** Whether {@code haystack}, split on any non-alphanumeric delimiter, contains {@code token} as a whole token (case-insensitive). */
    private static boolean containsToken(String haystack, String token) {
        if (haystack == null || haystack.isEmpty() || token == null || token.isEmpty()) {
            return false;
        }
        for (String part : haystack.split("[^A-Za-z0-9]+")) {
            if (part.equalsIgnoreCase(token)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Case-insensitive <em>substring</em> match of {@code token} against the row's
     * {@code code}/{@code name}. Used only for connection-type tokens, which are
     * multi-character product identifiers (e.g. {@code EVPL_VC}); metros must use
     * {@link #matchesMetro} and router packages {@link #routerPackageMatches} instead - 
     * a short substring ({@code AT}, {@code STANDARD} inside {@code NONSTANDARD}) is
     * not safe.
     */
    private static boolean mentions(Pricing p, String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        String needle = token.toLowerCase();
        String code = p.getCode() == null ? "" : p.getCode().toLowerCase();
        String name = p.getName() == null ? "" : p.getName().toLowerCase();
        return code.contains(needle) || name.contains(needle);
    }

    /**
     * Maps a price row to a quote, or {@link Optional#empty()} when the row carries no
     * charge this card can price - i.e. no {@link ChargeFrequency#MONTHLY_RECURRING} or
     * {@link ChargeFrequency#NON_RECURRING} charge with a non-null price (null/empty
     * charges, or only unmapped frequencies). Returning empty rather than a $0 quote
     * keeps "no priced charge found" distinct from a genuine $0, so the lookup defers to
     * the fallback instead of asserting a phantom free price tagged authoritative.
     */
    private static Optional<PriceQuote> toQuote(Pricing p) {
        BigDecimal monthly = BigDecimal.ZERO;
        BigDecimal nonRecurring = BigDecimal.ZERO;
        boolean priced = false;
        if (p.getCharges() != null) {
            for (Charge charge : p.getCharges()) {
                if (charge == null || charge.getPrice() == null || charge.getType() == null) {
                    continue;
                }
                // A row may carry several charges of the same frequency (e.g. a base fee plus a
                // surcharge, both MONTHLY_RECURRING): they SUM into the component. Assigning
                // last-wins would silently drop all but the final charge of each frequency.
                if (charge.getType() == ChargeFrequency.MONTHLY_RECURRING) {
                    monthly = monthly.add(charge.getPrice());
                    priced = true;
                } else if (charge.getType() == ChargeFrequency.NON_RECURRING) {
                    nonRecurring = nonRecurring.add(charge.getPrice());
                    priced = true;
                }
            }
        }
        if (!priced) {
            return Optional.empty();
        }
        return Optional.of(PriceQuote.of(monthly, nonRecurring, parseCurrency(p.getCurrency()), PriceSource.EQUINIX_LIVE)
                .withNote("Equinix price " + p.getCode()));
    }

    private static Currency parseCurrency(String code) {
        if (code != null) {
            try {
                return Currency.getInstance(code);
            } catch (IllegalArgumentException ignored) {
                // Unknown currency code - fall through to the default.
            }
        }
        return DEFAULT_CURRENCY;
    }
}
