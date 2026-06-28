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

package api.equinix.javasdk.iam.client.internal;

import api.equinix.javasdk.iam.model.PermissionSet;
import api.equinix.javasdk.iam.model.json.PermissionSetList;
import api.equinix.javasdk.iam.model.json.creators.CreatePermissionSetRequest;
import api.equinix.javasdk.iam.model.json.creators.UpdatePermissionSetRequest;

/**
 * Internal client for project-scoped IAM permission sets
 * ({@code /v1/projects/{projectId}/permissionSets}). Operations (by operationId):
 * {@code listPermissionSets}, {@code createPermissionSet}, {@code getPermissionSet},
 * {@code updatePermissionSet}, {@code deletePermissionSet}.
 */
public interface PermissionSetClient {

    /** {@code listPermissionSets} */
    PermissionSetList list(String projectId, String pageToken, Integer pageSize);

    /** {@code createPermissionSet} */
    PermissionSet create(String projectId, CreatePermissionSetRequest request);

    /** {@code getPermissionSet} */
    PermissionSet getByUuid(String projectId, String permissionSetId);

    /** {@code updatePermissionSet} */
    PermissionSet update(String projectId, String permissionSetId, UpdatePermissionSetRequest request);

    /** {@code deletePermissionSet} */
    Boolean delete(String projectId, String permissionSetId);
}
