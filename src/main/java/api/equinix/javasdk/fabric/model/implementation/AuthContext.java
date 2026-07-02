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
import api.equinix.javasdk.fabric.enums.CloudEventAuthType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * Authentication context of the actor that triggered a cloud event (the Fabric v4
 * {@code AuthContext} schema). {@code authtype} is {@code system} or {@code user};
 * {@code authid} is {@code equinix} or a user identifier.
 *
 * @author ianjones
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthContext {

    @JsonProperty("authtype")
    private CloudEventAuthType authType;

    @JsonProperty("authid")
    private String authId;

    @JsonProperty("name")
    private String name;

    @JsonProperty("email")
    private String email;
}
