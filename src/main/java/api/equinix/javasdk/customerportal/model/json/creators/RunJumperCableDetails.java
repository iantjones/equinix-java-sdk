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

package api.equinix.javasdk.customerportal.model.json.creators;

import api.equinix.javasdk.customerportal.enums.SmartHandsConnectorType;
import api.equinix.javasdk.customerportal.enums.SmartHandsJumperType;
import api.equinix.javasdk.customerportal.enums.SmartHandsMediaType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

/**
 * Typed {@code serviceDetails} for a smart hands run-jumper-cable order
 * ({@code runJumperCableRequest.serviceDetails} in the smart hands v1 spec). Pass an instance to
 * {@link SmartHandsRequestJson#builder(IbxLocation, java.util.List, ScheduleInfo, Object)}.
 *
 * <p>Required: {@code quantity} (one of {@code "1"}..{@code "12"} or {@code "12+"}) and
 * {@code scopeOfWork}. When {@code quantity} is {@code "1"}, {@code jumperType},
 * {@code mediaType}, {@code connector}, {@code provideTxRxLightLevels} and {@code deviceDetails}
 * become mandatory.</p>
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class RunJumperCableDetails {

    @JsonProperty("quantity")
    private final String quantity;

    @JsonProperty("scopeOfWork")
    private final String scopeOfWork;

    @JsonProperty("jumperType")
    private SmartHandsJumperType jumperType;

    @JsonProperty("mediaType")
    private SmartHandsMediaType mediaType;

    @JsonProperty("connector")
    private SmartHandsConnectorType connector;

    @JsonProperty("cableId")
    private String cableId;

    @JsonProperty("provideTxRxLightLevels")
    private Boolean provideTxRxLightLevels;

    @JsonProperty("deviceDetails")
    private List<SmartHandsDevice> deviceDetails;

    private RunJumperCableDetails(Builder builder) {
        this.quantity = builder.quantity;
        this.scopeOfWork = builder.scopeOfWork;
        this.jumperType = builder.jumperType;
        this.mediaType = builder.mediaType;
        this.connector = builder.connector;
        this.cableId = builder.cableId;
        this.provideTxRxLightLevels = builder.provideTxRxLightLevels;
        this.deviceDetails = builder.deviceDetails;
    }

    @JsonCreator
    private RunJumperCableDetails(
            @JsonProperty("quantity") String quantity,
            @JsonProperty("scopeOfWork") String scopeOfWork,
            @JsonProperty("jumperType") SmartHandsJumperType jumperType,
            @JsonProperty("mediaType") SmartHandsMediaType mediaType,
            @JsonProperty("connector") SmartHandsConnectorType connector,
            @JsonProperty("cableId") String cableId,
            @JsonProperty("provideTxRxLightLevels") Boolean provideTxRxLightLevels,
            @JsonProperty("deviceDetails") List<SmartHandsDevice> deviceDetails) {
        this.quantity = quantity;
        this.scopeOfWork = scopeOfWork;
        this.jumperType = jumperType;
        this.mediaType = mediaType;
        this.connector = connector;
        this.cableId = cableId;
        this.provideTxRxLightLevels = provideTxRxLightLevels;
        this.deviceDetails = deviceDetails;
    }

    /**
     * Returns a new builder for run-jumper-cable service details.
     *
     * @param quantity    the number of jumpers to run (required; e.g. {@code "1"}..{@code "12"}, {@code "12+"})
     * @param scopeOfWork the scope of work (required)
     * @return a new builder
     */
    public static Builder builder(String quantity, String scopeOfWork) {
        return new Builder(quantity, scopeOfWork);
    }

    public static class Builder {
        private final String quantity;
        private final String scopeOfWork;
        private SmartHandsJumperType jumperType;
        private SmartHandsMediaType mediaType;
        private SmartHandsConnectorType connector;
        private String cableId;
        private Boolean provideTxRxLightLevels;
        private List<SmartHandsDevice> deviceDetails;

        private Builder(String quantity, String scopeOfWork) {
            this.quantity = quantity;
            this.scopeOfWork = scopeOfWork;
        }

        public Builder jumperType(SmartHandsJumperType jumperType) {
            this.jumperType = jumperType;
            return this;
        }

        public Builder mediaType(SmartHandsMediaType mediaType) {
            this.mediaType = mediaType;
            return this;
        }

        public Builder connector(SmartHandsConnectorType connector) {
            this.connector = connector;
            return this;
        }

        public Builder cableId(String cableId) {
            this.cableId = cableId;
            return this;
        }

        public Builder provideTxRxLightLevels(Boolean provideTxRxLightLevels) {
            this.provideTxRxLightLevels = provideTxRxLightLevels;
            return this;
        }

        public Builder deviceDetails(List<SmartHandsDevice> deviceDetails) {
            this.deviceDetails = deviceDetails;
            return this;
        }

        public RunJumperCableDetails build() {
            return new RunJumperCableDetails(this);
        }
    }
}
