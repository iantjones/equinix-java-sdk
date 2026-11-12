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

package com.eqixiac.equinix.operations.model.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

/**
 * JSON model for the colocation locations response ({@code locations_details_response}).
 *
 * <p>The spec documents only the {@code crossConnects} array. Live (observed 2026-11-11) the array
 * is named after the requested permission code: {@code crossConnects} for {@code CROSS_CONNECT},
 * {@code workVisits} for {@code WORK_VISIT} and {@code shipments} for {@code SHIPMENTS}. All three
 * are read; {@link #getLocations()} returns whichever the response carried.</p>
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class LocationsDetailsResponseJson {

    @JsonProperty("crossConnects")
    private List<LookupLocationJson> crossConnects;

    @JsonProperty("workVisits")
    private List<LookupLocationJson> workVisits;

    @JsonProperty("shipments")
    private List<LookupLocationJson> shipments;

    /**
     * Returns the location array the response carried, whatever its permission-code key.
     *
     * @return the locations, or an empty list when the response carried none
     */
    public List<LookupLocationJson> getLocations() {
        if (crossConnects != null) {
            return crossConnects;
        }
        if (workVisits != null) {
            return workVisits;
        }
        return shipments != null ? shipments : List.of();
    }
}
