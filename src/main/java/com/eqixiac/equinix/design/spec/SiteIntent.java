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

package com.eqixiac.equinix.design.spec;

import com.eqixiac.equinix.design.optimizer.enums.SiteRole;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

/**
 * A place the design must serve: an office, a user population, a factory or an existing data
 * centre. Give either {@code metro} (an Equinix metro code such as {@code "LD"}) or
 * {@code latitude} + {@code longitude}; with neither, latency to the site is unknown and the site
 * is left out of latency scoring and latency limits.
 *
 * @param label      unique name within the spec, used in every result that refers to the site
 * @param metro      nearest Equinix metro code, or {@code null}
 * @param latitude   decimal degrees, or {@code null}
 * @param longitude  decimal degrees, or {@code null}
 * @param role       what the site is, which sets its default importance; may be {@code null}
 * @param headcount  people at the site, used as its weight when {@code weight} is unset; may be {@code null}
 * @param weight     explicit relative importance (positive); overrides headcount and role; may be {@code null}
 */
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SiteIntent(String label, String metro, Double latitude, Double longitude, SiteRole role,
                         Integer headcount, Double weight) {

    /** @return {@code true} when the site carries both coordinates */
    public boolean hasCoordinates() {
        return latitude != null && longitude != null;
    }
}
