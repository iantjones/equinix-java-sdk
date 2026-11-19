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

/**
 * A point on the earth.
 *
 * @param latitude degrees north, -90 to 90
 * @param longitude degrees east, -180 to 180
 */
public record GeoPoint(double latitude, double longitude) {

    /**
     * Creates a point.
     *
     * @throws IllegalArgumentException if a coordinate is out of range
     */
    public GeoPoint {
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Coordinates out of range: " + latitude + ", " + longitude);
        }
    }
}
