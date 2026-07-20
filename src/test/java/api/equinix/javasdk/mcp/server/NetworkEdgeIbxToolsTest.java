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

package api.equinix.javasdk.mcp.server;

import api.equinix.javasdk.IBXSmartView;
import api.equinix.javasdk.NetworkEdge;
import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.ibxsmartview.client.Environmentals;
import api.equinix.javasdk.ibxsmartview.client.PowerEvents;
import api.equinix.javasdk.ibxsmartview.enums.AlertStatus;
import api.equinix.javasdk.ibxsmartview.model.PowerEvent;
import api.equinix.javasdk.ibxsmartview.model.SensorReading;
import api.equinix.javasdk.ibxsmartview.model.implementation.Reading;
import api.equinix.javasdk.networkedge.client.Devices;
import api.equinix.javasdk.networkedge.enums.DeviceStatus;
import api.equinix.javasdk.networkedge.model.Device;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("ne_* and ibx_* tool handlers (mocked NetworkEdge + IBXSmartView)")
class NetworkEdgeIbxToolsTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private NetworkEdge networkEdge;
    private Devices devices;
    private IBXSmartView smartView;
    private Environmentals environmentals;
    private PowerEvents powerEvents;
    private ServerContext context;

    @BeforeEach
    void stubClients() {
        networkEdge = mock(NetworkEdge.class);
        devices = mock(Devices.class);
        when(networkEdge.devices()).thenReturn(devices);

        smartView = mock(IBXSmartView.class);
        environmentals = mock(Environmentals.class);
        powerEvents = mock(PowerEvents.class);
        when(smartView.environmentals()).thenReturn(environmentals);
        when(smartView.powerEvents()).thenReturn(powerEvents);

        context = ServerContext.builder()
                .networkEdge(networkEdge)
                .ibxSmartView(smartView)
                .environment(Map.of())
                .build();
    }

    private ObjectNode call(String name, String argsJson) throws Exception {
        ToolRegistration tool = EquinixMcpServer.catalog(EnumSet.of(Toolset.NE, Toolset.IBX)).stream()
                .filter(t -> t.getName().equals(name))
                .findFirst()
                .orElseThrow();
        return tool.getHandler().handle(MAPPER.readTree(argsJson), context);
    }

    // ── ne_list_devices ─────────────────────────────────────────────────────

    private static Device device(String uuid, String name, DeviceStatus status) {
        Device device = mock(Device.class);
        when(device.getUuid()).thenReturn(uuid);
        when(device.getName()).thenReturn(name);
        when(device.getDeviceTypeCode()).thenReturn("C8000V");
        when(device.getMetroCode()).thenReturn(MetroCode.DC);
        when(device.getStatus()).thenReturn(status);
        return device;
    }

    @Test
    @DisplayName("ne_list_devices summarizes uuid, name, type, metro, status")
    void listDevices() throws Exception {
        List<Device> fixture = List.of(
                device("uuid-1", "edge-router-1", DeviceStatus.PROVISIONED),
                device("uuid-2", "edge-fw-1", DeviceStatus.PROVISIONING));
        when(devices.list()).thenReturn(new PaginatedList<>(fixture, null, null, null, null));

        ObjectNode payload = call("ne_list_devices", "{}");

        assertEquals(2, payload.get("count").asInt());
        assertEquals("uuid-1", payload.get("devices").get(0).get("uuid").asText());
        assertEquals("edge-router-1", payload.get("devices").get(0).get("name").asText());
        assertEquals("C8000V", payload.get("devices").get(0).get("type").asText());
        assertEquals("DC", payload.get("devices").get(0).get("metro").asText());
        assertEquals("PROVISIONED", payload.get("devices").get(0).get("status").asText());
    }

    @Test
    @DisplayName("ne_list_devices caps the list and says so")
    void listDevicesTruncates() throws Exception {
        List<Device> many = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            many.add(device("uuid-" + i, "edge-" + i, DeviceStatus.PROVISIONED));
        }
        when(devices.list()).thenReturn(new PaginatedList<>(many, null, null, null, null));

        ObjectNode payload = call("ne_list_devices", "{\"limit\": 5}");

        assertEquals(5, payload.get("count").asInt());
        assertEquals(5, payload.get("devices").size());
        assertTrue(payload.get("truncated").asBoolean());
        assertTrue(payload.get("truncation_note").asText().contains("limit"));
    }

    // ── ibx_get_environmentals ──────────────────────────────────────────────

    private static SensorReading reading(String sensorId, String zone, double tempC, double humidityPct)
            throws Exception {
        SensorReading reading = mock(SensorReading.class);
        when(reading.getSensorId()).thenReturn(sensorId);
        when(reading.getZoneId()).thenReturn(zone);
        when(reading.getIbx()).thenReturn("DC11");
        when(reading.getTemperature()).thenReturn(MAPPER.readValue(
                "{\"value\":" + tempC + ",\"unit\":\"CELSIUS\"}", Reading.class));
        when(reading.getHumidity()).thenReturn(MAPPER.readValue(
                "{\"value\":" + humidityPct + ",\"unit\":\"PERCENT\"}", Reading.class));
        return reading;
    }

    @Test
    @DisplayName("ibx_get_environmentals returns per-sensor temperature and humidity")
    void environmentals() throws Exception {
        List<SensorReading> fixture = List.of(
                reading("s-1", "zone-a", 21.4, 45.0),
                reading("s-2", "zone-b", 23.9, 41.5));
        when(environmentals.list("DC11")).thenReturn(new PaginatedList<>(fixture, null, null, null, null));

        ObjectNode payload = call("ibx_get_environmentals", "{\"ibx\": \"dc11\"}");

        assertEquals("DC11", payload.get("ibx").asText(), "the IBX code is upper-cased");
        assertEquals(2, payload.get("count").asInt());
        assertEquals(21.4, payload.get("readings").get(0).get("temperature").get("value").asDouble());
        assertEquals("CELSIUS", payload.get("readings").get(0).get("temperature").get("unit").asText());
        assertEquals(45.0, payload.get("readings").get(0).get("humidity").get("value").asDouble());
        assertEquals("zone-a", payload.get("readings").get(0).get("zone_id").asText());
    }

    @Test
    @DisplayName("ibx_get_environmentals passes type/zone filters through to the client")
    void environmentalsFiltered() throws Exception {
        List<SensorReading> fixture = List.of(reading("s-1", "zone-a", 21.4, 45.0));
        when(environmentals.list("DC11", "temperature", "zone-a", 0, 10)).thenReturn(
                new PaginatedList<>(fixture, null, null, null, null));

        ObjectNode payload = call("ibx_get_environmentals",
                "{\"ibx\": \"DC11\", \"type\": \"temperature\", \"zone\": \"zone-a\", \"limit\": 10}");

        assertEquals(1, payload.get("count").asInt());
        verify(environmentals).list("DC11", "temperature", "zone-a", 0, 10);
    }

    @Test
    @DisplayName("ibx_get_environmentals requires the ibx argument")
    void environmentalsRequiresIbx() {
        assertThrows(IllegalArgumentException.class, () -> call("ibx_get_environmentals", "{}"));
    }

    // ── ibx_list_power_events ───────────────────────────────────────────────

    private static PowerEvent powerEvent(long id, AlertStatus status) {
        PowerEvent event = mock(PowerEvent.class);
        when(event.getId()).thenReturn(id);
        when(event.getAlertUid()).thenReturn("alert-" + id);
        when(event.getStatus()).thenReturn(status);
        when(event.getCategory()).thenReturn("Power");
        when(event.getEventType()).thenReturn("UPS");
        when(event.getConditionType()).thenReturn("On Battery");
        when(event.getTriggerValue()).thenReturn("0");
        when(event.getCurrentValue()).thenReturn("1");
        when(event.getAccountNo()).thenReturn("123456");
        return event;
    }

    @Test
    @DisplayName("ibx_list_power_events defaults to active events and summarizes them")
    void powerEventsDefaultActive() throws Exception {
        List<PowerEvent> fixture = List.of(powerEvent(7L, AlertStatus.ACTIVE));
        when(powerEvents.search(anyList(), anyList(), isNull(), anyInt(), anyInt())).thenReturn(
                new PaginatedList<>(fixture, null, null, null, null));

        ObjectNode payload = call("ibx_list_power_events", "{\"ibxs\": [\"dc11\"]}");

        assertEquals(1, payload.get("count").asInt());
        assertEquals("alert-7", payload.get("events").get(0).get("alert_uid").asText());
        assertEquals("ACTIVE", payload.get("events").get(0).get("status").asText());
        assertEquals("Power", payload.get("events").get(0).get("category").asText());
        verify(powerEvents).search(List.of("DC11"), List.of("ACTIVE"), null, 0, 25);
    }

    @Test
    @DisplayName("ibx_list_power_events requires at least one IBX")
    void powerEventsRequireIbx() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> call("ibx_list_power_events", "{}"));
        assertTrue(e.getMessage().contains("ibxs"), e.getMessage());
    }
}
