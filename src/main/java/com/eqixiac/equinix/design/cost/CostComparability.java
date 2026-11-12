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

import com.eqixiac.equinix.design.spi.CostLine;
import com.eqixiac.equinix.design.spi.CostModel;
import com.eqixiac.equinix.design.spi.DesignContext;
import com.eqixiac.equinix.design.spi.Direction;
import com.eqixiac.equinix.design.spi.MetroPortfolio;
import com.eqixiac.equinix.design.spi.Objective;
import com.eqixiac.equinix.design.spi.ObjectiveValue;
import com.eqixiac.equinix.design.spi.PortfolioCost;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Whether the cost of every feasible design can be compared in the spec's currency, and what the
 * optimiser does when it cannot because an exchange rate is missing. With the built-in
 * {@link RateCardCostModel} every line is converted ({@link CurrencyConverters}), so this is a safeguard
 * for plug-in cost models.
 *
 * <h2>The rule</h2>
 * <p>{@value #RULE}</p>
 *
 * <p>Why withdraw rather than rank: the weighted score gives an unknown objective no points, so a
 * design priced in GBP (London) loses the whole cost weight to a design priced in USD (Istanbul)
 * however cheap it really is. That is a ranking decided by which metros the price API happens to quote
 * in USD, not by cost. Withdrawing cost for every design keeps the comparison fair until the caller
 * supplies the rates; each design's priced lines are still reported, and the recommendation is stated
 * to be blocked on cost.</p>
 *
 * @param currency        the currency costs must be comparable in ({@code commercial.currency})
 * @param costWeight      the weight the spec gives the cost objective
 * @param missingRates    the currencies whose rate to {@code currency} is missing, across all feasible designs
 * @param affected        keys of the feasible designs whose cost is unknown only for want of a rate
 * @param feasibleCount   how many feasible designs were examined
 * @param costWithdrawn   whether the rule removed cost from the ranking
 */
public record CostComparability(String currency, double costWeight, SortedSet<String> missingRates,
                                List<String> affected, int feasibleCount, boolean costWithdrawn) {

    /** The rule this record applies, as stated in results. */
    public static final String RULE = "When cost carries weight and at least one feasible design's cost is unknown "
            + "only because an exchange rate to the spec currency is missing, cost is withdrawn from the ranking for "
            + "every design (so no design gains or loses for data the caller has not supplied), the result carries a "
            + "finding naming the missing rates, and the affected designs are listed. The built-in RateCardCostModel "
            + "converts every line (CurrencyConverters), so this applies only to a plug-in cost model that does not.";

    /** Copies the collections. */
    public CostComparability {
        missingRates = missingRates == null ? new TreeSet<>() : new TreeSet<>(missingRates);
        affected = affected == null ? List.of() : List.copyOf(affected);
    }

    /**
     * Assesses a set of feasible designs.
     *
     * @param feasible   every feasible portfolio
     * @param context    the run's facts (spec and cost model)
     * @param costWeight the weight of the cost objective
     * @return the assessment; {@code costWithdrawn} is true when the rule applies
     */
    public static CostComparability assess(Collection<MetroPortfolio> feasible, DesignContext context,
                                           double costWeight) {
        String currency = context.spec().commercial().currencyOrDefault();
        CostModel model = context.costModel();
        SortedSet<String> missing = new TreeSet<>();
        List<String> affected = new ArrayList<>();
        if (model != null) {
            for (MetroPortfolio portfolio : feasible) {
                PortfolioCost cost = model.price(portfolio, context);
                if (unknownOnlyForExchangeRate(cost, currency)) {
                    affected.add(portfolio.key());
                    missing.addAll(missingRates(cost, currency));
                }
            }
        }
        return new CostComparability(currency, costWeight, missing, affected, feasible.size(),
                costWeight > 0 && !affected.isEmpty());
    }

    /**
     * @param cost     a priced portfolio
     * @param currency the comparison currency
     * @return every line currency other than {@code currency}, sorted
     */
    public static SortedSet<String> missingRates(PortfolioCost cost, String currency) {
        SortedSet<String> missing = new TreeSet<>();
        if (cost != null) {
            for (CostLine line : cost.lines()) {
                if (!line.currency().equals(currency)) {
                    missing.add(line.currency());
                }
            }
        }
        return missing;
    }

    /**
     * @param cost     a priced portfolio
     * @param currency the comparison currency
     * @return {@code true} when every item is priced but some line is in another currency, so an exchange
     *         rate is the only thing standing between the design and a comparable total
     */
    public static boolean unknownOnlyForExchangeRate(PortfolioCost cost, String currency) {
        return cost != null && cost.isComplete() && !cost.lines().isEmpty() && !missingRates(cost, currency).isEmpty();
    }

    /** @return {@code true} when some feasible design's cost is unknown only for want of a rate */
    public boolean incomplete() {
        return !affected.isEmpty();
    }

    /**
     * @param portfolio a design
     * @return whether it is one of the designs whose cost is unknown only for want of a rate
     */
    public boolean flags(MetroPortfolio portfolio) {
        return affected.contains(portfolio.key());
    }

    /** @return the BLOCKING finding's text, e.g. "cost comparison is incomplete: supply rates for EUR, GBP" */
    public String headline() {
        return "cost comparison is incomplete: supply rates for " + String.join(", ", missingRates) + " (to "
                + currency + ")";
    }

    /**
     * The objectives with cost replaced by a withdrawn stand-in that reports every design's cost as unknown
     * (its explanation keeps the design's own cost statement), so cost neither raises nor lowers any score.
     *
     * @param objectives the run's objectives
     * @return the objectives to rank with
     */
    public static List<Objective> withdrawCost(List<Objective> objectives) {
        List<Objective> result = new ArrayList<>();
        for (Objective objective : objectives) {
            result.add(Objective.COST.equals(objective.id()) ? new Withdrawn(objective) : objective);
        }
        return result;
    }

    /** @return a Markdown section stating the rule's outcome */
    public String toMarkdown() {
        StringBuilder sb = new StringBuilder("### Cost comparability\n\n");
        if (!incomplete()) {
            sb.append("Every feasible design's cost is comparable in ").append(currency).append(".\n");
            return sb.toString();
        }
        sb.append("**").append(headline()).append(".**\n\n");
        sb.append(affected.size()).append(" of ").append(feasibleCount).append(" feasible design(s) are priced in "
                + "full but partly in another currency: ").append(String.join(", ", affected)).append(".\n\n");
        sb.append(costWithdrawn ? "Cost was withdrawn from the ranking (weight " + costWeight + ")."
                : "Cost carries no weight in this run, so the ranking is unaffected.").append("\n\n");
        sb.append("_Rule: ").append(RULE).append("_\n");
        return sb.toString();
    }

    /** The cost objective with every value reported unknown; keeps the delegate's explanation. */
    private record Withdrawn(Objective delegate) implements Objective {

        @Override
        public String id() {
            return delegate.id();
        }

        @Override
        public Direction direction() {
            return delegate.direction();
        }

        @Override
        public String unit() {
            return delegate.unit();
        }

        @Override
        public ObjectiveValue evaluate(MetroPortfolio portfolio, DesignContext context) {
            ObjectiveValue inner = delegate.evaluate(portfolio, context);
            return ObjectiveValue.unknown("cost withdrawn from the ranking because some designs' cost cannot be "
                    + "compared without exchange rates; this design: " + inner.explanation());
        }
    }
}
