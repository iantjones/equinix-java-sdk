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

package com.eqixiac.equinix.operations.client;

import com.eqixiac.equinix.core.model.RequestBuilderBase;
import com.eqixiac.equinix.core.util.ModelUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Query builders for operations list calls.
 */
public class RequestBuilder {

    /**
     * Query filter for billing v2 {@code GET /v2/invoices} and {@code GET /v2/invoices/details}.
     *
     * <p>Every filter is optional in the billing v2 spec; only the filters that were set are sent.
     * {@code accountNumbers} and {@code transactionIds} are declared {@code style: form,
     * explode: false} in the spec, so several values are sent as one comma-separated parameter
     * ({@code accountNumbers=1,2}).</p>
     */
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Invoice extends RequestBuilderBase<Invoice> {

        private LocalDate startDate;
        private LocalDate endDate;
        private final List<String> accountNumbers = new ArrayList<>();
        private final List<String> transactionIds = new ArrayList<>();

        /**
         * Starts an empty invoice filter.
         *
         * @return a new builder
         */
        public static Invoice builder() {
            return new Invoice();
        }

        /**
         * Sets {@code startDate}.
         *
         * @param startDate start date of the invoice, or {@code null} to leave unset
         * @return this builder
         */
        public Invoice withStart(LocalDate startDate) {
            this.startDate = startDate;
            return this;
        }

        /**
         * Sets {@code endDate}.
         *
         * @param endDate end date of the invoice, or {@code null} to leave unset
         * @return this builder
         */
        public Invoice withEnd(LocalDate endDate) {
            this.endDate = endDate;
            return this;
        }

        /**
         * Adds an account number to filter by; {@code null} or blank values are ignored.
         *
         * @param accountNumber billing account number
         * @return this builder
         */
        public Invoice withAccountNumber(String accountNumber) {
            if (accountNumber != null && !accountNumber.isBlank()) {
                this.accountNumbers.add(accountNumber);
            }
            return this;
        }

        /**
         * Adds a transaction id to filter by; {@code null} or blank values are ignored.
         *
         * @param transactionId invoice or credit-memo transaction id
         * @return this builder
         */
        public Invoice withTransactionId(String transactionId) {
            if (transactionId != null && !transactionId.isBlank()) {
                this.transactionIds.add(transactionId);
            }
            return this;
        }

        /**
         * Builds the query parameters from the filters that were set.
         *
         * @return this builder, marked as built
         */
        public Invoice build() {
            this.queryParameters = new HashMap<>();

            if (startDate != null) {
                this.queryParameters.put("startDate", ModelUtils.singleValueList(startDate.toString()));
            }
            if (endDate != null) {
                this.queryParameters.put("endDate", ModelUtils.singleValueList(endDate.toString()));
            }
            if (!accountNumbers.isEmpty()) {
                this.queryParameters.put("accountNumbers",
                        ModelUtils.singleValueList(String.join(",", accountNumbers)));
            }
            if (!transactionIds.isEmpty()) {
                this.queryParameters.put("transactionIds",
                        ModelUtils.singleValueList(String.join(",", transactionIds)));
            }

            this.wasBuilt = true;
            return this;
        }
    }
}
