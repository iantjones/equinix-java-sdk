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

import api.equinix.javasdk.fabric.enums.PortAssignmentStrategy;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * <p>AccessPointMetadata class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
@Getter
public class AccessPointMetadata {

    @JsonProperty("globalOrganization")
    private String globalOrganization;

    @JsonProperty("allowVcMigration")
    private Boolean allowVcMigration;

    @JsonProperty("regEx")
    private String regEx;

    @JsonProperty("regExMsg")
    private String regExMsg;

    @JsonProperty("redundantProfileId")
    private String redundantProfileUuid;

    @JsonProperty("connection_editable")
    private Boolean connectionEditable;

    @JsonProperty("limit_auth_key_conn")
    private Boolean limitAuthKeyConn;

    @JsonProperty("is_release_vlan")
    private Boolean isReleaseVlan;

    @JsonProperty("eqx_managed_port")
    private Boolean eqxManagedPort;

    @JsonProperty("connection_name_editable")
    private Boolean connectionNameEditable;

    @JsonProperty("allowSecondaryLocation")
    private Boolean allowSecondaryLocation;

    @JsonProperty("maxConnectionsOnPort")
    private Integer maxConnectionsOnPort;

    @JsonProperty("vlanRangeMaxValue")
    private Integer vlanRangeMaxValue;

    @JsonProperty("vlanRangeMinValue")
    private Integer vlanRangeMinValue;

    @JsonProperty("port_assignment_strategy")
    private PortAssignmentStrategy portAssignmentStrategy;

    @JsonProperty("max_qinq")
    private Integer qinqMax;

    @JsonProperty("max_dot1q")
    private Integer dot1qMax;
}
