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

import api.equinix.javasdk.core.http.response.PageablePost;
import api.equinix.javasdk.core.model.ResourceImpl;
import api.equinix.javasdk.fabric.client.internal.implementation.CompanyProfileClientImpl;
import api.equinix.javasdk.fabric.model.CompanyProfile;
import api.equinix.javasdk.fabric.model.implementation.Notification;
import api.equinix.javasdk.fabric.model.json.CompanyProfileJson;
import api.equinix.javasdk.fabric.model.wrappers.CompanyProfileWrapper;
import lombok.AccessLevel;
import lombok.Getter;

import java.util.List;

/**
 * Fluent builder for Fabric company profiles.
 *
 * @author ianjones
 */
public class CompanyProfileOperator extends ResourceImpl<CompanyProfile> {

    @Getter
    private final PageablePost<CompanyProfile> serviceClient;

    public CompanyProfileOperator(PageablePost<CompanyProfile> serviceClient) {
        this.serviceClient = serviceClient;
    }

    public CompanyProfileBuilder create(String type) {
        return new CompanyProfileBuilder(type);
    }

    @Getter(AccessLevel.PACKAGE)
    public class CompanyProfileBuilder {

        private final String type;
        private String name;
        private String summary;
        private String description;
        private String webUrl;
        private String contactUrl;
        private List<Notification> notifications;

        protected CompanyProfileBuilder(String type) {
            this.type = type;
        }

        public CompanyProfileBuilder name(String name) {
            this.name = name;
            return this;
        }

        public CompanyProfileBuilder summary(String summary) {
            this.summary = summary;
            return this;
        }

        public CompanyProfileBuilder description(String description) {
            this.description = description;
            return this;
        }

        public CompanyProfileBuilder webUrl(String webUrl) {
            this.webUrl = webUrl;
            return this;
        }

        public CompanyProfileBuilder contactUrl(String contactUrl) {
            this.contactUrl = contactUrl;
            return this;
        }

        /**
         * Sets the contact notifications for the profile, e.g. a {@code CONTACT} entry and a
         * {@code NOTIFICATION} entry each carrying the relevant email addresses.
         *
         * @param notifications the notification entries
         * @return this builder
         */
        public CompanyProfileBuilder notifications(List<Notification> notifications) {
            this.notifications = notifications;
            return this;
        }

        public CompanyProfile create() {
            CompanyProfileCreatorJson creatorJson = new CompanyProfileCreatorJson(this);
            CompanyProfileJson json = ((CompanyProfileClientImpl) CompanyProfileOperator.this.getServiceClient()).create(creatorJson);
            return new CompanyProfileWrapper(json, CompanyProfileOperator.this.getServiceClient());
        }
    }
}
