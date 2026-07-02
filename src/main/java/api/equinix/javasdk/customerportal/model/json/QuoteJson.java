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

import api.equinix.javasdk.customerportal.enums.SubChannel;
import api.equinix.javasdk.customerportal.enums.Channel;
import api.equinix.javasdk.customerportal.enums.QuoteRequestType;
import api.equinix.javasdk.customerportal.enums.QuoteStatus;
import api.equinix.javasdk.customerportal.model.implementation.QuoteContact;
import api.equinix.javasdk.customerportal.model.implementation.QuoteDetail;
import api.equinix.javasdk.customerportal.model.implementation.QuotePricing;
import api.equinix.javasdk.customerportal.model.implementation.QuoteTermsOfUse;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Getter;

import java.util.List;

/**
 * JSON model for a quote ({@code quote_response} = {@code quote_response_base} + {@code details[]})
 * from the Quotes v2 API. Wrapped by {@code QuoteWrapper} for the public {@code Quote} view.
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class QuoteJson {

    @Getter static TypeReference<List<QuoteJson>> listTypeRef = new TypeReference<>() {};

    @JsonProperty("quoteId")
    private String quoteId;

    @JsonProperty("accountName")
    private String accountName;

    @JsonProperty("accountNumber")
    private String accountNumber;

    @JsonProperty("quoteRequestType")
    private QuoteRequestType quoteRequestType;

    @JsonProperty("contacts")
    private List<QuoteContact> contacts;

    @JsonProperty("status")
    private QuoteStatus status;

    @JsonProperty("createdDateTime")
    private String createdDateTime;

    @JsonProperty("updatedDateTime")
    private String updatedDateTime;

    @JsonProperty("expirationDateTime")
    private String expirationDateTime;

    @JsonProperty("channel")
    private Channel channel;

    @JsonProperty("subChannel")
    private SubChannel subChannel;

    @JsonProperty("customerReferenceId")
    private String customerReferenceId;

    @JsonProperty("agreementNumber")
    private String agreementNumber;

    @JsonProperty("currencyCode")
    private String currencyCode;

    @JsonProperty("termsOfUse")
    private List<QuoteTermsOfUse> termsOfUse;

    @JsonProperty("totalPricing")
    private List<QuotePricing> totalPricing;

    @JsonProperty("versionNumber")
    private String versionNumber;

    @JsonProperty("details")
    private List<QuoteDetail> details;
}
