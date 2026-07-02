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
import api.equinix.javasdk.fabric.enums.*;
import api.equinix.javasdk.fabric.model.CloudRouter;
import api.equinix.javasdk.fabric.model.Project;
import api.equinix.javasdk.fabric.model.implementation.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Getter;

import java.util.List;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public final class CloudRouterJson {

    @Getter static TypeReference<List<CloudRouterJson>> listTypeRef = new TypeReference<>() {};

    @JsonProperty("uuid")
    private String uuid;

    @JsonProperty("href")
    private String href;

    @JsonProperty("name")
    private String name;

    @JsonProperty("type")
    private CloudRouterType type;

    @JsonProperty("state")
    private CloudRouterState state;

    @JsonProperty("location")
    private LocationCode location;

    @JsonProperty("package")
    private GatewayPackageRef routerPackage;

    @JsonProperty("order")
    private Order order;

    @JsonProperty("project")
    private Project project;

    @JsonProperty("account")
    private AccountSummary account;

    @JsonProperty("notifications")
    private List<Notification> notifications;

    @JsonProperty("equinixAsn")
    private Long equinixAsn;

    @JsonProperty("connectionsCount")
    private Integer connectionCount;

    @JsonProperty("marketplaceSubscription")
    private MarketplaceSubscriptionRef marketplaceSubscription;

    @JsonProperty("change")
    private CloudRouterChange change;

    @JsonProperty("changeLog")
    private ChangeLog changeLog;
}
