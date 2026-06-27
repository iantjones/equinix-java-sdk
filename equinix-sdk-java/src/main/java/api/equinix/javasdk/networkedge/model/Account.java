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

package api.equinix.javasdk.networkedge.model;

import api.equinix.javasdk.core.enums.MetroCode;

import java.util.ArrayList;

/**
 * <p>Account interface.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public interface Account {

    /**
     * <p>getAccountName.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    String getAccountName();

    /**
     * <p>getAccountNumber.</p>
     *
     * @return a {@link java.lang.Integer} object.
     */
    Integer getAccountNumber();

    /**
     * <p>getAccountUcmId.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    String getAccountUcmId();

    /**
     * <p>getAccountStatus.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    String getAccountStatus();

    /**
     * <p>getPortalOrgId.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    String getPortalOrgId();

    /**
     * <p>getPortalOrgName.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    String getPortalOrgName();

    /**
     * <p>getReferenceId.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    String getReferenceId();

    /**
     * <p>getMetros.</p>
     *
     * @return a {@link java.util.ArrayList} object.
     */
    ArrayList<MetroCode> getMetros();

    /**
     * <p>getSiblingCustOrgFlag.</p>
     *
     * @return a {@link java.lang.Boolean} object.
     */
    Boolean getSiblingCustOrgFlag();

    /**
     * <p>getCreditHold.</p>
     *
     * @return a {@link java.lang.Boolean} object.
     */
    Boolean getCreditHold();
}
