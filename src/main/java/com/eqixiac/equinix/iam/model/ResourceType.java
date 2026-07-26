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

package com.eqixiac.equinix.iam.model;

import java.util.List;
import java.util.Map;

/**
 * A registered resource type within the IAM access model, as returned by the IAM
 * resource-type operations.
 *
 * <p>This is a read-only response view (spec schema {@code ResourceType}).</p>
 */
public interface ResourceType {

    /**
     * @return the resource type name
     */
    String getResourceType();

    /**
     * @return the Equinix Resource Name (ERN) of the resource type
     */
    String getErn();

    /**
     * @return the user-controlled tags on the resource type
     */
    Map<String, String> getTags();

    /**
     * @return the attributes included on this resource type (may be {@code null})
     */
    List<Attribute> getAttributes();

    /**
     * @return the opaque revision of the resource type (used as {@code lastRev})
     */
    String getRev();

    /**
     * @return the principal that created the resource type
     */
    String getCreatedBy();

    /**
     * @return the creation timestamp
     */
    String getCreatedAt();

    /**
     * @return the principal that last updated the resource type
     */
    String getUpdatedBy();

    /**
     * @return the last-updated timestamp
     */
    String getUpdatedAt();

    /**
     * @return the principal that approved the resource type
     */
    String getApprovedBy();

    /**
     * @return the approval timestamp
     */
    String getApprovedAt();
}
