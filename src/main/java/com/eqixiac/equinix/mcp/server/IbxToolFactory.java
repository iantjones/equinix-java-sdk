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

import com.eqixiac.equinix.ibxsmartview.model.PowerEvent;
import com.eqixiac.equinix.ibxsmartview.model.SensorReading;
import com.eqixiac.equinix.ibxsmartview.model.implementation.Reading;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.Locale;

import static com.eqixiac.equinix.mcp.server.Args.optString;
import static com.eqixiac.equinix.mcp.server.Args.requireString;
import static com.eqixiac.equinix.mcp.server.Schemas.array;
import static com.eqixiac.equinix.mcp.server.Schemas.integer;
import static com.eqixiac.equinix.mcp.server.Schemas.looseObject;
import static com.eqixiac.equinix.mcp.server.Schemas.object;
import static com.eqixiac.equinix.mcp.server.Schemas.props;
import static com.eqixiac.equinix.mcp.server.Schemas.string;
import static com.eqixiac.equinix.mcp.server.Schemas.stringEnum;

/**
 * The {@code ibx_*} tools: cross-domain reach into IBX SmartView — environmental sensor
 * readings and power events — summarized for an agent.
 */
final class IbxToolFactory {

    private static final int DEFAULT_READINGS = 50;
    private static final int MAX_READINGS = 100;
    private static final int DEFAULT_EVENTS = 25;
    private static final int MAX_EVENTS = 50;

    private IbxToolFactory() {
    }

    static List<ToolRegistration> tools() {
        return List.of(getEnvironmentals(), listPowerEvents());
    }

    // ── ibx_get_environmentals ──────────────────────────────────────────────

    private static ToolRegistration getEnvironmentals() {
        return ToolRegistration.builder()
                .name("ibx_get_environmentals")
                .title("Get IBX environmental readings")
                .description("Reads live environmental sensor data (temperature and humidity per sensor and "
                        + "zone) for one Equinix IBX data center via IBX SmartView. Give the IBX code "
                        + "(e.g. 'DC11'); optionally narrow to a sensor type or zone.")
                .inputSchema(object(props(
                                "ibx", string("The IBX data-center code, e.g. 'DC11'."),
                                "type", stringEnum("Restrict to one sensor type.", "temperature", "humidity"),
                                "zone", string("Restrict to one zone id within the IBX."),
                                "limit", integer("Maximum readings to return (default " + DEFAULT_READINGS
                                        + ", max " + MAX_READINGS + ").")),
                        "ibx"))
                .outputSchema(looseObject("The sensor readings (sensor, zone, temperature, humidity) with a "
                        + "count and a truncation note when capped."))
                .toolset(Toolset.IBX)
                .handler(IbxToolFactory::handleGetEnvironmentals)
                .build();
    }

    private static ObjectNode handleGetEnvironmentals(JsonNode args, ServerContext ctx) {
        String ibx = requireString(args, "ibx").toUpperCase(Locale.ROOT);
        int limit = Args.limit(args, "limit", DEFAULT_READINGS, MAX_READINGS);
        String type = optString(args, "type").map(t -> t.toLowerCase(Locale.ROOT)).orElse(null);
        String zone = optString(args, "zone").orElse(null);

        Iterable<SensorReading> readings = (type == null && zone == null)
                ? ctx.ibxSmartView().environmentals().list(ibx)
                : ctx.ibxSmartView().environmentals().list(ibx, type, zone, 0, limit);

        ObjectNode payload = ctx.objectMapper().createObjectNode();
        payload.put("ibx", ibx);
        ArrayNode items = payload.putArray("readings");
        int shown = 0;
        boolean truncated = false;
        for (SensorReading reading : readings) {
            if (shown >= limit) {
                truncated = true;
                break;
            }
            ObjectNode r = items.addObject();
            r.put("sensor_id", reading.getSensorId());
            r.put("zone_id", reading.getZoneId());
            putReading(r, "temperature", reading.getTemperature());
            putReading(r, "humidity", reading.getHumidity());
            shown++;
        }
        payload.put("count", shown);
        if (truncated) {
            payload.put("truncated", true);
            payload.put("truncation_note", "Showing the first " + shown + " readings; raise 'limit' (max "
                    + MAX_READINGS + ") or narrow with 'type'/'zone'.");
        }
        return payload;
    }

