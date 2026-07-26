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

package com.eqixiac.equinix.ibxsmartview.model.json.creators;

import com.eqixiac.equinix.ibxsmartview.enums.PowerLevelType;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * Typed request body for the legacy {@code /power/v1/current} POST endpoint
 * ({@code PowerCurrentPostRequest}). Carries the account number, IBX code and the
 * power-hierarchy level type to query power data for.
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PowerCurrentPostRequest {

    @JsonProperty("accountNo")
    private final String accountNo;

    @JsonProperty("ibx")
    private final String ibx;

    @JsonProperty("levelType")
    private final PowerLevelType levelType;

    public PowerCurrentPostRequest(String accountNo, String ibx, PowerLevelType levelType) {
        this.accountNo = accountNo;
        this.ibx = ibx;
        this.levelType = levelType;
    }
}
