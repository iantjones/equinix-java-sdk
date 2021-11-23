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

package api.equinix.javasdk.fabric.client.implementation;

import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.Fabric;
import api.equinix.javasdk.fabric.client.Services;
import api.equinix.javasdk.fabric.client.internal.ServiceClient;
import api.equinix.javasdk.fabric.model.Service;
import api.equinix.javasdk.fabric.model.json.ServiceJson;
import api.equinix.javasdk.fabric.model.wrappers.ServiceWrapper;

/**
 * <p>ServicesImpl class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class ServicesImpl implements Services {

    private final Fabric serviceManager;

    private final ServiceClient<Service> serviceClient;

    /**
     * <p>Constructor for ServicesImpl.</p>
     *
     * @param serviceClient a {@link ServiceClient} object.
     * @param serviceManager a {@link api.equinix.javasdk.Fabric} object.
     */
    public ServicesImpl(ServiceClient<Service> serviceClient, Fabric serviceManager) {
        this.serviceManager = serviceManager;
        this.serviceClient = serviceClient;
    }

    /**
     * <p>listServices.</p>
     *
     * @return a {@link api.equinix.javasdk.core.http.response.PaginatedList} object.
     */
    public PaginatedList<Service> listServices() {
        Page<Service, ServiceJson> responsePage = this.serviceClient.list();
        PaginatedList<Service> serviceList = Utils.mapPaginatedList(responsePage.getItems(), this.serviceClient, ServiceWrapper::new);
        return new PaginatedList<Service>(serviceList, this.serviceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    /** {@inheritDoc} */
    public Service getServiceByUuid(String uuid) {
        ServiceJson serviceJson = this.serviceClient.getByUuid(uuid);
        return new ServiceWrapper(serviceJson, this.serviceClient);
    }
}
