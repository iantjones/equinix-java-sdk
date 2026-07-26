package com.eqixiac.equinix.design.optimizer.model;

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
 * ({@code vcBandwidthMax}) must not be exceeded, AND the bandwidth must be one
 * of the discrete {@code supportedBandwidths} tiers — unless the profile
 * {@code allowCustomBandwidth} allows custom bandwidth or publishes no discrete tier list at
 * all, either of which lifts the discrete-tier constraint.</p>
 *
 * <p>{@link #covers(int)} answers the <em>exact</em> question ("can this profile build a VC at exactly
 * this speed?"). Bandwidth <em>round-up</em> — the owner's rule that a requirement with no exact tier
 * selects the smallest tier that satisfies it (3000&nbsp;&rarr;&nbsp;5000, never an error) — is a
 * separate primitive, {@link #coveringTier(int)}: the smallest thing this profile can actually build
 * that is not below the requirement. The wizard stamps the connection at that covering tier (which
 * {@code covers()} then accepts exactly), so the round-up selection and the Layer-1 tier check stay in
 * agreement. {@code covers()} is deliberately left exact so it keeps mirroring {@code checkProfile}.</p>
 */
@Value
@Builder
public class ServiceProfileOption {

    /**
     * The {@link #coveringTier(int)} sentinel returned when a profile cannot build the requested
     * bandwidth even after rounding up — the requirement exceeds its largest tier (and its per-metro
     * ceiling). Chosen negative so it sorts below every real tier in a smallest-fit comparator.
     */
    public static final int NO_COVERING_TIER = -1;

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
     * The smallest bandwidth this profile can actually build that <em>satisfies</em> (is not below) the
     * requested speed — the bandwidth round-up primitive. This is what the wizard stamps the connection
     * at when the exact requirement is not an offered tier:
     * <ul>
     *   <li>a discrete-tier profile returns its smallest published tier {@code >= mbps} (so 3000 against
     *       {@code [1000, 5000, 10000]} returns 5000);</li>
     *   <li>a custom-bandwidth ({@code allowCustomBandwidth}) or tierless profile returns {@code mbps}
     *       itself — the exact speed is buildable, so nothing is rounded;</li>
     *   <li>in either case the result is clamped to the per-metro ceiling
     *       ({@code vcBandwidthMax}): a tier above the ceiling is not selectable.</li>
     * </ul>
     * Returns {@link #NO_COVERING_TIER} when even rounding up cannot satisfy the requirement (it exceeds
     * the largest tier this profile publishes and its ceiling). Because the returned tier is always one
     * {@link #covers(int)} accepts exactly, stamping the connection at it keeps the selector and the
     * Layer-1 validator in agreement.
     *
     * @param mbps the requested connection bandwidth in Mbps
     * @return the smallest satisfying tier in Mbps, or {@link #NO_COVERING_TIER} if none exists
     */
    public int coveringTier(int mbps) {
        if (mbps <= 0) {
            return NO_COVERING_TIER;
        }
        // Custom-bandwidth or tierless: the exact requested speed is buildable, up to the ceiling.
        if (allowCustomBandwidth || supportedBandwidths == null || supportedBandwidths.isEmpty()) {
            if (vcBandwidthMax != null && mbps > vcBandwidthMax) {
                return NO_COVERING_TIER;
            }
            return mbps;
        }
        int best = NO_COVERING_TIER;
        for (Integer tier : supportedBandwidths) {
            if (tier == null || tier < mbps) {
                continue;
            }
            if (vcBandwidthMax != null && tier > vcBandwidthMax) {
                continue;
            }
            if (best == NO_COVERING_TIER || tier < best) {
                best = tier;
            }
        }
        return best;
    }

    /**
     * Whether this profile can carry the requested bandwidth <em>after rounding up</em> — i.e.
     * whether {@link #coveringTier(int)} finds a satisfying tier. Distinct from
     * {@link #covers(int)}, which is the exact-tier test: a 3000&nbsp;Mbps request has
     * {@code covers()==false} but {@code canCover()==true} on a profile publishing a
     * 5000&nbsp;Mbps tier.
     *
     * @param mbps the requested connection bandwidth in Mbps
     * @return {@code true} when a satisfying tier exists (exact or rounded up)
     */
    public boolean canCover(int mbps) {
        return coveringTier(mbps) != NO_COVERING_TIER;
    }

    /**
     * The largest bandwidth this profile can build in Mbps — its largest published tier clamped to the
     * per-metro ceiling, the ceiling itself for a custom/tierless profile, or {@link Integer#MAX_VALUE}
     * when it is unbounded. Used to phrase the "requirement exceeds every profile" error with the real
     * maximum a customer could ask for instead.
     *
     * @return the largest buildable bandwidth in Mbps
     */
    public int maxCoverableMbps() {
        if (allowCustomBandwidth || supportedBandwidths == null || supportedBandwidths.isEmpty()) {
            return vcBandwidthMax != null ? vcBandwidthMax : Integer.MAX_VALUE;
        }
        int maxTier = maxSupportedBandwidth();
        if (maxTier <= 0) {
            return vcBandwidthMax != null ? vcBandwidthMax : Integer.MAX_VALUE;
        }
        return vcBandwidthMax != null ? Math.min(maxTier, vcBandwidthMax) : maxTier;
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
