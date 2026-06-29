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
import api.equinix.javasdk.core.http.response.Pageable;
import api.equinix.javasdk.core.model.ResourceImpl;
import api.equinix.javasdk.fabric.client.internal.implementation.PrecisionTimeClientImpl;
import api.equinix.javasdk.fabric.enums.PrecisionTimePackageCode;
import api.equinix.javasdk.fabric.enums.PrecisionTimeType;
import api.equinix.javasdk.fabric.model.PrecisionTime;
import api.equinix.javasdk.fabric.model.Project;
import api.equinix.javasdk.fabric.model.json.PrecisionTimeJson;
import api.equinix.javasdk.fabric.model.wrappers.PrecisionTimeWrapper;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author ianjones
 */
public class PrecisionTimeOperator extends ResourceImpl<PrecisionTime> {

    @Getter
    private final Pageable<PrecisionTime> serviceClient;

    public PrecisionTimeOperator(Pageable<PrecisionTime> serviceClient) {
        this.serviceClient = serviceClient;
    }

    public PrecisionTimeBuilder create() {
        return new PrecisionTimeBuilder();
    }

    /**
     * <p>Begins a fluent JSON Patch update of an existing precision time service, identified by uuid.</p>
     *
     * @param uuid the uuid of the time service to update
     */
    public PrecisionTimeUpdater update(String uuid) {
        return new PrecisionTimeUpdater(uuid);
    }

    @Getter
    public class PrecisionTimeBuilder {

        private PrecisionTimeType type;
        private String name;
        private String description;
        private PrecisionTimePackageCode packageCode;
        private Project project;

        protected PrecisionTimeBuilder() {
        }

        public PrecisionTimeOperator.PrecisionTimeBuilder withType(PrecisionTimeType type) {
            this.type = type;
            return this;
        }

        public PrecisionTimeOperator.PrecisionTimeBuilder withName(String name) {
            this.name = name;
            return this;
        }

        public PrecisionTimeOperator.PrecisionTimeBuilder withDescription(String description) {
            this.description = description;
            return this;
        }

        public PrecisionTimeOperator.PrecisionTimeBuilder withPackageCode(PrecisionTimePackageCode packageCode) {
            this.packageCode = packageCode;
            return this;
        }

        public PrecisionTimeOperator.PrecisionTimeBuilder withProject(Project project) {
            this.project = project;
            return this;
        }

        public PrecisionTime create() {
            PrecisionTimeCreatorJson precisionTimeCreatorJson = new PrecisionTimeCreatorJson(this);
            PrecisionTimeJson precisionTimeJson = ((PrecisionTimeClientImpl) PrecisionTimeOperator.this.getServiceClient()).create(precisionTimeCreatorJson);
            return new PrecisionTimeWrapper(precisionTimeJson, PrecisionTimeOperator.this.getServiceClient());
        }
    }

    /**
     * Fluent builder for updating an existing precision time service via RFC&nbsp;6902 JSON Patch.
     * Each typed setter records a {@code replace} operation; {@link #save()} sends them as one
     * {@code PATCH} with content-type {@code application/json-patch+json} and returns the refreshed model.
     *
     * <pre>{@code timeService.update().name("New-Name").save();}</pre>
     */
    public class PrecisionTimeUpdater {

        private final String uuid;
        private final List<PatchOperation> operations = new ArrayList<>();

        protected PrecisionTimeUpdater(String uuid) {
            this.uuid = uuid;
        }

        /**
         * Replaces the time service name.
         *
         * @param name the new name
         * @return this updater
         */
        public PrecisionTimeUpdater name(String name) {
            operations.add(PatchOperation.replace("/name", name));
            return this;
        }

        /**
         * Replaces the time service package (tier).
         *
         * @param packageCode the new package code
         * @return this updater
         */
        public PrecisionTimeUpdater changePackage(PrecisionTimePackageCode packageCode) {
            operations.add(PatchOperation.replace("/package/code", packageCode));
            return this;
        }

        /**
         * Adds an arbitrary JSON Patch operation, for paths not covered by the typed setters above
         * (e.g. {@code /ipv4} or {@code /ntpAdvancedConfiguration}).
         *
         * @param operation the patch operation
         * @return this updater
         */
        public PrecisionTimeUpdater patch(PatchOperation operation) {
            operations.add(operation);
            return this;
        }

        /**
         * Applies the accumulated changes and returns the time service refreshed from the server.
         *
         * @return the updated {@link api.equinix.javasdk.fabric.model.PrecisionTime}
         */
        public PrecisionTime save() {
            if (operations.isEmpty()) {
                throw new IllegalStateException("No changes specified; set at least one field before calling save().");
            }
            PrecisionTimeJson precisionTimeJson = ((PrecisionTimeClientImpl) PrecisionTimeOperator.this.getServiceClient()).update(uuid, operations);
            return new PrecisionTimeWrapper(precisionTimeJson, PrecisionTimeOperator.this.getServiceClient());
        }
    }
}
