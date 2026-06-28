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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * Request body for updating an IAM policy mask via {@code PUT
 * /v1/projects/{projectId}/policyMasks/{policyMaskId}} (operationId {@code updatePolicyMask}, spec
 * schema {@code UpdatePolicyMaskBody}).
 *
 * <p>The {@code managedPolicies}, {@code managedPermissionSets} and {@code subtract} fields are
 * {@code oneOf} scalar-or-array references and are therefore typed loosely as {@code Object};
 * callers may pass a single id string or a list of id strings. The {@code lastRev} field supports
 * optimistic concurrency control.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdatePolicyMaskRequest {

    @JsonProperty("tags")
    private Map<String, String> tags;

    @JsonProperty("description")
    private String description;

    @JsonProperty("managedPolicies")
    private Object managedPolicies;

    @JsonProperty("managedPermissionSets")
    private Object managedPermissionSets;

    @JsonProperty("subtract")
    private Object subtract;

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
     * Sets the managed policies ({@code oneOf} scalar-or-array reference).
     *
     * @param managedPolicies the managed policies (id string or list of id strings)
     * @return this request for chaining
     */
    public UpdatePolicyMaskRequest managedPolicies(Object managedPolicies) {
        this.managedPolicies = managedPolicies;
        return this;
    }

    /**
     * Sets the managed permission sets ({@code oneOf} scalar-or-array reference).
     *
     * @param managedPermissionSets the managed permission sets (id string or list of id strings)
     * @return this request for chaining
     */
    public UpdatePolicyMaskRequest managedPermissionSets(Object managedPermissionSets) {
        this.managedPermissionSets = managedPermissionSets;
        return this;
    }

    /**
     * Sets the {@code subtract} references ({@code oneOf} scalar-or-array reference).
     *
     * @param subtract the subtract references (id string or list of id strings)
     * @return this request for chaining
     */
    public UpdatePolicyMaskRequest subtract(Object subtract) {
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

    public Object getManagedPolicies() {
        return managedPolicies;
    }

    public Object getManagedPermissionSets() {
        return managedPermissionSets;
    }

    public Object getSubtract() {
        return subtract;
    }

    public String getLastRev() {
        return lastRev;
    }
}
