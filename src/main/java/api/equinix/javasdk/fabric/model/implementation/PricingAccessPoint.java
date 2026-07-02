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

import api.equinix.javasdk.fabric.enums.BridgePackageCode;
import api.equinix.javasdk.fabric.enums.AccessPointType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PricingAccessPoint {

    @JsonProperty("uuid")
    private String uuid;

    @JsonProperty("type")
    private AccessPointType type;

    @JsonProperty("location")
    LocationCode location;

    @JsonProperty("profile")
    ServiceProfileRef profile;

    @JsonProperty("port")
    private Port port;

    @JsonProperty("bridge")
    private Bridge bridge;

    /**
     * Access point port pricing attributes (spec schema
     * {@code VirtualConnectionPriceASide_accessPoint_port}).
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Port {

        @JsonProperty("settings")
        private Settings settings;

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Settings {

            @JsonProperty("buyout")
            private Boolean buyout;
        }
    }

    /**
     * Z-side bridge pricing attributes (spec schema
     * {@code VirtualConnectionPriceZSide_accessPoint_bridge}).
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Bridge {

        @JsonProperty("package")
        private BridgePackage bridgePackage;

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class BridgePackage {

            /**
             * Bridge package code; spec enum {@code REGIONAL} / {@code GLOBAL}.
             */
            @JsonProperty("code")
            private BridgePackageCode code;
        }
    }
}
