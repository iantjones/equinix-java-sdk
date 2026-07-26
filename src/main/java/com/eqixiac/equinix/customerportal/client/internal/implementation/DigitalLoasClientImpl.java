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
import com.eqixiac.equinix.core.http.ResponseHandler;
import com.eqixiac.equinix.core.http.SerializationHelper;
import com.eqixiac.equinix.core.http.request.EquinixRequest;
import com.eqixiac.equinix.customerportal.client.implementation.CustomerPortalConfigImpl;
import com.eqixiac.equinix.customerportal.client.internal.DigitalLoasClient;
import com.eqixiac.equinix.customerportal.model.BetaTermsAgreement;
import com.eqixiac.equinix.customerportal.model.DigitalLoa;
import com.eqixiac.equinix.customerportal.model.DigitalLoaChange;
import com.eqixiac.equinix.customerportal.model.LoaCustomerOrganization;
import com.eqixiac.equinix.customerportal.model.PrivateBetaPermission;
import com.eqixiac.equinix.customerportal.model.json.BetaTermsAgreementJson;
import com.eqixiac.equinix.customerportal.model.json.DigitalLoaChangeJson;
import com.eqixiac.equinix.customerportal.model.json.DigitalLoaJson;
import com.eqixiac.equinix.customerportal.model.json.DigitalLoaSearchResponseJson;
import com.eqixiac.equinix.customerportal.model.json.LoaCustomerOrganizationJson;
import com.eqixiac.equinix.customerportal.model.json.PrivateBetaPermissionJson;
import com.eqixiac.equinix.customerportal.model.json.creators.DigitalLoaCreateRequest;
import com.eqixiac.equinix.customerportal.model.json.creators.DigitalLoaSearchRequest;
import com.eqixiac.equinix.customerportal.model.json.creators.PrivateBetaAccessRequest;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.HashMap;
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

    public List<? extends DigitalLoa> search(DigitalLoaSearchRequest request, Integer offset, Integer limit, List<String> sort) {
        Map<String, List<String>> queryParams = new HashMap<>();
        if (offset != null) {
            queryParams.put("offset", List.of(String.valueOf(offset)));
        }
        if (limit != null) {
            queryParams.put("limit", List.of(String.valueOf(limit)));
        }
        if (sort != null && !sort.isEmpty()) {
            queryParams.put("sort", sort);
        }
        DigitalLoaSearchResponseJson response = postForType("SearchDigitalLoas", null,
                queryParams.isEmpty() ? null : queryParams, request,
                new TypeReference<DigitalLoaSearchResponseJson>() {});
        return response.getData();
    }

    public DigitalLoa update(String uuid, List<Map<String, Object>> operations) {
        EquinixRequest<DigitalLoaJson> request =
                buildRequestWithPathParams("PatchDigitalLoa", RequestType.SINGLE, Map.of("uuid", uuid), DigitalLoaJson.class);
        SerializationHelper.serializeJson(request, operations);
        return ResponseHandler.handleSingletonResponse(invoke(request), request);
    }

    public Boolean cancel(String uuid) {
        return booleanOp("CancelDigitalLoa", RequestType.SINGLE, Map.of("uuid", uuid), null, null);
    }

    public DigitalLoa performAction(String uuid, Map<String, Object> action) {
        EquinixRequest<DigitalLoaJson> request =
                buildRequestWithPathParams("PerformDigitalLoaAction", RequestType.SINGLE, Map.of("uuid", uuid), DigitalLoaJson.class);
        SerializationHelper.serializeJson(request, action);
        return ResponseHandler.handleSingletonResponse(invoke(request), request);
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

    public List<? extends LoaCustomerOrganization> listOrganizations(String ibx, List<String> productTypes) {
        Map<String, List<String>> queryParams = new HashMap<>();
        queryParams.put("location.ibx", List.of(ibx));
        if (productTypes != null && !productTypes.isEmpty()) {
            queryParams.put("product.type", productTypes);
        }
        return listAs("FindDigitalLoaOrganizations", null, queryParams, LoaCustomerOrganizationJson.class);
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
