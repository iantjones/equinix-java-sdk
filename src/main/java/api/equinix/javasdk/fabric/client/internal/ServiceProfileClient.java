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

import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.Pageable;
import api.equinix.javasdk.fabric.model.ServiceProfile;
import api.equinix.javasdk.fabric.model.json.ServiceProfileJson;

/**
 * <p>ServiceProfilesClient interface.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public interface ServiceProfileClient<T>  extends Pageable<T> {

    /**
     * <p>listServiceProfiles.</p>
     *
     * @return a {@link api.equinix.javasdk.core.http.response.Page} object.
     */
    Page<ServiceProfile, ServiceProfileJson> list();

    /**
     * <p>getServiceProfileByUuid.</p>
     *
     * @param uuid a {@link java.lang.String} object.
     * @return a {@link api.equinix.javasdk.fabric.model.json.ServiceProfileJson} object.
     */
    ServiceProfileJson getByUuid(String uuid);

    /**
     * <p>refreshServiceProfile.</p>
     *
     * @param uuid a {@link java.lang.String} object.
     * @return a {@link api.equinix.javasdk.fabric.model.json.ServiceProfileJson} object.
     */
    ServiceProfileJson refresh(String uuid);
}
