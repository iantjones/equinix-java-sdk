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

package com.eqixiac.equinix.customerportal.model.json.creators;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * A cross-connect entry for a smart hands patch-cable-removal order
 * ({@code cross-connect-removal} in the smart hands v1 spec). {@code serialNumber},
 * {@code deviceCabinet}, {@code deviceConnectorType}, {@code deviceDetails} and
 * {@code devicePort} are required; {@code removePatchCableWithLiveTraffic} and
 * {@code scopeOfWork} are optional.
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SmartHandsCrossConnectRemoval {

    @JsonProperty("serialNumber")
    private final String serialNumber;

    @JsonProperty("deviceCabinet")
    private final String deviceCabinet;

    @JsonProperty("deviceConnectorType")
    private final String deviceConnectorType;

    @JsonProperty("deviceDetails")
    private final String deviceDetails;

    @JsonProperty("devicePort")
    private final String devicePort;

    @JsonProperty("removePatchCableWithLiveTraffic")
    private Boolean removePatchCableWithLiveTraffic;

    @JsonProperty("scopeOfWork")
    private String scopeOfWork;

    private SmartHandsCrossConnectRemoval(Builder builder) {
        this.serialNumber = builder.serialNumber;
        this.deviceCabinet = builder.deviceCabinet;
        this.deviceConnectorType = builder.deviceConnectorType;
        this.deviceDetails = builder.deviceDetails;
        this.devicePort = builder.devicePort;
        this.removePatchCableWithLiveTraffic = builder.removePatchCableWithLiveTraffic;
        this.scopeOfWork = builder.scopeOfWork;
    }

    /**
     * Returns a new builder for a cross-connect removal entry.
     *
     * @param serialNumber        the cross-connect serial number (required)
     * @param deviceCabinet       the cross-connect cabinet (required)
     * @param deviceConnectorType the device connector type (required)
     * @param deviceDetails       the device details (required)
     * @param devicePort          the device port (required)
     * @return a new builder
     */
    public static Builder builder(String serialNumber, String deviceCabinet, String deviceConnectorType,
                                  String deviceDetails, String devicePort) {
        return new Builder(serialNumber, deviceCabinet, deviceConnectorType, deviceDetails, devicePort);
    }

    public static class Builder {
        private final String serialNumber;
        private final String deviceCabinet;
        private final String deviceConnectorType;
        private final String deviceDetails;
        private final String devicePort;
        private Boolean removePatchCableWithLiveTraffic;
        private String scopeOfWork;

        private Builder(String serialNumber, String deviceCabinet, String deviceConnectorType,
                        String deviceDetails, String devicePort) {
            this.serialNumber = serialNumber;
            this.deviceCabinet = deviceCabinet;
            this.deviceConnectorType = deviceConnectorType;
            this.deviceDetails = deviceDetails;
            this.devicePort = devicePort;
        }

        public Builder removePatchCableWithLiveTraffic(Boolean removePatchCableWithLiveTraffic) {
            this.removePatchCableWithLiveTraffic = removePatchCableWithLiveTraffic;
            return this;
        }

        public Builder scopeOfWork(String scopeOfWork) {
            this.scopeOfWork = scopeOfWork;
            return this;
        }

        public SmartHandsCrossConnectRemoval build() {
            return new SmartHandsCrossConnectRemoval(this);
        }
    }
}
