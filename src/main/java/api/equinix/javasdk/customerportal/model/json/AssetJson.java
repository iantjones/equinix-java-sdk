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
import api.equinix.javasdk.customerportal.enums.AssetType;
import api.equinix.javasdk.customerportal.model.Asset;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Getter;

import java.util.List;

@Getter
public class AssetJson implements Serializable, Asset {

    @Getter static TypeReference<Page<Asset, AssetJson>> pagedTypeRef = new TypeReference<>() {};
    @Getter static TypeReference<List<AssetJson>> listTypeRef = new TypeReference<>() {};
    @Getter static TypeReference<AssetJson> singleTypeRef = new TypeReference<>() {};

    @JsonProperty("uuid")
    private String uuid;

    @JsonProperty("assetType")
    private AssetType assetType;

    @JsonProperty("name")
    private String name;

    @JsonProperty("ibx")
    private String ibx;

    @JsonProperty("cageId")
    private String cageId;

    @JsonProperty("cabinetId")
    private String cabinetId;

    @JsonProperty("status")
    private String status;
}
