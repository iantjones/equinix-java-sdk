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

package com.eqixiac.equinix.design.spi;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * The priced lines of one portfolio plus the items that could not be priced. Totals are offered
 * only when every line shares one currency: amounts in different currencies are never added.
 *
 * @param lines     the priced items
 * @param unpriced  what could not be priced and why (one entry per item); a non-empty list means
 *                  every total understates the real cost
 */
public record PortfolioCost(List<CostLine> lines, List<String> unpriced) {

    /** Makes both lists immutable and non-null. */
    public PortfolioCost {
        lines = lines == null ? List.of() : List.copyOf(lines);
        unpriced = unpriced == null ? List.of() : List.copyOf(unpriced);
    }

    /** @return the distinct currencies of the lines, sorted */
    public Set<String> currencies() {
        Set<String> currencies = new TreeSet<>();
        for (CostLine line : lines) {
            currencies.add(line.currency());
        }
        return currencies;
    }

    /** @return {@code true} when nothing was left unpriced */
    public boolean isComplete() {
        return unpriced.isEmpty();
    }

    /** @return the expected monthly total, or empty when there are no lines or the lines span currencies */
    public Optional<BigDecimal> monthlyTotal() {
        return total(0);
    }

    /** @return the low end of the monthly total, or empty as for {@link #monthlyTotal()} */
    public Optional<BigDecimal> monthlyLow() {
        return total(1);
    }

    /** @return the high end of the monthly total, or empty as for {@link #monthlyTotal()} */
    public Optional<BigDecimal> monthlyHigh() {
        return total(2);
    }

    /** @return the one-time total, or empty as for {@link #monthlyTotal()} */
    public Optional<BigDecimal> oneTimeTotal() {
        return total(3);
    }

    /** @return the single currency of the totals, or empty when there are no lines or several currencies */
    public Optional<String> currency() {
        Set<String> currencies = currencies();
        return currencies.size() == 1 ? Optional.of(currencies.iterator().next()) : Optional.empty();
    }

    private Optional<BigDecimal> total(int which) {
        if (lines.isEmpty() || currencies().size() != 1) {
            return Optional.empty();
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (CostLine line : lines) {
            sum = sum.add(switch (which) {
                case 1 -> line.monthlyLow();
                case 2 -> line.monthlyHigh();
                case 3 -> line.oneTime();
                default -> line.monthly();
            });
        }
        return Optional.of(sum);
    }
}
