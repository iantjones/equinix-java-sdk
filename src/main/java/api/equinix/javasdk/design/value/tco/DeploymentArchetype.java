package api.equinix.javasdk.design.value.tco;

/**
 * The three deployment approaches the TCO model compares. The value-realization
 * story is the delta between them — chiefly how each treats cloud data egress.
 */
public enum DeploymentArchetype {

    PUBLIC_CLOUD_INTERNET("Public cloud over internet"),

    ON_PREM("On-premises / self-managed"),

    EQUINIX_INTERCONNECT("Equinix interconnected");

    private final String displayName;

    DeploymentArchetype(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
