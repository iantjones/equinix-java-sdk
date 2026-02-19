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

package api.equinix.javasdk.customerportal.model.json;

import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.model.Serializable;
import api.equinix.javasdk.customerportal.model.BillingCredit;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Getter;

import java.util.List;

@Getter
public class BillingCreditJson implements Serializable, BillingCredit {

    @Getter static TypeReference<Page<BillingCredit, BillingCreditJson>> pagedTypeRef = new TypeReference<>() {};
    @Getter static TypeReference<List<BillingCreditJson>> listTypeRef = new TypeReference<>() {};
    @Getter static TypeReference<BillingCreditJson> singleTypeRef = new TypeReference<>() {};

    @JsonProperty("uuid")
    private String uuid;

    @JsonProperty("creditNumber")
    private String creditNumber;

    @JsonProperty("amount")
    private Double amount;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("status")
    private String status;

    @JsonProperty("accountNumber")
    private String accountNumber;

    @JsonProperty("issuedDate")
    private String issuedDate;

    @JsonProperty("appliedDate")
    private String appliedDate;

    @JsonProperty("description")
    private String description;
}
