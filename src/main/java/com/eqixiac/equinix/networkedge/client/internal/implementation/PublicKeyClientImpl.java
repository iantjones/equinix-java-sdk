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

    public PublicKeyJson create(PublicKeyCreatorJson publicKeyCreatorJson) {
        return postOne("CreatePublicKey", publicKeyCreatorJson);
    }
}
