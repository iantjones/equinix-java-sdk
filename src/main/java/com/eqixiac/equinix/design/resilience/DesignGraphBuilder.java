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
package com.eqixiac.equinix.design.resilience;

import com.eqixiac.equinix.core.enums.Region;
import com.eqixiac.equinix.core.model.MetroId;
import com.eqixiac.equinix.design.optimizer.context.CatalogDesignContext;
import com.eqixiac.equinix.design.optimizer.context.LatencyFigure;
import com.eqixiac.equinix.design.peering.model.NetworkPresence;
import com.eqixiac.equinix.design.peering.model.PeeringIntelligenceResult;
import com.eqixiac.equinix.design.placement.PlacementPlan;
import com.eqixiac.equinix.design.placement.WorkloadPlacer;
import com.eqixiac.equinix.design.spec.CloudIntent;
import com.eqixiac.equinix.design.spec.DesignSpec;
import com.eqixiac.equinix.design.spec.PeerIntent;
import com.eqixiac.equinix.design.spec.SiteIntent;
import com.eqixiac.equinix.design.spec.WorkloadIntent;
import com.eqixiac.equinix.design.spi.DesignContext;
import com.eqixiac.equinix.design.spi.MetroFacts;
import com.eqixiac.equinix.design.spi.MetroPortfolio;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.TreeSet;

/**
 * Builds the {@link DesignGraph} of a spec on a portfolio. Pure: the same spec, portfolio, placement,
 * context and options give the same graph.
 *
 * <h2>What the graph holds and what is assumed</h2>
 * <ul>
 *   <li>One <b>metro</b> node per portfolio metro, with the catalogue's region and geo zones.</li>
 *   <li>One <b>Cloud Router</b> per metro when the design has cloud connections or more than one metro
 *       (the Layer-3 hub for cloud and inter-metro traffic, as the Deployment Wizard plans it), attached to its
 *       metro. Inter-metro <b>backbone</b> edges join every pair of routers with Fabric's round trip
 *       (measured where Fabric publishes it, estimated otherwise); their capacity is not provisioned by the
 *       spec and is left unknown, so it never constrains the simulation.</li>
 *   <li>One <b>on-ramp</b> per (cloud, metro) cloud leg of the {@link PlacementPlan}, a <b>cloud connection</b>
 *       from the metro's router to it, and a <b>provider link</b> on to the <b>cloud</b> node. A connection's
 *       capacity is the bandwidth of the sized workloads it carries as primary plus
 *       {@code standbyCapacityFraction} of those it carries as standby; unknown when it carries only unsized
 *       workloads. Its role is primary when it carries any workload as primary.</li>
 *   <li>Each <b>site</b> has access circuits into its {@code accessHoming} nearest portfolio metros by known
 *       round trip (the nearest is primary). Access capacity is unknown (the spec states none). A site with
 *       no known latency to any portfolio metro has no access edge and is reported as unknown.</li>
 *   <li>Each <b>workload</b> has a hosting edge to its primary metro and, when it has one, its standby metro.</li>
 *   <li>Optional <b>peer</b> nodes, from peering data, joined to the portfolio metros where the peer is present.</li>
 * </ul>
 */
public final class DesignGraphBuilder {

    private DesignGraphBuilder() {
    }

    /**
     * Builds the graph, placing the workloads with {@link WorkloadPlacer}.
     *
     * @param context   the facts of the run (its spec is the design)
     * @param portfolio the metros
     * @param options   build options
     * @return the graph
     */
    public static DesignGraph build(DesignContext context, MetroPortfolio portfolio, ResilienceOptions options) {
        return build(context, portfolio, WorkloadPlacer.place(portfolio, context), options, List.of());
    }

