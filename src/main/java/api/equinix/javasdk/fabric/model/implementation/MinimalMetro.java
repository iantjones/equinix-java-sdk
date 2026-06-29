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

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class MinimalMetro {

    /**
     * The raw metro code exactly as returned by the API. Held as a string (rather than a
     * {@link MetroCode} enum) so a metro the enum does not yet list is preserved verbatim instead of
     * collapsing to {@link MetroCode#UNKNOWN}; {@link #metroId()} exposes it forward-compatibly.
     */
    @JsonProperty("code")
    private String codeValue;

    /**
     * The metro code as a {@link MetroCode} enum, or {@link MetroCode#UNKNOWN} for a metro this enum
     * does not list. Prefer {@link #metroId()} when you need the exact code of an unlisted metro.
     *
     * @return the metro code enum constant (never null; {@code UNKNOWN} if unlisted)
     */
    @JsonIgnore
    public MetroCode getCode() {
        return MetroCode.fromCode(codeValue);
    }

    /**
     * The metro's code as a forward-compatible {@link MetroId}, preserving the exact wire value even
     * for metros absent from the {@link MetroCode} enum.
     *
     * @return the metro id, or {@code null} if the API returned no code
     */
    @JsonIgnore
    public MetroId metroId() {
        return codeValue == null ? null : MetroId.of(codeValue);
    }
}
