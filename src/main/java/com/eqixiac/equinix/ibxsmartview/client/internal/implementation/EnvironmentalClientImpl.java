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

package com.eqixiac.equinix.ibxsmartview.client.internal.implementation;

import com.eqixiac.equinix.core.client.ResourceClientBase;
import com.eqixiac.equinix.core.enums.RequestType;
import com.eqixiac.equinix.core.http.ResponseHandler;
import com.eqixiac.equinix.core.http.request.EquinixRequest;
import com.eqixiac.equinix.ibxsmartview.client.implementation.IBXSmartViewConfigImpl;
import com.eqixiac.equinix.ibxsmartview.client.internal.EnvironmentalClient;
import com.eqixiac.equinix.core.http.response.Page;
import com.eqixiac.equinix.ibxsmartview.model.SensorReading;
import com.eqixiac.equinix.ibxsmartview.model.json.SensorReadingJson;
import com.eqixiac.equinix.ibxsmartview.model.wrappers.SensorReadingWrapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EnvironmentalClientImpl extends ResourceClientBase<SensorReading, SensorReadingJson> implements EnvironmentalClient<SensorReading> {

    public EnvironmentalClientImpl(IBXSmartViewConfigImpl configClient) {
        super(configClient, "IBXSmartView", "Environmental", SensorReadingJson.class);
    }

    @Override
    protected SensorReading wrap(SensorReadingJson json) {
        return new SensorReadingWrapper(json, this);
    }

    public Page<SensorReadingJson> list(String ibx, String type, String zone, Integer offset, Integer limit) {
        Map<String, List<String>> qParams = new HashMap<>();
        if (type != null) {
            qParams.put("type", List.of(type));
        }
        if (zone != null) {
            qParams.put("zone", List.of(zone));
        }
        if (offset != null) {
            qParams.put("offset", List.of(String.valueOf(offset)));
        }
        if (limit != null) {
            qParams.put("limit", List.of(String.valueOf(limit)));
        }
        EquinixRequest<SensorReading> request = buildRequest(
                "ListSensorReadings", RequestType.PAGINATED, Map.of("ibx", ibx), qParams, SensorReadingJson.class);
        return ResponseHandler.handlePaginatedListResponse(invoke(request), request);
    }

    public SensorReadingJson getSensorReading(String ibx, String sensorId) {
        return getOne("GetSensorReading", Map.of("ibx", ibx, "sensorId", sensorId));
    }
}
