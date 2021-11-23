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
import api.equinix.javasdk.fabric.model.Service;
import api.equinix.javasdk.fabric.model.json.ServiceJson;

/**
 * <p>ServicesClient interface.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public interface ServiceClient<T>  extends Pageable<T> {

    /**
     * <p>listServices.</p>
     *
     * @return a {@link api.equinix.javasdk.core.http.response.Page} object.
     */
    Page<Service, ServiceJson> list();

    /**
     * <p>getServiceByUuid.</p>
     *
     * @param uuid a {@link java.lang.String} object.
     * @return a {@link api.equinix.javasdk.fabric.model.json.ServiceJson} object.
     */
    ServiceJson getByUuid(String uuid);

    /**
     * <p>refreshService.</p>
     *
     * @param uuid a {@link java.lang.String} object.
     * @return a {@link api.equinix.javasdk.fabric.model.json.ServiceJson} object.
     */
    ServiceJson refresh(String uuid);
}
