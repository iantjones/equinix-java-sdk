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

import api.equinix.javasdk.fabric.enums.ConnectionState;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.model.KeyValuePair;
import api.equinix.javasdk.fabric.enums.ConnectionType;
import api.equinix.javasdk.fabric.enums.Direction;
import api.equinix.javasdk.fabric.enums.GeoScopeType;
import api.equinix.javasdk.fabric.model.Connection;
import api.equinix.javasdk.fabric.model.Project;
import api.equinix.javasdk.fabric.model.implementation.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

/**
 *
 * @author ianjones
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public final class ConnectionJson {

    @JsonProperty("uuid")
    private String uuid;

    @JsonProperty("type")
    private ConnectionType type;

    @JsonProperty("href")
    private String href;

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("state")
    private ConnectionState state;

    @JsonProperty("geoScope")
    private GeoScopeType geoScope;

    @JsonProperty("order")
    private Order order;

    @JsonProperty("operation")
    private ConnectionOperation operation;

    @JsonProperty("notifications")
    private List<Notification> notifications;

    @JsonProperty("account")
    private Account account;

    @JsonProperty("changeLog")
    private ChangeLog changeLog;

    @JsonProperty("bandwidth")
    private Integer bandwidth;

    @JsonProperty("redundancy")
    private Redundancy redundancy;

    @JsonProperty("isRemote")
    private Boolean isRemote;

    @JsonProperty("direction")
    private Direction direction;

    @JsonProperty("aSide")
    private ConnectionSide aSide;

    @JsonProperty("zSide")
    private ConnectionSide zSide;

    @JsonProperty("additionalInfo")
    private List<KeyValuePair> additionalInfo;

    @JsonProperty("marketplaceSubscription")
    private MarketplaceSubscriptionRef marketplaceSubscription;

    @JsonProperty("project")
    private Project project;

    @JsonProperty("change")
    private Change change;
}
