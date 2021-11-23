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

package api.equinix.javasdk.networkedge.client.implementation;

import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.NetworkEdge;
import api.equinix.javasdk.networkedge.client.PublicKeys;
import api.equinix.javasdk.networkedge.client.internal.PublicKeyClient;
import api.equinix.javasdk.networkedge.model.PublicKey;
import api.equinix.javasdk.networkedge.model.json.creators.PublicKeyOperator;
import api.equinix.javasdk.networkedge.model.json.PublicKeyJson;
import api.equinix.javasdk.networkedge.model.wrappers.PublicKeyWrapper;
import lombok.Getter;

import java.util.List;

/**
 * <p>PublicKeysImpl class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
@Getter
public class PublicKeysImpl implements PublicKeys {

    private final NetworkEdge serviceManager;

    private final PublicKeyClient<PublicKey> serviceClient;

    /**
     * <p>Constructor for PublicKeysImpl.</p>
     *
     * @param serviceClient a {@link api.equinix.javasdk.networkedge.client.internal.PublicKeyClient} object.
     * @param serviceManager a {@link api.equinix.javasdk.NetworkEdge} object.
     */
    public PublicKeysImpl(PublicKeyClient<PublicKey> serviceClient,
                          NetworkEdge serviceManager) {
        this.serviceManager = serviceManager;
        this.serviceClient = serviceClient;
    }

    /**
     * <p>list.</p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<PublicKey> list() {
        return list(null);
    }

    /** {@inheritDoc} */
    public List<PublicKey> list(String accountUcmId) {
        List<PublicKeyJson> publicKeyList = serviceClient.list(accountUcmId);
        return Utils.mapList(publicKeyList, this.serviceClient, PublicKeyWrapper::new);
    }

    /** {@inheritDoc} */
    public PublicKey getByUuid(String uuid) {
        PublicKeyJson publicKeyJson = serviceClient.getByUuid(uuid);
        return new PublicKeyWrapper(publicKeyJson, this.serviceClient);
    }

    /** {@inheritDoc} */
    public PublicKeyOperator.PublicKeyBuilder define(String keyName, String keyValue) {
        return new PublicKeyOperator(this.serviceClient).create(keyName, keyValue);
    }
}
