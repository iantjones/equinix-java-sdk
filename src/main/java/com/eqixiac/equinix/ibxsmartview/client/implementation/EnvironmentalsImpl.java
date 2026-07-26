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
import com.eqixiac.equinix.ibxsmartview.client.Environmentals;
import com.eqixiac.equinix.ibxsmartview.client.internal.EnvironmentalClient;
import com.eqixiac.equinix.ibxsmartview.model.SensorReading;
import com.eqixiac.equinix.ibxsmartview.model.json.SensorReadingJson;
import com.eqixiac.equinix.ibxsmartview.model.wrappers.SensorReadingWrapper;
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
