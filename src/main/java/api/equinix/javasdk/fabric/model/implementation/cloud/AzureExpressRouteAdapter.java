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
import lombok.Getter;

/**
 * Adapter for bridging Microsoft Azure ExpressRoute SDK objects with Equinix Fabric connections.
 *
 * <p>Azure ExpressRoute provides private, dedicated connections between Azure data centers
 * and on-premises infrastructure. When creating an Equinix Fabric connection to Azure, you need:</p>
 * <ul>
 *   <li><strong>Authentication Key</strong>: The ExpressRoute Service Key (a GUID provided by Azure)</li>
 *   <li><strong>Seller Region</strong>: The Azure region or peering location (e.g., {@code "Silicon Valley"},
 *       {@code "Washington DC"})</li>
 *   <li><strong>Service Profile</strong>: The Equinix Fabric service profile UUID for Azure ExpressRoute</li>
 *   <li><strong>Peering Type</strong>: {@link PeeringType#PRIVATE} or {@link PeeringType#MICROSOFT}</li>
 * </ul>
 *
 * <h3>Usage with Azure SDK Object</h3>
 * <pre>{@code
 * // From Azure SDK
 * ExpressRouteCircuit circuit = azureClient.getExpressRouteCircuit("myResourceGroup", "myCircuit");
 *
 * AzureExpressRouteAdapter<ExpressRouteCircuit> adapter = new AzureExpressRouteAdapter<>(
 *     circuit,
 *     circuit.serviceKey(),                // ExpressRoute Service Key (GUID)
 *     circuit.peeringLocation(),           // e.g., "Silicon Valley"
 *     "equinix-azure-profile-uuid",        // Equinix service profile for Azure
 *     PeeringType.PRIVATE                  // Private or Microsoft peering
 * );
 *
 * Connection conn = fabric.connections()
 *     .define(ConnectionType.EVPL_VC)
 *     .name("Azure-ExpressRoute")
 *     .bandwidth(100)
 *     .aSideAccessPointPort(portUuid, LinkProtocol.dot1q().vlanTag(1500).create())
 *     .zSideCloudProvider(adapter)
 *     .notification("ops@example.com")
 *     .create();
 * }</pre>
 *
 * <h3>Manual Construction (No Azure SDK)</h3>
 * <pre>{@code
 * AzureExpressRouteAdapter<?> adapter = AzureExpressRouteAdapter.of(
 *     "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",  // ExpressRoute Service Key
 *     "Silicon Valley",                          // Peering location
 *     "equinix-azure-profile-uuid",              // Equinix service profile
 *     PeeringType.PRIVATE                        // Peering type
 * );
 * }</pre>
 *
 * @param <T> the type of the Azure SDK object being adapted
 * @author ianjones
 * @see CloudProviderConnectionAdapter
 */
@Getter
public class AzureExpressRouteAdapter<T> implements CloudProviderConnectionAdapter<T> {

    private final T source;
    private final String authenticationKey;
    private final String sellerRegion;
    private final String serviceProfileUuid;
    private final PeeringType preferredPeeringType;

    /**
     * Creates an adapter wrapping an Azure ExpressRoute SDK object.
     *
     * @param source                the Azure SDK object (e.g., {@code ExpressRouteCircuit})
     * @param expressRouteServiceKey the ExpressRoute Service Key (GUID)
     * @param peeringLocation       the Azure peering location (e.g., {@code "Silicon Valley"})
     * @param serviceProfileUuid    the Equinix Fabric service profile UUID for Azure
     * @param peeringType           the peering type (PRIVATE or MICROSOFT)
     */
    public AzureExpressRouteAdapter(T source, String expressRouteServiceKey, String peeringLocation,
                                    String serviceProfileUuid, PeeringType peeringType) {
        this.source = source;
        this.authenticationKey = expressRouteServiceKey;
        this.sellerRegion = peeringLocation;
        this.serviceProfileUuid = serviceProfileUuid;
        this.preferredPeeringType = peeringType;
    }

    /**
     * Creates an adapter without an Azure SDK object, using manually specified parameters.
     *
     * @param expressRouteServiceKey the ExpressRoute Service Key (GUID)
     * @param peeringLocation       the Azure peering location
     * @param serviceProfileUuid    the Equinix Fabric service profile UUID for Azure
     * @param peeringType           the peering type
     * @return a new adapter configured for Azure ExpressRoute
     */
    public static AzureExpressRouteAdapter<Void> of(String expressRouteServiceKey, String peeringLocation,
                                                     String serviceProfileUuid, PeeringType peeringType) {
        return new AzureExpressRouteAdapter<>(null, expressRouteServiceKey, peeringLocation,
                serviceProfileUuid, peeringType);
    }

    @Override
    public CloudProviderType getProviderType() {
        return CloudProviderType.AZURE;
    }

    @Override
    public ConnectionType getPreferredConnectionType() {
        return ConnectionType.EVPL_VC;
    }

    @Override
    public LinkProtocol getPreferredLinkProtocol() {
        return null; // VLAN tag must be user-specified
    }

    @Override
    public PeeringType getPreferredPeeringType() {
        return preferredPeeringType;
    }
}
