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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * <p>Region class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public enum Region implements APIParam  {

    @JsonProperty("AMER")
    AMER("AMER", "Americas"),

    @JsonProperty("EMEA")
    EMEA("EMEA", "Europe, Middle East, Africa"),

    @JsonProperty("APAC")
    APAC("APAC", "Asia Pacific");

    private String regionCode;
    private String regionDesc;

    Region(String regionCode, String regionDesc) {
        this.regionCode = regionCode;
        this.regionDesc = regionDesc;
    }

    /**
     * <p>Getter for the field <code>regionCode</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @JsonValue
    public String getRegionCode() {
        return regionCode;
    }

    /**
     * <p>Setter for the field <code>regionCode</code>.</p>
     *
     * @param regionCode a {@link java.lang.String} object.
     */
    public void setRegionCode(String regionCode) {
        this.regionCode = regionCode;
    }

    /**
     * <p>Getter for the field <code>regionDesc</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getRegionDesc() {
        return regionDesc;
    }

    /**
     * <p>Setter for the field <code>regionDesc</code>.</p>
     *
     * @param regionDesc a {@link java.lang.String} object.
     */
    public void setRegionDesc(String regionDesc) {
        this.regionDesc = regionDesc;
    }
}
