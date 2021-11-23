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
import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.Fabric;
import api.equinix.javasdk.fabric.client.Metros;
import api.equinix.javasdk.fabric.client.internal.MetroClient;
import api.equinix.javasdk.fabric.enums.MetroPresence;
import api.equinix.javasdk.fabric.model.Metro;
import api.equinix.javasdk.fabric.model.json.MetroJson;
import api.equinix.javasdk.fabric.model.wrappers.MetroWrapper;

/**
 * <p>MetrosImpl class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class MetrosImpl implements Metros {

    private final Fabric serviceManager;

    private final MetroClient<Metro> serviceClient;

    /**
     * <p>Constructor for MetrosImpl.</p>
     *
     * @param serviceClient a {@link MetroClient} object.
     * @param serviceManager a {@link api.equinix.javasdk.Fabric} object.
     */
    public MetrosImpl(MetroClient<Metro> serviceClient, Fabric serviceManager) {
        this.serviceManager = serviceManager;
        this.serviceClient = serviceClient;
    }

    /**
     * <p>list.</p>
     *
     * @return a {@link api.equinix.javasdk.core.http.response.PaginatedList} object.
     */
    public PaginatedList<Metro> list() {
        return this.list(null);
    }

    /** {@inheritDoc} */
    public PaginatedList<Metro> list(MetroPresence metroPresence) {
        Page<Metro, MetroJson> responsePage = this.serviceClient.list();
        PaginatedList<Metro> metroList = Utils.mapPaginatedList(responsePage.getItems(), this.serviceClient, MetroWrapper::new);
        return new PaginatedList<>(metroList, this.serviceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    /** {@inheritDoc} */
    public Metro getByMetroCode(MetroCode metroCode) {
        MetroJson metroJson = this.serviceClient.getByMetroCode(metroCode);
        return new MetroWrapper(metroJson, this.serviceClient);
    }
}
