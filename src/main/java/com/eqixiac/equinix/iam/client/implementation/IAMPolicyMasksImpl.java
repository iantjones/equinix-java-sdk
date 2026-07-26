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
import com.eqixiac.equinix.iam.client.IAMPolicyMasks;
import com.eqixiac.equinix.iam.client.internal.PolicyMaskClient;
import com.eqixiac.equinix.iam.model.PolicyMask;
import com.eqixiac.equinix.iam.model.json.PolicyMaskList;
import com.eqixiac.equinix.iam.model.json.creators.CreatePolicyMaskRequest;
import com.eqixiac.equinix.iam.model.json.creators.UpdatePolicyMaskRequest;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class IAMPolicyMasksImpl implements IAMPolicyMasks {

    private final PolicyMaskClient policyMaskClient;

    private final IAM serviceManager;

    public PolicyMaskList list(String projectId) {
        return this.policyMaskClient.list(projectId, null, null);
    }

    public PolicyMaskList list(String projectId, String pageToken, Integer pageSize) {
        return this.policyMaskClient.list(projectId, pageToken, pageSize);
    }

    public PolicyMask create(String projectId, CreatePolicyMaskRequest request) {
        return this.policyMaskClient.create(projectId, request);
    }

    public PolicyMask getByUuid(String projectId, String policyMaskId) {
        return this.policyMaskClient.getByUuid(projectId, policyMaskId);
    }

    public PolicyMask update(String projectId, String policyMaskId, UpdatePolicyMaskRequest request) {
        return this.policyMaskClient.update(projectId, policyMaskId, request);
    }

    public Boolean delete(String projectId, String policyMaskId, String lastRev) {
        return this.policyMaskClient.delete(projectId, policyMaskId, lastRev);
    }

    public PolicyMask enable(String projectId, String policyMaskId, String lastRev) {
        return this.policyMaskClient.enable(projectId, policyMaskId, lastRev);
    }

    public PolicyMask disable(String projectId, String policyMaskId, String lastRev) {
        return this.policyMaskClient.disable(projectId, policyMaskId, lastRev);
    }
}
