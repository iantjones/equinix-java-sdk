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
import com.eqixiac.equinix.fabric.model.Connection;
import com.eqixiac.equinix.fabric.model.Network;
import com.eqixiac.equinix.fabric.model.implementation.Change;
import com.eqixiac.equinix.fabric.model.implementation.filter.FilterPropertyList;
import com.eqixiac.equinix.fabric.model.implementation.sort.SortPropertyList;
import com.eqixiac.equinix.fabric.model.json.ConnectionJson;
import com.eqixiac.equinix.fabric.model.json.NetworkJson;
import com.eqixiac.equinix.fabric.model.json.creators.NetworkCreatorJson;

import java.util.List;

public interface NetworkClient<T> extends PageablePost<T> {

    Page<NetworkJson> search(FilterPropertyList filter, SortPropertyList sort);

    Page<ConnectionJson> getConnections(String networkId);

    List<Change> getChanges(String uuid);

    Change getChange(String uuid, String changeId);

    NetworkJson getByUuid(String uuid);

    NetworkJson create(NetworkCreatorJson networkCreatorJson);

    /**
     * Dry-run variant of {@link #create(NetworkCreatorJson)}: POSTs the same body to
     * {@code /fabric/v4/networks} with {@code dryRun=true} — per the Fabric v4 spec, an
     * "option to verify that API calls will succeed". Nothing is provisioned; the API responds
     * with the validated request echoed back (no {@code uuid}/{@code href}/{@code state}).
     */
    NetworkJson dryRunCreate(NetworkCreatorJson networkCreatorJson);

    NetworkJson update(String uuid, List<PatchOperation> operations);

    NetworkJson delete(String uuid);

    NetworkJson refresh(String uuid);
}
