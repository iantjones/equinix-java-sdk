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

package com.eqixiac.equinix.customerportal.model.json.creators;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * Technical contact details on a secure cabinet order ({@code ContactDetails} schema).
 * {@code firstName}, {@code lastName} and {@code email} are required; {@code phone} is optional.
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SecureCabinetContact {

    @JsonProperty("firstName")
    private final String firstName;

    @JsonProperty("lastName")
    private final String lastName;

    @JsonProperty("email")
    private final String email;

    @JsonProperty("phone")
    private SecureCabinetContactPhone phone;

    /**
     * Creates a technical contact.
     *
     * @param firstName the contact's first name (required)
     * @param lastName  the contact's last name (required)
     * @param email     the contact's email address (required)
     */
    public SecureCabinetContact(String firstName, String lastName, String email) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
    }

    /**
     * Sets the contact's phone details.
     *
     * @param phone the phone details
     * @return this contact
     */
    public SecureCabinetContact phone(SecureCabinetContactPhone phone) {
        this.phone = phone;
        return this;
    }
}
