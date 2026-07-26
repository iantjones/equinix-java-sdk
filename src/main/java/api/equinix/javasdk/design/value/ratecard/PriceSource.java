package api.equinix.javasdk.design.value.ratecard;

/**
 * Identifies where a {@link PriceQuote} came from, so every number in a cost
 * estimate or savings calculation is traceable to its origin and disclaimed
 * appropriately.
 *
 * <p>Provenance matters because the SDK blends authoritative Equinix pricing
 * (from the live Fabric Pricing API) with reference and customer-supplied
 * figures that the SDK cannot validate. On the trust spectrum:
 * {@link #EQUINIX_LIVE} and {@link #CUSTOM} are trustworthy for what they
 * cover; {@link #PROVIDER_API} is live and accurate for the <em>cloud-provider</em>
 * side but is public list pricing, not a negotiated rate; and
 * {@link #REFERENCE}/{@link #ESTIMATE} are indicative only. {@link #COMPOSITE}
 * inherits the trust of whatever it aggregated — consult the quote's note and
 * the surrounding disclaimer.</p>
 */
public enum PriceSource {

    /** Live figure from the Equinix Fabric Pricing API — authoritative for Equinix-side costs. */
    EQUINIX_LIVE,

    /** Caller-supplied figure (e.g. a negotiated contract rate) declared on a {@link CustomRateCard}. */
    CUSTOM,

    /**
     * Live figure from a public cloud-provider pricing API (AWS Price List, Azure Retail
     * Prices, GCP Billing Catalog, OCI Price List). Accurate and current for the provider
     * side, but a public <em>list</em> price — negotiated discounts are not reflected.
     */
    PROVIDER_API,

    /** Bundled, dated indicative figure from the {@link ReferenceRateCard} — an estimate, not a quote. */
    REFERENCE,

    /** Heuristic or derived figure with no external source — the least trustworthy provenance. */
    ESTIMATE,

    /**
     * Aggregate over mixed provenances: a {@code RateCard.layered(...)} chain reports this as
     * its dominant source, and {@code PriceQuote.plus(...)} stamps it on a sum whose operands
     * came from different sources.
     */
    COMPOSITE
}
