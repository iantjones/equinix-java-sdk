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

import java.util.List;

/**
 * One analysis rule: a small, pure function from an estate snapshot (and optional utilisation) to
 * findings. Rules never call an API and never change a resource; prices come from
 * {@link RuleContext#pricing()}.
 *
 * <p>The default rules are in {@code design.estate.rules}; supply your own through
 * {@code EstateAnalyzer.Builder.rules(...)} or {@code addRule(...)}.</p>
 */
public interface EstateRule {

    /**
     * A short, stable name, for example {@code right-size}.
     *
     * @return the name
     */
    String name();

    /**
     * Evaluates the rule.
     *
     * @param inventory   the snapshot
     * @param utilization the utilisation ({@link UtilizationSnapshot#none()} when not collected)
     * @param context     pricing, thresholds and the reference time
     * @return the findings, possibly empty
     */
    List<EstateFinding> evaluate(EstateInventory inventory, UtilizationSnapshot utilization, RuleContext context);
}
