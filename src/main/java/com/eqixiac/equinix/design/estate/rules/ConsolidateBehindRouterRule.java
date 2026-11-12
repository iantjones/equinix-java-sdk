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

package com.eqixiac.equinix.design.estate.rules;

import com.eqixiac.equinix.design.estate.Confidence;
import com.eqixiac.equinix.design.estate.EstateFinding;
import com.eqixiac.equinix.design.estate.EstateFinding.Evidence;
import com.eqixiac.equinix.design.estate.EstateFinding.ResourceRef;
import com.eqixiac.equinix.design.estate.EstateInventory;
import com.eqixiac.equinix.design.estate.EstateInventory.ConnectionRecord;
import com.eqixiac.equinix.design.estate.EstateInventory.RouterRecord;
import com.eqixiac.equinix.design.estate.EstatePricing;
import com.eqixiac.equinix.design.estate.EstateRule;
import com.eqixiac.equinix.design.estate.FinancialImpact;
import com.eqixiac.equinix.design.estate.FindingCategory;
import com.eqixiac.equinix.design.estate.RuleContext;
import com.eqixiac.equinix.design.estate.Severity;
import com.eqixiac.equinix.design.estate.UtilizationSnapshot;
import com.eqixiac.equinix.design.spi.CostLine;
import com.eqixiac.equinix.design.value.ratecard.Term;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * {@link FindingCategory#CONSOLIDATE_BEHIND_ROUTER}, both directions.
 *
 * <ul>
 *   <li><b>Consolidate:</b> at least {@code consolidateMinConnections} live port-to-cloud
 *       ({@code COLO} to {@code SP}) connections whose A side is in one metro. A Fabric Cloud Router
 *       there would give them one routing domain (cloud-to-cloud traffic without hair-pinning
 *       through the customer's router). When the scope already has a live router in that metro the
 *       recommendation re-homes onto it; otherwise it prices a new {@code STANDARD} router. The
 *       financial impact compares today's connections with the router plus the same bandwidth as
 *       {@code IP_VC}s; it is often a cost, not a saving, and is reported as such.</li>
 *   <li><b>Reverse:</b> a live router with exactly one connection. It could be replaced by a direct
 *       connection; the saving is the router price (the connection is assumed to cost the same as
 *       an {@code EVPL_VC} of the same bandwidth). Routers with no connections are left to
 *       {@link IdleResourceRule}.</li>
 * </ul>
 */
public final class ConsolidateBehindRouterRule implements EstateRule {

    /** The package priced for a new router (assumption: the smallest production package). */
    public static final String NEW_ROUTER_PACKAGE = "STANDARD";

    @Override
    public String name() {
        return "consolidate-behind-router";
    }

    @Override
    public List<EstateFinding> evaluate(EstateInventory inv, UtilizationSnapshot util, RuleContext ctx) {
        List<EstateFinding> out = new ArrayList<>();
        Map<String, List<ConnectionRecord>> byMetro = new TreeMap<>();
        for (ConnectionRecord c : inv.connections()) {
            if (c.isLive() && "COLO".equals(c.aSide().type()) && "SP".equals(c.zSide().type())
                    && c.aSide().metro() != null) {
                byMetro.computeIfAbsent(c.aSide().metro(), k -> new ArrayList<>()).add(c);
            }
        }
        byMetro.forEach((metro, conns) -> {
            if (conns.size() >= ctx.thresholds().consolidateMinConnections()) {
                out.add(consolidate(metro, conns, inv, ctx));
            }
        });
        for (RouterRecord r : inv.routers()) {
            if (!r.isLive()) {
                continue;
            }
            List<ConnectionRecord> onRouter = inv.connectionsOnRouter(r.id()).stream()
                    .filter(ConnectionRecord::isLive).toList();
            int count = r.connectionCount() != null ? r.connectionCount() : onRouter.size();
            if (count == 1) {
                out.add(underused(r, onRouter, inv, ctx));
            }
        }
        return out;
    }

    private EstateFinding consolidate(String metro, List<ConnectionRecord> conns, EstateInventory inv, RuleContext ctx) {
        Optional<RouterRecord> existing = inv.routers().stream()
                .filter(r -> r.isLive() && metro.equals(r.metro())).findFirst();
        List<EstatePricing.Priced> current = new ArrayList<>();
        List<EstatePricing.Priced> proposed = new ArrayList<>();
        List<ResourceRef> refs = new ArrayList<>();
        List<String> ids = new ArrayList<>();
        for (ConnectionRecord c : conns) {
            Term term = EstatePricing.termOf(c.termMonths());
            current.add(ctx.pricing().connection("connection " + c.id(), c.type(), c.bandwidthMbps(), metro, term));
            proposed.add(ctx.pricing().connection("IP_VC " + c.bandwidthMbps() + " Mbps", "IP_VC",
                    c.bandwidthMbps(), metro, term));
            refs.add(RuleSupport.ref(c));
            ids.add(c.id());
        }
        if (existing.isEmpty()) {
            proposed.add(ctx.pricing().router("new Cloud Router " + NEW_ROUTER_PACKAGE + " " + metro,
                    NEW_ROUTER_PACKAGE, metro, Term.MONTH_1));
        } else {
            refs.add(RuleSupport.ref(existing.get()));
        }
        CostLine now = RuleSupport.sum("current port-to-cloud connections in " + metro, current);
        CostLine then = RuleSupport.sum("Cloud Router design in " + metro, proposed);
        FinancialImpact impact = now != null && then != null ? FinancialImpact.between(now, then, null)
                : FinancialImpact.unpriced(RuleSupport.firstReason(now == null ? current : proposed));
        long profiles = conns.stream().map(c -> c.zSide().serviceProfileName() != null
                ? c.zSide().serviceProfileName() : String.valueOf(c.zSide().serviceProfileId())).distinct().count();
        List<Evidence> evidence = List.of(
                RuleSupport.inventory("port-to-cloud connections in " + metro, conns.size(), null, inv),
                RuleSupport.inventory("distinct cloud service profiles", profiles, null, inv),
                RuleSupport.inventory("existing live Cloud Router in metro", existing.map(RouterRecord::id).orElse("none"),
                        null, inv));
        String action = existing.map(r -> "Re-home " + conns.size() + " port-to-cloud connections in " + metro
                        + " onto Cloud Router " + RuleSupport.label(r.name(), r.id()) + " as IP_VC connections")
                .orElse("Order a " + NEW_ROUTER_PACKAGE + " Fabric Cloud Router in " + metro + " and re-home "
                        + conns.size() + " port-to-cloud connections onto it as IP_VC connections");
        List<String> assumptions = List.of(
                "the benefit is one routing domain (cloud-to-cloud routing on Fabric, fewer BGP sessions on the "
                        + "customer router); it is not necessarily a saving",
                "each connection keeps its bandwidth and term as an IP_VC",
                existing.isPresent() ? "the existing router has spare capacity"
                        : "a new router is priced as " + NEW_ROUTER_PACKAGE + " on demand");
        return new EstateFinding(RuleSupport.id(FindingCategory.CONSOLIDATE_BEHIND_ROUTER, "metro-" + metro, ids),
                FindingCategory.CONSOLIDATE_BEHIND_ROUTER, Severity.LOW,
                conns.size() + " port-to-cloud connections in " + metro + " could share a Cloud Router", refs,
                evidence, action, impact, Confidence.MEDIUM, assumptions, name());
    }

    private EstateFinding underused(RouterRecord r, List<ConnectionRecord> onRouter, EstateInventory inv,
                                    RuleContext ctx) {
        EstatePricing.Priced routerPrice = ctx.pricing().router("Cloud Router " + r.id(), r.packageCode(), r.metro(),
                EstatePricing.termOf(r.termMonths()));
        FinancialImpact impact = routerPrice.isPriced()
                ? FinancialImpact.between(routerPrice.line(), null, RuleSupport.priceNote(routerPrice))
                : FinancialImpact.unpriced(routerPrice.reason());
        List<ResourceRef> refs = new ArrayList<>();
        refs.add(RuleSupport.ref(r));
        onRouter.forEach(c -> refs.add(RuleSupport.ref(c)));
        return new EstateFinding(RuleSupport.id(FindingCategory.CONSOLIDATE_BEHIND_ROUTER, "single-connection-router",
                List.of(r.id())), FindingCategory.CONSOLIDATE_BEHIND_ROUTER, Severity.LOW,
                "Cloud Router with a single connection", refs,
                List.of(RuleSupport.inventory("connections on router", 1, null, inv),
                        RuleSupport.inventory("package", r.packageCode(), null, inv)),
                "Replace Cloud Router " + RuleSupport.label(r.name(), r.id())
                        + " and its one connection with a direct connection, unless more connections are planned",
                impact, Confidence.MEDIUM,
                List.of("a direct EVPL_VC of the same bandwidth costs about the same as the router's IP_VC",
                        "no Layer-3 feature of the router (routing protocols, route filters) is needed"), name());
    }
}
