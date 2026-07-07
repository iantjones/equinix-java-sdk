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

package api.equinix.javasdk.fabric.client.implementation;

import api.equinix.javasdk.core.http.ResponseHandler;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.fabric.client.AgentTemplates;
import api.equinix.javasdk.fabric.client.internal.AgentTemplateClient;
import api.equinix.javasdk.fabric.model.AgentTemplate;
import api.equinix.javasdk.fabric.model.json.AgentTemplateJson;

public class AgentTemplatesImpl implements AgentTemplates {

    private final AgentTemplateClient<AgentTemplate> serviceClient;

    public AgentTemplatesImpl(AgentTemplateClient<AgentTemplate> serviceClient) {
        this.serviceClient = serviceClient;
    }

    public PaginatedList<AgentTemplate> list() {
        Page<AgentTemplateJson> responsePage = this.serviceClient.list();
        PaginatedList<AgentTemplate> templateList = ResponseHandler.mapPaginatedList(responsePage.getItems(), this.serviceClient, (json, client) -> json);
        return new PaginatedList<>(templateList, this.serviceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    public AgentTemplate getByUuid(String uuid) {
        return this.serviceClient.getByUuid(uuid);
    }
}
