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

package api.equinix.javasdk.fabric.model.json.creators;

import api.equinix.javasdk.fabric.enums.IpBlockProductType;
import api.equinix.javasdk.fabric.model.Project;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Setter;

@Setter(AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IpBlockCreatorJson {

    @JsonProperty("type")
    private IpBlockProductType type;

    @JsonProperty("project")
    private Project project;

    @JsonProperty("prefixLength")
    private Integer prefixLength;

    @JsonProperty("prefix")
    private String prefix;

    public IpBlockCreatorJson(IpBlockOperator.IpBlockBuilder builder) {
        this.type = builder.getType();
        this.project = builder.getProject();
        this.prefixLength = builder.getPrefixLength();
        this.prefix = builder.getPrefix();
    }
}
