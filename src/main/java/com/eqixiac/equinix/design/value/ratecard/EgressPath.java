package com.eqixiac.equinix.design.value.ratecard;

/**
 * The network path data egress takes when leaving a cloud provider. Cloud providers
 * charge a high per-GB rate for traffic that exits to the public internet and a lower
 * rate for the same bytes over a dedicated interconnect (AWS Direct Connect, Azure
 * ExpressRoute, Google Cloud Interconnect), which an Equinix Fabric connection provides.
 *
 * <table>
 *   <caption>Paths and the charge model each one represents</caption>
 *   <tr><th>Path</th><th>Per-GB charge</th><th>Fixed charge modelled elsewhere</th></tr>
 *   <tr><td>{@link #INTERNET}</td><td>provider internet data-transfer-out rate</td><td>none</td></tr>
 *   <tr><td>{@link #PRIVATE}</td><td>provider private-interconnect rate</td>
 *       <td>Fabric connection, Cloud Router, CSP port ({@code RateCard.connection},
 *       {@code RateCard.cloudRouter}, {@code ReferenceRateCard.cspInterconnectPortMonthly})</td></tr>
 *   <tr><td>{@link #MULTICLOUD_INTERCONNECT}</td><td>per-GB rate on a native provider-to-provider
 *       link (zero on the products bundled in {@code ReferenceRateCard})</td>
 *       <td>two flat hourly fees, one per provider ({@code RateCard.multicloudLink})</td></tr>
 * </table>
 */
public enum EgressPath {

    /** Egress to the public internet — the provider's headline per-GB data-transfer-out rate. */
    INTERNET,

    /**
     * Egress over a dedicated private interconnect — AWS Direct Connect, Azure ExpressRoute,
     * Google Cloud Interconnect, or OCI FastConnect — typically reached via an Equinix Fabric
     * connection and billed at a substantially lower per-GB rate.
     */
    PRIVATE,

    /**
     * Egress over a native provider-to-provider multicloud link: AWS Interconnect - multicloud on
     * the AWS side, a Partner Cross-Cloud Interconnect transport on the Google Cloud side. There
     * is no Equinix resource on this path.
     *
     * <p><b>Beta</b>: the products behind this path reached general availability in 2026 and
     * their price lists are incomplete (see {@code ReferenceRateCard.multicloudLink}).</p>
     *
     * <p>The per-GB rate is a separate lookup from the link's flat fee. The bundled reference
     * data records a verified rate of zero for AWS and Google Cloud, each with its source URL and
     * retrieval date in the rate's note. For any other provider the reference card returns
     * empty (unknown), not zero. The classic Google Cross-Cloud Interconnect product is a
     * different product and still bills data transfer per GiB; it is priced under
     * {@link #PRIVATE}, not here.</p>
     *
     * <p>The live provider-API adapters in {@code design.value.ratecard.provider} do not price
     * this path and return empty for it.</p>
     */
    MULTICLOUD_INTERCONNECT
}
