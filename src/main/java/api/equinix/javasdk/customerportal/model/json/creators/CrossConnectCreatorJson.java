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

package api.equinix.javasdk.customerportal.model.json.creators;

import api.equinix.javasdk.customerportal.enums.CrossConnectType;
import api.equinix.javasdk.customerportal.enums.MediaType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class CrossConnectCreatorJson {

    @JsonProperty("name") private String name;
    @JsonProperty("type") private CrossConnectType type;
    @JsonProperty("mediaType") private MediaType mediaType;
    @JsonProperty("aEndIbx") private String aEndIbx;
    @JsonProperty("zEndIbx") private String zEndIbx;
    @JsonProperty("aEndCageId") private String aEndCageId;
    @JsonProperty("zEndCageId") private String zEndCageId;
    @JsonProperty("aEndPatchPanelId") private String aEndPatchPanelId;
    @JsonProperty("zEndPatchPanelId") private String zEndPatchPanelId;
    @JsonProperty("aEndPatchPanelPortId") private String aEndPatchPanelPortId;
    @JsonProperty("zEndPatchPanelPortId") private String zEndPatchPanelPortId;
    @JsonProperty("protocolType") private String protocolType;
    @JsonProperty("bandwidth") private Integer bandwidth;
    @JsonProperty("connectionService") private String connectionService;
    @JsonProperty("accountNumber") private String accountNumber;
    @JsonProperty("customerReferenceId") private String customerReferenceId;

    public CrossConnectCreatorJson(CrossConnectOperator.CrossConnectBuilder builder) {
        this.name = builder.getName();
        this.type = builder.getType();
        this.mediaType = builder.getMediaType();
        this.aEndIbx = builder.getAEndIbx();
        this.zEndIbx = builder.getZEndIbx();
        this.aEndCageId = builder.getAEndCageId();
        this.zEndCageId = builder.getZEndCageId();
        this.aEndPatchPanelId = builder.getAEndPatchPanelId();
        this.zEndPatchPanelId = builder.getZEndPatchPanelId();
        this.aEndPatchPanelPortId = builder.getAEndPatchPanelPortId();
        this.zEndPatchPanelPortId = builder.getZEndPatchPanelPortId();
        this.protocolType = builder.getProtocolType();
        this.bandwidth = builder.getBandwidth();
        this.connectionService = builder.getConnectionService();
        this.accountNumber = builder.getAccountNumber();
        this.customerReferenceId = builder.getCustomerReferenceId();
    }
}
