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

import api.equinix.javasdk.fabric.enums.PortType;
import api.equinix.javasdk.fabric.enums.PortState;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

/**
 * A physical member port backing a virtual port (the Fabric v4 {@code PhysicalPort} schema).
 *
 * @author ianjones
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class PhysicalPort {

    @JsonProperty("href")
    private String href;

    @JsonProperty("id")
    private Integer id;

    @JsonProperty("uuid")
    private String uuid;

    @JsonProperty("type")
    private PortType type;

    @JsonProperty("state")
    private PortState state;

    @JsonProperty("account")
    private Account account;

    @JsonProperty("interfaceSpeed")
    private Integer interfaceSpeed;

    @JsonProperty("interfaceType")
    private String interfaceType;

    @JsonProperty("tether")
    private Tether tether;

    @JsonProperty("demarcationPoint")
    private DemarcationPoint demarcationPoint;

    @JsonProperty("settings")
    private PhysicalPortSettings settings;

    @JsonProperty("interface")
    private PortInterface portInterface;

    @JsonProperty("notifications")
    private List<PortNotification> notifications;

    @JsonProperty("additionalInfo")
    private List<PortAdditionalInfo> additionalInfo;

    @JsonProperty("order")
    private PortOrder order;

    @JsonProperty("operation")
    private PortOperation portOperation;

    @JsonProperty("loas")
    private List<PortLoa> loas;
}
