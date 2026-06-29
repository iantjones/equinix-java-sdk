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

import api.equinix.javasdk.core.http.response.Pagination;
import api.equinix.javasdk.ibxsmartview.model.SubscriptionData;
import api.equinix.javasdk.ibxsmartview.model.implementation.AlarmMessageData;
import api.equinix.javasdk.ibxsmartview.model.implementation.AlertMessageData;
import api.equinix.javasdk.ibxsmartview.model.implementation.EnvironmentMessageData;
import api.equinix.javasdk.ibxsmartview.model.implementation.MeteredPowerMessageData;
import api.equinix.javasdk.ibxsmartview.model.implementation.PowerMessageData;
import api.equinix.javasdk.ibxsmartview.model.implementation.TagPointMessageData;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SubscriptionDataJson implements SubscriptionData {

    @JsonProperty("alarmMessageData")
    private List<AlarmMessageData> alarmMessageData;

    @JsonProperty("alertMessageData")
    private List<AlertMessageData> alertMessageData;

    @JsonProperty("environmentMessageData")
    private List<EnvironmentMessageData> environmentMessageData;

    @JsonProperty("meteredPowerMessageData")
    private List<MeteredPowerMessageData> meteredPowerMessageData;

    @JsonProperty("powerMessageData")
    private List<PowerMessageData> powerMessageData;

    @JsonProperty("tagPointMessageData")
    private List<TagPointMessageData> tagPointMessageData;

    @JsonProperty("pagination")
    private Pagination pagination;
}
