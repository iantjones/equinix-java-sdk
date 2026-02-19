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

package api.equinix.javasdk.ibxsmartview.client;

import api.equinix.javasdk.ibxsmartview.model.EnvironmentData;
import api.equinix.javasdk.ibxsmartview.model.TrendingEnvironmentData;

import java.util.List;

/**
 * Client interface for accessing legacy IBX SmartView environmental data. Provides methods
 * to retrieve current and trending environmental measurements such as temperature and
 * humidity at various hierarchy levels within an IBX.
 */
public interface LegacyEnvironmentals {

    /**
     * Retrieves the current environmental data for a specific hierarchy level and value.
     *
     * @param accountNo the Equinix account number
     * @param ibx the IBX code identifying the data center
     * @param levelType the hierarchy level type (e.g., cage, cabinet)
     * @param levelValue the specific value for the hierarchy level
     * @return the current environmental data
     */
    EnvironmentData getCurrent(String accountNo, String ibx, String levelType, String levelValue);

    /**
     * Lists current environmental data for all entries at the specified hierarchy level.
     *
     * @param accountNo the Equinix account number
     * @param ibx the IBX code identifying the data center
     * @param levelType the hierarchy level type (e.g., cage, cabinet)
     * @return a list of current environmental data entries
     */
    List<EnvironmentData> listCurrent(String accountNo, String ibx, String levelType);

    /**
     * Retrieves trending environmental data over a specified time range and interval.
     *
     * @param accountNo the Equinix account number
     * @param ibx the IBX code identifying the data center
     * @param dataPoint the environmental data point to trend (e.g., temperature, humidity)
     * @param levelType the hierarchy level type (e.g., cage, cabinet)
     * @param levelValue the specific value for the hierarchy level
     * @param interval the aggregation interval for trend data
     * @param fromDate the start date of the trend range
     * @param toDate the end date of the trend range
     * @return the trending environmental data
     */
    TrendingEnvironmentData getTrending(String accountNo, String ibx, String dataPoint,
                                        String levelType, String levelValue, String interval,
                                        String fromDate, String toDate);
}
