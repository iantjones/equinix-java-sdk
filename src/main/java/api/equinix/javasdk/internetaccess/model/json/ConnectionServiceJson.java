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

package api.equinix.javasdk.internetaccess.model.json;

import api.equinix.javasdk.internetaccess.model.ConnectionService;
import api.equinix.javasdk.internetaccess.model.implementation.ConnectionMediaType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

/**
 * Read-only JSON model for a {@link ConnectionService} returned by the Equinix Internet Access
 * (EIA) v1 product-availability lookup {@code GET /internetAccess/v1/connectionServices}.
 * Implements {@link ConnectionService} directly, so no wrapper is required.
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConnectionServiceJson implements ConnectionService {

    @JsonProperty("name")
    private String name;

    @JsonProperty("mediaTypes")
    private List<ConnectionMediaType> mediaTypes;
}
