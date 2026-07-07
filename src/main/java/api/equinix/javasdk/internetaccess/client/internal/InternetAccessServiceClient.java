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

package api.equinix.javasdk.internetaccess.client.internal;

import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.PageablePost;
import api.equinix.javasdk.internetaccess.model.InternetAccessService;
import api.equinix.javasdk.internetaccess.model.json.InternetAccessServiceJson;
import api.equinix.javasdk.internetaccess.model.json.creators.ChangeOperationUpdate;
import api.equinix.javasdk.internetaccess.model.json.creators.ServiceRequest;
import api.equinix.javasdk.internetaccess.model.json.creators.ServiceSearchRequest;

import java.util.List;

/**
 * Internal client for the Equinix Internet Access (EIA) v2 service lifecycle:
 * {@code POST /internetAccess/v2/services} (create),
 * {@code GET /internetAccess/v2/services/{serviceId}} (get details),
 * {@code PATCH /internetAccess/v2/services/{serviceId}} (update),
 * {@code DELETE /internetAccess/v2/services/{serviceId}} (delete) and
 * {@code POST /internetAccess/v2/services/search} (search).
 */
public interface InternetAccessServiceClient extends PageablePost<InternetAccessService> {

    InternetAccessService create(ServiceRequest serviceRequest);

    InternetAccessServiceJson getByUuid(String serviceId);

    InternetAccessServiceJson update(String serviceId, List<ChangeOperationUpdate> operations, boolean dryRun);

    Boolean delete(String serviceId, boolean dryRun);

    Page<InternetAccessServiceJson> search(ServiceSearchRequest searchRequest);
}
