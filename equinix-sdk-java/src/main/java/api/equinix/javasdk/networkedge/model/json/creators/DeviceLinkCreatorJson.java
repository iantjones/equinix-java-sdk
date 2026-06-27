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

import api.equinix.javasdk.core.enums.BandwidthUnit;
import api.equinix.javasdk.core.enums.MetroCode;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * <p>DeviceLinkCreatorJson class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
@Setter(AccessLevel.PRIVATE)
public class DeviceLinkCreatorJson {

    @JsonProperty("groupName")
    private final String groupName;

    @JsonProperty("subnet")
    private final String subnet;

    @JsonProperty("links")
    private final List<Link> links;

    @JsonProperty("linkDevices")
    private final List<LinkDevice> linkDevices;

    @AllArgsConstructor(access = AccessLevel.PACKAGE)
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    static class Link {
        @JsonProperty("accountNumber")
        private Integer accountNumber;

        @JsonProperty("sourceMetroCode")
        private MetroCode sourceMetroCode;

        @JsonProperty("destinationMetroCode")
        private MetroCode destinationMetroCode;

        @JsonProperty("throughput")
        private Double throughput;

        @JsonProperty("throughputUnit")
        private BandwidthUnit throughputUnit;
    }

    @AllArgsConstructor(access = AccessLevel.PACKAGE)
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    static class LinkDevice {
        @JsonProperty("deviceUuid")
        private String deviceUuid;

        @JsonProperty("asn")
        private Integer asn;

        @JsonProperty("interfaceId")
        private Integer interfaceId;
    }

    DeviceLinkCreatorJson(DeviceLinkOperator.DeviceLinkBuilder deviceLinkBuilder) {
        this.groupName = deviceLinkBuilder.getGroupName();
        this.subnet = deviceLinkBuilder.getSubnet();
        this.links = deviceLinkBuilder.getLinks();
        this.linkDevices = deviceLinkBuilder.getLinkDevices();
    }
}