    /**
     * Builds the graph.
     *
     * @param context   the facts of the run (its spec is the design)
     * @param portfolio the metros
     * @param plan      where every workload runs on {@code portfolio}
     * @param options   build options; {@code null} for the defaults
     * @param peers     peer presence to draw in; may be empty
     * @return the graph
     */
    public static DesignGraph build(DesignContext context, MetroPortfolio portfolio, PlacementPlan plan,
                                    ResilienceOptions options, List<PeerPresence> peers) {
        if (context == null || portfolio == null || plan == null) {
            throw new IllegalArgumentException("A design graph needs a context, a portfolio and a placement plan");
        }
        ResilienceOptions opt = options == null ? ResilienceOptions.defaults() : options;
        DesignSpec spec = context.spec();
        List<GraphNode> nodes = new ArrayList<>();
        List<GraphEdge> edges = new ArrayList<>();
        List<String> assumptions = new ArrayList<>();

        boolean routers = !plan.legs().isEmpty() || portfolio.size() > 1;
        for (MetroId m : portfolio.metros()) {
            MetroFacts facts = context.metro(m).orElse(null);
            nodes.add(new GraphNode(metroId(m.code()), NodeKind.METRO,
                    facts != null && facts.name() != null && !facts.name().equals(m.code())
                            ? m.code() + " " + facts.name() : m.code(),
                    m.code(), facts == null ? null : facts.region(), null,
                    facts == null ? Set.of() : facts.geoZones(), null));
        }
        if (routers) {
            for (MetroId m : portfolio.metros()) {
                nodes.add(new GraphNode(routerId(m.code()), NodeKind.CLOUD_ROUTER, "Cloud Router " + m.code(),
                        m.code(), regionOf(context, m), null, null, null));
                edges.add(new GraphEdge("attach:" + m.code(), EdgeKind.ROUTER_ATTACHMENT, metroId(m.code()),
                        routerId(m.code()), null, null, 0.0, "assumed", null, "inside the metro"));
            }
            assumptions.add("One Fabric Cloud Router per metro carries cloud and inter-metro traffic (as the "
                    + "Deployment Wizard plans it); a router is a single element unless the design adds a second.");
            List<MetroId> ms = portfolio.metros();
            for (int i = 0; i < ms.size(); i++) {
                for (int j = i + 1; j < ms.size(); j++) {
                    MetroId a = ms.get(i);
                    MetroId b = ms.get(j);
                    Latency l = metroLatency(context, a, b);
                    edges.add(new GraphEdge("backbone:" + a.code() + "-" + b.code(), EdgeKind.BACKBONE,
                            routerId(a.code()), routerId(b.code()), null, null, l.ms, l.basis, null, l.note));
                }
            }
            if (ms.size() > 1) {
                assumptions.add("Inter-metro backbone capacity is not provisioned by the spec: it is left unknown "
                        + "and does not constrain the simulation.");
            }
        }

        Map<String, Integer> bandwidth = new LinkedHashMap<>();
        for (PlacementPlan.Placement p : plan.placements()) {
            bandwidth.put(p.workload(), p.bandwidthMbps());
        }
        Set<String> cloudsUsed = new TreeSet<>();
        for (PlacementPlan.CloudLeg leg : plan.legs()) {
            cloudsUsed.add(leg.cloudId());
        }
        for (PlacementPlan.Placement p : plan.placements()) {
            cloudsUsed.addAll(p.clouds());
        }
        for (CloudIntent cloud : spec.clouds()) {
            if (cloudsUsed.contains(cloud.id())) {
                String providerRegion = cloud.region() == null ? null
                        : (cloud.provider() == null ? "" : cloud.provider().name() + " ") + cloud.region();
                nodes.add(new GraphNode(cloudId(cloud.id()), NodeKind.CLOUD, cloudLabel(cloud), null, null,
                        cloud.id(), null, null, providerRegion));
            }
        }
        boolean partialSizing = false;
        for (PlacementPlan.CloudLeg leg : plan.legs()) {
            String m = leg.metro().code();
            String onramp = onrampId(leg.cloudId(), m);
            nodes.add(new GraphNode(onramp, NodeKind.CLOUD_ONRAMP, leg.cloudId() + " on-ramp " + m, m,
                    regionOf(context, leg.metro()), leg.cloudId(), null, null));
            double primary = 0;
            double standby = 0;
            boolean anyPrimary = false;
            boolean anySized = false;
            for (String carried : leg.workloads()) {
                boolean isStandby = carried.endsWith(" (standby)");
                String label = isStandby ? carried.substring(0, carried.length() - " (standby)".length()) : carried;
                anyPrimary |= !isStandby;
                Integer bw = bandwidth.get(label);
                if (bw != null) {
                    anySized = true;
                    if (isStandby) {
                        standby += bw;
                    } else {
                        primary += bw;
                    }
                }
            }
            Double capacity = anySized ? primary + opt.standbyCapacityFraction() * standby : null;
            String note = null;
            if (anySized && !leg.unsizedWorkloads().isEmpty()) {
                partialSizing = true;
                note = "sized for its sized workloads only; unsized: " + String.join(", ", leg.unsizedWorkloads());
            } else if (!anySized) {
                note = "carries only unsized workloads: capacity unknown";
            }
            edges.add(new GraphEdge(connectionId(leg.cloudId(), m), EdgeKind.CLOUD_CONNECTION, routerId(m), onramp,
                    anyPrimary ? EdgeRole.PRIMARY : EdgeRole.STANDBY, capacity, null, null, leg.workloads(), note));
            edges.add(new GraphEdge("provider:" + leg.cloudId() + "@" + m, EdgeKind.PROVIDER_LINK, onramp,
                    cloudId(leg.cloudId()), null, null, null, null, null, "provider side: capacity and latency "
                    + "not known to Fabric"));
        }
        if (!plan.legs().isEmpty()) {
            assumptions.add(String.format(Locale.ROOT, "A cloud connection is sized as its primary workloads' "
                    + "bandwidth plus %.0f%% of its standby workloads' bandwidth.", 100 * opt.standbyCapacityFraction()));
        }
        if (partialSizing) {
            assumptions.add("Some connections also carry unsized workloads: their capacity covers the sized ones only.");
        }

        int unreachableSites = 0;
        for (SiteIntent site : spec.sites()) {
            String sid = siteId(site.label());
            nodes.add(new GraphNode(sid, NodeKind.SITE, site.label(), null, null, null, null,
                    context.siteWeight(site)));
            List<MetroId> ranked = new ArrayList<>();
            for (MetroId m : portfolio.metros()) {
                if (context.roundTripLatencyMs(m, site).isPresent()) {
                    ranked.add(m);
                }
            }
            ranked.sort(Comparator.comparingDouble((MetroId m) -> context.roundTripLatencyMs(m, site).getAsDouble())
                    .thenComparing(MetroId::code));
            if (ranked.isEmpty()) {
                unreachableSites++;
            }
            for (int i = 0; i < Math.min(opt.accessHoming(), ranked.size()); i++) {
                MetroId m = ranked.get(i);
                Latency l = siteLatency(context, m, site);
                edges.add(new GraphEdge("access:" + site.label() + "@" + m.code(), EdgeKind.SITE_ACCESS, sid,
                        metroId(m.code()), i == 0 ? EdgeRole.PRIMARY : EdgeRole.STANDBY, null, l.ms, l.basis,
                        null, l.note));
            }
        }
        if (!spec.sites().isEmpty()) {
            assumptions.add("Each site has access circuits into its " + opt.accessHoming() + " nearest portfolio "
                    + "metro(s) by round trip (the spec does not state site access); access capacity is unknown.");
        }
        if (unreachableSites > 0) {
            assumptions.add(unreachableSites + " site(s) have no known latency to any portfolio metro and no "
                    + "access edge: they are left out of reachability.");
        }

        Map<String, WorkloadIntent> intents = new LinkedHashMap<>();
        for (WorkloadIntent w : spec.workloads()) {
            intents.put(w.label(), w);
        }
        List<PlacementView> views = new ArrayList<>();
        for (PlacementPlan.Placement p : plan.placements()) {
            WorkloadIntent intent = intents.get(p.workload());
            boolean cloudOnly = p.workload().startsWith(WorkloadPlacer.CLOUD_PREFIX);
            PlacementView view = new PlacementView(p.workload(), code(p.primary()), code(p.standby()), p.clouds(),
                    p.bandwidthMbps(), intent == null ? null : intent.maxLatencyMs(), cloudOnly);
            views.add(view);
            nodes.add(new GraphNode(view.nodeId(), NodeKind.WORKLOAD, p.workload(), null, null, null, null, null));
            if (p.primary() != null) {
                edges.add(new GraphEdge("host:" + p.workload() + "@" + p.primary().code(), EdgeKind.WORKLOAD_HOSTING,
                        view.nodeId(), metroId(p.primary().code()), EdgeRole.PRIMARY,
                        p.bandwidthMbps() == null ? null : p.bandwidthMbps().doubleValue(), null, null, null, null));
            }
            if (p.standby() != null) {
                edges.add(new GraphEdge("host:" + p.workload() + "@" + p.standby().code(), EdgeKind.WORKLOAD_HOSTING,
                        view.nodeId(), metroId(p.standby().code()), EdgeRole.STANDBY,
                        p.bandwidthMbps() == null ? null : p.bandwidthMbps().doubleValue(), null, null, null, null));
            }
        }

        if (peers != null) {
            Set<String> seen = new LinkedHashSet<>();
            for (PeerPresence peer : peers) {
                String pid = "peer:AS" + peer.asn();
                if (!seen.add(pid)) {
                    continue;
                }
                nodes.add(new GraphNode(pid, NodeKind.PEER, (peer.label() == null ? "" : peer.label() + " ")
                        + "AS" + peer.asn(), null, null, null, null, null));
                for (String m : peer.metros()) {
                    if (portfolio.contains(MetroId.of(m))) {
                        edges.add(new GraphEdge("peering:AS" + peer.asn() + "@" + m, EdgeKind.PEERING, pid,
                                metroId(m), null, null, null, null, null, peer.source()));
                    }
                }
            }
            if (!seen.isEmpty()) {
                assumptions.add("Peer nodes show presence only (from " + peers.get(0).source() + "); they are not "
                        + "part of failure simulation.");
            }
        }

        nodes.sort(Comparator.comparing((GraphNode n) -> n.kind().ordinal()).thenComparing(GraphNode::id));
        edges.sort(Comparator.comparing((GraphEdge e) -> e.kind().ordinal()).thenComparing(GraphEdge::id));
        List<String> zones = new ArrayList<>();
        for (String z : spec.constraints().geoZones()) {
            zones.add(z.toUpperCase(Locale.ROOT));
        }
        List<String> required = new ArrayList<>();
        for (CloudIntent cloud : spec.clouds()) {
            if (Boolean.TRUE.equals(cloud.required())) {
                required.add(cloud.id());
            }
        }
        return new DesignGraph(spec.fingerprint(), spec.name(), portfolio.key(), nodes, edges, views, required, zones,
                spec.constraints().maxLatencyMs(), assumptions);
    }

