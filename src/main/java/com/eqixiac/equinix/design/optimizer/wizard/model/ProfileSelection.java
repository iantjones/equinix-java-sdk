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

package com.eqixiac.equinix.design.optimizer.wizard.model;

import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * How the wizard chose a service profile and a billable bandwidth for one provider connection — the
 * bandwidth-aware selection made visible rather than hidden. It records the raw requirement, the tier
 * the connection was stamped at (which may be <em>rounded up</em> from the requirement), and every
 * candidate profile that could carry the bandwidth, so an interactive layer can confirm the round-up
 * or prompt the user to pick a different covering profile.
 *
 * <h3>Round-up is surfaced, never silent</h3>
 * <p>When a requirement has no exact service-profile tier the wizard selects the smallest tier that
 * satisfies it (3000&nbsp;&rarr;&nbsp;5000) rather than erroring — but rounding up increases the billed
 * bandwidth, so it is recorded here ({@code isRoundedUp()}, {@link #roundedUpByMbps()}) and rendered
 * in the plan. A non-interactive caller still gets a valid plan (the smallest covering tier); an
 * interactive caller can confirm the increase before provisioning.</p>
 *
 * <h3>Choice is exposed, with a valid default</h3>
 * <p>{@code getAlternatives()} lists every covering candidate, the wizard's default first. When more
 * than one candidate can carry the bandwidth ({@link #hasChoice()}), the MCP layer may elicit a choice;
 * either way {@code getSelectedProfileUuid()} / {@code getSelectedSellerRegion()} are the default the
 * plan is built with, so the plan is always executable without interaction.</p>
 */
@Value
@Builder
public class ProfileSelection {

    /** The raw bandwidth requirement in Mbps that selection started from (before any round-up). */
    int requestedMbps;

    /** The uuid of the profile the wizard selected as the default. */
    String selectedProfileUuid;

    /** The seller region the wizard pinned (the selected profile's first region), or {@code null}. */
    String selectedSellerRegion;

    /**
     * The bandwidth the connection was actually stamped (and will be priced/billed) at — the selected
     * profile's smallest covering tier. Equal to {@link #requestedMbps} when an exact tier or custom
     * bandwidth existed; larger when the requirement was rounded up.
     */
    int selectedTierMbps;

    /** Whether {@link #selectedTierMbps} exceeds {@link #requestedMbps} — i.e. the bandwidth was rounded up. */
    boolean roundedUp;

    /**
     * Every service profile that can carry the requested bandwidth, ordered wizard-default first
     * (tightest rounded fit, then tightest overall ceiling, then the optimizer's default winner, then
     * a deterministic uuid tie-break). Always contains at least the selected profile. When it holds
     * more than one entry there is a genuine choice for an interactive layer to resolve.
     */
    List<ProfileCandidate> alternatives;

    /** A human/LLM-readable explanation of the selection, including any round-up and its cost impact. */
    String reasoning;

    /**
     * Whether there is more than one covering candidate — a genuine decision point an interactive layer
     * (the MCP server) can elicit, rather than a forced single choice.
     *
     * @return {@code true} when at least two profiles cover the requested bandwidth
     */
    public boolean hasChoice() {
        return alternatives != null && alternatives.size() > 1;
    }

    /**
     * How many Mbps the billed bandwidth was rounded up by, or {@code 0} when no round-up occurred.
     *
     * @return {@code selectedTierMbps - requestedMbps}, floored at 0
     */
    public int roundedUpByMbps() {
        return Math.max(0, selectedTierMbps - requestedMbps);
    }
}
