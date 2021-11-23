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
import api.equinix.javasdk.core.model.Serializable;
import api.equinix.javasdk.fabric.enums.ServiceProfileState;
import api.equinix.javasdk.fabric.enums.ServiceProfileType;
import api.equinix.javasdk.fabric.model.Service;
import api.equinix.javasdk.fabric.model.implementation.Account;
import api.equinix.javasdk.fabric.model.implementation.ChangeLog;
import api.equinix.javasdk.fabric.model.implementation.ServiceConfig;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Getter;

import java.util.List;

/**
 * <p>ServiceJson class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
@Getter
public final class ServiceJson implements Serializable {

    /**
     * <p>pagedTypeRef.</p>
     *
     * @return a {@link com.fasterxml.jackson.core.type.TypeReference} object.
     */
    public static TypeReference<Page<Service, ServiceJson>> pagedTypeRef() {
        return new TypeReference<>() {};
    }

    /**
     * <p>singleTypeRef.</p>
     *
     * @return a {@link com.fasterxml.jackson.core.type.TypeReference} object.
     */
    public static TypeReference<ServiceJson> singleTypeRef() {
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

    @JsonProperty("config")
    private ServiceConfig config;

    @JsonProperty("changeLog")
    private ChangeLog changeLog;

    @JsonProperty("account")
    private Account account;
}
