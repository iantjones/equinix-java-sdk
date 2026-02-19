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

package api.equinix.javasdk.ibxsmartview.model.json;

import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.model.Serializable;
import api.equinix.javasdk.ibxsmartview.model.SmartViewAsset;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SmartViewAssetJson implements SmartViewAsset, Serializable {

    @Getter static TypeReference<Page<SmartViewAsset, SmartViewAssetJson>> pagedTypeRef = new TypeReference<>() {};
    @Getter static TypeReference<SmartViewAssetJson> singleTypeRef = new TypeReference<>() {};

    @JsonProperty("assetId")
    private String assetId;

    @JsonProperty("ibx")
    private String ibx;

    @JsonProperty("classification")
    private String classification;

    @JsonProperty("assetType")
    private String assetType;

    @JsonProperty("displayName")
    private String displayName;

    @JsonProperty("cage")
    private String cage;

    @JsonProperty("cabinet")
    private String cabinet;

    @JsonProperty("status")
    private String status;
}
