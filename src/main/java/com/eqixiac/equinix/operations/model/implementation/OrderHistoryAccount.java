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

package com.eqixiac.equinix.operations.model.implementation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * The account on an order history record ({@code account}). The orderhistoryv1 spec names the
 * fields {@code number} and {@code name}; the live service returns {@code accountNumber} and
 * {@code accountName} (observed 2026-11-11, and patched the same way in Equinix's Go SDK). Both
 * spellings are read.
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderHistoryAccount {

    @JsonProperty("number")
    @JsonAlias("accountNumber")
    private String number;

    @JsonProperty("name")
    @JsonAlias("accountName")
    private String name;
}
