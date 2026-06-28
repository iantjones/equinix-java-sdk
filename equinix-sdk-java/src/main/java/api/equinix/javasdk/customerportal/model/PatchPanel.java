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

package api.equinix.javasdk.customerportal.model;

/**
 * A patch panel available for cross-connect ordering.
 */
public interface PatchPanel {

    /**
     * Returns the patch panel id.
     *
     * @return the patch panel id
     */
    String getPatchPanelId();

    /**
     * Returns the number of available ports.
     *
     * @return the available port count, or {@code null} if not provided
     */
    Integer getAvailablePortCount();

    /**
     * Returns the patch panel reference id.
     *
     * @return the patch panel reference id, or {@code null} if not provided
     */
    String getPatchPanelReferenceId();

    /**
     * Returns the provisioning type ({@code REGULAR} or {@code FAST_PROVISIONING}).
     *
     * @return the provisioning type, or {@code null} if not provided
     */
    String getProvisioningType();
}
