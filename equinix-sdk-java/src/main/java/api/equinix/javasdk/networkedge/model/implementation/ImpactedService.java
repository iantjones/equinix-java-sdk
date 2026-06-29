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

/**
 * <p>ImpactedService class. A service impacted by a downtime notification.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
public class ImpactedService {

    @JsonProperty("serviceName")
    private String serviceName;

    @JsonProperty("impact")
    private String impact;

    @JsonProperty("serviceStartDateTime")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime serviceStartDateTime;

    @JsonProperty("serviceEndDateTime")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime serviceEndDateTime;

    @JsonProperty("errorMessage")
    private String errorMessage;
}
