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

import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.networkedge.enums.RedundancyType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 *
 * @author ianjones
 */
@Setter(AccessLevel.PRIVATE)
public class DeviceLinkCreatorJson {

    @JsonProperty("groupName")
    private final String groupName;

    @JsonProperty("subnet")
    private final String subnet;

    @JsonProperty("redundancyType")
    private final RedundancyType redundancyType;

    @JsonProperty("metroLinks")
    private final List<Link> metroLinks;

    @JsonProperty("linkDevices")
    private final List<LinkDevice> linkDevices;

    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    static class Link {
        @JsonProperty("accountNumber")
        private String accountNumber;

        @JsonProperty("throughput")
        private String throughput;

        @JsonProperty("throughputUnit")
        private String throughputUnit;

        @JsonProperty("metroCode")
        private MetroCode metroCode;

        /**
         * Explicit constructor replacing the Lombok-generated {@code @AllArgsConstructor}: the
         * three adjacent same-typed {@code String} parameters are pinned here in code rather
         * than by field declaration order.
         *
         * @param accountNumber  the billing account number
         * @param throughput     the link throughput value
         * @param throughputUnit the link throughput unit
         * @param metroCode      the metro of the link
         */
        Link(String accountNumber, String throughput, String throughputUnit, MetroCode metroCode) {
            this.accountNumber = accountNumber;
            this.throughput = throughput;
            this.throughputUnit = throughputUnit;
            this.metroCode = metroCode;
        }
    }

    @AllArgsConstructor(access = AccessLevel.PACKAGE)
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    static class LinkDevice {
        @JsonProperty("deviceUuid")
        private String deviceUuid;

        @JsonProperty("asn")
        private Long asn;

        @JsonProperty("interfaceId")
        private Integer interfaceId;
    }

    DeviceLinkCreatorJson(DeviceLinkOperator.DeviceLinkBuilder deviceLinkBuilder) {
        this.groupName = deviceLinkBuilder.getGroupName();
        this.subnet = deviceLinkBuilder.getSubnet();
        this.redundancyType = deviceLinkBuilder.getRedundancyType();
        this.metroLinks = deviceLinkBuilder.getMetroLinks();
        this.linkDevices = deviceLinkBuilder.getLinkDevices();
    }
}
