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

import api.equinix.javasdk.core.http.response.Pageable;
import api.equinix.javasdk.networkedge.model.json.PublicKeyJson;
import api.equinix.javasdk.networkedge.model.json.creators.PublicKeyCreatorJson;

import java.util.List;

/**
 * <p>PublicKeyClient interface.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public interface PublicKeyClient<T> extends Pageable<T> {

    /**
     * <p>list.</p>
     *
     * @param accountUcmId a {@link java.lang.String} object.
     * @return a {@link java.util.List} object.
     */
    List<PublicKeyJson> list(String accountUcmId);

    /**
     * <p>getByUuid.</p>
     *
     * @param uuid a {@link java.lang.String} object.
     * @return a {@link api.equinix.javasdk.networkedge.model.json.PublicKeyJson} object.
     */
    PublicKeyJson getByUuid(String uuid);

    /**
     * <p>create.</p>
     *
     * @param publicKeyCreatorJson a {@link api.equinix.javasdk.networkedge.model.json.creators.PublicKeyCreatorJson} object.
     * @return a {@link api.equinix.javasdk.networkedge.model.json.PublicKeyJson} object.
     */
    PublicKeyJson create(PublicKeyCreatorJson publicKeyCreatorJson);

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
     * @return a {@link api.equinix.javasdk.networkedge.model.json.PublicKeyJson} object.
     */
    PublicKeyJson refresh(String uuid);
}
