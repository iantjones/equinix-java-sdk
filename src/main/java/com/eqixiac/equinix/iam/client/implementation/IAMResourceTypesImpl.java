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
import com.eqixiac.equinix.iam.client.IAMResourceTypes;
import com.eqixiac.equinix.iam.client.internal.ResourceTypeClient;
import com.eqixiac.equinix.iam.model.ServicePolicySchema;
import com.eqixiac.equinix.iam.model.json.ActionList;
import com.eqixiac.equinix.iam.model.json.ResourceTypeActionPage;
import com.eqixiac.equinix.iam.model.json.ResourceTypeList;
import com.eqixiac.equinix.iam.model.json.ServiceActionSetList;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class IAMResourceTypesImpl implements IAMResourceTypes {

    private final ResourceTypeClient resourceTypeClient;

    private final IAM serviceManager;

    public ResourceTypeList listResourceTypes(String projectId, String serviceId) {
        return this.resourceTypeClient.listResourceTypes(projectId, serviceId, null, null, null);
    }

    public ResourceTypeList listResourceTypes(String projectId, String serviceId, String pageToken, Integer pageSize,
                                              String projectErn) {
        return this.resourceTypeClient.listResourceTypes(projectId, serviceId, pageToken, pageSize, projectErn);
    }

    public ActionList listActions(String projectId, String serviceId) {
        return this.resourceTypeClient.listActions(projectId, serviceId, null, null, null);
    }

    public ActionList listActions(String projectId, String serviceId, String pageToken, Integer pageSize,
                                  String projectErn) {
        return this.resourceTypeClient.listActions(projectId, serviceId, pageToken, pageSize, projectErn);
    }

    public ServiceActionSetList listActionSets(String projectId, String serviceId) {
        return this.resourceTypeClient.listActionSets(projectId, serviceId, null, null, null);
    }

    public ServiceActionSetList listActionSets(String projectId, String serviceId, String pageToken, Integer pageSize,
                                               String projectErn) {
        return this.resourceTypeClient.listActionSets(projectId, serviceId, pageToken, pageSize, projectErn);
    }

    public ResourceTypeActionPage listResourceTypeActions(String projectId, String serviceId, String resourceType) {
        return this.resourceTypeClient.listResourceTypeActions(projectId, serviceId, resourceType, null, null, null,
                null);
    }

    public ResourceTypeActionPage listResourceTypeActions(String projectId, String serviceId, String resourceType,
                                                          String resourceTypeServiceId, String lastAction,
                                                          Integer pageSize, String projectErn) {
        return this.resourceTypeClient.listResourceTypeActions(projectId, serviceId, resourceType,
                resourceTypeServiceId, lastAction, pageSize, projectErn);
    }

    public ServicePolicySchema getServicePolicySchema(String projectId, String serviceId) {
        return this.resourceTypeClient.getServicePolicySchema(projectId, serviceId);
    }
}