    /**
     * Peer presence from a peering analysis, for {@link #build(DesignContext, MetroPortfolio, PlacementPlan,
     * ResilienceOptions, List)}: one entry per analysed network with every metro where it is present.
     *
     * @param result a peering analysis
     * @param spec   the design whose {@link PeerIntent}s name the networks, or {@code null} for every analysed network
     * @return the presence, in ASN order
     */
    public static List<PeerPresence> peers(PeeringIntelligenceResult result, DesignSpec spec) {
        List<PeerPresence> list = new ArrayList<>();
        if (result == null || result.getNetworkPresences() == null) {
            return list;
        }
        Set<Long> wanted = new TreeSet<>();
        if (spec != null) {
            for (PeerIntent peer : spec.peers()) {
                wanted.add(peer.asn());
            }
        }
        Set<Long> asns = new TreeSet<>(result.getNetworkPresences().keySet());
        for (Long asn : asns) {
            if (!wanted.isEmpty() && !wanted.contains(asn)) {
                continue;
            }
            NetworkPresence np = result.getNetworkPresences().get(asn);
            Set<String> metros = new TreeSet<>();
            if (np.getAllMetros() != null) {
                np.getAllMetros().forEach(m -> metros.add(m.code()));
            }
            list.add(new PeerPresence(asn, np.getLabel(), metros, "PeeringDB via PeeringIntelligence"));
        }
        return list;
    }

