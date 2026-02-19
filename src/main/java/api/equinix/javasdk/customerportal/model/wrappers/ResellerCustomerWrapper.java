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

package api.equinix.javasdk.customerportal.model.wrappers;

import api.equinix.javasdk.core.http.response.Pageable;
import api.equinix.javasdk.core.model.ResourceImpl;
import api.equinix.javasdk.customerportal.client.internal.implementation.ResellerCustomerClientImpl;
import api.equinix.javasdk.customerportal.model.ResellerCustomer;
import api.equinix.javasdk.customerportal.model.json.ResellerCustomerJson;
import api.equinix.javasdk.customerportal.model.json.creators.ResellerCustomerOperator;
import api.equinix.javasdk.customerportal.model.json.creators.ResellerCustomerUpdaterJson;
import lombok.Getter;
import lombok.experimental.Delegate;

public class ResellerCustomerWrapper extends ResourceImpl<ResellerCustomer> implements ResellerCustomer {

    @Delegate(excludes = ResellerCustomerMutability.class)
    private ResellerCustomerJson json;
    @Getter
    private final Pageable<ResellerCustomer> serviceClient;

    public ResellerCustomerWrapper(ResellerCustomerJson resellerCustomerJson, Pageable<ResellerCustomer> serviceClient) {
        this.json = resellerCustomerJson;
        this.serviceClient = serviceClient;
    }

    public ResellerCustomerOperator.ResellerCustomerUpdater update(String accountNumber) {
        return new ResellerCustomerOperator(this.serviceClient).update(accountNumber, this.json);
    }

    public Boolean save(String accountNumber, String customerAccountNumber, ResellerCustomerUpdaterJson updaterJson) {
        return ((ResellerCustomerClientImpl)this.serviceClient).update(accountNumber, customerAccountNumber, updaterJson);
    }

    public Boolean delete(String accountNumber, String reason) {
        return ((ResellerCustomerClientImpl)this.serviceClient).remove(accountNumber, this.getCustomerAccountNumber(), reason);
    }

    public Boolean refresh(String accountNumber, String customerAccountNumber) {
        this.json = ((ResellerCustomerClientImpl)this.serviceClient).getResellerCustomer(accountNumber, customerAccountNumber);
        return true;
    }

    private interface ResellerCustomerMutability {
        ResellerCustomerOperator.ResellerCustomerUpdater update(String accountNumber);
        Boolean save(String accountNumber, String customerAccountNumber, ResellerCustomerUpdaterJson updaterJson);
        Boolean delete(String accountNumber, String reason);
        Boolean refresh(String accountNumber, String customerAccountNumber);
    }
}
