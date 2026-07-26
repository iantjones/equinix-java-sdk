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

package com.eqixiac.equinix.customerportal.client.implementation;

import com.eqixiac.equinix.CustomerPortal;
import com.eqixiac.equinix.customerportal.client.SupportCases;
import com.eqixiac.equinix.customerportal.client.internal.SupportCasesClient;
import com.eqixiac.equinix.customerportal.model.EmailDetails;
import com.eqixiac.equinix.customerportal.model.SupportCase;
import com.eqixiac.equinix.customerportal.model.json.creators.SupportCaseCancelRequest;
import com.eqixiac.equinix.customerportal.model.json.creators.SupportCaseCreateRequest;
import com.eqixiac.equinix.customerportal.model.json.creators.SupportCaseNoteRequest;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SupportCasesImpl implements SupportCases {

    private final SupportCasesClient serviceClient;

    private final CustomerPortal serviceManager;

    public String create(SupportCaseCreateRequest request) {
        return this.serviceClient.create(request);
    }

    public SupportCase getByCaseOrOrderNumber(String id) {
        return this.serviceClient.getByCaseOrOrderNumber(id);
    }

    public Boolean addNotesById(String id, SupportCaseNoteRequest request) {
        return this.serviceClient.addNotesById(id, request);
    }

    public Boolean cancel(String id, SupportCaseCancelRequest request) {
        return this.serviceClient.cancel(id, request);
    }

    public Boolean addNotesByCaseNumber(String caseNumber, SupportCaseNoteRequest request) {
        return this.serviceClient.addNotesByCaseNumber(caseNumber, request);
    }

    public byte[] downloadAttachment(String caseId, String attachmentId) {
        return this.serviceClient.downloadAttachment(caseId, attachmentId);
    }

    public EmailDetails getEmailDetails(String emailId, String caseNumber) {
        return this.serviceClient.getEmailDetails(emailId, caseNumber);
    }
}
