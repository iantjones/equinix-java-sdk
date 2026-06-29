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

package api.equinix.javasdk.internetaccess.model;

import api.equinix.javasdk.internetaccess.enums.Redundancy;
import api.equinix.javasdk.internetaccess.enums.UseCase;
import api.equinix.javasdk.internetaccess.model.implementation.CustomerRouteRoutingProtocol;

/**
 * An allowed customer-route configuration for an Equinix Internet Access (EIA) v1 service, as
 * returned by {@code GET /internetAccess/v1/customerRouteConfigurations}.
 *
 * <p>This is a read-only response view.</p>
 */
public interface CustomerRouteConfiguration {

    /**
     * @return the redundancy configuration ({@code SINGLE_PORT} or {@code DUAL_PORT})
     */
    Redundancy getType();

    /**
     * @return the intended use case ({@code MAIN}, {@code BACKUP} or {@code MANAGEMENT_ACCESS})
     */
    UseCase getUseCase();

    /**
     * @return the allowed routing-protocol / customer-route configuration
     */
    CustomerRouteRoutingProtocol getRoutingProtocol();
}
