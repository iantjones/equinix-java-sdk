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

import com.eqixiac.equinix.fabric.enums.PortType;
import com.eqixiac.equinix.core.http.request.PatchOperation;
import com.eqixiac.equinix.core.http.response.PageablePost;
import com.eqixiac.equinix.core.model.ResourceImpl;
import com.eqixiac.equinix.fabric.client.internal.implementation.ServiceProfileClientImpl;
import com.eqixiac.equinix.fabric.enums.NotificationType;
import com.eqixiac.equinix.fabric.enums.ServiceProfileType;
import com.eqixiac.equinix.fabric.enums.ServiceProfileVisibility;
import com.eqixiac.equinix.fabric.model.Port;
import com.eqixiac.equinix.fabric.model.Project;
import com.eqixiac.equinix.fabric.model.ServiceProfile;
import com.eqixiac.equinix.fabric.model.implementation.*;
import com.eqixiac.equinix.fabric.model.json.ServiceProfileJson;
import com.eqixiac.equinix.fabric.model.wrappers.ServiceProfileWrapper;
import lombok.AccessLevel;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ServiceProfileOperator extends ResourceImpl<ServiceProfile> {

    @Getter
    private final PageablePost<ServiceProfile> serviceClient;

    public ServiceProfileOperator(PageablePost<ServiceProfile> serviceClient) {
        this.serviceClient = serviceClient;
    }

    public ServiceProfileBuilder create(ServiceProfileType type) {
        return new ServiceProfileBuilder(type);
    }

    /**
     * Begins a fluent PATCH update of an existing service profile, identified by uuid.
     *
     * @param uuid the uuid of the service profile to update
     * @return a {@link ServiceProfileUpdater}
     */
    public ServiceProfileUpdater update(String uuid) {
        return new ServiceProfileUpdater(uuid);
    }

    @Getter(AccessLevel.PACKAGE)
    public class ServiceProfileBuilder {

        private final ServiceProfileType type;
        private String name;
        private String description;
        private List<Notification> notifications;
        private ServiceProfileVisibility visibility;
        private List<String> allowedEmails;
        private List<String> tags;
        private List<AccessPointTypeConfigPort> ports;
        private List<AccessPointTypeConfig> accessPointTypeConfigs;
        private List<CustomField> customFields;
        private MarketingInfo marketingInfo;
        private Project project;

        protected ServiceProfileBuilder(ServiceProfileType type) {
            this.type = type;
        }

        public ServiceProfileBuilder name(String name) {
            this.name = name;
            return this;
        }

        public ServiceProfileBuilder description(String description) {
            this.description = description;
            return this;
        }

        public ServiceProfileBuilder notification(NotificationType type, String emailAddress) {
            if(this.notifications == null) {
                this.notifications = new ArrayList<>(List.of(new Notification(type, new ArrayList<>(List.of(emailAddress)))));
            }
            else {
                if (notifications.stream().noneMatch(o -> o.getType().equals(type))) {
                    notifications.add(new Notification(type, new ArrayList<>(List.of(emailAddress))));
                } else {
                    Objects.requireNonNull(notifications.stream().filter(o -> o.getType().equals(type)).findFirst().orElse(null)).addEmail(emailAddress);
                }
            }
            return this;
        }

         public ServiceProfileBuilder notification(List<Notification> notifications) {
            this.notifications = notifications;
            return this;
        }

        public ServiceProfileBuilder visibility(ServiceProfileVisibility visibility) {
            this.visibility = visibility;
            return this;
        }

        public ServiceProfileBuilder allowedEmail(String emailAddress) {
            if(this.allowedEmails == null) {
                this.allowedEmails = new ArrayList<>(List.of(emailAddress));
            }
            else {
                this.allowedEmails.add(emailAddress);
            }

            return this;
        }

        public ServiceProfileBuilder tag(String tag) {
            if(this.tags == null) {
                this.tags = new ArrayList<>(List.of(tag));
            }
            else {
                this.tags.add(tag);
            }

            return this;
        }

        public ServiceProfileBuilder port(String portUuid, PortType portType) {
            if(this.ports == null) {
                this.ports = new ArrayList<>(List.of(new AccessPointTypeConfigPort(portUuid, portType)));
            }
            else {
                this.ports.add(new AccessPointTypeConfigPort(portUuid, portType));
            }

            return this;
        }

        public ServiceProfileBuilder port(Port port, PortType portType) {
            return port(port.getUuid(), portType);
        }

        public ServiceProfileBuilder accessPointTypeConfig(AccessPointTypeConfig accessPointTypeConfig) {
            if(this.accessPointTypeConfigs == null) {
                this.accessPointTypeConfigs = new ArrayList<>(List.of(accessPointTypeConfig));
            }
            else {
                this.accessPointTypeConfigs.add(accessPointTypeConfig);
            }

            return this;
        }

        public ServiceProfileBuilder customField(CustomField customField) {
            if(this.customFields == null) {
                this.customFields = new ArrayList<>(List.of(customField));
            }
            else {
                this.customFields.add(customField);
            }

            return this;
        }

        public ServiceProfileBuilder marketingInfo(MarketingInfo marketingInfo) {
            this.marketingInfo = marketingInfo;
            return this;
        }

        /**
         * Sets the project this service profile is created in (the spec
         * {@code ServiceProfileRequest.project} attribute).
         *
         * @param project the project reference
         * @return this builder
         */
        public ServiceProfileBuilder project(Project project) {
            this.project = project;
            return this;
        }

        /**
         * Sets the project this service profile is created in, by project id.
         *
         * @param projectId the project identifier
         * @return this builder
         */
        public ServiceProfileBuilder project(String projectId) {
            this.project = new Project(projectId);
            return this;
        }

        public ServiceProfile create() {
            ServiceProfileCreatorJson serviceProfileCreatorJson = new ServiceProfileCreatorJson(this);
            ServiceProfileJson serviceProfileJson = ((ServiceProfileClientImpl) ServiceProfileOperator.this.getServiceClient()).create(serviceProfileCreatorJson);
            return new ServiceProfileWrapper(serviceProfileJson, ServiceProfileOperator.this.getServiceClient());
        }

        /**
         * Fully replaces (PUT) the service profile identified by {@code uuid} with the attributes
         * configured on this builder, and returns the refreshed model.
         *
         * @param uuid the uuid of the service profile to replace
         * @return the updated {@link ServiceProfile}
         */
        public ServiceProfile replace(String uuid) {
            ServiceProfileCreatorJson serviceProfileCreatorJson = new ServiceProfileCreatorJson(this);
            ServiceProfileJson serviceProfileJson = ((ServiceProfileClientImpl) ServiceProfileOperator.this.getServiceClient()).put(uuid, serviceProfileCreatorJson);
            return new ServiceProfileWrapper(serviceProfileJson, ServiceProfileOperator.this.getServiceClient());
        }
    }

    /**
     * Fluent builder for PATCH-updating an existing service profile. Each typed setter records a
     * {@code replace} change operation; {@link #save()} sends them as one {@code PATCH} and returns
     * the refreshed model.
     */
    public class ServiceProfileUpdater {

        private final String uuid;
        private final List<PatchOperation> operations = new ArrayList<>();

        protected ServiceProfileUpdater(String uuid) {
            this.uuid = uuid;
        }

        public ServiceProfileUpdater name(String name) {
            operations.add(PatchOperation.replace("/name", name));
            return this;
        }

        public ServiceProfileUpdater description(String description) {
            operations.add(PatchOperation.replace("/description", description));
            return this;
        }

        public ServiceProfileUpdater patch(PatchOperation operation) {
            operations.add(operation);
            return this;
        }

        public ServiceProfile save() {
            if (operations.isEmpty()) {
                throw new IllegalStateException("No changes specified; set at least one field before calling save().");
            }
            ServiceProfileJson serviceProfileJson = ((ServiceProfileClientImpl) ServiceProfileOperator.this.getServiceClient()).update(uuid, operations);
            return new ServiceProfileWrapper(serviceProfileJson, ServiceProfileOperator.this.getServiceClient());
        }
    }
}
