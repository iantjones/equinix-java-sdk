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

/**
 * The association between an action and a resource type, as returned by the IAM
 * resource-type-action operations.
 *
 * <p>This is a read-only response view (spec schema {@code ResourceTypeAction}).</p>
 */
public interface ResourceTypeAction {

    /**
     * @return the action
     */
    String getAction();

    /**
     * @return the resource type
     */
    String getResourceType();

    /**
     * @return the Equinix Resource Name (ERN) of the resource type
     */
    String getResourceTypeErn();

    /**
     * @return the opaque revision of the association (used as {@code lastRev})
     */
    String getRev();

    /**
     * @return the principal that created the association
     */
    String getCreatedBy();

    /**
     * @return the creation timestamp
     */
    String getCreatedAt();
}
