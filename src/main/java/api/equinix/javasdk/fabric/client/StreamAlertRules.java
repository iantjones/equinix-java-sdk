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

package api.equinix.javasdk.fabric.client;

import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.fabric.model.StreamAlertRule;
import api.equinix.javasdk.fabric.model.json.creators.StreamAlertRuleOperator;

/**
 * Client interface for managing alert rules attached to Equinix Fabric streams.
 */
public interface StreamAlertRules {

    /**
     * Lists the alert rules configured on a stream.
     *
     * @param streamId the unique identifier of the stream
     * @return a paginated list of alert rules
     */
    PaginatedList<StreamAlertRule> list(String streamId);

    /**
     * Retrieves a single alert rule on a stream by its unique identifier.
     *
     * @param streamId the unique identifier of the stream
     * @param uuid the unique identifier of the alert rule
     * @return the alert rule matching the given UUID
     */
    StreamAlertRule getByUuid(String streamId, String uuid);

    /**
     * Begins the fluent builder for creating a new alert rule on a stream.
     *
     * @param streamId the unique identifier of the stream
     * @return a builder for configuring the new alert rule
     */
    StreamAlertRuleOperator.StreamAlertRuleBuilder define(String streamId);
}
