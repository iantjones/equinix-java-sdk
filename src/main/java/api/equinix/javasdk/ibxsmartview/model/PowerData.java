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

import api.equinix.javasdk.ibxsmartview.model.implementation.ComparisonData;
import api.equinix.javasdk.ibxsmartview.model.implementation.Status;

/**
 * Current power consumption data for a single hierarchy node, wrapped in the
 * {@code payLoad}/{@code status} envelope returned by the legacy {@code /power/v1/current}
 * GET endpoint.
 */
public interface PowerData {

    /**
     * @return the power data payload, or {@code null} when the response carried no data
     */
    Payload getPayLoad();

    /**
     * @return the response status envelope
     */
    Status getStatus();

    /**
     * The {@code payLoad} of a {@link PowerData} response.
     */
    interface Payload {

        String getIbx();

        String getAccountNo();

        String getLevelType();

        String getLevelValue();

        String getIsAlarm();

        Double getKva();

        Double getAmps();

        Double getSoldKva();

        Double getCabinetRating();

        Double getContractualKva();

        Double getPercentageKva();

        ComparisonData getComparisonData();

        Double getPeakKvaLastSevenDays();

        Double getPeakKvaLastSevenDaysPercentage();

        Double getPeakKvaLastSevenDaysContractualKva();

        Long getPeakKvaLastSevenDaysTime();

        Integer getSoldAmps();

        Double getPrimaryKva();

        Double getRedundantKva();

        String getKw();

        String getPowerFactor();

        String getReadingTime();

        String getLastUpdatedTime();

        String getCustomerName();
    }
}
