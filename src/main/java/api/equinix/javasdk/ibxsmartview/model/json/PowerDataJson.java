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

import api.equinix.javasdk.ibxsmartview.enums.PowerLevelType;
import api.equinix.javasdk.ibxsmartview.model.PowerData;
import api.equinix.javasdk.ibxsmartview.model.implementation.ComparisonData;
import api.equinix.javasdk.ibxsmartview.model.implementation.Status;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class PowerDataJson implements PowerData {

    @JsonProperty("payLoad")
    private PayloadJson payLoad;

    @JsonProperty("status")
    private Status status;

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PayloadJson implements Payload {

        @JsonProperty("ibx")
        private String ibx;

        @JsonProperty("accountNo")
        private String accountNo;

        @JsonProperty("levelType")
        private PowerLevelType levelType;

        @JsonProperty("levelValue")
        private String levelValue;

        @JsonProperty("isAlarm")
        private String isAlarm;

        @JsonProperty("kva")
        private Double kva;

        @JsonProperty("amps")
        private Double amps;

        @JsonProperty("soldKva")
        private Double soldKva;

        @JsonProperty("cabinetRating")
        private Double cabinetRating;

        @JsonProperty("contractualKva")
        private Double contractualKva;

        @JsonProperty("percentageKva")
        private Double percentageKva;

        @JsonProperty("comparisonData")
        private ComparisonData comparisonData;

        @JsonProperty("peakKvaLastSevenDays")
        private Double peakKvaLastSevenDays;

        @JsonProperty("peakKvaLastSevenDaysPercentage")
        private Double peakKvaLastSevenDaysPercentage;

        @JsonProperty("peakKvaLastSevenDaysContractualKva")
        private Double peakKvaLastSevenDaysContractualKva;

        @JsonProperty("peakKvaLastSevenDaysTime")
        private Long peakKvaLastSevenDaysTime;

        @JsonProperty("soldAmps")
        private Integer soldAmps;

        @JsonProperty("primaryKva")
        private Double primaryKva;

        @JsonProperty("redundantKva")
        private Double redundantKva;

        @JsonProperty("kw")
        private String kw;

        @JsonProperty("powerFactor")
        private String powerFactor;

        @JsonProperty("readingTime")
        private String readingTime;

        @JsonProperty("lastUpdatedTime")
        private String lastUpdatedTime;

        @JsonProperty("customerName")
        private String customerName;
    }
}
