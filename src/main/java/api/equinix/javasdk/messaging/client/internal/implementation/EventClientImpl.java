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

package api.equinix.javasdk.messaging.client.internal.implementation;

import api.equinix.javasdk.core.client.ResourceClientBase;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.messaging.client.implementation.MessagingConfigImpl;
import api.equinix.javasdk.messaging.client.internal.EventClient;
import api.equinix.javasdk.messaging.model.Event;
import api.equinix.javasdk.messaging.model.json.EventJson;

public class EventClientImpl extends ResourceClientBase<Event, EventJson> implements EventClient<Event> {

    public EventClientImpl(MessagingConfigImpl configClient) {
        super(configClient, "Messaging", "Events", EventJson.class);
    }

    @Override
    protected Event wrap(EventJson json) {
        return json;
    }

    public Page<Event, EventJson> list() {
        return listPage("ListEvents");
    }

    public EventJson getByUuid(String uuid) {
        return getOne("GetEvent", uuid);
    }
}
