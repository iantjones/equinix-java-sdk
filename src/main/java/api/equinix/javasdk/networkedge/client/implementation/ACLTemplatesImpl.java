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

import api.equinix.javasdk.core.http.ResponseHandler;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.NetworkEdge;
import api.equinix.javasdk.networkedge.client.ACLTemplates;
import api.equinix.javasdk.networkedge.client.internal.ACLTemplateClient;
import api.equinix.javasdk.networkedge.model.ACLTemplate;
import api.equinix.javasdk.networkedge.model.json.ACLTemplateJson;
import api.equinix.javasdk.networkedge.model.json.creators.ACLTemplateOperator;
import api.equinix.javasdk.networkedge.model.wrappers.ACLTemplateWrapper;
import lombok.Getter;

/**
 *
 * @author ianjones
 */
@Getter
public class ACLTemplatesImpl implements ACLTemplates {

    private final NetworkEdge serviceManager;

    private final ACLTemplateClient<ACLTemplate> serviceClient;

    public ACLTemplatesImpl(ACLTemplateClient<ACLTemplate> serviceClient, NetworkEdge serviceManager) {
        this.serviceManager = serviceManager;
        this.serviceClient = serviceClient;
    }

    public PaginatedList<ACLTemplate> list() {
        return list(null);
    }

    public PaginatedList<ACLTemplate> list(String accountUcmId) {
        Page<ACLTemplateJson> responsePage = serviceClient.list(accountUcmId);
        PaginatedList<ACLTemplate> deviceList = ResponseHandler.mapPaginatedList(responsePage.getItems(), this.serviceClient, ACLTemplateWrapper::new);
        return new PaginatedList<>(deviceList, this.serviceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    public ACLTemplate getByUuid(String uuid) {
        return getByUuid(uuid, null);
    }

    public ACLTemplate getByUuid(String uuid, String accountUcmId) {
        ACLTemplateJson deviceLinkJson = serviceClient.getByUuid(uuid, accountUcmId);
        return new ACLTemplateWrapper(deviceLinkJson, this.serviceClient);
    }

    public ACLTemplateOperator.ACLTemplateBuilder define(String aclTemplateName) {
        return new ACLTemplateOperator(this.serviceClient).create(aclTemplateName);
    }
}
