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

package api.equinix.javasdk.internetaccess.model.implementation;

import api.equinix.javasdk.internetaccess.enums.ContactAvailability;
import api.equinix.javasdk.internetaccess.enums.ContactDetailType;
import api.equinix.javasdk.internetaccess.enums.ContactType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

/**
 * A contact attached to an Equinix Internet Access (EIA) v2 service order, as returned in the
 * service order read model.
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ContactItem {

    @JsonProperty("href")
    private String href;

    @JsonProperty("type")
    private ContactType type;

    @JsonProperty("registeredUser")
    private String registeredUser;

    @JsonProperty("firstName")
    private String firstName;

    @JsonProperty("lastName")
    private String lastName;

    @JsonProperty("timezone")
    private String timezone;

    @JsonProperty("availability")
    private ContactAvailability availability;

    @JsonProperty("notes")
    private String notes;

    @JsonProperty("details")
    private List<ContactItemDetails> details;

    /**
     * A contact detail (email/phone/etc.) of a {@link ContactItem}.
     */
    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ContactItemDetails {

        @JsonProperty("type")
        private ContactDetailType type;

        @JsonProperty("value")
        private String value;

        @JsonProperty("notes")
        private String notes;
    }
}
