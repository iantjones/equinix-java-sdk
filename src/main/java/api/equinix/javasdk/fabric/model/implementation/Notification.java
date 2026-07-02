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
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * A notification recipient set (the Fabric v4 {@code SimplifiedNotification} schema).
 *
 * @author ianjones
 */
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Notification {

    @JsonProperty("type")
    private NotificationType type;

    @JsonProperty("emails")
    private List<String> emails;

    @JsonProperty("sendInterval")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String sendInterval;

    @JsonProperty("registeredUsers")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<String> registeredUsers;

    public Notification(NotificationType type, List<String> emails) {
        this.type = type;
        this.emails = emails;
    }

    /**
     * Creates a notification recipient set with all spec attributes.
     *
     * @param type the notification type
     * @param emails the recipient email addresses
     * @param sendInterval the send interval
     * @param registeredUsers the registered user keys to notify
     */
    public Notification(NotificationType type, List<String> emails, String sendInterval, List<String> registeredUsers) {
        this.type = type;
        this.emails = emails;
        this.sendInterval = sendInterval;
        this.registeredUsers = registeredUsers;
    }

    public void addEmail(String email) {
        if(this.emails == null) {
            this.emails = new ArrayList<>();
        }

        this.emails.add(email);
    }
}
