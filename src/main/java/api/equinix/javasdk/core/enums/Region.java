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

package api.equinix.javasdk.core.enums;

import api.equinix.javasdk.core.model.APIParam;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * The three Equinix geographic regions. Serializes on the wire as the region code
 * (via {@code getRegionCode()}).
 *
 * @author ianjones
 */
public enum Region implements APIParam  {

    AMER("AMER", "Americas"),

    EMEA("EMEA", "Europe, Middle East, Africa"),

    APAC("APAC", "Asia Pacific");

    private final String regionCode;
    @Getter private final String regionDesc;

    Region(String regionCode, String regionDesc) {
        this.regionCode = regionCode;
        this.regionDesc = regionDesc;
    }

    @JsonValue
    public String getRegionCode() {
        return regionCode;
    }
}
