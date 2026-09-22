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
 * Cloud provider SDK interoperability layer for Equinix Fabric connections.
 *
 * <p>This package provides the <strong>Adapter Pattern</strong> for bridging cloud provider SDK
 * objects (AWS Direct Connect, Azure ExpressRoute, Google Cloud Interconnect, Oracle FastConnect)
 * with the Equinix Fabric
 * {@link com.eqixiac.equinix.fabric.model.json.creators.ConnectionOperator.ConnectionBuilder ConnectionBuilder}.
 * </p>
 *
 * <h3>Architecture</h3>
 * <p>The central interface {@link com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderConnectionAdapter}
 * defines the contract for extracting connection parameters from any cloud provider SDK object.
 * Four built-in reference implementations are provided:</p>
 *
 * <ul>
 *   <li>{@link com.eqixiac.equinix.fabric.model.implementation.cloud.AwsDirectConnectAdapter} &mdash; AWS Direct Connect</li>
 *   <li>{@link com.eqixiac.equinix.fabric.model.implementation.cloud.AzureExpressRouteAdapter} &mdash; Azure ExpressRoute</li>
 *   <li>{@link com.eqixiac.equinix.fabric.model.implementation.cloud.GoogleCloudInterconnectAdapter} &mdash; Google Cloud Interconnect</li>
 *   <li>{@link com.eqixiac.equinix.fabric.model.implementation.cloud.OracleFastConnectAdapter} &mdash; Oracle FastConnect</li>
 * </ul>
 *
 * <h3>Quick Start</h3>
 * <pre>{@code
 * // Option 1: Wrap a cloud provider SDK object
 * AwsDirectConnectAdapter<Connection> adapter = new AwsDirectConnectAdapter<>(
 *     awsConnection, awsAccountId, "us-east-1", equinixProfileUuid);
 *
 * // Option 2: Manual construction without a provider SDK
 * AwsDirectConnectAdapter<?> adapter = AwsDirectConnectAdapter.of(
 *     "123456789012", "us-east-1", equinixProfileUuid);
 *
 * // Use the adapter with the Fabric ConnectionBuilder
 * Connection conn = fabric.connections()
 *     .define(ConnectionType.EVPL_VC)
 *     .name("My-Cloud-Connection")
 *     .bandwidth(100)
 *     .aSideAccessPointPort(portUuid, LinkProtocol.dot1q().vlanTag(1000).create())
 *     .zSideCloudProvider(adapter)
 *     .notification("ops@example.com")
 *     .create();
 * }</pre>
 *
 * <h3>Custom Adapters</h3>
 * <p>Implement {@link com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderConnectionAdapter}
 * for any cloud provider or service not covered by the built-in adapters. The interface defines
 * required methods for service profile UUID, authentication key, and seller region, plus optional
 * defaults for connection type, link protocol, and peering type.</p>
 *
 * <h3>Authentication key and activation key</h3>
 * <p>The Fabric v4 {@code AccessPoint} schema defines two string properties, and the adapter
 * exposes one method for each. Neither is derived from, or substituted for, the other.</p>
 * <table>
 *   <caption>Key properties on an access point</caption>
 *   <tr><th>Wire property</th><th>Adapter method</th><th>Content</th><th>Status</th></tr>
 *   <tr><td>{@code authenticationKey}</td><td>{@code getAuthenticationKey()} (abstract)</td>
 *       <td>Provider-defined identifier: AWS account id, ExpressRoute service key, GCP pairing
 *       key, OCI virtual circuit OCID.</td><td>GA</td></tr>
 *   <tr><td>{@code activationKey}</td><td>{@code getActivationKey()} (default {@code null})</td>
 *       <td>Provider-encoded activation key, sent unmodified. It can be checked first with
 *       {@code ServiceProfiles.validateActivationKey(...)}.</td>
 *       <td><b>Beta</b>: defined in the catalog fetched 2026-09-21; the catalog has no
 *       connection-create example that sets it.</td></tr>
 * </table>
 * <p>The four built-in adapters do not override {@code getActivationKey()}, so the request bodies
 * they produce contain no {@code activationKey} property.</p>
 *
 * @see com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderConnectionAdapter
 * @see com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType
 */
package com.eqixiac.equinix.fabric.model.implementation.cloud;
