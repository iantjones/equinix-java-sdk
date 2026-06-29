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
 * Adapter for bridging Google Cloud Interconnect SDK objects with Equinix Fabric connections.
 *
 * <p>Google Cloud Interconnect provides dedicated, high-bandwidth connections between
 * on-premises networks and Google Cloud. When creating an Equinix Fabric connection to
 * Google Cloud, you need:</p>
 * <ul>
 *   <li><strong>Authentication Key</strong>: The GCP Pairing Key (provided by Google when creating
 *       an Interconnect Attachment)</li>
 *   <li><strong>Seller Region</strong>: The GCP region (e.g., {@code "us-east1"}, {@code "europe-west1"})</li>
 *   <li><strong>Service Profile</strong>: The Equinix Fabric service profile UUID for Google Cloud</li>
 * </ul>
 *
 * <h3>Usage with Google Cloud SDK Object</h3>
 * <pre>{@code
 * // From Google Cloud SDK
 * InterconnectAttachment attachment = computeClient.getInterconnectAttachment(
 *     "my-project", "us-east1", "my-attachment");
 *
 * GoogleCloudInterconnectAdapter<InterconnectAttachment> adapter =
 *     new GoogleCloudInterconnectAdapter<>(
 *         attachment,
 *         attachment.getPairingKey(),           // GCP Pairing Key
 *         attachment.getRegion(),               // e.g., "us-east1"
 *         "equinix-gcp-profile-uuid"            // Equinix service profile for GCP
 *     );
 *
 * Connection conn = fabric.connections()
 *     .define(ConnectionType.EVPL_VC)
 *     .name("GCP-Interconnect")
 *     .bandwidth(100)
 *     .aSideAccessPointPort(portUuid, LinkProtocol.dot1q().vlanTag(2000).create())
 *     .zSideCloudProvider(adapter)
 *     .notification("ops@example.com")
 *     .create();
 * }</pre>
 *
 * <h3>Manual Construction (No GCP SDK)</h3>
 * <pre>{@code
 * GoogleCloudInterconnectAdapter<?> adapter = GoogleCloudInterconnectAdapter.of(
 *     "xxxx/xxxx/xxxx/xxxx",                   // GCP Pairing Key
 *     "us-east1",                               // Region
 *     "equinix-gcp-profile-uuid"                // Equinix service profile
 * );
 * }</pre>
 *
 * @param <T> the type of the Google Cloud SDK object being adapted
 * @author ianjones
 * @see CloudProviderConnectionAdapter
 */
@Getter
public class GoogleCloudInterconnectAdapter<T> implements CloudProviderConnectionAdapter<T> {

    private final T source;
    private final String authenticationKey;
    private final String sellerRegion;
    private final String serviceProfileUuid;

    /**
     * Creates an adapter wrapping a Google Cloud Interconnect SDK object.
     *
     * @param source             the GCP SDK object (e.g., {@code InterconnectAttachment})
     * @param gcpPairingKey      the GCP Pairing Key provided by Google Cloud
     * @param gcpRegion          the GCP region (e.g., {@code "us-east1"})
     * @param serviceProfileUuid the Equinix Fabric service profile UUID for Google Cloud
     */
    public GoogleCloudInterconnectAdapter(T source, String gcpPairingKey, String gcpRegion, String serviceProfileUuid) {
        this.source = source;
        this.authenticationKey = gcpPairingKey;
        this.sellerRegion = gcpRegion;
        this.serviceProfileUuid = serviceProfileUuid;
    }

    /**
     * Creates an adapter without a GCP SDK object, using manually specified parameters.
     *
     * @param gcpPairingKey      the GCP Pairing Key
     * @param gcpRegion          the GCP region
     * @param serviceProfileUuid the Equinix Fabric service profile UUID for Google Cloud
     * @return a new adapter configured for Google Cloud Interconnect
     */
    public static GoogleCloudInterconnectAdapter<Void> of(String gcpPairingKey, String gcpRegion, String serviceProfileUuid) {
        return new GoogleCloudInterconnectAdapter<>(null, gcpPairingKey, gcpRegion, serviceProfileUuid);
    }

    @Override
    public CloudProviderType getProviderType() {
        return CloudProviderType.GOOGLE_CLOUD;
    }

    @Override
    public ConnectionType getPreferredConnectionType() {
        return ConnectionType.EVPL_VC;
    }
}
