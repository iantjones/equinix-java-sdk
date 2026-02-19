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

package api.equinix.javasdk.networkedge.client;

import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.networkedge.model.VPN;
import api.equinix.javasdk.networkedge.model.json.creators.VPNOperator;

/**
 * Client interface for managing VPN configurations on Network Edge devices. Provides
 * operations to list, retrieve, and create site-to-site VPN connections.
 *
 * @author ianjones
 * @version $Id: $Id
 */
public interface VPNs {

    /**
     * Lists available VPNs.
     *
     * @return {@link api.equinix.javasdk.core.http.response.PaginatedList}
     */
    PaginatedList<VPN> list();

    /**
     * Lists VPNs based on the parameters provided.
     *
     * @param requestBuilder the desired query parameters.
     * @return {@link api.equinix.javasdk.core.http.response.PaginatedList}
     */
    PaginatedList<VPN> list(RequestBuilder.VPN requestBuilder);

    /**
     * Gets the specified VPN.
     *
     * @param uuid the unique identifier of the VPN.
     * @return {@link api.equinix.javasdk.networkedge.model.VPN}
     */
    VPN getByUuid(String uuid);

    /**
     * Returns an instance of VPNBuilder for defining a new VPN.
     *
     * @param configName the configuration name of the new VPN.
     * @return {@link api.equinix.javasdk.networkedge.model.json.creators.VPNOperator.VPNBuilder}
     */
    VPNOperator.VPNBuilder define(String configName);
}
