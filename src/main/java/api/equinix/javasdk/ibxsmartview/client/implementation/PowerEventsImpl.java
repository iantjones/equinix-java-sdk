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
import api.equinix.javasdk.core.http.ResponseHandler;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.Pageable;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.ibxsmartview.client.PowerEvents;
import api.equinix.javasdk.ibxsmartview.client.internal.PowerEventClient;
import api.equinix.javasdk.ibxsmartview.model.PowerAlertConfiguration;
import api.equinix.javasdk.ibxsmartview.model.PowerEvent;
import api.equinix.javasdk.ibxsmartview.model.json.PowerAlertConfigurationJson;
import api.equinix.javasdk.ibxsmartview.model.json.PowerEventJson;
import api.equinix.javasdk.ibxsmartview.model.json.creators.PowerAlertConfigurationOperator;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class PowerEventsImpl implements PowerEvents {

    private final PowerEventClient<PowerEvent> serviceClient;

    private final IBXSmartView serviceManager;

    public PaginatedList<PowerEvent> search(List<String> ibx, List<String> status, String edgeCollectedOn, int offset, int limit) {
        Page<PowerEventJson> responsePage = serviceClient.getPowerEvents(ibx, status, edgeCollectedOn, offset, limit);
        PaginatedList<PowerEvent> eventList = ResponseHandler.mapPaginatedList(responsePage.getItems(), this.serviceClient, (json, client) -> json);
        return new PaginatedList<>(eventList, this.serviceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    public PowerAlertConfigurationOperator.PowerAlertConfigurationBuilder defineAlertConfiguration() {
        return new PowerAlertConfigurationOperator(this.serviceClient).create();
    }

    public PaginatedList<PowerAlertConfiguration> searchAlertConfigurations(List<String> ibx, List<String> state, int offset, int limit) {
        Page<PowerAlertConfigurationJson> responsePage = serviceClient.searchAlertConfigurations(ibx, state, offset, limit);
        // Configuration paging needs a dedicated Pageable: the internal client is a Pageable<PowerEvent>
        // whose inherited nextPage(...) wraps with a PowerEventJson checkcast, which would
        // ClassCastException on the deserialized PowerAlertConfigurationJson items. The dedicated
        // pageable deserializes the nested AlertPaginatedResponse and wraps with identity.
        Pageable<PowerAlertConfiguration> pageableClient = serviceClient.alertConfigurationPageable();
        PaginatedList<PowerAlertConfiguration> configList = ResponseHandler.mapPaginatedList(responsePage.getItems(), pageableClient, (json, client) -> json);
        return new PaginatedList<>(configList, pageableClient, responsePage.getAssociatedRequest(),
                responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    public PowerAlertConfigurationOperator.PowerAlertConfigurationUpdateBuilder updateAlertConfiguration(String alertConfigurationUid) {
        return new PowerAlertConfigurationOperator(this.serviceClient).update(alertConfigurationUid);
    }

    public void pauseAlertConfiguration(String alertConfigurationUid) {
        serviceClient.pauseAlertConfiguration(alertConfigurationUid);
    }

    public void resumeAlertConfiguration(String alertConfigurationUid) {
        serviceClient.resumeAlertConfiguration(alertConfigurationUid);
    }

    public void deleteAlertConfiguration(String alertConfigurationUid) {
        serviceClient.deleteAlertConfiguration(alertConfigurationUid);
    }
}
