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

import api.equinix.javasdk.core.client.PageableBase;
import api.equinix.javasdk.core.enums.RequestType;
import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.request.EquinixRequest;
import api.equinix.javasdk.core.http.request.PaginatedRequest;
import api.equinix.javasdk.core.http.response.EquinixResponse;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.customerportal.client.implementation.CustomerPortalConfigImpl;
import api.equinix.javasdk.customerportal.client.internal.ResellerCustomerClient;
import api.equinix.javasdk.customerportal.model.ResellerCustomer;
import api.equinix.javasdk.customerportal.model.json.ResellerCustomerJson;
import api.equinix.javasdk.customerportal.model.json.creators.ResellerCustomerCreatorJson;
import api.equinix.javasdk.customerportal.model.json.creators.ResellerCustomerUpdaterJson;
import api.equinix.javasdk.customerportal.model.wrappers.ResellerCustomerWrapper;

import java.util.Map;

public class ResellerCustomerClientImpl extends PageableBase implements ResellerCustomerClient<ResellerCustomer> {

    public ResellerCustomerClientImpl(CustomerPortalConfigImpl configClient) {
        super(configClient, "CustomerPortal", "Resellers");
    }

    public Page<ResellerCustomer, ResellerCustomerJson> list(String accountNumber) {
        Map<String, String> pParams = Map.of("accountNumber", accountNumber);
        EquinixRequest<ResellerCustomer> equinixRequest = this.buildRequestWithPathParams("ListResellerCustomers", RequestType.PAGINATED, pParams, ResellerCustomerJson.getPagedTypeRef());
        EquinixResponse<ResellerCustomer> equinixResponse = this.invoke(equinixRequest);
        return Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
    }

    public ResellerCustomerJson getResellerCustomer(String accountNumber, String customerAccountNumber) {
        Map<String, String> pParams = Map.of("accountNumber", accountNumber, "customerAccountNumber", customerAccountNumber);
        EquinixRequest<ResellerCustomer> equinixRequest = this.buildRequestWithPathParams("GetResellerCustomer", RequestType.SINGLE, pParams, ResellerCustomerJson.getSingleTypeRef());
        EquinixResponse<ResellerCustomer> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public Boolean create(String accountNumber, ResellerCustomerCreatorJson resellerCustomerCreatorJson) {
        Map<String, String> pParams = Map.of("accountNumber", accountNumber);

        EquinixRequest<ResellerCustomerJson> equinixRequest = this.buildRequestWithPathParams("CreateResellerCustomer", RequestType.SINGLE, pParams);
        Utils.serializeJson(equinixRequest, resellerCustomerCreatorJson);
        EquinixResponse<ResellerCustomerJson> equinixResponse = this.invoke(equinixRequest);

        return Utils.handleBooleanResponse(equinixResponse, equinixRequest);
    }

    public Boolean update(String accountNumber, String customerAccountNumber, ResellerCustomerUpdaterJson resellerCustomerUpdaterJson) {
        Map<String, String> pParams = Map.of("accountNumber", accountNumber, "customerAccountNumber", customerAccountNumber);

        EquinixRequest<ResellerCustomerJson> equinixRequest = this.buildRequestWithPathParams("UpdateResellerCustomer", RequestType.SINGLE, pParams);
        Utils.serializeJson(equinixRequest, resellerCustomerUpdaterJson);
        EquinixResponse<ResellerCustomerJson> equinixResponse = this.invoke(equinixRequest);

        return Utils.handleBooleanResponse(equinixResponse, equinixRequest);
    }

    public Boolean remove(String accountNumber, String customerAccountNumber, String reason) {
        Map<String, String> pParams = Map.of("accountNumber", accountNumber, "customerAccountNumber", customerAccountNumber);
        Map<String, String> requestBody = Utils.singlePropertyBody("reason", reason);

        EquinixRequest<ResellerCustomerJson> equinixRequest = this.buildRequestWithPathParams("RemoveResellerCustomer", RequestType.SINGLE, pParams);
        Utils.serializeJson(equinixRequest, requestBody);
        EquinixResponse<ResellerCustomerJson> equinixResponse = this.invoke(equinixRequest);

        return Utils.handleBooleanResponse(equinixResponse, equinixRequest);
    }

    public PaginatedList<ResellerCustomer> nextPage(PaginatedRequest<ResellerCustomer> equinixRequest) {
        EquinixResponse<ResellerCustomer> equinixResponse = this.invoke(equinixRequest);
        Page<ResellerCustomer, ResellerCustomerJson> nextPage = Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
        PaginatedList<ResellerCustomer> newPaginatedList = Utils.mapPaginatedList(nextPage.getItems(), this, ResellerCustomerWrapper::new);
        return new PaginatedList<>(newPaginatedList, this, equinixRequest, equinixResponse, nextPage.getPagination());
    }
}
