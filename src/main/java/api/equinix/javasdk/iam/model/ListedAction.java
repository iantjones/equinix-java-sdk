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

package api.equinix.javasdk.iam.model;

import api.equinix.javasdk.iam.enums.ServiceAspect;

import java.util.List;
import java.util.Map;

/**
 * An action available within a service, as returned by the IAM list-actions operation.
 *
 * <p>This is a read-only response view (spec schema {@code ListedAction}).</p>
 */
public interface ListedAction {

    /**
     * @return the action identifier
     */
    String getActionId();

    /**
     * @return the service aspect the action belongs to
     */
    ServiceAspect getServiceAspect();

    /**
     * @return the user-controlled tags on the action
     */
    Map<String, String> getTags();

    /**
     * @return the RBAC permission associated with the action (may be {@code null})
     */
    RbacPermission getRbacPermission();

    /**
     * @return the permission codes associated with the action, keyed by permission code (may be {@code null})
     */
    Map<String, PermissionCode> getPermissionCodes();

    /**
     * @return the attributes included on this action (may be {@code null})
     */
    List<Attribute> getAttributes();

    /**
     * The action mapping to access-management permissions, used when performing authorization via
     * role-assignment token scope (spec schema {@code RBACPermission}).
     */
    interface RbacPermission {

        /**
         * @return the permission name (e.g. {@code fabric.port.read})
         */
        String getPermission();

        /**
         * @return the resource type the permission applies to (e.g. {@code PROJECT})
         */
        String getPermissionResourceType();
    }

    interface PermissionCode {

        /**
         * @return {@code true} when the action is only allowed if the permission code is granted
         *         unconditionally at the top level of the asset hierarchy
         */
        Boolean getRequiresAll();
    }
}
