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

package com.eqixiac.equinix.fabric.model.json.creators;

import com.eqixiac.equinix.fabric.enums.CloudRouterType;
import com.eqixiac.equinix.fabric.enums.GatewayPackageCode;
import com.eqixiac.equinix.fabric.enums.MarketplaceSubscriptionType;
import com.eqixiac.equinix.fabric.enums.NotificationType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

@Getter
public class CloudRouterCreatorJson {

    @JsonProperty("type") private CloudRouterType type;
    @JsonProperty("name") private String name;
    @JsonProperty("location") private LocationRef location;
    @JsonProperty("package") private PackageRef routerPackage;
    @JsonProperty("order") private OrderRef order;
    @JsonProperty("project") private ProjectRef project;
    @JsonProperty("account") private AccountRef account;
    @JsonProperty("marketplaceSubscription") private MarketplaceSubscriptionRef marketplaceSubscription;
    @JsonProperty("notifications") private List<NotificationRef> notifications;

    public CloudRouterCreatorJson(CloudRouterOperator.CloudRouterBuilder builder) {
        this.type = builder.getType();
        this.name = builder.getName();
        this.location = builder.getLocation();
        this.routerPackage = builder.getRouterPackage();
        this.order = builder.getOrder();
        this.project = builder.getProject();
        this.account = builder.getAccount();
        this.marketplaceSubscription = builder.getMarketplaceSubscription();
        this.notifications = builder.getNotifications();
    }

    @Getter
    public static class LocationRef {
        @JsonProperty("metroCode") private String metroCode;
        public LocationRef(String metroCode) { this.metroCode = metroCode; }
    }

    /** Spec schema {@code CloudRouterPostRequestPackage} ({@code code} enum: LAB/BASIC/STANDARD/ADVANCED/PREMIUM). */
    @Getter
    public static class PackageRef {
        @JsonProperty("code") private GatewayPackageCode code;
        public PackageRef(GatewayPackageCode code) { this.code = code; }
    }

    @Getter
    public static class OrderRef {
        @JsonProperty("purchaseOrderNumber") private String purchaseOrderNumber;
        /** Term length in months (1, 12, 24 or 36). */
        @JsonProperty("termLength") private Integer termLength;
        @JsonProperty("customerReferenceNumber") private String customerReferenceNumber;

        public OrderRef(String purchaseOrderNumber) { this.purchaseOrderNumber = purchaseOrderNumber; }

        public OrderRef(String purchaseOrderNumber, Integer termLength, String customerReferenceNumber) {
            this.purchaseOrderNumber = purchaseOrderNumber;
            this.termLength = termLength;
            this.customerReferenceNumber = customerReferenceNumber;
        }
    }

    /** Spec schema {@code marketplaceSubscription} (writable members: type, uuid). */
    @Getter
    public static class MarketplaceSubscriptionRef {
        @JsonProperty("type") private MarketplaceSubscriptionType type;
        @JsonProperty("uuid") private String uuid;

        public MarketplaceSubscriptionRef(MarketplaceSubscriptionType type, String uuid) {
            this.type = type;
            this.uuid = uuid;
        }
    }

    @Getter
    public static class ProjectRef {
        @JsonProperty("projectId") private String projectId;
        public ProjectRef(String projectId) { this.projectId = projectId; }
    }

    @Getter
    public static class AccountRef {
        @JsonProperty("accountNumber") private Long accountNumber;
        public AccountRef(Long accountNumber) { this.accountNumber = accountNumber; }
    }

    /** Spec schema {@code SimplifiedNotification} (writable members: type, emails). */
    @Getter
    public static class NotificationRef {
        @JsonProperty("type") private NotificationType type;
        @JsonProperty("emails") private List<String> emails;
        public NotificationRef(NotificationType type, List<String> emails) { this.type = type; this.emails = emails; }
    }
}
