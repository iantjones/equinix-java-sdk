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
import api.equinix.javasdk.fabric.client.StreamAlertRules;
import api.equinix.javasdk.fabric.client.internal.StreamAlertRuleClient;
import api.equinix.javasdk.fabric.model.StreamAlertRule;
import api.equinix.javasdk.fabric.model.json.StreamAlertRuleJson;
import api.equinix.javasdk.fabric.model.json.creators.StreamAlertRuleOperator;
import api.equinix.javasdk.fabric.model.wrappers.StreamAlertRuleWrapper;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class StreamAlertRulesImpl implements StreamAlertRules {

    private final StreamAlertRuleClient<StreamAlertRule> serviceClient;

    public PaginatedList<StreamAlertRule> list(String streamId) {
        Page<StreamAlertRuleJson> responsePage = this.serviceClient.list(streamId);
        PaginatedList<StreamAlertRule> alertRuleList = ResponseHandler.mapPaginatedList(responsePage.getItems(), this.serviceClient, StreamAlertRuleWrapper::new);
        return new PaginatedList<>(alertRuleList, this.serviceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    public StreamAlertRule getByUuid(String streamId, String uuid) {
        StreamAlertRuleJson json = this.serviceClient.getByUuid(streamId, uuid);
        return new StreamAlertRuleWrapper(json, this.serviceClient);
    }

    public StreamAlertRuleOperator.StreamAlertRuleBuilder define(String streamId) {
        return new StreamAlertRuleOperator(this.serviceClient).create(streamId);
    }
}
