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
    String getServiceAspect();

    /**
     * @return the user-controlled tags on the action
     */
    Map<String, String> getTags();

    /**
     * @return the RBAC permission associated with the action, as raw deserialized JSON
     */
    Object getRbacPermission();

    /**
     * @return the permission codes associated with the action, as raw deserialized JSON map
     */
    Object getPermissionCodes();

    /**
     * @return the action attributes, as raw deserialized JSON
     */
    List<Object> getAttributes();
}
