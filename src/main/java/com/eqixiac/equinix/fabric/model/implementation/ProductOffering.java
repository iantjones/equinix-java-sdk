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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Product offering of a connection z-side access point (spec 4.34.0 {@code ProductOffering},
 * "applicable to zSide only"; example {@code Colo2Sp-LMaaS-Primary}).
 */
@Getter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductOffering {

    /** Product offering identifier. */
    @JsonProperty("id")
    private String id;

    /** Product offering name. */
    @JsonProperty("name")
    private String name;

    /** Billing term in months: 1, 12, 24, 36, 48 or 60. */
    @JsonProperty("billingTerm")
    private Integer billingTerm;

    /**
     * Creates an offering reference.
     *
     * @param id the offering id
     * @param name the offering name, or {@code null}
     * @param billingTerm the billing term in months (1, 12, 24, 36, 48 or 60), or {@code null}
     */
    public ProductOffering(String id, String name, Integer billingTerm) {
        if (billingTerm != null && !java.util.Set.of(1, 12, 24, 36, 48, 60).contains(billingTerm)) {
            throw new IllegalArgumentException("billingTerm must be one of 1, 12, 24, 36, 48, 60 (spec ProductOffering)");
        }
        this.id = id;
        this.name = name;
        this.billingTerm = billingTerm;
    }
}
