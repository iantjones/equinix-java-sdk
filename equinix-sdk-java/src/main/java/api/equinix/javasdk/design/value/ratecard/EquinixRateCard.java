package api.equinix.javasdk.design.value.ratecard;

import api.equinix.javasdk.FabricGateway;
import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.core.http.response.PaginatedFilteredList;
import api.equinix.javasdk.fabric.enums.ChargeFrequency;
import api.equinix.javasdk.fabric.enums.ConnectionType;
import api.equinix.javasdk.fabric.enums.PriceType;
import api.equinix.javasdk.fabric.model.Pricing;
import api.equinix.javasdk.fabric.model.implementation.Charge;
import api.equinix.javasdk.fabric.model.implementation.PricingConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Optional;

/**
 * A {@link RateCard} backed by the live Equinix Fabric Pricing API
 * ({@code fabric.prices()}). This is the authoritative source of Equinix-side
 * costs for the value-realization models, and the keystone that replaces the
 * hardcoded price literals previously baked into the optimizer and wizard.
 *
 * <p>The Fabric prices search exposes no documented price-specific filter
 * fields, and a price row carries its connection <em>type</em> and metro only
 * in its {@code code}/{@code name} (the structured descriptor holds just the
 * bandwidth). This card therefore fetches the price catalogue once, caches it,
 * and matches <em>client-side</em> on the structured bandwidth plus the
 * type/metro tokens in the code — rather than trusting an unverified
 * server-side filter.</p>
 *
 * <p>The card is deliberately fault-tolerant: if the catalogue cannot be
 * fetched (network error, unauthenticated client, endpoint unavailable), it
 * yields {@link Optional#empty()} for every lookup so callers fall back to
 * another rate card or a heuristic rather than failing. Every quote it does
 * produce is tagged {@link PriceSource#EQUINIX_LIVE}.</p>
 *
 * <p>The catalogue fetch pages through all of {@code prices/search} and caches the
 * result for the card's lifetime. Targeted server-side filtering by price type/bandwidth
 * is a further optimization once those prices filter fields are confirmed against the
 * Fabric spec; until then a miss simply defers to the fallback, never a wrong number.</p>
 */
public final class EquinixRateCard implements RateCard {

    private static final Logger log = LoggerFactory.getLogger(EquinixRateCard.class);
    private static final Currency DEFAULT_CURRENCY = Currency.getInstance("USD");

    private final FabricGateway fabric;
    private volatile List<Pricing> catalog;

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
        Pricing bandwidthAndTypeMatch = null;
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
            // A row that also matches the metro is the most specific result.
            if (metro != null && mentions(p, metro.name())) {
                return Optional.of(toQuote(p));
            }
            if (bandwidthAndTypeMatch == null) {
                bandwidthAndTypeMatch = p;
            }
        }
        return bandwidthAndTypeMatch == null ? Optional.empty() : Optional.of(toQuote(bandwidthAndTypeMatch));
    }

    @Override
    public Optional<PriceQuote> cloudRouter(String packageCode, MetroCode metro, Term term) {
        for (Pricing p : catalog()) {
            if (p.getType() != PriceType.FABRIC_GATEWAY_PRODUCT) {
                continue;
            }
            if (packageCode != null && !mentions(p, packageCode)) {
                continue;
            }
            return Optional.of(toQuote(p));
        }
        return Optional.empty();
    }

    @Override
    public PriceSource source() {
        return PriceSource.EQUINIX_LIVE;
    }

    // ── Catalogue ──

    private List<Pricing> catalog() {
        List<Pricing> local = catalog;
        if (local == null) {
            synchronized (this) {
                local = catalog;
                if (local == null) {
                    local = fetchCatalog();
                    catalog = local;
                }
            }
        }
        return local;
    }

    private List<Pricing> fetchCatalog() {
        try {
            PaginatedFilteredList<Pricing> page = fabric.prices().list(null);
            if (page != null) {
                // Page through the entire catalogue (not just the first page) so a match is never
                // missed when the catalogue exceeds one page; fetched once and cached for the
                // card's lifetime.
                return new ArrayList<>(page.loadAll().toList());
            }
        } catch (RuntimeException e) {
            log.debug("Could not fetch Equinix price catalogue; live rate card will yield no prices", e);
        }
        return new ArrayList<>();
    }

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

    private static boolean mentions(Pricing p, String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        String needle = token.toLowerCase();
        String code = p.getCode() == null ? "" : p.getCode().toLowerCase();
        String name = p.getName() == null ? "" : p.getName().toLowerCase();
        return code.contains(needle) || name.contains(needle);
    }

    private static PriceQuote toQuote(Pricing p) {
        BigDecimal monthly = BigDecimal.ZERO;
        BigDecimal nonRecurring = BigDecimal.ZERO;
        if (p.getCharges() != null) {
            for (Charge charge : p.getCharges()) {
                if (charge == null || charge.getPrice() == null || charge.getType() == null) {
                    continue;
                }
                if (charge.getType() == ChargeFrequency.MONTHLY_RECURRING) {
                    monthly = charge.getPrice();
                } else if (charge.getType() == ChargeFrequency.NON_RECURRING) {
                    nonRecurring = charge.getPrice();
                }
            }
        }
        return PriceQuote.of(monthly, nonRecurring, parseCurrency(p.getCurrency()), PriceSource.EQUINIX_LIVE)
                .withNote("Equinix price " + p.getCode());
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
