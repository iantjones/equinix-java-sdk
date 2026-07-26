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
import com.eqixiac.equinix.fabric.client.internal.implementation.AgentClientImpl;
import com.eqixiac.equinix.fabric.model.Agent;
import com.eqixiac.equinix.fabric.model.json.AgentJson;
import com.eqixiac.equinix.fabric.model.json.creators.AgentOperator;
import lombok.Getter;
import lombok.experimental.Delegate;

public class AgentWrapper extends ResourceImpl<Agent> implements Agent {

    @Delegate(excludes = AgentMutability.class)
    private AgentJson jsonObject;
    @Getter
    private final Pageable<Agent> serviceClient;

    public AgentWrapper(AgentJson jsonObject, Pageable<Agent> serviceClient) {
        this.jsonObject = jsonObject;
        this.serviceClient = serviceClient;
    }

    public AgentOperator.AgentUpdater update() {
        return new AgentOperator((AgentClientImpl) this.serviceClient).update(this.getUuid());
    }

    public Boolean delete() {
        this.jsonObject = ((AgentClientImpl) this.serviceClient).delete(this.getUuid());
        return true;
    }

    public void refresh() {
        this.jsonObject = ((AgentClientImpl) this.serviceClient).refresh(this.getUuid());
    }

    private interface AgentMutability {
        AgentOperator.AgentUpdater update();
        Boolean delete();
        void refresh();
    }
}
