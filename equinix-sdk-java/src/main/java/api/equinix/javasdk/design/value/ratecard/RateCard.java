package api.equinix.javasdk.design.value.ratecard;

import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.fabric.enums.ConnectionType;

import java.util.Arrays;
import java.util.Optional;

/**
 * A source of unit prices for the design-time cost, savings, and TCO models.
 *
 * <p>A rate card resolves the price of an individual planned resource — a Fabric
 * connection or a Cloud Router — for a given bandwidth, metro, and commitment
 * {@link Term}. Lookups return an {@link Optional}: an empty result means
 * <em>this card cannot price that item</em>, which is deliberately distinct from
 * a zero price. That distinction lets callers fall back to another source rather
 * than silently treating "unknown" as "free."</p>
 *
 * <p>Implementations differ by provenance (see {@link PriceSource}):</p>
 * <ul>
 *   <li>{@link CustomRateCard} — caller-supplied rates (e.g. negotiated contract pricing), built fluently.</li>
 *   <li>{@code EquinixRateCard} — live {@code fabric.prices()} lookups (authoritative).</li>
 *   <li>{@code ReferenceRateCard} — bundled, dated reference figures (indicative).</li>
 * </ul>
 *
 * <p>Use {@link #layered(RateCard...)} to combine several cards into a precedence
 * chain — the first card that can price an item wins.</p>
 */
public interface RateCard {

    /**
     * Resolves the price of a single Fabric connection / virtual connection.
     *
     * @param type          the connection type (e.g. {@code EVPL_VC})
     * @param bandwidthMbps the provisioned bandwidth in Mbps
     * @param metro         the metro the connection originates in (may be {@code null} if metro-agnostic)
     * @param term          the commitment term
     * @return the resolved quote, or {@link Optional#empty()} if this card cannot price the connection
     */
    Optional<PriceQuote> connection(ConnectionType type, int bandwidthMbps, MetroCode metro, Term term);

    /**
     * Resolves the price of a single Fabric Cloud Router instance.
     *
     * @param packageCode the router package / tier code (e.g. {@code "STANDARD"})
     * @param metro       the metro the router is deployed in (may be {@code null} if metro-agnostic)
     * @param term        the commitment term
     * @return the resolved quote, or {@link Optional#empty()} if this card cannot price the router
     */
    Optional<PriceQuote> cloudRouter(String packageCode, MetroCode metro, Term term);

    /**
     * The dominant provenance of prices this card produces. Aggregating cards
     * report {@link PriceSource#COMPOSITE}.
     *
     * @return the price source
     */
    PriceSource source();

    /**
     * Combines several rate cards into a single card that consults each in order
     * and returns the first non-empty quote. Earlier cards take precedence, so
     * the conventional ordering is most-trusted first: custom → Equinix live →
     * reference.
     *
     * @param cards the cards to chain, in priority order
     * @return a composing rate card
     */
    static RateCard layered(RateCard... cards) {
        return new LayeredRateCard(Arrays.asList(cards));
    }
}
