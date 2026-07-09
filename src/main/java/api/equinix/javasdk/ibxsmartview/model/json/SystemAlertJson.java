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

import api.equinix.javasdk.ibxsmartview.enums.AlertStatus;
import api.equinix.javasdk.ibxsmartview.model.SystemAlert;
import api.equinix.javasdk.ibxsmartview.model.implementation.AlertAsset;
import api.equinix.javasdk.ibxsmartview.model.implementation.AlertConfiguration;
import api.equinix.javasdk.ibxsmartview.model.implementation.AlertProcessing;
import api.equinix.javasdk.ibxsmartview.model.implementation.ValueReadModel;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class SystemAlertJson implements SystemAlert {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("alertUid")
    private String alertUid;

    @JsonProperty("traceUid")
    private String traceUid;

    @JsonProperty("assetTagUid")
    private String assetTagUid;

    @JsonProperty("assetTagDisplayName")
    private String assetTagDisplayName;

    @JsonProperty("status")
    private AlertStatus status;

    @JsonProperty("value")
    private ValueReadModel value;

    @JsonProperty("asset")
    private AlertAsset asset;

    @JsonProperty("configuration")
    private AlertConfiguration configuration;

    @JsonProperty("activeProcessing")
    private AlertProcessing activeProcessing;

    @JsonProperty("inactiveProcessing")
    private AlertProcessing inactiveProcessing;
}
