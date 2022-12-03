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

import api.equinix.javasdk.core.enums.PortType;
import api.equinix.javasdk.core.http.response.PageablePost;
import api.equinix.javasdk.core.model.ResourcePostImpl;
import api.equinix.javasdk.fabric.client.internal.implementation.ServiceProfileClientImpl;
import api.equinix.javasdk.fabric.enums.NotificationType;
import api.equinix.javasdk.fabric.enums.ServiceProfileType;
import api.equinix.javasdk.fabric.enums.ServiceProfileVisibility;
import api.equinix.javasdk.fabric.model.ServiceProfile;
import api.equinix.javasdk.fabric.model.implementation.*;
import api.equinix.javasdk.fabric.model.json.ServiceProfileJson;
import api.equinix.javasdk.fabric.model.wrappers.ServiceProfileWrapper;
import lombok.AccessLevel;
import lombok.Getter;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ServiceProfileOperator extends ResourcePostImpl<ServiceProfile> {

    @Getter
    private final PageablePost<ServiceProfile> serviceClient;

    public ServiceProfileOperator(PageablePost<ServiceProfile> serviceClient) {
        this.serviceClient = serviceClient;
    }

    public ServiceProfileBuilder create(ServiceProfileType type) {
        return new ServiceProfileBuilder(type);
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
                this.notifications = Collections.singletonList(new Notification(type, Collections.singletonList(emailAddress)));
            }
            else {
                if (notifications.stream().noneMatch(o -> o.getType().equals(type))) {
                    notifications.add(new Notification(type, Collections.singletonList(emailAddress)));
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
                this.allowedEmails = Collections.singletonList(emailAddress);
            }
            else {
                this.allowedEmails.add(emailAddress);
            }

            return this;
        }

        public ServiceProfileBuilder tag(String tag) {
            if(this.tags == null) {
                this.tags = Collections.singletonList(tag);
            }
            else {
                this.tags.add(tag);
            }

            return this;
        }

        public ServiceProfileBuilder port(String portUuid, PortType portType) {
            if(this.ports == null) {
                this.ports = Collections.singletonList(new AccessPointTypeConfigPort(portUuid, portType));
            }
            else {
                this.ports.add(new AccessPointTypeConfigPort(portUuid, portType));
            }

            return this;
        }

        public ServiceProfileBuilder accessPointTypeConfig(AccessPointTypeConfig accessPointTypeConfig) {
            if(this.accessPointTypeConfigs == null) {
                this.accessPointTypeConfigs = Collections.singletonList(accessPointTypeConfig);
            }
            else {
                this.accessPointTypeConfigs.add(accessPointTypeConfig);
            }

            return this;
        }

        public ServiceProfileBuilder customField(CustomField customField) {
            if(this.customFields == null) {
                this.customFields = Collections.singletonList(customField);
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

        public ServiceProfile create() {
            ServiceProfileCreatorJson serviceProfileCreatorJson = new ServiceProfileCreatorJson(this);
            ServiceProfileJson serviceProfileJson = ((ServiceProfileClientImpl) ServiceProfileOperator.this.getServiceClient()).create(serviceProfileCreatorJson);
            return new ServiceProfileWrapper(serviceProfileJson, ServiceProfileOperator.this.getServiceClient());
        }
    }
}
