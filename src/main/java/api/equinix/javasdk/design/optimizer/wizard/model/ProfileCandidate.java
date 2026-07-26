/*
 * Copyright 2021 Ian Jones. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package api.equinix.javasdk.design.optimizer.wizard.model;

import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * One service profile that can carry a given connection's bandwidth, as a typed decision point the
 * wizard exposes instead of silently picking. When several profiles cover a connection's speed in
 * genuinely different ways — a hosted profile versus a dedicated one at the same tier, or the same
 * profile in different seller regions — every covering candidate is carried on the connection's
 * {@link ProfileSelection} {@code alternatives} so an interactive layer (the MCP server) can
 * prompt the user to choose, while a non-interactive caller takes the first (the wizard's default).
 *
 * <p>Each candidate reports the exact tier <em>this</em> profile would use for the requested bandwidth
 * ({@code coveringTierMbps} — the round-up result for this profile), so the
 * caller can compare not just which profiles are eligible but how much each would over-provision.</p>
 */
@Value
@Builder
public class ProfileCandidate {

    /** The Fabric service-profile uuid this candidate refers to. */
    String serviceProfileUuid;

    /**
     * The seller regions this profile publishes in the metro. The wizard's default pins the first;
     * an interactive layer can offer the full list. Paired with {@link #serviceProfileUuid} — never
     * spliced across profiles.
     */
    List<String> sellerRegions;

    /**
     * The bandwidth this candidate would be billed at for the requested speed — the smallest tier it
     * can build that satisfies the requirement (equal to the request when an exact tier or custom
     * bandwidth exists, larger when the request was rounded up).
     */
    int coveringTierMbps;

    /** The discrete bandwidth tiers this profile publishes, or empty/{@code null} when it publishes none. */
    List<Integer> supportedBandwidths;

    /** Whether this profile allows a custom (non-tiered) bandwidth. */
    boolean allowCustomBandwidth;

    /** The per-metro maximum VC speed in Mbps for this metro, or {@code null} when the metro sets none. */
    Integer vcBandwidthMax;

    /** The first published seller region, or {@code null} when the profile publishes none. */
    public String firstSellerRegion() {
        return sellerRegions != null && !sellerRegions.isEmpty() ? sellerRegions.get(0) : null;
    }
}
