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

package com.eqixiac.equinix.ibxsmartview.client.implementation;

import com.eqixiac.equinix.IBXSmartView;
import com.eqixiac.equinix.core.http.ResponseHandler;
import com.eqixiac.equinix.core.http.response.Page;
import com.eqixiac.equinix.core.http.response.PaginatedList;
import com.eqixiac.equinix.ibxsmartview.client.SystemAlerts;
import com.eqixiac.equinix.ibxsmartview.client.internal.SystemAlertClient;
import com.eqixiac.equinix.ibxsmartview.model.SystemAlert;
import com.eqixiac.equinix.ibxsmartview.model.json.SystemAlertJson;
import com.eqixiac.equinix.ibxsmartview.model.json.creators.SearchRequest;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SystemAlertsImpl implements SystemAlerts {

    private final SystemAlertClient<SystemAlert> serviceClient;

    private final IBXSmartView serviceManager;

    public PaginatedList<SystemAlert> search(String status, String assetClassification, String edgeCollectedOn, int offset, int limit) {
        Page<SystemAlertJson> responsePage = serviceClient.search(status, assetClassification, edgeCollectedOn, offset, limit);
        PaginatedList<SystemAlert> alertList = ResponseHandler.mapPaginatedList(responsePage.getItems(), this.serviceClient, (json, client) -> json);
        return new PaginatedList<>(alertList, this.serviceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    public PaginatedList<SystemAlert> searchPost(SearchRequest filterBody) {
        Page<SystemAlertJson> responsePage = serviceClient.searchPost(filterBody);
        PaginatedList<SystemAlert> alertList = ResponseHandler.mapPaginatedList(responsePage.getItems(), this.serviceClient, (json, client) -> json);
        return new PaginatedList<>(alertList, this.serviceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }
}
