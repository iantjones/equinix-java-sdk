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

package api.equinix.javasdk.fabric.model.implementation.cloud;

import api.equinix.javasdk.fabric.enums.ConnectionType;
import api.equinix.javasdk.fabric.enums.PeeringType;
import api.equinix.javasdk.fabric.model.implementation.LinkProtocol;

/**
 * Adapter interface for bridging cloud provider SDK objects with Equinix Fabric connection creation.
 *
 * <p>Cloud providers like AWS, Microsoft Azure, Google Cloud, and Oracle Cloud each have their own
 * SDKs for managing direct connectivity (AWS Direct Connect, Azure ExpressRoute, Google Cloud
 * Interconnect, Oracle FastConnect). This interface defines a contract that allows objects from
 * those SDKs to supply the connection parameters needed by the Equinix Fabric API.</p>
 *
 * <h3>Design Pattern</h3>
 * <p>This uses the <strong>Adapter Pattern</strong> to bridge the gap between cloud provider SDK
 * objects and the Equinix Fabric {@link api.equinix.javasdk.fabric.model.json.creators.ConnectionOperator.ConnectionBuilder}.
 * Implementors wrap a cloud provider object (e.g., an AWS {@code Connection} or Azure
 * {@code ExpressRouteCircuit}) and extract the fields required by Equinix's API:</p>
 *
 * <ul>
 *   <li>{@link #getServiceProfileUuid()} &mdash; The Equinix service profile for this cloud provider</li>
 *   <li>{@link #getAuthenticationKey()} &mdash; The cloud provider's authentication key (e.g., AWS Account ID,
 *       Azure Service Key, GCP Pairing Key)</li>
 *   <li>{@link #getSellerRegion()} &mdash; The cloud provider's region (e.g., {@code "us-east-1"},
 *       {@code "eastus"}, {@code "us-east1"})</li>
 * </ul>
 *
 * <h3>Quick Start</h3>
 * <p>Using a built-in reference adapter with AWS Direct Connect:</p>
 * <pre>{@code
 * // Wrap an AWS Direct Connect connection object
 * AwsDirectConnectAdapter adapter = new AwsDirectConnectAdapter(
 *     awsConnection,                           // AWS SDK Connection object
 *     "equinix-aws-service-profile-uuid"        // Equinix service profile for AWS
 * );
 *
 * // Use it directly in the Equinix connection builder
 * Connection connection = fabric.connections()
 *     .define(ConnectionType.EVPL_VC)
 *     .name("My-AWS-Connection")
 *     .bandwidth(100)
 *     .aSideAccessPointPort(myPortUuid, LinkProtocol.dot1q().vlanTag(1000).create())
 *     .zSideCloudProvider(adapter)
 *     .notification("ops@example.com")
 *     .create();
 * }</pre>
 *
 * <h3>Custom Adapter</h3>
 * <p>Implement this interface for any cloud provider or custom service profile:</p>
 * <pre>{@code
 * public class MyCloudAdapter implements CloudProviderConnectionAdapter<MyCloudObject> {
 *
 *     private final MyCloudObject cloudObject;
 *     private final String equinixProfileUuid;
 *
 *     public MyCloudAdapter(MyCloudObject cloudObject, String equinixProfileUuid) {
 *         this.cloudObject = cloudObject;
 *         this.equinixProfileUuid = equinixProfileUuid;
 *     }
 *
 *     public String getServiceProfileUuid()  { return equinixProfileUuid; }
 *     public String getAuthenticationKey()   { return cloudObject.getApiKey(); }
 *     public String getSellerRegion()        { return cloudObject.getRegion(); }
 *     public MyCloudObject getSource()       { return cloudObject; }
 * }
 * }</pre>
 *
 * @param <T> the type of the cloud provider SDK object being adapted
 * @author ianjones
 * @see AwsDirectConnectAdapter
 * @see AzureExpressRouteAdapter
 * @see GoogleCloudInterconnectAdapter
 * @see OracleFastConnectAdapter
 */
public interface CloudProviderConnectionAdapter<T> {

