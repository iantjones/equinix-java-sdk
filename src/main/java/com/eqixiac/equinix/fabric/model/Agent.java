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

package com.eqixiac.equinix.fabric.model;
import com.eqixiac.equinix.fabric.enums.AgentState;

import com.eqixiac.equinix.fabric.model.implementation.AgentConfiguration;
import com.eqixiac.equinix.fabric.model.implementation.AgentTemplateRef;
import com.eqixiac.equinix.fabric.model.implementation.ChangeLog;
import com.eqixiac.equinix.fabric.model.json.creators.AgentOperator;

/**
 * A Fabric agent (for example an Autonomous Network Operations agent).
 */
public interface Agent {

    String getHref();

    String getType();

    String getUuid();

    String getName();

    String getDescription();

    AgentState getState();

    Boolean getEnabled();

    Project getProject();

    AgentTemplateRef getAgentTemplate();

    AgentConfiguration getConfiguration();

    ChangeLog getChangeLog();

    /**
     * Begins a fluent PATCH update of this agent.
     *
     * @return a {@link AgentOperator.AgentUpdater}
     */
    AgentOperator.AgentUpdater update();

    Boolean delete();

    void refresh();
}
