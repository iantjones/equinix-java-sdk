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

package api.equinix.javasdk.ibxsmartview.model.json.creators;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

/**
 * Typed request body for the asset/details POST endpoint (AssetDetailsRequest = GenericRequest + assetIds).
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AssetDetailsRequest {

    @JsonProperty("accountNo")
    private final String accountNo;

    @JsonProperty("ibx")
    private final String ibx;

    @JsonProperty("classification")
    private final String classification;

    @JsonProperty("assetIds")
    private final List<String> assetIds;

    public AssetDetailsRequest(String accountNo, String ibx, String classification, List<String> assetIds) {
        this.accountNo = accountNo;
        this.ibx = ibx;
        this.classification = classification;
        this.assetIds = assetIds;
    }
}
