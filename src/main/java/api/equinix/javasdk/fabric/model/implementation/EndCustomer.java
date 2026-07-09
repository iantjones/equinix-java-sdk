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

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * End customer details for a connection.
 *
 * <p>Prefer {@code builder()} over the positional constructor — {@code name} and
 * {@code mdmId} are adjacent same-typed {@code String} parameters.</p>
 */
@Getter
@NoArgsConstructor
public class EndCustomer {

    @JsonProperty("isDisclosed")
    private Boolean isDisclosed;

    @JsonProperty("name")
    private String name;

    @JsonProperty("mdmId")
    private String mdmId;

    /**
     * Explicit constructor replacing the Lombok-generated {@code @AllArgsConstructor}: the
     * argument order is pinned here in code ({@code name} and {@code mdmId} are same-typed
     * {@code String} parameters) rather than by field declaration order.
     *
     * @param isDisclosed whether the end customer's identity is disclosed
     * @param name        the end customer's name
     * @param mdmId       the end customer's MDM id
     */
    @Builder
    public EndCustomer(Boolean isDisclosed, String name, String mdmId) {
        this.isDisclosed = isDisclosed;
        this.name = name;
        this.mdmId = mdmId;
    }
}
