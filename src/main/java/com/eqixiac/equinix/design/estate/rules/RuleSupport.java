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

import com.eqixiac.equinix.design.estate.EstateFinding;
import com.eqixiac.equinix.design.estate.EstateFinding.Evidence;
import com.eqixiac.equinix.design.estate.EstateFinding.ResourceRef;
import com.eqixiac.equinix.design.estate.EstateInventory;
import com.eqixiac.equinix.design.estate.EstateInventory.ConnectionRecord;
import com.eqixiac.equinix.design.estate.EstatePricing;
import com.eqixiac.equinix.design.estate.UtilizationSnapshot;
import com.eqixiac.equinix.design.spi.CostLine;
import com.eqixiac.equinix.design.value.ratecard.PriceSource;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

/** Small helpers shared by the default rules. */
final class RuleSupport {

    static final String INVENTORY = "inventory (Fabric v4 API)";

    private RuleSupport() {
    }

    static ResourceRef ref(ConnectionRecord c) {
        return new ResourceRef("connection", c.id(), c.name());
    }

    static ResourceRef ref(EstateInventory.PortRecord p) {
        return new ResourceRef("port", p.id(), p.name());
    }

    static ResourceRef ref(EstateInventory.RouterRecord r) {
        return new ResourceRef("cloudRouter", r.id(), r.name());
    }

    static ResourceRef ref(EstateInventory.NetworkRecord n) {
        return new ResourceRef("network", n.id(), n.name());
    }

    static ResourceRef ref(EstateInventory.ServiceTokenRecord t) {
        return new ResourceRef("serviceToken", t.id(), t.name());
    }

    static ResourceRef ref(EstateInventory.DeviceRecord d) {
        return new ResourceRef("device", d.id(), d.name());
    }

    static String label(String name, String id) {
        return (name == null || name.isBlank() ? "" : name + " ") + "(" + id + ")";
    }

    static Evidence inventory(String label, Object value, String unit, EstateInventory inv) {
        return new Evidence(label, String.valueOf(value), unit, inv.source(), "as of " + inv.asOf());
    }

    static String fmt(double v) {
        return String.format(Locale.ROOT, "%.1f", v);
    }

    static String windowOf(UtilizationSnapshot u) {
        return u.windowStart() + " .. " + u.windowEnd();
    }

    static List<Evidence> utilisation(UtilizationSnapshot.Stats s, UtilizationSnapshot u) {
        String src = u.source();
        String w = windowOf(u);
        return List.of(
                new Evidence("provisioned bandwidth", String.valueOf(s.provisionedMbps()), "Mbps", INVENTORY, null),
                new Evidence("p50 utilisation", fmt(s.p50Mbps()), "Mbps", src, w),
                new Evidence("p95 utilisation", fmt(s.p95Mbps()), "Mbps", src, w),
                new Evidence("peak utilisation", fmt(s.peakMbps()), "Mbps", src, w),
                new Evidence("p95 as percent of provisioned", s.p95Percent().map(RuleSupport::fmt).orElse("unknown"),
                        "%", src, w),
                new Evidence("samples", String.valueOf(s.samples()), s.interval(), src, w));
    }

    static String priceNote(EstatePricing.Priced... priced) {
        StringBuilder b = new StringBuilder();
        for (EstatePricing.Priced p : priced) {
            if (p != null && p.isPriced() && p.line().note() != null) {
                b.append(b.isEmpty() ? "" : "; ").append(p.line().note());
            }
        }
        return b.isEmpty() ? null : b.toString();
    }

    static boolean allExact(EstatePricing.Priced... priced) {
        for (EstatePricing.Priced p : priced) {
            if (p == null || !p.isExact()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Sums cost lines of one currency into one line; returns {@code null} when any is missing or
     * the currencies differ.
     */
    static CostLine sum(String label, List<EstatePricing.Priced> items) {
        if (items.isEmpty()) {
            return null;
        }
        String currency = null;
        PriceSource source = null;
        BigDecimal m = BigDecimal.ZERO;
        BigDecimal lo = BigDecimal.ZERO;
        BigDecimal hi = BigDecimal.ZERO;
        BigDecimal once = BigDecimal.ZERO;
        for (EstatePricing.Priced p : items) {
            if (p == null || !p.isPriced()) {
                return null;
            }
            CostLine l = p.line();
            if (currency == null) {
                currency = l.currency();
                source = l.source();
            } else if (!currency.equals(l.currency())) {
                return null;
            } else if (source != l.source()) {
                source = PriceSource.COMPOSITE;
            }
            m = m.add(l.monthly());
            lo = lo.add(l.monthlyLow());
            hi = hi.add(l.monthlyHigh());
            once = once.add(l.oneTime());
        }
        return new CostLine(label, null, m, once, currency, source, lo, hi, null);
    }

    static String firstReason(List<EstatePricing.Priced> items) {
        return items.stream().filter(p -> p != null && !p.isPriced()).map(EstatePricing.Priced::reason).findFirst()
                .orElse("prices are in different currencies; no conversion is made");
    }

    static String id(com.eqixiac.equinix.design.estate.FindingCategory c, String discriminator, List<String> ids) {
        return EstateFinding.stableId(c, discriminator, ids);
    }
}
