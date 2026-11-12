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
import com.eqixiac.equinix.design.estate.EstateInventory.DeviceRecord;
import com.eqixiac.equinix.design.estate.EstateInventory.PortRecord;
import com.eqixiac.equinix.design.estate.EstatePricing;
import com.eqixiac.equinix.design.estate.EstateRule;
import com.eqixiac.equinix.design.estate.FinancialImpact;
import com.eqixiac.equinix.design.estate.FindingCategory;
import com.eqixiac.equinix.design.estate.RuleContext;
import com.eqixiac.equinix.design.estate.Severity;
import com.eqixiac.equinix.design.estate.UtilizationSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * {@link FindingCategory#SINGLE_POINT_OF_FAILURE}.
 *
 * <ul>
 *   <li><b>Missing secondary:</b> a live cloud ({@code SP}) connection alone in its redundancy group
 *       (impact: the price of adding a secondary of the same type and bandwidth, a negative saving).</li>
 *   <li><b>Shared attachment:</b> a redundancy group whose members all use one port, one Cloud
 *       Router or one Network Edge device (HIGH), or ports on one Equinix switch (MEDIUM).</li>
 *   <li><b>One path to a cloud:</b> two or more live connections to one service profile that all
 *       leave from one port/router/device (HIGH) or from one metro (LOW).</li>
 *   <li><b>Single device:</b> a provisioned Network Edge device with no redundant peer.</li>
 * </ul>
 * <p>A secondary that exists outside the analysed scope cannot be seen; findings say so.</p>
 */
public final class SinglePointOfFailureRule implements EstateRule {

    @Override
    public String name() {
        return "single-point-of-failure";
    }

    @Override
    public List<EstateFinding> evaluate(EstateInventory inv, UtilizationSnapshot util, RuleContext ctx) {
        List<EstateFinding> out = new ArrayList<>();
        List<ConnectionRecord> live = inv.connections().stream().filter(ConnectionRecord::isLive).toList();
        Map<String, List<ConnectionRecord>> groups = new TreeMap<>();
        for (ConnectionRecord c : live) {
            String g = c.redundancyGroup() != null ? c.redundancyGroup() : "solo:" + c.id();
            groups.computeIfAbsent(g, k -> new ArrayList<>()).add(c);
        }
        String outOfScope = "a secondary in another project or account is not visible to this analysis";
        groups.forEach((group, members) -> {
            if (members.size() == 1) {
                ConnectionRecord c = members.get(0);
                if ("SP".equals(c.zSide().type())) {
                    EstatePricing.Priced add = ctx.pricing().connection("secondary for " + c.id(), c.type(),
                            c.bandwidthMbps(), c.pricingMetro(), EstatePricing.termOf(c.termMonths()));
                    out.add(new EstateFinding(RuleSupport.id(FindingCategory.SINGLE_POINT_OF_FAILURE,
                            "missing-secondary", List.of(c.id())), FindingCategory.SINGLE_POINT_OF_FAILURE,
                            Severity.MEDIUM, "Cloud connection without a secondary", List.of(RuleSupport.ref(c)),
                            List.of(RuleSupport.inventory("members in redundancy group", 1, null, inv),
                                    RuleSupport.inventory("redundancy priority", c.redundancyPriority(), null, inv),
                                    RuleSupport.inventory("cloud service profile", profile(c), null, inv)),
                            "Add a SECONDARY connection to " + profile(c) + " in the redundancy group of "
                                    + RuleSupport.label(c.name(), c.id()) + ", from a different port (ideally a "
                                    + "different metro)",
                            add.isPriced() ? FinancialImpact.addedCost(add.line(), RuleSupport.priceNote(add))
                                    : FinancialImpact.unpriced(add.reason()),
                            Confidence.MEDIUM, List.of(outOfScope, "the secondary is priced like the primary"),
                            name()));
                }
                return;
            }
            sharedAttachment(out, members, inv);
        });
        Map<String, List<ConnectionRecord>> byProfile = new TreeMap<>();
        for (ConnectionRecord c : live) {
            if ("SP".equals(c.zSide().type()) && profileKey(c) != null) {
                byProfile.computeIfAbsent(profileKey(c), k -> new ArrayList<>()).add(c);
            }
        }
        byProfile.forEach((key, conns) -> {
            if (conns.size() < 2) {
                return;
            }
            Set<String> attach = conns.stream().map(SinglePointOfFailureRule::attachment).filter(Objects::nonNull)
                    .collect(Collectors.toCollection(TreeSet::new));
            Set<String> metros = conns.stream().map(c -> c.aSide().metro()).filter(Objects::nonNull)
                    .collect(Collectors.toCollection(TreeSet::new));
            List<ResourceRef> refs = conns.stream().map(RuleSupport::ref).toList();
            List<String> ids = conns.stream().map(ConnectionRecord::id).toList();
            String name = profile(conns.get(0));
            if (attach.size() == 1 && conns.stream().allMatch(c -> attachment(c) != null)) {
                out.add(new EstateFinding(RuleSupport.id(FindingCategory.SINGLE_POINT_OF_FAILURE, "one-attachment",
                        ids), FindingCategory.SINGLE_POINT_OF_FAILURE, Severity.HIGH,
                        "Every connection to " + name + " leaves from one " + attach.iterator().next().split(":")[0],
                        refs, List.of(RuleSupport.inventory("connections to the service", conns.size(), null, inv),
                        RuleSupport.inventory("distinct A-side attachments", 1, null, inv)),
                        "Move one connection to " + name + " to a second port, router or device, ideally in a "
                                + "second metro",
                        FinancialImpact.unpriced("moving a connection changes no Fabric price, but a second port "
                                + "(not in the rate card) may be needed"),
                        Confidence.HIGH, List.of(outOfScope), name()));
            } else if (metros.size() == 1) {
                out.add(new EstateFinding(RuleSupport.id(FindingCategory.SINGLE_POINT_OF_FAILURE, "one-metro", ids),
                        FindingCategory.SINGLE_POINT_OF_FAILURE, Severity.LOW,
                        "Every connection to " + name + " leaves from metro " + metros.iterator().next(), refs,
                        List.of(RuleSupport.inventory("connections to the service", conns.size(), null, inv),
                                RuleSupport.inventory("distinct A-side metros", 1, null, inv)),
                        "Consider a connection to " + name + " from a second metro to survive a metro-wide event",
                        FinancialImpact.unpriced("the cost depends on presence in a second metro"),
                        Confidence.MEDIUM, List.of(outOfScope), name()));
            }
        });
        for (DeviceRecord d : inv.devices()) {
            boolean single = (d.redundancyType() == null || "NONE".equals(d.redundancyType())) && d.redundantId() == null;
            if ("PROVISIONED".equals(d.status()) && single) {
                out.add(new EstateFinding(RuleSupport.id(FindingCategory.SINGLE_POINT_OF_FAILURE, "single-device",
                        List.of(d.id())), FindingCategory.SINGLE_POINT_OF_FAILURE, Severity.MEDIUM,
                        "Network Edge device without a redundant peer", List.of(RuleSupport.ref(d)),
                        List.of(RuleSupport.inventory("redundancy type", d.redundancyType(), null, inv)),
                        "Add a secondary device (or a cluster) for " + RuleSupport.label(d.name(), d.id()),
                        FinancialImpact.unpriced("the rate card has no Network Edge device prices"),
                        Confidence.HIGH, List.of(), name()));
            }
        }
        return out;
    }

    private void sharedAttachment(List<EstateFinding> out, List<ConnectionRecord> members, EstateInventory inv) {
        Set<String> attach = members.stream().map(SinglePointOfFailureRule::attachment)
                .collect(Collectors.toCollection(TreeSet::new));
        List<ResourceRef> refs = members.stream().map(RuleSupport::ref).toList();
        List<String> ids = members.stream().map(ConnectionRecord::id).toList();
        if (attach.size() == 1 && members.stream().allMatch(c -> attachment(c) != null)) {
            String a = attach.iterator().next();
            out.add(new EstateFinding(RuleSupport.id(FindingCategory.SINGLE_POINT_OF_FAILURE, "pair-same-attachment",
                    ids), FindingCategory.SINGLE_POINT_OF_FAILURE, Severity.HIGH,
                    "Both members of a redundant pair use the same " + a.split(":")[0], refs,
                    List.of(RuleSupport.inventory("shared A-side attachment", a, null, inv)),
                    "Move the SECONDARY connection to a different " + a.split(":")[0]
                            + " (ideally a different metro)",
                    FinancialImpact.unpriced("moving a connection changes no Fabric price, but a second port "
                            + "(not in the rate card) may be needed"),
                    Confidence.HIGH, List.of(), name()));
            return;
        }
        Set<String> switches = new TreeSet<>();
        for (ConnectionRecord c : members) {
            PortRecord p = inv.port(c.aSide().portId()).orElse(null);
            if (p == null || p.deviceName() == null) {
                return;
            }
            switches.add(p.deviceName());
        }
        if (switches.size() == 1) {
            out.add(new EstateFinding(RuleSupport.id(FindingCategory.SINGLE_POINT_OF_FAILURE, "pair-same-switch", ids),
                    FindingCategory.SINGLE_POINT_OF_FAILURE, Severity.MEDIUM,
                    "Both members of a redundant pair use ports on one Equinix switch", refs,
                    List.of(RuleSupport.inventory("shared Equinix switch", switches.iterator().next(), null, inv)),
                    "Move the SECONDARY connection to a port on a different Equinix switch (a secondary port)",
                    FinancialImpact.unpriced("a secondary port is not in the rate card"), Confidence.MEDIUM,
                    List.of("switch identity comes from the port's device name"), name()));
        }
    }

    private static String attachment(ConnectionRecord c) {
        if (c.aSide().portId() != null) {
            return "port:" + c.aSide().portId();
        }
        if (c.aSide().routerId() != null) {
            return "cloudRouter:" + c.aSide().routerId();
        }
        if (c.aSide().deviceId() != null) {
            return "device:" + c.aSide().deviceId();
        }
        return null;
    }

    private static String profileKey(ConnectionRecord c) {
        return c.zSide().serviceProfileId() != null ? c.zSide().serviceProfileId() : c.zSide().serviceProfileName();
    }

    private static String profile(ConnectionRecord c) {
        String n = c.zSide().serviceProfileName();
        String id = c.zSide().serviceProfileId();
        return n != null ? n : id != null ? "service profile " + id : "the service provider";
    }
}
