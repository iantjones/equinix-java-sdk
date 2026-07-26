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

package com.eqixiac.equinix.internetaccess.model.json.creators;

import com.eqixiac.equinix.internetaccess.enums.ServiceTypeV2;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

/**
 * Request body for creating an Equinix Internet Access (EIA) v2 service via
 * {@code POST /internetAccess/v2/services}.
 *
 * <p>The IP blocks and routing configuration are all nested in this single body: the
 * {@link #routingProtocol} is one of {@link DirectRoutingProtocolRequest},
 * {@link StaticRoutingProtocolRequest} or {@link BgpRoutingProtocolRequest}, each of which
 * carries the customer routes and (for direct) peerings.</p>
 *
 * <p>Instances are produced by the public
 * {@link com.eqixiac.equinix.internetaccess.client.InternetAccessServices#define() builder}.</p>
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ServiceRequest {

    @JsonProperty("name") private final String name;
    @JsonProperty("description") private final String description;
    @JsonProperty("type") private final ServiceTypeV2 type;
    @JsonProperty("tags") private final List<String> tags;
    @JsonProperty("connections") private final List<String> connections;
    @JsonProperty("routingProtocol") private final RoutingProtocolRequest routingProtocol;
    @JsonProperty("order") private final ServiceOrderRequest order;

    public ServiceRequest(InternetAccessServiceOperator.InternetAccessServiceBuilder builder) {
        this.name = builder.getName();
        this.description = builder.getDescription();
        this.type = builder.getType();
        this.tags = builder.getTags();
        this.connections = builder.getConnections();
        this.routingProtocol = builder.getRoutingProtocol();
        this.order = builder.getOrder();
    }
}
