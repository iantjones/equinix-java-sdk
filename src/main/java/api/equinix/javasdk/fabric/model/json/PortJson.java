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

package api.equinix.javasdk.fabric.model.json;

import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.enums.PortType;
import api.equinix.javasdk.core.model.Serializable;
import api.equinix.javasdk.fabric.enums.PortState;
import api.equinix.javasdk.fabric.model.implementation.Account;
import api.equinix.javasdk.fabric.model.implementation.ChangeLog;
import api.equinix.javasdk.fabric.model.implementation.Device;
import api.equinix.javasdk.fabric.model.implementation.Encapsulation;
import api.equinix.javasdk.fabric.model.implementation.LinkAggregationGroup;
import api.equinix.javasdk.fabric.model.implementation.Location;
import api.equinix.javasdk.fabric.model.implementation.PortOperation;

import api.equinix.javasdk.fabric.model.implementation.PhysicalPort;
import api.equinix.javasdk.fabric.model.implementation.PortSettings;
import api.equinix.javasdk.fabric.model.Port;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Getter;

import java.util.List;

/**
 * <p>PortJson class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
@Getter
public final class PortJson implements Serializable {

    /**
     * <p>pagedTypeRef.</p>
     *
     * @return a {@link com.fasterxml.jackson.core.type.TypeReference} object.
     */
    public static TypeReference<Page<Port, PortJson>> pagedTypeRef() {
        return new TypeReference<>() {};
    }

    /**
     * <p>singleTypeRef.</p>
     *
     * @return a {@link com.fasterxml.jackson.core.type.TypeReference} object.
     */
    public static TypeReference<PortJson> singleTypeRef() {
        return new TypeReference<>() {};
    }

    @JsonProperty("uuid")
    private String uuid;

    @JsonProperty("type")
    private PortType type;

    @JsonProperty("name")
    private String name;

    @JsonProperty("href")
    private String href;

    @JsonProperty("state")
    private PortState state;

    @JsonProperty("cvpId")
    private String cvpId;

    @JsonProperty("bandwidth")
    private Integer bandwidth;

    @JsonProperty("usedBandwidth")
    private Integer usedBandwidth;

    @JsonProperty("availableBandwidth")
    private Integer availableBandwidth;

    @JsonProperty("location")
    private Location location;

    @JsonProperty("device")
    private Device device;

    @JsonProperty("encapsulation")
    private Encapsulation encapsulation;

    @JsonProperty("lag")
    LinkAggregationGroup lag;

    @JsonProperty("settings")
    PortSettings settings;

    @JsonProperty("physicalPorts")
    List<PhysicalPort> physicalPorts;

    @JsonProperty("operation")
    private PortOperation portOperation;

    @JsonProperty("account")
    private Account account;

    @JsonProperty("changeLog")
    private ChangeLog changeLog;

    @JsonProperty("projectId")
    private String projectId;
}
