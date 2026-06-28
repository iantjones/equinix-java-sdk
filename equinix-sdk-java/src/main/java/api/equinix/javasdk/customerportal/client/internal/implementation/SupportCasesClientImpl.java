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

package api.equinix.javasdk.customerportal.client.internal.implementation;

import api.equinix.javasdk.core.client.ClientBase;
import api.equinix.javasdk.core.enums.RequestType;
import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.request.EquinixRequest;
import api.equinix.javasdk.customerportal.client.implementation.CustomerPortalConfigImpl;
import api.equinix.javasdk.customerportal.client.internal.SupportCasesClient;
import api.equinix.javasdk.customerportal.model.SupportCase;
import api.equinix.javasdk.customerportal.model.json.SupportCaseJson;
import api.equinix.javasdk.customerportal.model.json.creators.SupportCaseCancelRequest;
import api.equinix.javasdk.customerportal.model.json.creators.SupportCaseCreateRequest;
import api.equinix.javasdk.customerportal.model.json.creators.SupportCaseNoteRequest;

import java.util.Map;

public class SupportCasesClientImpl extends ClientBase implements SupportCasesClient {

    public SupportCasesClientImpl(CustomerPortalConfigImpl configClient) {
        super(configClient, "CustomerPortal", "SupportCases");
    }

    public String create(SupportCaseCreateRequest request) {
        EquinixRequest<Object> equinixRequest = buildRequest("CreateSupportCase", RequestType.SINGLE, Object.class);
        Utils.serializeJson(equinixRequest, request);
        return Utils.extractFromHeader(invoke(equinixRequest), "Location", OrderLocation.LAST_SEGMENT_PATTERN);
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
}
