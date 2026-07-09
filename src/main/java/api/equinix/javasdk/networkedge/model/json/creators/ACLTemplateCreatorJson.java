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

import api.equinix.javasdk.networkedge.enums.Protocol;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 *
 * @author ianjones
 */
@Getter
public class ACLTemplateCreatorJson {

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("projectId")
    private String projectId;

    @JsonProperty("inboundRules")
    List<InboundRule> inboundRules;

    @JsonIgnore
    private String accountUcmId;

    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    static class InboundRule {

        @JsonProperty("protocol")
        private Protocol protocol;

        @JsonProperty("srcPort")
        private String srcPort;

        @JsonProperty("dstPort")
        private String dstPort;

        @JsonProperty("subnet")
        private String subnet;

        @JsonProperty("seqNo")
        private Integer seqNo;

        @JsonProperty("description")
        private String description;

        /**
         * Explicit constructor replacing the Lombok-generated {@code @AllArgsConstructor}: the
         * adjacent same-typed {@code String} parameters ({@code srcPort}/{@code dstPort} in
         * particular — a swap writes a wrong firewall rule) are pinned here in code rather
         * than by field declaration order.
         *
         * @param protocol    the rule protocol
         * @param srcPort     the source port or range
         * @param dstPort     the destination port or range
         * @param subnet      the source subnet CIDR
         * @param seqNo       the rule sequence number
         * @param description the rule description
         */
        InboundRule(Protocol protocol, String srcPort, String dstPort, String subnet,
                    Integer seqNo, String description) {
            this.protocol = protocol;
            this.srcPort = srcPort;
            this.dstPort = dstPort;
            this.subnet = subnet;
            this.seqNo = seqNo;
            this.description = description;
        }
    }

    ACLTemplateCreatorJson(ACLTemplateOperator.ACLTemplateBuilder deviceLinkBuilder) {
        this.name = deviceLinkBuilder.getName();
        this.description = deviceLinkBuilder.getDescription();
        this.projectId = deviceLinkBuilder.getProjectId();
        this.inboundRules = deviceLinkBuilder.getInboundRules();
        // accountUcmId is @JsonIgnore (sent as a query param, not in the body) but must be carried
        // through so create()/getByUuid() can attach it; it was previously dropped here.
        this.accountUcmId = deviceLinkBuilder.getAccountUcmId();
    }
}
