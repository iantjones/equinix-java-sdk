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
import api.equinix.javasdk.customerportal.enums.SmartHandsStatus;
import api.equinix.javasdk.customerportal.enums.SmartHandsType;
import api.equinix.javasdk.customerportal.model.SmartHands;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Getter;

import java.util.List;

@Getter
public class SmartHandsJson implements Serializable {

    @Getter static TypeReference<Page<SmartHands, SmartHandsJson>> pagedTypeRef = new TypeReference<>() {};
    @Getter static TypeReference<List<SmartHandsJson>> listTypeRef = new TypeReference<>() {};
    @Getter static TypeReference<SmartHandsJson> singleTypeRef = new TypeReference<>() {};

    @JsonProperty("uuid")
    private String uuid;

    @JsonProperty("href")
    private String href;

    @JsonProperty("requestId")
    private String requestId;

    @JsonProperty("type")
    private SmartHandsType type;

    @JsonProperty("status")
    private SmartHandsStatus status;

    @JsonProperty("ibxCode")
    private String ibxCode;

    @JsonProperty("accountNumber")
    private String accountNumber;

    @JsonProperty("summary")
    private String summary;

    @JsonProperty("description")
    private String description;

    @JsonProperty("requestorName")
    private String requestorName;

    @JsonProperty("requestorEmail")
    private String requestorEmail;

    @JsonProperty("scheduleStartDate")
    private String scheduleStartDate;

    @JsonProperty("scheduleEndDate")
    private String scheduleEndDate;

    @JsonProperty("createdDate")
    private String createdDate;

    @JsonProperty("lastUpdatedDate")
    private String lastUpdatedDate;
}
