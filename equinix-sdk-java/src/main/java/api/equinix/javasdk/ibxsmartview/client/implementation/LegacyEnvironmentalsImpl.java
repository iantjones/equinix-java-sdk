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
import api.equinix.javasdk.ibxsmartview.client.LegacyEnvironmentals;
import api.equinix.javasdk.ibxsmartview.client.internal.LegacyEnvironmentalClient;
import api.equinix.javasdk.ibxsmartview.model.EnvironmentData;
import api.equinix.javasdk.ibxsmartview.model.TrendingEnvironmentData;
import api.equinix.javasdk.ibxsmartview.model.json.EnvironmentDataJson;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class LegacyEnvironmentalsImpl implements LegacyEnvironmentals {

    private final IBXSmartView serviceManager;

    private final LegacyEnvironmentalClient serviceClient;

    public LegacyEnvironmentalsImpl(LegacyEnvironmentalClient serviceClient, IBXSmartView serviceManager) {
        this.serviceManager = serviceManager;
        this.serviceClient = serviceClient;
    }

    public EnvironmentData getCurrent(String accountNo, String ibx, String levelType, String levelValue) {
        return serviceClient.getCurrent(accountNo, ibx, levelType, levelValue);
    }

    public List<EnvironmentData> listCurrent(String accountNo, String ibx, String levelType) {
        List<EnvironmentDataJson> jsonList = serviceClient.listCurrent(accountNo, ibx, levelType);
        return new ArrayList<>(jsonList);
    }

    public TrendingEnvironmentData getTrending(String accountNo, String ibx, String dataPoint,
                                               String levelType, String levelValue, String interval,
                                               String fromDate, String toDate) {
        return serviceClient.getTrending(accountNo, ibx, dataPoint, levelType, levelValue, interval, fromDate, toDate);
    }
}
