package api.equinix.javasdk.design.value.ratecard;

import api.equinix.javasdk.FabricGateway;
import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.fabric.enums.ConnectionType;
import api.equinix.javasdk.fabric.model.implementation.cloud.CloudProviderType;

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
 *   <li>the {@code design.value.ratecard.provider} adapters — live cloud-egress rates from the public
 *       provider pricing APIs ({@code AzureRetailPricesRateCard}, {@code AwsPriceListRateCard},
 *       {@code GcpBillingCatalogRateCard}, {@code OracleCloudPriceListRateCard}), tagged
 *       {@link PriceSource#PROVIDER_API}.</li>
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
     * Resolves the per-GB data-egress rate for a cloud provider over a given path.
     *
     * <p>Equinix does not sell cloud egress, so {@code EquinixRateCard} returns
     * {@link Optional#empty()} here (the default); egress rates come from a
     * {@link CustomRateCard} (your figures), a {@code ReferenceRateCard} (bundled
     * indicative figures), or a provider-pricing-API card. The default returns
     * empty so cards that do not model egress need not override it.</p>
     *
     * @param provider the cloud provider the data is leaving
     * @param region   the provider region (may be {@code null} for a provider-wide rate)
     * @param path     internet vs. private interconnect — the savings lever
     * @param term     the commitment term
     * @return the resolved egress rate, or {@link Optional#empty()} if this card cannot price it
     */
    default Optional<EgressRate> egress(CloudProviderType provider, String region, EgressPath path, Term term) {
        return Optional.empty();
    }

    /**
     * Resolves the price of an Equinix <em>colocation</em> primitive — a cabinet, a kW of power, or
     * a cross-connect (see {@link ColocationItem} for the per-unit semantics). This lets the cost
     * models incorporate the physical-infrastructure side of a colocation-vs-cloud comparison with
     * the caller's real figures, not just the interconnection. Cards that do not model colocation
     * return {@link Optional#empty()} (the default); {@link CustomRateCard} prices whatever the
     * caller declares.
     *
     * @param item  the colocation primitive to price
     * @param metro the metro (may be {@code null} if metro-agnostic)
     * @param term  the commitment term
     * @return the resolved per-unit monthly quote, or {@link Optional#empty()} if not priced
     */
    default Optional<PriceQuote> colocation(ColocationItem item, MetroCode metro, Term term) {
        return Optional.empty();
    }

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

    /**
     * The canonical egress-capable default chain for the value-realization models:
     * live Equinix Fabric pricing first, then the bundled reference figures (which
     * also supply the cloud-egress rates). This is the default used by the savings
     * calculator, TCO comparison, and plan value realization when no rate card is
     * supplied.
     *
     * @param fabric the Fabric client (or any {@link FabricGateway}) for live pricing
     * @return a layered rate card: {@code EquinixRateCard} → {@code ReferenceRateCard}
     */
    static RateCard standardChain(FabricGateway fabric) {
        return layered(EquinixRateCard.of(fabric), ReferenceRateCard.standard());
    }
}
