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

import java.util.List;
import java.util.Map;

/**
 * Request body for updating an IAM access policy via {@code PUT
 * /v1/projects/{projectId}/accessPolicies/{accessPolicyId}} (operationId {@code updateAccessPolicy},
 * spec schema {@code UpdateAccessPolicyBody}).
 *
 * <p>The {@code permissions}, {@code intersect} and {@code subtract} fields carry the polymorphic
 * {@code UserRectSet}/{@code InlinePermission} entries and are therefore typed loosely as
 * {@code List<Object>}; callers may pass inline-permission maps, permission-set id strings, ERNs
 * or foreign-access-policy reference maps. The {@code lastRev} field supports optimistic
 * concurrency control.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdateAccessPolicyRequest {

    @JsonProperty("tags")
    private Map<String, String> tags;

    @JsonProperty("description")
    private String description;

    @JsonProperty("permissions")
    private List<Object> permissions;

    @JsonProperty("intersect")
    private List<Object> intersect;

    @JsonProperty("subtract")
    private List<Object> subtract;

    @JsonProperty("allowBadRefs")
    private String allowBadRefs;

    @JsonProperty("lastRev")
    private String lastRev;

    /**
     * Sets the user-controlled tags.
     *
     * @param tags the tags
     * @return this request for chaining
     */
    public UpdateAccessPolicyRequest tags(Map<String, String> tags) {
        this.tags = tags;
        return this;
    }

    /**
     * Sets the description of the access policy.
     *
     * @param description the description
     * @return this request for chaining
     */
    public UpdateAccessPolicyRequest description(String description) {
        this.description = description;
        return this;
    }

    /**
     * Sets the permission set entries — polymorphic {@code UserRectSet} entries.
     *
     * @param permissions the permission entries
     * @return this request for chaining
     */
    public UpdateAccessPolicyRequest permissions(List<Object> permissions) {
        this.permissions = permissions;
        return this;
    }

    /**
     * Sets the {@code intersect} permission entries.
     *
     * @param intersect the intersect entries
     * @return this request for chaining
     */
    public UpdateAccessPolicyRequest intersect(List<Object> intersect) {
        this.intersect = intersect;
        return this;
    }

    /**
     * Sets the {@code subtract} permission entries.
     *
     * @param subtract the subtract entries
     * @return this request for chaining
     */
    public UpdateAccessPolicyRequest subtract(List<Object> subtract) {
        this.subtract = subtract;
        return this;
    }

    /**
     * Sets the {@code allowBadRefs} flag ({@code additional}) to permit invalid references.
     *
     * @param allowBadRefs the flag value
     * @return this request for chaining
     */
    public UpdateAccessPolicyRequest allowBadRefs(String allowBadRefs) {
        this.allowBadRefs = allowBadRefs;
        return this;
    }

    /**
     * Sets the last known revision for optimistic concurrency control.
     *
     * @param lastRev the last revision
     * @return this request for chaining
     */
    public UpdateAccessPolicyRequest lastRev(String lastRev) {
        this.lastRev = lastRev;
        return this;
    }

    public Map<String, String> getTags() {
        return tags;
    }

    public String getDescription() {
        return description;
    }

    public List<Object> getPermissions() {
        return permissions;
    }

    public List<Object> getIntersect() {
        return intersect;
    }

    public List<Object> getSubtract() {
        return subtract;
    }

    public String getAllowBadRefs() {
        return allowBadRefs;
    }

    public String getLastRev() {
        return lastRev;
    }
}
