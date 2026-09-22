package com.eqixiac.equinix.design.value.tco;

import lombok.Getter;

/**
 * The deployment approaches the TCO model compares. The value-realization
 * story is the delta between them — chiefly how each treats cloud data egress.
 *
 * <p>The first three apply to every comparison. {@link #NATIVE_MULTICLOUD_INTERCONNECT}
 * applies only to a cloud-to-cloud comparison (a peer cloud declared with
 * {@code TcoCalculator.Builder.toCloud(...)}), and joins the default set only then.</p>
 */
@Getter
public enum DeploymentArchetype {

    /**
     * Workload in the public cloud, egressing over the public internet. Modelled as the
     * egress volume at the provider's internet per-GB rate (from the resolved rate card —
     * reference figures by default, or provider-API/custom rates when supplied). This is
     * the comparison's savings baseline.
     */
    PUBLIC_CLOUD_INTERNET("Public cloud over internet"),

    /**
     * Self-managed on-premises deployment. Modelled from four coarse inputs — carrier IP
     * transit (per Mbps), amortized hardware, a cross-connect, and power/space (per kW) —
     * sourced from the bundled reference midpoints unless individually overridden on the
     * builder ({@code onPrem*} methods). Reference-only: no live pricing exists for this
     * archetype.
     */
    ON_PREM("On-premises / self-managed"),

    /**
     * Workload reached over an Equinix private interconnect. Modelled as private-path
     * egress plus the Fabric connection (live Equinix pricing where available), an
     * optional Fabric Cloud Router, the caller's colocation primitives (cabinets,
     * cross-connects, per-kW power) when the rate card prices them, and reference
     * fold-ins for the CSP interconnect port and cross-connect fallback.
     */
    EQUINIX_INTERCONNECT("Equinix interconnected"),

    /**
     * Two clouds joined by the providers' own native multicloud link (AWS Interconnect -
     * multicloud paired with the peer provider's counterpart, e.g. a Google Partner Cross-Cloud
     * Interconnect transport). No Equinix resource is involved.
     *
     * <p><b>Beta</b>: the products reached general availability in 2026 and their public price
     * lists are incomplete.</p>
     *
     * <p>Modelled as two flat monthly fees, one per provider, from
     * {@code RateCard.multicloudLink(...)} (hourly list rates converted at
     * {@code MulticloudLinkQuote.HOURS_PER_MONTH} = 730 h/month), plus the per-GB charge on the
     * link from {@code RateCard.egress(..., EgressPath.MULTICLOUD_INTERCONNECT, ...)} applied to
     * the traffic in each direction. When both providers publish a per-GB rate of zero the
     * breakdown carries an explicit zero-cost data-transfer line; that line appears only when
     * both sides are priced. A side without a verifiable rate leaves the archetype partially
     * priced ({@code isPriced() == false}) with a note naming the unpriced side; a partially
     * priced archetype is never recommended. The AWS free tier is applied only on
     * {@code useAwsFreeTier(true)}.</p>
     *
     * <p>Requires a peer cloud: requesting this archetype without
     * {@code TcoCalculator.Builder.toCloud(...)} fails at {@code compare()}. The name avoids the
     * bare word "interconnect", which {@link #EQUINIX_INTERCONNECT} already uses for the
     * Equinix-to-cloud on-ramp.</p>
     */
    NATIVE_MULTICLOUD_INTERCONNECT("Native cloud-to-cloud interconnect");

    private final String displayName;

    DeploymentArchetype(String displayName) {
        this.displayName = displayName;
    }
}
