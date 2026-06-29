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
 * Typed {@code serviceDetails} for a smart hands locate-package order
 * ({@code locatePackageRequest.serviceDetails} in the smart hands v1 spec). Pass an instance to
 * {@link SmartHandsRequestJson#builder(IbxLocation, java.util.List, ScheduleInfo, Object)}.
 *
 * <p>All five fields ({@code shipmentOrderNumber}, {@code trackingNumber}, {@code possibleLocation},
 * {@code packageDescription}, {@code scopeOfWork}) are required.</p>
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LocatePackageDetails {

    @JsonProperty("shipmentOrderNumber")
    private final String shipmentOrderNumber;

    @JsonProperty("trackingNumber")
    private final String trackingNumber;

    @JsonProperty("possibleLocation")
    private final String possibleLocation;

    @JsonProperty("packageDescription")
    private final String packageDescription;

    @JsonProperty("scopeOfWork")
    private final String scopeOfWork;

    public LocatePackageDetails(String shipmentOrderNumber, String trackingNumber, String possibleLocation,
                                String packageDescription, String scopeOfWork) {
        this.shipmentOrderNumber = shipmentOrderNumber;
        this.trackingNumber = trackingNumber;
        this.possibleLocation = possibleLocation;
        this.packageDescription = packageDescription;
        this.scopeOfWork = scopeOfWork;
    }
}
