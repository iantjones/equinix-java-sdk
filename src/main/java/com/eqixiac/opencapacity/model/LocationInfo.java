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
 * A location that a provider accepts in a requirement.
 *
 * @param level the level
 * @param code the code to send in a requirement
 * @param name the display name
 * @param standardCode for a metro, the UN/LOCODE without a space, or {@code null}
 * @param parent the location that contains this one, or {@code null}
 */
public record LocationInfo(
        LocationLevel level, String code, String name, String standardCode, Location parent) {
    /**
     * Returns this location as a reference for a requirement.
     *
     * @return the reference
     */
    public Location toLocation() {
        return new Location(level, code, null, null, null);
    }
}
