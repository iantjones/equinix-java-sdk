package api.equinix.javasdk.design.value.tco;

/**
 * The three deployment approaches the TCO model compares. The value-realization
 * story is the delta between them — chiefly how each treats cloud data egress.
 */
public enum DeploymentArchetype {

    /** Public cloud with traffic egressing over the public internet (the do-nothing baseline). */
    PUBLIC_CLOUD_INTERNET("Public cloud over internet"),

    /** Self-managed on-premises / colocation deployment with carrier transit. */
    ON_PREM("On-premises / self-managed"),

    /** Equinix-interconnected: colocation + Fabric + cloud on-ramps with discounted private egress. */
    EQUINIX_INTERCONNECT("Equinix interconnected");

    private final String displayName;

    DeploymentArchetype(String displayName) {
        this.displayName = displayName;
    }

    /** A human-readable label for reports. */
    public String getDisplayName() {
        return displayName;
    }
}
