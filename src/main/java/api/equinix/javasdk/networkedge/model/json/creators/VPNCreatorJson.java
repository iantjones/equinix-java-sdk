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

package api.equinix.javasdk.networkedge.model.json.creators;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * <p>VPNCreatorJson class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
@Getter
public class VPNCreatorJson {

    @JsonProperty("virtualDeviceUuid")
    private String siteName;

    @JsonProperty("virtualDeviceUuid")
    private String virtualDeviceUuid;

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

    @JsonProperty("tunnelIp")
    private String tunnelIp;

    @JsonProperty("secondary")
    private VPNCreatorJson secondary;

    @JsonProperty("useNetworkServiceConnection")
    private Boolean useNetworkServiceConnection;

    VPNCreatorJson(VPNOperator.VPNBuilder vpnBuilder) {
            this.siteName = vpnBuilder.getSiteName();
            this.virtualDeviceUuid = vpnBuilder.getVirtualDeviceUuid();
            this.configName = vpnBuilder.getConfigName();
            this.peerIp = vpnBuilder.getPeerIp();
            this.peerSharedKey = vpnBuilder.getPeerSharedKey();
            this.remoteAsn = vpnBuilder.getRemoteAsn();
            this.remoteIpAddress = vpnBuilder.getRemoteIpAddress();
            this.password = vpnBuilder.getPassword();
            this.localAsn = vpnBuilder.getLocalAsn();
            this.tunnelIp = vpnBuilder.getTunnelIp();
    }
}
