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

package api.equinix.javasdk.ibxsmartview.model.implementation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A recipient to be notified when a power alert condition is met. Used both when reading an
 * existing power alert configuration and when supplying recipients for a create or update request.
 */
@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PowerAlertRecipient {

    @JsonProperty("firstName")
    private String firstName;

    @JsonProperty("lastName")
    private String lastName;

    @JsonProperty("sms")
    private PowerAlertContactMethod sms;

    @JsonProperty("email")
    private PowerAlertContactMethod email;

    /**
     * Creates a recipient.
     *
     * @param firstName the recipient's first name
     * @param lastName the recipient's last name
     * @param sms the SMS contact method, or {@code null} if not configured
     * @param email the email contact method, or {@code null} if not configured
     */
    public PowerAlertRecipient(String firstName, String lastName, PowerAlertContactMethod sms, PowerAlertContactMethod email) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.sms = sms;
        this.email = email;
    }
}
