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

package api.equinix.javasdk.fabric.client.internal.implementation;

import api.equinix.javasdk.core.client.ResourceClientBase;
import api.equinix.javasdk.core.enums.RequestType;
import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.request.EquinixRequest;
import api.equinix.javasdk.core.http.request.PatchOperation;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.fabric.client.implementation.FabricConfigImpl;
import api.equinix.javasdk.fabric.client.internal.PrecisionTimeClient;
import api.equinix.javasdk.fabric.enums.PrecisionTimePackageCode;
import api.equinix.javasdk.fabric.model.PrecisionTime;
import api.equinix.javasdk.fabric.model.TimeServiceConnection;
import api.equinix.javasdk.fabric.model.TimeServicePackage;
import api.equinix.javasdk.fabric.model.json.PrecisionTimeJson;
import api.equinix.javasdk.fabric.model.json.TimeServiceConnectionJson;
import api.equinix.javasdk.fabric.model.json.TimeServicePackageJson;
import api.equinix.javasdk.fabric.model.json.creators.PrecisionTimeCreatorJson;
import api.equinix.javasdk.fabric.model.wrappers.PrecisionTimeWrapper;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class PrecisionTimeClientImpl extends ResourceClientBase<PrecisionTime, PrecisionTimeJson> implements PrecisionTimeClient<PrecisionTime> {

    public PrecisionTimeClientImpl(FabricConfigImpl configClient) {
        super(configClient, "Fabric", "PrecisionTime", PrecisionTimeJson.class);
    }

    @Override
    protected PrecisionTime wrap(PrecisionTimeJson json) {
        return new PrecisionTimeWrapper(json, this);
    }

    public Page<PrecisionTime, PrecisionTimeJson> list() {
        return listPage("GetTimeServices");
    }

    public PrecisionTimeJson getByUuid(String uuid) {
        return getOne("GetTimeService", uuid);
    }

    public PrecisionTimeJson create(PrecisionTimeCreatorJson precisionTimeCreatorJson) {
        return postOne("PostTimeService", precisionTimeCreatorJson);
    }

    public PrecisionTimeJson update(String uuid, List<PatchOperation> operations) {
        return patchOne("UpdateTimeService", uuid, operations);
    }

    public PrecisionTimeJson delete(String uuid) {
        return deleteOne("DeleteTimeService", uuid);
    }

    public List<TimeServicePackage> getPackages() {
        EquinixRequest<TimeServicePackage> request = buildRequest("GetTimeServicePackages", RequestType.PAGINATED, TimeServicePackageJson.class);
        Page<TimeServicePackage, TimeServicePackageJson> page = Utils.handlePaginatedListResponse(invoke(request), request);
        return (page != null && page.getItems() != null) ? List.copyOf(page.getItems()) : Collections.emptyList();
    }

    public TimeServicePackageJson getPackageByCode(PrecisionTimePackageCode packageCode) {
        return getAs("GetTimeServicePackage", Map.of("packageCode", packageCode.toString()), null, TimeServicePackageJson.class);
    }

    public List<TimeServiceConnection> getConnections(String serviceId) {
        EquinixRequest<TimeServiceConnection> request = buildRequestWithPathParams("GetTimeServiceConnections", RequestType.PAGINATED,
                Map.of("serviceId", serviceId), TimeServiceConnectionJson.class);
        Page<TimeServiceConnection, TimeServiceConnectionJson> page = Utils.handlePaginatedListResponse(invoke(request), request);
        return (page != null && page.getItems() != null) ? List.copyOf(page.getItems()) : Collections.emptyList();
    }

    public PrecisionTimeJson refresh(String uuid) {
        return getByUuid(uuid);
    }
}
