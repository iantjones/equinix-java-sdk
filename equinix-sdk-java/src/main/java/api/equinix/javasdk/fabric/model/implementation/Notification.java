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

package api.equinix.javasdk.fabric.model.implementation;

import api.equinix.javasdk.fabric.enums.NotificationType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>Notification class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Notification {

    @JsonProperty("type")
    private NotificationType type;

    @JsonProperty("emails")
    private List<String> emails;

    /**
     * <p>addEmail.</p>
     *
     * @param email a {@link java.lang.String} object.
     */
    public void addEmail(String email) {
        if(this.emails == null) {
            this.emails = new ArrayList<>();
        }

        this.emails.add(email);
    }
}
