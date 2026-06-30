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

import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.core.model.MetroId;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 *
 * @author ianjones
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConnectedMetro {

    /**
     * The connected metro's raw code, held as a string so a metro not listed by {@link MetroCode}
     * is preserved verbatim (see {@link #metroId()}) rather than collapsing to
     * {@link MetroCode#UNKNOWN}.
     */
    @JsonProperty("code")
    private String codeValue;

    @JsonProperty("href")
    private String href;

    @JsonProperty("avgLatency")
    private Double avgLatency;

    /**
     * @return the connected metro's code as a {@link MetroCode} enum, or {@link MetroCode#UNKNOWN}
     *         for a metro this enum does not list; prefer {@link #metroId()} for the exact code
     */
    @JsonIgnore
    public MetroCode getCode() {
        return MetroCode.fromCode(codeValue);
    }

    /**
     * @return the connected metro's forward-compatible {@link MetroId}, or {@code null} if no code
     *         was returned
     */
    @JsonIgnore
    public MetroId metroId() {
        return codeValue == null ? null : MetroId.of(codeValue);
    }
}
