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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * Typed {@code serviceDetails} for a smart hands move-jumper-cable order
 * ({@code moveJumperCableRequest.serviceDetails} in the smart hands v1 spec). Pass an instance to
 * {@link SmartHandsRequestJson#builder(IbxLocation, java.util.List, ScheduleInfo, Object)}.
 *
 * <p>Required: {@code quantity} (one of {@code "1"}..{@code "12"} or {@code "12+"}) and
 * {@code scopeOfWork}. When {@code quantity} is {@code "1"}, {@code cableId},
 * {@code currentDeviceDetails} and {@code newDeviceDetails} are mandatory.</p>
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MoveJumperCableDetails {

    @JsonProperty("quantity")
    private final String quantity;

    @JsonProperty("scopeOfWork")
    private final String scopeOfWork;

    @JsonProperty("cableId")
    private String cableId;

    @JsonProperty("currentDeviceDetails")
    private SmartHandsDevice currentDeviceDetails;

    @JsonProperty("newDeviceDetails")
    private SmartHandsDevice newDeviceDetails;

    private MoveJumperCableDetails(Builder builder) {
        this.quantity = builder.quantity;
        this.scopeOfWork = builder.scopeOfWork;
        this.cableId = builder.cableId;
        this.currentDeviceDetails = builder.currentDeviceDetails;
        this.newDeviceDetails = builder.newDeviceDetails;
    }

    /**
     * Returns a new builder for move-jumper-cable service details.
     *
     * @param quantity    the number of jumpers to move (required; e.g. {@code "1"}..{@code "12"}, {@code "12+"})
     * @param scopeOfWork the scope of work (required)
     * @return a new builder
     */
    public static Builder builder(String quantity, String scopeOfWork) {
        return new Builder(quantity, scopeOfWork);
    }

    public static class Builder {
        private final String quantity;
        private final String scopeOfWork;
        private String cableId;
        private SmartHandsDevice currentDeviceDetails;
        private SmartHandsDevice newDeviceDetails;

        private Builder(String quantity, String scopeOfWork) {
            this.quantity = quantity;
            this.scopeOfWork = scopeOfWork;
        }

        public Builder cableId(String cableId) {
            this.cableId = cableId;
            return this;
        }

        public Builder currentDeviceDetails(SmartHandsDevice currentDeviceDetails) {
            this.currentDeviceDetails = currentDeviceDetails;
            return this;
        }

        public Builder newDeviceDetails(SmartHandsDevice newDeviceDetails) {
            this.newDeviceDetails = newDeviceDetails;
            return this;
        }

        public MoveJumperCableDetails build() {
            return new MoveJumperCableDetails(this);
        }
    }
}
