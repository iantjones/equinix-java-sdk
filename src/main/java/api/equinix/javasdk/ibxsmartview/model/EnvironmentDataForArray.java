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

package api.equinix.javasdk.ibxsmartview.model;

/**
 * A single environmental data entry within the {@code payLoad.data} array returned by the
 * legacy {@code /environment/v1/listCurrent} endpoint.
 */
public interface EnvironmentDataForArray {

    String getIbx();

    String getAccountNo();

    String getZone();

    String getCage();

    String getCabinet();

    String getSensor();

    String getTemperature();

    String getHumidity();

    String getTimestamp();

    String getTemperatureUom();

    String getHumidityUom();
}
