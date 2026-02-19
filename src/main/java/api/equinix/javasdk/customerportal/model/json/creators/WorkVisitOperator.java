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
import api.equinix.javasdk.customerportal.client.internal.implementation.WorkVisitClientImpl;
import api.equinix.javasdk.customerportal.model.WorkVisit;
import api.equinix.javasdk.customerportal.model.json.WorkVisitJson;
import api.equinix.javasdk.customerportal.model.wrappers.WorkVisitWrapper;
import lombok.Getter;

public class WorkVisitOperator extends ResourceImpl<WorkVisit> {

    @Getter
    private final Pageable<WorkVisit> serviceClient;

    public WorkVisitOperator(Pageable<WorkVisit> serviceClient) {
        this.serviceClient = serviceClient;
    }

    public WorkVisitBuilder create() {
        return new WorkVisitBuilder();
    }

    @Getter
    public class WorkVisitBuilder {
        private String ibxCode;
        private String accountNumber;
        private String description;
        private String scheduleStartDate;
        private String scheduleEndDate;
        private String visitorCompany;
        private String visitorName;
        private String visitorEmail;
        private String visitorPhone;

        public WorkVisitBuilder ibxCode(String ibxCode) {
            this.ibxCode = ibxCode;
            return this;
        }

        public WorkVisitBuilder accountNumber(String accountNumber) {
            this.accountNumber = accountNumber;
            return this;
        }

        public WorkVisitBuilder description(String description) {
            this.description = description;
            return this;
        }

        public WorkVisitBuilder scheduleStartDate(String scheduleStartDate) {
            this.scheduleStartDate = scheduleStartDate;
            return this;
        }

        public WorkVisitBuilder scheduleEndDate(String scheduleEndDate) {
            this.scheduleEndDate = scheduleEndDate;
            return this;
        }

        public WorkVisitBuilder visitorCompany(String visitorCompany) {
            this.visitorCompany = visitorCompany;
            return this;
        }

        public WorkVisitBuilder visitorName(String visitorName) {
            this.visitorName = visitorName;
            return this;
        }

        public WorkVisitBuilder visitorEmail(String visitorEmail) {
            this.visitorEmail = visitorEmail;
            return this;
        }

        public WorkVisitBuilder visitorPhone(String visitorPhone) {
            this.visitorPhone = visitorPhone;
            return this;
        }

        public WorkVisit create() {
            WorkVisitCreatorJson creatorJson = new WorkVisitCreatorJson(this);
            WorkVisitJson workVisitJson = ((WorkVisitClientImpl) WorkVisitOperator.this.getServiceClient()).create(creatorJson);
            return new WorkVisitWrapper(workVisitJson, WorkVisitOperator.this.getServiceClient());
        }
    }
}
