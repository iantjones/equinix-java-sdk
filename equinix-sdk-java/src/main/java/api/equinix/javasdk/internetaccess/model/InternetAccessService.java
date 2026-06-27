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

import api.equinix.javasdk.internetaccess.enums.ServiceState;
import api.equinix.javasdk.internetaccess.enums.ServiceTypeV2;

/**
 * An Equinix Internet Access (EIA) v2 service, as returned by
 * {@code POST /internetAccess/v2/services}.
 *
 * <p>This is a read-only response view; the nested IP blocks and routing configuration are
 * supplied at creation time through the
 * {@link api.equinix.javasdk.internetaccess.client.InternetAccessServices#define() builder}.</p>
 */
public interface InternetAccessService {

    /**
     * @return the unique identifier of the service
     */
    String getUuid();

    /**
     * @return the topology of the service ({@code SINGLE} or {@code DUAL})
     */
    ServiceTypeV2 getType();

    /**
     * @return service bandwidth in Mbps
     */
    Integer getBandwidth();

    /**
     * @return the lifecycle state of the service
     */
    ServiceState getState();
}