    static String metroId(String code) {
        return "metro:" + code;
    }

    static String routerId(String code) {
        return "router:" + code;
    }

    static String onrampId(String cloud, String metro) {
        return "onramp:" + cloud + "@" + metro;
    }

    static String connectionId(String cloud, String metro) {
        return "conn:" + cloud + "@" + metro;
    }

    static String cloudId(String cloud) {
        return "cloud:" + cloud;
    }

    static String siteId(String label) {
        return "site:" + label;
    }

    private static String code(MetroId m) {
        return m == null ? null : m.code();
    }

    private static Region regionOf(DesignContext context, MetroId m) {
        return context.metro(m).map(MetroFacts::region).orElse(null);
    }

    private static String cloudLabel(CloudIntent cloud) {
        StringBuilder sb = new StringBuilder(cloud.id());
        if (cloud.provider() != null) {
            sb.append(" (").append(cloud.provider().name());
            if (cloud.region() != null) {
                sb.append(' ').append(cloud.region());
            }
            sb.append(')');
        } else if (cloud.region() != null) {
            sb.append(" (").append(cloud.region()).append(')');
        }
        return sb.toString();
    }

    private record Latency(Double ms, String basis, String note) {
    }

    private static Latency metroLatency(DesignContext context, MetroId a, MetroId b) {
        if (context instanceof CatalogDesignContext catalog) {
            return of(catalog.latency(a, b));
        }
        OptionalDouble ms = context.roundTripLatencyMs(a, b);
        return ms.isPresent() ? new Latency(ms.getAsDouble(), "context", null)
                : new Latency(null, null, "unknown: no measurement and no coordinates");
    }

    private static Latency siteLatency(DesignContext context, MetroId m, SiteIntent site) {
        if (context instanceof CatalogDesignContext catalog) {
            return of(catalog.latency(m, site));
        }
        OptionalDouble ms = context.roundTripLatencyMs(m, site);
        if (ms.isEmpty()) {
            return new Latency(null, null, "unknown");
        }
        return new Latency(ms.getAsDouble(), context.latencyMeasured(m, site) ? "measured" : "estimated", null);
    }

    private static Latency of(LatencyFigure f) {
        if (f.roundTripMs() == null) {
            return new Latency(null, null, f.note());
        }
        return new Latency(f.roundTripMs(), f.basis().name().toLowerCase(Locale.ROOT).replace('_', '-'), f.note());
    }
}
