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
 * The shared design document every design engine reads: {@link com.eqixiac.equinix.design.spec.DesignSpec}.
 *
 * <p>A {@code DesignSpec} states intent, not mechanism: where the users and facilities are
 * ({@link com.eqixiac.equinix.design.spec.SiteIntent}), what runs
 * ({@link com.eqixiac.equinix.design.spec.WorkloadIntent}), which clouds and networks it must reach
 * ({@link com.eqixiac.equinix.design.spec.CloudIntent}, {@link com.eqixiac.equinix.design.spec.PeerIntent}),
 * the hard limits ({@link com.eqixiac.equinix.design.spec.DesignConstraints}), what "better" means
 * ({@link com.eqixiac.equinix.design.spec.DesignObjectives}) and the commercial frame
 * ({@link com.eqixiac.equinix.design.spec.CommercialTerms}).</p>
 *
 * <p>It is immutable, versioned ({@code schemaVersion}), and round-trips through JSON and YAML
 * ({@link com.eqixiac.equinix.design.spec.DesignSpecIO}), so a design can be stored in version control,
 * diffed, reviewed and replayed. {@code fingerprint()} gives a stable identity for a spec's content.
 * The optimizer, the cost model, peering analysis, the deployment planner and the MCP tools all take
 * the same document, so their results can be chained and compared.</p>
 */
package com.eqixiac.equinix.design.spec;
