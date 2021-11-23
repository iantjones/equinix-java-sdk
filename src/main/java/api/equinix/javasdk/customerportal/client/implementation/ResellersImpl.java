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

package api.equinix.javasdk.customerportal.client.implementation;

import api.equinix.javasdk.CustomerPortal;
import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.customerportal.client.Resellers;
import api.equinix.javasdk.customerportal.client.internal.ResellerClient;
import api.equinix.javasdk.customerportal.client.internal.ResellerCustomerClient;
import api.equinix.javasdk.customerportal.model.Reseller;
import api.equinix.javasdk.customerportal.model.ResellerCustomer;
import api.equinix.javasdk.customerportal.model.json.ResellerCustomerJson;
import api.equinix.javasdk.customerportal.model.json.ResellerJson;
import api.equinix.javasdk.customerportal.model.json.creators.ResellerCustomerOperator;
import api.equinix.javasdk.customerportal.model.wrappers.ResellerCustomerWrapper;
import api.equinix.javasdk.customerportal.model.wrappers.ResellerWrapper;

public class ResellersImpl implements Resellers {

    private final CustomerPortal serviceManager;

    private final ResellerClient<Reseller> serviceClient;

    private final ResellerCustomerClient<ResellerCustomer> serviceClientCustomer;

    public ResellersImpl(ResellerClient<Reseller> serviceClient, ResellerCustomerClient<ResellerCustomer> serviceClientCustomer,
                         CustomerPortal serviceManager) {
        this.serviceClient = serviceClient;
        this.serviceClientCustomer = serviceClientCustomer;
        this.serviceManager = serviceManager;
    }

    public PaginatedList<Reseller> list() {
        Page<Reseller, ResellerJson> responsePage = this.serviceClient.list();
        PaginatedList<Reseller> serviceTokenList = Utils.mapPaginatedList(responsePage.getItems(), this.serviceClient, ResellerWrapper::new);
        return new PaginatedList<>(serviceTokenList, this.serviceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    public PaginatedList<ResellerCustomer> listCustomers(String accountNumber) {
        Page<ResellerCustomer, ResellerCustomerJson> responsePage = this.serviceClientCustomer.list(accountNumber);
        PaginatedList<ResellerCustomer> resellerCustomerList = Utils.mapPaginatedList(responsePage.getItems(), this.serviceClientCustomer, ResellerCustomerWrapper::new);
        return new PaginatedList<>(resellerCustomerList, this.serviceClientCustomer, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    public ResellerCustomer getResellerCustomer(String accountNumber, String customerAccountNumber) {
        ResellerCustomerJson resellerCustomerJson = this.serviceClientCustomer.getResellerCustomer(accountNumber, customerAccountNumber);
        return new ResellerCustomerWrapper(resellerCustomerJson, this.serviceClientCustomer);
    }

    public ResellerCustomerOperator.ResellerCustomerBuilder define(String accountNumber, String customerName) {
        return new ResellerCustomerOperator(this.serviceClientCustomer).create(accountNumber, customerName);
    }
}
