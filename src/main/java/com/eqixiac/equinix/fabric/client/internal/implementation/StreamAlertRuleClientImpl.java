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

package com.eqixiac.equinix.fabric.client.internal.implementation;

import com.eqixiac.equinix.core.client.ResourceClientBase;
import com.eqixiac.equinix.core.http.response.Page;
import com.eqixiac.equinix.fabric.client.implementation.FabricConfigImpl;
import com.eqixiac.equinix.fabric.client.internal.StreamAlertRuleClient;
import com.eqixiac.equinix.fabric.model.StreamAlertRule;
import com.eqixiac.equinix.fabric.model.json.StreamAlertRuleJson;
import com.eqixiac.equinix.fabric.model.json.creators.StreamAlertRuleCreatorJson;
import com.eqixiac.equinix.fabric.model.json.creators.StreamAlertRulePutJson;
import com.eqixiac.equinix.fabric.model.wrappers.StreamAlertRuleWrapper;

import java.util.Map;

public class StreamAlertRuleClientImpl extends ResourceClientBase<StreamAlertRule, StreamAlertRuleJson> implements StreamAlertRuleClient<StreamAlertRule> {

    public StreamAlertRuleClientImpl(FabricConfigImpl configClient) {
        super(configClient, "Fabric", "StreamAlertRules", StreamAlertRuleJson.class);
    }

    @Override
    protected StreamAlertRule wrap(StreamAlertRuleJson json) {
        return new StreamAlertRuleWrapper(json, this);
    }

    public Page<StreamAlertRuleJson> list(String streamId) {
        return listPagePath("GetStreamAlertRules", Map.of("streamId", streamId));
    }

    public StreamAlertRuleJson getByUuid(String streamId, String uuid) {
        return getOne("GetStreamAlertRule", Map.of("streamId", streamId, "uuid", uuid));
    }

    public StreamAlertRuleJson create(String streamId, StreamAlertRuleCreatorJson creatorJson) {
        return postOne("PostStreamAlertRule", Map.of("streamId", streamId), creatorJson);
    }

    public StreamAlertRuleJson update(String streamId, String uuid, StreamAlertRulePutJson putJson) {
        // PATCH /streams/{streamId}/alertRules/{uuid} with a full AlertRulePutRequest body (application/json).
        return updateOne("UpdateStreamAlertRule", Map.of("streamId", streamId, "uuid", uuid), putJson);
    }

    public StreamAlertRuleJson delete(String streamId, String uuid) {
        return deleteOne("DeleteStreamAlertRule", Map.of("streamId", streamId, "uuid", uuid));
    }

    public StreamAlertRuleJson refresh(String streamId, String uuid) {
        return getByUuid(streamId, uuid);
    }
}
