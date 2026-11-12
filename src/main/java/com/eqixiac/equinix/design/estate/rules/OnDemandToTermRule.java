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
import com.eqixiac.equinix.design.value.ratecard.Term;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * {@link FindingCategory#ON_DEMAND_TO_TERM}: a live, on-demand connection or Cloud Router, older
 * than {@code minAgeForTermDays}, that is cheaper on one of the candidate terms.
 *
 * <p>Each candidate term is priced through the rate card. A term is a saving only when its monthly
 * price is lower than on-demand; the rule recommends the term with the largest monthly saving and
 * reports every priced term and the break-even month (when a term adds a one-time charge). A rate
 * card that has no term axis (the bundled reference card) prices every term the same, so it never
 * produces this finding: a term saving is only claimed from term-specific prices. A resource whose
 * age is unknown is skipped. When on-demand or every term is unpriced the rule says so in an
 * unpriced, low-confidence finding.</p>
 */
public final class OnDemandToTermRule implements EstateRule {

    @Override
    public String name() {
        return "on-demand-to-term";
    }

    @Override
    public List<EstateFinding> evaluate(EstateInventory inv, UtilizationSnapshot util, RuleContext ctx) {
        List<EstateFinding> out = new ArrayList<>();
        int minAge = ctx.thresholds().minAgeForTermDays();
        for (ConnectionRecord c : inv.connections()) {
            long age = ctx.ageDays(c.createdAt());
            if (!c.isLive() || !c.isOnDemand() || age < minAge || c.bandwidthMbps() == null) {
                continue;
            }
            evaluateOne(out, RuleSupport.ref(c), "connection", age,
                    term -> ctx.pricing().connection("connection " + c.id() + " " + c.bandwidthMbps() + " Mbps " + term,
                            c.type(), c.bandwidthMbps(), c.pricingMetro(), term), ctx, inv);
        }
        for (RouterRecord r : inv.routers()) {
            long age = ctx.ageDays(r.createdAt());
            if (!r.isLive() || (r.termMonths() != null && r.termMonths() > 1) || age < minAge) {
                continue;
            }
            evaluateOne(out, RuleSupport.ref(r), "cloudRouter", age,
                    term -> ctx.pricing().router("Cloud Router " + r.id() + " " + r.packageCode() + " " + term,
                            r.packageCode(), r.metro(), term), ctx, inv);
        }
        return out;
    }

    private void evaluateOne(List<EstateFinding> out, ResourceRef ref, String kind, long age,
                             Function<Term, EstatePricing.Priced> price, RuleContext ctx, EstateInventory inv) {
        EstatePricing.Priced onDemand = price.apply(Term.MONTH_1);
        List<Evidence> evidence = new ArrayList<>();
        evidence.add(RuleSupport.inventory("age", age, "days", inv));
        evidence.add(RuleSupport.inventory("current term", "on-demand (1 month)", null, inv));
        List<String> assumptions = new ArrayList<>(List.of(
                "a term commitment cannot be reduced or cancelled without early-termination charges",
                "only resources older than " + ctx.thresholds().minAgeForTermDays()
                        + " days are considered, as a proxy for a stable need"));
        String id = RuleSupport.id(FindingCategory.ON_DEMAND_TO_TERM, kind, List.of(ref.id()));
        if (!onDemand.isPriced()) {
            out.add(new EstateFinding(id, FindingCategory.ON_DEMAND_TO_TERM, Severity.INFO,
                    "On-demand " + kind + " older than " + ctx.thresholds().minAgeForTermDays() + " days",
                    List.of(ref), evidence, "Request term pricing for " + kind + " "
                    + RuleSupport.label(ref.name(), ref.id()) + " and compare it with on-demand",
                    FinancialImpact.unpriced(onDemand.reason()), Confidence.LOW, assumptions, name()));
            return;
        }
        evidence.add(new Evidence("on-demand price", onDemand.line().monthly().toPlainString(),
                onDemand.line().currency() + "/month", onDemand.line().source().name(), null));
        Term best = null;
        EstatePricing.Priced bestPrice = null;
        BigDecimal bestSaving = BigDecimal.ZERO;
        boolean anyPriced = false;
        for (Term term : ctx.thresholds().candidateTerms()) {
            EstatePricing.Priced p = price.apply(term);
            if (!p.isPriced() || !p.line().currency().equals(onDemand.line().currency())) {
                continue;
            }
            anyPriced = true;
            BigDecimal saving = onDemand.line().monthly().subtract(p.line().monthly());
            evidence.add(new Evidence(term.months() + "-month price", p.line().monthly().toPlainString(),
                    p.line().currency() + "/month", p.line().source().name(), p.line().note()));
            if (saving.compareTo(bestSaving) > 0) {
                best = term;
                bestPrice = p;
                bestSaving = saving;
            }
        }
        if (best == null) {
            if (!anyPriced) {
                out.add(new EstateFinding(id, FindingCategory.ON_DEMAND_TO_TERM, Severity.INFO,
                        "On-demand " + kind + ": no term price available", List.of(ref), evidence,
                        "Request term pricing for " + kind + " " + RuleSupport.label(ref.name(), ref.id()),
                        FinancialImpact.unpriced("no candidate term could be priced"), Confidence.LOW, assumptions,
                        name()));
            }
            return;
        }
        FinancialImpact impact = FinancialImpact.between(onDemand.line(), bestPrice.line(),
                RuleSupport.priceNote(onDemand, bestPrice));
        BigDecimal extraOnce = bestPrice.line().oneTime().subtract(onDemand.line().oneTime());
        long breakEven = extraOnce.signum() <= 0 ? 1
                : extraOnce.divide(bestSaving, 0, RoundingMode.CEILING).longValue();
        evidence.add(new Evidence("break-even month", String.valueOf(breakEven), "month", "derived", null));
        BigDecimal overTerm = bestSaving.multiply(BigDecimal.valueOf(best.months())).subtract(extraOnce.max(BigDecimal.ZERO));
        evidence.add(new Evidence("saving over the term", overTerm.setScale(2, RoundingMode.HALF_UP).toPlainString(),
                onDemand.line().currency(), "derived", null));
        Confidence conf = RuleSupport.allExact(onDemand, bestPrice) ? Confidence.HIGH : Confidence.MEDIUM;
        Severity sev = bestSaving.compareTo(BigDecimal.valueOf(500)) >= 0 ? Severity.HIGH
                : bestSaving.compareTo(BigDecimal.valueOf(50)) >= 0 ? Severity.MEDIUM : Severity.LOW;
        out.add(new EstateFinding(id, FindingCategory.ON_DEMAND_TO_TERM, sev,
                "On-demand " + kind + " would cost less on a " + best.months() + "-month term", List.of(ref),
                evidence, "Move " + kind + " " + RuleSupport.label(ref.name(), ref.id()) + " from on-demand to a "
                + best.months() + "-month term (break-even in month " + breakEven + ")",
                impact, conf, assumptions, name()));
    }
}
