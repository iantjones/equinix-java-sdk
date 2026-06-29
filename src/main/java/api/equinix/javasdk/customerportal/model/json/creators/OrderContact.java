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

import java.util.List;

/**
 * A contact on a colocation v2 order (cross-connect, shipment, work visit) or a v2 trouble
 * ticket / support case. The {@code contacts} array entries are modelled by the spec as an
 * {@code anyOf} of two variants:
 *
 * <ul>
 *   <li><b>Registered user</b> ({@code ContactRequestRegisteredUser}): {@code registeredUsers[]} +
 *       {@code type} ({@code NOTIFICATION}, {@code TECHNICAL}, or {@code ORDERING} on support
 *       cases), with optional {@code availability} and {@code timezone}.</li>
 *   <li><b>Non-registered contact</b> ({@code ContactRequestNonRegisteredUser}): {@code firstName},
 *       {@code lastName}, {@code type} ({@code TECHNICAL}), {@code details[]} of
 *       {@code {type, value}} (where {@code type} is {@code PHONE}, {@code MOBILE} or
 *       {@code EMAIL}), with optional {@code availability} and {@code timezone}.</li>
 * </ul>
 *
 * <p>Use {@link #registered(String, java.util.List)} / {@link #registered(String, java.util.List,
 * String, String)} for the first variant and {@link #nonRegistered(String, String, java.util.List)}
 * / {@link #nonRegistered(String, String, java.util.List, String, String)} for the second. Only the
 * fields relevant to the chosen variant are serialized.</p>
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderContact {

    @JsonProperty("registeredUsers")
    private final List<String> registeredUsers;

    @JsonProperty("firstName")
    private final String firstName;

    @JsonProperty("lastName")
    private final String lastName;

    @JsonProperty("type")
    private final String type;

    @JsonProperty("availability")
    private final String availability;

    @JsonProperty("timezone")
    private final String timezone;

    @JsonProperty("details")
    private final List<ContactDetail> details;

    private OrderContact(List<String> registeredUsers, String firstName, String lastName, String type,
                         String availability, String timezone, List<ContactDetail> details) {
        this.registeredUsers = registeredUsers;
        this.firstName = firstName;
        this.lastName = lastName;
        this.type = type;
        this.availability = availability;
        this.timezone = timezone;
        this.details = details;
    }

    /**
     * Builds a registered-user contact ({@code ContactRequestRegisteredUser}).
     *
     * @param type            the contact type (e.g. {@code NOTIFICATION}, {@code TECHNICAL})
     * @param registeredUsers the registered customer-portal user names
     * @return the contact
     */
    public static OrderContact registered(String type, List<String> registeredUsers) {
        return new OrderContact(registeredUsers, null, null, type, null, null, null);
    }

    /**
     * Builds a registered-user contact ({@code ContactRequestRegisteredUser}) with availability and
     * timezone.
     *
     * @param type            the contact type (e.g. {@code NOTIFICATION}, {@code TECHNICAL})
     * @param registeredUsers the registered customer-portal user names
     * @param availability    the contact's availability ({@code WORK_HOURS} or {@code ANYTIME})
     * @param timezone        the contact's timezone
     * @return the contact
     */
    public static OrderContact registered(String type, List<String> registeredUsers, String availability, String timezone) {
        return new OrderContact(registeredUsers, null, null, type, availability, timezone, null);
    }

    /**
     * Builds a non-registered contact ({@code ContactRequestNonRegisteredUser}).
     *
     * @param firstName the contact's first name
     * @param lastName  the contact's last name
     * @param details   the contact details ({@code {type, value}} entries; email is required)
     * @return the contact
     */
    public static OrderContact nonRegistered(String firstName, String lastName, List<ContactDetail> details) {
        return new OrderContact(null, firstName, lastName, "TECHNICAL", null, null, details);
    }

    /**
     * Builds a non-registered contact ({@code ContactRequestNonRegisteredUser}) with availability
     * and timezone.
     *
     * @param firstName    the contact's first name
     * @param lastName     the contact's last name
     * @param details      the contact details ({@code {type, value}} entries; email is required)
     * @param availability the contact's availability ({@code WORK_HOURS} or {@code ANYTIME})
     * @param timezone     the contact's timezone
     * @return the contact
     */
    public static OrderContact nonRegistered(String firstName, String lastName, List<ContactDetail> details,
                                             String availability, String timezone) {
        return new OrderContact(null, firstName, lastName, "TECHNICAL", availability, timezone, details);
    }

    /**
     * A single contact-detail entry ({@code ContactsRequestDetails}) for a non-registered contact.
     */
    @Getter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ContactDetail {

        @JsonProperty("type")
        private final String type;

        @JsonProperty("value")
        private final String value;

        /**
         * @param type  the detail type ({@code PHONE}, {@code MOBILE} or {@code EMAIL})
         * @param value the detail value (email address, or phone number prefixed with country code)
         */
        public ContactDetail(String type, String value) {
            this.type = type;
            this.value = value;
        }
    }
}
