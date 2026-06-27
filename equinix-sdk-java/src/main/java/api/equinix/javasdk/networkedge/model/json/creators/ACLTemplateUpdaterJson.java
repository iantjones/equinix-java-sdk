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
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * <p>ACLTemplateUpdaterJson class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
@Getter(AccessLevel.PACKAGE)
@Setter(AccessLevel.PACKAGE)
public class ACLTemplateUpdaterJson {

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("inboundRules")
    List<InboundRule> inboundRules;

    @JsonIgnore
    @Getter(AccessLevel.PUBLIC) private String accountUcmId;

    @Getter(AccessLevel.PACKAGE)
    @AllArgsConstructor(access = AccessLevel.PACKAGE)
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
    }
}
