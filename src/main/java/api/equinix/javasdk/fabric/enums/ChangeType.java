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

package api.equinix.javasdk.fabric.enums;

import api.equinix.javasdk.core.model.APIParam;
import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * The kind of lifecycle change recorded against a Fabric resource (the {@code change.type} field).
 *
 * <p>Fabric reports change types across many resource families (connections, networks, ports,
 * routers, route filters/aggregations and their rules, routing protocols, etc.). The full set is
 * enumerated here, and unknown/newly-introduced values deserialize to {@link #UNKNOWN} rather than
 * failing the whole response — the API can add change types without breaking existing SDK clients.</p>
 *
 * @author ianjones
 */
public enum ChangeType implements APIParam {
    CONNECTION_CREATION,
    CONNECTION_UPDATE,
    CONNECTION_UPDATE_REQUEST,
    CONNECTION_DELETION,
    NETWORK_CREATION,
    NETWORK_UPDATE,
    NETWORK_DELETION,
    PORT_CREATION,
    PORT_UPDATE,
    PORT_DELETION,
    ROUTER_UPDATE,
    PROFILE_UPDATE,
    SERVICE_PROFILE_VISIBILITY_UPDATE,
    COMPANY_PROFILE_CREATION,
    ASSET_CREATION,
    ROUTING_PROTOCOL_CREATION,
    ROUTING_PROTOCOL_UPDATE,
    ROUTING_PROTOCOL_DELETION,
    PREFIX_FILTER_CREATION,
    PREFIX_FILTER_UPDATE,
    PREFIX_FILTER_DELETION,
    PREFIX_FILTER_RULE_CREATION,
    PREFIX_FILTER_RULE_UPDATE,
    PREFIX_FILTER_RULE_DELETION,
    BGP_PREFIX_FILTER_RULE_CREATION,
    PREFIX_AGGREGATION_CREATION,
    PREFIX_AGGREGATION_UPDATE,
    PREFIX_AGGREGATION_DELETION,
    PREFIX_AGGREGATION_RULE_CREATION,
    PREFIX_AGGREGATION_RULE_UPDATE,
    PREFIX_AGGREGATION_RULE_DELETION,
    BGP_PREFIX_AGGREGATION_RULE_CREATION,
    ROUTE_TABLE_ENTRY_UPDATE,
    ADVERTISED_ROUTE_ENTRY_UPDATE,
    RECEIVED_ROUTE_ENTRY_UPDATE,
    BGP_SESSION_STATUS_UPDATE,
    /** Fallback for any change type not (yet) modelled by the SDK. */
    UNKNOWN;

    /**
     * Deserializes a change type leniently: an unrecognized value maps to {@link #UNKNOWN} instead of
     * failing the enclosing response. (Fabric introduces new change types over time and uses a single
     * {@code change.type} field across many resource kinds.)
     *
     * @param value the raw API value
     * @return the matching constant, or {@link #UNKNOWN}
     */
    @JsonCreator
    public static ChangeType fromString(String value) {
        if (value == null) {
            return UNKNOWN;
        }
        try {
            return ChangeType.valueOf(value);
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
}
