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

package api.equinix.javasdk.fabric.model.implementation;

import api.equinix.javasdk.core.model.deserializers.LocalDateTimeDeserializer;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Operational information for a stream subscription (the Fabric v4
 * {@code StreamSubscriptionOperation} schema): delivery counters, last successful
 * delivery time, suspension time and delivery errors.
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class StreamSubscriptionOperation {

    @JsonProperty("eventsDeliveredCount")
    private Integer eventsDeliveredCount;

    @JsonProperty("metricsDeliveredCount")
    private Integer metricsDeliveredCount;

    @JsonProperty("alertsDeliveredCount")
    private Integer alertsDeliveredCount;

    @JsonProperty("lastSuccessfulDeliveryDateTime")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime lastSuccessfulDeliveryDateTime;

    @JsonProperty("suspendedDateTime")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime suspendedDateTime;

    @JsonProperty("errors")
    private List<StreamSubscriptionOperationError> errors;
}
