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

package com.eqixiac.equinix.customerportal.client.internal.implementation;

import com.eqixiac.equinix.core.client.ClientBase;
import com.eqixiac.equinix.core.enums.RequestType;
import com.eqixiac.equinix.customerportal.client.implementation.CustomerPortalConfigImpl;
import com.eqixiac.equinix.customerportal.client.internal.SupportCasesClient;
import com.eqixiac.equinix.customerportal.model.EmailDetails;
import com.eqixiac.equinix.customerportal.model.SupportCase;
import com.eqixiac.equinix.customerportal.model.json.EmailDetailsResponseJson;
import com.eqixiac.equinix.customerportal.model.json.SupportCaseJson;
import com.eqixiac.equinix.customerportal.model.json.TicketResponseJson;
import com.eqixiac.equinix.customerportal.model.json.creators.SupportCaseCancelRequest;
import com.eqixiac.equinix.customerportal.model.json.creators.SupportCaseCreateRequest;
import com.eqixiac.equinix.customerportal.model.json.creators.SupportCaseNoteRequest;

import java.util.Map;

public class SupportCasesClientImpl extends ClientBase implements SupportCasesClient {

    public SupportCasesClientImpl(CustomerPortalConfigImpl configClient) {
        super(configClient, "CustomerPortal", "SupportCases");
    }

    public String create(SupportCaseCreateRequest request) {
        TicketResponseJson response = postAs("CreateSupportCase", request, TicketResponseJson.class);
        return response.getId();
    }

    public SupportCase getByCaseOrOrderNumber(String id) {
        return getAs("GetSupportCase", Map.of("id", id), null, SupportCaseJson.class);
    }

    public Boolean addNotesById(String id, SupportCaseNoteRequest request) {
        return booleanOp("AddSupportCaseNotesById", RequestType.SINGLE, Map.of("id", id), null, request);
    }

    public Boolean cancel(String id, SupportCaseCancelRequest request) {
        return booleanOp("CancelSupportCase", RequestType.SINGLE, Map.of("id", id), null, request);
    }

    public Boolean addNotesByCaseNumber(String caseNumber, SupportCaseNoteRequest request) {
        return booleanOp("AddSupportCaseNotesByCaseNumber", RequestType.SINGLE, Map.of("caseNumber", caseNumber), null, request);
    }

    public byte[] downloadAttachment(String caseId, String attachmentId) {
        return bytesOp("DownloadSupportCaseAttachment", Map.of("caseId", caseId, "attachmentId", attachmentId), null);
    }

    public EmailDetails getEmailDetails(String emailId, String caseNumber) {
        return getAs("GetSupportCaseEmailDetails", Map.of("emailId", emailId, "caseNumber", caseNumber), null,
                EmailDetailsResponseJson.class);
    }
}
