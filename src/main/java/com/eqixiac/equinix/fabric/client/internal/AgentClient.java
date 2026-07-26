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

package com.eqixiac.equinix.fabric.client.internal;

import com.eqixiac.equinix.core.http.request.PatchOperation;
import com.eqixiac.equinix.core.http.response.Page;
import com.eqixiac.equinix.core.http.response.PageablePost;
import com.eqixiac.equinix.fabric.model.Agent;
import com.eqixiac.equinix.fabric.model.AgentActivity;
import com.eqixiac.equinix.fabric.model.json.AgentActivityJson;
import com.eqixiac.equinix.fabric.model.json.AgentJson;
import com.eqixiac.equinix.fabric.model.json.creators.AgentCreatorJson;

import java.util.List;

public interface AgentClient<T> extends PageablePost<T> {

    Page<AgentJson> list();

    AgentJson getByUuid(String uuid);

    AgentJson create(AgentCreatorJson agentCreatorJson);

    AgentJson update(String uuid, List<PatchOperation> operations);

    AgentJson delete(String uuid);

    List<AgentActivity> activities(String agentId);

    AgentJson refresh(String uuid);
}
