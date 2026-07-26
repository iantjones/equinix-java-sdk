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
 * Lightweight PeeringDB REST client and its wire models — a credential and transport entirely
 * separate from the Equinix OAuth client.
 *
 * <p>{@link com.eqixiac.equinix.design.peering.client.PeeringDbClient} is
 * {@code AutoCloseable}, supports API-key or anonymous access, batches multi-ASN lookups with
 * PeeringDB's {@code asn__in} operator, and honours 429 responses with capped
 * {@code Retry-After}-aware backoff. {@code loadEquinixCatalog()} must be called before any
 * {@code getEquinix*} method — it fetches Equinix's organization record ({@code org/2},
 * {@code depth=2}) once and caches the IX/facility catalog the presence filters run against.</p>
 *
 * <p>The remaining types ({@link com.eqixiac.equinix.design.peering.client.PeeringDbNetwork},
 * {@link com.eqixiac.equinix.design.peering.client.PeeringDbIx},
 * {@link com.eqixiac.equinix.design.peering.client.PeeringDbFacility},
 * {@link com.eqixiac.equinix.design.peering.client.PeeringDbNetIxlan},
 * {@link com.eqixiac.equinix.design.peering.client.PeeringDbNetFac},
 * {@link com.eqixiac.equinix.design.peering.client.PeeringDbOrg}) are Jackson-mapped wire
 * models of the corresponding PeeringDB objects.</p>
 *
 * @see com.eqixiac.equinix.design.peering.client.PeeringDbClient
 * @see com.eqixiac.equinix.design.peering.PeeringIntelligence
 */
package com.eqixiac.equinix.design.peering.client;
