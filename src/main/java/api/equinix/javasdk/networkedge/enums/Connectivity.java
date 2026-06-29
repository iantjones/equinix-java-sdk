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

package api.equinix.javasdk.networkedge.enums;

import api.equinix.javasdk.core.model.APIParam;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * <p>Connectivity class. Specifies the connectivity on a virtual device.</p>
 *
 * @author ianjones
 */
public enum Connectivity implements APIParam {
    @JsonProperty("INTERNET-ACCESS")
    INTERNET_ACCESS("INTERNET-ACCESS"),
    @JsonProperty("PRIVATE")
    PRIVATE("PRIVATE"),
    @JsonProperty("INTERNET-ACCESS-WITH-PRVT-MGMT")
    INTERNET_ACCESS_WITH_PRVT_MGMT("INTERNET-ACCESS-WITH-PRVT-MGMT");

    private final String formatted;

    Connectivity(String formatted) {
        this.formatted = formatted;
    }

    @Override
    public String toString() {
        return formatted;
    }
}
