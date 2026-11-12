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

package com.eqixiac.equinix.networkedge.client.internal.implementation;

import com.eqixiac.equinix.core.client.ResourceClientBase;
import com.eqixiac.equinix.core.http.ParameterMapper;
import com.eqixiac.equinix.networkedge.client.implementation.NetworkEdgeConfigImpl;
import com.eqixiac.equinix.networkedge.client.internal.PublicKeyClient;
import com.eqixiac.equinix.networkedge.model.PublicKey;
import com.eqixiac.equinix.networkedge.model.json.PublicKeyJson;
import com.eqixiac.equinix.networkedge.model.json.creators.PublicKeyCreatorJson;
import com.eqixiac.equinix.networkedge.model.wrappers.PublicKeyWrapper;

import java.util.List;
import java.util.Map;

/**
 *
 * @author ianjones
 */
public class PublicKeyClientImpl extends ResourceClientBase<PublicKey, PublicKeyJson> implements PublicKeyClient<PublicKey> {

    public PublicKeyClientImpl(NetworkEdgeConfigImpl configClient) {
        super(configClient, "NetworkEdge", "PublicKeys", PublicKeyJson.class);
    }

    @Override
    protected PublicKey wrap(PublicKeyJson json) {
        return new PublicKeyWrapper(json, this);
    }

    public List<PublicKeyJson> list(String accountUcmId) {
        Map<String, List<String>> qParams = ParameterMapper.singleParamMap("accountUcmId" , accountUcmId);
        PublicKeyJson.NestedList nestedList = getAs("ListPublicKeys", null, qParams, PublicKeyJson.NestedList.class);
        return nestedList.getData();
    }

    /**
     * {@code POST /ne/v1/publicKeys}. Live (2026-11-11) the service answers {@code 201} with an
     * empty body and the new id only in the {@code Location} header ({@code /v1/publicKeys/{uuid}});
     * the spec documents a {@code PublicKeyResponse} body. The key is therefore read back from the
     * list by that id.
     */
    public PublicKeyJson create(PublicKeyCreatorJson publicKeyCreatorJson) {
        String uuid = createReturningLocationUuid("CreatePublicKey", null, null, publicKeyCreatorJson);
        if (uuid == null) {
            throw new com.eqixiac.equinix.core.exception.EquinixClientException(
                    "POST /ne/v1/publicKeys returned no Location header with the new key id");
        }
        List<PublicKeyJson> keys = list(publicKeyCreatorJson.getAccountUcmId());
        if (keys != null) {
            for (PublicKeyJson key : keys) {
                if (uuid.equalsIgnoreCase(key.getUuid())) {
                    return key;
                }
            }
        }
        return PublicKeyJson.of(uuid, publicKeyCreatorJson.getKeyName(), publicKeyCreatorJson.getKeyValue(),
                publicKeyCreatorJson.getKeyType());
    }

    /**
     * {@code DELETE /ne/v1/publicKeys/{uuid}}: not in the published network-edgev1 spec; answers
     * {@code 204} live (observed 2026-11-11).
     */
    public Boolean delete(String uuid) {
        return booleanOp("DeletePublicKey", com.eqixiac.equinix.core.enums.RequestType.SINGLE,
                Map.of("uuid", uuid), null, null);
    }
}
