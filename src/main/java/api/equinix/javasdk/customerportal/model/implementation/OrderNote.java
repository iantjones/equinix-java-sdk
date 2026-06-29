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

package api.equinix.javasdk.customerportal.model.implementation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

/**
 * A note associated with an order ({@code Note}): its {@code id} and {@code referenceId},
 * {@code createdDateTime}, free-text {@code text}, {@code author}, {@code type}
 * ({@code CUSTOMER_QUERY}, {@code CUSTOMER_NOTES}, {@code TECHNICIAN_QUERY} or
 * {@code TECHNICIAN_NOTES}) and any {@link OrderAttachmentInfo attachments}.
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderNote {

    @JsonProperty("id")
    private String id;

    @JsonProperty("referenceId")
    private String referenceId;

    @JsonProperty("createdDateTime")
    private String createdDateTime;

    @JsonProperty("text")
    private String text;

    @JsonProperty("author")
    private String author;

    @JsonProperty("type")
    private String type;

    @JsonProperty("attachments")
    private List<OrderAttachmentInfo> attachments;
}
