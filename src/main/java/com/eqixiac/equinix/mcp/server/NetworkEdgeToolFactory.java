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

package com.eqixiac.equinix.mcp.server;

import com.eqixiac.equinix.networkedge.model.Device;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;

import static com.eqixiac.equinix.mcp.server.Schemas.integer;
import static com.eqixiac.equinix.mcp.server.Schemas.looseObject;
import static com.eqixiac.equinix.mcp.server.Schemas.object;
import static com.eqixiac.equinix.mcp.server.Schemas.props;

/**
 * The {@code ne_*} tools: cross-domain reach into Network Edge, summarized for an agent.
 */
final class NetworkEdgeToolFactory {

    private static final int DEFAULT_DEVICES = 25;
    private static final int MAX_DEVICES = 50;

    private NetworkEdgeToolFactory() {
    }

    static List<ToolRegistration> tools() {
        return List.of(listDevices());
    }

    private static ToolRegistration listDevices() {
        return ToolRegistration.builder()
                .name("ne_list_devices")
                .title("List Network Edge devices")
                .description("Lists the account's Network Edge virtual devices (routers, firewalls, "
                        + "SD-WAN appliances) with uuid, name, device type, metro, and lifecycle status — "
                        + "the inventory to reference when reasoning about existing virtual infrastructure.")
                .inputSchema(object(props(
                        "limit", integer("Maximum devices to return (default " + DEFAULT_DEVICES
                                + ", max " + MAX_DEVICES + ")."))))
                .outputSchema(looseObject("The devices (uuid, name, type, metro, status) with a count and a "
                        + "truncation note when capped."))
                .toolset(Toolset.NE)
                .handler(NetworkEdgeToolFactory::handleListDevices)
                .build();
    }

    private static ObjectNode handleListDevices(JsonNode args, ServerContext ctx) {
        int limit = Args.limit(args, "limit", DEFAULT_DEVICES, MAX_DEVICES);
        Iterable<Device> devices = ctx.networkEdge().devices().list();

        ObjectNode payload = ctx.objectMapper().createObjectNode();
        ArrayNode items = payload.putArray("devices");
        int shown = 0;
        boolean truncated = false;
        for (Device device : devices) {
            if (shown >= limit) {
                truncated = true;
                break;
            }
            ObjectNode d = items.addObject();
            d.put("uuid", device.getUuid());
            d.put("name", device.getName());
            d.put("type", device.getDeviceTypeCode());
            d.put("metro", String.valueOf(device.getMetroCode()));
            d.put("status", String.valueOf(device.getStatus()));
            shown++;
        }
        payload.put("count", shown);
        if (truncated) {
            payload.put("truncated", true);
            payload.put("truncation_note", "Showing the first " + shown
                    + " devices; raise 'limit' (max " + MAX_DEVICES + ") to see more.");
        }
        return payload;
    }
}
