package api.equinix.javasdk.design.value.ratecard;

/**
 * The network path data egress takes when leaving a cloud provider. Cloud providers
 * charge a high per-GB rate for traffic that exits to the public internet and a lower
 * rate for the same bytes over a dedicated interconnect (AWS Direct Connect, Azure
 * ExpressRoute, Google Cloud Interconnect), which an Equinix Fabric connection provides.
 */
public enum EgressPath {

    /** Egress to the public internet — the provider's headline per-GB data-transfer-out rate. */
    INTERNET,

    /**
     * Egress over a dedicated private interconnect — AWS Direct Connect, Azure ExpressRoute,
     * Google Cloud Interconnect, or OCI FastConnect — typically reached via an Equinix Fabric
     * connection and billed at a substantially lower per-GB rate.
     */
    PRIVATE
}
