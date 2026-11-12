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
 * Sustainability plug-ins for the design engines (Beta: may change in a minor release). The
 * {@link com.eqixiac.equinix.design.sustainability.CarbonObjective} ranks portfolios by the grid carbon
 * intensity of where their workloads run, from a
 * {@link com.eqixiac.equinix.design.sustainability.CarbonIntensityTable} with its source, period and method:
 * the caller's own figures, or the bundled country-level reference table. It is a siting indicator, not an
 * emissions estimate, and it never invents a figure: a metro with no figure makes the value unknown.
 */
package com.eqixiac.equinix.design.sustainability;
