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

package api.equinix.javasdk.fabric.client.internal;

import api.equinix.javasdk.core.http.request.PatchOperation;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.PageablePost;
import api.equinix.javasdk.fabric.model.Connection;
import api.equinix.javasdk.fabric.model.Network;
import api.equinix.javasdk.fabric.model.implementation.Change;
import api.equinix.javasdk.fabric.model.implementation.filter.FilterPropertyList;
import api.equinix.javasdk.fabric.model.implementation.sort.SortPropertyList;
import api.equinix.javasdk.fabric.model.json.ConnectionJson;
import api.equinix.javasdk.fabric.model.json.NetworkJson;
import api.equinix.javasdk.fabric.model.json.creators.NetworkCreatorJson;

import java.util.List;

public interface NetworkClient<T> extends PageablePost<T> {

    Page<NetworkJson> search(FilterPropertyList filter, SortPropertyList sort);

    Page<ConnectionJson> getConnections(String networkId);

    List<Change> getChanges(String uuid);

    Change getChange(String uuid, String changeId);

    NetworkJson getByUuid(String uuid);

    NetworkJson create(NetworkCreatorJson networkCreatorJson);

    NetworkJson update(String uuid, List<PatchOperation> operations);

    NetworkJson delete(String uuid);

    NetworkJson refresh(String uuid);
}
