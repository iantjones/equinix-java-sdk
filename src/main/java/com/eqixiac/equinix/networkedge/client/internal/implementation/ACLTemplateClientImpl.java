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
import com.eqixiac.equinix.core.http.response.Page;
import com.eqixiac.equinix.core.enums.RequestType;
import com.eqixiac.equinix.networkedge.client.implementation.NetworkEdgeConfigImpl;
import com.eqixiac.equinix.networkedge.client.internal.ACLTemplateClient;
import com.eqixiac.equinix.networkedge.model.ACLTemplate;
import com.eqixiac.equinix.networkedge.model.json.ACLTemplateJson;
import com.eqixiac.equinix.networkedge.model.json.creators.ACLTemplateCreatorJson;
import com.eqixiac.equinix.networkedge.model.json.creators.ACLTemplateUpdaterJson;
import com.eqixiac.equinix.networkedge.model.wrappers.ACLTemplateWrapper;

import java.util.List;
import java.util.Map;

/**
 *
 * @author ianjones
 */
public class ACLTemplateClientImpl extends ResourceClientBase<ACLTemplate, ACLTemplateJson> implements ACLTemplateClient<ACLTemplate> {

    public ACLTemplateClientImpl(NetworkEdgeConfigImpl configClient) {
        super(configClient, "NetworkEdge", "ACLTemplates", ACLTemplateJson.class);
    }

    @Override
    protected ACLTemplate wrap(ACLTemplateJson json) {
        return new ACLTemplateWrapper(json, this);
    }

    public Page<ACLTemplateJson> list(String accountUcmId) {
        Map<String, List<String>> qParams = ParameterMapper.singleParamMap("accountUcmId" , accountUcmId);
        return listPage("ListACLTemplates", qParams);
    }

    public ACLTemplateJson getByUuid(String uuid, String accountUcmId) {
        Map<String, String> pParams = Map.of("uuid", uuid);
        Map<String, List<String>> qParams = ParameterMapper.singleParamMap("accountUcmId" , accountUcmId);
        return getAs("GetACLTemplate", pParams, qParams, ACLTemplateJson.class);
    }

    public ACLTemplateJson create(ACLTemplateCreatorJson aclTemplateCreatorJson) {
        String uuid = createReturningLocationUuid("CreateACLTemplate", null,
                ParameterMapper.singleParamMap("accountUcmId", aclTemplateCreatorJson.getAccountUcmId()), aclTemplateCreatorJson);
        return getByUuid(uuid, aclTemplateCreatorJson.getAccountUcmId());
    }

    public ACLTemplateJson update(String uuid, ACLTemplateUpdaterJson aclTemplateUpdaterJson) {
        Map<String, String> pParams = Map.of("uuid", uuid);
        Map<String, List<String>> qParams = ParameterMapper.singleParamMap("accountUcmId" , aclTemplateUpdaterJson.getAccountUcmId());
        voidOp("UpdateACLTemplate", RequestType.SINGLE, pParams, qParams, aclTemplateUpdaterJson);
        return getByUuid(uuid, aclTemplateUpdaterJson.getAccountUcmId());
    }

    public Boolean delete(String uuid, String accountUcmId) {
        Map<String, String> pParams = Map.of("uuid", uuid);
        Map<String, List<String>> qParams = ParameterMapper.singleParamMap("accountUcmId" , accountUcmId);
        return booleanOp("DeleteACLTemplate", RequestType.SINGLE, pParams, qParams, null);
    }

    public ACLTemplateJson refresh(String uuid, String accountUcmId) {
        return this.getByUuid(uuid, accountUcmId);
    }
}
