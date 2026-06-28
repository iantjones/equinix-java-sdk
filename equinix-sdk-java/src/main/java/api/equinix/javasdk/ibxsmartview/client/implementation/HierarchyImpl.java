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
import api.equinix.javasdk.ibxsmartview.model.implementation.HierarchyNode;
import api.equinix.javasdk.ibxsmartview.model.implementation.PowerHierarchyNode;
import lombok.Getter;

import java.util.List;

@Getter
public class HierarchyImpl implements Hierarchy {

    private final IBXSmartView serviceManager;

    private final HierarchyClient serviceClient;

    public HierarchyImpl(HierarchyClient serviceClient, IBXSmartView serviceManager) {
        this.serviceManager = serviceManager;
        this.serviceClient = serviceClient;
    }

    public List<HierarchyNode> getLocationHierarchy(String accountNo, String ibx) {
        return serviceClient.getLocationHierarchy(accountNo, ibx);
    }

    public List<PowerHierarchyNode> getPowerHierarchy(String accountNo, String ibx) {
        return serviceClient.getPowerHierarchy(accountNo, ibx);
    }
}
