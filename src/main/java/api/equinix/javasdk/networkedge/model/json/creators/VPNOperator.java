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

import api.equinix.javasdk.core.http.response.Pageable;
import api.equinix.javasdk.core.internal.Constants;
import api.equinix.javasdk.core.model.ResourceImpl;
import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.networkedge.client.internal.implementation.VPNClientImpl;
import api.equinix.javasdk.networkedge.model.VPN;
import api.equinix.javasdk.networkedge.model.json.VPNJson;
import api.equinix.javasdk.networkedge.model.wrappers.VPNWrapper;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

//TODO Add Secondary VPN
/**
 * <p>VPNOperator class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
@Setter(AccessLevel.PRIVATE)
public class VPNOperator extends ResourceImpl<VPN> {

    @Getter
    private final Pageable<VPN> serviceClient;

    /**
     * <p>Constructor for VPNOperator.</p>
     *
     * @param serviceClient a {@link api.equinix.javasdk.core.http.response.Pageable} object.
     */
    public VPNOperator(Pageable<VPN> serviceClient) {
        this.serviceClient = serviceClient;
    }

    /**
     * <p>create.</p>
     *
     * @param configName a {@link java.lang.String} object.
     * @return a {@link api.equinix.javasdk.networkedge.model.json.creators.VPNOperator.VPNBuilder} object.
     */
    public VPNBuilder create(String configName) {
        return new VPNBuilder(configName);
    }

    /**
     * <p>update.</p>
     *
     * @param json a {@link api.equinix.javasdk.networkedge.model.json.VPNJson} object.
     * @return a {@link api.equinix.javasdk.networkedge.model.json.creators.VPNOperator.VPNUpdater} object.
     */
    public VPNUpdater update(VPNJson json) {
        return new VPNUpdater(json);
    }

    @Getter
    public class VPNBuilder {
        private String siteName;
        private String virtualDeviceUuid;
        private String configName;
        private String peerIp;
        private String peerSharedKey;
        private Integer remoteAsn;
        private String remoteIpAddress;
        private String password;
        private Integer localAsn;
        private String tunnelIp;
        private Boolean useNetworkServiceConnection;

        protected VPNBuilder(String configName) {
            this.configName = configName;
        }

         public VPNBuilder inMetro(String metroTitle) {
            this.siteName = metroTitle;
            return this;
        }

         public VPNBuilder onDeviceUuid(String deviceUuid) {
            this.virtualDeviceUuid = deviceUuid;
            return this;
        }

         public VPNBuilder withPeerIp(String peerIp) {
            this.peerIp = peerIp;
            return this;
        }

         public VPNBuilder withPeerSharedKey(String peerSharedKey) {
            this.peerSharedKey = peerSharedKey;
            return this;
        }

         public VPNBuilder withRemoteAsn(Integer remoteAsn) {
            this.remoteAsn = remoteAsn;
            return this;
        }

         public VPNBuilder withRemoteIpAddress(String remoteIpAddress) {
            this.remoteIpAddress = remoteIpAddress;
            return this;
        }

         public VPNBuilder withPassword(String password) {
            this.password = password;
            return this;
        }

         public VPNBuilder withLocalAsn(Integer localAsn) {
            this.localAsn = localAsn;
            return this;
        }

         public VPNBuilder withTunnelIp(String tunnelIp) {
            this.tunnelIp = tunnelIp;
            return this;
        }

        public VPNBuilder useNetworkServiceConnection(Boolean useNetworkServiceConnection) {
            this.useNetworkServiceConnection = useNetworkServiceConnection;
            return this;
        }

        public VPN save() {
            VPNCreatorJson deviceLinkCreatorJson = new VPNCreatorJson(this);
            VPNJson deviceLinkJson = ((VPNClientImpl) VPNOperator.this.getServiceClient()).create(deviceLinkCreatorJson);
            return new VPNWrapper(deviceLinkJson, VPNOperator.this.getServiceClient());
        }
    }

    public class VPNUpdater {

        private VPNJson json;
        private VPNUpdaterJson updaterJson;

        protected VPNUpdater(VPNJson json) {
            this.json = json;
            this.updaterJson = Constants.JSON_CONVERTOR.convertValue(this.json, VPNUpdaterJson.class);
        }

        public VPNUpdater withConfigName(String configName) {
            this.updaterJson.setConfigName(configName);
            return this;
        }

        public VPNUpdater withPeerIp(String peerIp) {
            this.updaterJson.setPeerIp(peerIp);
            return this;
        }

        public VPNUpdater withPeerSharedKey(String peerSharedKey) {
            this.updaterJson.setPeerSharedKey(peerSharedKey);
            return this;
        }

        public VPNUpdater withRemoteAsn(Integer remoteAsn) {
            this.updaterJson.setRemoteAsn(remoteAsn);
            return this;
        }

        public VPNUpdater withRemoteIpAddress(String remoteIpAddress) {
            this.updaterJson.setRemoteIpAddress(remoteIpAddress);
            return this;
        }

        public VPNUpdater withPassword(String password) {
            this.updaterJson.setPassword(password);
            return this;
        }

        public VPNUpdater withLocalAsn(Integer localAsn) {
            this.updaterJson.setLocalAsn(localAsn);
            return this;
        }

        public VPNUpdater withTunnelIp(String tunnelIp) {
            this.updaterJson.setTunnelIp(tunnelIp);
            return this;
        }

        public VPNUpdater useNetworkServiceConnection(Boolean useNetworkServiceConnection) {
            this.updaterJson.setUseNetworkServiceConnection(useNetworkServiceConnection);
            return this;
        }

        public VPN save() {
            json = ((VPNClientImpl) VPNOperator.this.getServiceClient()).update(this.json.getUuid(), this.updaterJson);
            return new VPNWrapper(json, VPNOperator.this.getServiceClient());
        }
    }
}
