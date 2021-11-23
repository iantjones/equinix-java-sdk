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

package api.equinix.javasdk.fabric.client;

import api.equinix.javasdk.core.enums.Side;
import api.equinix.javasdk.fabric.model.ServiceToken;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.fabric.model.json.creators.ServiceTokenOperator;

/**
 * <p>ServiceTokens interface.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public interface ServiceTokens {

    /**
     * <p>list.</p>
     *
     * @return a {@link api.equinix.javasdk.core.http.response.PaginatedList} object.
     */
    PaginatedList<ServiceToken> list();

    /**
     * <p>getByUuid.</p>
     *
     * @param uuid a {@link java.lang.String} object.
     * @return a {@link api.equinix.javasdk.fabric.model.ServiceToken} object.
     */
    ServiceToken getByUuid(String uuid);

    /**
     * <p>define.</p>
     *
     * @param issuerSide a {@link api.equinix.javasdk.core.enums.Side} object.
     * @return a {@link api.equinix.javasdk.fabric.model.json.creators.ServiceTokenOperator.ServiceTokenBuilder} object.
     */
    ServiceTokenOperator.ServiceTokenBuilder define(Side issuerSide);
}
