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

package api.equinix.javasdk.fabric.model.json.creators;

import api.equinix.javasdk.core.enums.Side;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.fabric.enums.*;
import api.equinix.javasdk.fabric.model.ServiceProfile;
import api.equinix.javasdk.fabric.model.ServiceToken;
import api.equinix.javasdk.fabric.model.implementation.Account;
import api.equinix.javasdk.fabric.model.implementation.ChangeLog;
import api.equinix.javasdk.fabric.model.implementation.Notification;
import api.equinix.javasdk.fabric.model.implementation.ServiceProfileConfig;
import api.equinix.javasdk.fabric.model.json.ServiceProfileJson;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.*;

import java.util.List;

/**
 * <p>ServiceTokenJson class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
@Setter(AccessLevel.PRIVATE)
public class ServiceProfileCreatorJson {

    public static TypeReference<Page<ServiceProfile, ServiceProfileJson>> pagedTypeRef() {
        return new TypeReference<>() {};
    }

    public static TypeReference<ServiceProfileJson> singleTypeRef() {
        return new TypeReference<>() {};
    }

    @JsonProperty("uuid")
    private String uuid;

    @JsonProperty("state")
    private ServiceProfileState state;

    @JsonProperty("tags")
    private List<String> tags;

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("type")
    private ServiceProfileType type;

    @JsonProperty("notifications")
    private List<Notification> notifications;

    @JsonProperty("config")
    private ServiceProfileConfig config;

    @JsonProperty("changeLog")
    private ChangeLog changeLog;

    @JsonProperty("account")
    private Account account;


}
