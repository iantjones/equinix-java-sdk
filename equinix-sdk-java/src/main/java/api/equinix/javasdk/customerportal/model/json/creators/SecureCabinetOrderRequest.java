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

import api.equinix.javasdk.customerportal.enums.ContractTerm;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * Request body for a secure cabinet order
 * ({@code POST /securecabinet/v1/orders}, {@code OrderCreateRequest}).
 *
 * <p>{@code accountNumber}, {@code ibxCode}, {@code contractTerm} and {@code orderItem} are
 * required by the API. {@code contractTerm} is a {@link ContractTerm} and {@code orderItem} is a
 * typed {@link SecureCabinetOrderItem} carrying the cabinet configuration. The optional
 * {@code technicalContact} is a typed {@link SecureCabinetContact}.</p>
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SecureCabinetOrderRequest {

    @JsonProperty("accountNumber")
    private final String accountNumber;

    @JsonProperty("ibxCode")
    private final String ibxCode;

    @JsonProperty("contractTerm")
    private final ContractTerm contractTerm;

    @JsonProperty("orderItem")
    private final SecureCabinetOrderItem orderItem;

    @JsonProperty("customerReference")
    private final String customerReference;

    @JsonProperty("endCustomerName")
    private final String endCustomerName;

    @JsonProperty("purchaseOrderNumber")
    private final String purchaseOrderNumber;

    @JsonProperty("technicalContact")
    private final SecureCabinetContact technicalContact;

    private SecureCabinetOrderRequest(Builder builder) {
        this.accountNumber = builder.accountNumber;
        this.ibxCode = builder.ibxCode;
        this.contractTerm = builder.contractTerm;
        this.orderItem = builder.orderItem;
        this.customerReference = builder.customerReference;
        this.endCustomerName = builder.endCustomerName;
        this.purchaseOrderNumber = builder.purchaseOrderNumber;
        this.technicalContact = builder.technicalContact;
    }

    /**
     * Returns a new builder for a secure cabinet order request.
     *
     * @param accountNumber the ordering account number (required)
     * @param ibxCode       the target IBX code (required)
     * @param contractTerm  the contract term (required)
     * @param orderItem     the cabinet configuration (required)
     * @return a new builder
     */
    public static Builder builder(String accountNumber, String ibxCode, ContractTerm contractTerm,
                                  SecureCabinetOrderItem orderItem) {
        return new Builder(accountNumber, ibxCode, contractTerm, orderItem);
    }

    public static class Builder {
        private final String accountNumber;
        private final String ibxCode;
        private final ContractTerm contractTerm;
        private final SecureCabinetOrderItem orderItem;
        private String customerReference;
        private String endCustomerName;
        private String purchaseOrderNumber;
        private SecureCabinetContact technicalContact;

        private Builder(String accountNumber, String ibxCode, ContractTerm contractTerm, SecureCabinetOrderItem orderItem) {
            this.accountNumber = accountNumber;
            this.ibxCode = ibxCode;
            this.contractTerm = contractTerm;
            this.orderItem = orderItem;
        }

        public Builder customerReference(String customerReference) {
            this.customerReference = customerReference;
            return this;
        }

        public Builder endCustomerName(String endCustomerName) {
            this.endCustomerName = endCustomerName;
            return this;
        }

        public Builder purchaseOrderNumber(String purchaseOrderNumber) {
            this.purchaseOrderNumber = purchaseOrderNumber;
            return this;
        }

        public Builder technicalContact(SecureCabinetContact technicalContact) {
            this.technicalContact = technicalContact;
            return this;
        }

        public SecureCabinetOrderRequest build() {
            return new SecureCabinetOrderRequest(this);
        }
    }
}
