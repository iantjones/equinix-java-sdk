/*
 * Copyright 2026 Ian Jones. All Rights Reserved.
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

package com.eqixiac.equinix.fabric.beta.client;

import com.eqixiac.equinix.fabric.beta.Beta;

/**
 * Hub for the Fabric v4 operation families the spec marks <strong>Beta</strong>: one typed client per family.
 *
 * <p>Obtain it from {@code fabric.beta()}. Endpoints live in their own apiParams functional area
 * ({@code FabricBeta}, file {@code json/apiParams_FabricBeta.json}) with the same URI format and version as
 * {@code Fabric}, so they never shadow or replace a GA Fabric endpoint.</p>
 *
 * <p>Beta endpoints may change shape or be withdrawn by Equinix without notice, and the account must be
 * entitled to them (an un-entitled account typically sees HTTP 403 or 404). None of these operations offers
 * {@code dryRun} in the spec. Cross connects, LOAs and optical connects are real physical orders.</p>
 * <p><strong>Beta</strong>: Fabric v4 4.34.0 (spec read 2026-11-11).</p>
 */
@Beta
public interface FabricBeta {

    /**
     * Autonomous System Numbers (ASNs): public and private ASN records (5 operations).
     *
     * @return the {@link Asns} client
     */
    Asns asns();

    /**
     * Internet Exchange (IX) services (5 operations).
     *
     * @return the {@link ExchangeServices} client
     */
    ExchangeServices exchangeServices();

    /**
     * Application Connect domains (6 operations).
     *
     * @return the {@link AppDomains} client
     */
    AppDomains appDomains();

    /**
     * Application Connect links and their domain, service and processor attachments (21 operations).
     *
     * @return the {@link AppLinks} client
     */
    AppLinks appLinks();

    /**
     * Application Connect processors (2 operations).
     *
     * @return the {@link AppProcessors} client
     */
    AppProcessors appProcessors();

    /**
     * Application Connect services (8 operations).
     *
     * @return the {@link AppServices} client
     */
    AppServices appServices();

    /**
     * Application Connect subscriptions (5 operations).
     *
     * @return the {@link AppSubscriptions} client
     */
    AppSubscriptions appSubscriptions();

    /**
     * Optical Metro Connect services (4 operations).
     *
     * @return the {@link OpticalConnects} client
     */
    OpticalConnects opticalConnects();

    /**
     * Fabric v4 cross connects (real physical orders) (12 operations).
     *
     * @return the {@link CrossConnects} client
     */
    CrossConnects crossConnects();

    /**
     * Digital Letters of Authorization (LOAs) (10 operations).
     *
     * @return the {@link Loas} client
     */
    Loas loas();

    /**
     * Fabric tags (single-tag lookup) (1 operations).
     *
     * @return the {@link Tags} client
     */
    Tags tags();

    /**
     * Company-profile logos (3 operations).
     *
     * @return the {@link Logos} client
     */
    Logos logos();
}
