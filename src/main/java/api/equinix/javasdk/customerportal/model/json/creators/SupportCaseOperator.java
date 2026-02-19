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

package api.equinix.javasdk.customerportal.model.json.creators;

import api.equinix.javasdk.core.http.response.Pageable;
import api.equinix.javasdk.core.model.ResourceImpl;
import api.equinix.javasdk.customerportal.client.internal.implementation.SupportCaseClientImpl;
import api.equinix.javasdk.customerportal.enums.CasePriority;
import api.equinix.javasdk.customerportal.model.SupportCase;
import api.equinix.javasdk.customerportal.model.json.SupportCaseJson;
import api.equinix.javasdk.customerportal.model.wrappers.SupportCaseWrapper;
import lombok.Getter;

public class SupportCaseOperator extends ResourceImpl<SupportCase> {

    @Getter
    private final Pageable<SupportCase> serviceClient;

    public SupportCaseOperator(Pageable<SupportCase> serviceClient) {
        this.serviceClient = serviceClient;
    }

    public SupportCaseBuilder create() {
        return new SupportCaseBuilder();
    }

    @Getter
    public class SupportCaseBuilder {
        private String subject;
        private String description;
        private CasePriority priority;

        public SupportCaseBuilder subject(String subject) {
            this.subject = subject;
            return this;
        }

        public SupportCaseBuilder description(String description) {
            this.description = description;
            return this;
        }

        public SupportCaseBuilder priority(CasePriority priority) {
            this.priority = priority;
            return this;
        }

        public SupportCase create() {
            SupportCaseCreatorJson creatorJson = new SupportCaseCreatorJson(this);
            SupportCaseJson supportCaseJson = ((SupportCaseClientImpl) SupportCaseOperator.this.getServiceClient()).create(creatorJson);
            return new SupportCaseWrapper(supportCaseJson, SupportCaseOperator.this.getServiceClient());
        }
    }
}
