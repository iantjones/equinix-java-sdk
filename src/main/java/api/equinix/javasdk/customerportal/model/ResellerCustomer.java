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

package api.equinix.javasdk.customerportal.model;

import api.equinix.javasdk.customerportal.enums.AccountStatus;
import api.equinix.javasdk.customerportal.model.json.creators.ResellerCustomerOperator;
import api.equinix.javasdk.customerportal.model.json.creators.ResellerCustomerUpdaterJson;

public interface ResellerCustomer {

    String getCustomerAccountNumber();

    String getCustomerAccountName();

    AccountStatus getStatus();

    String getAccountNumber();
    
    ResellerCustomerOperator.ResellerCustomerUpdater update(String accountNumber);

    Boolean save(String accountNumber, String customerAccountNumber, ResellerCustomerUpdaterJson updaterJson);

    Boolean delete(String accountNumber, String reason);

    Boolean refresh(String accountNumber, String customerAccountNumber);
}