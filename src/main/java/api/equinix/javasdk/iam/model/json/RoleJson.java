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

package api.equinix.javasdk.iam.model.json;

import api.equinix.javasdk.iam.model.Role;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Read-only JSON model for the IAM {@code RoleDetails} response. Implements {@link Role} directly.
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class RoleJson implements Role {

    @JsonProperty("roleId")
    private String roleId;

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("assignmentScopeTypes")
    private List<String> assignmentScopeTypes;

    @JsonProperty("permissions")
    private List<PermissionJson> permissions;

    @Override
    public List<Role.Permission> getPermissions() {
        return permissions == null ? null : List.copyOf(permissions);
    }

    /** Read-only JSON model for a single {@link Role.Permission}. */
    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PermissionJson implements Role.Permission {

        @JsonProperty("action")
        private String action;

        @JsonProperty("description")
        private String description;
    }
}
