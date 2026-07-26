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

import com.eqixiac.equinix.fabric.model.implementation.AgentDefinition;
import com.eqixiac.equinix.fabric.model.implementation.ChangeLog;

/**
 * An Equinix-provided template that defines a class of Fabric agents. Read-only.
 */
public interface AgentTemplate {

    String getHref();

    String getType();

    String getUuid();

    String getName();

    String getDescription();

    AgentState getState();

    Boolean getEnabled();

    /**
     * The template's definition document reference (ReadMe URL).
     *
     * @return the agent definition
     */
    AgentDefinition getAgentDefinition();

    ChangeLog getChangeLog();
}
