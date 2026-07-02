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

package api.equinix.javasdk.fabric.model.json;

import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.model.deserializers.LocalDateTimeDeserializer;
import api.equinix.javasdk.fabric.enums.ServiceTokenState;
import api.equinix.javasdk.fabric.enums.ServiceTokenType;
import api.equinix.javasdk.fabric.model.implementation.AccountSummary;
import api.equinix.javasdk.fabric.model.implementation.Notification;
import api.equinix.javasdk.fabric.model.implementation.ChangeLog;
import api.equinix.javasdk.fabric.model.implementation.Connection;
import api.equinix.javasdk.fabric.model.Project;
import api.equinix.javasdk.fabric.model.ServiceToken;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 *
 * @author ianjones
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ServiceTokenJson {

    @Getter static TypeReference<List<ServiceTokenJson>> listTypeRef = new TypeReference<>() {};

    @JsonProperty("uuid")
    private String uuid;

    @JsonProperty("type")
    private ServiceTokenType type;

    @JsonProperty("href")
    private String href;

    @JsonProperty("state")
    private ServiceTokenState state;

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    /**
     * Information about the side of the connection the token issuer is on. Deprecated in the spec.
     */
    @JsonProperty("issuerSide")
    private String issuerSide;

    @JsonProperty("expiry")
    private Integer expiry;

    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonProperty("expirationDateTime")
    private LocalDateTime expirationDateTime;

    @JsonProperty("connection")
    private Connection connection;

    @JsonProperty("notifications")
    private List<Notification> notifications;

    @JsonProperty("account")
    private AccountSummary account;

    @JsonProperty("project")
    private Project project;

    @JsonProperty("changeLog")
    private ChangeLog changeLog;
}
