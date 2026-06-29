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

package api.equinix.javasdk.networkedge.model.implementation;

import api.equinix.javasdk.core.model.deserializers.LocalDateTimeDeserializer;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>InterfaceStats class. Interface throughput statistics returned by getInterfaceStatisticsByUuid.</p>
 *
 * @author ianjones
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
public class InterfaceStats {

    @JsonProperty("stats")
    private StatsDetail stats;

    @Getter
    public static class StatsDetail {

        @JsonProperty("startDateTime")
        @JsonDeserialize(using = LocalDateTimeDeserializer.class)
        private LocalDateTime startDateTime;

        @JsonProperty("endDateTime")
        @JsonDeserialize(using = LocalDateTimeDeserializer.class)
        private LocalDateTime endDateTime;

        @JsonProperty("unit")
        private String unit;

        @JsonProperty("inbound")
        private TrafficStats inbound;

        @JsonProperty("outbound")
        private TrafficStats outbound;
    }

    @Getter
    public static class TrafficStats {

        @JsonProperty("max")
        private Double max;

        @JsonProperty("mean")
        private Double mean;

        @JsonProperty("lastPolled")
        private Double lastPolled;

        @JsonProperty("metrics")
        private List<PolledMetric> metrics;
    }

    @Getter
    public static class PolledMetric {

        @JsonProperty("intervalDateTime")
        @JsonDeserialize(using = LocalDateTimeDeserializer.class)
        private LocalDateTime intervalDateTime;

        @JsonProperty("mean")
        private Double mean;
    }
}
