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

package api.equinix.javasdk.fabric.client.internal.implementation;

import api.equinix.javasdk.core.client.PageableBase;
import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.request.EquinixRequest;
import api.equinix.javasdk.core.http.request.PaginatedRequest;
import api.equinix.javasdk.core.http.response.EquinixResponse;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.core.enums.RequestType;
import api.equinix.javasdk.fabric.client.implementation.FabricConfigImpl;
import api.equinix.javasdk.fabric.client.internal.MetroClient;
import api.equinix.javasdk.fabric.enums.MetroPresence;
import api.equinix.javasdk.fabric.model.Metro;
import api.equinix.javasdk.fabric.model.json.MetroJson;
import api.equinix.javasdk.fabric.model.wrappers.MetroWrapper;

import java.util.List;
import java.util.Map;

/**
 * <p>MetrosClientImpl class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class MetroClientImpl extends PageableBase implements MetroClient<Metro> {

    /**
     * <p>Constructor for MetrosClientImpl.</p>
     *
     * @param configClient a {@link api.equinix.javasdk.fabric.client.implementation.FabricConfigImpl} object.
     */
    public MetroClientImpl(FabricConfigImpl configClient) {
        super(configClient, "Fabric", "Metros");
    }

    /**
     * <p>list.</p>
     *
     * @return a {@link api.equinix.javasdk.core.http.response.Page} object.
     */
    public Page<Metro, MetroJson> list() {
        return list(null);
    }

    /** {@inheritDoc} */
    public Page<Metro, MetroJson> list(MetroPresence metroPresence) {
        Map<String, List<String>> qParams = null;
        if(metroPresence != null) {
            qParams = Map.of("presence", Utils.singleParamList(metroPresence));
        }

        EquinixRequest<Metro> equinixRequest = this.buildRequest("GetMetros", RequestType.PAGINATED, null, qParams, MetroJson.pagedTypeRef());
        EquinixResponse<Metro> equinixResponse = this.invoke(equinixRequest);
        return Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
    }

    /** {@inheritDoc} */
    public MetroJson getByMetroCode(MetroCode metroCode) {
        Map<String, String> pParams = Map.of("metroCode", metroCode.toString());
        EquinixRequest<Metro> equinixRequest = this.buildRequestWithPathParams("GetMetro", RequestType.SINGLE, pParams, MetroJson.singleTypeRef());
        EquinixResponse<Metro> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    /** {@inheritDoc} */
    public MetroJson refresh(MetroCode metroCode) {
        return this.getByMetroCode(metroCode);
    }

    /** {@inheritDoc} */
    public PaginatedList<Metro> nextPage(PaginatedRequest<Metro> equinixRequest) {
        EquinixResponse<Metro> equinixResponse = this.invoke(equinixRequest);
        Page<Metro, MetroJson> nextPage = Utils.handlePaginatedListResponse(equinixResponse, equinixRequest);
        PaginatedList<Metro> newPaginatedList = Utils.mapPaginatedList(nextPage.getItems(), this, MetroWrapper::new);
        return new PaginatedList<>(newPaginatedList, this, equinixRequest, equinixResponse, nextPage.getPagination());
    }
}
