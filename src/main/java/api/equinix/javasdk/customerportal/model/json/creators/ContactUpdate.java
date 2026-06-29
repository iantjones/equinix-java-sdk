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
 * A notification contact on a colocation v2 order / trouble-ticket <em>update</em>
 * ({@code ContactRequestRegisteredUser_Update}). Only registered users may be updated, and the
 * {@code type} is fixed to {@code NOTIFICATION}.
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ContactUpdate {

    @JsonProperty("registeredUsers")
    private final List<String> registeredUsers;

    @JsonProperty("type")
    private final String type;

    @JsonProperty("availability")
    private final String availability;

    @JsonProperty("timezone")
    private final String timezone;

    /**
     * Builds a notification-contact update referencing one or more registered users.
     *
     * @param registeredUsers the registered customer-portal user names
     */
    public ContactUpdate(List<String> registeredUsers) {
        this(registeredUsers, null, null);
    }

    /**
     * Builds a notification-contact update referencing one or more registered users, with
     * availability and timezone.
     *
     * @param registeredUsers the registered customer-portal user names
     * @param availability    the contact's availability ({@code WORK_HOURS} or {@code ANYTIME})
     * @param timezone        the contact's timezone
     */
    public ContactUpdate(List<String> registeredUsers, String availability, String timezone) {
        this.registeredUsers = registeredUsers;
        this.type = "NOTIFICATION";
        this.availability = availability;
        this.timezone = timezone;
    }
}
