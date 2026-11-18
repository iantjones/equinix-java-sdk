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

import com.eqixiac.equinix.core.model.MetroId;
import com.eqixiac.equinix.design.connectivity.ProductAvailability;
import com.eqixiac.equinix.design.cost.RateCardCostModel;
import com.eqixiac.equinix.design.optimizer.enums.SiteRole;
import com.eqixiac.equinix.design.optimizer.model.OptimizationResult;
import com.eqixiac.equinix.design.placement.PlacementPlan;
import com.eqixiac.equinix.design.spec.DesignSpec;
import com.eqixiac.equinix.design.spec.SiteIntent;
import com.eqixiac.equinix.design.spi.MetroPortfolio;
import com.eqixiac.equinix.fabric.enums.RedundancyPriority;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * Decides how each metro of a design is entered, using what the project already has before it plans an
 * order. The rules, applied to each metro in this order:
 * <ol>
 *   <li>Ports the caller named with {@link #usePorts} are used. A uuid the inventory does not hold is
 *       assumed to exist.</li>
 *   <li>Devices the caller named with {@link #useDevices} are used, in the same way.</li>
 *   <li>Usable ports of the inventory in the metro, with room for the access connection, are used. A
 *       pair in one redundancy group is preferred, then two ports on different Fabric devices, then
 *       one port with an order for the second.</li>
 *   <li>A metro with a site of role {@code DATA_CENTER} and no usable port gets an order for two ports
 *       in one redundancy group, each with a cross connect.</li>
 *   <li>Usable Network Edge devices of the inventory in the metro are used: a device and its redundant
 *       partner, then any two devices, then one device with an order for the second.</li>
 *   <li>A metro where Network Edge is offered gets an order for a redundant device pair.</li>
 *   <li>Otherwise the metro has its Cloud Router only.</li>
 * </ol>
 *
 * <p>Each port or device gets one virtual connection to the Cloud Router of its metro. The connection
 * is sized as the cloud bandwidth the placement lands in the metro, rounded up to a Fabric tier, or
 * 1,000 Mbps when the placement sizes nothing there.</p>
 *
 * <p>The planner reads nothing and orders nothing.</p>
 */
public final class AccessPlanner {

    /** Bandwidth of an access connection in a metro where the placement sizes no cloud traffic, in Mbps. */
    public static final int DEFAULT_LINK_MBPS = 1_000;

    private final DesignSpec spec;
    private MetroPortfolio portfolio;
    private PlacementPlan placement;
    private ProductAvailability availability = ProductAvailability.unknown();
    private AccessInventory inventory = AccessInventory.empty();
    private final Map<String, List<String>> portUuids = new LinkedHashMap<>();
    private final Map<String, List<String>> deviceUuids = new LinkedHashMap<>();
    private final Set<String> internetAccess = new TreeSet<>();

    private AccessPlanner(DesignSpec spec) {
        this.spec = spec;
    }

    /**
     * @param spec the design
     * @return a planner to configure and {@link #plan()}
     */
    public static AccessPlanner forSpec(DesignSpec spec) {
        if (spec == null) {
            throw new IllegalArgumentException("forSpec needs a spec");
        }
        return new AccessPlanner(spec);
    }

    /**
     * @param result the optimizer's result for this spec; its recommendation gives the metros and the placement
     * @return this planner
     */
    public AccessPlanner optimization(OptimizationResult result) {
        if (result != null && result.getRecommended() != null) {
            this.portfolio = result.getRecommended().portfolio();
            this.placement = result.getPlacement();
        }
        return this;
    }

    /**
     * @param metros    the metros of the design
     * @param placement where the workloads run on them, or {@code null} when unknown
     * @return this planner
     */
    public AccessPlanner portfolio(MetroPortfolio metros, PlacementPlan placement) {
        this.portfolio = metros;
        this.placement = placement;
        return this;
    }

    /**
     * @param productAvailability where Network Edge is offered; {@code null} means unknown
     * @return this planner
     */
    public AccessPlanner availability(ProductAvailability productAvailability) {
        this.availability = productAvailability == null ? ProductAvailability.unknown() : productAvailability;
        return this;
    }

    /**
     * @param existing the ports and devices the project already has; {@code null} means none were read
     * @return this planner
     */
    public AccessPlanner inventory(AccessInventory existing) {
        this.inventory = existing == null ? AccessInventory.empty() : existing;
        return this;
    }

    /**
     * Names the ports to use in a metro. The first is the primary.
     *
     * @param metro a metro code of the design
     * @param uuids one or more port uuids
     * @return this planner
     */
    public AccessPlanner usePorts(String metro, String... uuids) {
        portUuids.put(code(metro), List.of(uuids));
        return this;
    }

    /**
     * Names the Network Edge devices to use in a metro. The first is the primary.
     *
     * @param metro a metro code of the design
     * @param uuids one or more device uuids
     * @return this planner
     */
    public AccessPlanner useDevices(String metro, String... uuids) {
        deviceUuids.put(code(metro), List.of(uuids));
        return this;
    }

    /**
     * @param metros metros where the design uses Internet Access; each gets an Internet Access item to order
     * @return this planner
     */
    public AccessPlanner internetAccess(Collection<String> metros) {
        internetAccess.clear();
        if (metros != null) {
            metros.forEach(m -> internetAccess.add(code(m)));
        }
        return this;
    }

    /**
     * @return the access of every metro
     * @throws IllegalStateException when no metros were given
     */
    public AccessPlan plan() {
        if (portfolio == null) {
            throw new IllegalStateException("plan() needs an optimization result with a recommendation, or a portfolio");
        }
        List<MetroAccess> metros = new ArrayList<>();
        Set<String> known = new TreeSet<>();
        for (MetroId metro : portfolio.metros()) {
            known.add(metro.code());
            metros.add(access(metro));
        }
        List<String> notes = new ArrayList<>(inventory.notes());
        for (String named : union(portUuids.keySet(), deviceUuids.keySet())) {
            if (!known.contains(named)) {
                notes.add("ports or devices were named for " + named + ", which is not a metro of the design");
            }
        }
        return new AccessPlan(spec.fingerprint(), metros, notes);
    }

    private MetroAccess access(MetroId metroId) {
        String metro = metroId.code();
        Sizing sizing = sizing(metroId);
        List<PlannedAccessItem> items = new ArrayList<>();
        List<PlannedAccessLink> links = new ArrayList<>();
        List<String> findings = new ArrayList<>();
        AccessMode mode;
        String reason;

        Optional<SiteIntent> cage = spec.sites().stream()
                .filter(s -> s.role() == SiteRole.DATA_CENTER && metro.equalsIgnoreCase(s.metro())).findFirst();
        List<ExistingPort> ports = inventory.portsIn(metro).stream().filter(p -> p.hasRoomFor(sizing.mbps)).toList();
        List<ExistingEdgeDevice> devices = inventory.devicesIn(metro);

        if (portUuids.containsKey(metro)) {
            mode = AccessMode.PHYSICAL_PORTS;
            reason = "the ports were named for this metro";
            List<String> uuids = portUuids.get(metro);
            for (int i = 0; i < uuids.size(); i++) {
                ExistingPort found = inventory.port(uuids.get(i));
                items.add(found != null ? existing(found, priority(i, uuids.size()), "named for this metro")
                        : new PlannedAccessItem(metro + "-port-" + (i + 1), AccessKind.FABRIC_PORT, metro,
                        AccessSource.ASSUMED, uuids.get(i), null, null, priority(i, uuids.size()), null,
                        "named for this metro; not in the inventory, so the plan assumes it exists"));
            }
        } else if (deviceUuids.containsKey(metro)) {
            mode = AccessMode.NETWORK_EDGE;
            reason = "the Network Edge devices were named for this metro";
            List<String> uuids = deviceUuids.get(metro);
            for (int i = 0; i < uuids.size(); i++) {
                ExistingEdgeDevice found = inventory.device(uuids.get(i));
                items.add(found != null ? existing(found, priority(i, uuids.size()), "named for this metro")
                        : new PlannedAccessItem(metro + "-ne-" + (i + 1), AccessKind.NETWORK_EDGE_DEVICE, metro,
                        AccessSource.ASSUMED, uuids.get(i), null, null, priority(i, uuids.size()), null,
                        "named for this metro; not in the inventory, so the plan assumes it exists"));
            }
        } else if (!ports.isEmpty()) {
            mode = AccessMode.PHYSICAL_PORTS;
            reason = "the project has " + ports.size() + " usable port(s) in this metro";
            List<ExistingPort> pair = portPair(ports);
            String why = pair.size() < 2 ? "the only usable port with room for " + sizing.mbps + " Mbps"
                    : samePairGroup(pair) ? "a redundant pair in group " + pair.get(0).redundancyGroup()
                    : differentDevices(pair) ? "two ports on different Fabric devices"
                    : "two ports; the API does not show that they are on different Fabric devices";
            for (int i = 0; i < pair.size(); i++) {
                items.add(existing(pair.get(i), priority(i, 2), why));
            }
            if (pair.size() < 2) {
                order(items, metro, "secondary", RedundancyPriority.SECONDARY, sizing.mbps, cage.isPresent());
                findings.add("one usable port exists; " + metro + "-port-secondary is an order to place, and until "
                        + "it is in service the metro is entered through one port");
            } else if (!samePairGroup(pair) && !differentDevices(pair)) {
                findings.add("the two ports are not shown to be on different Fabric devices; check before relying on "
                        + "them as a redundant pair");
            }
        } else if (cage.isPresent()) {
            mode = AccessMode.PHYSICAL_PORTS;
            reason = "the site '" + cage.get().label() + "' is a data center in this metro and the inventory has "
                    + "no usable port here";
            order(items, metro, "primary", RedundancyPriority.PRIMARY, sizing.mbps, true);
            order(items, metro, "secondary", RedundancyPriority.SECONDARY, sizing.mbps, true);
        } else if (!devices.isEmpty()) {
            mode = AccessMode.NETWORK_EDGE;
            reason = "the account has " + devices.size() + " usable Network Edge device(s) in this metro";
            List<ExistingEdgeDevice> pair = devicePair(devices);
            for (int i = 0; i < pair.size(); i++) {
                items.add(existing(pair.get(i), priority(i, 2), pair.size() == 2
                        && pair.get(1).uuid().equalsIgnoreCase(String.valueOf(pair.get(0).redundantUuid()))
                        ? "a device and its redundant partner" : "usable device in this metro"));
            }
            if (pair.size() < 2) {
                items.add(orderDevice(metro, "secondary", RedundancyPriority.SECONDARY));
                findings.add("one usable device exists; " + metro + "-ne-secondary is an order to place, and until it "
                        + "is in service the metro is entered through one device");
            }
        } else if (availability.networkEdgeMetros().contains(metro)) {
            mode = AccessMode.NETWORK_EDGE;
            reason = "no data center site here and Network Edge is offered";
            items.add(orderDevice(metro, "primary", RedundancyPriority.PRIMARY));
            items.add(orderDevice(metro, "secondary", RedundancyPriority.SECONDARY));
        } else {
            mode = AccessMode.CLOUD_ROUTER_ONLY;
            reason = availability.networkEdgeMetros().isEmpty()
                    ? "no data center site here and Network Edge availability is unknown"
                    : "no data center site here and Network Edge is not offered";
        }

        List<PlannedAccessItem> endpoints = items.stream()
                .filter(i -> i.kind() == AccessKind.FABRIC_PORT || i.kind() == AccessKind.NETWORK_EDGE_DEVICE).toList();
        if (endpoints.size() == 1) {
            findings.add("the metro is entered through one " + PlannedAccessItem.label(endpoints.get(0).kind())
                    + ": its loss cuts the metro off from the sites that use it");
        }
        Set<String> ibxs = new TreeSet<>();
        endpoints.stream().filter(i -> i.kind() == AccessKind.FABRIC_PORT && i.ibx() != null)
                .forEach(i -> ibxs.add(i.ibx()));
        if (endpoints.size() > 1 && ibxs.size() == 1
                && endpoints.stream().allMatch(i -> i.kind() == AccessKind.FABRIC_PORT && i.ibx() != null)) {
            findings.add("every port is in IBX " + ibxs.iterator().next() + ": the IBX is a shared point of failure");
        }
        for (int i = 0; i < endpoints.size(); i++) {
            PlannedAccessItem item = endpoints.get(i);
            if (item.speedMbps() != null && item.speedMbps() < sizing.mbps) {
                findings.add(item.name() + " is a " + item.speedMbps() + " Mbps port, below the " + sizing.mbps
                        + " Mbps the access connection needs");
            }
            links.add(new PlannedAccessLink(metro + "-access-" + (endpoints.size() == 1 ? "1"
                    : item.priority() == RedundancyPriority.SECONDARY ? "secondary" : i == 0 ? "primary"
                    : String.valueOf(i + 1)), metro, item.name(), item.kind(), item.existingUuid(), sizing.mbps,
                    item.priority(), sizing.basis));
        }
        if (internetAccess.contains(metro)) {
            items.add(new PlannedAccessItem(metro + "-internet-access", AccessKind.INTERNET_ACCESS, metro,
                    AccessSource.ORDER, null, null, null, null, null,
                    "general internet traffic and peers with no presence in the design leave here"));
        }
        return new MetroAccess(metro, mode, reason, items, links, findings);
    }

    private record Sizing(int mbps, String basis) {
    }

    private Sizing sizing(MetroId metro) {
        int total = 0;
        if (placement != null) {
            for (PlacementPlan.CloudLeg leg : placement.legsIn(metro)) {
                total += Math.max(0, leg.sizedMbps());
            }
        }
        if (total <= 0) {
            return new Sizing(DEFAULT_LINK_MBPS, "default " + DEFAULT_LINK_MBPS
                    + " Mbps: the placement sizes no cloud traffic in this metro");
        }
        int tier = RateCardCostModel.tierFor(total);
        int mbps = tier > 0 ? tier : total;
        return new Sizing(mbps, "the " + total + " Mbps of cloud traffic the placement lands here"
                + (mbps == total ? "" : ", rounded up to the " + mbps + " Mbps Fabric tier"));
    }

    private static List<ExistingPort> portPair(List<ExistingPort> ports) {
        List<ExistingPort> sorted = new ArrayList<>(ports);
        sorted.sort(Comparator.comparing((ExistingPort p) -> p.name() == null ? "" : p.name())
                .thenComparing(p -> p.uuid() == null ? "" : p.uuid()));
        for (ExistingPort a : sorted) {
            if (a.redundancyGroup() == null || a.priority() != RedundancyPriority.PRIMARY) {
                continue;
            }
            for (ExistingPort b : sorted) {
                if (b != a && a.redundancyGroup().equals(b.redundancyGroup())
                        && b.priority() == RedundancyPriority.SECONDARY) {
                    return List.of(a, b);
                }
            }
        }
        for (ExistingPort a : sorted) {
            for (ExistingPort b : sorted) {
                if (b != a && differentDevices(List.of(a, b))) {
                    return List.of(a, b);
                }
            }
        }
        return sorted.size() > 1 ? List.of(sorted.get(0), sorted.get(1)) : List.of(sorted.get(0));
    }

    private static boolean samePairGroup(List<ExistingPort> pair) {
        return pair.size() == 2 && pair.get(0).redundancyGroup() != null
                && pair.get(0).redundancyGroup().equals(pair.get(1).redundancyGroup())
                && pair.get(0).priority() != pair.get(1).priority();
    }

    private static boolean differentDevices(List<ExistingPort> pair) {
        return pair.size() == 2 && pair.get(0).fabricDevice() != null && pair.get(1).fabricDevice() != null
                && !pair.get(0).fabricDevice().equals(pair.get(1).fabricDevice());
    }

    private static List<ExistingEdgeDevice> devicePair(List<ExistingEdgeDevice> devices) {
        List<ExistingEdgeDevice> sorted = new ArrayList<>(devices);
        sorted.sort(Comparator.comparing((ExistingEdgeDevice d) -> "SECONDARY".equals(d.redundancy()))
                .thenComparing(d -> d.name() == null ? "" : d.name())
                .thenComparing(d -> d.uuid() == null ? "" : d.uuid()));
        for (ExistingEdgeDevice a : sorted) {
            for (ExistingEdgeDevice b : sorted) {
                if (b != a && a.redundantUuid() != null && a.redundantUuid().equalsIgnoreCase(b.uuid())) {
                    return List.of(a, b);
                }
            }
        }
        return sorted.size() > 1 ? List.of(sorted.get(0), sorted.get(1)) : List.of(sorted.get(0));
    }

    private static PlannedAccessItem existing(ExistingPort port, RedundancyPriority priority, String why) {
        return new PlannedAccessItem(port.name() == null ? port.uuid() : port.name(), AccessKind.FABRIC_PORT,
                port.metro(), AccessSource.EXISTING, port.uuid(), port.ibx(), port.bandwidthMbps(), priority, null,
                why + (port.fabricDevice() == null ? "" : "; Fabric device " + port.fabricDevice())
                        + "; its cross connect is in place");
    }

    private static PlannedAccessItem existing(ExistingEdgeDevice device, RedundancyPriority priority, String why) {
        return new PlannedAccessItem(device.name() == null ? device.uuid() : device.name(),
                AccessKind.NETWORK_EDGE_DEVICE, device.metro(), AccessSource.EXISTING, device.uuid(), null, null,
                priority, null, why + (device.typeCode() == null ? "" : "; type " + device.typeCode()));
    }

    private static void order(List<PlannedAccessItem> items, String metro, String which,
                              RedundancyPriority priority, int linkMbps, boolean crossConnect) {
        int speed = linkMbps > 10_000 ? 100_000 : 10_000;
        String port = metro + "-port-" + which;
        items.add(new PlannedAccessItem(port, AccessKind.FABRIC_PORT, metro, AccessSource.ORDER, null, null, speed,
                priority, null, "order a " + (speed / 1000) + "G port in one redundancy group with priority "
                + priority.name()));
        if (crossConnect) {
            items.add(new PlannedAccessItem(metro + "-xc-" + which, AccessKind.CROSS_CONNECT, metro,
                    AccessSource.ORDER, null, null, null, priority, port, "from the cage to " + port
                    + (priority == RedundancyPriority.SECONDARY ? " on a diverse path" : "")));
        }
    }

    private static PlannedAccessItem orderDevice(String metro, String which, RedundancyPriority priority) {
        return new PlannedAccessItem(metro + "-ne-" + which, AccessKind.NETWORK_EDGE_DEVICE, metro,
                AccessSource.ORDER, null, null, null, priority, null,
                "create a redundant device pair (SD-WAN or router image); this is the " + priority.name() + " device");
    }

    private static RedundancyPriority priority(int index, int count) {
        return count < 2 ? null : index == 0 ? RedundancyPriority.PRIMARY : RedundancyPriority.SECONDARY;
    }

    private static Set<String> union(Set<String> a, Set<String> b) {
        Set<String> all = new TreeSet<>(a);
        all.addAll(b);
        return all;
    }

    private static String code(String metro) {
        return metro == null ? "" : metro.trim().toUpperCase(Locale.ROOT);
    }
}
