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
package com.eqixiac.equinix.design.access;

import com.eqixiac.equinix.Equinix;
import com.eqixiac.equinix.fabric.model.Port;
import com.eqixiac.equinix.fabric.model.implementation.filter.FilterPropertyList;
import com.eqixiac.equinix.fabric.model.implementation.filter.FilterType;
import com.eqixiac.equinix.networkedge.model.Device;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * The Fabric ports and Network Edge devices a customer already has, for {@link AccessPlanner} to use
 * before it plans a new order.
 *
 * @param ports   Fabric ports of the project
 * @param devices Network Edge devices of the account
 * @param notes   what was not read, and why
 */
public record AccessInventory(List<ExistingPort> ports, List<ExistingEdgeDevice> devices, List<String> notes) {

    /** Makes the lists immutable. */
    public AccessInventory {
        ports = ports == null ? List.of() : List.copyOf(ports);
        devices = devices == null ? List.of() : List.copyOf(devices);
        notes = notes == null ? List.of() : List.copyOf(notes);
    }

    /** @return an inventory with nothing in it */
    public static AccessInventory empty() {
        return new AccessInventory(List.of(), List.of(), List.of("existing ports and devices were not read"));
    }

    /**
     * @param ports   ports to offer the planner
     * @param devices devices to offer the planner
     * @return a fixed inventory, for tests and offline runs
     */
    public static AccessInventory of(List<ExistingPort> ports, List<ExistingEdgeDevice> devices) {
        return new AccessInventory(ports, devices, List.of());
    }

    /**
     * Reads the inventory with GET and search requests only. Ports are read for {@code projectId} and
     * devices for {@code accountNumber}; a scope that is not given is not read, because an API client
     * can see resources of other projects and accounts. A lookup that fails adds a note and does not throw.
     *
     * @param session       an open session
     * @param projectId     the project whose Fabric ports to read, or {@code null} to read no port
     * @param accountNumber the account whose Network Edge devices to read, or {@code null} to read no device
     * @return the inventory
     */
    public static AccessInventory read(Equinix session, String projectId, String accountNumber) {
        List<ExistingPort> ports = new ArrayList<>();
        List<ExistingEdgeDevice> devices = new ArrayList<>();
        List<String> notes = new ArrayList<>();
        if (projectId == null || projectId.isBlank()) {
            notes.add("no project id was given, so existing Fabric ports were not read");
        } else {
            try {
                FilterPropertyList filter = new FilterPropertyList(FilterType.AND)
                        .equals("/project/projectId", projectId);
                for (Port port : session.fabric().ports().search(filter)) {
                    if (projectId.equals(projectIdOf(port))) {
                        ports.add(port(port));
                    }
                }
            } catch (RuntimeException e) {
                notes.add("Fabric ports could not be read: " + e.getClass().getSimpleName());
            }
        }
        if (accountNumber == null || accountNumber.isBlank()) {
            notes.add("no account number was given, so existing Network Edge devices were not read");
        } else {
            try {
                for (Device device : session.networkEdge().devices().list()) {
                    if (accountNumber.equals(device.getAccountNumber())) {
                        devices.add(device(device));
                    }
                }
            } catch (RuntimeException e) {
                notes.add("Network Edge devices could not be read: " + e.getClass().getSimpleName());
            }
        }
        ports.sort(Comparator.comparing(ExistingPort::metro, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(ExistingPort::name, Comparator.nullsLast(Comparator.naturalOrder())));
        devices.sort(Comparator.comparing(ExistingEdgeDevice::metro, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(ExistingEdgeDevice::name, Comparator.nullsLast(Comparator.naturalOrder())));
        return new AccessInventory(ports, devices, notes);
    }

    /**
     * @param metro a metro code
     * @return the usable ports in that metro
     */
    public List<ExistingPort> portsIn(String metro) {
        return ports.stream().filter(p -> p.usable() && same(metro, p.metro())).toList();
    }

    /**
     * @param metro a metro code
     * @return the usable devices in that metro
     */
    public List<ExistingEdgeDevice> devicesIn(String metro) {
        return devices.stream().filter(d -> d.usable() && same(metro, d.metro())).toList();
    }

    /**
     * @param uuid a port uuid
     * @return the port, or {@code null} when the inventory does not hold it
     */
    public ExistingPort port(String uuid) {
        return ports.stream().filter(p -> p.uuid() != null && p.uuid().equalsIgnoreCase(uuid)).findFirst()
                .orElse(null);
    }

    /**
     * @param uuid a device uuid
     * @return the device, or {@code null} when the inventory does not hold it
     */
    public ExistingEdgeDevice device(String uuid) {
        return devices.stream().filter(d -> d.uuid() != null && d.uuid().equalsIgnoreCase(uuid)).findFirst()
                .orElse(null);
    }

    private static boolean same(String a, String b) {
        return a != null && b != null && a.trim().toUpperCase(Locale.ROOT).equals(b.trim().toUpperCase(Locale.ROOT));
    }

    private static String projectIdOf(Port port) {
        if (port.getProjectId() != null) {
            return port.getProjectId();
        }
        return port.getProject() == null ? null : port.getProject().getProjectId();
    }

    private static ExistingPort port(Port port) {
        String metro = port.getLocation() == null || port.getLocation().getMetroCode() == null ? null
                : port.getLocation().getMetroCode().name();
        String ibx = port.getLocation() != null && port.getLocation().getIbx() != null
                ? port.getLocation().getIbx() : port.getDemarcationPointIbx();
        String group = null;
        com.eqixiac.equinix.fabric.enums.RedundancyPriority priority = null;
        if (port.getRedundancy() != null) {
            group = port.getRedundancy().getGroup();
            priority = port.getRedundancy().getPriority();
        }
        return new ExistingPort(port.getUuid(), port.getName(), metro, ibx,
                port.getDevice() == null ? null : port.getDevice().getName(), group, priority, port.getBandwidth(),
                port.getAvailableBandwidth(), port.getState() == null ? null : port.getState().name());
    }

    private static ExistingEdgeDevice device(Device device) {
        return new ExistingEdgeDevice(device.getUuid(), device.getName(),
                device.getMetroCode() == null ? null : device.getMetroCode().name(), device.getDeviceTypeCode(),
                device.getStatus() == null ? null : device.getStatus().name(),
                device.getRedundancyType() == null ? null : device.getRedundancyType().name(),
                device.getRedundantUuid());
    }
}
