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
import api.equinix.javasdk.networkedge.model.ACLTemplate;
import api.equinix.javasdk.networkedge.model.BGPPeering;
import api.equinix.javasdk.networkedge.model.SSHUser;
import api.equinix.javasdk.networkedge.model.json.creators.ACLTemplateOperator;
import api.equinix.javasdk.networkedge.model.json.creators.BGPPeeringOperator;

/**
 * <p>BGPPeerings interface.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public interface BGPPeerings {

    /**
     * Lists available BGP Peerings.
     *
     * @return {@link api.equinix.javasdk.core.http.response.PaginatedList}
     */
    PaginatedList<BGPPeering> list();

    /**
     * Lists BGP Peerings based on the parameters provided.
     *
     * @param requestBuilder the desired query parameters.
     * @return {@link api.equinix.javasdk.core.http.response.PaginatedList}
     */
    PaginatedList<BGPPeering> list(RequestBuilder.BGP requestBuilder);

    /**
     * Gets the specified BGP Peering.
     *
     * @param uuid the unique identifier of the BGP Peering.
     * @return {@link api.equinix.javasdk.networkedge.model.BGPPeering}
     */
    BGPPeering getByUuid(String uuid);

    /**
     * Returns an instance of BGPPeeringBuilder for defining a new BGP Peering.
     *
     * @return {@link api.equinix.javasdk.networkedge.model.json.creators.BGPPeeringOperator.BGPPeeringBuilder}
     */
    BGPPeeringOperator.BGPPeeringBuilder define();
}
