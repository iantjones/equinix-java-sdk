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

import com.eqixiac.equinix.ibxsmartview.model.TrendingEnvironmentData;
import com.eqixiac.equinix.ibxsmartview.model.implementation.DataValue;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class TrendingEnvironmentDataJson implements TrendingEnvironmentData {

    @JsonProperty("payLoad")
    private PayloadJson payLoad;

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PayloadJson implements Payload {

        @JsonProperty("ibx")
        private String ibx;

        @JsonProperty("accountNo")
        private String accountNo;

        @JsonProperty("interval")
        private String interval;

        @JsonProperty("datapoint")
        private String datapoint;

        @JsonProperty("uom")
        private String uom;

        @JsonProperty("series")
        private List<DataValue> series;
    }
}
