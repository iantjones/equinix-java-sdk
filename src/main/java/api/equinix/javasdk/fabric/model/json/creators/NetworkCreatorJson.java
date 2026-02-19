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

import api.equinix.javasdk.fabric.enums.NetworkScope;
import api.equinix.javasdk.fabric.enums.NetworkType;
import api.equinix.javasdk.fabric.model.Project;
import api.equinix.javasdk.fabric.model.implementation.MinimalLocation;
import api.equinix.javasdk.fabric.model.implementation.Notification;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Setter;

import java.util.List;

@Setter(AccessLevel.PRIVATE)
public class NetworkCreatorJson {

    @JsonProperty("type")
    private NetworkType type;

    @JsonProperty("name")
    private String name;

    @JsonProperty("scope")
    private NetworkScope scope;

    @JsonProperty("project")
    private Project project;

    @JsonProperty("location")
    private MinimalLocation location;

    @JsonProperty("notifications")
    private List<Notification> notifications;

    public NetworkCreatorJson(NetworkOperator.NetworkBuilder networkBuilder) {
        this.type = networkBuilder.getType();
        this.name = networkBuilder.getName();
        this.scope = networkBuilder.getScope();
        this.project = networkBuilder.getProject();
        this.location = networkBuilder.getLocation();
        this.notifications = networkBuilder.getNotifications();
    }
}
