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

package com.eqixiac.equinix.customerportal.model.json;

import com.eqixiac.equinix.customerportal.model.EmailDetails;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * Full details of an email associated with a support case ({@code EmailDetailsResponse}), returned
 * by {@code GET /support/v1/tickets/emailDetails/{emailId}/caseNumber/{caseNumber}}.
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class EmailDetailsResponseJson implements EmailDetails {

    @JsonProperty("orderId")
    private String orderId;

    @JsonProperty("createdDateTime")
    private String createdDateTime;

    @JsonProperty("subject")
    private String subject;

    @JsonProperty("email_content")
    private String emailContent;

    @JsonProperty("FromAddress")
    private String fromAddress;

    @JsonProperty("ToAddress")
    private String toAddress;

    @JsonProperty("CCAddress")
    private String ccAddress;
}
