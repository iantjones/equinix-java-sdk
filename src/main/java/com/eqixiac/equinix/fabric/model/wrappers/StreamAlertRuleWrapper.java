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

package com.eqixiac.equinix.fabric.model.wrappers;

import com.eqixiac.equinix.core.http.response.Pageable;
import com.eqixiac.equinix.core.model.ResourceImpl;
import com.eqixiac.equinix.fabric.client.internal.implementation.StreamAlertRuleClientImpl;
import com.eqixiac.equinix.fabric.model.StreamAlertRule;
import com.eqixiac.equinix.fabric.model.json.StreamAlertRuleJson;
import com.eqixiac.equinix.fabric.model.json.creators.StreamAlertRuleOperator;
import lombok.Getter;
import lombok.experimental.Delegate;

public class StreamAlertRuleWrapper extends ResourceImpl<StreamAlertRule> implements StreamAlertRule {

    @Delegate(excludes = StreamAlertRuleMutability.class)
    private StreamAlertRuleJson jsonObject;
    @Getter
    private final Pageable<StreamAlertRule> serviceClient;

    public StreamAlertRuleWrapper(StreamAlertRuleJson jsonObject, Pageable<StreamAlertRule> serviceClient) {
        this.jsonObject = jsonObject;
        this.serviceClient = serviceClient;
    }

    public StreamAlertRuleOperator.StreamAlertRuleUpdater update(String streamId) {
        return new StreamAlertRuleOperator((StreamAlertRuleClientImpl) this.serviceClient).update(streamId, this.getUuid());
    }

    public Boolean delete(String streamId) {
        this.jsonObject = ((StreamAlertRuleClientImpl) this.serviceClient).delete(streamId, this.getUuid());
        return true;
    }

    public void refresh(String streamId) {
        this.jsonObject = ((StreamAlertRuleClientImpl) this.serviceClient).refresh(streamId, this.getUuid());
    }

    private interface StreamAlertRuleMutability {
        StreamAlertRuleOperator.StreamAlertRuleUpdater update(String streamId);
        Boolean delete(String streamId);
        void refresh(String streamId);
    }
}
