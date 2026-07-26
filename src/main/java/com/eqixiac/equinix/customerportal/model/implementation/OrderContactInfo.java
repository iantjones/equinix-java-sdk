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

package com.eqixiac.equinix.customerportal.model.implementation;

import com.eqixiac.equinix.customerportal.enums.ContactAvailability;
import com.eqixiac.equinix.customerportal.enums.OrderContactType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

/**
 * A related party associated with an order ({@code Contacts}). Either references a
 * {@code registeredUser} or carries a {@code firstName}/{@code lastName} pair, with a contact
 * {@code type} ({@code ORDERING}, {@code NOTIFICATION}, {@code TECHNICAL} or {@code RESELLER}),
 * optional {@code availability} ({@code WORK_HOURS} or {@code ANYTIME}) and {@code timezone}, and a
 * list of communication {@code details}.
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderContactInfo {

    @JsonProperty("registeredUser")
    private String registeredUser;

    @JsonProperty("firstName")
    private String firstName;

    @JsonProperty("lastName")
    private String lastName;

    @JsonProperty("type")
    private OrderContactType type;

    @JsonProperty("availability")
    private ContactAvailability availability;

    @JsonProperty("timezone")
    private String timezone;

    @JsonProperty("details")
    private List<OrderContactDetail> details;
}
