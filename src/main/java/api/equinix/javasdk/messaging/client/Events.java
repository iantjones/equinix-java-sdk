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

package api.equinix.javasdk.messaging.client;

import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.messaging.model.Event;

/**
 * Client interface for accessing Equinix Messaging events. Provides methods to list
 * and retrieve events that represent notifications triggered by resource changes
 * across Equinix services.
 */
public interface Events {

    /**
     * Lists all messaging events for the current account.
     *
     * @return a paginated list of events
     */
    PaginatedList<Event> list();

    /**
     * Retrieves a specific event by its unique identifier.
     *
     * @param uuid the unique identifier of the event
     * @return the event
     */
    Event getByUuid(String uuid);
}
