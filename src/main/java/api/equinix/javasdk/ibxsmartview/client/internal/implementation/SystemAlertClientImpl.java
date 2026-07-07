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

package api.equinix.javasdk.ibxsmartview.client.internal.implementation;

import api.equinix.javasdk.core.client.ResourceClientBase;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.ibxsmartview.client.implementation.IBXSmartViewConfigImpl;
import api.equinix.javasdk.ibxsmartview.client.internal.SystemAlertClient;
import api.equinix.javasdk.ibxsmartview.model.SystemAlert;
import api.equinix.javasdk.ibxsmartview.model.json.SystemAlertJson;
import api.equinix.javasdk.ibxsmartview.model.json.creators.SearchRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SystemAlertClientImpl extends ResourceClientBase<SystemAlert, SystemAlertJson> implements SystemAlertClient<SystemAlert> {

    public SystemAlertClientImpl(IBXSmartViewConfigImpl configClient) {
        super(configClient, "IBXSmartView", "SystemAlerts", SystemAlertJson.class);
    }

    @Override
    protected SystemAlert wrap(SystemAlertJson json) {
        return json;
    }

    public Page<SystemAlertJson> search(String status, String assetClassification, String edgeCollectedOn, int offset, int limit) {
        // status, assetClassification and edgeCollectedOn are optional (required:false in the spec);
        // only add them when non-null so an unfiltered search(null, null, null, ...) does not NPE.
        Map<String, List<String>> qParams = new HashMap<>();
        if (status != null) {
            qParams.put("status", List.of(status));
        }
        if (assetClassification != null) {
            qParams.put("assetClassification", List.of(assetClassification));
        }
        if (edgeCollectedOn != null) {
            qParams.put("edgeCollectedOn", List.of(edgeCollectedOn));
        }
        qParams.put("offset", List.of(String.valueOf(offset)));
        qParams.put("limit", List.of(String.valueOf(limit)));
        return listPage("SearchAlertsGet", qParams);
    }

    public Page<SystemAlertJson> searchPost(SearchRequest filterBody) {
        return searchPage("SearchAlertsPost", filterBody);
    }
}
