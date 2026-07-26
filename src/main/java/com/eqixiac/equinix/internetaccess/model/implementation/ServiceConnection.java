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

package com.eqixiac.equinix.internetaccess.model.implementation;

import com.eqixiac.equinix.internetaccess.enums.ConnectionType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * A connection of an Equinix Internet Access (EIA) v2 service, as returned in the service read
 * model (the {@code ConnectionReadModel}: uuid, href, type and A-side).
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ServiceConnection {

    @JsonProperty("uuid")
    private String uuid;

    @JsonProperty("href")
    private String href;

    @JsonProperty("type")
    private ConnectionType type;

    @JsonProperty("aSide")
    private ASide aSide;

    /**
     * A-side of a {@link ServiceConnection}: the originating access-point type and service.
     */
    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ASide {

        @JsonProperty("type")
        private String type;

        @JsonProperty("service")
        private ASideService service;
    }

    /**
     * Service referenced on the A-side of a {@link ServiceConnection}.
     */
    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ASideService {

        @JsonProperty("uuid")
        private String uuid;
    }
}
