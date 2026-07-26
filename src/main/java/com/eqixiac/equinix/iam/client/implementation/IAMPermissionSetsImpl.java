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

package com.eqixiac.equinix.iam.client.implementation;

import com.eqixiac.equinix.IAM;
import com.eqixiac.equinix.iam.client.IAMPermissionSets;
import com.eqixiac.equinix.iam.client.internal.PermissionSetClient;
import com.eqixiac.equinix.iam.model.PermissionSet;
import com.eqixiac.equinix.iam.model.json.PermissionSetList;
import com.eqixiac.equinix.iam.model.json.creators.CreatePermissionSetRequest;
import com.eqixiac.equinix.iam.model.json.creators.UpdatePermissionSetRequest;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class IAMPermissionSetsImpl implements IAMPermissionSets {

    private final PermissionSetClient permissionSetClient;

    private final IAM serviceManager;

    public PermissionSetList list(String projectId) {
        return this.permissionSetClient.list(projectId, null, null);
    }

    public PermissionSetList list(String projectId, String pageToken, Integer pageSize) {
        return this.permissionSetClient.list(projectId, pageToken, pageSize);
    }

    public PermissionSet create(String projectId, CreatePermissionSetRequest request) {
        return this.permissionSetClient.create(projectId, request);
    }

    public PermissionSet getByUuid(String projectId, String permissionSetId) {
        return this.permissionSetClient.getByUuid(projectId, permissionSetId);
    }

    public PermissionSet update(String projectId, String permissionSetId, UpdatePermissionSetRequest request) {
        return this.permissionSetClient.update(projectId, permissionSetId, request);
    }

    public Boolean delete(String projectId, String permissionSetId, String lastRev) {
        return this.permissionSetClient.delete(projectId, permissionSetId, lastRev);
    }
}
