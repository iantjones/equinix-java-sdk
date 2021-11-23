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
import api.equinix.javasdk.networkedge.model.PublicKey;
import api.equinix.javasdk.networkedge.model.json.creators.ACLTemplateOperator;
import api.equinix.javasdk.networkedge.model.json.creators.PublicKeyOperator;

import java.util.List;

/**
 * <p>PublicKeys interface.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public interface PublicKeys {

    /**
     * Lists available Public Keys.
     *
     * @return {@link api.equinix.javasdk.core.http.response.PaginatedList}
     */
    List<PublicKey> list();

    /**
     * Lists available Public Keys for the provided accountUcmId.
     *
     * @param accountUcmId the unique account identifier.
     * @return {@link api.equinix.javasdk.core.http.response.PaginatedList}
     */
    List<PublicKey> list(String accountUcmId);

    /**
     * Gets the specified Public Key.
     *
     * @param uuid the unique identifier of the Public Key.
     * @return {@link api.equinix.javasdk.networkedge.model.PublicKey}
     */
    PublicKey getByUuid(String uuid);

    /**
     * Returns an instance of PublicKeyBuilder for defining a new Public Key.
     *
     * @param keyName the name of the new Public Key.
     * @param keyValue the value of the new Public Key.
     * @return {@link api.equinix.javasdk.networkedge.model.json.creators.PublicKeyOperator.PublicKeyBuilder}
     */
    PublicKeyOperator.PublicKeyBuilder define(String keyName, String keyValue);
}
