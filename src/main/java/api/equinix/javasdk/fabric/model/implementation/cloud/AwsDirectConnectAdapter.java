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
import api.equinix.javasdk.fabric.model.implementation.LinkProtocol;
import lombok.Getter;

/**
 * Adapter for bridging AWS Direct Connect SDK objects with Equinix Fabric connections.
 *
 * <p>AWS Direct Connect provides dedicated network connections between AWS and
 * colocation facilities. When creating an Equinix Fabric connection to AWS, you need:</p>
 * <ul>
 *   <li><strong>Authentication Key</strong>: Your 12-digit AWS Account ID</li>
 *   <li><strong>Seller Region</strong>: The AWS region (e.g., {@code "us-east-1"}, {@code "eu-west-1"})</li>
 *   <li><strong>Service Profile</strong>: The Equinix Fabric service profile UUID for AWS Direct Connect</li>
 * </ul>
 *
 * <h3>Usage with AWS SDK Object</h3>
 * <pre>{@code
 * // From AWS Direct Connect SDK
 * com.amazonaws.services.directconnect.model.Connection awsConnection = ...;
 *
 * AwsDirectConnectAdapter<Connection> adapter = new AwsDirectConnectAdapter<>(
 *     awsConnection,
 *     awsConnection.getOwnerAccount(),   // 12-digit AWS Account ID
 *     awsConnection.getRegion(),          // e.g., "us-east-1"
 *     "equinix-aws-profile-uuid"          // Equinix service profile for AWS
 * );
 *
 * Connection conn = fabric.connections()
 *     .define(ConnectionType.EVPL_VC)
 *     .name("AWS-" + awsConnection.getConnectionName())
 *     .bandwidth(awsConnection.getBandwidth())
 *     .aSideAccessPointPort(portUuid, LinkProtocol.dot1q().vlanTag(1000).create())
 *     .zSideCloudProvider(adapter)
 *     .notification("ops@example.com")
 *     .create();
 * }</pre>
 *
 * <h3>Manual Construction (No AWS SDK)</h3>
 * <pre>{@code
 * AwsDirectConnectAdapter<?> adapter = AwsDirectConnectAdapter.of(
 *     "123456789012",                      // AWS Account ID
 *     "us-east-1",                          // Region
 *     "equinix-aws-profile-uuid"            // Equinix service profile
 * );
 * }</pre>
 *
 * @param <T> the type of the AWS SDK object being adapted (e.g., {@code Connection}, or use {@code ?} for manual construction)
 * @author ianjones
 * @see CloudProviderConnectionAdapter
 */
@Getter
public class AwsDirectConnectAdapter<T> implements CloudProviderConnectionAdapter<T> {

    private final T source;
    private final String authenticationKey;
    private final String sellerRegion;
    private final String serviceProfileUuid;

    /**
     * Creates an adapter wrapping an AWS Direct Connect SDK object.
     *
     * @param source             the AWS SDK object (e.g., {@code Connection})
     * @param awsAccountId       the 12-digit AWS Account ID used as the authentication key
     * @param awsRegion          the AWS region (e.g., {@code "us-east-1"})
     * @param serviceProfileUuid the Equinix Fabric service profile UUID for AWS Direct Connect
     */
    public AwsDirectConnectAdapter(T source, String awsAccountId, String awsRegion, String serviceProfileUuid) {
        this.source = source;
        this.authenticationKey = awsAccountId;
        this.sellerRegion = awsRegion;
        this.serviceProfileUuid = serviceProfileUuid;
    }

    /**
     * Creates an adapter without an AWS SDK object, using manually specified parameters.
     *
     * @param awsAccountId       the 12-digit AWS Account ID
     * @param awsRegion          the AWS region (e.g., {@code "us-east-1"})
     * @param serviceProfileUuid the Equinix Fabric service profile UUID for AWS Direct Connect
     * @return a new adapter configured for AWS Direct Connect
     */
    public static AwsDirectConnectAdapter<Void> of(String awsAccountId, String awsRegion, String serviceProfileUuid) {
        return new AwsDirectConnectAdapter<>(null, awsAccountId, awsRegion, serviceProfileUuid);
    }

    @Override
    public CloudProviderType getProviderType() {
        return CloudProviderType.AWS;
    }

    @Override
    public ConnectionType getPreferredConnectionType() {
        return ConnectionType.EVPL_VC;
    }

    @Override
    public LinkProtocol getPreferredLinkProtocol() {
        return null; // DOT1Q is typical, but VLAN tag must be user-specified
    }
}
