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
import api.equinix.javasdk.fabric.enums.ServiceTokenAction;
import api.equinix.javasdk.fabric.model.ServiceToken;
import api.equinix.javasdk.fabric.model.implementation.filter.FilterPropertyList;
import api.equinix.javasdk.fabric.model.implementation.sort.SortPropertyList;
import api.equinix.javasdk.fabric.model.json.ServiceTokenJson;
import api.equinix.javasdk.fabric.model.json.creators.ServiceTokenCreatorJson;

import java.util.List;

/**
 * <p>ServiceTokensClient interface.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public interface ServiceTokenClient<T> extends PageablePost<T> {

    /**
     * <p>list.</p>
     *
     * @return a {@link api.equinix.javasdk.core.http.response.Page} object.
     */
    Page<ServiceToken, ServiceTokenJson> list();

    Page<ServiceToken, ServiceTokenJson> search(FilterPropertyList filter, SortPropertyList sort);

    ServiceTokenJson update(String uuid, List<PatchOperation> operations);

    ServiceTokenJson createAction(String uuid, ServiceTokenAction type);

    /**
     * <p>getByUuid.</p>
     *
     * @param uuid a {@link java.lang.String} object.
     * @return a {@link api.equinix.javasdk.fabric.model.json.ServiceTokenJson} object.
     */
    ServiceTokenJson getByUuid(String uuid);

    /**
     * <p>create.</p>
     *
     * @param serviceTokenCreatorJson a {@link api.equinix.javasdk.fabric.model.json.ServiceTokenJson} object.
     * @return a {@link api.equinix.javasdk.fabric.model.json.ServiceTokenJson} object.
     */
    ServiceTokenJson create(ServiceTokenCreatorJson serviceTokenCreatorJson);

    /**
     * <p>dryRunCreate.</p>
     *
     * @param serviceTokenCreatorJson a {@link api.equinix.javasdk.fabric.model.json.creators.ServiceTokenCreatorJson} object.
     * @return a {@link api.equinix.javasdk.fabric.model.json.ServiceTokenJson} object.
     */
    ServiceTokenJson dryRunCreate(ServiceTokenCreatorJson serviceTokenCreatorJson);

    /**
     * <p>delete.</p>
     *
     * @param uuid a {@link java.lang.String} object.
     * @return a {@link api.equinix.javasdk.fabric.model.json.ServiceTokenJson} object.
     */
    ServiceTokenJson delete(String uuid);

    /**
     * <p>refresh.</p>
     *
     * @param uuid a {@link java.lang.String} object.
     * @return a {@link api.equinix.javasdk.fabric.model.json.ServiceTokenJson} object.
     */
    ServiceTokenJson refresh(String uuid);
}
