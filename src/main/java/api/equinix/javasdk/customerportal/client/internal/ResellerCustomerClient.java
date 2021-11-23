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

package api.equinix.javasdk.customerportal.client.internal;

import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.Pageable;
import api.equinix.javasdk.customerportal.model.ResellerCustomer;
import api.equinix.javasdk.customerportal.model.json.ResellerCustomerJson;
import api.equinix.javasdk.customerportal.model.json.creators.ResellerCustomerCreatorJson;
import api.equinix.javasdk.customerportal.model.json.creators.ResellerCustomerUpdaterJson;

public interface ResellerCustomerClient<T> extends Pageable<T> {

    Page<ResellerCustomer, ResellerCustomerJson> list(String accountNumber);

    ResellerCustomerJson getResellerCustomer(String accountNumber, String customerAccountNumber);

    Boolean create(String accountNumber, ResellerCustomerCreatorJson resellerCustomerCreatorJson);

    Boolean update(String accountNumber, String customerAccountNumber, ResellerCustomerUpdaterJson resellerCustomerUpdaterJson);

    Boolean remove(String accountNumber, String customerAccountNumber, String reason);
}
