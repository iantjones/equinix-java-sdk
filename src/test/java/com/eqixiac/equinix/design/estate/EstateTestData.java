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

package com.eqixiac.equinix.design.estate;

import com.eqixiac.equinix.core.enums.MetroCode;
import com.eqixiac.equinix.design.estate.EstateInventory.ConnectionRecord;
import com.eqixiac.equinix.design.estate.EstateInventory.DeviceRecord;
import com.eqixiac.equinix.design.estate.EstateInventory.Endpoint;
import com.eqixiac.equinix.design.estate.EstateInventory.PortRecord;
import com.eqixiac.equinix.design.estate.EstateInventory.RouterRecord;
import com.eqixiac.equinix.design.value.ratecard.CustomRateCard;
import com.eqixiac.equinix.design.value.ratecard.RateCard;
import com.eqixiac.equinix.design.value.ratecard.Term;
import com.eqixiac.equinix.fabric.enums.ConnectionType;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Hand-built inventories, utilisation and prices for the estate tests (synthetic values). */
public final class EstateTestData {

    public static final Instant AS_OF = Instant.parse("2026-11-12T00:00:00Z");
    public static final String PROJECT = "11111111-1111-1111-1111-111111111111";

    private EstateTestData() {
    }

    public static EstateInventory.Builder inventory() {
        return EstateInventory.builder(EstateScope.projects(PROJECT), AS_OF).source("test");
    }

    public static Instant daysAgo(long days) {
        return AS_OF.minus(Duration.ofDays(days));
    }

    public static Endpoint colo(String portId, String metro) {
        return new Endpoint("COLO", metro, portId, null, null, null, null, null, null);
    }

    public static Endpoint router(String routerId, String metro) {
        return new Endpoint("CLOUD_ROUTER", metro, null, routerId, null, null, null, null, null);
    }

    public static Endpoint device(String deviceId, String metro) {
        return new Endpoint("VD", metro, null, null, deviceId, null, null, null, null);
    }

    public static Endpoint cloud(String profileId, String profileName, String metro) {
        return new Endpoint("SP", metro, null, null, null, null, profileId, profileName, null);
    }

    /** A live, on-demand EVPL connection from a port to a cloud, 200 days old, with a PO and notifications. */
    public static ConnectionRecord conn(String id, int mbps, Endpoint a, Endpoint z) {
        return new ConnectionRecord(id, "conn-" + id, "EVPL_VC", "ACTIVE", "PROVISIONED", "AVAILABLE", mbps, a, z, 1,
                "group-" + id, "PRIMARY", daysAgo(200), PROJECT, "100", "PO-1", 1);
    }

    public static ConnectionRecord withState(ConnectionRecord c, String state, String equinixStatus, Instant created) {
        return new ConnectionRecord(c.id(), c.name(), c.type(), state, equinixStatus, c.providerStatus(),
                c.bandwidthMbps(), c.aSide(), c.zSide(), c.termMonths(), c.redundancyGroup(), c.redundancyPriority(),
                created, c.projectId(), c.accountNumber(), c.purchaseOrder(), c.notificationCount());
    }

    public static ConnectionRecord withGroup(ConnectionRecord c, String group, String priority) {
        return new ConnectionRecord(c.id(), c.name(), c.type(), c.state(), c.equinixStatus(), c.providerStatus(),
                c.bandwidthMbps(), c.aSide(), c.zSide(), c.termMonths(), group, priority, c.createdAt(),
                c.projectId(), c.accountNumber(), c.purchaseOrder(), c.notificationCount());
    }

    public static ConnectionRecord withTerm(ConnectionRecord c, Integer termMonths, Instant created) {
        return new ConnectionRecord(c.id(), c.name(), c.type(), c.state(), c.equinixStatus(), c.providerStatus(),
                c.bandwidthMbps(), c.aSide(), c.zSide(), termMonths, c.redundancyGroup(), c.redundancyPriority(),
                created, c.projectId(), c.accountNumber(), c.purchaseOrder(), c.notificationCount());
    }

    public static ConnectionRecord withTags(ConnectionRecord c, String po, Integer notifications) {
        return new ConnectionRecord(c.id(), c.name(), c.type(), c.state(), c.equinixStatus(), c.providerStatus(),
                c.bandwidthMbps(), c.aSide(), c.zSide(), c.termMonths(), c.redundancyGroup(), c.redundancyPriority(),
                c.createdAt(), c.projectId(), c.accountNumber(), po, notifications);
    }

