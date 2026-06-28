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

package api.equinix.javasdk.customerportal.model.json.creators;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * A contact on a colocation v2 order (cross-connect, shipment, work visit, trouble ticket).
 *
 * <p>Registered customer-portal users are referenced by {@code userName} and a {@code contactType}
 * (e.g. {@code NOTIFICATION}, {@code TECHNICAL}, {@code ORDERING}); non-registered contacts may be
 * supplied inline with {@code name}, {@code email} and {@code phone}.</p>
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderContact {

    @JsonProperty("contactType")
    private final String contactType;

    @JsonProperty("userName")
    private final String userName;

    @JsonProperty("name")
    private final String name;

    @JsonProperty("email")
    private final String email;

    @JsonProperty("phone")
    private final String phone;

    private OrderContact(String contactType, String userName, String name, String email, String phone) {
        this.contactType = contactType;
        this.userName = userName;
        this.name = name;
        this.email = email;
        this.phone = phone;
    }

    /**
     * Builds a registered-user contact referenced by user name.
     *
     * @param contactType the contact type (e.g. {@code NOTIFICATION})
     * @param userName    the registered customer-portal user name
     * @return the contact
     */
    public static OrderContact registered(String contactType, String userName) {
        return new OrderContact(contactType, userName, null, null, null);
    }

    /**
     * Builds a non-registered contact specified inline.
     *
     * @param contactType the contact type
     * @param name        the contact's full name
     * @param email       the contact's email
     * @param phone       the contact's phone number
     * @return the contact
     */
    public static OrderContact nonRegistered(String contactType, String name, String email, String phone) {
        return new OrderContact(contactType, null, name, email, phone);
    }
}
