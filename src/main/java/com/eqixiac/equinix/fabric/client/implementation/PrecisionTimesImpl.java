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

package com.eqixiac.equinix.fabric.client.implementation;

import com.eqixiac.equinix.core.http.ResponseHandler;
import com.eqixiac.equinix.core.http.response.Page;
import com.eqixiac.equinix.core.http.response.PaginatedList;
import com.eqixiac.equinix.fabric.client.PrecisionTimes;
import com.eqixiac.equinix.fabric.client.internal.PrecisionTimeClient;
import com.eqixiac.equinix.fabric.enums.PrecisionTimePackageCode;
import com.eqixiac.equinix.fabric.model.PrecisionTime;
import com.eqixiac.equinix.fabric.model.TimeServiceConnection;
import com.eqixiac.equinix.fabric.model.TimeServicePackage;
import com.eqixiac.equinix.fabric.model.implementation.TimeServiceFulfillRequest;
import com.eqixiac.equinix.fabric.model.json.PrecisionTimeJson;
import com.eqixiac.equinix.fabric.model.json.creators.PrecisionTimeOperator;
import com.eqixiac.equinix.fabric.model.wrappers.PrecisionTimeWrapper;

import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PrecisionTimesImpl implements PrecisionTimes {

    private final PrecisionTimeClient<PrecisionTime> serviceClient;

    public PaginatedList<PrecisionTime> list() {
        Page<PrecisionTimeJson> responsePage = this.serviceClient.list();
        PaginatedList<PrecisionTime> precisionTimeList = ResponseHandler.mapPaginatedList(responsePage.getItems(), this.serviceClient, PrecisionTimeWrapper::new);
        return new PaginatedList<>(precisionTimeList, this.serviceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    public PrecisionTime getByUuid(String uuid) {
        PrecisionTimeJson precisionTimeJson = this.serviceClient.getByUuid(uuid);
        return new PrecisionTimeWrapper(precisionTimeJson, this.serviceClient);
    }

    public PrecisionTimeOperator.PrecisionTimeBuilder define() {
        return new PrecisionTimeOperator(this.serviceClient).create();
    }

    public List<TimeServicePackage> packages() {
        return this.serviceClient.getPackages();
    }

    public TimeServicePackage packageByCode(PrecisionTimePackageCode packageCode) {
        return this.serviceClient.getPackageByCode(packageCode);
    }

    public List<TimeServiceConnection> getConnections(String serviceId) {
        return this.serviceClient.getConnections(serviceId);
    }

    public PrecisionTime fulfill(String uuid, List<String> connectionUuids) {
        return fulfill(uuid, new TimeServiceFulfillRequest(connectionUuids));
    }

    public PrecisionTime fulfill(String uuid, TimeServiceFulfillRequest request) {
        PrecisionTimeJson precisionTimeJson = this.serviceClient.fulfill(uuid, request);
        return new PrecisionTimeWrapper(precisionTimeJson, this.serviceClient);
    }
}
