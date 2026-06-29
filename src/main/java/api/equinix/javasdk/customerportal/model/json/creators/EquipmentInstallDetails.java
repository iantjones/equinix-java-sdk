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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * Typed {@code serviceDetails} for a smart hands equipment-install order
 * ({@code equipmentInstallRequest.serviceDetails} in the smart hands v1 spec). Pass an instance
 * to {@link SmartHandsRequestJson#builder(IbxLocation, java.util.List, ScheduleInfo, Object)}.
 *
 * <p>Required: {@code deviceLocation}, {@code elevationDrawingAttached}, {@code installationPoint},
 * {@code installedEquipmentPhotoRequired}, {@code mountHardwareIncluded}, {@code patchDevices},
 * {@code powerItOn}, {@code scopeOfWork}.</p>
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class EquipmentInstallDetails {

    @JsonProperty("deviceLocation")
    private final String deviceLocation;

    @JsonProperty("elevationDrawingAttached")
    private final Boolean elevationDrawingAttached;

    @JsonProperty("installationPoint")
    private final String installationPoint;

    @JsonProperty("installedEquipmentPhotoRequired")
    private final Boolean installedEquipmentPhotoRequired;

    @JsonProperty("mountHardwareIncluded")
    private final Boolean mountHardwareIncluded;

    @JsonProperty("patchDevices")
    private final Boolean patchDevices;

    @JsonProperty("powerItOn")
    private final Boolean powerItOn;

    @JsonProperty("scopeOfWork")
    private final String scopeOfWork;

    @JsonProperty("patchingInfo")
    private String patchingInfo;

    @JsonProperty("needSupportFromASubmarineCableStationEngineer")
    private Boolean needSupportFromASubmarineCableStationEngineer;

    private EquipmentInstallDetails(Builder builder) {
        this.deviceLocation = builder.deviceLocation;
        this.elevationDrawingAttached = builder.elevationDrawingAttached;
        this.installationPoint = builder.installationPoint;
        this.installedEquipmentPhotoRequired = builder.installedEquipmentPhotoRequired;
        this.mountHardwareIncluded = builder.mountHardwareIncluded;
        this.patchDevices = builder.patchDevices;
        this.powerItOn = builder.powerItOn;
        this.scopeOfWork = builder.scopeOfWork;
        this.patchingInfo = builder.patchingInfo;
        this.needSupportFromASubmarineCableStationEngineer = builder.needSupportFromASubmarineCableStationEngineer;
    }

    @JsonCreator
    private EquipmentInstallDetails(
            @JsonProperty("deviceLocation") String deviceLocation,
            @JsonProperty("elevationDrawingAttached") Boolean elevationDrawingAttached,
            @JsonProperty("installationPoint") String installationPoint,
            @JsonProperty("installedEquipmentPhotoRequired") Boolean installedEquipmentPhotoRequired,
            @JsonProperty("mountHardwareIncluded") Boolean mountHardwareIncluded,
            @JsonProperty("patchDevices") Boolean patchDevices,
            @JsonProperty("powerItOn") Boolean powerItOn,
            @JsonProperty("scopeOfWork") String scopeOfWork,
            @JsonProperty("patchingInfo") String patchingInfo,
            @JsonProperty("needSupportFromASubmarineCableStationEngineer") Boolean needSupportFromASubmarineCableStationEngineer) {
        this.deviceLocation = deviceLocation;
        this.elevationDrawingAttached = elevationDrawingAttached;
        this.installationPoint = installationPoint;
        this.installedEquipmentPhotoRequired = installedEquipmentPhotoRequired;
        this.mountHardwareIncluded = mountHardwareIncluded;
        this.patchDevices = patchDevices;
        this.powerItOn = powerItOn;
        this.scopeOfWork = scopeOfWork;
        this.patchingInfo = patchingInfo;
        this.needSupportFromASubmarineCableStationEngineer = needSupportFromASubmarineCableStationEngineer;
    }

    /**
     * Returns a new builder for equipment-install service details.
     *
     * @param deviceLocation                  the device location (required)
     * @param elevationDrawingAttached        whether an elevation drawing is attached (required)
     * @param installationPoint               the installation point (required)
     * @param installedEquipmentPhotoRequired whether an installed equipment photo is required (required)
     * @param mountHardwareIncluded           whether mount hardware is included (required)
     * @param patchDevices                    whether devices should be patched (required)
     * @param powerItOn                       whether to power the equipment on (required)
     * @param scopeOfWork                     the scope of work (required)
     * @return a new builder
     */
    public static Builder builder(String deviceLocation, Boolean elevationDrawingAttached, String installationPoint,
                                  Boolean installedEquipmentPhotoRequired, Boolean mountHardwareIncluded,
                                  Boolean patchDevices, Boolean powerItOn, String scopeOfWork) {
        return new Builder(deviceLocation, elevationDrawingAttached, installationPoint,
                installedEquipmentPhotoRequired, mountHardwareIncluded, patchDevices, powerItOn, scopeOfWork);
    }

    public static class Builder {
        private final String deviceLocation;
        private final Boolean elevationDrawingAttached;
        private final String installationPoint;
        private final Boolean installedEquipmentPhotoRequired;
        private final Boolean mountHardwareIncluded;
        private final Boolean patchDevices;
        private final Boolean powerItOn;
        private final String scopeOfWork;
        private String patchingInfo;
        private Boolean needSupportFromASubmarineCableStationEngineer;

        private Builder(String deviceLocation, Boolean elevationDrawingAttached, String installationPoint,
                        Boolean installedEquipmentPhotoRequired, Boolean mountHardwareIncluded,
                        Boolean patchDevices, Boolean powerItOn, String scopeOfWork) {
            this.deviceLocation = deviceLocation;
            this.elevationDrawingAttached = elevationDrawingAttached;
            this.installationPoint = installationPoint;
            this.installedEquipmentPhotoRequired = installedEquipmentPhotoRequired;
            this.mountHardwareIncluded = mountHardwareIncluded;
            this.patchDevices = patchDevices;
            this.powerItOn = powerItOn;
            this.scopeOfWork = scopeOfWork;
        }

        public Builder patchingInfo(String patchingInfo) {
            this.patchingInfo = patchingInfo;
            return this;
        }

        public Builder needSupportFromASubmarineCableStationEngineer(Boolean needSupportFromASubmarineCableStationEngineer) {
            this.needSupportFromASubmarineCableStationEngineer = needSupportFromASubmarineCableStationEngineer;
            return this;
        }

        public EquipmentInstallDetails build() {
            return new EquipmentInstallDetails(this);
        }
    }
}
