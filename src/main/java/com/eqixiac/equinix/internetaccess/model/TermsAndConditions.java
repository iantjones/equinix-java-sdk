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

import com.eqixiac.equinix.internetaccess.enums.TermsProduct;
import com.eqixiac.equinix.internetaccess.enums.TermsType;
import com.eqixiac.equinix.internetaccess.model.implementation.TermsAccount;
import com.eqixiac.equinix.internetaccess.model.implementation.TermsConnectivitySource;
import com.eqixiac.equinix.internetaccess.model.implementation.IbxLocation;

/**
 * A set of Equinix Internet Access (EIA) terms and conditions, as returned by the v1 terms lookup
 * ({@code GET /internetAccess/v1/terms}).
 *
 * <p>This is a read-only response view.</p>
 */
public interface TermsAndConditions {

    /**
     * @return the body text of the terms and conditions
     */
    String getText();

    /**
     * @return the version of the terms and conditions
     */
    String getVersion();

    /**
     * @return the location the terms and conditions apply to
     */
    IbxLocation getLocation();

    /**
     * @return the account the terms and conditions apply to
     */
    TermsAccount getAccount();

    /**
     * @return the connectivity source the terms and conditions apply to
     */
    TermsConnectivitySource getConnectivitySource();

    /**
     * @return the category of the terms and conditions
     */
    TermsType getType();

    /**
     * @return the Equinix product the terms and conditions apply to
     */
    TermsProduct getProduct();

    /**
     * @return the language of the terms and conditions
     */
    String getLanguage();
}
