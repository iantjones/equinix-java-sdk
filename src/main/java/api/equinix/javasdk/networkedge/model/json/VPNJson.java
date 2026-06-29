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

package api.equinix.javasdk.networkedge.model.json;

import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.model.Lifecycle;
import api.equinix.javasdk.core.model.deserializers.LocalDateTimeDeserializer;
import api.equinix.javasdk.core.enums.OperationalStatus;
import api.equinix.javasdk.networkedge.enums.BGPState;
import api.equinix.javasdk.networkedge.enums.UserStatus;
import api.equinix.javasdk.networkedge.enums.VPNStatus;
import api.equinix.javasdk.networkedge.model.VPN;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 *
 * @author ianjones
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class VPNJson extends Lifecycle {


    @JsonProperty("secondary")
    private VPN secondary;

    @JsonProperty("uuid")
    private String uuid;

    @JsonProperty("siteName")
    private String siteName;

    @JsonProperty("status")
    private VPNStatus status;

    @JsonProperty("bgpState")
    private BGPState bgpState;

    @JsonProperty("tunnelStatus")
    OperationalStatus tunnelStatus;

    @JsonProperty("virtualDeviceUuid")
    private String virtualDeviceUuid;

    @JsonProperty("useNetworkServiceConnection")
    private Boolean useNetworkServiceConnection;

    @JsonProperty("configName")
    private String configName;

    @JsonProperty("peerIp")
    private String peerIp;

    @JsonProperty("peerSharedKey")
    private String peerSharedKey;

    @JsonProperty("remoteAsn")
    private Long remoteAsn;

    @JsonProperty("remoteIpAddress")
    private String remoteIpAddress;

    @JsonProperty("password")
    private String password;

    @JsonProperty("localAsn")
    private Long localAsn;

    @JsonProperty("projectId")
    private String projectId;

    @JsonProperty("tunnelIp")
    private String tunnelIp;

    @JsonProperty("inboundBytes")
    private String inboundBytes;

    @JsonProperty("inboundPackets")
    private String inboundPackets;

    @JsonProperty("outboundBytes")
    private String outboundBytes;

    @JsonProperty("outboundPackets")
    private String outboundPackets;

    @JsonProperty("custOrgId")
    private Long custOrgId;

    // Network Edge responses use *DateTime audit fields rather than the shared Lifecycle *Date names.
    @JsonProperty("createdDateTime")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime createdDateTime;

    @JsonProperty("lastUpdatedDateTime")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime lastUpdatedDateTime;

    @JsonProperty("deletedDateTime")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime deletedDateTime;

    // VpnResponse expanded audit block (created-by / updated-by user metadata).
    @JsonProperty("createdByFirstName")
    private String createdByFirstName;

    @JsonProperty("createdByLastName")
    private String createdByLastName;

    @JsonProperty("createdByEmail")
    private String createdByEmail;

    @JsonProperty("createdByUserKey")
    private Long createdByUserKey;

    @JsonProperty("createdByAccountUcmId")
    private Long createdByAccountUcmId;

    @JsonProperty("createdByUserName")
    private String createdByUserName;

    @JsonProperty("createdByCustOrgId")
    private Long createdByCustOrgId;

    @JsonProperty("createdByCustOrgName")
    private String createdByCustOrgName;

    @JsonProperty("createdByUserStatus")
    private UserStatus createdByUserStatus;

    @JsonProperty("createdByCompanyName")
    private String createdByCompanyName;

    @JsonProperty("updatedByFirstName")
    private String updatedByFirstName;

    @JsonProperty("updatedByLastName")
    private String updatedByLastName;

    @JsonProperty("updatedByEmail")
    private String updatedByEmail;

    @JsonProperty("updatedByUserKey")
    private Long updatedByUserKey;

    @JsonProperty("updatedByAccountUcmId")
    private Long updatedByAccountUcmId;

    @JsonProperty("updatedByUserName")
    private String updatedByUserName;

    @JsonProperty("updatedByCustOrgId")
    private Long updatedByCustOrgId;

    @JsonProperty("updatedByCustOrgName")
    private String updatedByCustOrgName;

    @JsonProperty("updatedByUserStatus")
    private UserStatus updatedByUserStatus;

    @JsonProperty("updatedByCompanyName")
    private String updatedByCompanyName;

    @Override
    public LocalDateTime getCreatedDate() {
        return createdDateTime != null ? createdDateTime : super.getCreatedDate();
    }

    @Override
    public LocalDateTime getLastUpdatedDate() {
        return lastUpdatedDateTime != null ? lastUpdatedDateTime : super.getLastUpdatedDate();
    }

    @Override
    public LocalDateTime getDeletedDate() {
        return deletedDateTime != null ? deletedDateTime : super.getDeletedDate();
    }
}
