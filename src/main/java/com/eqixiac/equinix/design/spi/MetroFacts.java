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

package com.eqixiac.equinix.design.spi;

import com.eqixiac.equinix.core.enums.Region;
import com.eqixiac.equinix.core.model.MetroId;

import java.util.Set;

/**
 * What a run knows about one metro, taken from the live Fabric catalogue.
 *
 * @param id         the metro
 * @param name       display name, for example {@code Ashburn}
 * @param region     AMER, EMEA or APAC; may be {@code null}
 * @param country    country name as Fabric reports it; may be {@code null}
 * @param latitude   decimal degrees; may be {@code null}
 * @param longitude  decimal degrees; may be {@code null}
 * @param geoZones   Fabric geo scopes / geo zones the metro belongs to (for example {@code EU}); never {@code null}
 */
public record MetroFacts(MetroId id, String name, Region region, String country, Double latitude, Double longitude,
                         Set<String> geoZones) {

    /** Requires an id and makes {@code geoZones} immutable and non-null. */
    public MetroFacts {
        if (id == null) {
            throw new IllegalArgumentException("A metro needs an id");
        }
        geoZones = geoZones == null ? Set.of() : Set.copyOf(geoZones);
    }

    /** @return {@code true} when both coordinates are present */
    public boolean hasCoordinates() {
        return latitude != null && longitude != null;
    }
}
