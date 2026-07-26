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
 * <p>The fetch is narrowed <em>server-side</em> by price {@code /type} to the
 * two product families this card prices — virtual connections
 * ({@link PriceType#VIRTUAL_CONNECTION_PRODUCT}) and Cloud Routers
 * ({@link PriceType#CLOUD_ROUTER_PRODUCT}) — rather than pulling the whole
 * catalogue (ports, IP blocks, …) and discarding most of it. A connection price
 * row still carries its connection <em>type</em> and metro only in its
 * {@code code}/{@code name} (the structured descriptor holds just the bandwidth),
 * so the card matches those remaining axes <em>client-side</em> over the (much
 * smaller) cached set. The metro is matched against a whole delimited
 * <em>token</em> of the code/name, never a bare two-letter substring, so a metro
 * code like {@code AT}/{@code DE} cannot false-match an unrelated word. Cloud
 * Router rows carry a structured descriptor and are matched on it: exact
 * package-code equality plus the row's own {@code metroCode}.</p>
 *
 * <p>Both lookups honour the request's remaining axes strictly. When a metro is
 * requested, a row of another metro is never returned — the lookup yields empty
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
 * <p>Each type-scoped query is paged in full and the combined result cached for
 * the card's lifetime <em>once a fetch fully succeeds</em>. A fetch in which any
 * type-scoped query failed is <em>not</em> cached and is retried on the next
 * lookup, so a transient {@code 429}/{@code 5xx}/auth error never permanently
 * poisons the card. A lookup that finds no matching <em>or no priced</em> row
 * simply defers to the fallback — never a wrong or phantom-zero number.</p>
 */
@Slf4j
public final class EquinixRateCard implements RateCard {

    private static final Currency DEFAULT_CURRENCY = Currency.getInstance("USD");

    private final FabricGateway fabric;

    /** Cached only after a fully successful fetch; a failed fetch leaves this {@code null} to force a retry. */
    private volatile List<Pricing> catalog;

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
        // The live catalogue is term-scoped: the same circuit appears once per commitment term at
        // different rates, so a lookup must prefer the row whose termLength matches the requested
        // term — otherwise a 1-month estimate silently picks up a 36-month discounted figure
        // tagged authoritative. A row that names no termLength is term-neutral and acceptable as
        // a match; a row with an explicitly different termLength is only usable as a labelled
        // last resort (its note names the substitution so the caller can see the mismatch).
        PriceQuote termFallback = null;
        Integer termFallbackLength = null;
        for (Pricing p : catalog()) {
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
                return quote;
            }
            if (termFallback == null) {
                termFallback = quote.get();
                termFallbackLength = p.getTermLength();
            }
        }
        // No term-matched row: surface the other-term row only with an explicit substitution
        // label, or empty when nothing matched at all (the layered card consults the next layer).
        if (termFallback == null) {
            return Optional.empty();
        }
        return Optional.of(withTermSubstitutionNote(termFallback, termFallbackLength, term));
    }

    @Override
    public Optional<PriceQuote> cloudRouter(String packageCode, MetroCode metro, Term term) {
        // Cloud Router rows carry a structured descriptor (router package code + location), so the
        // match is structural: exact package-code equality (a substring match would let STANDARD
        // false-match a NONSTANDARD row) and the row's own metroCode against the requested metro
        // (the first priced row of ANY metro is not an answer — an SG lookup must not return a DC
        // price tagged authoritative). No structural match yields empty so a layered card can
        // consult the next layer. Term rows are preferred exactly as in connection().
        PriceQuote termFallback = null;
        Integer termFallbackLength = null;
        for (Pricing p : catalog()) {
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
                return quote;
            }
            if (termFallback == null) {
                termFallback = quote.get();
                termFallbackLength = p.getTermLength();
            }
        }
        if (termFallback == null) {
            return Optional.empty();
        }
        return Optional.of(withTermSubstitutionNote(termFallback, termFallbackLength, term));
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
     * A successful fetch — even one that legitimately returns no rows — clears
     * this. Lets callers distinguish an empty catalogue from a failed fetch.
     *
     * @return {@code true} if the last catalogue fetch failed and rows may be missing
     */
    public boolean isLiveSourceUnavailable() {
        return liveSourceUnavailable;
    }

    // ── Catalogue ──

    private List<Pricing> catalog() {
        List<Pricing> local = catalog;
        if (local != null) {
            return local;
        }
        synchronized (this) {
            if (catalog != null) {
                return catalog;
            }
            CatalogFetch fetch = fetchCatalog();
            if (fetch.complete()) {
                // Memoize only a fully successful fetch, so a transient failure is retried
                // on the next lookup instead of being cached for the card's lifetime.
                catalog = fetch.rows();
                liveSourceUnavailable = false;
                return catalog;
            }
            liveSourceUnavailable = true;
            return fetch.rows();
        }
    }

    private CatalogFetch fetchCatalog() {
        // Fetch only the two product families this card actually prices, each narrowed
        // server-side by /type, instead of scanning the entire price catalogue. The two
        // type-scoped result sets are paged in full and combined. The fetch is "complete"
        // only if every type-scoped query succeeded; a partial/failed fetch is not cached.
        List<Pricing> combined = new ArrayList<>();
        boolean complete = true;
        for (PriceType type : List.of(PriceType.VIRTUAL_CONNECTION_PRODUCT, PriceType.CLOUD_ROUTER_PRODUCT)) {
            TypeFetch outcome = fetchByType(type);
            combined.addAll(outcome.rows());
            complete = complete && outcome.succeeded();
        }
        return new CatalogFetch(combined, complete);
    }

    private TypeFetch fetchByType(PriceType type) {
        try {
            // The fetch narrows server-side by /type only. The fabricv4 spec documents no
            // /termLength filter property for POST /prices/search (its search examples filter
            // /type, /connection/* and /router/* exclusively), so terms cannot be narrowed
            // server-side — and the client-side term preference needs the other-term rows
            // present anyway to produce a labelled substitution fallback.
            FilterPropertyList filter = new FilterPropertyList(FilterType.AND).equals("/type", type.name());
            PaginatedFilteredList<Pricing> page = fabric.prices().list(filter);
            // Page through the full filtered result so a match is never missed when the
            // type's price list exceeds one page. A null page is a genuine empty result
            // (no rows of this type), distinct from a fetch failure below.
            List<Pricing> rows = (page == null)
                    ? new ArrayList<>()
                    : new ArrayList<>(page.loadAll().toList());
            return new TypeFetch(rows, true);
        } catch (RuntimeException e) {
            log.warn("Could not fetch Equinix {} prices from the live Fabric Pricing API; the live rate card "
                    + "will report the live source as unavailable and retry on the next lookup", type, e);
            return new TypeFetch(new ArrayList<>(), false);
        }
    }

    /** The outcome of the whole catalogue fetch: the combined rows and whether every type-scoped query succeeded. */
    private record CatalogFetch(List<Pricing> rows, boolean complete) {}

    /** The outcome of one type-scoped query: its rows and whether the fetch succeeded (vs. errored). */
    private record TypeFetch(List<Pricing> rows, boolean succeeded) {}

    // ── Matching ──

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
     * different {@code termLength} is a mismatch — usable solely as a labelled fallback via
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
     * request — a substring match over code/name is not safe ({@code STANDARD} is a substring of
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
     * {@link #matchesMetro} and router packages {@link #routerPackageMatches} instead —
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
     * charge this card can price — i.e. no {@link ChargeFrequency#MONTHLY_RECURRING} or
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
                // Unknown currency code — fall through to the default.
            }
        }
        return DEFAULT_CURRENCY;
    }
}
