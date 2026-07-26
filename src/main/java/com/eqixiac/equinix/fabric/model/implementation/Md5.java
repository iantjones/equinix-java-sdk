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

package com.eqixiac.equinix.fabric.model.implementation;

import com.eqixiac.equinix.fabric.enums.Md5Type;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * An NTP MD5 authentication entry (the Fabric v4 {@code md5} schema), used as the items of a
 * Precision Time service's {@code ntpAdvancedConfiguration}. {@code type} is {@code ASCII} or
 * {@code HEX}; {@code key} is the Base64-encoded plaintext authentication key.
 *
 * @author ianjones
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Md5 {

    @JsonProperty("type")
    private Md5Type type;

    @JsonProperty("keyNumber")
    private Integer keyNumber;

    @JsonProperty("key")
    private String key;
}
