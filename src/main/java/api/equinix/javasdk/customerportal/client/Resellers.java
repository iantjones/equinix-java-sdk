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

/**
 * Client interface for managing reseller relationships in the Equinix Customer Portal.
 * Provides operations to list reseller accounts, manage reseller customers, and create
 * new customer associations under a reseller account.
 */
public interface Resellers {

    /**
     * Lists all reseller accounts for the current user.
     *
     * @return a paginated list of reseller accounts
     */
    PaginatedList<Reseller> list();

    /**
     * Lists all customers under the specified reseller account.
     *
     * @param accountNUmber the reseller account number
     * @return a paginated list of reseller customers
     */
    PaginatedList<ResellerCustomer> listCustomers(String accountNUmber);

    /**
     * Retrieves a specific reseller customer by the reseller and customer account numbers.
     *
     * @param accountNumber the reseller account number
     * @param customerAccountNumber the customer account number
     * @return the matching reseller customer
     */
    ResellerCustomer getResellerCustomer(String accountNumber, String customerAccountNumber);

    /**
     * Returns a builder for defining a new reseller customer association.
     *
     * @param accountNumber the reseller account number
     * @param customerName the name of the new customer
     * @return a new ResellerCustomerBuilder instance
     */
    ResellerCustomerOperator.ResellerCustomerBuilder define(String accountNumber, String customerName);
}
