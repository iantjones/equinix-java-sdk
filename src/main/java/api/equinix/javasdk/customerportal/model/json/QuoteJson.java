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
import api.equinix.javasdk.customerportal.enums.QuoteStatus;
import api.equinix.javasdk.customerportal.model.Quote;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Getter;

import java.util.List;

@Getter
public class QuoteJson implements Serializable {

    @Getter static TypeReference<Page<Quote, QuoteJson>> pagedTypeRef = new TypeReference<>() {};
    @Getter static TypeReference<List<QuoteJson>> listTypeRef = new TypeReference<>() {};
    @Getter static TypeReference<QuoteJson> singleTypeRef = new TypeReference<>() {};

    @JsonProperty("uuid")
    private String uuid;

    @JsonProperty("quoteNumber")
    private String quoteNumber;

    @JsonProperty("status")
    private QuoteStatus status;

    @JsonProperty("totalAmount")
    private String totalAmount;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("expirationDate")
    private String expirationDate;

    @JsonProperty("createdDate")
    private String createdDate;
}
