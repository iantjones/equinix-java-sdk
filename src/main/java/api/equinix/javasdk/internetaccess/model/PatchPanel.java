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

package api.equinix.javasdk.internetaccess.model;

import api.equinix.javasdk.internetaccess.enums.PatchPanelType;
import api.equinix.javasdk.internetaccess.model.implementation.CabinetRef;
import api.equinix.javasdk.internetaccess.model.implementation.CageRef;
import api.equinix.javasdk.internetaccess.model.implementation.PatchPanelOwnedPort;
import api.equinix.javasdk.internetaccess.model.implementation.SecureCageAccount;
import api.equinix.javasdk.internetaccess.model.implementation.IbxLocation;

import java.util.List;

/**
 * A patch panel in an Equinix IBX data center, as returned by the Equinix Internet Access (EIA)
 * v1 product-availability lookup {@code GET /internetAccess/v1/patchPanels}.
 *
 * <p>This is a read-only response view.</p>
 */
public interface PatchPanel {

    /**
     * @return the patch panel number
     */
    String getNumber();

    /**
     * @return the customer reference number of the patch panel
     */
    String getCustomerRefNumber();

    /**
     * @return the patch panel type
     */
    PatchPanelType getType();

    /**
     * @return whether the patch panel is prewired
     */
    Boolean getPrewired();

    /**
     * @return the number of available ports
     */
    Integer getAvailablePortsCount();

    /**
     * @return the available port numbers
     */
    List<Integer> getAvailablePorts();

    /**
     * @return the number of owned (used or reserved) ports
     */
    Integer getOwnedPortsCount();

    /**
     * @return the owned (used or reserved) ports
     */
    List<PatchPanelOwnedPort> getOwnedPorts();

    /**
     * @return the media types supported by the patch panel
     */
    List<String> getMediaTypes();

    /**
     * @return the dedicated media type of the patch panel, if any
     */
    String getDedicatedMediaType();

    /**
     * @return the cage that contains the patch panel
     */
    CageRef getCage();

    /**
     * @return the cabinet that contains the patch panel
     */
    CabinetRef getCabinet();

    /**
     * @return the IBX location of the patch panel
     */
    IbxLocation getLocation();

    /**
     * @return the customer billing account that owns the patch panel
     */
    SecureCageAccount getAccount();
}
