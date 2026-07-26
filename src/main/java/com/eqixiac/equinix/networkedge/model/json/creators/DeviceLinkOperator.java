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

package com.eqixiac.equinix.networkedge.model.json.creators;

import com.eqixiac.equinix.core.http.response.Pageable;
import com.eqixiac.equinix.core.internal.Constants;
import com.eqixiac.equinix.core.model.ResourceImpl;
import com.eqixiac.equinix.core.enums.MetroCode;
import com.eqixiac.equinix.networkedge.client.internal.implementation.DeviceLinkClientImpl;
import com.eqixiac.equinix.networkedge.enums.RedundancyType;
import com.eqixiac.equinix.networkedge.model.Device;
import com.eqixiac.equinix.networkedge.model.DeviceLink;
import com.eqixiac.equinix.networkedge.model.json.DeviceLinkJson;
import com.eqixiac.equinix.networkedge.model.wrappers.DeviceLinkWrapper;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 *
 * @author ianjones
 */
public class DeviceLinkOperator extends ResourceImpl<DeviceLink> {

    @Getter
    private final Pageable<DeviceLink> serviceClient;

    public DeviceLinkOperator(Pageable<DeviceLink> serviceClient) {
        this.serviceClient = serviceClient;
    }

    public DeviceLinkBuilder create(String groupName) {
        return new DeviceLinkBuilder(groupName);
    }

    public DeviceLinkUpdater update(DeviceLinkJson json) {
        return new DeviceLinkUpdater(json);
    }

    @Getter
    public class DeviceLinkBuilder {
        private final String groupName;
        private String subnet;
        private RedundancyType redundancyType;
        private List<DeviceLinkCreatorJson.Link> metroLinks;
        private List<DeviceLinkCreatorJson.LinkDevice> linkDevices;

        protected DeviceLinkBuilder(String groupName) {
            this.groupName = groupName;
        }

        public DeviceLinkBuilder onSubnet(String subnet) {
            this.subnet = subnet;
            return this;
        }

        public DeviceLinkBuilder withRedundancyType(RedundancyType redundancyType) {
            this.redundancyType = redundancyType;
            return this;
        }

        public DeviceLinkBuilder forDevice(String deviceUuid, Long asn, Integer interfaceId) {
            if(this.linkDevices == null) {
                this.linkDevices = new ArrayList<>();
            }
            this.linkDevices.add(new DeviceLinkCreatorJson.LinkDevice(deviceUuid, asn, interfaceId));
            return this;
        }

        public DeviceLinkBuilder forDevice(Device device, Long asn, Integer interfaceId) {
            return forDevice(device.getUuid(), asn, interfaceId);
        }

        public DeviceLinkBuilder withLink(String accountNumber, String throughput, String throughputUnit, MetroCode metroCode) {
            if(this.metroLinks == null) {
                this.metroLinks = new ArrayList<>();
            }
            this.metroLinks.add(new DeviceLinkCreatorJson.Link(accountNumber, throughput, throughputUnit, metroCode));
            return this;
        }

        public DeviceLink save() {
            DeviceLinkCreatorJson deviceLinkCreatorJson = new DeviceLinkCreatorJson(this);
            DeviceLinkJson deviceLinkJson = ((DeviceLinkClientImpl) DeviceLinkOperator.this.getServiceClient()).create(deviceLinkCreatorJson);
            return new DeviceLinkWrapper(deviceLinkJson, DeviceLinkOperator.this.getServiceClient());
        }
    }

    public class DeviceLinkUpdater {

        private DeviceLinkJson json;
        private DeviceLinkUpdaterJson updaterJson;

        protected DeviceLinkUpdater(DeviceLinkJson json) {
            this.json = json;
            this.updaterJson = Constants.converter().convertValue(this.json, DeviceLinkUpdaterJson.class);
        }

        public DeviceLinkUpdater withGroupName(String groupName) {
            this.updaterJson.setGroupName(groupName);
            return this;
        }

        public DeviceLinkUpdater onSubnet(String subnet) {
            this.updaterJson.setSubnet(subnet);
            return this;
        }

        public DeviceLinkUpdater withRedundancyType(RedundancyType redundancyType) {
            this.updaterJson.setRedundancyType(redundancyType);
            return this;
        }

        public DeviceLinkOperator.DeviceLinkUpdater addDevice(String deviceUuid, Long asn, Integer interfaceId) {
            List<DeviceLinkUpdaterJson.LinkDevice> linkDevices = updaterJson.getLinkDevices();
            if(linkDevices == null) {
                linkDevices = new ArrayList<>();
            }
            linkDevices.add(new DeviceLinkUpdaterJson.LinkDevice(deviceUuid, asn, interfaceId));
            updaterJson.setLinkDevices(linkDevices);
            return this;
        }

        public DeviceLinkOperator.DeviceLinkUpdater addDevice(Device device, Long asn, Integer interfaceId) {
            return addDevice(device.getUuid(), asn, interfaceId);
        }

        public DeviceLinkOperator.DeviceLinkUpdater addLink(String accountNumber, String throughput, String throughputUnit, MetroCode metroCode) {
            List<DeviceLinkUpdaterJson.Link> metroLinks = updaterJson.getMetroLinks();
            if(metroLinks == null) {
                metroLinks = new ArrayList<>();
            }
            metroLinks.add(new DeviceLinkUpdaterJson.Link(accountNumber, throughput, throughputUnit, metroCode));
            updaterJson.setMetroLinks(metroLinks);
            return this;
        }

        public DeviceLinkOperator.DeviceLinkUpdater removeDevice(String deviceUuid, Long asn, Integer interfaceId) {
            List<DeviceLinkUpdaterJson.LinkDevice> linkDevices = updaterJson.getLinkDevices();

            if(linkDevices == null) {
                return this;
            }

            updaterJson.setLinkDevices(
                    linkDevices.stream().filter(Predicate.not(linkDevice -> linkDevice.getDeviceUuid().equals(deviceUuid) && java.util.Objects.equals(linkDevice.getAsn(), asn)
                            && linkDevice.getInterfaceId().equals(interfaceId))).collect(Collectors.toList()));
            return this;
        }

        public DeviceLinkOperator.DeviceLinkUpdater removeDevice(Device device, Long asn, Integer interfaceId) {
            return removeDevice(device.getUuid(), asn, interfaceId);
        }

        public DeviceLinkOperator.DeviceLinkUpdater removeLink(String accountNumber, String throughput, String throughputUnit, MetroCode metroCode) {
            List<DeviceLinkUpdaterJson.Link> metroLinks = updaterJson.getMetroLinks();

            if(metroLinks == null) {
                return this;
            }

            updaterJson.setMetroLinks(
                    metroLinks.stream().filter(Predicate.not(link -> java.util.Objects.equals(link.getAccountNumber(), accountNumber) && java.util.Objects.equals(link.getThroughput(), throughput)
                        && java.util.Objects.equals(link.getThroughputUnit(), throughputUnit) && link.getMetroCode() == metroCode)).collect(Collectors.toList()));
            return this;
        }

        public DeviceLink save() {
            json = ((DeviceLinkClientImpl) DeviceLinkOperator.this.getServiceClient()).update(this.json.getUuid(), this.updaterJson);
            return new DeviceLinkWrapper(json, DeviceLinkOperator.this.getServiceClient());
        }
    }
}
