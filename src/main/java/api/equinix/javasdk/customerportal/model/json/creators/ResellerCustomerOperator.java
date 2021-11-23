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

package api.equinix.javasdk.customerportal.model.json.creators;

import api.equinix.javasdk.core.http.response.Pageable;
import api.equinix.javasdk.core.internal.Constants;
import api.equinix.javasdk.core.model.ResourceImpl;
import api.equinix.javasdk.customerportal.client.internal.implementation.ResellerCustomerClientImpl;
import api.equinix.javasdk.customerportal.enums.Service;
import api.equinix.javasdk.customerportal.model.ResellerCustomer;
import api.equinix.javasdk.customerportal.model.implementation.Address;
import api.equinix.javasdk.customerportal.model.json.ResellerCustomerJson;
import lombok.AccessLevel;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

public class ResellerCustomerOperator extends ResourceImpl<ResellerCustomer> {

    @Getter
    private final Pageable<ResellerCustomer> serviceClient;

    public ResellerCustomerOperator(Pageable<ResellerCustomer> serviceClient) {
        this.serviceClient = serviceClient;
    }

    public ResellerCustomerBuilder create(String accountNumber, String customerAccountName) {
        return new ResellerCustomerBuilder(accountNumber, customerAccountName);
    }

    public ResellerCustomerUpdater update(String accountNumber, ResellerCustomerJson resellerCustomerJson) {
        return new ResellerCustomerUpdater(accountNumber, resellerCustomerJson);
    }

    @Getter(AccessLevel.PACKAGE)
    public class ResellerCustomerBuilder {

        private final String accountNumber;
        private final String customerAccountName;
        private String resellerNotificationContact;
        private List<Service> permittedServices;
        private Address address;

        protected ResellerCustomerBuilder(String accountNumber, String customerAccountName) {
            this.accountNumber = accountNumber;
            this.customerAccountName = customerAccountName;
        }

        public ResellerCustomerBuilder withResellerNotificationContact(String resellerNotificationContact) {
            this.resellerNotificationContact = resellerNotificationContact;
            return this;
        }

        public ResellerCustomerBuilder withPermittedService(Service permittedService) {
            if(this.permittedServices == null) {
                this.permittedServices = new ArrayList<>();
            }

            this.permittedServices.add(permittedService);
            return this;
        }

        public ResellerCustomerBuilder withAddress(Address address) {
            this.address = address;
            return this;
        }

        public Boolean create() {
            ResellerCustomerCreatorJson resellerCustomerCreatorJson = new ResellerCustomerCreatorJson(this);
            return ((ResellerCustomerClientImpl) ResellerCustomerOperator.this.getServiceClient()).create(this.accountNumber, resellerCustomerCreatorJson);
        }
    }

    public class ResellerCustomerUpdater {

        private final String accountNumber;
        private ResellerCustomerJson json;
        private ResellerCustomerUpdaterJson updaterJson;

        protected ResellerCustomerUpdater(String accountNumber, ResellerCustomerJson json) {
            this.accountNumber = accountNumber;
            this.json = json;
            this.updaterJson = Constants.JSON_CONVERTOR.convertValue(this.json, ResellerCustomerUpdaterJson.class);
        }

        public ResellerCustomerOperator.ResellerCustomerUpdater withCustomerAccountName(String customerAccountName) {
            this.updaterJson.setCustomerAccountName(customerAccountName);
            return this;
        }

        public ResellerCustomerOperator.ResellerCustomerUpdater withResellerNotificationContact(String resellerNotificationContact) {
            this.updaterJson.setResellerNotificationContact(resellerNotificationContact);
            return this;
        }

        public ResellerCustomerOperator.ResellerCustomerUpdater addService(Service service) {
            List<Service> permittedServices = updaterJson.getPermittedServices();
            if(permittedServices == null) {
                permittedServices = new ArrayList<>();
            }

            if(!permittedServices.contains(service)) {
                permittedServices.add(service);
            }
            updaterJson.setPermittedServices(permittedServices);

            return this;
        }

        public ResellerCustomerOperator.ResellerCustomerUpdater removeService(Service service) {
            List<Service> permittedServices = updaterJson.getPermittedServices();

            if(permittedServices == null) {
                return this;
            }
            permittedServices.remove(service);
            updaterJson.setPermittedServices(permittedServices);

            return this;
        }

        public ResellerCustomerOperator.ResellerCustomerUpdater replaceAddress(Address address) {
            updaterJson.setAddress(address);
            return this;
        }

        public Boolean save() {
            return ((ResellerCustomerClientImpl) ResellerCustomerOperator.this.getServiceClient()).update(this.accountNumber, this.json.getCustomerAccountNumber(), this.updaterJson);
        }
    }
}
