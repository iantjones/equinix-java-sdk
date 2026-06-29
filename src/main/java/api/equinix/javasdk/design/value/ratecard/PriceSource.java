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

    EQUINIX_LIVE,

    CUSTOM,

    PROVIDER_API,

    REFERENCE,

    ESTIMATE,

    COMPOSITE
}
