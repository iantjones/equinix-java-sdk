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
import api.equinix.javasdk.fabric.model.IpBlock;
import api.equinix.javasdk.fabric.model.implementation.filter.FilterPropertyList;
import api.equinix.javasdk.fabric.model.implementation.sort.SortPropertyList;
import api.equinix.javasdk.fabric.model.json.IpBlockJson;
import api.equinix.javasdk.fabric.model.json.creators.IpBlockCreatorJson;

import java.util.List;

public interface IpBlockClient<T> extends PageablePost<T> {

    Page<IpBlockJson> search(FilterPropertyList filter, SortPropertyList sort);

    IpBlockJson getByUuid(String uuid);

    IpBlockJson create(IpBlockCreatorJson ipBlockCreatorJson);

    IpBlockJson update(String uuid, List<PatchOperation> operations);

    IpBlockJson delete(String uuid);

    IpBlockJson refresh(String uuid);
}
