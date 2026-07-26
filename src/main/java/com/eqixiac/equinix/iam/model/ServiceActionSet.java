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
 * A named set of actions grouped within a service, as returned by the IAM
 * service-action-set operations.
 *
 * <p>This is a read-only response view (spec schema {@code ServiceActionSetNoErn}).</p>
 */
public interface ServiceActionSet {

    /**
     * @return the action set identifier
     */
    String getActionSetId();

    /**
     * @return the service the action set belongs to
     */
    String getServiceId();

    /**
     * @return the description of the action set (may be {@code null})
     */
    String getDescription();

    /**
     * @return the user-controlled tags on the action set
     */
    Map<String, String> getTags();

    /**
     * @return the actions contained in the action set
     */
    List<String> getActionSet();

    /**
     * @return the opaque revision of the action set (used as {@code lastRev})
     */
    String getRev();

    /**
     * @return the principal that created the action set
     */
    String getCreatedBy();

    /**
     * @return the creation timestamp
     */
    String getCreatedAt();

    /**
     * @return the principal that last updated the action set
     */
    String getUpdatedBy();

    /**
     * @return the last-updated timestamp
     */
    String getUpdatedAt();

    /**
     * @return the principal that approved the action set
     */
    String getApprovedBy();

    /**
     * @return the approval timestamp
     */
    String getApprovedAt();
}