    private static void putReading(ObjectNode parent, String field, Reading reading) {
        if (reading == null) {
            return;
        }
        ObjectNode node = parent.putObject(field);
        if (reading.getValue() != null) {
            node.put("value", reading.getValue());
        }
        node.put("unit", String.valueOf(reading.getUnit()));
    }

    // ── ibx_list_power_events ───────────────────────────────────────────────

    private static ToolRegistration listPowerEvents() {
        return ToolRegistration.builder()
                .name("ibx_list_power_events")
                .title("List IBX power events")
                .description("Lists power infrastructure events (alerts on power assets) for one or more "
                        + "Equinix IBX data centers via IBX SmartView, with status, category, event type, "
                        + "trigger/current values, and the affected asset. By default returns active "
                        + "events; pass statuses=[\"inactive\"] for history.")
                .inputSchema(object(props(
                                "ibxs", array("The IBX codes to check, e.g. [\"DC11\", \"LD8\"].",
                                        string("IBX code.")),
                                "statuses", array("Event statuses to include: active and/or inactive. "
                                        + "Default: active only.", stringEnum("Status.", "active", "inactive")),
                                "limit", integer("Maximum events to return (default " + DEFAULT_EVENTS
                                        + ", max " + MAX_EVENTS + ").")),
                        "ibxs"))
                .outputSchema(looseObject("The power events with status, category, values, and affected "
                        + "asset, plus a count and truncation note when capped."))
                .toolset(Toolset.IBX)
                .handler(IbxToolFactory::handleListPowerEvents)
                .build();
    }

    private static ObjectNode handleListPowerEvents(JsonNode args, ServerContext ctx) {
        List<String> ibxs = Args.stringList(args, "ibxs").stream()
                .map(code -> code.toUpperCase(Locale.ROOT))
                .toList();
        if (ibxs.isEmpty()) {
            throw new IllegalArgumentException("'ibxs' is required: an array with at least one IBX code, "
                    + "e.g. [\"DC11\"].");
        }
        int limit = Args.limit(args, "limit", DEFAULT_EVENTS, MAX_EVENTS);
        List<String> statuses = Args.stringList(args, "statuses").stream()
                .map(s -> s.toUpperCase(Locale.ROOT))
                .toList();
        if (statuses.isEmpty()) {
            statuses = List.of("ACTIVE");
        }

        Iterable<PowerEvent> events = ctx.ibxSmartView().powerEvents().search(ibxs, statuses, null, 0, limit);

        ObjectNode payload = ctx.objectMapper().createObjectNode();
        ArrayNode requested = payload.putArray("ibxs");
        ibxs.forEach(requested::add);
        ArrayNode items = payload.putArray("events");
        int shown = 0;
        boolean truncated = false;
        for (PowerEvent event : events) {
            if (shown >= limit) {
                truncated = true;
                break;
            }
            ObjectNode e = items.addObject();
            if (event.getId() != null) {
                e.put("id", event.getId());
            }
            e.put("alert_uid", event.getAlertUid());
            e.put("status", String.valueOf(event.getStatus()));
            e.put("category", event.getCategory());
            e.put("event_type", event.getEventType());
            e.put("condition_type", event.getConditionType());
            e.put("trigger_value", event.getTriggerValue());
            e.put("current_value", event.getCurrentValue());
            e.put("account_no", event.getAccountNo());
            if (event.getAsset() != null) {
                ObjectNode asset = e.putObject("asset");
                asset.put("ibx", event.getAsset().getIbx());
                asset.put("asset_uid", event.getAsset().getAssetUid());
            }
            shown++;
        }
        payload.put("count", shown);
        if (truncated) {
            payload.put("truncated", true);
            payload.put("truncation_note", "Showing the first " + shown + " events; raise 'limit' (max "
                    + MAX_EVENTS + ") or narrow 'ibxs'/'statuses'.");
        }
        return payload;
    }
}
