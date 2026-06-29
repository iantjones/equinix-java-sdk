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
 * {@link api.equinix.javasdk.fabric.model.json.creators.ConnectionOperator.ConnectionBuilder ConnectionBuilder}.
 * </p>
 *
 * <h3>Architecture</h3>
 * <p>The central interface {@link api.equinix.javasdk.fabric.model.implementation.cloud.CloudProviderConnectionAdapter}
 * defines the contract for extracting connection parameters from any cloud provider SDK object.
 * Four built-in reference implementations are provided:</p>
 *
 * <ul>
 *   <li>{@link api.equinix.javasdk.fabric.model.implementation.cloud.AwsDirectConnectAdapter} &mdash; AWS Direct Connect</li>
 *   <li>{@link api.equinix.javasdk.fabric.model.implementation.cloud.AzureExpressRouteAdapter} &mdash; Azure ExpressRoute</li>
 *   <li>{@link api.equinix.javasdk.fabric.model.implementation.cloud.GoogleCloudInterconnectAdapter} &mdash; Google Cloud Interconnect</li>
 *   <li>{@link api.equinix.javasdk.fabric.model.implementation.cloud.OracleFastConnectAdapter} &mdash; Oracle FastConnect</li>
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
 * <p>Implement {@link api.equinix.javasdk.fabric.model.implementation.cloud.CloudProviderConnectionAdapter}
 * for any cloud provider or service not covered by the built-in adapters. The interface defines
 * required methods for service profile UUID, authentication key, and seller region, plus optional
 * defaults for connection type, link protocol, and peering type.</p>
 *
 * @see api.equinix.javasdk.fabric.model.implementation.cloud.CloudProviderConnectionAdapter
 * @see api.equinix.javasdk.fabric.model.implementation.cloud.CloudProviderType
 */
package api.equinix.javasdk.fabric.model.implementation.cloud;
