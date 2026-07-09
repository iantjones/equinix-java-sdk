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

import java.util.List;
import java.util.Map;
import lombok.Getter;

/**
 * Request body for updating an IAM permission set via {@code PUT
 * /v1/projects/{projectId}/permissionSets/{permissionSetId}} (operationId
 * {@code updatePermissionSet}, spec schema {@code UpdatePermissionSetBody}).
 *
 * <p>The {@code permissions}, {@code intersect} and {@code subtract} fields carry the polymorphic
 * {@code UserRectSet}/{@code InlinePermission} entries; each entry is a {@link PolicyExpression}
 * that losslessly represents whichever {@code oneOf} form is supplied — a bare permission-set id /
 * ERN string or a structured inline-permission / reference object. The {@code lastRev} field
 * supports optimistic concurrency control.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
public class UpdatePermissionSetRequest {

    @JsonProperty("tags")
    private Map<String, String> tags;

    @JsonProperty("description")
    private String description;

    @JsonProperty("permissions")
    private List<PolicyExpression> permissions;

    @JsonProperty("intersect")
    private List<PolicyExpression> intersect;

    @JsonProperty("subtract")
    private List<PolicyExpression> subtract;

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
    public UpdatePermissionSetRequest tags(Map<String, String> tags) {
        this.tags = tags;
        return this;
    }

    /**
     * Sets the description of the permission set.
     *
     * @param description the description
     * @return this request for chaining
     */
    public UpdatePermissionSetRequest description(String description) {
        this.description = description;
        return this;
    }

    /**
     * Sets the permission entries — polymorphic {@code UserRectSet} entries.
     *
     * @param permissions the permission entries
     * @return this request for chaining
     */
    public UpdatePermissionSetRequest permissions(List<PolicyExpression> permissions) {
        this.permissions = permissions;
        return this;
    }

    /**
     * Sets the {@code intersect} permission entries.
     *
     * @param intersect the intersect entries
     * @return this request for chaining
     */
    public UpdatePermissionSetRequest intersect(List<PolicyExpression> intersect) {
        this.intersect = intersect;
        return this;
    }

    /**
     * Sets the {@code subtract} permission entries.
     *
     * @param subtract the subtract entries
     * @return this request for chaining
     */
    public UpdatePermissionSetRequest subtract(List<PolicyExpression> subtract) {
        this.subtract = subtract;
        return this;
    }

    /**
     * Sets the {@code allowBadRefs} flag ({@code additional}) to permit invalid references.
     *
     * @param allowBadRefs the flag value
     * @return this request for chaining
     */
    public UpdatePermissionSetRequest allowBadRefs(String allowBadRefs) {
        this.allowBadRefs = allowBadRefs;
        return this;
    }

    /**
     * Sets the last known revision for optimistic concurrency control.
     *
     * @param lastRev the last revision
     * @return this request for chaining
     */
    public UpdatePermissionSetRequest lastRev(String lastRev) {
        this.lastRev = lastRev;
        return this;
    }
}
