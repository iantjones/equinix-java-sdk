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

import com.eqixiac.equinix.core.http.request.PatchOperation;
import com.eqixiac.equinix.core.http.response.PageablePost;
import com.eqixiac.equinix.core.model.ResourceImpl;
import com.eqixiac.equinix.fabric.client.internal.implementation.EiaServiceClientImpl;
import com.eqixiac.equinix.fabric.enums.EiaServiceType;
import com.eqixiac.equinix.fabric.enums.EiaBillingType;
import com.eqixiac.equinix.fabric.model.EiaService;
import com.eqixiac.equinix.fabric.model.Project;
import com.eqixiac.equinix.fabric.model.json.EiaServiceJson;
import com.eqixiac.equinix.fabric.model.wrappers.EiaServiceWrapper;
import lombok.AccessLevel;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author ianjones
 */
public class EiaServiceOperator extends ResourceImpl<EiaService> {

    @Getter
    private final PageablePost<EiaService> serviceClient;

    public EiaServiceOperator(PageablePost<EiaService> serviceClient) {
        this.serviceClient = serviceClient;
    }

    public EiaServiceBuilder create() {
        return new EiaServiceBuilder();
    }

    /**
     * <p>Begins a fluent update of an existing EIA service, identified by uuid.</p>
     *
     * @param uuid the uuid of the EIA service to update
     */
    public EiaServiceUpdater update(String uuid) {
        return new EiaServiceUpdater(uuid);
    }

    @Getter(AccessLevel.PACKAGE)
    public class EiaServiceBuilder {

        private EiaServiceType type;
        private String name;
        private Integer bandwidth;
        private Integer bandwidthCommit;
        private EiaRoutingProtocolRequest routingProtocol;
        private Project project;
        private String accountNumber;
        private EiaBillingType billingType;
        private String purchaseOrderNumber;

        protected EiaServiceBuilder() {
        }

        public EiaServiceOperator.EiaServiceBuilder ofType(EiaServiceType type) {
            this.type = type;
            return this;
        }

        public EiaServiceOperator.EiaServiceBuilder name(String name) {
            this.name = name;
            return this;
        }

        public EiaServiceOperator.EiaServiceBuilder bandwidth(Integer bandwidth) {
            this.bandwidth = bandwidth;
            return this;
        }

        public EiaServiceOperator.EiaServiceBuilder bandwidthCommit(Integer bandwidthCommit) {
            this.bandwidthCommit = bandwidthCommit;
            return this;
        }

        public EiaServiceOperator.EiaServiceBuilder withRoutingProtocol(EiaRoutingProtocolRequest routingProtocol) {
            this.routingProtocol = routingProtocol;
            return this;
        }

        public EiaServiceOperator.EiaServiceBuilder withProject(Project project) {
            this.project = project;
            return this;
        }

        public EiaServiceOperator.EiaServiceBuilder withAccountNumber(String accountNumber) {
            this.accountNumber = accountNumber;
            return this;
        }

        public EiaServiceOperator.EiaServiceBuilder withBillingType(EiaBillingType billingType) {
            this.billingType = billingType;
            return this;
        }

        public EiaServiceOperator.EiaServiceBuilder purchaseOrderNumber(String purchaseOrderNumber) {
            this.purchaseOrderNumber = purchaseOrderNumber;
            return this;
        }

        public EiaService create() {
            EiaServiceCreatorJson eiaServiceCreatorJson = new EiaServiceCreatorJson(this);
            EiaServiceJson eiaServiceJson = ((EiaServiceClientImpl) EiaServiceOperator.this.getServiceClient()).create(eiaServiceCreatorJson);
            return new EiaServiceWrapper(eiaServiceJson, EiaServiceOperator.this.getServiceClient());
        }
    }

    /**
     * Fluent builder for updating an existing EIA service. Each typed setter records a change
     * operation along one of the spec's allowed patch paths ({@code /bandwidth},
     * {@code /bandwidthCommit}, {@code /order/purchaseOrderNumber}, and
     * {@code /routingProtocol/customerRoutes}); {@link #save()} sends them as one {@code PATCH}
     * (an op/path/value array, content-type {@code application/json}) and returns the refreshed model.
     *
     * <pre>{@code eiaService.update().bandwidth(1000).save();}</pre>
     */
    public class EiaServiceUpdater {

        private final String uuid;
        private final List<PatchOperation> operations = new ArrayList<>();

        protected EiaServiceUpdater(String uuid) {
            this.uuid = uuid;
        }

        /**
         * Replaces the service bandwidth.
         *
         * @param bandwidth the new bandwidth
         * @return this updater
         */
        public EiaServiceUpdater bandwidth(Integer bandwidth) {
            operations.add(PatchOperation.replace("/bandwidth", bandwidth));
            return this;
        }

        /**
         * Replaces the minimum bandwidth commit (for burst-billing variants).
         *
         * @param bandwidthCommit the new bandwidth commit
         * @return this updater
         */
        public EiaServiceUpdater bandwidthCommit(Integer bandwidthCommit) {
            operations.add(PatchOperation.replace("/bandwidthCommit", bandwidthCommit));
            return this;
        }

        /**
         * Replaces the order purchase order number.
         *
         * @param purchaseOrderNumber the new purchase order number
         * @return this updater
         */
        public EiaServiceUpdater purchaseOrderNumber(String purchaseOrderNumber) {
            operations.add(PatchOperation.replace("/order/purchaseOrderNumber", purchaseOrderNumber));
            return this;
        }

        /**
         * Adds a customer route referencing an IP block by UUID.
         *
         * @param ipBlockUuid the UUID of the IP block to advertise
         * @return this updater
         */
        public EiaServiceUpdater addCustomerRoute(String ipBlockUuid) {
            operations.add(PatchOperation.add("/routingProtocol/customerRoutes",
                    new EiaRoutingProtocolRequest.CustomerRoute(new EiaRoutingProtocolRequest.IpBlockRef(ipBlockUuid))));
            return this;
        }

        /**
         * Removes the customer route for the IP block identified by the given UUID.
         *
         * @param ipBlockUuid the UUID of the IP block to stop advertising
         * @return this updater
         */
        public EiaServiceUpdater removeCustomerRoute(String ipBlockUuid) {
            operations.add(PatchOperation.remove("/routingProtocol/customerRoutes[@.ipBlock.uuid=" + ipBlockUuid + "]"));
            return this;
        }

        /**
         * Adds an arbitrary change operation, for paths not covered by the typed setters above.
         *
         * @param operation the patch operation
         * @return this updater
         */
        public EiaServiceUpdater patch(PatchOperation operation) {
            operations.add(operation);
            return this;
        }

        /**
         * Applies the accumulated changes and returns the EIA service refreshed from the server.
         *
         * @return the updated {@link com.eqixiac.equinix.fabric.model.EiaService}
         */
        public EiaService save() {
            if (operations.isEmpty()) {
                throw new IllegalStateException("No changes specified; set at least one field before calling save().");
            }
            EiaServiceJson eiaServiceJson = ((EiaServiceClientImpl) EiaServiceOperator.this.getServiceClient()).update(uuid, operations);
            return new EiaServiceWrapper(eiaServiceJson, EiaServiceOperator.this.getServiceClient());
        }
    }
}
