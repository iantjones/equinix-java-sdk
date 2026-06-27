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

package api.equinix.javasdk.ibxsmartview.client.implementation;

import api.equinix.javasdk.IBXSmartView;
import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.ibxsmartview.client.SystemAlerts;
import api.equinix.javasdk.ibxsmartview.client.internal.SystemAlertClient;
import api.equinix.javasdk.ibxsmartview.model.SystemAlert;
import api.equinix.javasdk.ibxsmartview.model.json.SystemAlertJson;
import lombok.Getter;

@Getter
public class SystemAlertsImpl implements SystemAlerts {

    private final IBXSmartView serviceManager;

    private final SystemAlertClient<SystemAlert> serviceClient;

    public SystemAlertsImpl(SystemAlertClient<SystemAlert> serviceClient, IBXSmartView serviceManager) {
        this.serviceManager = serviceManager;
        this.serviceClient = serviceClient;
    }

    public PaginatedList<SystemAlert> search(String status, String assetClassification, String edgeCollectedOn, int offset, int limit) {
        Page<SystemAlert, SystemAlertJson> responsePage = serviceClient.search(status, assetClassification, edgeCollectedOn, offset, limit);
        PaginatedList<SystemAlert> alertList = Utils.mapPaginatedList(responsePage.getItems(), this.serviceClient, (json, client) -> json);
        return new PaginatedList<>(alertList, this.serviceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    public PaginatedList<SystemAlert> searchPost(Object filterBody) {
        Page<SystemAlert, SystemAlertJson> responsePage = serviceClient.searchPost(filterBody);
        PaginatedList<SystemAlert> alertList = Utils.mapPaginatedList(responsePage.getItems(), this.serviceClient, (json, client) -> json);
        return new PaginatedList<>(alertList, this.serviceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }
}
