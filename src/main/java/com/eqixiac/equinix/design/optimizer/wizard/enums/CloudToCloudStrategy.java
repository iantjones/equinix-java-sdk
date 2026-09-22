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

package com.eqixiac.equinix.design.optimizer.wizard.enums;

/**
 * How the Deployment Wizard treats a flow between two clouds. A flow exists when one workload
 * declares a dependency on two or more clouds ({@code WorkloadSpec.getDependsOnProviders()}); each
 * pair of those clouds is one flow.
 *
 * <p><b>Beta</b>: the native provider-to-provider links this lever plans for reached general
 * availability in 2026 and are described by a bundled catalog
 * ({@code MulticloudEnvironmentCatalog}) that goes stale as providers add regions.</p>
 *
 * <p>A native link has no Equinix A-side and no Fabric request body. Under every strategy the
 * wizard plans and prices it and never provisions it.</p>
 *
 * <table>
 *   <caption>Effect of each strategy on the plan</caption>
 *   <tr><th>Strategy</th><th>Equinix connections for the flow</th><th>Native links on the plan</th>
 *       <th>No catalog environment for the pair</th></tr>
 *   <tr><td>{@link #EQUINIX_ONLY}</td><td>planned</td><td>none; the catalog is not read</td>
 *       <td>no effect</td></tr>
 *   <tr><td>{@link #COMPARE}</td><td>planned</td><td>one {@code ALTERNATIVE} per matched pair</td>
 *       <td>no link is attached</td></tr>
 *   <tr><td>{@link #NATIVE_WHEN_AVAILABLE}</td><td>omitted only where the replacement rule allows
 *       </td><td>{@code REPLACEMENT} where a connection was omitted, otherwise
 *       {@code ALTERNATIVE}</td><td>no link is attached</td></tr>
 *   <tr><td>{@link #NATIVE_ONLY}</td><td>as {@code NATIVE_WHEN_AVAILABLE}</td>
 *       <td>as {@code NATIVE_WHEN_AVAILABLE}, plus one {@code UNAVAILABLE} entry per pair without
 *       a usable environment</td><td>Layer-1 validation error; the plan is invalid</td></tr>
 * </table>
 *
 * <h2>Replacement rule ({@code NATIVE_WHEN_AVAILABLE}, {@code NATIVE_ONLY})</h2>
 * <p>The Cloud Router to cloud connection at a metro is omitted only when all of the following
 * hold. Otherwise it is planned exactly as under {@code EQUINIX_ONLY}, at the same bandwidth.</p>
 * <ol>
 *   <li>The matched environment's status is {@code GA} and it publishes a size that covers the
 *       requested bandwidth.</li>
 *   <li>The optimization request declares no user site. The wizard does not model which site
 *       reaches which cloud, so any site is assumed to reach every cloud through the metro.</li>
 *   <li>The cloud is not a request-level provider requirement
 *       ({@code OptimizationRequest.getProviders()}).</li>
 *   <li>Every workload that depends on the cloud and would use this metro's connection (placed at
 *       this metro, placed at a metro where the cloud is unavailable, or unplaced) depends on
 *       well-known clouds only, and each of its flows involving the cloud has a {@code GA}
 *       environment with a covering size.</li>
 *   <li>No other workload is sized onto the connection (under {@code BandwidthStrategy.AGGREGATED}
 *       every workload at the metro is), and its bandwidth was not set by the custom bandwidth
 *       map.</li>
 * </ol>
 * <p>The metro's Cloud Router is never omitted: the wizard plans one per recommended metro. When
 * a replacement leaves it without a provider connection, the link's reasoning says so.</p>
 */
public enum CloudToCloudStrategy {

    /**
     * Plan the Equinix path only. The multicloud environment catalog is not consulted, no native
     * link is attached, and the plan, its pricing and its rendered output are those of a wizard
     * without this lever.
     */
    EQUINIX_ONLY,

    /**
     * Omit the Equinix connections for a cloud-to-cloud flow where the replacement rule in the
     * class documentation allows, and carry the native link as a {@code REPLACEMENT}. Where the
     * rule does not allow it (no {@code GA} environment, a user site, another consumer of the
     * connection), the Equinix connections stay and a matched link is carried as an
     * {@code ALTERNATIVE} with the reason.
     */
    NATIVE_WHEN_AVAILABLE,

    /**
     * As {@link #NATIVE_WHEN_AVAILABLE}, and additionally a cloud-to-cloud flow with no usable
     * catalog environment (none for the region pair, a status other than {@code GA}, or no size
     * covering the requested bandwidth) is a Layer-1 validation error. The replacement rule is
     * unchanged: a connection another consumer still needs stays on the plan and is not an error.
     */
    NATIVE_ONLY,

    /**
     * The default. Plan the Equinix path exactly as {@link #EQUINIX_ONLY} does, and attach one
     * {@code ALTERNATIVE} native link per cloud pair that has a catalog environment matching the
     * planned seller regions, with two-sided pricing where published, the Equinix-path cost for
     * the same flow, and the break-even sustained rate.
     */
    COMPARE
}
