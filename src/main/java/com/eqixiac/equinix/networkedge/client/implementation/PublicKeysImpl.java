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

package com.eqixiac.equinix.networkedge.client.implementation;

import com.eqixiac.equinix.core.http.ResponseHandler;
import com.eqixiac.equinix.NetworkEdge;
import com.eqixiac.equinix.networkedge.client.PublicKeys;
import com.eqixiac.equinix.networkedge.client.internal.PublicKeyClient;
import com.eqixiac.equinix.networkedge.model.PublicKey;
import com.eqixiac.equinix.networkedge.model.json.creators.PublicKeyOperator;
import com.eqixiac.equinix.networkedge.model.json.PublicKeyJson;
import com.eqixiac.equinix.networkedge.model.wrappers.PublicKeyWrapper;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 *
 * @author ianjones
 */
@RequiredArgsConstructor
public class PublicKeysImpl implements PublicKeys {

    private final PublicKeyClient<PublicKey> serviceClient;

    private final NetworkEdge serviceManager;

    public List<PublicKey> list() {
        return list(null);
    }

    public List<PublicKey> list(String accountUcmId) {
        List<PublicKeyJson> publicKeyList = serviceClient.list(accountUcmId);
        return ResponseHandler.mapList(publicKeyList, this.serviceClient, PublicKeyWrapper::new);
    }

    public PublicKeyOperator.PublicKeyBuilder define(String keyName, String keyValue) {
        return new PublicKeyOperator(this.serviceClient).create(keyName, keyValue);
    }
}
