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

package api.equinix.javasdk.fabric.client.internal;

import api.equinix.javasdk.core.http.request.PatchOperation;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.Pageable;
import api.equinix.javasdk.fabric.enums.PrecisionTimePackageCode;
import api.equinix.javasdk.fabric.model.PrecisionTime;
import api.equinix.javasdk.fabric.model.TimeServiceConnection;
import api.equinix.javasdk.fabric.model.TimeServicePackage;
import api.equinix.javasdk.fabric.model.implementation.TimeServiceFulfillRequest;
import api.equinix.javasdk.fabric.model.json.PrecisionTimeJson;
import api.equinix.javasdk.fabric.model.json.TimeServicePackageJson;
import api.equinix.javasdk.fabric.model.json.creators.PrecisionTimeCreatorJson;

import java.util.List;

public interface PrecisionTimeClient<T> extends Pageable<T> {

    Page<PrecisionTimeJson> list();

    PrecisionTimeJson getByUuid(String uuid);

    PrecisionTimeJson create(PrecisionTimeCreatorJson precisionTimeCreatorJson);

    PrecisionTimeJson fulfill(String uuid, TimeServiceFulfillRequest request);

    PrecisionTimeJson update(String uuid, List<PatchOperation> operations);

    PrecisionTimeJson delete(String uuid);

    List<TimeServicePackage> getPackages();

    TimeServicePackageJson getPackageByCode(PrecisionTimePackageCode packageCode);

    List<TimeServiceConnection> getConnections(String serviceId);

    PrecisionTimeJson refresh(String uuid);
}
