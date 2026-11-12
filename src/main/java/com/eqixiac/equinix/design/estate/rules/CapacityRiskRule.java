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
import com.eqixiac.equinix.design.estate.EstateInventory;
import com.eqixiac.equinix.design.estate.EstateInventory.ConnectionRecord;
import com.eqixiac.equinix.design.estate.EstateInventory.PortRecord;
import com.eqixiac.equinix.design.estate.EstateInventory.RouterRecord;
import com.eqixiac.equinix.design.estate.EstatePricing;
import com.eqixiac.equinix.design.estate.EstateRule;
import com.eqixiac.equinix.design.estate.EstateThresholds;
import com.eqixiac.equinix.design.estate.FinancialImpact;
import com.eqixiac.equinix.design.estate.FindingCategory;
import com.eqixiac.equinix.design.estate.RuleContext;
import com.eqixiac.equinix.design.estate.Severity;
import com.eqixiac.equinix.design.estate.UtilizationSnapshot;
import com.eqixiac.equinix.design.value.ratecard.Term;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link FindingCategory#CAPACITY_RISK}.
 *
 * <ul>
 *   <li><b>Ports:</b> bandwidth allocated to virtual connections at or above
 *       {@code portCapacityWarnPercent} of the port speed (HIGH at {@code portCapacityCriticalPercent}).
 *       The allocation is the API's {@code usedBandwidth} when reported, else the sum of the in-scope
 *       live connections on the port (which can only under-count).</li>
 *   <li><b>Cloud Routers:</b> connections at or above {@code routerLimitWarnPercent} of the package's
 *       connection limit. The limit comes from the {@code /routerPackages} API ({@code vcCountMax})
 *       or from {@link EstateThresholds#routerPackageConnectionLimits()}; with neither, the router
 *       is not assessed (the report lists that as not analysed). The impact is the price
 *       difference to the next package.</li>
 * </ul>
 */
public final class CapacityRiskRule implements EstateRule {

    private static final List<String> PACKAGE_ORDER = List.of("LAB", "BASIC", "STANDARD", "ADVANCED", "PREMIUM");

    @Override
    public String name() {
        return "capacity-risk";
    }

    @Override
    public List<EstateFinding> evaluate(EstateInventory inv, UtilizationSnapshot util, RuleContext ctx) {
        EstateThresholds t = ctx.thresholds();
        List<EstateFinding> out = new ArrayList<>();
        for (PortRecord p : inv.ports()) {
            if (!"ACTIVE".equals(p.state()) || p.speedMbps() == null || p.speedMbps() <= 0) {
                continue;
            }
            boolean reported = p.usedMbps() != null;
            long allocated = reported ? p.usedMbps() : inv.connectionsOnPort(p.id()).stream()
                    .filter(ConnectionRecord::isLive).mapToLong(c -> c.bandwidthMbps() == null ? 0 : c.bandwidthMbps())
                    .sum();
            double pct = allocated * 100.0 / p.speedMbps();
            if (pct < t.portCapacityWarnPercent()) {
                continue;
            }
            Severity sev = pct >= t.portCapacityCriticalPercent() ? Severity.HIGH : Severity.MEDIUM;
            out.add(new EstateFinding(RuleSupport.id(FindingCategory.CAPACITY_RISK, "port", List.of(p.id())),
                    FindingCategory.CAPACITY_RISK, sev,
                    "Port " + RuleSupport.fmt(pct) + "% allocated", List.of(RuleSupport.ref(p)),
                    List.of(RuleSupport.inventory("allocated bandwidth", allocated, "Mbps",
                                    inv),
                            RuleSupport.inventory("allocation source", reported ? "usedBandwidth (API)"
                                    : "sum of in-scope live connections", null, inv),
                            RuleSupport.inventory("port speed", p.speedMbps(), "Mbps", inv),
                            RuleSupport.inventory("allocated percent", RuleSupport.fmt(pct), "%", inv)),
                    "Plan more capacity for port " + RuleSupport.label(p.name(), p.id())
                            + " (add a LAG member or a second port) before ordering more connections on it",
                    FinancialImpact.unpriced("the rate card has no Fabric port prices"),
                    reported ? Confidence.HIGH : Confidence.MEDIUM,
                    List.of("allocated bandwidth is committed, not measured, traffic"), name()));
        }
        for (RouterRecord r : inv.routers()) {
            if (!r.isLive() || r.packageCode() == null) {
                continue;
            }
            Integer limit = inv.routerPackageLimits().get(r.packageCode());
            String limitSource = "routerPackages API (vcCountMax)";
            if (limit == null) {
                limit = t.routerPackageConnectionLimits().get(r.packageCode());
                limitSource = "caller-supplied threshold";
            }
            if (limit == null || limit <= 0) {
                continue;
            }
            int count = r.connectionCount() != null ? r.connectionCount() : inv.connectionsOnRouter(r.id()).size();
            double pct = count * 100.0 / limit;
            if (pct < t.routerLimitWarnPercent()) {
                continue;
            }
            int idx = PACKAGE_ORDER.indexOf(r.packageCode());
            String next = idx >= 0 && idx + 1 < PACKAGE_ORDER.size() ? PACKAGE_ORDER.get(idx + 1) : null;
            FinancialImpact impact;
            String action;
            if (next == null) {
                action = "Split the connections of Cloud Router " + RuleSupport.label(r.name(), r.id())
                        + " across a second router: no larger package is listed";
                impact = FinancialImpact.unpriced("no larger package than " + r.packageCode() + " is listed");
            } else {
                Term term = EstatePricing.termOf(r.termMonths());
                EstatePricing.Priced now = ctx.pricing().router("Cloud Router " + r.id() + " " + r.packageCode(),
                        r.packageCode(), r.metro(), term);
                EstatePricing.Priced up = ctx.pricing().router("Cloud Router " + r.id() + " " + next, next,
                        r.metro(), term);
                impact = now.isPriced() && up.isPriced()
                        ? FinancialImpact.between(now.line(), up.line(), RuleSupport.priceNote(now, up))
                        : FinancialImpact.unpriced(now.isPriced() ? up.reason() : now.reason());
                action = "Upgrade Cloud Router " + RuleSupport.label(r.name(), r.id()) + " from " + r.packageCode()
                        + " to " + next + " before adding connections";
            }
            out.add(new EstateFinding(RuleSupport.id(FindingCategory.CAPACITY_RISK, "cloudRouter", List.of(r.id())),
                    FindingCategory.CAPACITY_RISK, pct >= 100 ? Severity.HIGH : Severity.MEDIUM,
                    "Cloud Router at " + count + " of " + limit + " connections", List.of(RuleSupport.ref(r)),
                    List.of(RuleSupport.inventory("connections", count, null, inv),
                            RuleSupport.inventory("package limit", limit, "connections", inv),
                            RuleSupport.inventory("limit source", limitSource, null, inv)),
                    action, impact, Confidence.HIGH, List.of(), name()));
        }
        return out;
    }
}
