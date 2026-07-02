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

package api.equinix.javasdk.fabric.model.implementation;

import api.equinix.javasdk.fabric.enums.PrecisionTimeType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * Precision Time product configuration of a price row (the Fabric v4 {@code TimeServicePrice}
 * schema): protocol type, package and the connection access point the price applies to
 * ({@code TimeServicePriceConnection} / {@code TimeServicePriceConnectionASide} /
 * {@code TimeServicePriceConnectionAccessPoint}).
 *
 * @author ianjones
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class TimeServicePrice {

    @JsonProperty("type")
    private PrecisionTimeType type;

    @JsonProperty("package")
    private PrecisionTimePackageRequest timePackage;

    @JsonProperty("connection")
    private Connection connection;

    /**
     * Time Service price connection configuration (the {@code TimeServicePriceConnection} schema).
     */
    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Connection {

        @JsonProperty("aSide")
        private ASide aSide;
    }

    /**
     * A-side of the Time Service price connection (the {@code TimeServicePriceConnectionASide}
     * schema).
     */
    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ASide {

        @JsonProperty("accessPoint")
        private AccessPoint accessPoint;
    }

    /**
     * Access point of the Time Service price connection (the
     * {@code TimeServicePriceConnectionAccessPoint} schema).
     */
    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AccessPoint {

        @JsonProperty("location")
        private PriceLocation location;
    }
}
