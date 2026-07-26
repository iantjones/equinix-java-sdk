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

import com.eqixiac.equinix.customerportal.enums.TicketStatus;
import com.eqixiac.equinix.customerportal.model.implementation.TicketAttachment;
import com.eqixiac.equinix.customerportal.model.implementation.TicketContact;
import com.eqixiac.equinix.customerportal.model.implementation.TicketNote;
import com.eqixiac.equinix.customerportal.model.implementation.TicketResolution;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * A trouble ticket as returned by the Tickets v2 API ({@code Tickets}). A record of an issue in
 * the ticket management system, identified by {@code getId()}.
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class TroubleTicketJson {

    @JsonProperty("id")
    private String id;

    @JsonProperty("category")
    private String category;

    @JsonProperty("subCategory")
    private String subCategory;

    @JsonProperty("description")
    private String description;

    @JsonProperty("primaryId")
    private String primaryId;

    @JsonProperty("secondaryId")
    private String secondaryId;

    @JsonProperty("customerReferenceId")
    private String customerReferenceId;

    @JsonProperty("occurredDateTime")
    private String occurredDateTime;

    /**
     * Category-specific trouble details ({@code Tickets.details}, {@code anyOf} eleven
     * category schemas such as {@code LAYER1_0000_XXXX} or {@code POWER_0002_XXXX}), captured as a
     * free-form map because the shape depends on the ticket category.
     */
    @JsonProperty("details")
    private Map<String, Object> details;

    @JsonProperty("resolutionDateTime")
    private String resolutionDateTime;

    @JsonProperty("status")
    private TicketStatus status;

    @JsonProperty("resolutions")
    private List<TicketResolution> resolutions;

    @JsonProperty("notes")
    private List<TicketNote> notes;

    @JsonProperty("attachments")
    private List<TicketAttachment> attachments;

    @JsonProperty("contacts")
    private List<TicketContact> contacts;
}
