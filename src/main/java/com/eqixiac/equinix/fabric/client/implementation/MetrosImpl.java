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

package com.eqixiac.equinix.fabric.client.implementation;

import com.eqixiac.equinix.core.http.ResponseHandler;
import com.eqixiac.equinix.core.http.response.Page;
import com.eqixiac.equinix.core.http.response.PaginatedList;
import com.eqixiac.equinix.core.enums.MetroCode;
import com.eqixiac.equinix.core.model.MetroId;
import com.eqixiac.equinix.fabric.client.Metros;
import com.eqixiac.equinix.fabric.client.internal.MetroClient;
import com.eqixiac.equinix.fabric.enums.MetroPresence;
import com.eqixiac.equinix.fabric.model.Metro;
import com.eqixiac.equinix.fabric.model.json.MetroJson;
import com.eqixiac.equinix.fabric.model.wrappers.MetroWrapper;
import lombok.RequiredArgsConstructor;

/**
 *
 * @author ianjones
 */
@RequiredArgsConstructor
public class MetrosImpl implements Metros {

    private final MetroClient<Metro> serviceClient;

    public PaginatedList<Metro> list() {
        return this.list(null);
    }

    public PaginatedList<Metro> list(MetroPresence metroPresence) {
        Page<MetroJson> responsePage = this.serviceClient.list(metroPresence);
        PaginatedList<Metro> metroList = ResponseHandler.mapPaginatedList(responsePage.getItems(), this.serviceClient, MetroWrapper::new);
        return new PaginatedList<>(metroList, this.serviceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    public Metro getByMetroCode(MetroCode metroCode) {
        return getByMetroCode(metroCode.toString());
    }

    public Metro getByMetroCode(String metroCode) {
        MetroJson metroJson = this.serviceClient.getByMetroCode(metroCode);
        return new MetroWrapper(metroJson, this.serviceClient);
    }

    public Metro getByMetroId(MetroId metroId) {
        return getByMetroCode(metroId.code());
    }
}
