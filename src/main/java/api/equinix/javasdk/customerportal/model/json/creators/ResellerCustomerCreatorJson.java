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

import api.equinix.javasdk.customerportal.enums.Service;
import api.equinix.javasdk.customerportal.model.implementation.Address;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Setter(AccessLevel.PRIVATE)
public class ResellerCustomerCreatorJson {

    @JsonProperty("customerAccountName")
    private String customerAccountName;

    @JsonProperty("resellerNotificationContact")
    private String resellerNotificationContact;

    @JsonProperty("permittedServices")
    private List<Service> permittedServices;

    @JsonProperty("address")
    private Address address;

    public ResellerCustomerCreatorJson(ResellerCustomerOperator.ResellerCustomerBuilder resellerCustomerBuilder) {
        this.customerAccountName = resellerCustomerBuilder.getCustomerAccountName();
        this.resellerNotificationContact = resellerCustomerBuilder.getResellerNotificationContact();
        this.permittedServices = resellerCustomerBuilder.getPermittedServices();
        this.address = resellerCustomerBuilder.getAddress();
    }
}
