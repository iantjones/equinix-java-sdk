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

package api.equinix.javasdk.ibxsmartview.model.wrappers;

import api.equinix.javasdk.core.http.response.Pageable;
import api.equinix.javasdk.core.model.ResourceImpl;
import api.equinix.javasdk.ibxsmartview.model.SensorReading;
import api.equinix.javasdk.ibxsmartview.model.implementation.Reading;
import api.equinix.javasdk.ibxsmartview.model.json.SensorReadingJson;
import lombok.Getter;

public class SensorReadingWrapper extends ResourceImpl<SensorReading> implements SensorReading {

    private SensorReadingJson json;
    @Getter
    private final Pageable<SensorReading> serviceClient;

    public SensorReadingWrapper(SensorReadingJson sensorReadingJson, Pageable<SensorReading> serviceClient) {
        this.json = sensorReadingJson;
        this.serviceClient = serviceClient;
    }

    public String getSensorId() {
        return this.json.getSensorId();
    }

    public String getZoneId() {
        return this.json.getZoneId();
    }

    public String getIbx() {
        return this.json.getIbx();
    }

    public Reading getHumidity() {
        return this.json.getHumidity();
    }

    public Reading getTemperature() {
        return this.json.getTemperature();
    }
}