    /**
     * Returns the Equinix Fabric service profile UUID for this cloud provider.
     *
     * <p>Each cloud provider has a corresponding Equinix service profile that defines
     * the connectivity parameters. This UUID can be found in the Equinix Developer Portal
     * or by querying {@code fabric.serviceProfiles().search()} for the provider's profile.</p>
     *
     * @return the UUID of the Equinix service profile for this cloud provider
     */
    String getServiceProfileUuid();

    /**
     * Returns the authentication key required by the cloud provider to authorize the connection.
     *
     * <p>The meaning of this key varies by provider:</p>
     * <ul>
     *   <li><strong>AWS Direct Connect</strong>: The AWS Account ID (12-digit number)</li>
     *   <li><strong>Azure ExpressRoute</strong>: The ExpressRoute Service Key (GUID)</li>
     *   <li><strong>Google Cloud Interconnect</strong>: The GCP Pairing Key</li>
     *   <li><strong>Oracle FastConnect</strong>: The Oracle OCID of the virtual circuit</li>
     * </ul>
     *
     * @return the provider-specific authentication key
     */
    String getAuthenticationKey();

    /**
     * Returns the cloud provider's region where the connection will terminate.
     *
     * <p>The region string follows the provider's naming convention:</p>
     * <ul>
     *   <li><strong>AWS</strong>: {@code "us-east-1"}, {@code "eu-west-1"}, etc.</li>
     *   <li><strong>Azure</strong>: {@code "eastus"}, {@code "westeurope"}, etc.</li>
     *   <li><strong>Google Cloud</strong>: {@code "us-east1"}, {@code "europe-west1"}, etc.</li>
     *   <li><strong>Oracle Cloud</strong>: {@code "us-ashburn-1"}, {@code "uk-london-1"}, etc.</li>
     * </ul>
     *
     * @return the provider-specific region identifier
     */
    String getSellerRegion();

    /**
     * Returns the underlying cloud provider SDK object that was adapted.
     *
     * <p>Provides access to the original provider object for cases where additional
     * provider-specific properties are needed beyond what the adapter interface exposes.</p>
     *
     * @return the original cloud provider SDK object
     */
    T getSource();

    /**
     * Returns the cloud provider type for this adapter.
     *
     * <p>Used for logging, diagnostics, and provider-specific defaults. Implementations
     * should return the appropriate {@link CloudProviderType} constant.</p>
     *
     * @return the cloud provider type
     */
    CloudProviderType getProviderType();

    /**
     * Returns the preferred connection type for this cloud provider.
     *
     * <p>Most cloud provider connections use {@link ConnectionType#EVPL_VC}, but implementations
     * can override this to return a different default. The user can always override the
     * connection type when calling the builder.</p>
     *
     * @return the preferred connection type; defaults to {@link ConnectionType#EVPL_VC}
     */
    default ConnectionType getPreferredConnectionType() {
        return ConnectionType.EVPL_VC;
    }

    /**
     * Returns the preferred link protocol for this cloud provider connection.
     *
     * <p>Most cloud provider connections use DOT1Q encapsulation. Implementations can
     * override this to provide a provider-specific default. Returns {@code null} to indicate
     * no default (the user must specify the link protocol explicitly).</p>
     *
     * @return the preferred link protocol, or {@code null} if none
     */
    default LinkProtocol getPreferredLinkProtocol() {
        return null;
    }

    /**
     * Returns the preferred peering type for this cloud provider connection.
     *
     * <p>Relevant for providers that support multiple peering types (e.g., Azure ExpressRoute
     * supports PRIVATE and MICROSOFT peering). Returns {@code null} to indicate no preference.</p>
     *
     * @return the preferred peering type, or {@code null} if not applicable
     */
    default PeeringType getPreferredPeeringType() {
        return null;
    }

    /**
     * Returns a human-readable description of the cloud provider connection for use in
     * logging and diagnostics.
     *
     * @return a description string such as "AWS Direct Connect to us-east-1"
     */
    default String describe() {
        return getProviderType().getDisplayName() + " to " + getSellerRegion();
    }
}
