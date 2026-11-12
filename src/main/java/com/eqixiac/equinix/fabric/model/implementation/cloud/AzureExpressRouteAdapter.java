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

package com.eqixiac.equinix.fabric.model.implementation.cloud;

import com.eqixiac.equinix.fabric.enums.ConnectionType;
import com.eqixiac.equinix.fabric.enums.PeeringType;
import com.eqixiac.equinix.fabric.model.implementation.LinkProtocol;
import lombok.Getter;

/**
 * Adapter for bridging Microsoft Azure ExpressRoute SDK objects with Equinix Fabric connections.
 *
 * <p>When creating an Equinix Fabric connection to Azure ExpressRoute you need:</p>
 * <ul>
 *   <li><strong>Authentication Key</strong>: the ExpressRoute Service Key (a GUID provided by Azure)</li>
 *   <li><strong>Service Profile</strong>: the Equinix Fabric service profile UUID for Azure ExpressRoute</li>
 *   <li><strong>Peering Type</strong>: {@link PeeringType#PRIVATE} or {@link PeeringType#MICROSOFT}</li>
 *   <li><strong>Metro</strong>: the Equinix metro of the ExpressRoute peering location (for example
 *       {@code "SV"} for Silicon Valley), sent as {@code zSide.accessPoint.location.metroCode}</li>
 * </ul>
 *
 * <p>No {@code sellerRegion} is sent: none of the Fabric v4 4.34.0 Azure examples
 * ({@code Colo2Sp-Azure-Primary}, {@code Vd2Sp-Azure}, {@code Fcr2Sp-Azure}, {@code Metal2Sp-Azure})
 * carries one; the peering location is expressed by the metro. {@link #getSellerRegion()} returns
 * {@code null}.</p>
 *
 * <pre>{@code
 * AzureExpressRouteAdapter<?> adapter = AzureExpressRouteAdapter.of(
 *     "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",  // ExpressRoute Service Key
 *     "equinix-azure-profile-uuid",              // Equinix service profile
 *     PeeringType.PRIVATE,                       // Peering type
 *     "SV"                                       // Equinix metro of the peering location
 * );
 *
 * Connection conn = fabric.connections()
 *     .define(ConnectionType.EVPL_VC)
 *     .name("Azure-ExpressRoute")
 *     .bandwidth(100)
 *     .aSideAccessPointPort(portUuid, LinkProtocol.qinq().vlanSTag(1500).vlanCTag(10).create())
 *     .zSideCloudProvider(adapter)
 *     .notification("ops@example.com")
 *     .create();
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
    private final String serviceProfileUuid;
    private final PeeringType preferredPeeringType;
    private final String metroCode;

    /**
     * Creates an adapter wrapping an Azure ExpressRoute SDK object.
     *
     * @param source                 the Azure SDK object (e.g., {@code ExpressRouteCircuit})
     * @param expressRouteServiceKey the ExpressRoute Service Key (GUID)
     * @param serviceProfileUuid     the Equinix Fabric service profile UUID for Azure
     * @param peeringType            the peering type (PRIVATE or MICROSOFT)
     * @param metroCode              the Equinix metro of the peering location, e.g. {@code "SV"}; required
     */
    public AzureExpressRouteAdapter(T source, String expressRouteServiceKey, String serviceProfileUuid,
                                    PeeringType peeringType, String metroCode) {
        this.source = source;
        this.authenticationKey = expressRouteServiceKey;
        this.serviceProfileUuid = serviceProfileUuid;
        this.preferredPeeringType = peeringType;
        this.metroCode = AdapterArguments.requireMetro(metroCode);
    }

    /**
     * Creates an adapter without an Azure SDK object.
     *
     * @param expressRouteServiceKey the ExpressRoute Service Key (GUID)
     * @param serviceProfileUuid     the Equinix Fabric service profile UUID for Azure
     * @param peeringType            the peering type
     * @param metroCode              the Equinix metro of the peering location; required
     * @return a new adapter configured for Azure ExpressRoute
     */
    public static AzureExpressRouteAdapter<Void> of(String expressRouteServiceKey, String serviceProfileUuid,
                                                     PeeringType peeringType, String metroCode) {
        return new AzureExpressRouteAdapter<>(null, expressRouteServiceKey, serviceProfileUuid, peeringType, metroCode);
    }

    /**
     * Azure connections send no {@code sellerRegion} (see the class description).
     *
     * @return {@code null}
     */
    @Override
    public String getSellerRegion() {
        return null;
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

    @Override
    public String describe() {
        return getProviderType().getDisplayName() + " in metro " + metroCode;
    }
}
