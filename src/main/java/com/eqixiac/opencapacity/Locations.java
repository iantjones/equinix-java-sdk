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

package com.eqixiac.opencapacity;

import com.eqixiac.opencapacity.model.LocationInfo;
import com.eqixiac.opencapacity.model.LocationLevel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** The locations a provider accepts in a requirement. */
public final class Locations {

    private final Transport transport;

    Locations(Transport transport) {
        this.transport = transport;
    }

    /**
     * Lists every location the provider names.
     *
     * @return the locations, across all pages
     * @throws OpenCapacityException if a request fails
     */
    public List<LocationInfo> list() {
        return list(null, null);
    }

    /**
     * Lists the locations at one level.
     *
     * @param level the level
     * @return the locations at that level, across all pages
     * @throws OpenCapacityException if a request fails
     */
    public List<LocationInfo> list(LocationLevel level) {
        return list(level, null);
    }

    /**
     * Lists the locations at one level inside another location.
     *
     * @param level the level, or {@code null} for every level
     * @param within the code of the containing location, for example a country code, or {@code null}
     * @return the matching locations, across all pages
     * @throws OpenCapacityException if a request fails
     */
    public List<LocationInfo> list(LocationLevel level, String within) {
        StringBuilder query = new StringBuilder("/locations?limit=200");
        if (level != null) {
            query.append("&level=").append(level.name());
        }
        if (within != null) {
            query.append("&within=").append(Transport.encode(within));
        }
        List<LocationInfo> all = new ArrayList<>();
        String cursor = null;
        do {
            String path = cursor == null ? query.toString() : query + "&cursor=" + Transport.encode(cursor);
            Transport.Reply reply = transport.send("GET", path, null, Map.of(), true);
            Wire.Page<LocationInfo> page = transport.read(reply, Transport.type(Wire.Page.class, LocationInfo.class));
            all.addAll(page.items());
            cursor = page.next();
        } while (cursor != null);
        return List.copyOf(all);
    }
}
