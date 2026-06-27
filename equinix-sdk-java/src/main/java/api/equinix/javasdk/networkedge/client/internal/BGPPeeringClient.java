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

package api.equinix.javasdk.networkedge.client.internal;

import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.Pageable;
import api.equinix.javasdk.networkedge.client.RequestBuilder;
import api.equinix.javasdk.networkedge.model.BGPPeering;
import api.equinix.javasdk.networkedge.model.json.BGPPeeringJson;
import api.equinix.javasdk.networkedge.model.json.creators.BGPPeeringCreatorJson;
import api.equinix.javasdk.networkedge.model.json.creators.BGPPeeringUpdaterJson;

/**
 * <p>BGPPeeringClient interface.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public interface BGPPeeringClient<T> extends Pageable<T> {

    /**
     * <p>list.</p>
     *
     * @param requestBuilder a {@link api.equinix.javasdk.networkedge.client.RequestBuilder.BGP} object.
     * @return a {@link api.equinix.javasdk.core.http.response.Page} object.
     */
    Page<BGPPeering, BGPPeeringJson> list(RequestBuilder.BGP requestBuilder);

    /**
     * <p>getByUuid.</p>
     *
     * @param uuid a {@link java.lang.String} object.
     * @return a {@link api.equinix.javasdk.networkedge.model.json.BGPPeeringJson} object.
     */
    BGPPeeringJson getByUuid(String uuid);

    /**
     * <p>create.</p>
     *
     * @param aclTemplateCreatorJson a {@link api.equinix.javasdk.networkedge.model.json.creators.BGPPeeringCreatorJson} object.
     * @return a {@link api.equinix.javasdk.networkedge.model.json.BGPPeeringJson} object.
     */
    BGPPeeringJson create(BGPPeeringCreatorJson aclTemplateCreatorJson);

    /**
     * <p>update.</p>
     *
     * @param uuid a {@link java.lang.String} object.
     * @param vpnUpdaterJson a {@link api.equinix.javasdk.networkedge.model.json.creators.BGPPeeringUpdaterJson} object.
     * @return a {@link api.equinix.javasdk.networkedge.model.json.BGPPeeringJson} object.
     */
    BGPPeeringJson update(String uuid, BGPPeeringUpdaterJson vpnUpdaterJson);

    /**
     * <p>delete.</p>
     *
     * @param uuid a {@link java.lang.String} object.
     * @return a {@link java.lang.Boolean} object.
     */
    Boolean delete(String uuid);

    /**
     * <p>refresh.</p>
     *
     * @param uuid a {@link java.lang.String} object.
     * @return a {@link api.equinix.javasdk.networkedge.model.json.BGPPeeringJson} object.
     */
    BGPPeeringJson refresh(String uuid);
}
