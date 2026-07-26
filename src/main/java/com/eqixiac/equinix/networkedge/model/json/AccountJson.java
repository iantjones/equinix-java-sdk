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

package com.eqixiac.equinix.networkedge.model.json;

import com.eqixiac.equinix.core.enums.MetroCode;
import com.eqixiac.equinix.networkedge.enums.AccountStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author ianjones
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
public class AccountJson {

    @Getter static TypeReference<AccountJson.NestedList> listTypeRef = new TypeReference<>() {};

    /**
     * The listAccounts endpoint returns a {@code PageResponseMetroAccountResponse} object
     * ({@code {accountCreateUrl, pagination, data:[...]}}); the account list is carried under the
     * {@code data} property.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Getter
    public static class NestedList {
        @JsonProperty("data")
        private List<AccountJson> data;
    }

    @JsonProperty("accountName")
    private String accountName;

    @JsonProperty("accountNumber")
    private Integer accountNumber;

    @JsonProperty("accountUcmId")
    private String accountUcmId;

    @JsonProperty("accountStatus")
    private AccountStatus accountStatus;

    @JsonProperty("referenceId")
    private String referenceId;

    @JsonProperty("metros")
    private ArrayList<MetroCode> metros;

    @JsonProperty("creditHold")
    private Boolean creditHold;
}
