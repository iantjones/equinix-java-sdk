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
import com.eqixiac.equinix.design.estate.EstatePricing;
import com.eqixiac.equinix.design.estate.EstateRule;
import com.eqixiac.equinix.design.estate.EstateThresholds;
import com.eqixiac.equinix.design.estate.FinancialImpact;
import com.eqixiac.equinix.design.estate.FindingCategory;
import com.eqixiac.equinix.design.estate.RuleContext;
import com.eqixiac.equinix.design.estate.Severity;
import com.eqixiac.equinix.design.estate.UtilizationSnapshot;
import com.eqixiac.equinix.design.value.ratecard.Term;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * {@link FindingCategory#RIGHT_SIZE_DOWN} and {@link FindingCategory#RIGHT_SIZE_UP}: a live
 * connection whose 95th-percentile use is far below (or close to) its provisioned bandwidth.
 *
 * <p>The target tier is the smallest of {@link EstateThresholds#bandwidthTiersMbps()} at or above
 * p95 × {@link EstateThresholds#headroomFactor()}. Down: p95 below
 * {@link EstateThresholds#rightSizeDownBelowPercent()} and the target is smaller than today's
 * bandwidth. Up: p95 above {@link EstateThresholds#rightSizeUpAbovePercent()} or the peak reached the
 * provisioned bandwidth; the target is the next tier that gives the headroom. The monthly price
 * difference uses the connection's current term. Connections without enough samples
 * ({@link EstateThresholds#minUtilizationSamples()}) are skipped: no data, no recommendation. A
 * connection with no traffic at all is left to {@link IdleResourceRule}.</p>
 */
public final class RightSizeRule implements EstateRule {

    @Override
    public String name() {
        return "right-size";
    }

    @Override
    public List<EstateFinding> evaluate(EstateInventory inv, UtilizationSnapshot util, RuleContext ctx) {
        EstateThresholds t = ctx.thresholds();
        List<EstateFinding> out = new ArrayList<>();
        for (ConnectionRecord c : inv.connections()) {
            if (!c.isLive() || c.bandwidthMbps() == null || c.bandwidthMbps() <= 0) {
                continue;
            }
            Optional<UtilizationSnapshot.Stats> st = util.known(c.id());
            if (st.isEmpty() || st.get().samples() < t.minUtilizationSamples()) {
                continue;
            }
            UtilizationSnapshot.Stats s = st.get();
            if (s.peakMbps() == 0.0) {
                continue; // no traffic at all: IdleResourceRule reports it, so the savings are not counted twice
            }
            double pct = s.p95Percent().orElse(Double.NaN);
            if (Double.isNaN(pct)) {
                continue;
            }
            int current = c.bandwidthMbps();
            double needed = s.p95Mbps() * t.headroomFactor();
            boolean saturated = s.peakPercent().orElse(0.0) >= 100.0;
            if (pct < t.rightSizeDownBelowPercent()) {
                Integer target = t.tierAtOrAbove(needed);
                if (target != null && target < current) {
                    out.add(finding(FindingCategory.RIGHT_SIZE_DOWN, c, current, target, s, util, inv, ctx, pct));
                }
            } else if (pct > t.rightSizeUpAbovePercent() || saturated) {
                Integer target = t.tierAtOrAbove(Math.max(needed, current + 1));
                out.add(finding(FindingCategory.RIGHT_SIZE_UP, c, current, target, s, util, inv, ctx, pct));
            }
        }
        return out;
    }

    private EstateFinding finding(FindingCategory cat, ConnectionRecord c, int current, Integer target,
                                  UtilizationSnapshot.Stats s, UtilizationSnapshot util, EstateInventory inv,
                                  RuleContext ctx, double pct) {
        EstateThresholds t = ctx.thresholds();
        Term term = EstatePricing.termOf(c.termMonths());
        String metro = c.pricingMetro();
        EstatePricing.Priced now = ctx.pricing().connection("connection " + c.id() + " at " + current + " Mbps",
                c.type(), current, metro, term);
        FinancialImpact impact;
        String action;
        EstatePricing.Priced next = null;
        if (target == null) {
            action = "Connection " + RuleSupport.label(c.name(), c.id()) + " runs at " + RuleSupport.fmt(pct)
                    + "% of " + current + " Mbps at p95 and no listed tier gives " + t.headroomFactor()
                    + "x headroom: add a parallel connection or move to a larger port";
            impact = FinancialImpact.unpriced("no bandwidth tier above " + current + " Mbps is listed");
        } else {
            next = ctx.pricing().connection("connection " + c.id() + " at " + target + " Mbps",
                    c.type(), target, metro, term);
            action = (cat == FindingCategory.RIGHT_SIZE_DOWN ? "Reduce" : "Increase") + " connection "
                    + RuleSupport.label(c.name(), c.id()) + " from " + current + " to " + target + " Mbps";
            if (now.isPriced() && next.isPriced()) {
                impact = FinancialImpact.between(now.line(), next.line(), RuleSupport.priceNote(now, next));
            } else {
                impact = FinancialImpact.unpriced(now.isPriced() ? next.reason() : now.reason());
            }
        }
        Confidence conf = s.samples() >= 7 * 24 ? Confidence.HIGH : Confidence.MEDIUM;
        if (impact.isPriced() && !RuleSupport.allExact(now, next)) {
            conf = conf.min(Confidence.MEDIUM);
        }
        Severity sev;
        if (cat == FindingCategory.RIGHT_SIZE_UP) {
            sev = s.peakPercent().orElse(0.0) >= 100.0 ? Severity.HIGH : Severity.MEDIUM;
        } else if (impact.isPriced()) {
            BigDecimal m = impact.monthlySaving();
            sev = m.compareTo(BigDecimal.valueOf(1000)) >= 0 ? Severity.HIGH
                    : m.compareTo(BigDecimal.valueOf(100)) >= 0 ? Severity.MEDIUM : Severity.LOW;
        } else {
            sev = Severity.LOW;
        }
        String title = (cat == FindingCategory.RIGHT_SIZE_DOWN ? "Over-provisioned" : "Near-saturated")
                + " connection: p95 " + RuleSupport.fmt(pct) + "% of " + current + " Mbps";
        List<String> assumptions = List.of(
                "each sample is the larger of transmit and receive for the interval",
                "target tier is the smallest listed tier at or above p95 x " + t.headroomFactor(),
                "listed tiers: " + t.bandwidthTiersMbps() + " Mbps (the service on the far side must also support the target)",
                "priced on the connection's current term " + term);
        return new EstateFinding(RuleSupport.id(cat, "connection", List.of(c.id())), cat, sev, title,
                List.of(RuleSupport.ref(c)), RuleSupport.utilisation(s, util), action, impact, conf, assumptions,
                name());
    }
}
