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

package api.equinix.javasdk.internetaccess.model.json.creators;

import api.equinix.javasdk.internetaccess.enums.RoutingConfigType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

@Getter
public class RoutingConfigCreatorJson {

    @JsonProperty("type") private RoutingConfigType type;
    @JsonProperty("asn") private Long asn;
    @JsonProperty("prefixes") private List<String> prefixes;

    public RoutingConfigCreatorJson(RoutingConfigOperator.RoutingConfigBuilder builder) {
        this.type = builder.getType();
        this.asn = builder.getAsn();
        this.prefixes = builder.getPrefixes();
    }
}
