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

package api.equinix.javasdk.fabric.model.implementation;

import api.equinix.javasdk.fabric.enums.RedundancyPriority;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Redundancy configuration shared by the spec's {@code PortRedundancy} ({@code enabled},
 * {@code group}, {@code priority}) and {@code ConnectionRedundancy} ({@code group},
 * {@code priority}) schemas. {@code enabled} only applies to ports; leave it {@code null}
 * for connections and it is omitted from the JSON.
 *
 * @author ianjones
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Redundancy {

    @JsonProperty("enabled")
    private Boolean enabled;

    @JsonProperty("group")
    private String group;

    @JsonProperty("priority")
    private RedundancyPriority priority;
}
