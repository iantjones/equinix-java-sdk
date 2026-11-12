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

import com.eqixiac.equinix.design.spi.CostLine;
import com.eqixiac.equinix.design.value.ratecard.PriceSource;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * The money a finding's recommended action would save (positive) or cost (negative), with where the
 * figures came from and how far they could be off.
 *
 * <p>Three kinds: {@link Kind#PRICED} carries a currency, an expected monthly saving with a
 * low/high range, any one-time cost, and the {@link CostLine}s it was derived from (the current and
 * the proposed state); {@link Kind#UNPRICED} means the action has a financial effect that could not
 * be priced, and {@link #reason()} says why; {@link Kind#NONE} means the action has no direct
 * financial effect (for example fixing a missing notification). An unpriced saving is never
 * presented as zero.</p>
 *
 * @param kind                  priced, unpriced, or no direct financial effect
 * @param currency              ISO 4217 code of every amount; {@code null} unless priced
 * @param monthlySaving         the expected monthly saving (negative for an added cost); {@code null} unless priced
 * @param monthlySavingLow      the low end of the monthly saving range; {@code null} unless priced
 * @param monthlySavingHigh     the high end of the monthly saving range; {@code null} unless priced
 * @param oneTimeCost           a one-time cost of the action (zero when none); {@code null} unless priced
 * @param source                the provenance of the figures; {@code null} unless priced
 * @param basis                 the cost lines the saving was derived from (current first, then proposed)
 * @param reason                why it is unpriced or has no effect, or a provenance note when priced
 */
public record FinancialImpact(Kind kind, String currency, BigDecimal monthlySaving, BigDecimal monthlySavingLow,
                              BigDecimal monthlySavingHigh, BigDecimal oneTimeCost, PriceSource source,
                              List<CostLine> basis, String reason) {

    /** The kind of impact. */
    public enum Kind {
        /** Priced in one currency, with a range. */
        PRICED,
        /** Has a financial effect that could not be priced. */
        UNPRICED,
        /** Has no direct financial effect. */
        NONE
    }

    /** Validates that a priced impact carries its figures and a range that contains the expectation. */
    public FinancialImpact {
        if (kind == null) {
            throw new IllegalArgumentException("kind is required");
        }
        basis = basis == null ? List.of() : List.copyOf(basis);
        if (kind == Kind.PRICED) {
            if (currency == null || monthlySaving == null || source == null) {
                throw new IllegalArgumentException("A priced impact needs a currency, a monthly saving and a source");
            }
            monthlySavingLow = monthlySavingLow == null ? monthlySaving : monthlySavingLow;
            monthlySavingHigh = monthlySavingHigh == null ? monthlySaving : monthlySavingHigh;
            oneTimeCost = oneTimeCost == null ? BigDecimal.ZERO : oneTimeCost;
            if (monthlySavingLow.compareTo(monthlySaving) > 0 || monthlySavingHigh.compareTo(monthlySaving) < 0) {
                throw new IllegalArgumentException("The saving range must contain the expected saving");
            }
        } else if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("An unpriced or no-effect impact needs a reason");
        }
    }

    /**
     * An action whose financial effect could not be priced.
     *
     * @param reason why
     * @return the impact
     */
    public static FinancialImpact unpriced(String reason) {
        return new FinancialImpact(Kind.UNPRICED, null, null, null, null, null, null, List.of(), reason);
    }

    /**
     * An action with no direct financial effect.
     *
     * @param reason a short explanation
     * @return the impact
     */
    public static FinancialImpact none(String reason) {
        return new FinancialImpact(Kind.NONE, null, null, null, null, null, null, List.of(), reason);
    }

    /**
     * The saving of moving from {@code current} to {@code proposed}. Lines in different currencies
     * are never combined: the result is then unpriced.
     *
     * @param current  what the resource costs today
     * @param proposed what it would cost after the action ({@code null} when the action removes it)
     * @param note     a provenance note, or {@code null}
     * @return the impact
     */
    public static FinancialImpact between(CostLine current, CostLine proposed, String note) {
        if (current == null) {
            return unpriced("the current cost could not be priced");
        }
        if (proposed != null && !current.currency().equals(proposed.currency())) {
            return unpriced("current and proposed prices are in different currencies ("
                    + current.currency() + " vs " + proposed.currency() + "); no conversion is made");
        }
        BigDecimal expected = current.monthly().subtract(proposed == null ? BigDecimal.ZERO : proposed.monthly());
        BigDecimal low = current.monthlyLow().subtract(proposed == null ? BigDecimal.ZERO : proposed.monthlyHigh());
        BigDecimal high = current.monthlyHigh().subtract(proposed == null ? BigDecimal.ZERO : proposed.monthlyLow());
        PriceSource source = proposed == null || proposed.source() == current.source()
                ? current.source() : PriceSource.COMPOSITE;
        BigDecimal oneTime = proposed == null ? BigDecimal.ZERO : proposed.oneTime();
        List<CostLine> basis = proposed == null ? List.of(current) : List.of(current, proposed);
        return new FinancialImpact(Kind.PRICED, current.currency(), scale(expected), scale(low.min(expected)),
                scale(high.max(expected)), scale(oneTime), source, basis, note);
    }

    /**
     * An added cost (a negative saving), for example adding a missing secondary connection.
     *
     * @param added the cost line of what would be added
     * @param note  a provenance note, or {@code null}
     * @return the impact
     */
    public static FinancialImpact addedCost(CostLine added, String note) {
        if (added == null) {
            return unpriced("the added resource could not be priced");
        }
        return new FinancialImpact(Kind.PRICED, added.currency(), scale(added.monthly().negate()),
                scale(added.monthlyHigh().negate()), scale(added.monthlyLow().negate()), scale(added.oneTime()),
                added.source(), List.of(added), note);
    }

    /**
     * Whether the figures are priced.
     *
     * @return {@code true} for {@link Kind#PRICED}
     */
    @JsonIgnore
    public boolean isPriced() {
        return kind == Kind.PRICED;
    }

    /**
     * Whether the action saves money (priced and the expected saving is positive).
     *
     * @return {@code true} for a priced positive saving
     */
    @JsonIgnore
    public boolean isSaving() {
        return isPriced() && monthlySaving.signum() > 0;
    }

    /**
     * A short human-readable rendering, for example {@code USD 300.00/month (240.00..360.00, REFERENCE)}.
     *
     * @return the text
     */
    public String describe() {
        return switch (kind) {
            case NONE -> "no direct cost effect (" + reason + ")";
            case UNPRICED -> "unpriced: " + reason;
            case PRICED -> {
                String verb = monthlySaving.signum() >= 0 ? "saves " : "costs ";
                BigDecimal abs = monthlySaving.abs();
                String range = monthlySavingLow.compareTo(monthlySavingHigh) == 0 ? ""
                        : " (range " + monthlySavingLow.toPlainString() + ".." + monthlySavingHigh.toPlainString() + ")";
                String once = oneTimeCost.signum() == 0 ? "" : ", one-time " + currency + " " + oneTimeCost.toPlainString();
                yield verb + currency + " " + abs.toPlainString() + "/month" + range + once + " [" + source + "]";
            }
        };
    }

    private static BigDecimal scale(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP);
    }
}
