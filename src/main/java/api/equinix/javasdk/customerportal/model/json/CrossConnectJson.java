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
import api.equinix.javasdk.customerportal.enums.CrossConnectStatus;
import api.equinix.javasdk.customerportal.enums.CrossConnectType;
import api.equinix.javasdk.customerportal.enums.MediaType;
import api.equinix.javasdk.customerportal.model.CrossConnect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Getter;

import java.util.List;

@Getter
public class CrossConnectJson implements Serializable {

    @Getter static TypeReference<Page<CrossConnect, CrossConnectJson>> pagedTypeRef = new TypeReference<>() {};
    @Getter static TypeReference<List<CrossConnectJson>> listTypeRef = new TypeReference<>() {};
    @Getter static TypeReference<CrossConnectJson> singleTypeRef = new TypeReference<>() {};

    @JsonProperty("uuid")
    private String uuid;

    @JsonProperty("href")
    private String href;

    @JsonProperty("name")
    private String name;

    @JsonProperty("type")
    private CrossConnectType type;

    @JsonProperty("status")
    private CrossConnectStatus status;

    @JsonProperty("mediaType")
    private MediaType mediaType;

    @JsonProperty("aEndIbx")
    private String aEndIbx;

    @JsonProperty("zEndIbx")
    private String zEndIbx;

    @JsonProperty("aEndCageId")
    private String aEndCageId;

    @JsonProperty("zEndCageId")
    private String zEndCageId;

    @JsonProperty("aEndPatchPanelId")
    private String aEndPatchPanelId;

    @JsonProperty("zEndPatchPanelId")
    private String zEndPatchPanelId;

    @JsonProperty("aEndPatchPanelPortId")
    private String aEndPatchPanelPortId;

    @JsonProperty("zEndPatchPanelPortId")
    private String zEndPatchPanelPortId;

    @JsonProperty("protocolType")
    private String protocolType;

    @JsonProperty("bandwidth")
    private Integer bandwidth;

    @JsonProperty("connectionService")
    private String connectionService;

    @JsonProperty("accountNumber")
    private String accountNumber;

    @JsonProperty("customerReferenceId")
    private String customerReferenceId;

    @JsonProperty("createdDate")
    private String createdDate;

    @JsonProperty("lastUpdatedDate")
    private String lastUpdatedDate;
}
