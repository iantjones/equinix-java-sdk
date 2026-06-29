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

package api.equinix.javasdk.customerportal.model.json.creators;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * A device referenced by a smart hands move-jumper-cable or run-jumper-cable order
 * ({@code device} in the smart hands v1 spec). {@code name}, {@code slot} and {@code port} are
 * all required.
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SmartHandsDevice {

    @JsonProperty("name")
    private final String name;

    @JsonProperty("slot")
    private final String slot;

    @JsonProperty("port")
    private final String port;

    @JsonCreator
    public SmartHandsDevice(@JsonProperty("name") String name, @JsonProperty("slot") String slot,
                            @JsonProperty("port") String port) {
        this.name = name;
        this.slot = slot;
        this.port = port;
    }
}
