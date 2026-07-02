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

import api.equinix.javasdk.fabric.enums.NetworkScope;
import api.equinix.javasdk.fabric.enums.NetworkType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * The Fabric network used at an access-point selector (spec schema
 * {@code SimplifiedTokenNetwork}). Only {@code uuid} is sent on requests; the remaining
 * members are populated on service-token reads.
 */
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SimplifiedTokenNetwork extends NetworkRef {

    public SimplifiedTokenNetwork(String uuid) {
        super(uuid);
    }

    @JsonProperty("href")
    private String href;

    @JsonProperty("type")
    private NetworkType type;

    @JsonProperty("name")
    private String name;

    @JsonProperty("scope")
    private NetworkScope scope;

    @JsonProperty("location")
    private Location location;
}
