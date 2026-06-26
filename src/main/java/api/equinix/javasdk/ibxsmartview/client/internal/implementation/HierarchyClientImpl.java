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
import api.equinix.javasdk.ibxsmartview.client.implementation.IBXSmartViewConfigImpl;
import api.equinix.javasdk.ibxsmartview.client.internal.HierarchyClient;
import api.equinix.javasdk.ibxsmartview.model.LocationHierarchy;
import api.equinix.javasdk.ibxsmartview.model.json.LocationHierarchyJson;
import api.equinix.javasdk.ibxsmartview.model.json.PowerHierarchyJson;

import java.util.List;
import java.util.Map;

public class HierarchyClientImpl extends ResourceClientBase<LocationHierarchy, LocationHierarchyJson> implements HierarchyClient<LocationHierarchy> {

    public HierarchyClientImpl(IBXSmartViewConfigImpl configClient) {
        super(configClient, "IBXSmartView", "Hierarchy", LocationHierarchyJson.class);
    }

    @Override
    protected LocationHierarchy wrap(LocationHierarchyJson json) {
        return json;
    }

    public LocationHierarchyJson getLocationHierarchy(String accountNo, String ibx) {
        Map<String, List<String>> qParams = Map.of(
                "accountNo", List.of(accountNo),
                "ibx", List.of(ibx)
        );
        return getAs("GetLocationHierarchy", Map.of(), qParams, LocationHierarchyJson.class);
    }

    public PowerHierarchyJson getPowerHierarchy(String accountNo, String ibx) {
        Map<String, List<String>> qParams = Map.of(
                "accountNo", List.of(accountNo),
                "ibx", List.of(ibx)
        );
        return getAs("GetPowerHierarchy", Map.of(), qParams, PowerHierarchyJson.class);
    }
}
