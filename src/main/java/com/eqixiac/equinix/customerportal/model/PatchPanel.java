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

package com.eqixiac.equinix.customerportal.model;

import com.eqixiac.equinix.customerportal.enums.InstallLocation;
import com.eqixiac.equinix.customerportal.enums.PatchPanelType;
import com.eqixiac.equinix.customerportal.enums.ProvisioningType;
import com.eqixiac.equinix.customerportal.model.implementation.UserPortDetails;

import java.util.List;

/**
 * A patch panel available for cross-connect ordering. The list endpoint
 * ({@code ListPatchPanels}) populates the summary fields; the detail endpoint
 * ({@code GetPatchPanel}) additionally populates the cage/cabinet, port and connection-service
 * details.
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
     * Returns whether the patch panel supports only intra-facility connections.
     *
     * @return {@code true} if intra-facility only, or {@code null} if not provided
     */
    Boolean getIfcEnabled();

    /**
     * Returns the provisioning type ({@code REGULAR} or {@code FAST_PROVISIONING}).
     *
     * @return the provisioning type, or {@code null} if not provided
     */
    ProvisioningType getProvisioningType();

    /**
     * Returns the IBX location code.
     *
     * @return the IBX code, or {@code null} if not provided
     */
    String getIbx();

    /**
     * Returns the cage id.
     *
     * @return the cage id, or {@code null} if not provided
     */
    String getCageId();

    /**
     * Returns the cabinet id.
     *
     * @return the cabinet id, or {@code null} if not provided
     */
    String getCabinetId();

    /**
     * Returns the customer cage account number.
     *
     * @return the account number, or {@code null} if not provided
     */
    String getAccountNumber();

    /**
     * Returns the customer cage account name.
     *
     * @return the account name, or {@code null} if not provided
     */
    String getAccountName();

    /**
     * Returns the dedicated media type.
     *
     * @return the dedicated media type, or {@code null} if not provided
     */
    String getDedicatedMediaType();

    /**
     * Returns whether cross-connects are prewired.
     *
     * @return {@code true} if prewired, or {@code null} if not provided
     */
    Boolean getPreWired();

    /**
     * Returns the patch panel type.
     *
     * @return the patch panel type, or {@code null} if not provided
     */
    PatchPanelType getType();

    /**
     * Returns the rack location of the patch panel.
     *
     * @return the rack location, or {@code null} if not provided
     */
    String getRackLocations();

    /**
     * Returns the location of the installed patch panel.
     *
     * @return the install location, or {@code null} if not provided
     */
    InstallLocation getInstallLocations();

    /**
     * Returns whether Equinix installation is required.
     *
     * @return {@code true} if installation is required, or {@code null} if not provided
     */
    Boolean getInstallationRequired();

    /**
     * Returns whether circuits are available for the patch panel.
     *
     * @return {@code true} if circuits are available, or {@code null} if not provided
     */
    Boolean getCircuitAvailable();

    /**
     * Returns the individually identified available ports.
     *
     * @return the available ports, or {@code null} if not provided
     */
    List<Integer> getAvailablePorts();

    /**
     * Returns the connection services available on the patch panel.
     *
     * @return the connection services, or {@code null} if not provided
     */
    List<? extends ConnectionService> getConnectionServices();

    /**
     * Returns the details of ports currently in use.
     *
     * @return the used port details, or {@code null} if not provided
     */
    List<UserPortDetails> getUsedPortsDetails();
}
