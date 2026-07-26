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

import com.eqixiac.equinix.fabric.enums.PortServiceCode;
import com.eqixiac.equinix.fabric.enums.PortServiceType;
import com.eqixiac.equinix.fabric.enums.PortState;
import com.eqixiac.equinix.fabric.enums.PortType;
import com.eqixiac.equinix.fabric.model.Project;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

/**
 * Port reference returned on an access point (spec schema {@code SimplifiedPort}).
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class PortSummary extends PortRef {

    @JsonProperty("href")
    private String href;

    @JsonProperty("type")
    private PortType type;

    @JsonProperty("id")
    private Integer id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("state")
    private PortState state;

    @JsonProperty("physicalPortsSpeed")
    private Integer physicalPortsSpeed;

    @JsonProperty("connectionsCount")
    private Integer connectionsCount;

    @JsonProperty("project")
    private Project project;

    @JsonProperty("operation")
    private PortOperation operation;

    @JsonProperty("account")
    private AccountSummary account;

    @JsonProperty("serviceType")
    private PortServiceType serviceType;

    @JsonProperty("serviceCode")
    private PortServiceCode serviceCode;

    @JsonProperty("bandwidth")
    private Long bandwidth;

    @JsonProperty("availableBandwidth")
    private Long availableBandwidth;

    @JsonProperty("usedBandwidth")
    private Long usedBandwidth;

    @JsonProperty("location")
    private Location location;

    @JsonProperty("device")
    private Device device;

    @JsonProperty("interface")
    private PortInterface portInterface;

    @JsonProperty("tether")
    private Tether tether;

    @JsonProperty("demarcationPoint")
    private DemarcationPoint demarcationPoint;

    @JsonProperty("encapsulation")
    private Encapsulation encapsulation;

    @JsonProperty("lagEnabled")
    private Boolean lagEnabled;

    @JsonProperty("package")
    private PackageRef portPackage;

    @JsonProperty("settings")
    private PortSettings settings;

    @JsonProperty("physicalPortQuantity")
    private Integer physicalPortQuantity;

    @JsonProperty("additionalInfo")
    private List<PortAdditionalInfo> additionalInfo;

    @JsonProperty("redundancy")
    private Redundancy redundancy;

    @JsonProperty("physicalPorts")
    private List<PhysicalPort> physicalPorts;
}
