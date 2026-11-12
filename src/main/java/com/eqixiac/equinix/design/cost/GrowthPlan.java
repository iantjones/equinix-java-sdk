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

package com.eqixiac.equinix.design.cost;

import com.eqixiac.equinix.design.value.ratecard.Term;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * A design priced month by month under traffic growth, with a commitment-term recommendation per item.
 * Produced by {@link GrowthPlanner#plan()}.
 *
 * @param specFingerprint the spec's fingerprint
 * @param currency        the currency every amount is in
 * @param annualGrowth    the annual growth rate used (fraction)
 * @param horizonMonths   the horizon
 * @param items           one plan per priced item
 * @param monthlyOnDemand the design's on-demand total per month (month 1 first, one-time charges in month 1);
 *                        a {@code null} entry is a month with an unpriced item
 * @param unpriced        items or months that could not be priced, with the reason
 * @param assumptions     every modelling assumption
 */
public record GrowthPlan(String specFingerprint, String currency, double annualGrowth, int horizonMonths,
                         List<ItemPlan> items, List<BigDecimal> monthlyOnDemand, List<String> unpriced,
                         List<String> assumptions) {

    /** Copies the lists ({@code monthlyOnDemand} may hold nulls). */
    public GrowthPlan {
        items = List.copyOf(items);
        monthlyOnDemand = java.util.Collections.unmodifiableList(new java.util.ArrayList<>(monthlyOnDemand));
        unpriced = List.copyOf(unpriced);
        assumptions = List.copyOf(assumptions);
    }

    /**
     * One item's projection and term decision.
     *
     * @param label          what is paid for
     * @param kind           CONNECTION, PROVIDER_FEE, ROUTER or EGRESS
     * @param tierByMonth    the Fabric tier (Mbps) needed each month for capacity items, -1 above the largest
     *                       tier; empty for other items
     * @param upgradeMonth   the first month the tier changes, or {@code null} when it does not within the horizon
     * @param upgradeNote    a sentence describing the upgrade, or {@code null}
     * @param options        one option per term compared (on demand only for items without a term axis)
     * @param recommended    the term with the lowest total over the horizon, or {@code null} when none priced
     * @param recommendation why
     */
    public record ItemPlan(String label, String kind, List<Integer> tierByMonth, Integer upgradeMonth,
                           String upgradeNote, List<TermOption> options, Term recommended, String recommendation) {

        /** Copies the lists. */
        public ItemPlan {
            tierByMonth = List.copyOf(tierByMonth);
            options = List.copyOf(options);
        }
    }

    /**
     * One commitment term for one item.
     *
     * @param term             the term
     * @param priced           whether every month could be priced at this term
     * @param unpricedReason   why not, or {@code null}
     * @param monthlyAtStart   month 1's monthly price
     * @param oneTime          the one-time charge
     * @param totalOverHorizon every month's price + the commitment tail + the one-time charge
     * @param tailMonths       months committed beyond the horizon
     * @param tail             the cost of those months (at the last month's price)
     * @param breakEvenMonth   for a commitment: how many months of on-demand spend equal the commitment's first-term
     *                         obligation (keep the item at least this long for the commitment to pay); {@code null}
     *                         for on demand or when on-demand spend never reaches it within the term
     */
    public record TermOption(Term term, boolean priced, String unpricedReason, BigDecimal monthlyAtStart,
                             BigDecimal oneTime, BigDecimal totalOverHorizon, int tailMonths, BigDecimal tail,
                             Integer breakEvenMonth) {

        /**
         * @param term   the term
         * @param reason why it cannot be priced
         * @return an unpriced option
         */
        public static TermOption unpriced(Term term, String reason) {
            return new TermOption(term, false, reason, null, null, null, 0, null, null);
        }
    }

    /** @return the on-demand total over the horizon, or {@code null} when a month is unpriced */
    public BigDecimal onDemandTotal() {
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal m : monthlyOnDemand) {
            if (m == null) {
                return null;
            }
            sum = sum.add(m);
        }
        return sum;
    }

    /** @return a Markdown report: items with upgrade points and term options, monthly totals, assumptions */
    public String toMarkdown() {
        StringBuilder sb = new StringBuilder("## Growth and commitment terms\n\n");
        sb.append(String.format(java.util.Locale.ROOT, "Growth %.1f%%/year over %d months, amounts in %s. Spec `%s`.%n%n",
                annualGrowth * 100, horizonMonths, currency, specFingerprint.substring(0, Math.min(12, specFingerprint.length()))));
        for (ItemPlan item : items) {
            sb.append("### ").append(item.label()).append("\n\n");
            if (item.upgradeNote() != null) {
                sb.append("- ").append(item.upgradeNote()).append("\n");
            } else if (!item.tierByMonth().isEmpty()) {
                sb.append("- stays at ").append(item.tierByMonth().get(0)).append(" Mbps through month ")
                        .append(horizonMonths).append("\n");
            }
            sb.append("- Recommended: ").append(item.recommended() == null ? "none" : item.recommended())
                    .append(" (").append(item.recommendation()).append(")\n\n");
            sb.append("| Term | Month 1 | One-time | Total over horizon | Tail | Break-even |\n|---|---:|---:|---:|---:|---:|\n");
            for (TermOption o : item.options()) {
                if (!o.priced()) {
                    sb.append("| ").append(o.term()).append(" | unpriced: ").append(o.unpricedReason())
                            .append(" | | | | |\n");
                    continue;
                }
                sb.append("| ").append(o.term()).append(" | ").append(money(o.monthlyAtStart())).append(" | ")
                        .append(money(o.oneTime())).append(" | ").append(money(o.totalOverHorizon())).append(" | ")
                        .append(o.tailMonths() == 0 ? "-" : o.tailMonths() + " mo, " + money(o.tail())).append(" | ")
                        .append(o.breakEvenMonth() == null ? "-" : "month " + o.breakEvenMonth()).append(" |\n");
            }
            sb.append("\n");
        }
        BigDecimal total = onDemandTotal();
        sb.append("**On-demand total over the horizon:** ").append(total == null ? "incomplete" : money(total))
                .append("\n\n");
        for (String u : unpriced) {
            sb.append("- Unpriced: ").append(u).append("\n");
        }
        for (String a : assumptions) {
            sb.append("- Assumption: ").append(a).append("\n");
        }
        return sb.toString();
    }

    private String money(BigDecimal v) {
        return v == null ? "n/a" : currency + " " + v.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
