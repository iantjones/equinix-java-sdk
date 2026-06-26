package api.equinix.javasdk.design.value.ratecard;

/**
 * Identifies where a {@link PriceQuote} came from, so every number in a cost
 * estimate or savings calculation is traceable to its origin and disclaimed
 * appropriately.
 *
 * <p>Provenance matters because the SDK blends authoritative Equinix pricing
 * (from the live Fabric Pricing API) with reference and customer-supplied
 * figures that the SDK cannot validate. A consumer should treat
 * {@link #EQUINIX_LIVE} and {@link #CUSTOM} as trustworthy and
 * {@link #REFERENCE}/{@link #ESTIMATE} as indicative only.</p>
 */
public enum PriceSource {

    /** Resolved from the live Equinix Fabric Pricing API ({@code fabric.prices()}). Authoritative. */
    EQUINIX_LIVE,

    /** Supplied by the caller via a {@link CustomRateCard} — e.g. negotiated contract rates. Trusted by definition. */
    CUSTOM,

    /** Fetched from a cloud provider's public pricing API (AWS Price List, Azure Retail Prices, GCP Billing Catalog). */
    PROVIDER_API,

    /** Drawn from a bundled, dated reference rate card. Indicative — confirm against the provider before relying on it. */
    REFERENCE,

    /** Produced by a built-in heuristic when no rate card could price the item. Rough estimate only. */
    ESTIMATE,

    /** Aggregated from quotes of differing sources (see the individual line items for their origins). */
    COMPOSITE
}
