package api.equinix.javasdk.design.optimizer.model;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Objects;

/**
 * One candidate Fabric service profile for a given (provider, metro), carrying the bandwidth
 * capability the deployment wizard needs to pick a profile that can actually carry a connection's
 * speed.
 *
 * <p>A provider legitimately publishes several service profiles at the same metro (a hosted
 * Direct-Connect profile capped at 500&nbsp;Mbps and a dedicated profile that lists 1G/10G/100G, for
 * example). The optimizer resolves a single <em>outranks</em> winner for scoring and region
 * preference, but that winner is chosen with zero knowledge of the connection's eventual bandwidth —
 * so a hosted profile can win for a metro and then be paired with a 3000&nbsp;Mbps connection it
 * cannot build. To let the wizard choose a covering profile once the bandwidth is known, EVERY
 * matching profile's capability for the metro is carried forward as one of these options.</p>
 *
 * <p>The {@link #covers(int)} rule mirrors the Layer-1 validator
 * ({@code PlanValidator.checkProfile}) bit-for-bit, so a profile the selector accepts is exactly a
 * profile the validator will not reject, and vice-versa: the per-metro ceiling
 * ({@link #getVcBandwidthMax() vcBandwidthMax}) must not be exceeded, AND the bandwidth must be one
 * of the discrete {@link #getSupportedBandwidths() supportedBandwidths} tiers — unless the profile
 * {@link #isAllowCustomBandwidth() allows custom bandwidth} or publishes no discrete tier list at
 * all, either of which lifts the discrete-tier constraint.</p>
 */
@Value
@Builder
public class ServiceProfileOption {

    /** The Fabric service-profile uuid this candidate refers to. */
    String serviceProfileUuid;

    /**
     * The seller regions this profile publishes in the metro. Paired with
     * {@link #serviceProfileUuid} — the wizard pins the z-side uuid and seller region together from
     * the SAME option, never spliced across profiles.
     */
    List<String> sellerRegions;

    /**
     * The discrete bandwidth tiers this profile supports, aggregated across every access-point-type
     * config (profile-level, metro-independent). Empty when the profile publishes no discrete tier
     * list, which lifts the discrete-tier constraint.
     */
    List<Integer> supportedBandwidths;

    /** Whether any of the profile's access-point-type configs allows a custom (non-tiered) bandwidth. */
    boolean allowCustomBandwidth;

    /** The per-metro maximum VC speed in Mbps for this metro, or {@code null} when the metro sets none. */
    Integer vcBandwidthMax;

    /**
     * Whether this profile can carry a connection of the given bandwidth, applying the SAME rule as
     * {@code PlanValidator.checkProfile} so the selector and the Layer-1 validator agree exactly:
     * the per-metro ceiling must not be exceeded, and the bandwidth must be an allowed discrete tier
     * unless custom bandwidth is allowed or no tier list is published.
     *
     * @param mbps the requested connection bandwidth in Mbps
     * @return {@code true} when a connection of {@code mbps} is buildable on this profile
     */
    public boolean covers(int mbps) {
        if (vcBandwidthMax != null && mbps > vcBandwidthMax) {
            return false;
        }
        if (allowCustomBandwidth) {
            return true;
        }
        if (supportedBandwidths == null || supportedBandwidths.isEmpty()) {
            return true;
        }
        return supportedBandwidths.contains(mbps);
    }

    /**
     * A capacity yardstick for "smallest-capable-first" selection: the largest discrete tier this
     * profile publishes, or — for a custom-bandwidth/tierless profile — its per-metro ceiling, or
     * {@link Integer#MAX_VALUE} when it is unbounded. A smaller value is a tighter fit, so a request
     * both a hosted ([50..500]) and a dedicated profile could carry prefers the hosted one.
     *
     * @return the profile's effective capacity ceiling in Mbps
     */
    public int capacityCeiling() {
        int maxTier = maxSupportedBandwidth();
        if (maxTier > 0) {
            return maxTier;
        }
        return vcBandwidthMax != null ? vcBandwidthMax : Integer.MAX_VALUE;
    }

    /**
     * How many discrete tiers this profile publishes strictly above {@code mbps} — the tie-break for
     * two profiles with the same {@link #capacityCeiling()}: the fewer wasted tiers, the tighter.
     *
     * @param mbps the requested connection bandwidth in Mbps
     * @return the count of published tiers greater than {@code mbps}
     */
    public int excessTiersAbove(int mbps) {
        if (supportedBandwidths == null) {
            return 0;
        }
        return (int) supportedBandwidths.stream()
                .filter(Objects::nonNull)
                .filter(b -> b > mbps)
                .count();
    }

    private int maxSupportedBandwidth() {
        if (supportedBandwidths == null) {
            return 0;
        }
        return supportedBandwidths.stream()
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0);
    }
}
