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

package com.eqixiac.equinix.fabric.model.json.creators;

import com.eqixiac.equinix.fabric.enums.EiaBillingType;
import com.eqixiac.equinix.fabric.enums.EiaServiceType;
import com.eqixiac.equinix.fabric.model.Project;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Setter;

/**
 * Request body for creating an Equinix Internet Access (EIA) service, modelling the spec's
 * {@code InternetAccessPostRequest}. The required fields are {@code type}, {@code name},
 * {@code account}, {@code billing}, {@code project}, and {@code routingProtocol}.
 */
@Setter(AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EiaServiceCreatorJson {

    @JsonProperty("type")
    private EiaServiceType type;

    @JsonProperty("name")
    private String name;

    @JsonProperty("bandwidth")
    private Integer bandwidth;

    @JsonProperty("bandwidthCommit")
    private Integer bandwidthCommit;

    @JsonProperty("routingProtocol")
    private EiaRoutingProtocolRequest routingProtocol;

    @JsonProperty("order")
    private OrderRequest order;

    @JsonProperty("billing")
    private BillingRequest billing;

    @JsonProperty("project")
    private Project project;

    @JsonProperty("account")
    private AccountRequest account;

    public EiaServiceCreatorJson(EiaServiceOperator.EiaServiceBuilder eiaServiceBuilder) {
        this.type = eiaServiceBuilder.getType();
        this.name = eiaServiceBuilder.getName();
        this.bandwidth = eiaServiceBuilder.getBandwidth();
        this.bandwidthCommit = eiaServiceBuilder.getBandwidthCommit();
        this.routingProtocol = eiaServiceBuilder.getRoutingProtocol();
        this.project = eiaServiceBuilder.getProject();
        if (eiaServiceBuilder.getAccountNumber() != null) {
            this.account = new AccountRequest(eiaServiceBuilder.getAccountNumber());
        }
        if (eiaServiceBuilder.getBillingType() != null) {
            this.billing = new BillingRequest(eiaServiceBuilder.getBillingType());
        }
        if (eiaServiceBuilder.getPurchaseOrderNumber() != null) {
            this.order = new OrderRequest(eiaServiceBuilder.getPurchaseOrderNumber());
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    static class AccountRequest {
        @JsonProperty("accountNumber")
        private final String accountNumber;

        AccountRequest(String accountNumber) {
            this.accountNumber = accountNumber;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    static class BillingRequest {
        @JsonProperty("type")
        private final EiaBillingType type;

        BillingRequest(EiaBillingType type) {
            this.type = type;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    static class OrderRequest {
        @JsonProperty("purchaseOrderNumber")
        private final String purchaseOrderNumber;

        OrderRequest(String purchaseOrderNumber) {
            this.purchaseOrderNumber = purchaseOrderNumber;
        }
    }
}
