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

package api.equinix.javasdk.internetaccess.model.json.creators;

import api.equinix.javasdk.internetaccess.enums.ContactAvailability;
import api.equinix.javasdk.internetaccess.enums.ContactType;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.util.List;

/**
 * A contact ({@code ContactItem}) supplied in the {@code order} of an Equinix Internet Access (EIA)
 * v2 service create. A contact is either a registered user (by {@code registeredUser} identifier) or
 * an ad-hoc contact described inline.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ContactItemRequest {

    /** Contact type (required). */
    @JsonProperty("type") private ContactType type;

    @JsonProperty("registeredUser") private String registeredUser;
    @JsonProperty("firstName") private String firstName;
    @JsonProperty("lastName") private String lastName;
    @JsonProperty("timezone") private String timezone;
    @JsonProperty("availability") private ContactAvailability availability;
    @JsonProperty("notes") private String notes;

    @Singular("detail")
    @JsonProperty("details") private List<ContactItemDetailRequest> details;

    /**
     * A contact detail (email/phone/etc.) of a {@link ContactItemRequest}.
     */
    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ContactItemDetailRequest {

        /** One of {@code EMAIL}, {@code PHONE}, {@code MOBILE} or {@code SECONDARY_EMAIL}. */
        @JsonProperty("type") private String type;
        @JsonProperty("value") private String value;
        @JsonProperty("notes") private String notes;
    }
}
