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

package api.equinix.javasdk.networkedge.client.internal.implementation;

import api.equinix.javasdk.core.client.ResourceClientBase;
import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.request.EquinixRequest;
import api.equinix.javasdk.core.http.response.EquinixResponse;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.internal.Constants;
import api.equinix.javasdk.core.enums.RequestType;
import api.equinix.javasdk.networkedge.client.implementation.NetworkEdgeConfigImpl;
import api.equinix.javasdk.networkedge.client.internal.ACLTemplateClient;
import api.equinix.javasdk.networkedge.model.ACLTemplate;
import api.equinix.javasdk.networkedge.model.json.ACLTemplateJson;
import api.equinix.javasdk.networkedge.model.json.creators.ACLTemplateCreatorJson;
import api.equinix.javasdk.networkedge.model.json.creators.ACLTemplateUpdaterJson;
import api.equinix.javasdk.networkedge.model.wrappers.ACLTemplateWrapper;

import java.util.List;
import java.util.Map;

/**
 * <p>ACLTemplateClientImpl class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class ACLTemplateClientImpl extends ResourceClientBase<ACLTemplate, ACLTemplateJson> implements ACLTemplateClient<ACLTemplate> {

    /**
     * <p>Constructor for ACLTemplateClientImpl.</p>
     *
     * @param configClient a {@link api.equinix.javasdk.networkedge.client.implementation.NetworkEdgeConfigImpl} object.
     */
    public ACLTemplateClientImpl(NetworkEdgeConfigImpl configClient) {
        super(configClient, "NetworkEdge", "ACLTemplates", ACLTemplateJson.class);
    }

    /** {@inheritDoc} */
    @Override
    protected ACLTemplate wrap(ACLTemplateJson json) {
        return new ACLTemplateWrapper(json, this);
    }

    /** {@inheritDoc} */
    public Page<ACLTemplate, ACLTemplateJson> list(String accountUcmId) {
        Map<String, List<String>> qParams = Utils.singleParamMap("accountUcmId" , accountUcmId);
        return listPage("ListACLTemplates", qParams);
    }

    /** {@inheritDoc} */
    public ACLTemplateJson getByUuid(String uuid, String accountUcmId) {
        Map<String, String> pParams = Map.of("uuid", uuid);
        Map<String, List<String>> qParams = Utils.singleParamMap("accountUcmId" , accountUcmId);
        return getAs("GetACLTemplate", pParams, qParams, ACLTemplateJson.class);
    }

    /** {@inheritDoc} */
    public ACLTemplateJson create(ACLTemplateCreatorJson aclTemplateCreatorJson) {
        Map<String, List<String>> qParams = Utils.singleParamMap("accountUcmId" , aclTemplateCreatorJson.getAccountUcmId());
        EquinixRequest<ACLTemplateJson> equinixRequest = this.buildRequest("CreateACLTemplate", RequestType.SINGLE, null, qParams, ACLTemplateJson.class);
        Utils.serializeJson(equinixRequest, aclTemplateCreatorJson);
        EquinixResponse<ACLTemplateJson> equinixResponse = this.invoke(equinixRequest);
        String aclTemplateUuid = Utils.extractFromHeader(equinixResponse, "Location", Constants.UUID_PATTERN);
        return getByUuid(aclTemplateUuid, aclTemplateCreatorJson.getAccountUcmId());
    }

    /** {@inheritDoc} */
    public ACLTemplateJson update(String uuid, ACLTemplateUpdaterJson aclTemplateUpdaterJson) {
        Map<String, String> pParams = Map.of("uuid", uuid);
        Map<String, List<String>> qParams = Utils.singleParamMap("accountUcmId" , aclTemplateUpdaterJson.getAccountUcmId());
        voidOp("UpdateACLTemplate", RequestType.SINGLE, pParams, qParams, aclTemplateUpdaterJson);
        return getByUuid(uuid, aclTemplateUpdaterJson.getAccountUcmId());
    }

    /** {@inheritDoc} */
    public Boolean delete(String uuid, String accountUcmId) {
        Map<String, String> pParams = Map.of("uuid", uuid);
        Map<String, List<String>> qParams = Utils.singleParamMap("accountUcmId" , accountUcmId);
        return booleanOp("DeleteACLTemplate", RequestType.SINGLE, pParams, qParams, null);
    }

    /** {@inheritDoc} */
    public ACLTemplateJson refresh(String uuid, String accountUcmId) {
        return this.getByUuid(uuid, accountUcmId);
    }
}
