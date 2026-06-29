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

import api.equinix.javasdk.customerportal.enums.SecureCabinetContactAvailability;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * Phone details for a secure cabinet technical contact ({@code Phone} schema). The phone
 * {@code number} (full international format) and {@code availability} are required.
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SecureCabinetContactPhone {

    @JsonProperty("number")
    private final String number;

    @JsonProperty("availability")
    private final SecureCabinetContactAvailability availability;

    /**
     * Creates phone details for a technical contact.
     *
     * @param number       a phone number in full international format (required)
     * @param availability the best time to call (required)
     */
    public SecureCabinetContactPhone(String number, SecureCabinetContactAvailability availability) {
        this.number = number;
        this.availability = availability;
    }
}
