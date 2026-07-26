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
import com.eqixiac.equinix.iam.client.IAMRoles;
import com.eqixiac.equinix.iam.client.internal.RoleClient;
import com.eqixiac.equinix.iam.model.json.RoleList;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class IAMRolesImpl implements IAMRoles {

    private final RoleClient roleClient;

    private final IAM serviceManager;

    public RoleList list() {
        return this.roleClient.listRoles(null, null, null, null);
    }

    public RoleList list(String pageToken, Integer pageSize, String projectErn) {
        return this.roleClient.listRoles(pageToken, pageSize, null, projectErn);
    }

    public RoleList list(String pageToken, Integer pageSize, String projectId, String projectErn) {
        return this.roleClient.listRoles(pageToken, pageSize, projectId, projectErn);
    }

    public RoleList listByProject(String projectId) {
        return this.roleClient.listRolesByProjectId(projectId, null, null, null);
    }

    public RoleList listByProject(String projectId, String pageToken, Integer pageSize, String projectErn) {
        return this.roleClient.listRolesByProjectId(projectId, pageToken, pageSize, projectErn);
    }
}
