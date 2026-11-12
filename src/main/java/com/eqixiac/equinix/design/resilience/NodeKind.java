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

/** What a {@link GraphNode} of a {@link DesignGraph} stands for. */
public enum NodeKind {
    /** A place the design serves (an office, a user population): a {@code SiteIntent}. */
    SITE,
    /** An Equinix metro of the portfolio. */
    METRO,
    /** The Fabric Cloud Router the design uses in a metro (Layer-3 hub for cloud and inter-metro traffic). */
    CLOUD_ROUTER,
    /** A cloud on-ramp: the provider's Fabric service profile at one metro (cloud x metro). */
    CLOUD_ONRAMP,
    /** A cloud or service provider of the spec (a {@code CloudIntent}), in its provider region. */
    CLOUD,
    /** A workload of the spec (or the placement made for a cloud no workload names). */
    WORKLOAD,
    /** A network the spec wants to peer with (a {@code PeerIntent}), where peering data was supplied. */
    PEER
}
