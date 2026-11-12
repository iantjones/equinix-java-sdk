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


/**
 * Pricing of candidate designs: {@link com.eqixiac.equinix.design.cost.RateCardCostModel} is the
 * {@link com.eqixiac.equinix.design.spi.CostModel} the Metro Optimizer uses, pricing each metro's
 * needs from a {@code RateCard} with every line's source and a range from
 * {@link com.eqixiac.equinix.design.cost.PriceBands}.
 *
 * <p>Currency: every amount is compared in the spec's currency. {@link com.eqixiac.equinix.design.cost.CurrencyConverters}
 * builds the converter (pinned spec rates, a supplied converter, live ECB reference rates via
 * {@link com.eqixiac.equinix.design.cost.FrankfurterExchangeRates}, then
 * {@link com.eqixiac.equinix.design.cost.BundledExchangeRates}); {@link com.eqixiac.equinix.design.cost.ConvertingRateCard}
 * applies it to any rate card for the TCO and savings models.
 * {@link com.eqixiac.equinix.design.cost.CostComparability} is the ranking safeguard for plug-in cost models that do
 * not convert.</p>
 *
 * <p>Growth and terms: {@link com.eqixiac.equinix.design.cost.GrowthPlanner} projects traffic and Fabric tiers month
 * by month, recommends a commitment term per item ({@link com.eqixiac.equinix.design.cost.GrowthPlan}) and produces
 * deterministic and seeded Monte Carlo scenario bands ({@link com.eqixiac.equinix.design.cost.ScenarioBand}).</p>
 */
package com.eqixiac.equinix.design.cost;
