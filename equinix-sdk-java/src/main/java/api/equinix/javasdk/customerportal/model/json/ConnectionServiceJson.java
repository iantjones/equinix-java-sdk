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

package api.equinix.javasdk.customerportal.model.json;

import api.equinix.javasdk.customerportal.model.ConnectionService;
import api.equinix.javasdk.customerportal.model.implementation.LookupMediaType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

/**
 * JSON model for a cross-connect {@code Connection_services_details} element.
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConnectionServiceJson implements ConnectionService {

    @JsonProperty("name")
    private String name;

    @JsonProperty("mediaTypes")
    private List<LookupMediaType> mediaTypes;
}
