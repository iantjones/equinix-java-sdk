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

/** What a {@link GraphEdge} of a {@link DesignGraph} stands for. Every edge is bidirectional. */
public enum EdgeKind {
    /** A site's access circuit into a metro. */
    SITE_ACCESS,
    /** A metro's attachment to its Cloud Router (inside one metro). */
    ROUTER_ATTACHMENT,
    /** Fabric inter-metro connectivity between two Cloud Routers (or metros). */
    BACKBONE,
    /** A Fabric virtual connection from a Cloud Router to a cloud on-ramp: the primary or standby leg. */
    CLOUD_CONNECTION,
    /** The provider side of an on-ramp: from the on-ramp to the cloud region. */
    PROVIDER_LINK,
    /** Where a workload runs (primary) or fails over to (standby). */
    WORKLOAD_HOSTING,
    /** Where a peer network is present (IX or facility) in a portfolio metro; informational. */
    PEERING
}
