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

import api.equinix.javasdk.core.http.request.PatchOperation;
import api.equinix.javasdk.core.http.response.PageablePost;
import api.equinix.javasdk.core.model.ResourceImpl;
import api.equinix.javasdk.fabric.client.internal.implementation.CloudRouterClientImpl;
import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.fabric.enums.CloudRouterType;
import api.equinix.javasdk.fabric.enums.GatewayPackageCode;
import api.equinix.javasdk.fabric.enums.MarketplaceSubscriptionType;
import api.equinix.javasdk.fabric.enums.NotificationType;
import api.equinix.javasdk.fabric.model.CloudRouter;
import api.equinix.javasdk.fabric.model.json.CloudRouterJson;
import api.equinix.javasdk.fabric.model.wrappers.CloudRouterWrapper;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

public class CloudRouterOperator extends ResourceImpl<CloudRouter> {

    @Getter
    private final PageablePost<CloudRouter> serviceClient;

    public CloudRouterOperator(PageablePost<CloudRouter> serviceClient) {
        this.serviceClient = serviceClient;
    }

    public CloudRouterBuilder create() {
        return new CloudRouterBuilder();
    }

    /**
     * <p>Begins a fluent JSON Patch update of an existing cloud router, identified by uuid.</p>
     *
     * @param uuid the uuid of the cloud router to update
     */
    public CloudRouterUpdater update(String uuid) {
        return new CloudRouterUpdater(uuid);
    }

    @Getter
    public class CloudRouterBuilder {
        private CloudRouterType type = CloudRouterType.XF_ROUTER;
        private String name;
        private CloudRouterCreatorJson.LocationRef location;
        private CloudRouterCreatorJson.PackageRef routerPackage;
        private CloudRouterCreatorJson.OrderRef order;
        private CloudRouterCreatorJson.ProjectRef project;
        private CloudRouterCreatorJson.AccountRef account;
        private CloudRouterCreatorJson.MarketplaceSubscriptionRef marketplaceSubscription;
        private List<CloudRouterCreatorJson.NotificationRef> notifications = new ArrayList<>();
        private boolean dryRun;

        public CloudRouterBuilder name(String name) {
            this.name = name;
            return this;
        }

        public CloudRouterBuilder inMetro(String metroCode) {
            this.location = new CloudRouterCreatorJson.LocationRef(metroCode);
            return this;
        }

        public CloudRouterBuilder inMetro(MetroCode metroCode) {
            return inMetro(metroCode.toString());
        }

        /**
         * Sets the cloud router package tier (spec {@code CloudRouterPostRequestPackage.code}:
         * {@code LAB}, {@code BASIC}, {@code STANDARD}, {@code ADVANCED} or {@code PREMIUM}).
         *
         * @param packageCode the package code
         * @return this builder
         */
        public CloudRouterBuilder withPackage(GatewayPackageCode packageCode) {
            this.routerPackage = new CloudRouterCreatorJson.PackageRef(packageCode);
            return this;
        }

        public CloudRouterBuilder purchaseOrderNumber(String purchaseOrderNumber) {
            this.order = new CloudRouterCreatorJson.OrderRef(purchaseOrderNumber);
            return this;
        }

        /**
         * Sets the full order details, including an optional term commitment.
         *
         * @param purchaseOrderNumber the purchase order number, or {@code null}
         * @param termLength term length in months (1, 12, 24 or 36), or {@code null}
         * @param customerReferenceNumber the customer reference number, or {@code null}
         * @return this builder
         */
        public CloudRouterBuilder order(String purchaseOrderNumber, Integer termLength, String customerReferenceNumber) {
            this.order = new CloudRouterCreatorJson.OrderRef(purchaseOrderNumber, termLength, customerReferenceNumber);
            return this;
        }

        /**
         * Orders this Fabric Cloud Router via a cloud marketplace subscription.
         *
         * @param type the subscription type (e.g. {@code AWS_MARKETPLACE_SUBSCRIPTION})
         * @param uuid the subscription identifier
         * @return this builder
         */
        public CloudRouterBuilder marketplaceSubscription(MarketplaceSubscriptionType type, String uuid) {
            this.marketplaceSubscription = new CloudRouterCreatorJson.MarketplaceSubscriptionRef(type, uuid);
            return this;
        }

