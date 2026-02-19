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
import api.equinix.javasdk.ibxsmartview.client.Hierarchy;
import api.equinix.javasdk.ibxsmartview.client.internal.HierarchyClient;
import api.equinix.javasdk.ibxsmartview.model.LocationHierarchy;
import api.equinix.javasdk.ibxsmartview.model.PowerHierarchy;
import api.equinix.javasdk.ibxsmartview.model.json.LocationHierarchyJson;
import api.equinix.javasdk.ibxsmartview.model.json.PowerHierarchyJson;
import lombok.Getter;

@Getter
public class HierarchyImpl implements Hierarchy {

    private final IBXSmartView serviceManager;

    private final HierarchyClient<LocationHierarchy> serviceClient;

    public HierarchyImpl(HierarchyClient<LocationHierarchy> serviceClient, IBXSmartView serviceManager) {
        this.serviceManager = serviceManager;
        this.serviceClient = serviceClient;
    }

    public LocationHierarchy getLocationHierarchy(String accountNo, String ibx) {
        LocationHierarchyJson locationHierarchyJson = serviceClient.getLocationHierarchy(accountNo, ibx);
        return locationHierarchyJson;
    }

    public PowerHierarchy getPowerHierarchy(String accountNo, String ibx) {
        PowerHierarchyJson powerHierarchyJson = serviceClient.getPowerHierarchy(accountNo, ibx);
        return powerHierarchyJson;
    }
}
