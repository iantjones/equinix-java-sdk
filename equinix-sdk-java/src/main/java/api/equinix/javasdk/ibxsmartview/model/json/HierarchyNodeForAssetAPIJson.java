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

import api.equinix.javasdk.ibxsmartview.model.HierarchyNodeForAssetAPI;
import api.equinix.javasdk.ibxsmartview.model.implementation.Cages;
import api.equinix.javasdk.ibxsmartview.model.implementation.CircuitsMapWithCage;
import api.equinix.javasdk.ibxsmartview.model.implementation.Status;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Deserialization holder for the affected-assets response ({@code HierarchyNodeForAssetAPI}).
 */
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class HierarchyNodeForAssetAPIJson implements HierarchyNodeForAssetAPI {

    @JsonProperty("payLoad")
    private PayloadJson payLoad;

    @JsonProperty("status")
    private Status status;

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PayloadJson implements Payload {

        @JsonProperty("cages")
        private List<Cages> cages;

        @JsonProperty("circuits")
        private List<CircuitsMapWithCage> circuits;
    }
}
