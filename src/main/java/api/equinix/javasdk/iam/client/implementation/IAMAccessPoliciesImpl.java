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

package api.equinix.javasdk.iam.client.implementation;

import api.equinix.javasdk.IAM;
import api.equinix.javasdk.iam.client.IAMAccessPolicies;
import api.equinix.javasdk.iam.client.internal.AccessPolicyClient;
import api.equinix.javasdk.iam.model.AccessPolicy;
import api.equinix.javasdk.iam.model.AccessPolicyGrant;
import api.equinix.javasdk.iam.model.json.AccessPolicyGrantList;
import api.equinix.javasdk.iam.model.json.AccessPolicyList;
import api.equinix.javasdk.iam.model.json.creators.AddGrantRequest;
import api.equinix.javasdk.iam.model.json.creators.CreateAccessPolicyRequest;
import api.equinix.javasdk.iam.model.json.creators.UpdateAccessPolicyRequest;

public class IAMAccessPoliciesImpl implements IAMAccessPolicies {

    private final IAM serviceManager;

    private final AccessPolicyClient accessPolicyClient;

    public IAMAccessPoliciesImpl(AccessPolicyClient accessPolicyClient, IAM serviceManager) {
        this.serviceManager = serviceManager;
        this.accessPolicyClient = accessPolicyClient;
    }

    public AccessPolicyList list(String projectId) {
        return this.accessPolicyClient.list(projectId, null, null);
    }

    public AccessPolicyList list(String projectId, String pageToken, Integer pageSize) {
        return this.accessPolicyClient.list(projectId, pageToken, pageSize);
    }

    public AccessPolicy create(String projectId, CreateAccessPolicyRequest request) {
        return this.accessPolicyClient.create(projectId, request);
    }

    public AccessPolicy getByUuid(String projectId, String accessPolicyId) {
        return this.accessPolicyClient.getByUuid(projectId, accessPolicyId);
    }

    public AccessPolicy update(String projectId, String accessPolicyId, UpdateAccessPolicyRequest request) {
        return this.accessPolicyClient.update(projectId, accessPolicyId, request);
    }

    public Boolean delete(String projectId, String accessPolicyId, String lastRev) {
        return this.accessPolicyClient.delete(projectId, accessPolicyId, lastRev);
    }

    public AccessPolicy enable(String projectId, String accessPolicyId, String lastRev) {
        return this.accessPolicyClient.enable(projectId, accessPolicyId, lastRev);
    }

    public AccessPolicy disable(String projectId, String accessPolicyId, String lastRev) {
        return this.accessPolicyClient.disable(projectId, accessPolicyId, lastRev);
    }

    public AccessPolicyGrantList listGrants(String projectId, String accessPolicyId) {
        return this.accessPolicyClient.listGrants(projectId, accessPolicyId, null, null);
    }

    public AccessPolicyGrantList listGrants(String projectId, String accessPolicyId, String pageToken, Integer pageSize) {
        return this.accessPolicyClient.listGrants(projectId, accessPolicyId, pageToken, pageSize);
    }

    public AccessPolicyGrant addGrant(String projectId, String accessPolicyId, AddGrantRequest request) {
        return this.accessPolicyClient.addGrant(projectId, accessPolicyId, request);
    }

    public Boolean removeGrant(String projectId, String accessPolicyId, String grantId) {
        return this.accessPolicyClient.removeGrant(projectId, accessPolicyId, grantId);
    }
}
