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
 * Adapter for bridging Oracle Cloud Infrastructure FastConnect SDK objects with Equinix Fabric connections.
 *
 * <p>Oracle FastConnect provides dedicated, private connections between Oracle Cloud Infrastructure
 * (OCI) and on-premises networks. When creating an Equinix Fabric connection to Oracle Cloud, you need:</p>
 * <ul>
 *   <li><strong>Authentication Key</strong>: The Oracle OCID of the virtual circuit
 *       (e.g., {@code "ocid1.virtualcircuit.oc1..."})</li>
 *   <li><strong>Seller Region</strong>: The OCI region (e.g., {@code "us-ashburn-1"}, {@code "uk-london-1"})</li>
 *   <li><strong>Service Profile</strong>: The Equinix Fabric service profile UUID for Oracle FastConnect</li>
 * </ul>
 *
 * <h3>Usage with Oracle Cloud SDK Object</h3>
 * <pre>{@code
 * // From OCI SDK
 * VirtualCircuit vc = virtualNetworkClient.getVirtualCircuit(
 *     GetVirtualCircuitRequest.builder().virtualCircuitId(vcOcid).build()
 * ).getVirtualCircuit();
 *
 * OracleFastConnectAdapter<VirtualCircuit> adapter = new OracleFastConnectAdapter<>(
 *     vc,
 *     vc.getId(),                            // OCID of the virtual circuit
 *     vc.getRegion(),                        // e.g., "us-ashburn-1"
 *     "equinix-oracle-profile-uuid"          // Equinix service profile for Oracle
 * );
 *
 * Connection conn = fabric.connections()
 *     .define(ConnectionType.EVPL_VC)
 *     .name("Oracle-FastConnect")
 *     .bandwidth(100)
 *     .aSideAccessPointPort(portUuid, LinkProtocol.dot1q().vlanTag(3000).create())
 *     .zSideCloudProvider(adapter)
 *     .notification("ops@example.com")
 *     .create();
 * }</pre>
 *
 * <h3>Manual Construction (No OCI SDK)</h3>
 * <pre>{@code
 * OracleFastConnectAdapter<?> adapter = OracleFastConnectAdapter.of(
 *     "ocid1.virtualcircuit.oc1...",           // Virtual circuit OCID
 *     "us-ashburn-1",                           // Region
 *     "equinix-oracle-profile-uuid"             // Equinix service profile
 * );
 * }</pre>
 *
 * @param <T> the type of the Oracle Cloud SDK object being adapted
 * @author ianjones
 * @see CloudProviderConnectionAdapter
 */
@Getter
public class OracleFastConnectAdapter<T> implements CloudProviderConnectionAdapter<T> {

    private final T source;
    private final String authenticationKey;
    private final String sellerRegion;
    private final String serviceProfileUuid;

    /**
     * Creates an adapter wrapping an Oracle Cloud FastConnect SDK object.
     *
     * @param source                the OCI SDK object (e.g., {@code VirtualCircuit})
     * @param virtualCircuitOcid    the OCID of the Oracle virtual circuit
     * @param ociRegion             the OCI region (e.g., {@code "us-ashburn-1"})
     * @param serviceProfileUuid    the Equinix Fabric service profile UUID for Oracle FastConnect
     */
    public OracleFastConnectAdapter(T source, String virtualCircuitOcid, String ociRegion, String serviceProfileUuid) {
        this.source = source;
        this.authenticationKey = virtualCircuitOcid;
        this.sellerRegion = ociRegion;
        this.serviceProfileUuid = serviceProfileUuid;
    }

    /**
     * Creates an adapter without an OCI SDK object, using manually specified parameters.
     *
     * @param virtualCircuitOcid    the OCID of the Oracle virtual circuit
     * @param ociRegion             the OCI region
     * @param serviceProfileUuid    the Equinix Fabric service profile UUID for Oracle FastConnect
     * @return a new adapter configured for Oracle FastConnect
     */
    public static OracleFastConnectAdapter<Void> of(String virtualCircuitOcid, String ociRegion, String serviceProfileUuid) {
        return new OracleFastConnectAdapter<>(null, virtualCircuitOcid, ociRegion, serviceProfileUuid);
    }

    @Override
    public CloudProviderType getProviderType() {
        return CloudProviderType.ORACLE_CLOUD;
    }

    @Override
    public ConnectionType getPreferredConnectionType() {
        return ConnectionType.EVPL_VC;
    }
}
