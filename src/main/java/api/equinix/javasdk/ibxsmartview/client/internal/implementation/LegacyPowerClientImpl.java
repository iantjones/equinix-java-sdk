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
import api.equinix.javasdk.ibxsmartview.client.internal.LegacyPowerClient;
import api.equinix.javasdk.ibxsmartview.model.PowerData;
import api.equinix.javasdk.ibxsmartview.model.TrendingPowerData;
import api.equinix.javasdk.ibxsmartview.model.json.PowerDataJson;
import api.equinix.javasdk.ibxsmartview.model.json.TrendingPowerDataJson;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LegacyPowerClientImpl extends PageableBase implements LegacyPowerClient {

    public LegacyPowerClientImpl(IBXSmartViewConfigImpl configClient) {
        super(configClient, "IBXSmartView", "LegacyPower");
    }

    public PowerDataJson getCurrent(String accountNo, String ibx, String levelType, String levelValue) {
        Map<String, List<String>> qParams = new HashMap<>();
        Utils.addAdditionalValue(qParams, "accountNo", accountNo);
        Utils.addAdditionalValue(qParams, "ibx", ibx);
        Utils.addAdditionalValue(qParams, "levelType", levelType);
        Utils.addAdditionalValue(qParams, "levelValue", levelValue);

        EquinixRequest<PowerData> equinixRequest = this.buildRequestWithQueryParams("GetCurrentPower", RequestType.SINGLE, qParams, PowerDataJson.getSingleTypeRef());
        EquinixResponse<PowerData> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    public List<PowerDataJson> postCurrent(Object requestBody) {
        EquinixRequest<PowerData> equinixRequest = this.buildRequest("PostCurrentPower", RequestType.LIST, PowerDataJson.getListTypeRef());
        Utils.serializeJson(equinixRequest, requestBody);
        EquinixResponse<PowerData> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleListResponse(equinixResponse, equinixRequest);
    }

    public TrendingPowerDataJson getTrending(String accountNo, String ibx, String levelType,
                                             String levelValue, String interval,
                                             String fromDate, String toDate) {
        Map<String, List<String>> qParams = new HashMap<>();
        Utils.addAdditionalValue(qParams, "accountNo", accountNo);
        Utils.addAdditionalValue(qParams, "ibx", ibx);
        Utils.addAdditionalValue(qParams, "levelType", levelType);
        Utils.addAdditionalValue(qParams, "levelValue", levelValue);
        Utils.addAdditionalValue(qParams, "interval", interval);
        Utils.addAdditionalValue(qParams, "fromDate", fromDate);
        Utils.addAdditionalValue(qParams, "toDate", toDate);

        EquinixRequest<TrendingPowerData> equinixRequest = this.buildRequestWithQueryParams("GetTrendingPower", RequestType.SINGLE, qParams, TrendingPowerDataJson.getSingleTypeRef());
        EquinixResponse<TrendingPowerData> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

}
