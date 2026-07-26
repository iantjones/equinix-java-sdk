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
import com.eqixiac.equinix.core.http.response.Pageable;
import com.eqixiac.equinix.core.http.response.PageablePost;
import com.eqixiac.equinix.fabric.model.ServiceProfile;
import com.eqixiac.equinix.fabric.model.ServiceProfileAction;
import com.eqixiac.equinix.fabric.model.implementation.ServiceMetro;
import com.eqixiac.equinix.fabric.model.implementation.filter.FilterPropertyList;
import com.eqixiac.equinix.fabric.model.implementation.sort.SortPropertyList;
import com.eqixiac.equinix.fabric.model.json.ServiceProfileJson;
import com.eqixiac.equinix.fabric.model.json.creators.ServiceProfileCreatorJson;

import java.util.List;

/**
 *
 * @author ianjones
 */
public interface ServiceProfileClient<T> extends PageablePost<T> {

    Page<ServiceProfileJson> list();

    Page<ServiceProfileJson> search(FilterPropertyList filter, SortPropertyList sort);

    ServiceProfileJson getByUuid(String uuid);

    ServiceProfileJson create(ServiceProfileCreatorJson serviceProfileCreatorJson);

    ServiceProfileJson update(String uuid, List<PatchOperation> operations);

    ServiceProfileJson put(String uuid, ServiceProfileCreatorJson serviceProfileCreatorJson);

    ServiceProfileJson delete(String uuid);

    ServiceProfileAction createAction(String uuid, String type, String description);

    List<ServiceMetro> getMetros(String uuid);

    ServiceProfileJson refresh(String uuid);
}
