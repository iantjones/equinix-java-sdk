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
import api.equinix.javasdk.ibxsmartview.client.Power;
import api.equinix.javasdk.ibxsmartview.client.internal.PowerClient;
import api.equinix.javasdk.ibxsmartview.model.PowerReading;
import api.equinix.javasdk.ibxsmartview.model.json.PowerReadingJson;
import lombok.Getter;

@Getter
public class PowerImpl implements Power {

    private final IBXSmartView serviceManager;

    private final PowerClient<PowerReading> serviceClient;

    public PowerImpl(PowerClient<PowerReading> serviceClient, IBXSmartView serviceManager) {
        this.serviceManager = serviceManager;
        this.serviceClient = serviceClient;
    }

    public PaginatedList<PowerReading> list(String ibx) {
        Page<PowerReading, PowerReadingJson> responsePage = serviceClient.list(ibx);
        PaginatedList<PowerReading> deviceList = Utils.mapPaginatedList(responsePage.getItems(), this.serviceClient, (json, client) -> json);
        return new PaginatedList<>(deviceList, this.serviceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    public PowerReading getPowerReading(String ibx, String cabinetId) {
        PowerReadingJson powerReadingJson = serviceClient.getPowerReading(ibx, cabinetId);
        return powerReadingJson;
    }
}
