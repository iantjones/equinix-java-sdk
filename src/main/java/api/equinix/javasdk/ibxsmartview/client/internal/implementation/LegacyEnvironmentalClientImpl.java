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

import api.equinix.javasdk.core.client.PageableBase;
import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.request.EquinixRequest;
import api.equinix.javasdk.core.http.response.EquinixResponse;
import api.equinix.javasdk.core.enums.RequestType;
import api.equinix.javasdk.ibxsmartview.client.implementation.IBXSmartViewConfigImpl;
import api.equinix.javasdk.ibxsmartview.client.internal.LegacyEnvironmentalClient;
import api.equinix.javasdk.ibxsmartview.model.EnvironmentData;
import api.equinix.javasdk.ibxsmartview.model.TrendingEnvironmentData;
import api.equinix.javasdk.ibxsmartview.model.json.EnvironmentDataJson;
import api.equinix.javasdk.ibxsmartview.model.json.TrendingEnvironmentDataJson;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LegacyEnvironmentalClientImpl extends PageableBase implements LegacyEnvironmentalClient {

    public LegacyEnvironmentalClientImpl(IBXSmartViewConfigImpl configClient) {
        super(configClient, "IBXSmartView", "LegacyEnvironmental");
    }

    public EnvironmentDataJson getCurrent(String accountNo, String ibx, String levelType, String levelValue) {
        Map<String, List<String>> qParams = new HashMap<>();
        Utils.addAdditionalValue(qParams, "accountNo", accountNo);
        Utils.addAdditionalValue(qParams, "ibx", ibx);
        Utils.addAdditionalValue(qParams, "levelType", levelType);
        Utils.addAdditionalValue(qParams, "levelValue", levelValue);

        EquinixRequest<EnvironmentData> equinixRequest = this.buildRequestWithQueryParams("GetCurrentEnvironment", RequestType.SINGLE, qParams, EnvironmentDataJson.getSingleTypeRef());
        EquinixResponse<EnvironmentData> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public List<EnvironmentDataJson> listCurrent(String accountNo, String ibx, String levelType) {
        Map<String, List<String>> qParams = new HashMap<>();
        Utils.addAdditionalValue(qParams, "accountNo", accountNo);
        Utils.addAdditionalValue(qParams, "ibx", ibx);
        Utils.addAdditionalValue(qParams, "levelType", levelType);

        EquinixRequest<EnvironmentData> equinixRequest = this.buildRequestWithQueryParams("ListCurrentEnvironment", RequestType.LIST, qParams, EnvironmentDataJson.getListTypeRef());
        EquinixResponse<EnvironmentData> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleListResponse(equinixResponse, equinixRequest);
    }

    public TrendingEnvironmentDataJson getTrending(String accountNo, String ibx, String dataPoint,
                                                   String levelType, String levelValue, String interval,
                                                   String fromDate, String toDate) {
        Map<String, List<String>> qParams = new HashMap<>();
        Utils.addAdditionalValue(qParams, "accountNo", accountNo);
        Utils.addAdditionalValue(qParams, "ibx", ibx);
        Utils.addAdditionalValue(qParams, "dataPoint", dataPoint);
        Utils.addAdditionalValue(qParams, "levelType", levelType);
        Utils.addAdditionalValue(qParams, "levelValue", levelValue);
        Utils.addAdditionalValue(qParams, "interval", interval);
        Utils.addAdditionalValue(qParams, "fromDate", fromDate);
        Utils.addAdditionalValue(qParams, "toDate", toDate);

        EquinixRequest<TrendingEnvironmentData> equinixRequest = this.buildRequestWithQueryParams("GetTrendingEnvironment", RequestType.SINGLE, qParams, TrendingEnvironmentDataJson.getSingleTypeRef());
        EquinixResponse<TrendingEnvironmentData> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

}
