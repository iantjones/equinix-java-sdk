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
import api.equinix.javasdk.core.enums.BandwidthUnit;
import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.networkedge.client.internal.implementation.DeviceLinkClientImpl;
import api.equinix.javasdk.networkedge.model.DeviceLink;
import api.equinix.javasdk.networkedge.model.json.DeviceLinkJson;
import api.equinix.javasdk.networkedge.model.wrappers.DeviceLinkWrapper;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * <p>DeviceLinkOperator class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class DeviceLinkOperator extends ResourceImpl<DeviceLink> {

    @Getter
    private final Pageable<DeviceLink> serviceClient;

    /**
     * <p>Constructor for DeviceLinkOperator.</p>
     *
     * @param serviceClient a {@link api.equinix.javasdk.core.http.response.Pageable} object.
     */
    public DeviceLinkOperator(Pageable<DeviceLink> serviceClient) {
        this.serviceClient = serviceClient;
    }

    /**
     * <p>create.</p>
     *
     * @param groupName a {@link java.lang.String} object.
     * @return a {@link api.equinix.javasdk.networkedge.model.json.creators.DeviceLinkOperator.DeviceLinkBuilder} object.
     */
    public DeviceLinkBuilder create(String groupName) {
        return new DeviceLinkBuilder(groupName);
    }

    /**
     * <p>update.</p>
     *
     * @param json a {@link api.equinix.javasdk.networkedge.model.json.DeviceLinkJson} object.
     * @return a {@link api.equinix.javasdk.networkedge.model.json.creators.DeviceLinkOperator.DeviceLinkUpdater} object.
     */
    public DeviceLinkUpdater update(DeviceLinkJson json) {
        return new DeviceLinkUpdater(json);
    }

    @Getter
    public class DeviceLinkBuilder {
        private final String groupName;
        private String subnet;
        private List<DeviceLinkCreatorJson.Link> links;
        private List<DeviceLinkCreatorJson.LinkDevice> linkDevices;

        protected DeviceLinkBuilder(String groupName) {
            this.groupName = groupName;
        }

        public DeviceLinkBuilder onSubnet(String subnet) {
            this.subnet = subnet;
            return this;
        }

        public DeviceLinkBuilder forDevice(String deviceUuid, Integer asn, Integer interfaceId) {
            if(this.linkDevices == null) {
                this.linkDevices = new ArrayList<>();
            }
            this.linkDevices.add(new DeviceLinkCreatorJson.LinkDevice(deviceUuid, asn, interfaceId));
            return this;
        }

        public DeviceLinkBuilder withLink(Integer accountNumber, Integer throughput, BandwidthUnit throughputUnit, MetroCode sourceMetroCode, MetroCode destinationMetroCode) {
            if(this.links == null) {
                this.links = new ArrayList<>();
            }
            this.links.add(new DeviceLinkCreatorJson.Link(accountNumber, sourceMetroCode, destinationMetroCode, throughput.doubleValue(), throughputUnit));
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
            this.updaterJson = Constants.JSON_CONVERTOR.convertValue(this.json, DeviceLinkUpdaterJson.class);
        }

        public DeviceLinkUpdater withGroupName(String groupName) {
            this.updaterJson.setGroupName(groupName);
            return this;
        }

        public DeviceLinkUpdater onSubnet(String subnet) {
            this.updaterJson.setSubnet(subnet);
            return this;
        }

        public DeviceLinkOperator.DeviceLinkUpdater addDevice(String deviceUuid, Integer asn, Integer interfaceId) {
            List<DeviceLinkUpdaterJson.LinkDevice> linkDevices = updaterJson.getLinkDevices();
            if(linkDevices == null) {
                linkDevices = new ArrayList<>();
            }
            linkDevices.add(new DeviceLinkUpdaterJson.LinkDevice(deviceUuid, asn, interfaceId));
            updaterJson.setLinkDevices(linkDevices);
            return this;
        }

        public DeviceLinkOperator.DeviceLinkUpdater addLink(Integer accountNumber, Integer throughput, BandwidthUnit throughputUnit, MetroCode sourceMetroCode, MetroCode destinationMetroCode) {
            List<DeviceLinkUpdaterJson.Link> links = updaterJson.getLinks();
            if(links == null) {
                links = new ArrayList<>();
            }
            links.add(new DeviceLinkUpdaterJson.Link(accountNumber, sourceMetroCode, destinationMetroCode, throughput, throughputUnit));
            updaterJson.setLinks(links);
            return this;
        }

        public DeviceLinkOperator.DeviceLinkUpdater removeDevice(String deviceUuid, Integer asn, Integer interfaceId) {
            List<DeviceLinkUpdaterJson.LinkDevice> linkDevices = updaterJson.getLinkDevices();

            if(linkDevices == null) {
                return this;
            }

            updaterJson.setLinkDevices(
                    linkDevices.stream().filter(Predicate.not(linkDevice -> linkDevice.getDeviceUuid().equals(deviceUuid) && linkDevice.getAsn().equals(asn)
                            && linkDevice.getInterfaceId().equals(interfaceId))).collect(Collectors.toList()));
            return this;
        }

        public DeviceLinkOperator.DeviceLinkUpdater removeLink(Integer accountNumber, Integer throughput, BandwidthUnit throughputUnit, MetroCode sourceMetroCode, MetroCode destinationMetroCode) {
            List<DeviceLinkUpdaterJson.Link> links = updaterJson.getLinks();

            if(links == null) {
                return this;
            }

            updaterJson.setLinks(
                    links.stream().filter(Predicate.not(link -> link.getAccountNumber().equals(accountNumber) && link.getThroughput().equals(throughput) && link.getThroughputUnit() == throughputUnit
                        && link.getSourceMetroCode() == sourceMetroCode && link.getDestinationMetroCode() == destinationMetroCode)).collect(Collectors.toList()));
            return this;
        }

        public DeviceLink save() {
            json = ((DeviceLinkClientImpl) DeviceLinkOperator.this.getServiceClient()).update(this.json.getUuid(), this.updaterJson);
            return new DeviceLinkWrapper(json, DeviceLinkOperator.this.getServiceClient());
        }
    }
}
