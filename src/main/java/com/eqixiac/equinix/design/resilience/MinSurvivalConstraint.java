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
package com.eqixiac.equinix.design.resilience;

import com.eqixiac.equinix.design.spi.Constraint;
import com.eqixiac.equinix.design.spi.ConstraintVerdict;
import com.eqixiac.equinix.design.spi.DesignContext;
import com.eqixiac.equinix.design.spi.MetroPortfolio;

import java.util.Locale;

/**
 * {@value #ID}: a portfolio must keep at least a minimum share of its baseline service through its worst
 * single-element failure (see {@link N1Survival}). Opt-in: with no threshold the constraint passes
 * everything without simulating.
 *
 * <p>The threshold, a percentage 0..100, comes from (first that is set wins): the constructor; the spec's
 * {@code constraints.minSurvivalPct}; the spec label {@value #LABEL} (for example
 * {@code labels: {"design.min_survival_pct": "75"}}). The label is the older, schema-free way to opt in and
 * keeps working; when both are set the {@code constraints.minSurvivalPct} field wins. A malformed label fails
 * every portfolio with a reason rather than being ignored. The Metro Optimizer adds this constraint itself when
 * the spec sets {@code constraints.minSurvivalPct}, so the field never needs the {@link ResiliencePlugin}.</p>
 */
public final class MinSurvivalConstraint implements Constraint {

    /** The constraint id. */
    public static final String ID = "min_survival";
    /** The spec label that sets the threshold (a percentage, 0..100). */
    public static final String LABEL = "design.min_survival_pct";

    private final Double thresholdPct;
    private final ResilienceOptions options;

    /** Reads the threshold from the spec ({@code constraints.minSurvivalPct}, else the label {@value #LABEL}); inert without either. */
    public MinSurvivalConstraint() {
        this(null, ResilienceOptions.defaults());
    }

    /**
     * @param thresholdPct the least share of service that must survive, 0..100; {@code null} to read the spec
     *                     ({@code constraints.minSurvivalPct}, else the label {@value #LABEL})
     * @param options      graph and simulation options
     */
    public MinSurvivalConstraint(Double thresholdPct, ResilienceOptions options) {
        if (thresholdPct != null && !(thresholdPct >= 0 && thresholdPct <= 100)) {
            throw new IllegalArgumentException("A survival threshold is a percentage between 0 and 100");
        }
        this.thresholdPct = thresholdPct;
        this.options = options == null ? ResilienceOptions.defaults() : options;
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public ConstraintVerdict check(MetroPortfolio portfolio, DesignContext context) {
        Double threshold = thresholdPct;
        if (threshold == null) {
            threshold = context.spec().constraints().minSurvivalPct();
        }
        if (threshold == null) {
            String label = context.spec().labels().get(LABEL);
            if (label == null || label.isBlank()) {
                return ConstraintVerdict.pass(ID, "not requested (no constraints.minSurvivalPct and no '" + LABEL
                        + "' label)");
            }
            try {
                threshold = Double.parseDouble(label.trim());
            } catch (NumberFormatException e) {
                threshold = null;
            }
            if (threshold == null || !(threshold >= 0 && threshold <= 100)) {
                return ConstraintVerdict.fail(ID, "label '" + LABEL + "' must be a percentage 0..100, got '" + label + "'");
            }
        }
        N1Survival n1 = N1Survival.of(portfolio, context, options);
        if (!n1.known()) {
            return ConstraintVerdict.pass(ID, "nothing to protect: " + n1.reason());
        }
        double pct = 100 * n1.worstSurvival();
        if (pct + 1e-9 < threshold) {
            return ConstraintVerdict.fail(ID, String.format(Locale.ROOT, "%s: %.0f%%, below the %.0f%% minimum",
                    n1.reason(), pct, threshold));
        }
        return ConstraintVerdict.pass(ID, String.format(Locale.ROOT, "%.0f%% survives the worst single failure "
                + "(minimum %.0f%%)", pct, threshold));
    }
}
