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
import lombok.Getter;

/**
 * Request body for creating an IAM policy mask via {@code POST
 * /v1/projects/{projectId}/policyMasks} (operationId {@code createPolicyMask}, spec schema
 * {@code CreatePolicyMaskBody}).
 *
 * <p>The {@code managedPolicies}/{@code managedPermissionSets} fields are a {@code oneOf} of the
 * literal string {@code "none"} or an array of ids, and {@code subtract} is a structured object;
 * each is a {@link PolicyExpression} that losslessly preserves whichever form is supplied — use
 * {@link PolicyExpression#of(String)} for {@code "none"},
 * {@link PolicyExpression#ofStrings(java.util.List)} for an id array, or
 * {@link PolicyExpression#of(com.fasterxml.jackson.databind.JsonNode)} for the {@code subtract}
 * object.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
public class CreatePolicyMaskRequest {

    @JsonProperty("policyMaskId")
    private String policyMaskId;

    @JsonProperty("description")
    private String description;

    @JsonProperty("tags")
    private Map<String, String> tags;

    @JsonProperty("managedPolicies")
    private PolicyExpression managedPolicies;

    @JsonProperty("managedPermissionSets")
    private PolicyExpression managedPermissionSets;

    @JsonProperty("subtract")
    private PolicyExpression subtract;

    /**
     * Sets the policy mask identifier (required), e.g. {@code policymask:my-mask}.
     *
     * @param policyMaskId the policy mask id
     * @return this request for chaining
     */
    public CreatePolicyMaskRequest policyMaskId(String policyMaskId) {
        this.policyMaskId = policyMaskId;
        return this;
    }

    /**
     * Sets the description of the policy mask.
     *
     * @param description the description
     * @return this request for chaining
     */
    public CreatePolicyMaskRequest description(String description) {
        this.description = description;
        return this;
    }

    /**
     * Sets the user-controlled tags.
     *
     * @param tags the tags
     * @return this request for chaining
     */
    public CreatePolicyMaskRequest tags(Map<String, String> tags) {
        this.tags = tags;
        return this;
    }

    /**
     * Sets the managed policies — either {@code "none"} or an array of {@code managedpolicy:} ids.
     *
     * @param managedPolicies the managed policies as a {@link PolicyExpression}
     * @return this request for chaining
     */
    public CreatePolicyMaskRequest managedPolicies(PolicyExpression managedPolicies) {
        this.managedPolicies = managedPolicies;
        return this;
    }

    /**
     * Sets the managed permission sets — either {@code "none"} or an array of {@code managedset:} ids.
     *
     * @param managedPermissionSets the managed permission sets as a {@link PolicyExpression}
     * @return this request for chaining
     */
    public CreatePolicyMaskRequest managedPermissionSets(PolicyExpression managedPermissionSets) {
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
    public CreatePolicyMaskRequest subtract(PolicyExpression subtract) {
        this.subtract = subtract;
        return this;
    }
}
