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
import api.equinix.javasdk.core.http.Utils;
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
import lombok.Getter;

import java.util.List;

@Getter
public class PowerEventsImpl implements PowerEvents {

    private final IBXSmartView serviceManager;

    private final PowerEventClient<PowerEvent> serviceClient;

    public PowerEventsImpl(PowerEventClient<PowerEvent> serviceClient, IBXSmartView serviceManager) {
        this.serviceManager = serviceManager;
        this.serviceClient = serviceClient;
    }

    public PaginatedList<PowerEvent> search(List<String> ibx, List<String> status, String edgeCollectedOn, int offset, int limit) {
        Page<PowerEvent, PowerEventJson> responsePage = serviceClient.getPowerEvents(ibx, status, edgeCollectedOn, offset, limit);
        PaginatedList<PowerEvent> eventList = Utils.mapPaginatedList(responsePage.getItems(), this.serviceClient, (json, client) -> json);
        return new PaginatedList<>(eventList, this.serviceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    public PowerAlertConfigurationOperator.PowerAlertConfigurationBuilder defineAlertConfiguration() {
        return new PowerAlertConfigurationOperator(this.serviceClient).create();
    }

    @SuppressWarnings("unchecked")
    public PaginatedList<PowerAlertConfiguration> searchAlertConfigurations(List<String> ibx, List<String> state, int offset, int limit) {
        Page<PowerAlertConfiguration, PowerAlertConfigurationJson> responsePage = serviceClient.searchAlertConfigurations(ibx, state, offset, limit);
        // The internal client is a Pageable<PowerEvent>; its inherited nextPage(...) deserializes
        // each subsequent page using the request's own response type (PowerAlertConfigurationJson),
        // so reusing it for configuration paging is correct — only the generic parameter is laundered.
        Pageable<PowerAlertConfiguration> pageableClient = (Pageable<PowerAlertConfiguration>) (Object) this.serviceClient;
        PaginatedList<PowerAlertConfiguration> configList = Utils.mapPaginatedList(responsePage.getItems(), pageableClient, (json, client) -> json);
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
