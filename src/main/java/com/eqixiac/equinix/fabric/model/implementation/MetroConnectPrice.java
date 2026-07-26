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

package com.eqixiac.equinix.fabric.model.implementation;

import com.eqixiac.equinix.fabric.enums.MetroConnectDestinationType;
import com.eqixiac.equinix.fabric.enums.MetroConnectPathType;
import com.eqixiac.equinix.fabric.enums.MetroConnectType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * Metro Connect product configuration of a price row (the Fabric v4 {@code metroConnect}
 * member of the {@code Price} payload, as returned for {@code METRO_CONNECT_PRODUCT}
 * rows by {@code POST /prices/search}): connect type, bandwidth, path protection,
 * destination type and the A-side/Z-side IBX locations the price applies to.
 *
 * @author ianjones
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class MetroConnectPrice {

    @JsonProperty("type")
    private MetroConnectType type;

    @JsonProperty("bandwidth")
    private Integer bandwidth;

    @JsonProperty("pathType")
    private MetroConnectPathType pathType;

    @JsonProperty("connectionDestinationType")
    private MetroConnectDestinationType connectionDestinationType;

    @JsonProperty("aSide")
    private Side aSide;

    @JsonProperty("zSide")
    private Side zSide;

    /**
     * One side (A-side or Z-side) of the priced Metro Connect: the IBX location.
     */
    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Side {

        @JsonProperty("location")
        private Location location;
    }

    /**
     * IBX location of a Metro Connect side (wire property {@code ibxCode}).
     */
    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Location {

        @JsonProperty("ibxCode")
        private String ibxCode;
    }
}
