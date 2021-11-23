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

import api.equinix.javasdk.core.model.Lifecycle;
import api.equinix.javasdk.core.enums.OperationalStatus;
import api.equinix.javasdk.networkedge.enums.BGPState;
import api.equinix.javasdk.networkedge.enums.BGPStatus;
import api.equinix.javasdk.networkedge.enums.ServiceType;
import api.equinix.javasdk.networkedge.enums.VPNStatus;
import api.equinix.javasdk.networkedge.model.implementation.BackupService;
import api.equinix.javasdk.networkedge.model.implementation.GenericDataObject;
import api.equinix.javasdk.networkedge.model.implementation.InboundRule;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * <p>RestoreFeasibilityJson class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
@Getter
public class RestoreFeasibilityJson {

    @Getter static TypeReference<RestoreFeasibilityJson> singleTypeRef = new TypeReference<>() {};

    @JsonProperty("deviceBackup")
    private BackupJson deviceBackup;

    @JsonProperty("services")
    private Map<ServiceType, List<BackupService>> services;

    @JsonProperty("restoreAllowedAfterDeleteOrEdit")
    private Boolean restoreAllowedAfterDeleteOrEdit;

    @JsonProperty("restoreAllowed")
    private Boolean restoreAllowed;

    public static class Connection implements GenericDataObject<Connection> {
        @JsonProperty("id")
        private String id;

        @JsonProperty("name")
        private String name;

        @JsonProperty("vlan")
        private Integer vlan;

        @JsonProperty("overlayIpCidr")
        private String overlayIpCidr;

        @Override
        @JsonIgnore
        public Connection getDataObject() {
            return this;
        }
    }

    public static class VPN extends Lifecycle implements GenericDataObject<VPN> {
        @JsonProperty("uuid")
        private String uuid;

        @JsonProperty("configName")
        private String configName;

        @JsonProperty("status")
        private VPNStatus status;

        @JsonProperty("bgpState")
        private BGPState bgpState;

        @JsonProperty("tunnelStatus")
        private OperationalStatus tunnelStatus;

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

        @JsonProperty("siteName")
        private String siteName;

        @JsonProperty("secondary")
        private VPN secondary;

        @JsonProperty("useNetworkServiceConnection")
        private Boolean useNetworkServiceConnection;

        @Override
        @JsonIgnore
        public VPN getDataObject() {
            return this;
        }
    }

    public static class BGPPeering extends Lifecycle implements GenericDataObject<BGPPeering> {
        @JsonProperty(value = "uuid")
        private String uuid;

        @JsonProperty(value = "connectionUuid")
        private String connectionUuid;

        @JsonProperty(value = "connectionName")
        private String connectionName;

        @JsonProperty(value = "virtualDeviceUuid")
        private String virtualDeviceUuid;

        @JsonProperty(value = "localIpAddress")
        private String localIpAddress;

        @JsonProperty(value = "remoteIpAddress")
        private String remoteIpAddress;

        @JsonProperty(value = "localAsn")
        private Integer localAsn;

        @JsonProperty(value = "remoteAsn")
        private Integer remoteAsn;

        @JsonProperty(value = "authenticationKey")
        private String authenticationKey;

        @JsonProperty(value = "status")
        private BGPStatus status;

        @JsonProperty(value = "bgpState")
        private BGPState bgpState;

        @Override
        @JsonIgnore
        public BGPPeering getDataObject() {
            return this;
        }
    }

    public static class ACLTemplate extends Lifecycle implements GenericDataObject<ACLTemplate> {
        @JsonProperty("uuid")
        private String uuid;

        @JsonProperty("name")
        private String name;

        @JsonProperty("description")
        private String description;

        @JsonProperty("inboundRules")
        List<InboundRule> inboundRules;

        @Override
        @JsonIgnore
        public ACLTemplate getDataObject() {
            return this;
        }
    }

    public static class License implements GenericDataObject<License> {
        @JsonProperty("virtualDeviceUuid")
        private String virtualDeviceUuid;

        @Override
        @JsonIgnore
        public License getDataObject() {
            return this;
        }
    }
    
    public static class DeviceLink extends Lifecycle implements GenericDataObject<DeviceLink> {
        @JsonProperty("id")
        private String id;

        @JsonProperty("name")
        private String name;

        @JsonProperty("vlan")
        private String vlan;

        @JsonProperty("overlayIpCidr")
        private String overlayIpCidr;

        @Override
        @JsonIgnore
        public DeviceLink getDataObject() {
            return this;
        }
    }

}
