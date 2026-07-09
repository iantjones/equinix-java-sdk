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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Order delegate details for a port order signature (the Fabric v4
 * {@code PortOrderSignatureDelegate} schema).
 *
 * <p>Prefer {@code builder()} over the positional constructor — all three parameters are
 * {@code String}s.</p>
 *
 * @author ianjones
 */
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PortOrderSignatureDelegate {

    @JsonProperty("firstName")
    private String firstName;

    @JsonProperty("lastName")
    private String lastName;

    @JsonProperty("email")
    private String email;

    /**
     * Explicit constructor replacing the Lombok-generated {@code @AllArgsConstructor}: the
     * argument order is pinned here in code (three same-typed {@code String} parameters)
     * rather than by field declaration order.
     *
     * @param firstName the delegate's first name
     * @param lastName  the delegate's last name
     * @param email     the delegate's email address
     */
    @Builder
    public PortOrderSignatureDelegate(String firstName, String lastName, String email) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
    }
}
