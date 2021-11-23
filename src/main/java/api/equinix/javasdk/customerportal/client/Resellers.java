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

package api.equinix.javasdk.customerportal.client;

import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.customerportal.model.Reseller;
import api.equinix.javasdk.customerportal.model.ResellerCustomer;
import api.equinix.javasdk.customerportal.model.json.creators.ResellerCustomerOperator;

public interface Resellers {

    PaginatedList<Reseller> list();

    PaginatedList<ResellerCustomer> listCustomers(String accountNUmber);

    ResellerCustomer getResellerCustomer(String accountNumber, String customerAccountNumber);

    ResellerCustomerOperator.ResellerCustomerBuilder define(String accountNumber, String customerName);
}
