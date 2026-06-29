package api.equinix.javasdk.design.value.ratecard;

/**
 * The network path data egress takes when leaving a cloud provider — the lever
 * at the heart of the value-realization savings story. Cloud providers charge a
 * high per-GB rate for traffic that exits to the public internet and a markedly
 * lower rate for the same bytes leaving over a dedicated interconnect (AWS Direct
 * Connect, Azure ExpressRoute, Google Cloud Interconnect), which is what an
 * Equinix Fabric connection provides.
 */
public enum EgressPath {

    /** Data egress to the public internet (the expensive default). */
    INTERNET,

    /** Data egress over a dedicated/private interconnect (DX / ExpressRoute / Interconnect). */
    PRIVATE
}
