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

package api.equinix.javasdk.ibxsmartview.client.internal.implementation;

import api.equinix.javasdk.core.client.ClientBase;
import api.equinix.javasdk.core.http.ParameterMapper;
import api.equinix.javasdk.ibxsmartview.client.implementation.IBXSmartViewConfigImpl;
import api.equinix.javasdk.ibxsmartview.client.internal.LegacyEnvironmentalClient;
import api.equinix.javasdk.ibxsmartview.model.json.EnvironmentDataForArrayJson;
import api.equinix.javasdk.ibxsmartview.model.json.EnvironmentDataJson;
import api.equinix.javasdk.ibxsmartview.model.json.EnvironmentDataResponseJson;
import api.equinix.javasdk.ibxsmartview.model.json.TrendingEnvironmentDataJson;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LegacyEnvironmentalClientImpl extends ClientBase implements LegacyEnvironmentalClient {

    public LegacyEnvironmentalClientImpl(IBXSmartViewConfigImpl configClient) {
        super(configClient, "IBXSmartView", "LegacyEnvironmental");
    }

    public EnvironmentDataJson getCurrent(String accountNo, String ibx, String levelType, String levelValue) {
        Map<String, List<String>> qParams = new HashMap<>();
        ParameterMapper.addAdditionalValue(qParams, "accountNo", accountNo);
        ParameterMapper.addAdditionalValue(qParams, "ibx", ibx);
        ParameterMapper.addAdditionalValue(qParams, "levelType", levelType);
        ParameterMapper.addAdditionalValue(qParams, "levelValue", levelValue);
        return getAs("GetCurrentEnvironment", null, qParams, EnvironmentDataJson.class);
    }

    public List<EnvironmentDataForArrayJson> listCurrent(String accountNo, String ibx, String levelType) {
        Map<String, List<String>> qParams = new HashMap<>();
        ParameterMapper.addAdditionalValue(qParams, "accountNo", accountNo);
        ParameterMapper.addAdditionalValue(qParams, "ibx", ibx);
        ParameterMapper.addAdditionalValue(qParams, "levelType", levelType);
        EnvironmentDataResponseJson response = getAs("ListCurrentEnvironment", null, qParams, EnvironmentDataResponseJson.class);
        if (response == null || response.getPayLoad() == null || response.getPayLoad().getData() == null) {
            return Collections.emptyList();
        }
        return response.getPayLoad().getData();
    }

    public TrendingEnvironmentDataJson getTrending(String accountNo, String ibx, String dataPoint,
                                                   String levelType, String levelValue, String interval,
                                                   String fromDate, String toDate) {
        Map<String, List<String>> qParams = new HashMap<>();
        ParameterMapper.addAdditionalValue(qParams, "accountNo", accountNo);
        ParameterMapper.addAdditionalValue(qParams, "ibx", ibx);
        ParameterMapper.addAdditionalValue(qParams, "dataPoint", dataPoint);
        ParameterMapper.addAdditionalValue(qParams, "levelType", levelType);
        ParameterMapper.addAdditionalValue(qParams, "levelValue", levelValue);
        ParameterMapper.addAdditionalValue(qParams, "interval", interval);
        ParameterMapper.addAdditionalValue(qParams, "fromDate", fromDate);
        ParameterMapper.addAdditionalValue(qParams, "toDate", toDate);
        return getAs("GetTrendingEnvironment", null, qParams, TrendingEnvironmentDataJson.class);
    }
}
