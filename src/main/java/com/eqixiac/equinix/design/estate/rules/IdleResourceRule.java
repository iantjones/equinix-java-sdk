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
import com.eqixiac.equinix.design.estate.EstateInventory.PortRecord;
import com.eqixiac.equinix.design.estate.EstateInventory.RouterRecord;
import com.eqixiac.equinix.design.estate.EstateInventory.ServiceTokenRecord;
import com.eqixiac.equinix.design.estate.EstatePricing;
import com.eqixiac.equinix.design.estate.EstateRule;
import com.eqixiac.equinix.design.estate.FinancialImpact;
import com.eqixiac.equinix.design.estate.FindingCategory;
import com.eqixiac.equinix.design.estate.RuleContext;
import com.eqixiac.equinix.design.estate.Severity;
import com.eqixiac.equinix.design.estate.UtilizationSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * {@link FindingCategory#IDLE_OR_UNUSED}: resources that are held but not used.
 *
 * <ul>
 *   <li>an {@code ACTIVE} port the API reports with zero connections (unpriced: the rate card has no
 *       port prices);</li>
 *   <li>a live connection with utilisation data whose peak over the window is zero (saving: its
 *       price);</li>
 *   <li>a provisioned Cloud Router with no connections (saving: its price);</li>
 *   <li>service tokens that expired or are past their expiry date unredeemed (one finding);</li>
 *   <li>deprovisioned or deleted records still returned by the API (one informational finding).</li>
 * </ul>
 */
public final class IdleResourceRule implements EstateRule {

    private static final Set<String> GONE = Set.of("DEPROVISIONED", "DELETED", "CANCELLED");

    @Override
    public String name() {
        return "idle-or-unused";
    }

    @Override
    public List<EstateFinding> evaluate(EstateInventory inv, UtilizationSnapshot util, RuleContext ctx) {
        List<EstateFinding> out = new ArrayList<>();
        for (PortRecord p : inv.ports()) {
            boolean noneOnPort = p.connectionCount() != null ? p.connectionCount() == 0
                    : p.usedMbps() != null && p.usedMbps() == 0 && inv.connectionsOnPort(p.id()).isEmpty();
            if ("ACTIVE".equals(p.state()) && noneOnPort) {
                out.add(new EstateFinding(RuleSupport.id(FindingCategory.IDLE_OR_UNUSED, "port", List.of(p.id())),
                        FindingCategory.IDLE_OR_UNUSED, Severity.MEDIUM, "Active port with no connections",
                        List.of(RuleSupport.ref(p)),
                        List.of(RuleSupport.inventory("connections on port",
                                        p.connectionCount() == null ? 0 : p.connectionCount(), null, inv),
                                RuleSupport.inventory("allocated bandwidth", p.usedMbps(), "Mbps", inv),
                                RuleSupport.inventory("port speed", p.speedMbps(), "Mbps", inv),
                                RuleSupport.inventory("age", ctx.ageDays(p.createdAt()), "days", inv)),
                        "Confirm port " + RuleSupport.label(p.name(), p.id())
                                + " is not reserved for planned connections, then decommission it",
                        FinancialImpact.unpriced("the rate card has no Fabric port prices; the saving is the port's "
                                + "monthly charge on the invoice"),
                        Confidence.MEDIUM, List.of("the API's connection count includes connections of every project"),
                        name()));
            }
        }
        for (ConnectionRecord c : inv.connections()) {
            if (!c.isLive()) {
                continue;
            }
            Optional<UtilizationSnapshot.Stats> s = util.known(c.id());
            if (s.isPresent() && s.get().samples() >= ctx.thresholds().minUtilizationSamples()
                    && s.get().peakMbps() == 0.0) {
                EstatePricing.Priced price = ctx.pricing().connection("connection " + c.id(), c.type(),
                        c.bandwidthMbps(), c.pricingMetro(), EstatePricing.termOf(c.termMonths()));
                FinancialImpact impact = price.isPriced()
                        ? FinancialImpact.between(price.line(), null, RuleSupport.priceNote(price))
                        : FinancialImpact.unpriced(price.reason());
                List<String> assumptions = new ArrayList<>(List.of(
                        "no traffic in the window means no use; a standby (secondary) path is idle by design"));
                if (!c.isOnDemand()) {
                    assumptions.add("the connection is on a " + c.termMonths()
                            + "-month term: early termination may be charged");
                }
                boolean standby = "SECONDARY".equals(c.redundancyPriority());
                out.add(new EstateFinding(RuleSupport.id(FindingCategory.IDLE_OR_UNUSED, "connection", List.of(c.id())),
                        FindingCategory.IDLE_OR_UNUSED, standby ? Severity.INFO : Severity.MEDIUM,
                        "Connection carried no traffic in the window" + (standby ? " (secondary path)" : ""),
                        List.of(RuleSupport.ref(c)), RuleSupport.utilisation(s.get(), util),
                        standby ? "Confirm secondary connection " + RuleSupport.label(c.name(), c.id())
                                + " is a deliberate standby and that fail-over is tested"
                                : "Confirm connection " + RuleSupport.label(c.name(), c.id())
                                + " is unused, then delete it",
                        impact, standby ? Confidence.LOW : Confidence.MEDIUM, assumptions, name()));
            }
        }
        for (RouterRecord r : inv.routers()) {
            if (!r.isLive()) {
                continue;
            }
            int count = r.connectionCount() != null ? r.connectionCount() : inv.connectionsOnRouter(r.id()).size();
            if (count == 0) {
                EstatePricing.Priced price = ctx.pricing().router("Cloud Router " + r.id(), r.packageCode(),
                        r.metro(), EstatePricing.termOf(r.termMonths()));
                FinancialImpact impact = price.isPriced()
                        ? FinancialImpact.between(price.line(), null, RuleSupport.priceNote(price))
                        : FinancialImpact.unpriced(price.reason());
                out.add(new EstateFinding(RuleSupport.id(FindingCategory.IDLE_OR_UNUSED, "cloudRouter", List.of(r.id())),
                        FindingCategory.IDLE_OR_UNUSED, impact.isSaving() ? Severity.MEDIUM : Severity.LOW,
                        "Provisioned Cloud Router with no connections", List.of(RuleSupport.ref(r)),
                        List.of(RuleSupport.inventory("connections on router", 0, null, inv),
                                RuleSupport.inventory("package", r.packageCode(), null, inv),
                                RuleSupport.inventory("age", ctx.ageDays(r.createdAt()), "days", inv)),
                        "Delete Cloud Router " + RuleSupport.label(r.name(), r.id()) + " unless connections are planned",
                        impact, Confidence.HIGH, List.of("connection count as reported by the API"), name()));
            }
        }
        List<ResourceRef> tokens = new ArrayList<>();
        for (ServiceTokenRecord t : inv.serviceTokens()) {
            boolean expired = "EXPIRED".equals(t.state())
                    || ("INACTIVE".equals(t.state()) || "ACTIVE".equals(t.state()))
                    && t.expiresAt() != null && t.expiresAt().isBefore(ctx.asOf());
            if (expired) {
                tokens.add(RuleSupport.ref(t));
            }
        }
        if (!tokens.isEmpty()) {
            out.add(new EstateFinding(RuleSupport.id(FindingCategory.IDLE_OR_UNUSED, "expired-service-tokens",
                    tokens.stream().map(ResourceRef::id).toList()), FindingCategory.IDLE_OR_UNUSED, Severity.LOW,
                    tokens.size() + " service token(s) expired without being redeemed", tokens,
                    List.of(RuleSupport.inventory("expired tokens", tokens.size(), null, inv)),
                    "Delete the expired service tokens, or reissue them if the connections are still wanted",
                    FinancialImpact.none("service tokens are not billed"), Confidence.HIGH, List.of(), name()));
        }
        List<ResourceRef> gone = new ArrayList<>();
        inv.connections().stream().filter(c -> GONE.contains(c.state())).forEach(c -> gone.add(RuleSupport.ref(c)));
        inv.routers().stream().filter(r -> GONE.contains(r.state())).forEach(r -> gone.add(RuleSupport.ref(r)));
        inv.networks().stream().filter(n -> GONE.contains(n.state())).forEach(n -> gone.add(RuleSupport.ref(n)));
        inv.ports().stream().filter(p -> GONE.contains(p.state())).forEach(p -> gone.add(RuleSupport.ref(p)));
        if (!gone.isEmpty()) {
            out.add(new EstateFinding(RuleSupport.id(FindingCategory.IDLE_OR_UNUSED, "deprovisioned-records",
                    gone.stream().map(ResourceRef::id).toList()), FindingCategory.IDLE_OR_UNUSED, Severity.INFO,
                    gone.size() + " deprovisioned or deleted record(s) still listed", gone,
                    List.of(new Evidence("deprovisioned or deleted records", String.valueOf(gone.size()), null,
                            inv.source(), "as of " + inv.asOf())),
                    "No action needed; exclude these records from inventory reports and automation",
                    FinancialImpact.none("deprovisioned resources are not billed"), Confidence.HIGH, List.of(),
                    name()));
        }
        return out;
    }
}
