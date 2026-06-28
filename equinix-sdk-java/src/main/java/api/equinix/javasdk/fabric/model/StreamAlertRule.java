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

package api.equinix.javasdk.fabric.model;

import api.equinix.javasdk.fabric.model.implementation.ChangeLog;
import api.equinix.javasdk.fabric.model.json.creators.StreamAlertRuleOperator;

/**
 * A metric alert rule attached to a Fabric stream.
 */
public interface StreamAlertRule {

    String getHref();

    String getUuid();

    String getType();

    String getName();

    String getDescription();

    String getState();

    Boolean getEnabled();

    ChangeLog getChangeLog();

    /**
     * Begins a fluent update of this alert rule on its parent stream.
     *
     * @param streamId the uuid of the parent stream
     * @return a {@link StreamAlertRuleOperator.StreamAlertRuleUpdater}
     */
    StreamAlertRuleOperator.StreamAlertRuleUpdater update(String streamId);

    Boolean delete(String streamId);

    void refresh(String streamId);
}
