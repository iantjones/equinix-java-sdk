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

package com.eqixiac.equinix.internetaccess.model.implementation;

import com.eqixiac.equinix.internetaccess.enums.OrderSignatory;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * Signature configuration of an Equinix Internet Access (EIA) v2 service order.
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderSignature {

    @JsonProperty("signatory")
    private OrderSignatory signatory;

    @JsonProperty("delegate")
    private Delegate delegate;

    /**
     * Delegate the signature request is sent to when the signatory is {@code DELEGATE}.
     */
    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Delegate {

        @JsonProperty("firstName")
        private String firstName;

        @JsonProperty("lastName")
        private String lastName;

        @JsonProperty("email")
        private String email;
    }
}
