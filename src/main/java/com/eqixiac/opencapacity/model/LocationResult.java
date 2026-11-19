/*
 * Copyright 2026 Ian Jones. All Rights Reserved.
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

package com.eqixiac.opencapacity.model;

import java.util.List;

/**
 * The answer for one location. Only the location, the grade, and the feature entries are always
 * present. Every other component is {@code null} or empty when the provider does not disclose it.
 *
 * @param location the location answered, which can be coarser than the one requested
 * @param requested the requested location where the answer is at another level, or {@code null}
 * @param grade the grade for the location
 * @param timing when the capacity can be contracted and used, or {@code null}
 * @param density the density per cabinet the location supports for this requirement, or {@code null}
 * @param cabinets the cabinet band, or {@code null}
 * @param power the power band, or {@code null}
 * @param supportedFeatures one entry for each desired feature
 * @param interconnection the answers for the interconnection parties
 * @param reasons why the grade is not {@code AVAILABLE}
 * @param nextStepUrl where to continue with the provider, or {@code null}
 */
public record LocationResult(
        Location location, Location requested, Grade grade, Timing timing, DensityRange density,
        CabinetBand cabinets, PowerBand power, List<SupportedFeature> supportedFeatures,
        List<InterconnectionResult> interconnection, List<Reason> reasons, String nextStepUrl) {

    /** Creates a result. */
    public LocationResult {
        grade = grade == null ? Grade.NOT_DISCLOSED : grade;
        supportedFeatures = supportedFeatures == null ? List.of() : List.copyOf(supportedFeatures);
        interconnection = interconnection == null ? List.of() : List.copyOf(interconnection);
        reasons = reasons == null ? List.of() : List.copyOf(reasons);
    }

    /**
     * Returns the provider's answer for a feature.
     *
     * @param feature the feature
     * @return the entry for the feature, or an entry graded {@code NOT_DISCLOSED} if the provider
     *     returned none
     */
    public SupportedFeature supportedFeature(Feature feature) {
        for (SupportedFeature candidate : supportedFeatures) {
            if (candidate.feature().equals(feature)) {
                return candidate;
            }
        }
        return new SupportedFeature(feature, Grade.NOT_DISCLOSED, null, null);
    }

    /**
     * Returns the provider's answer for an interconnection party.
     *
     * @param party the party
     * @return the grade for the party, or {@code NOT_DISCLOSED} if the provider returned none
     */
    public Grade presenceOf(Party party) {
        for (InterconnectionResult candidate : interconnection) {
            if (candidate.party() != null && candidate.party().matches(party)) {
                return candidate.presence() == null ? Grade.NOT_DISCLOSED : candidate.presence();
            }
        }
        return Grade.NOT_DISCLOSED;
    }
}
