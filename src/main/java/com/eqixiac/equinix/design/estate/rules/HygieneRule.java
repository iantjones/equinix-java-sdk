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
import com.eqixiac.equinix.design.estate.EstateFinding.ResourceRef;
import com.eqixiac.equinix.design.estate.EstateInventory;
import com.eqixiac.equinix.design.estate.EstateInventory.ConnectionRecord;
import com.eqixiac.equinix.design.estate.EstateInventory.PortRecord;
import com.eqixiac.equinix.design.estate.EstateInventory.RouterRecord;
import com.eqixiac.equinix.design.estate.EstateRule;
import com.eqixiac.equinix.design.estate.FinancialImpact;
import com.eqixiac.equinix.design.estate.FindingCategory;
import com.eqixiac.equinix.design.estate.RuleContext;
import com.eqixiac.equinix.design.estate.Severity;
import com.eqixiac.equinix.design.estate.UtilizationSnapshot;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * {@link FindingCategory#HYGIENE}: one finding per issue and resource kind, listing every affected
 * resource (so an estate with 80 stuck connections gets one finding, not 80).
 *
 * <ul>
 *   <li>stuck: a connection, router or port in a pending, provisioning or failed state, created longer ago
 *       than {@code stuckAfterDays} (grouped by state and Equinix status; FAILED is HIGH; unknown age is skipped);</li>
 *   <li>live connections and routers with no notification contacts;</li>
 *   <li>live connections, routers and ports with no project id;</li>
 *   <li>live connections with no purchase-order number.</li>
 * </ul>
 */
public final class HygieneRule implements EstateRule {

    private static final Set<String> STUCK_CONNECTION = Set.of("PENDING", "PROVISIONING", "DEPROVISIONING", "FAILED",
            "DRAFT", "NOT_AVAILABLE");
    private static final Set<String> STUCK_ROUTER = Set.of("PROVISIONING", "REPROVISIONING", "DEPROVISIONING",
            "NOT_PROVISIONED", "NOT_DEPROVISIONED");
    private static final Set<String> STUCK_PORT = Set.of("PENDING", "PENDING_CROSS_CONNECT", "PROVISIONING",
            "REPROVISIONING", "DEPROVISIONING", "FAILED");

    @Override
    public String name() {
        return "hygiene";
    }

    @Override
    public List<EstateFinding> evaluate(EstateInventory inv, UtilizationSnapshot util, RuleContext ctx) {
        List<EstateFinding> out = new ArrayList<>();
        int stuckDays = ctx.thresholds().stuckAfterDays();
        Map<String, List<ResourceRef>> stuck = new TreeMap<>();
        for (ConnectionRecord c : inv.connections()) {
            if (STUCK_CONNECTION.contains(c.state()) && old(c.createdAt(), ctx)) {
                String key = "connection|" + c.state() + (c.equinixStatus() == null ? "" : "/" + c.equinixStatus());
                stuck.computeIfAbsent(key, k -> new ArrayList<>()).add(RuleSupport.ref(c));
            }
        }
        for (RouterRecord r : inv.routers()) {
            if (STUCK_ROUTER.contains(r.state()) && old(r.createdAt(), ctx)) {
                stuck.computeIfAbsent("cloudRouter|" + r.state(), k -> new ArrayList<>()).add(RuleSupport.ref(r));
            }
        }
        for (PortRecord p : inv.ports()) {
            if (STUCK_PORT.contains(p.state()) && old(p.createdAt(), ctx)) {
                stuck.computeIfAbsent("port|" + p.state(), k -> new ArrayList<>()).add(RuleSupport.ref(p));
            }
        }
        stuck.forEach((key, refs) -> {
            String kind = key.substring(0, key.indexOf('|'));
            String state = key.substring(key.indexOf('|') + 1);
            boolean failed = state.startsWith("FAILED") || state.startsWith("NOT_");
            String advice = state.contains("PENDING_APPROVAL")
                    ? "approve them with the service provider (for a cloud, accept the connection in the provider "
                    + "console) or delete them"
                    : failed ? "open a ticket or delete and re-order them" : "check the order status or delete them";
            out.add(new EstateFinding(RuleSupport.id(FindingCategory.HYGIENE, "stuck-" + kind + "-" + state,
                    refs.stream().map(ResourceRef::id).toList()), FindingCategory.HYGIENE,
                    failed ? Severity.HIGH : Severity.MEDIUM,
                    refs.size() + " " + kind + "(s) in " + state + " for more than " + stuckDays + " days", refs,
                    List.of(RuleSupport.inventory(kind + "s in " + state, refs.size(), null, inv),
                            RuleSupport.inventory("stuck threshold", stuckDays, "days since creation", inv)),
                    "Review the " + refs.size() + " " + kind + "(s) in " + state + ": " + advice,
                    FinancialImpact.none("a resource is not billed until it is provisioned (assumption)"),
                    Confidence.HIGH, List.of("age is measured from creation; the API reports no state-change time"),
                    name()));
        });
        List<ResourceRef> noNotify = new ArrayList<>();
        List<ResourceRef> noProject = new ArrayList<>();
        List<ResourceRef> noPo = new ArrayList<>();
        for (ConnectionRecord c : inv.connections()) {
            if (!c.isLive()) {
                continue;
            }
            if (c.notificationCount() != null && c.notificationCount() == 0) {
                noNotify.add(RuleSupport.ref(c));
            }
            if (c.projectId() == null) {
                noProject.add(RuleSupport.ref(c));
            }
            if (c.purchaseOrder() == null || c.purchaseOrder().isBlank()) {
                noPo.add(RuleSupport.ref(c));
            }
        }
        for (RouterRecord r : inv.routers()) {
            if (!r.isLive()) {
                continue;
            }
            if (r.notificationCount() != null && r.notificationCount() == 0) {
                noNotify.add(RuleSupport.ref(r));
            }
            if (r.projectId() == null) {
                noProject.add(RuleSupport.ref(r));
            }
        }
        for (PortRecord p : inv.ports()) {
            if ("ACTIVE".equals(p.state()) && p.projectId() == null) {
                noProject.add(RuleSupport.ref(p));
            }
        }
        aggregate(out, "no-notifications", Severity.LOW, noNotify, "live resource(s) without notification contacts",
                "Add notification e-mail contacts so that maintenance and outage notices reach someone", inv);
        aggregate(out, "no-project", Severity.LOW, noProject, "live resource(s) without a project",
                "Assign the resources to a project so that access and cost can be attributed", inv);
        aggregate(out, "no-purchase-order", Severity.INFO, noPo, "live connection(s) without a purchase-order number",
                "Set purchase-order numbers where your invoicing needs them", inv);
        return out;
    }

    private void aggregate(List<EstateFinding> out, String issue, Severity sev, List<ResourceRef> refs, String what,
                           String action, EstateInventory inv) {
        if (refs.isEmpty()) {
            return;
        }
        out.add(new EstateFinding(RuleSupport.id(FindingCategory.HYGIENE, issue,
                refs.stream().map(ResourceRef::id).toList()), FindingCategory.HYGIENE, sev,
                refs.size() + " " + what, refs, List.of(RuleSupport.inventory(what, refs.size(), null, inv)),
                action, FinancialImpact.none("tagging and notifications are not billed"), Confidence.HIGH, List.of(),
                name()));
    }

    private static boolean old(Instant createdAt, RuleContext ctx) {
        long age = ctx.ageDays(createdAt);
        return age >= 0 && age >= ctx.thresholds().stuckAfterDays();
    }
}