    public static PortRecord port(String id, String metro, int speed, Integer used, Integer connections,
                                  String deviceName) {
        return new PortRecord(id, "port-" + id, metro, "ACTIVE", speed, used, used == null ? null : speed - used,
                "DOT1Q", false, 1, "PRIMARY", null, deviceName, connections, PROJECT, "100", daysAgo(400), 1);
    }

    public static RouterRecord router(String id, String pkg, String metro, Integer connections, int ageDays) {
        return new RouterRecord(id, "router-" + id, pkg, metro, "PROVISIONED", connections, null, daysAgo(ageDays),
                PROJECT, "100", 1);
    }

    public static DeviceRecord device(String id, String redundancyType, String redundantId) {
        return new DeviceRecord(id, "device-" + id, "C8000V", "DC", "PROVISIONED", redundancyType, redundantId, 12,
                daysAgo(300), "100");
    }

    /** Utilisation with the given p95-ish level: 168 hourly samples, flat at {@code mbps} with one peak. */
    public static UtilizationSnapshot.Stats flat(String id, int provisioned, double mbps, double peak, int samples) {
        List<Double> s = new ArrayList<>();
        for (int i = 0; i < samples - 1; i++) {
            s.add(mbps);
        }
        s.add(peak);
        return UtilizationSnapshot.Stats.reduce(id, provisioned, "PT1H", s);
    }

    public static UtilizationSnapshot util(UtilizationSnapshot.Stats... stats) {
        Map<String, UtilizationSnapshot.Stats> m = new java.util.TreeMap<>();
        for (UtilizationSnapshot.Stats s : stats) {
            m.put(s.resourceId(), s);
        }
        return new UtilizationSnapshot(daysAgo(7), AS_OF, "test metrics", m, List.of());
    }

    /** EVPL prices: 50:100, 100:120, 200:150, 500:200, 1000:300, 2000:500 USD/month; routers STANDARD 1500, LAB 0, ADVANCED 3000. */
    public static RateCard customCard() {
        CustomRateCard.Builder b = CustomRateCard.builder();
        int[][] rows = {{50, 100}, {100, 120}, {200, 150}, {500, 200}, {1000, 300}, {2000, 500}};
        for (int[] r : rows) {
            b.connectionRate(ConnectionType.EVPL_VC, r[0], BigDecimal.valueOf(r[1]));
            b.connectionRate(ConnectionType.IP_VC, r[0], BigDecimal.valueOf(r[1]));
        }
        b.cloudRouterRate("STANDARD", BigDecimal.valueOf(1500));
        b.cloudRouterRate("LAB", BigDecimal.ZERO);
        b.cloudRouterRate("ADVANCED", BigDecimal.valueOf(3000));
        return b.build();
    }

    /** A card with term-specific EVPL 1000 Mbps prices in DC: 300 on demand, 270 / 240 / 210 (setup 90 on 36 months). */
    public static RateCard termCard() {
        return CustomRateCard.builder()
                .connectionRate(ConnectionType.EVPL_VC, 1000, MetroCode.DC, Term.MONTH_1, BigDecimal.valueOf(300))
                .connectionRate(ConnectionType.EVPL_VC, 1000, MetroCode.DC, Term.MONTH_12, BigDecimal.valueOf(270))
                .connectionRate(ConnectionType.EVPL_VC, 1000, MetroCode.DC, Term.MONTH_24, BigDecimal.valueOf(240))
                .connectionRate(ConnectionType.EVPL_VC, 1000, MetroCode.DC, Term.MONTH_36, BigDecimal.valueOf(210),
                        BigDecimal.valueOf(270))
                .cloudRouterRate("STANDARD", MetroCode.DC, Term.MONTH_1, BigDecimal.valueOf(1500))
                .cloudRouterRate("STANDARD", MetroCode.DC, Term.MONTH_12, BigDecimal.valueOf(1400))
                .build();
    }

    public static RuleContext ctx(RateCard card) {
        return new RuleContext(new EstatePricing(card, EstateThresholds.defaults()), EstateThresholds.defaults(), AS_OF);
    }

    public static RuleContext ctx(RateCard card, EstateThresholds t) {
        return new RuleContext(new EstatePricing(card, t), t, AS_OF);
    }
}
