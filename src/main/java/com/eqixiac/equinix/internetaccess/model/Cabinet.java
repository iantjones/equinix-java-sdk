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

package com.eqixiac.equinix.internetaccess.model;

import com.eqixiac.equinix.internetaccess.model.implementation.CageRef;
import com.eqixiac.equinix.internetaccess.model.implementation.IbxLocation;
import com.eqixiac.equinix.internetaccess.model.implementation.SecureCageAccount;

/**
 * A cabinet in an Equinix IBX data center, as returned by the Equinix Internet Access (EIA) v1
 * product-availability lookup {@code GET /internetAccess/v1/cabinets}.
 *
 * <p>This is a read-only response view.</p>
 */
public interface Cabinet {

    /**
     * @return the unique space identifier of the cabinet
     */
    String getSpaceId();

    /**
     * @return the cabinet number
     */
    String getNumber();

    /**
     * @return the number of patch panels in the cabinet
     */
    Integer getPatchPanelsCount();

    /**
     * @return the cage that contains the cabinet
     */
    CageRef getCage();

    /**
     * @return the IBX location of the cabinet
     */
    IbxLocation getLocation();

    /**
     * @return the customer billing account that owns the cabinet
     */
    SecureCageAccount getAccount();
}
