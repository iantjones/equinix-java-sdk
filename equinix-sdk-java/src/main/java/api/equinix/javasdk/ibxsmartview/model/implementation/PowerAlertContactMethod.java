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
 * A contact method (SMS or email) for power alert notifications. Used both when reading an
 * existing power alert configuration and when supplying recipients for a create or update request.
 */
@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PowerAlertContactMethod {

    @JsonProperty("value")
    private String value;

    @JsonProperty("enabled")
    private Boolean enabled;

    /**
     * Creates a contact method.
     *
     * @param value the contact value (phone number for SMS, email address for email)
     * @param enabled whether this contact method is enabled for notifications
     */
    public PowerAlertContactMethod(String value, Boolean enabled) {
        this.value = value;
        this.enabled = enabled;
    }
}
