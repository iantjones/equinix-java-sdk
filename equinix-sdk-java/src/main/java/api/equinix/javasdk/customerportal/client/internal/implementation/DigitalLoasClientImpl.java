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
import api.equinix.javasdk.customerportal.client.internal.DigitalLoasClient;
import api.equinix.javasdk.customerportal.model.BetaTermsAgreement;
import api.equinix.javasdk.customerportal.model.DigitalLoa;
import api.equinix.javasdk.customerportal.model.DigitalLoaChange;
import api.equinix.javasdk.customerportal.model.LoaCustomerOrganization;
import api.equinix.javasdk.customerportal.model.PrivateBetaPermission;
import api.equinix.javasdk.customerportal.model.json.BetaTermsAgreementJson;
import api.equinix.javasdk.customerportal.model.json.DigitalLoaChangeJson;
import api.equinix.javasdk.customerportal.model.json.DigitalLoaJson;
import api.equinix.javasdk.customerportal.model.json.DigitalLoaSearchResponseJson;
import api.equinix.javasdk.customerportal.model.json.LoaCustomerOrganizationJson;
import api.equinix.javasdk.customerportal.model.json.PrivateBetaPermissionJson;
import api.equinix.javasdk.customerportal.model.json.creators.DigitalLoaCreateRequest;
import api.equinix.javasdk.customerportal.model.json.creators.DigitalLoaSearchRequest;
import api.equinix.javasdk.customerportal.model.json.creators.PrivateBetaAccessRequest;

import java.util.List;
import java.util.Map;

public class DigitalLoasClientImpl extends ClientBase implements DigitalLoasClient {

    public DigitalLoasClientImpl(CustomerPortalConfigImpl configClient) {
        super(configClient, "CustomerPortal", "DigitalLoas");
    }

    public DigitalLoa create(DigitalLoaCreateRequest request) {
        return postAs("CreateDigitalLoa", request, DigitalLoaJson.class);
    }

    public DigitalLoa findByUuid(String uuid) {
        return getAs("GetDigitalLoa", Map.of("uuid", uuid), null, DigitalLoaJson.class);
    }

    public List<? extends DigitalLoa> search(DigitalLoaSearchRequest request) {
        DigitalLoaSearchResponseJson response = postAs("SearchDigitalLoas", request, DigitalLoaSearchResponseJson.class);
        return response.getData();
    }

    public DigitalLoa patch(String uuid, List<Map<String, Object>> operations) {
        EquinixRequest<DigitalLoaJson> request =
                buildRequestWithPathParams("PatchDigitalLoa", RequestType.SINGLE, Map.of("uuid", uuid), DigitalLoaJson.class);
        Utils.serializeJson(request, operations);
        return Utils.handleSingletonResponse(invoke(request), request);
    }

    public Boolean cancel(String uuid) {
        return booleanOp("CancelDigitalLoa", RequestType.SINGLE, Map.of("uuid", uuid), null, null);
    }

    public DigitalLoa performAction(String uuid, Map<String, Object> action) {
        EquinixRequest<DigitalLoaJson> request =
                buildRequestWithPathParams("PerformDigitalLoaAction", RequestType.SINGLE, Map.of("uuid", uuid), DigitalLoaJson.class);
        Utils.serializeJson(request, action);
        return Utils.handleSingletonResponse(invoke(request), request);
    }

    public Boolean createRequest(Map<String, Object> request) {
        return booleanOp("CreateDigitalLoaRequest", RequestType.SINGLE, null, null, request);
    }

    public List<? extends DigitalLoaChange> findChangesByLoaUuid(String uuid) {
        return listAs("GetDigitalLoaChanges", Map.of("uuid", uuid), null, DigitalLoaChangeJson.class);
    }

    public DigitalLoaChange findChangeByUuid(String uuid, String changeUuid) {
        return getAs("GetDigitalLoaChange", Map.of("uuid", uuid, "changeUuid", changeUuid), null, DigitalLoaChangeJson.class);
    }

    public List<? extends LoaCustomerOrganization> findOrganizations() {
        return listAs("FindDigitalLoaOrganizations", null, null, LoaCustomerOrganizationJson.class);
    }

    public PrivateBetaPermission isPrivateBetaAllowed() {
        return getAs("GetDigitalLoaPrivateBetaAccess", null, null, PrivateBetaPermissionJson.class);
    }

    public Boolean createPrivateBetaAccessRequest(PrivateBetaAccessRequest request) {
        return booleanOp("CreateDigitalLoaPrivateBetaAccess", RequestType.SINGLE, null, null, request);
    }

    public BetaTermsAgreement getBetaTermsAgreement() {
        return getAs("GetDigitalLoaBetaTermsAgreement", null, null, BetaTermsAgreementJson.class);
    }

    public BetaTermsAgreement updateBetaTermsAgreement(Boolean agreementAccepted) {
        return postAs("UpdateDigitalLoaBetaTermsAgreement", Map.of("agreementAccepted", agreementAccepted), BetaTermsAgreementJson.class);
    }
}
