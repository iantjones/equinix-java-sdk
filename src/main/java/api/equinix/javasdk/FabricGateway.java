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
 * than the full concrete client. That is the composition boundary the {@code design} and
 * {@code mcp} module extraction was built around: it keeps the engines decoupled from the entire
 * Fabric surface and makes them straightforward to unit-test against a stub gateway.</p>
 *
 * @author ianjones
 */
public interface FabricGateway {

    /** @return the Metros resource client. */
    Metros metros();

    /** @return the Service Profiles resource client. */
    ServiceProfiles serviceProfiles();

    /** @return the Cloud Routers resource client. */
    CloudRouters cloudRouters();

    /** @return the Connections resource client. */
    Connections connections();

    /** @return the Routing Protocols resource client. */
    RoutingProtocols routingProtocols();

    /** @return the Pricing resource client, used by the value-realization cost and savings models. */
    Prices prices();
}