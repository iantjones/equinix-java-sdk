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

import api.equinix.javasdk.iam.enums.ServiceAspect;
import api.equinix.javasdk.iam.model.Attribute;
import api.equinix.javasdk.iam.model.ListedAction;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * Read-only JSON model for the IAM {@code ListedAction} response. Implements
 * {@link ListedAction} directly.
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ListedActionJson implements ListedAction {

    @JsonProperty("actionId")
    private String actionId;

    @JsonProperty("serviceAspect")
    private ServiceAspect serviceAspect;

    @JsonProperty("tags")
    private Map<String, String> tags;

    @JsonProperty("rbacPermission")
    private RbacPermissionJson rbacPermission;

    @JsonProperty("permissionCodes")
    private Map<String, PermissionCodeJson> permissionCodes;

    @JsonProperty("attributes")
    private List<AttributeJson> attributes;

    @Override
    public List<Attribute> getAttributes() {
        if (attributes == null) {
            return null;
        }
        return java.util.Collections.unmodifiableList(attributes);
    }

    @Override
    public ListedAction.RbacPermission getRbacPermission() {
        return rbacPermission;
    }

    @Override
    public Map<String, ListedAction.PermissionCode> getPermissionCodes() {
        if (permissionCodes == null) {
            return null;
        }
        Map<String, ListedAction.PermissionCode> result = new java.util.LinkedHashMap<>(permissionCodes);
        return java.util.Collections.unmodifiableMap(result);
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RbacPermissionJson implements ListedAction.RbacPermission {

        @JsonProperty("permission")
        private String permission;

        @JsonProperty("permissionResourceType")
        private String permissionResourceType;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PermissionCodeJson implements ListedAction.PermissionCode {

        @JsonProperty("requiresAll")
        private Boolean requiresAll;
    }
}
