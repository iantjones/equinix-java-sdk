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

package api.equinix.javasdk;

import api.equinix.javasdk.fabric.client.CloudRouters;
import api.equinix.javasdk.fabric.client.Connections;
import api.equinix.javasdk.fabric.client.Metros;
import api.equinix.javasdk.fabric.client.Prices;
import api.equinix.javasdk.fabric.client.RoutingProtocols;
import api.equinix.javasdk.fabric.client.ServiceProfiles;

/**
 * Narrow capability view of {@link Fabric} that the value-add design engines depend on —
 * the metro/service-profile/cloud-router/connection/routing-protocol resources the optimizer,
 * deployment wizard, and peering intelligence actually use.
 *
 * <p>{@code Fabric} implements this interface, so existing callers (which pass a concrete
 * {@code Fabric}) are unaffected; the engines, however, depend only on this small interface rather
 * than the full concrete client. That is the composition boundary the {@code design} module
 * extraction was built around: it keeps the engines decoupled from the entire Fabric surface
 * and makes them straightforward to unit-test against a stub gateway.</p>
 *
 * <p><strong>Custom implementations</strong> need not back every method with a live client. The
 * engines degrade deliberately when a surface is unavailable: the deployment wizard's
 * {@code PlanValidator} records checks it cannot run as SKIPPED (with the reason) rather than
 * passing or failing them, the optimizer reports an unreadable catalog as an incomplete scan, and
 * peering intelligence marks Fabric availability as "not analyzed". A stub that throws from a
 * method it cannot serve is therefore a legitimate offline gateway — but {@link #metros()} and
 * {@link #serviceProfiles()} feed everything and should be real for any meaningful run.</p>
 *
 * @author ianjones
 */
public interface FabricGateway {

    /**
     * @return the Metros client — the metro catalog (codes, regions, coordinates, connected-metro
     *         latencies) every design engine reads
     */
    Metros metros();

    /**
     * @return the Service Profiles client — provider availability for the optimizer, z-side
     *         profile selection for the wizard, and Fabric on-ramp cross-referencing for peering
     *         intelligence
     */
    ServiceProfiles serviceProfiles();

    /**
     * @return the Cloud Routers client — used by the deployment wizard to validate router
     *         packages and to create Cloud Routers on {@code execute()}
     */
    CloudRouters cloudRouters();

    /**
     * @return the Connections client — used by the deployment wizard for connection dry-runs and
     *         creation
     */
    Connections connections();

    /**
     * @return the Routing Protocols client — used by the deployment wizard to attach BGP/DIRECT
     *         protocols to created connections
     */
    RoutingProtocols routingProtocols();

    /**
     * @return the Prices client — live Fabric pricing for {@code EquinixRateCard} and the
     *         engines' cost estimation
     */
    Prices prices();
}
