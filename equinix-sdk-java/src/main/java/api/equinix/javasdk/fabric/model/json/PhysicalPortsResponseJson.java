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

import api.equinix.javasdk.core.http.response.Pagination;
import api.equinix.javasdk.fabric.model.implementation.PhysicalPort;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

/**
 * Response body for the add-to-LAG (bulk physical port) endpoint
 * ({@code POST /fabric/v4/ports/{portId}/physicalPorts/bulk}, schema {@code AllPhysicalPortsResponse}):
 * the full list of physical ports backing the virtual port after the addition, with pagination.
 *
 * @author ianjones
 * @version $Id: $Id
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class PhysicalPortsResponseJson {

    @JsonProperty("pagination")
    private Pagination pagination;

    @JsonProperty("data")
    private List<PhysicalPort> data;
}
