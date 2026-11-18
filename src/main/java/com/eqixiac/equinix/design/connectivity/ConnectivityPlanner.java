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
package com.eqixiac.equinix.design.connectivity;

import com.eqixiac.equinix.core.model.MetroId;
import com.eqixiac.equinix.design.access.AccessInventory;
import com.eqixiac.equinix.design.access.AccessPlan;
import com.eqixiac.equinix.design.access.AccessPlanner;
import com.eqixiac.equinix.design.access.MetroAccess;
import com.eqixiac.equinix.design.access.PlannedAccessItem;
import com.eqixiac.equinix.design.access.PlannedAccessLink;
import com.eqixiac.equinix.design.connectivity.ConnectivityPlan.Access;
import com.eqixiac.equinix.design.connectivity.ConnectivityPlan.AccessDecision;
import com.eqixiac.equinix.design.connectivity.ConnectivityPlan.Reach;
import com.eqixiac.equinix.design.connectivity.ConnectivityPlan.ReachDecision;
import com.eqixiac.equinix.design.optimizer.model.OptimizationResult;
import com.eqixiac.equinix.design.peering.model.PeeringIntelligenceResult;
import com.eqixiac.equinix.design.peering.model.UnifiedConnectivityView;
import com.eqixiac.equinix.design.placement.PlacementPlan;
import com.eqixiac.equinix.design.spec.CloudIntent;
import com.eqixiac.equinix.design.spec.DesignSpec;
import com.eqixiac.equinix.design.spec.PeerIntent;
import com.eqixiac.equinix.design.spi.MetroPortfolio;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Applies fixed rules to decide how each destination of a design is reached and how each metro is entered.
 *
 * <p>Reach rules:</p>
 * <ol>
 *   <li>A cloud is reached over a private Fabric connection from the Cloud Router in each metro where the
 *       optimizer's placement connects it.</li>
 *   <li>A peer is reached by public peering at an Internet Exchange (IX) when Peering Intelligence finds
 *       it at an IX in a metro of the design; otherwise over a private Fabric connection when it publishes
 *       a service profile there; otherwise over Internet Access. Without a Peering Intelligence result the
 *       peer is undecided.</li>
 *   <li>General internet traffic leaves through Internet Access in each metro of the design that offers
 *       it.</li>
 * </ol>
 *
 * <p>Access is decided by {@link AccessPlanner}, whose rules use a port or Network Edge device the
 * project already has before they plan an order. Give it the existing resources with {@link #inventory},
 * or name them with {@link #usePorts} and {@link #useDevices}.</p>
 *
 * <p>The planner reads nothing and orders nothing.</p>
 */
public final class ConnectivityPlanner {

    private final DesignSpec spec;
    private OptimizationResult optimization;
    private PeeringIntelligenceResult peering;
    private ProductAvailability availability = ProductAvailability.unknown();
    private AccessInventory inventory = AccessInventory.empty();
    private final java.util.Map<String, List<String>> portUuids = new java.util.LinkedHashMap<>();
    private final java.util.Map<String, List<String>> deviceUuids = new java.util.LinkedHashMap<>();

    private ConnectivityPlanner(DesignSpec spec) {
        this.spec = spec;
    }

    /**
     * @param spec the design
     * @return a planner to configure and {@link #plan()}
     */
    public static ConnectivityPlanner forSpec(DesignSpec spec) {
        if (spec == null) {
            throw new IllegalArgumentException("forSpec needs a spec");
        }
        return new ConnectivityPlanner(spec);
    }

    /**
     * @param result the optimizer's result for this spec; required, and it must have a recommendation
     * @return this planner
     */
    public ConnectivityPlanner optimization(OptimizationResult result) {
        if (result != null && result.getSpecFingerprint() != null
                && !result.getSpecFingerprint().equals(spec.fingerprint())) {
            throw new IllegalArgumentException("the optimization was computed for another spec");
        }
        this.optimization = result;
        return this;
    }

    /**
     * @param result a Peering Intelligence result for the spec's peers, or {@code null} to leave peers undecided
     * @return this planner
     */
    public ConnectivityPlanner peering(PeeringIntelligenceResult result) {
        this.peering = result;
        return this;
    }

    /**
     * @param productAvailability where Network Edge and Internet Access are offered; {@code null} means unknown
     * @return this planner
     */
    public ConnectivityPlanner availability(ProductAvailability productAvailability) {
        this.availability = productAvailability == null ? ProductAvailability.unknown() : productAvailability;
        return this;
    }

    /**
     * @param existing the ports and devices the project already has, used before a new order is planned;
     *                 {@code null} means none were read
     * @return this planner
     */
    public ConnectivityPlanner inventory(AccessInventory existing) {
        this.inventory = existing == null ? AccessInventory.empty() : existing;
        return this;
    }

    /**
     * Names the Fabric ports to use in a metro. The first is the primary. A uuid the inventory does not
     * hold is assumed to exist.
     *
     * @param metro a metro code of the design
     * @param uuids one or more port uuids
     * @return this planner
     */
    public ConnectivityPlanner usePorts(String metro, String... uuids) {
        portUuids.put(metro, List.of(uuids));
        return this;
    }

    /**
     * Names the Network Edge devices to use in a metro. The first is the primary.
     *
     * @param metro a metro code of the design
     * @param uuids one or more device uuids
     * @return this planner
     */
    public ConnectivityPlanner useDevices(String metro, String... uuids) {
        deviceUuids.put(metro, List.of(uuids));
        return this;
    }

    /**
     * @return the decisions
     * @throws IllegalStateException when no optimization with a recommendation was given
     */
    public ConnectivityPlan plan() {
        if (optimization == null || optimization.getRecommended() == null) {
            throw new IllegalStateException("plan() needs an optimization result with a recommended design");
        }
        MetroPortfolio portfolio = optimization.getRecommended().portfolio();
        List<String> notes = new ArrayList<>(availability.notes());
        List<ReachDecision> reach = reach(portfolio, notes);
        AccessPlan accessPlan = accessPlan(portfolio, internetMetros(portfolio));
        notes.addAll(accessPlan.notes());
        return new ConnectivityPlan(spec.fingerprint(), reach, access(accessPlan), notes, accessPlan);
    }

    private List<ReachDecision> reach(MetroPortfolio portfolio, List<String> notes) {
        List<ReachDecision> decisions = new ArrayList<>();
        PlacementPlan placement = optimization.getPlacement();
        for (CloudIntent cloud : spec.clouds()) {
            Set<String> metros = new LinkedHashSet<>();
            if (placement != null) {
                for (MetroId metro : portfolio.metros()) {
                    placement.legsIn(metro).stream().filter(leg -> cloud.id().equals(leg.cloudId()))
                            .forEach(leg -> metros.add(metro.code()));
                }
            }
            decisions.add(new ReachDecision(cloud.id(), metros.isEmpty() ? Reach.UNDECIDED : Reach.FABRIC_CLOUD_ROUTER,
                    List.copyOf(metros), metros.isEmpty() ? "no placement connects this cloud"
                    : "private on-ramp where the placement connects it"));
        }
        List<String> breakout = internetMetros(portfolio);
        if (peering == null && !spec.peers().isEmpty()) {
            notes.add("No Peering Intelligence result was given, so the peers are undecided");
        }
        for (PeerIntent peer : spec.peers()) {
            String label = peer.label() == null ? "AS" + peer.asn() : peer.label() + " (AS" + peer.asn() + ")";
            UnifiedConnectivityView view = peering == null ? null : peering.unifiedView(peer.asn());
            if (view == null) {
                decisions.add(new ReachDecision(label, Reach.UNDECIDED, List.of(),
                        "no Peering Intelligence result for this peer"));
                continue;
            }
            List<String> ix = new ArrayList<>();
            List<String> fabric = new ArrayList<>();
            for (MetroId metro : portfolio.metros()) {
                UnifiedConnectivityView.MetroConnectivity in = view.forMetro(metro);
                if (in != null && in.isHasIxPeering()) {
                    ix.add(metro.code());
                }
                if (in != null && in.isHasFabric()) {
                    fabric.add(metro.code());
                }
            }
            if (!ix.isEmpty()) {
                decisions.add(new ReachDecision(label, Reach.INTERNET_EXCHANGE, ix,
                        "the peer is at an IX in a metro of the design"));
            } else if (!fabric.isEmpty()) {
                decisions.add(new ReachDecision(label, Reach.FABRIC_PRIVATE, fabric,
                        "no IX presence in a metro of the design; the peer publishes a Fabric service profile there"));
            } else {
                decisions.add(new ReachDecision(label, breakout.isEmpty() ? Reach.UNDECIDED : Reach.INTERNET_ACCESS,
                        breakout, breakout.isEmpty()
                        ? "the peer is in no metro of the design and Internet Access availability is unknown"
                        : "the peer is in no metro of the design"));
            }
        }
        decisions.add(new ReachDecision("internet", breakout.isEmpty() ? Reach.UNDECIDED : Reach.INTERNET_ACCESS,
                breakout, breakout.isEmpty() ? "Internet Access availability is unknown for the metros of the design"
                : "Internet Access in each metro of the design that offers it"));
        return decisions;
    }

    private List<String> internetMetros(MetroPortfolio portfolio) {
        List<String> metros = new ArrayList<>();
        for (MetroId metro : portfolio.metros()) {
            if (availability.internetAccessMetros().contains(metro.code())) {
                metros.add(metro.code());
            }
        }
        return metros;
    }

    private AccessPlan accessPlan(MetroPortfolio portfolio, List<String> internetMetros) {
        AccessPlanner planner = AccessPlanner.forSpec(spec).portfolio(portfolio, optimization.getPlacement())
                .availability(availability).inventory(inventory).internetAccess(internetMetros);
        portUuids.forEach((metro, uuids) -> planner.usePorts(metro, uuids.toArray(String[]::new)));
        deviceUuids.forEach((metro, uuids) -> planner.useDevices(metro, uuids.toArray(String[]::new)));
        return planner.plan();
    }

    private static List<AccessDecision> access(AccessPlan plan) {
        List<AccessDecision> decisions = new ArrayList<>();
        for (MetroAccess metro : plan.metros()) {
            List<String> orders = new ArrayList<>();
            for (PlannedAccessItem item : metro.items()) {
                String what = PlannedAccessItem.label(item.kind()) + " " + item.name();
                orders.add(item.exists() ? "use existing " + what + " (" + item.existingUuid() + ")"
                        : "order " + what + (item.note() == null ? "" : ": " + item.note()));
            }
            for (PlannedAccessLink link : metro.links()) {
                orders.add("virtual connection " + link.name() + ", " + link.bandwidthMbps() + " Mbps, from the "
                        + "Cloud Router in " + metro.metro() + " to " + link.item());
            }
            decisions.add(new AccessDecision(metro.metro(), switch (metro.mode()) {
                case PHYSICAL_PORTS -> Access.PHYSICAL_PORTS;
                case NETWORK_EDGE -> Access.NETWORK_EDGE;
                case CLOUD_ROUTER_ONLY -> Access.CLOUD_ROUTER_ONLY;
            }, metro.reason(), orders));
        }
        return decisions;
    }
}
