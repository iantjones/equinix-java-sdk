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
import api.equinix.javasdk.fabric.client.internal.implementation.IpBlockClientImpl;
import api.equinix.javasdk.fabric.enums.IpBlockProductType;
import api.equinix.javasdk.fabric.model.IpBlock;
import api.equinix.javasdk.fabric.model.Project;
import api.equinix.javasdk.fabric.model.implementation.IpBlockAccount;
import api.equinix.javasdk.fabric.model.implementation.IpBlockLocation;
import api.equinix.javasdk.fabric.model.implementation.IpBlockOrder;
import api.equinix.javasdk.fabric.model.implementation.IpBlockRegulations;
import api.equinix.javasdk.fabric.model.json.IpBlockJson;
import api.equinix.javasdk.fabric.model.wrappers.IpBlockWrapper;
import lombok.AccessLevel;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>IpBlockOperator class.</p>
 *
 * @author ianjones
 */
public class IpBlockOperator extends ResourceImpl<IpBlock> {

    @Getter
    private final PageablePost<IpBlock> serviceClient;

    public IpBlockOperator(PageablePost<IpBlock> serviceClient) {
        this.serviceClient = serviceClient;
    }

    public IpBlockBuilder create() {
        return new IpBlockBuilder();
    }

    public IpBlockUpdater update(String uuid) {
        return new IpBlockUpdater(uuid);
    }

    @Getter(AccessLevel.PACKAGE)
    public class IpBlockBuilder {

        private IpBlockProductType type;
        private Project project;
        private IpBlockLocation location;
        private IpBlockAccount account;
        private IpBlockOrder order;
        private IpBlockRegulations regulations;
        private Integer prefixLength;
        private String prefix;

        protected IpBlockBuilder() {
        }

        public IpBlockBuilder ofType(IpBlockProductType type) {
            this.type = type;
            return this;
        }

        public IpBlockBuilder withProject(Project project) {
            this.project = project;
            return this;
        }

        public IpBlockBuilder inMetro(String metroCode) {
            this.location = new IpBlockLocation(metroCode);
            return this;
        }

        public IpBlockBuilder withLocation(IpBlockLocation location) {
            this.location = location;
            return this;
        }

        public IpBlockBuilder withAccount(IpBlockAccount account) {
            this.account = account;
            return this;
        }

        public IpBlockBuilder withOrder(IpBlockOrder order) {
            this.order = order;
            return this;
        }

        public IpBlockBuilder withRegulations(IpBlockRegulations regulations) {
            this.regulations = regulations;
            return this;
        }

        public IpBlockBuilder prefixLength(Integer prefixLength) {
            this.prefixLength = prefixLength;
            return this;
        }

        public IpBlockBuilder prefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        public IpBlock create() {
            IpBlockCreatorJson creatorJson = new IpBlockCreatorJson(this);
            IpBlockJson ipBlockJson = ((IpBlockClientImpl) IpBlockOperator.this.getServiceClient()).create(creatorJson);
            return new IpBlockWrapper(ipBlockJson, IpBlockOperator.this.getServiceClient());
        }
    }

    /**
     * Fluent builder for PATCH-updating an existing IP block.
     */
    public class IpBlockUpdater {

        private final String uuid;
        private final List<PatchOperation> operations = new ArrayList<>();

        protected IpBlockUpdater(String uuid) {
            this.uuid = uuid;
        }

        public IpBlockUpdater patch(PatchOperation operation) {
            operations.add(operation);
            return this;
        }

        public IpBlock save() {
            if (operations.isEmpty()) {
                throw new IllegalStateException("No changes specified; set at least one field before calling save().");
            }
            IpBlockJson ipBlockJson = ((IpBlockClientImpl) IpBlockOperator.this.getServiceClient()).update(uuid, operations);
            return new IpBlockWrapper(ipBlockJson, IpBlockOperator.this.getServiceClient());
        }
    }
}
