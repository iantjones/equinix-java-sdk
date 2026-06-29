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

import api.equinix.javasdk.core.model.RequestBuilderBase;
import api.equinix.javasdk.core.util.ModelUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class RequestBuilder {

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Invoice extends RequestBuilderBase<Invoice> {

        private LocalDate startDate;
        private LocalDate endDate;
        private List<String> accountNumbers;
        private List<String> transactionIds;

        public static Invoice builder() {
            return new Invoice();
        }

        public Invoice withStart(LocalDate startDate) {
            this.startDate = startDate;
            return this;
        }

        public Invoice withEnd(LocalDate endDate) {
            this.endDate = endDate;
            return this;
        }

        public Invoice withAccountNumber(String accountNumber) {
            if(this.accountNumbers == null) {
                this.accountNumbers = new ArrayList<>();
            }
            this.accountNumbers.add(accountNumber);
            return this;
        }

        public Invoice withTransactionId(String transactionId) {
            if(this.transactionIds == null) {
                this.transactionIds = new ArrayList<>();
            }
            this.transactionIds.add(transactionId);
            return this;
        }

        public Invoice build() {
            this.queryParameters = new HashMap<>();

            this.queryParameters.put("startDate", ModelUtils.singleValueList(this.startDate.toString()));
            this.queryParameters.put("endDate", ModelUtils.singleValueList(this.endDate.toString()));

            if(accountNumbers.size() > 0) {
                this.queryParameters.put("accountNumbers", this.accountNumbers);
            }

            if(transactionIds.size() > 0) {
                this.queryParameters.put("transactionIds", this.transactionIds);
            }

            this.wasBuilt = true;
            return this;
        }
    }
}
