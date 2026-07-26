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

import com.eqixiac.equinix.core.http.response.Page;
import com.eqixiac.equinix.core.http.response.Pageable;
import com.eqixiac.equinix.fabric.enums.BGPActionType;
import com.eqixiac.equinix.fabric.model.BGPAction;
import com.eqixiac.equinix.fabric.model.RoutingProtocol;
import com.eqixiac.equinix.fabric.model.implementation.Change;
import com.eqixiac.equinix.fabric.model.json.BGPActionJson;
import com.eqixiac.equinix.fabric.model.json.RoutingProtocolJson;
import com.eqixiac.equinix.fabric.model.json.creators.RoutingProtocolCreatorJson;

public interface RoutingProtocolClient<T> extends Pageable<T> {

    Page<RoutingProtocolJson> list(String connectionId);

    RoutingProtocolJson getByUuid(String connectionId, String uuid);

    RoutingProtocolJson create(String connectionId, RoutingProtocolCreatorJson routingProtocolCreatorJson);

    RoutingProtocolJson replace(String connectionId, String uuid, RoutingProtocolCreatorJson routingProtocolCreatorJson);

    java.util.List<RoutingProtocol> createBulk(String connectionId, java.util.List<RoutingProtocolCreatorJson> routingProtocolCreatorJsonList);

    RoutingProtocolJson update(String connectionId, String uuid, java.util.List<com.eqixiac.equinix.core.http.request.PatchOperation> operations);

    RoutingProtocolJson delete(String connectionId, String uuid);

    RoutingProtocolJson refresh(String connectionId, String uuid);

    java.util.List<BGPAction> getBgpActions(String connectionId, String routingProtocolId);

    BGPActionJson createBgpAction(String connectionId, String routingProtocolId, BGPActionType type);

    BGPAction getBgpAction(String connectionId, String routingProtocolId, String actionId);

    java.util.List<Change> getChanges(String connectionId, String routingProtocolId);

    Change getChange(String connectionId, String routingProtocolId, String changeId);
}
