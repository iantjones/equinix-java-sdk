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

package com.eqixiac.equinix.ibxsmartview.model.json;

import com.eqixiac.equinix.ibxsmartview.model.AssetDetailsResponse;
import com.eqixiac.equinix.ibxsmartview.model.implementation.AssetDetailsPayload;
import com.eqixiac.equinix.ibxsmartview.model.implementation.Status;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

/**
 * Deserialization holder for the asset/details POST response ({@code AssetDetailsResponse}).
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class AssetDetailsResponseJson implements AssetDetailsResponse {

    @JsonProperty("payLoad")
    private PayloadJson payLoad;

    @JsonProperty("status")
    private Status status;

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PayloadJson implements Payload {

        @JsonProperty("totalCount")
        private Integer totalCount;

        @JsonProperty("assetDetails")
        private List<AssetDetailsPayload> assetDetails;
    }
}
