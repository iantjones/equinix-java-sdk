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

import com.eqixiac.equinix.customerportal.enums.Channel;
import com.eqixiac.equinix.customerportal.enums.SupportCaseStatus;
import com.eqixiac.equinix.customerportal.model.SupportCase;
import com.eqixiac.equinix.customerportal.model.implementation.SupportCaseAttachmentInfo;
import com.eqixiac.equinix.customerportal.model.implementation.SupportCaseContact;
import com.eqixiac.equinix.customerportal.model.implementation.SupportCaseEmail;
import com.eqixiac.equinix.customerportal.model.implementation.SupportCaseLocation;
import com.eqixiac.equinix.customerportal.model.implementation.SupportCaseNote;
import com.eqixiac.equinix.customerportal.model.implementation.SupportCaseOtherDetails;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

/**
 * A support case retrieved by case or order number from the Equinix Customer Portal support v2 API
 * ({@code SingleCaseResponseV2}).
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class SupportCaseJson implements SupportCase {

    @JsonProperty("id")
    private String id;

    @JsonProperty("accountNumber")
    private String accountNumber;

    @JsonProperty("accountName")
    private String accountName;

    @JsonProperty("customerReferenceId")
    private String customerReferenceId;

    @JsonProperty("channel")
    private Channel channel;

    @JsonProperty("orderId")
    private String orderId;

    @JsonProperty("status")
    private SupportCaseStatus status;

    @JsonProperty("createdDateTime")
    private String createdDateTime;

    @JsonProperty("location")
    private SupportCaseLocation location;

    @JsonProperty("contacts")
    private List<SupportCaseContact> contacts;

    @JsonProperty("notes")
    private List<SupportCaseNote> notes;

    @JsonProperty("attachments")
    private List<SupportCaseAttachmentInfo> attachments;

    @JsonProperty("email")
    private List<SupportCaseEmail> email;

    @JsonProperty("otherDetails")
    private SupportCaseOtherDetails otherDetails;
}
