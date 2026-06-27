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
     * @return a {@link api.equinix.javasdk.fabric.model.json.creators.CloudRouterOperator.CloudRouterUpdater} object.
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
        private List<CloudRouterCreatorJson.NotificationRef> notifications = new ArrayList<>();

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

        public CloudRouterBuilder withPackage(String packageCode) {
            this.routerPackage = new CloudRouterCreatorJson.PackageRef(packageCode);
            return this;
        }

        public CloudRouterBuilder purchaseOrderNumber(String purchaseOrderNumber) {
            this.order = new CloudRouterCreatorJson.OrderRef(purchaseOrderNumber);
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

        public CloudRouterBuilder notification(String type, List<String> emails) {
            this.notifications.add(new CloudRouterCreatorJson.NotificationRef(type, emails));
            return this;
        }

        public CloudRouter create() {
            CloudRouterCreatorJson creatorJson = new CloudRouterCreatorJson(this);
            CloudRouterJson cloudRouterJson = ((CloudRouterClientImpl) CloudRouterOperator.this.getServiceClient()).create(creatorJson);
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
        public CloudRouterUpdater changePackage(String packageCode) {
            operations.add(PatchOperation.replace("/package/code", packageCode));
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
