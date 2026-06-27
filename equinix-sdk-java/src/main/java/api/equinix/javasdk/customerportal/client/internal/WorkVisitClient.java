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

package api.equinix.javasdk.customerportal.client.internal;

import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.Pageable;
import api.equinix.javasdk.customerportal.model.WorkVisit;
import api.equinix.javasdk.customerportal.model.json.WorkVisitJson;
import api.equinix.javasdk.customerportal.model.json.creators.WorkVisitCreatorJson;

public interface WorkVisitClient<T> extends Pageable<T> {

    Page<WorkVisit, WorkVisitJson> list();

    WorkVisitJson getByUuid(String uuid);

    WorkVisitJson create(WorkVisitCreatorJson workVisitCreatorJson);

    WorkVisitJson update(String uuid, WorkVisitCreatorJson workVisitCreatorJson);

    WorkVisitJson cancel(String uuid);

    WorkVisitJson refresh(String uuid);
}
