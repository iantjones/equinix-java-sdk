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

package com.eqixiac.equinix.networkedge.client;

import com.eqixiac.equinix.networkedge.model.PublicKey;
import com.eqixiac.equinix.networkedge.model.json.creators.PublicKeyOperator;

import java.util.List;

/**
 * Client interface for managing SSH public keys on Network Edge. Provides operations
 * to list, retrieve, and create public keys used for secure authentication to
 * virtual devices.
 *
 * @author ianjones
 */
public interface PublicKeys {

    /**
     * Lists available Public Keys.
     *
     * @return {@link com.eqixiac.equinix.core.http.response.PaginatedList}
     */
    List<PublicKey> list();

    /**
     * Lists available Public Keys for the provided accountUcmId.
     *
     * @param accountUcmId the unique account identifier.
     * @return {@link com.eqixiac.equinix.core.http.response.PaginatedList}
     */
    List<PublicKey> list(String accountUcmId);

    /**
     * Returns an instance of PublicKeyBuilder for defining a new Public Key.
     *
     * @param keyName the name of the new Public Key.
     * @param keyValue the value of the new Public Key.
     * @return {@link com.eqixiac.equinix.networkedge.model.json.creators.PublicKeyOperator.PublicKeyBuilder}
     */
    PublicKeyOperator.PublicKeyBuilder define(String keyName, String keyValue);
}
