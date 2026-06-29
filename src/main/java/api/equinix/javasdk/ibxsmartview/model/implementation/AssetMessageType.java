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

package api.equinix.javasdk.ibxsmartview.model.implementation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * An {@code asset} message-type entry within a subscription's {@link MessageType}. Provides the
 * latest readings for environmental, electrical and mechanical assets within an IBX.
 */
@Getter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AssetMessageType {

    @JsonProperty("accountNumber")
    private String accountNumber;

    @JsonProperty("ibx")
    private List<String> ibx;

    @JsonProperty("assetClassification")
    private List<String> assetClassification;

    @JsonProperty("assetId")
    private List<String> assetId;

    @Builder
    public AssetMessageType(String accountNumber, List<String> ibx, List<String> assetClassification, List<String> assetId) {
        this.accountNumber = accountNumber;
        this.ibx = ibx;
        this.assetClassification = assetClassification;
        this.assetId = assetId;
    }
}
