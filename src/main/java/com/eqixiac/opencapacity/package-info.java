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

/**
 * Client for the OpenCapacity API, a proposed open specification that lets a customer ask a data
 * center provider whether a capacity requirement can be met, record the requirement as a standing
 * signal, and subscribe to changes of the answer.
 *
 * <p><strong>Proposal.</strong> No provider implements this specification. It is not an Equinix
 * API, and it can change before version 1.0 is final.
 *
 * <p>The client is separate from the Equinix clients of this SDK. It has no default host: you give
 * it the provider's endpoint and credentials for that provider, and it never sends a token to any
 * other host.
 *
 * <pre>{@code
 * OpenCapacity capacity = OpenCapacity.builder()
 *     .endpoint("https://provider.example/capacity/v1")
 *     .credentials(CLIENT_ID, CLIENT_SECRET)
 *     .build();
 *
 * CapacityRequirement requirement = CapacityRequirement.define()
 *     .addLocation(Location.metro("FR"))
 *     .cabinets(40)
 *     .density(DensityRange.kva().min(5).target(8).max(100).create())
 *     .addFeature(Feature.LIQUID_COOLING, true)
 *     .create();
 *
 * CapacityAnswer answer = capacity.queries().ask(requirement);
 * Signal signal = capacity.signals().define(requirement).create();
 * Subscription subscription = capacity.subscriptions().define(signal).pull().create();
 * }</pre>
 *
 * <p>Requests are not retried, except once after a 401 with a new token. A failed request throws
 * {@link com.eqixiac.opencapacity.OpenCapacityException}.
 */
package com.eqixiac.opencapacity;
