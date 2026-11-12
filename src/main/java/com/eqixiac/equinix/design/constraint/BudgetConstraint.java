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

package com.eqixiac.equinix.design.constraint;

import com.eqixiac.equinix.design.spi.PortfolioCost;
import com.eqixiac.equinix.design.spi.Constraint;
import com.eqixiac.equinix.design.spi.ConstraintVerdict;
import com.eqixiac.equinix.design.spi.DesignContext;
import com.eqixiac.equinix.design.spi.MetroPortfolio;

import java.math.BigDecimal;

/**
 * The expected monthly cost must not exceed {@code commercial.budgetMaxMonthly}. Compared in ONE
 * currency only: when the priced lines are in another currency, or span several, the budget is
 * reported as not evaluable and the design is not rejected; amounts are never converted or compared
 * across currencies. When items are unpriced the priced part is a lower bound: a design is rejected
 * only if that lower bound already exceeds the budget.
 */
public final class BudgetConstraint implements Constraint {

    /** The id. */
    public static final String ID = "budget";

    @Override
    public String id() {
        return ID;
    }

    /** @return {@code true}: the budget is checked against the priced portfolio */
    @Override
    public boolean usesCost() {
        return true;
    }

    @Override
    public ConstraintVerdict check(MetroPortfolio portfolio, DesignContext context) {
        BigDecimal budget = context.spec().commercial().budgetMaxMonthly();
        String currency = context.spec().commercial().currencyOrDefault();
        if (context.costModel() == null) {
            return ConstraintVerdict.pass(ID, "budget not evaluable: no cost model");
        }
        PortfolioCost cost = context.costModel().price(portfolio, context);
        if (cost.lines().isEmpty()) {
            return ConstraintVerdict.pass(ID, "budget not evaluable: nothing priced"
                    + (cost.isComplete() ? "" : " (" + cost.unpriced().size() + " unpriced item(s))"));
        }
        if (cost.currency().isEmpty() || !cost.currency().get().equals(currency)) {
            return ConstraintVerdict.pass(ID, "budget not evaluable: it is stated in " + currency + " but the design is "
                    + "priced in " + cost.currencies() + "; amounts in different currencies are not compared");
        }
        BigDecimal total = cost.monthlyTotal().orElseThrow();
        if (total.compareTo(budget) > 0) {
            return ConstraintVerdict.fail(ID, currency + " " + total.toPlainString() + "/month"
                    + (cost.isComplete() ? "" : " (priced part only)") + " exceeds the budget " + currency + " "
                    + budget.toPlainString());
        }
        return ConstraintVerdict.pass(ID, currency + " " + total.toPlainString() + "/month within budget "
                + budget.toPlainString() + (cost.isComplete() ? "" : "; " + cost.unpriced().size()
                + " item(s) unpriced, so the total is a lower bound"));
    }
}
