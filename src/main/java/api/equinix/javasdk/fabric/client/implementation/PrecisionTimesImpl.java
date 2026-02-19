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

package api.equinix.javasdk.fabric.client.implementation;

import api.equinix.javasdk.Fabric;
import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.fabric.client.PrecisionTimes;
import api.equinix.javasdk.fabric.client.internal.PrecisionTimeClient;
import api.equinix.javasdk.fabric.model.PrecisionTime;
import api.equinix.javasdk.fabric.model.json.PrecisionTimeJson;
import api.equinix.javasdk.fabric.model.json.creators.PrecisionTimeOperator;
import api.equinix.javasdk.fabric.model.wrappers.PrecisionTimeWrapper;

public class PrecisionTimesImpl implements PrecisionTimes {

    private final Fabric serviceManager;

    private final PrecisionTimeClient<PrecisionTime> serviceClient;

    public PrecisionTimesImpl(PrecisionTimeClient<PrecisionTime> serviceClient, Fabric serviceManager) {
        this.serviceManager = serviceManager;
        this.serviceClient = serviceClient;
    }

    public PaginatedList<PrecisionTime> list() {
        Page<PrecisionTime, PrecisionTimeJson> responsePage = this.serviceClient.list();
        PaginatedList<PrecisionTime> precisionTimeList = Utils.mapPaginatedList(responsePage.getItems(), this.serviceClient, PrecisionTimeWrapper::new);
        return new PaginatedList<>(precisionTimeList, this.serviceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    public PrecisionTime getByUuid(String uuid) {
        PrecisionTimeJson precisionTimeJson = this.serviceClient.getByUuid(uuid);
        return new PrecisionTimeWrapper(precisionTimeJson, this.serviceClient);
    }

    public PrecisionTimeOperator.PrecisionTimeBuilder define() {
        return new PrecisionTimeOperator(this.serviceClient).create();
    }
}
