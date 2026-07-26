package api.equinix.javasdk.design.value.tco;

import lombok.Getter;

/**
 * The three deployment approaches the TCO model compares. The value-realization
 * story is the delta between them — chiefly how each treats cloud data egress.
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
    EQUINIX_INTERCONNECT("Equinix interconnected");

    private final String displayName;

    DeploymentArchetype(String displayName) {
        this.displayName = displayName;
    }
}
