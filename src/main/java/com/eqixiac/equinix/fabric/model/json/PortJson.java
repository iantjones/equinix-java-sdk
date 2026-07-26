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

package com.eqixiac.equinix.fabric.model.json;

import com.eqixiac.equinix.core.http.response.Page;
import com.eqixiac.equinix.fabric.enums.BmmrType;
import com.eqixiac.equinix.fabric.enums.ConnectivitySourceType;
import com.eqixiac.equinix.fabric.enums.PhysicalPortType;
import com.eqixiac.equinix.fabric.enums.PortServiceCode;
import com.eqixiac.equinix.fabric.enums.PortServiceType;
import com.eqixiac.equinix.fabric.enums.PortType;
import com.eqixiac.equinix.fabric.enums.PortState;
import com.eqixiac.equinix.fabric.model.Project;
import com.eqixiac.equinix.fabric.model.implementation.*;

import com.eqixiac.equinix.fabric.model.Port;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Getter;

import java.util.List;

/**
 *
 * @author ianjones
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public final class PortJson {



    @JsonProperty("uuid")
    private String uuid;

    @JsonProperty("type")
    private PortType type;

    @JsonProperty("name")
    private String name;

    @JsonProperty("href")
    private String href;

    @JsonProperty("description")
    private String description;

    @JsonProperty("state")
    private PortState state;

    /**
     * Customer virtual port id. Not declared on the spec's {@code Port} schema, but the spec's own
     * response examples for {@code GET /fabric/v4/ports} ({@code ports}) and
     * {@code GET /fabric/v4/ports/{portId}} ({@code portByUUID}) both carry it (e.g.
     * {@code "cvpId": "2000833"}), so it is retained here.
     */
    @JsonProperty("cvpId")
    private String cvpId;

    @JsonProperty("bandwidth")
    private Integer bandwidth;

    @JsonProperty("usedBandwidth")
    private Integer usedBandwidth;

    @JsonProperty("availableBandwidth")
    private Integer availableBandwidth;

    @JsonProperty("physicalPortsSpeed")
    private Integer physicalPortsSpeed;

    @JsonProperty("physicalPortsType")
    private PhysicalPortType physicalPortsType;

    @JsonProperty("physicalPortsCount")
    private Integer physicalPortsCount;

    @JsonProperty("physicalPortQuantity")
    private Integer physicalPortQuantity;

    @JsonProperty("connectionsCount")
    private Integer connectionsCount;

    @JsonProperty("connectivitySourceType")
    private ConnectivitySourceType connectivitySourceType;

    @JsonProperty("bmmrType")
    private BmmrType bmmrType;

    @JsonProperty("serviceType")
    private PortServiceType serviceType;

    @JsonProperty("serviceCode")
    private PortServiceCode serviceCode;

    @JsonProperty("asn")
    private Long asn;

    @JsonProperty("location")
    private Location location;

    @JsonProperty("device")
    private Device device;

    @JsonProperty("interface")
    private PortInterface portInterface;

    @JsonProperty("demarcationPointIbx")
    private String demarcationPointIbx;

    @JsonProperty("tetherIbx")
    private String tetherIbx;

    @JsonProperty("demarcationPoint")
    private DemarcationPoint demarcationPoint;

    @JsonProperty("encapsulation")
    private Encapsulation encapsulation;

    @JsonProperty("lag")
    LinkAggregationGroup lag;

    @JsonProperty("package")
    private PackageRef portPackage;

    @JsonProperty("settings")
    PortSettings settings;

    @JsonProperty("physicalPorts")
    List<PhysicalPort> physicalPorts;

    @JsonProperty("redundancy")
    Redundancy redundancy;

    @JsonProperty("lagEnabled")
    Boolean lagEnabled;

    @JsonProperty("operation")
    private PortOperation portOperation;

    @JsonProperty("account")
    private Account account;

    @JsonProperty("order")
    private PortOrder order;

    @JsonProperty("change")
    private PortChange change;

    @JsonProperty("changeLog")
    private ChangeLog changeLog;

    @JsonProperty("project")
    private Project project;

    @JsonProperty("notifications")
    private List<PortNotification> notifications;

    @JsonProperty("additionalInfo")
    private List<PortAdditionalInfo> additionalInfo;

    @JsonProperty("endCustomer")
    private EndCustomer endCustomer;

    @JsonProperty("loas")
    private List<PortLoa> loas;

    @JsonProperty("marketplaceSubscription")
    private MarketplaceSubscriptionRef marketplaceSubscription;

    /**
     * Convenience accessor for the subscriber-assigned project identifier, sourced from the
     * nested {@code project.projectId} response attribute (the Port schema has no top-level
     * {@code projectId} property).
     *
     * @return the project identifier, or {@code null} if the port carries no project
     */
    public String getProjectId() {
        return this.project != null ? this.project.getProjectId() : null;
    }
}