        public CloudRouterBuilder projectId(String projectId) {
            this.project = new CloudRouterCreatorJson.ProjectRef(projectId);
            return this;
        }

        public CloudRouterBuilder accountNumber(Long accountNumber) {
            this.account = new CloudRouterCreatorJson.AccountRef(accountNumber);
            return this;
        }

        /**
         * Adds a notification preference (spec {@code SimplifiedNotification}).
         *
         * @param type the notification type
         * @param emails the contact emails
         * @return this builder
         */
        public CloudRouterBuilder notification(NotificationType type, List<String> emails) {
            this.notifications.add(new CloudRouterCreatorJson.NotificationRef(type, emails));
            return this;
        }

        /**
         * Marks this create as a dry run: the request is sent with the spec's {@code dryRun=true}
         * query parameter ("option to verify that API calls will succeed"; boolean, default
         * {@code false}). Nothing is provisioned — {@link #create()} returns the validated request
         * echoed back by the API with no {@code uuid}/{@code href}/{@code state} (spec example
         * {@code CloudRouterResponseExampleDryRun}).
         *
         * @return this builder
         */
        public CloudRouterBuilder dryRun() {
            this.dryRun = true;
            return this;
        }

        public CloudRouter create() {
            CloudRouterCreatorJson creatorJson = new CloudRouterCreatorJson(this);
            CloudRouterClientImpl clientImpl = (CloudRouterClientImpl) CloudRouterOperator.this.getServiceClient();
            CloudRouterJson cloudRouterJson = dryRun
                    ? clientImpl.dryRunCreate(creatorJson)
                    : clientImpl.create(creatorJson);
            return new CloudRouterWrapper(cloudRouterJson, CloudRouterOperator.this.getServiceClient());
        }
    }

    /**
     * Fluent builder for updating an existing cloud router via RFC&nbsp;6902 JSON Patch. Each typed
     * setter records a {@code replace} operation; {@link #save()} sends them as one
     * {@code PATCH} with content-type {@code application/json-patch+json} and returns the refreshed model.
     *
     * <pre>{@code cloudRouter.update().name("New-Name").save();}</pre>
     */
    public class CloudRouterUpdater {

        private final String uuid;
        private final List<PatchOperation> operations = new ArrayList<>();

        protected CloudRouterUpdater(String uuid) {
            this.uuid = uuid;
        }

        /**
         * Replaces the cloud router name.
         *
         * @param name the new name
         * @return this updater
         */
        public CloudRouterUpdater name(String name) {
            operations.add(PatchOperation.replace("/name", name));
            return this;
        }

        /**
         * Replaces the cloud router package (tier).
         *
         * @param packageCode the new package code
         * @return this updater
         */
        public CloudRouterUpdater changePackage(GatewayPackageCode packageCode) {
            operations.add(PatchOperation.replace("/package/code", packageCode));
            return this;
        }

        /**
         * Replaces the order term length ({@code /order/termLength}), e.g. to move the cloud router
         * onto (or extend) a term commitment (Fabric releases R2025.6/R2026.1).
         *
         * @param termLength the new term length in months (for example 12, 24 or 36)
         * @return this updater
         */
        public CloudRouterUpdater termLength(Integer termLength) {
            operations.add(PatchOperation.replace("/order/termLength", termLength));
            return this;
        }

        /**
         * Adds an arbitrary JSON Patch operation, for paths not covered by the typed setters above.
         *
         * @param operation the patch operation
         * @return this updater
         */
        public CloudRouterUpdater patch(PatchOperation operation) {
            operations.add(operation);
            return this;
        }

        /**
         * Applies the accumulated changes and returns the cloud router refreshed from the server.
         *
         * @return the updated {@link api.equinix.javasdk.fabric.model.CloudRouter}
         */
        public CloudRouter save() {
            if (operations.isEmpty()) {
                throw new IllegalStateException("No changes specified; set at least one field before calling save().");
            }
            CloudRouterJson cloudRouterJson = ((CloudRouterClientImpl) CloudRouterOperator.this.getServiceClient()).update(uuid, operations);
            return new CloudRouterWrapper(cloudRouterJson, CloudRouterOperator.this.getServiceClient());
        }
    }
}
