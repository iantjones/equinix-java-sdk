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
import com.eqixiac.equinix.fabric.enums.ServiceTokenAction;
import com.eqixiac.equinix.fabric.model.ServiceToken;
import com.eqixiac.equinix.fabric.model.implementation.filter.FilterPropertyList;
import com.eqixiac.equinix.fabric.model.implementation.sort.SortPropertyList;
import com.eqixiac.equinix.fabric.model.json.ServiceTokenJson;
import com.eqixiac.equinix.fabric.model.json.creators.ServiceTokenCreatorJson;

import java.util.List;

/**
 *
 * @author ianjones
 */
public interface ServiceTokenClient<T> extends PageablePost<T> {

    Page<ServiceTokenJson> list();

    Page<ServiceTokenJson> search(FilterPropertyList filter, SortPropertyList sort);

    ServiceTokenJson update(String uuid, List<PatchOperation> operations);

    /**
     * Dry-run variant of {@link #update(String, List)}: sends the same change-operations array to
     * {@code PATCH /fabric/v4/serviceTokens/{uuid}} with {@code dryRun=true} — per the Fabric v4
     * spec, an "option to verify that API calls will succeed". Nothing is persisted; the API
     * responds {@code 200} with the validated/simulated token entity.
     */
    ServiceTokenJson dryRunUpdate(String uuid, List<PatchOperation> operations);

    ServiceTokenJson createAction(String uuid, ServiceTokenAction type);

    ServiceTokenJson getByUuid(String uuid);

    ServiceTokenJson create(ServiceTokenCreatorJson serviceTokenCreatorJson);

    ServiceTokenJson dryRunCreate(ServiceTokenCreatorJson serviceTokenCreatorJson);

    ServiceTokenJson delete(String uuid);

    ServiceTokenJson refresh(String uuid);
}
