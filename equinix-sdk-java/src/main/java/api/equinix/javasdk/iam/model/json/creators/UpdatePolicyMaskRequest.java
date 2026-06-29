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

package api.equinix.javasdk.iam.model.json.creators;

import api.equinix.javasdk.iam.model.PolicyExpression;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * Request body for updating an IAM policy mask via {@code PUT
 * /v1/projects/{projectId}/policyMasks/{policyMaskId}} (operationId {@code updatePolicyMask}, spec
 * schema {@code UpdatePolicyMaskBody}).
 *
 * <p>The {@code managedPolicies}/{@code managedPermissionSets} fields are a {@code oneOf} of the
 * literal string {@code "none"} or an array of ids, and {@code subtract} is a structured object;
 * each is a {@link PolicyExpression} that losslessly preserves whichever form is supplied. The
 * {@code lastRev} field supports optimistic concurrency control.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdatePolicyMaskRequest {

    @JsonProperty("tags")
    private Map<String, String> tags;

    @JsonProperty("description")
    private String description;

    @JsonProperty("managedPolicies")
    private PolicyExpression managedPolicies;

    @JsonProperty("managedPermissionSets")
    private PolicyExpression managedPermissionSets;

    @JsonProperty("subtract")
    private PolicyExpression subtract;

    @JsonProperty("lastRev")
    private String lastRev;

    /**
     * Sets the user-controlled tags.
     *
     * @param tags the tags
     * @return this request for chaining
     */
    public UpdatePolicyMaskRequest tags(Map<String, String> tags) {
        this.tags = tags;
        return this;
    }

    /**
     * Sets the description of the policy mask.
     *
     * @param description the description
     * @return this request for chaining
     */
    public UpdatePolicyMaskRequest description(String description) {
        this.description = description;
        return this;
    }

    /**
     * Sets the managed policies — either {@code "none"} or an array of {@code managedpolicy:} ids.
     *
     * @param managedPolicies the managed policies as a {@link PolicyExpression}
     * @return this request for chaining
     */
    public UpdatePolicyMaskRequest managedPolicies(PolicyExpression managedPolicies) {
        this.managedPolicies = managedPolicies;
        return this;
    }

    /**
     * Sets the managed permission sets — either {@code "none"} or an array of {@code managedset:} ids.
     *
     * @param managedPermissionSets the managed permission sets as a {@link PolicyExpression}
     * @return this request for chaining
     */
    public UpdatePolicyMaskRequest managedPermissionSets(PolicyExpression managedPermissionSets) {
        this.managedPermissionSets = managedPermissionSets;
        return this;
    }

    /**
     * Sets the {@code subtract} object (carrying nested {@code managedPolicies}/
     * {@code managedPermissionSets} arrays).
     *
     * @param subtract the subtract object as a {@link PolicyExpression}
     * @return this request for chaining
     */
    public UpdatePolicyMaskRequest subtract(PolicyExpression subtract) {
        this.subtract = subtract;
        return this;
    }

    /**
     * Sets the last known revision for optimistic concurrency control.
     *
     * @param lastRev the last revision
     * @return this request for chaining
     */
    public UpdatePolicyMaskRequest lastRev(String lastRev) {
        this.lastRev = lastRev;
        return this;
    }

    public Map<String, String> getTags() {
        return tags;
    }

    public String getDescription() {
        return description;
    }

    public PolicyExpression getManagedPolicies() {
        return managedPolicies;
    }

    public PolicyExpression getManagedPermissionSets() {
        return managedPermissionSets;
    }

    public PolicyExpression getSubtract() {
        return subtract;
    }

    public String getLastRev() {
        return lastRev;
    }
}
