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

package com.eqixiac.equinix.design.objective;

import com.eqixiac.equinix.design.spi.ValueScale;
import com.eqixiac.equinix.design.cost.CostComparability;
import com.eqixiac.equinix.design.spi.CostModel;
import com.eqixiac.equinix.design.spi.DesignContext;
import com.eqixiac.equinix.design.spi.Direction;
import com.eqixiac.equinix.design.spi.MetroPortfolio;
import com.eqixiac.equinix.design.spi.Objective;
import com.eqixiac.equinix.design.spi.ObjectiveValue;
import com.eqixiac.equinix.design.spi.PortfolioCost;

import java.math.BigDecimal;

/**
 * Expected monthly total of the portfolio from the run's {@link CostModel}: the same figure the result
 * reports. Unknown when anything is unpriced (the total would understate the cost, and comparing
 * understated totals rewards missing data), when any line is in a currency other than the spec's
 * {@code commercial.currency} (comparing an all-EUR total with an all-USD one would rank by currency),
 * or when there is no cost model; the explanation names the missing items or exchange rates.
 */
public final class CostObjective implements Objective {

    @Override
    public String id() {
        return COST;
    }

    @Override
    public Direction direction() {
        return Direction.MINIMIZE;
    }

    /**
     * Scored by ratio to the cheapest feasible design: 7% dearer scores about 93, twice as dear 50.
     * (A spread-based scale turned a 7% difference into 60 points and let a far-away metro win on a
     * trivial saving; found live on 2026-11-12.)
     */
    @Override
    public ValueScale scale(DesignContext context) {
        return ValueScale.ratioToBest();
    }

    @Override
    public String unit() {
        return "per month";
    }

    @Override
    public ObjectiveValue evaluate(MetroPortfolio portfolio, DesignContext context) {
        CostModel model = context.costModel();
        if (model == null) {
            return ObjectiveValue.unknown("no cost model in this run");
        }
        PortfolioCost cost = model.price(portfolio, context);
        if (!cost.isComplete()) {
            return ObjectiveValue.unknown(cost.unpriced().size() + " item(s) unpriced: "
                    + String.join("; ", cost.unpriced()));
        }
        if (cost.lines().isEmpty()) {
            return ObjectiveValue.of(0, "nothing in this design is chargeable by the cost model");
        }
        String currency = context.spec().commercial().currencyOrDefault();
        if (cost.currency().isEmpty() || !cost.currency().get().equals(currency)) {
            // An all-EUR total compared numerically with an all-USD total would rank by currency, not cost.
            return ObjectiveValue.unknown("lines are priced in " + cost.currencies() + " but costs are compared in "
                    + currency + "; missing exchange rate(s) for " + String.join(", ",
                    CostComparability.missingRates(cost, currency)) + " (supply commercial.exchangeRates)");
        }
        BigDecimal total = cost.monthlyTotal().orElseThrow();
        return ObjectiveValue.of(total.doubleValue(), cost.currency().get() + " " + total.toPlainString()
                + "/month (range " + cost.monthlyLow().orElseThrow().toPlainString() + "-"
                + cost.monthlyHigh().orElseThrow().toPlainString() + ") over " + cost.lines().size() + " line(s)");
    }
}
