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
import com.eqixiac.equinix.ibxsmartview.client.LegacyEnvironmentals;
import com.eqixiac.equinix.ibxsmartview.client.internal.LegacyEnvironmentalClient;
import com.eqixiac.equinix.ibxsmartview.model.EnvironmentData;
import com.eqixiac.equinix.ibxsmartview.model.EnvironmentDataForArray;
import com.eqixiac.equinix.ibxsmartview.model.TrendingEnvironmentData;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class LegacyEnvironmentalsImpl implements LegacyEnvironmentals {

    private final LegacyEnvironmentalClient serviceClient;

    private final IBXSmartView serviceManager;

    public EnvironmentData getCurrent(String accountNo, String ibx, String levelType, String levelValue) {
        return serviceClient.getCurrent(accountNo, ibx, levelType, levelValue);
    }

    public List<EnvironmentDataForArray> listCurrent(String accountNo, String ibx, String levelType) {
        return new ArrayList<>(serviceClient.listCurrent(accountNo, ibx, levelType));
    }

    public TrendingEnvironmentData getTrending(String accountNo, String ibx, String dataPoint,
                                               String levelType, String levelValue, String interval,
                                               String fromDate, String toDate) {
        return serviceClient.getTrending(accountNo, ibx, dataPoint, levelType, levelValue, interval, fromDate, toDate);
    }
}
