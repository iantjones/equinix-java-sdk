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

import api.equinix.javasdk.core.client.ResourceClientBase;
import api.equinix.javasdk.core.enums.RequestType;
import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.request.EquinixRequest;
import api.equinix.javasdk.core.http.response.EquinixResponse;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.Pagination;
import api.equinix.javasdk.core.internal.Constants;
import api.equinix.javasdk.ibxsmartview.client.implementation.IBXSmartViewConfigImpl;
import api.equinix.javasdk.ibxsmartview.client.internal.PowerEventClient;
import api.equinix.javasdk.ibxsmartview.model.PowerAlertConfiguration;
import api.equinix.javasdk.ibxsmartview.model.PowerEvent;
import api.equinix.javasdk.ibxsmartview.model.json.PowerAlertConfigurationCreateResponseJson;
import api.equinix.javasdk.ibxsmartview.model.json.PowerAlertConfigurationJson;
import api.equinix.javasdk.ibxsmartview.model.json.PowerEventJson;
import api.equinix.javasdk.ibxsmartview.model.json.PowerEventsPaginatedResponseJson;
import api.equinix.javasdk.ibxsmartview.model.json.creators.PowerAlertConfigurationCreatorJson;
import api.equinix.javasdk.ibxsmartview.model.json.creators.PowerAlertConfigurationUpdateJson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PowerEventClientImpl extends ResourceClientBase<PowerEvent, PowerEventJson> implements PowerEventClient<PowerEvent> {

    public PowerEventClientImpl(IBXSmartViewConfigImpl configClient) {
        super(configClient, "IBXSmartView", "PowerEvents", PowerEventJson.class);
    }

    @Override
    protected PowerEvent wrap(PowerEventJson json) {
        return json;
    }

    // GET /dcim/v3/powerEvents/search — operationId getPowerEvents
    //
    // PowerEventsPaginatedResponse returns its pagination fields (items/limit/offset/totalCount) at
    // the TOP LEVEL rather than under a nested 'pagination' object, so the response cannot be
    // deserialized directly into the core Page (which expects {data/items, pagination{offset,limit,
    // total}}). We deserialize the flat shape ourselves and assemble a Page with a Pagination whose
    // total is mapped from totalCount, so getIsLastPage()/total are correct for the returned page.
    public Page<PowerEvent, PowerEventJson> getPowerEvents(List<String> ibx, List<String> status, String edgeCollectedOn, int offset, int limit) {
        Map<String, List<String>> qParams = new HashMap<>();
        if (ibx != null && !ibx.isEmpty()) {
            qParams.put("ibx", List.of(String.join(",", ibx)));
        }
        if (status != null && !status.isEmpty()) {
            qParams.put("status", List.of(String.join(",", status)));
        }
        if (edgeCollectedOn != null) {
            qParams.put("edgeCollectedOn", List.of(edgeCollectedOn));
        }
        qParams.put("offset", List.of(String.valueOf(offset)));
        qParams.put("limit", List.of(String.valueOf(limit)));

        EquinixRequest<PowerEvent> request = buildRequestWithQueryParams(
                "GetPowerEvents", RequestType.PAGINATED, qParams, PowerEventJson.class);
        EquinixResponse<PowerEvent> response = invoke(request);
        // Re-target deserialization at the flat response holder before reading the body.
        request.setJavaType(Constants.objectMapper.getTypeFactory().constructType(PowerEventsPaginatedResponseJson.class));
        PowerEventsPaginatedResponseJson flat = Utils.handleSingletonResponse(response, request);

        Page<PowerEvent, PowerEventJson> page = new Page<>();
        page.setItems(flat != null && flat.getItems() != null ? flat.getItems() : new ArrayList<>());
        page.setPagination(toPagination(flat));
        page.setAssociatedRequest(request);
        page.setAssociatedResponse(response);
        return page;
    }

    private Pagination toPagination(PowerEventsPaginatedResponseJson flat) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("offset", flat != null ? flat.getOffset() : null);
        values.put("limit", flat != null ? flat.getLimit() : null);
        values.put("total", flat != null ? flat.getTotalCount() : null);
        return Constants.objectMapper.convertValue(values, Pagination.class);
    }

    // POST /dcim/v3/powerEvents/configurations — operationId createPowerAlertConfiguration
    public PowerAlertConfigurationCreateResponseJson createPowerAlertConfiguration(PowerAlertConfigurationCreatorJson creatorJson) {
        return postAs("CreatePowerAlertConfiguration", creatorJson, PowerAlertConfigurationCreateResponseJson.class);
    }

    // PUT /dcim/v3/powerEvents/configurations — operationId updatePowerAlertConfiguration
    public void updatePowerAlertConfiguration(PowerAlertConfigurationUpdateJson updateJson) {
        voidOp("UpdatePowerAlertConfiguration", RequestType.SINGLE, null, null, updateJson);
    }

    // GET /dcim/v3/powerEvents/configurations/search — operationId searchAlertConfigurations
    public Page<PowerAlertConfiguration, PowerAlertConfigurationJson> searchAlertConfigurations(List<String> ibx, List<String> state, int offset, int limit) {
        Map<String, List<String>> qParams = new HashMap<>();
        if (ibx != null && !ibx.isEmpty()) {
            qParams.put("ibx", List.of(String.join(",", ibx)));
        }
        if (state != null && !state.isEmpty()) {
            qParams.put("state", List.of(String.join(",", state)));
        }
        qParams.put("offset", List.of(String.valueOf(offset)));
        qParams.put("limit", List.of(String.valueOf(limit)));
        EquinixRequest<PowerAlertConfiguration> request = buildRequestWithQueryParams(
                "SearchAlertConfigurations", RequestType.PAGINATED, qParams, PowerAlertConfigurationJson.class);
        return Utils.handlePaginatedListResponse(invoke(request), request);
    }

    // PUT /dcim/v3/powerEvents/configurations/{alertConfigurationUid}/pause — operationId pauseAlertConfiguration
    public void pauseAlertConfiguration(String alertConfigurationUid) {
        voidOp("PauseAlertConfiguration", RequestType.SINGLE, Map.of("alertConfigurationUid", alertConfigurationUid), null, null);
    }

    // PUT /dcim/v3/powerEvents/configurations/{alertConfigurationUid}/resume — operationId resumeAlertConfiguration
    public void resumeAlertConfiguration(String alertConfigurationUid) {
        voidOp("ResumeAlertConfiguration", RequestType.SINGLE, Map.of("alertConfigurationUid", alertConfigurationUid), null, null);
    }

    // DELETE /dcim/v3/powerEvents/configurations/{alertConfigurationUid} — operationId deleteAlertConfiguration
    public void deleteAlertConfiguration(String alertConfigurationUid) {
        voidOp("DeleteAlertConfiguration", RequestType.SINGLE, Map.of("alertConfigurationUid", alertConfigurationUid), null, null);
    }
}
