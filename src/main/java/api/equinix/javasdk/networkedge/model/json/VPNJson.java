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
import api.equinix.javasdk.core.enums.OperationalStatus;
import api.equinix.javasdk.networkedge.enums.BGPState;
import api.equinix.javasdk.networkedge.enums.VPNStatus;
import api.equinix.javasdk.networkedge.model.VPN;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Getter;

/**
 * <p>VPNJson class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
@Getter
public class VPNJson extends Lifecycle {

    @Getter static TypeReference<Page<VPN, VPNJson>> pagedTypeRef = new TypeReference<>() {};
    @Getter static TypeReference<VPNJson> singleTypeRef = new TypeReference<>() {};

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
    private Integer remoteAsn;

    @JsonProperty("remoteIpAddress")
    private String remoteIpAddress;

    @JsonProperty("password")
    private String password;

    @JsonProperty("localAsn")
    private Integer localAsn;

    @JsonProperty("projectId")
    private String projectId;

    @JsonProperty("tunnelIp")
    private String tunnelIp;
}
