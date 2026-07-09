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
import api.equinix.javasdk.core.http.ResponseHandler;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.ibxsmartview.client.Environmentals;
import api.equinix.javasdk.ibxsmartview.client.internal.EnvironmentalClient;
import api.equinix.javasdk.ibxsmartview.model.SensorReading;
import api.equinix.javasdk.ibxsmartview.model.json.SensorReadingJson;
import api.equinix.javasdk.ibxsmartview.model.wrappers.SensorReadingWrapper;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class EnvironmentalsImpl implements Environmentals {

    private final EnvironmentalClient<SensorReading> serviceClient;

    private final IBXSmartView serviceManager;

    public PaginatedList<SensorReading> list(String ibx) {
        return list(ibx, null, null, null, null);
    }

    public PaginatedList<SensorReading> list(String ibx, String type, String zone, Integer offset, Integer limit) {
        Page<SensorReadingJson> responsePage = serviceClient.list(ibx, type, zone, offset, limit);
        PaginatedList<SensorReading> deviceList = ResponseHandler.mapPaginatedList(responsePage.getItems(), this.serviceClient, SensorReadingWrapper::new);
        return new PaginatedList<>(deviceList, this.serviceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    public SensorReading getSensorReading(String ibx, String sensorId) {
        SensorReadingJson sensorReadingJson = serviceClient.getSensorReading(ibx, sensorId);
        return new SensorReadingWrapper(sensorReadingJson, this.serviceClient);
    }
}
