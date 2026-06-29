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

package api.equinix.javasdk.fabric.model.json;

import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.fabric.model.Metric;
import api.equinix.javasdk.fabric.model.implementation.MetricDatapoint;
import api.equinix.javasdk.fabric.model.implementation.MetricResource;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Getter;

import java.util.List;

/**
 * <p>Read-only JSON model for a Fabric {@link Metric}. Implements the public {@link Metric}
 * interface directly, so no wrapper is required.</p>
 *
 * @author ianjones
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class MetricJson implements Metric {

    @Getter static TypeReference<Page<Metric, MetricJson>> pagedTypeRef = new TypeReference<>() {};

    @JsonProperty("type")
    private String type;

    @JsonProperty("name")
    private String name;

    @JsonProperty("unit")
    private String unit;

    @JsonProperty("interval")
    private String interval;

    @JsonProperty("resource")
    private MetricResource resource;

    @JsonProperty("summary")
    private String summary;

    @JsonProperty("datapoints")
    private List<MetricDatapoint> datapoints;
}
