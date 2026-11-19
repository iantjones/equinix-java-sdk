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

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Objects;

/**
 * A location in a requirement or an answer: a named location at one level, or an area given as a
 * point and a radius.
 *
 * <p>A region is {@code AMER}, {@code EMEA}, or {@code APAC}. A country is an ISO 3166-1 alpha-2
 * code. A metro, a campus, and a facility use the provider's own codes, as the provider's location
 * list gives them.
 *
 * @param level the level, or {@link LocationLevel#AREA}
 * @param code the code of a named location, or {@code null} for an area
 * @param scheme {@link LocationScheme#STANDARD} for a metro given as a UN/LOCODE, otherwise
 *     {@code null}
 * @param center the center of an area, or {@code null} for a named location
 * @param radiusKm the radius of an area in kilometers, or {@code null} for a named location
 */
public record Location(LocationLevel level, String code, LocationScheme scheme, GeoPoint center, Double radiusKm) {

    /**
     * Creates a location.
     *
     * @throws IllegalArgumentException if an area has no center or radius, or a named location has
     *     no code
     */
    public Location {
        Objects.requireNonNull(level, "level");
        if (level == LocationLevel.AREA) {
            if (center == null || radiusKm == null || radiusKm <= 0 || radiusKm > 1000) {
                throw new IllegalArgumentException("An area needs a center and a radius of up to 1000 km");
            }
        } else if (code == null || code.length() < 2) {
            throw new IllegalArgumentException("A named location needs a code of at least 2 characters");
        }
    }

    private static Location named(LocationLevel level, String code) {
        return new Location(level, code, null, null, null);
    }

    /**
     * Returns a region.
     *
     * @param code {@code AMER}, {@code EMEA}, or {@code APAC}
     * @return the location
     */
    public static Location region(String code) {
        return named(LocationLevel.REGION, code);
    }

    /**
     * Returns a country.
     *
     * @param code the ISO 3166-1 alpha-2 code
     * @return the location
     */
    public static Location country(String code) {
        return named(LocationLevel.COUNTRY, code);
    }

    /**
     * Returns a metro by the provider's own code.
     *
     * @param code the provider's metro code
     * @return the location
     */
    public static Location metro(String code) {
        return named(LocationLevel.METRO, code);
    }

    /**
     * Returns a metro by its UN/LOCODE, for use without knowing the provider's code.
     *
     * @param unLocode the UN/LOCODE without a space, for example {@code DEFRA}
     * @return the location
     */
    public static Location metroByUnLocode(String unLocode) {
        return new Location(LocationLevel.METRO, unLocode, LocationScheme.STANDARD, null, null);
    }

    /**
     * Returns a campus.
     *
     * @param code the provider's campus code
     * @return the location
     */
    public static Location campus(String code) {
        return named(LocationLevel.CAMPUS, code);
    }

    /**
     * Returns a facility.
     *
     * @param code the provider's facility code
     * @return the location
     */
    public static Location facility(String code) {
        return named(LocationLevel.FACILITY, code);
    }

    /**
     * Returns an area: everything within a distance of a point.
     *
     * @param latitude degrees north
     * @param longitude degrees east
     * @param radiusKm the distance in kilometers, up to 1000
     * @return the location
     */
    public static Location within(double latitude, double longitude, double radiusKm) {
        return new Location(LocationLevel.AREA, null, null, new GeoPoint(latitude, longitude), radiusKm);
    }

    /**
     * Says whether the location is an area.
     *
     * @return {@code true} for a point and a radius
     */
    @JsonIgnore
    public boolean isArea() {
        return level == LocationLevel.AREA;
    }
}
