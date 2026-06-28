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
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * A streaming alert message describing a notable event for an IBX asset, including the
 * event type, associated asset, tag and threshold details.
 */
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Alert {

    @JsonProperty("streamId")
    private String streamId;

    @JsonProperty("ibx")
    private String ibx;

    @JsonProperty("region")
    private String region;

    @JsonProperty("id")
    private String id;

    @JsonProperty("type")
    private String type;

    @JsonProperty("typeId")
    private String typeId;

    @JsonProperty("conditional")
    private String conditional;

    @JsonProperty("eventType")
    private String eventType;

    @JsonProperty("heartbeat")
    private Boolean heartbeat;

    @JsonProperty("asset")
    private AlertAssetDetails asset;

    @JsonProperty("tag")
    private AlertTagDetails tag;

    @JsonProperty("threshold")
    private AlertThresholdDetails threshold;

    @JsonProperty("triggeredTime")
    private String triggeredTime;

    @JsonProperty("dataQuality")
    private String dataQuality;
}
