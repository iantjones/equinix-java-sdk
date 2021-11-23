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

package api.equinix.javasdk.networkedge.client.internal;

import api.equinix.javasdk.networkedge.enums.LicenseType;
import api.equinix.javasdk.networkedge.model.implementation.AgreementStatus;

/**
 * <p>AgreementClient interface.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public interface AgreementClient {

    /**
     * <p>getAgreementStatus.</p>
     *
     * @param accountNumber a {@link java.lang.String} object.
     * @return a {@link api.equinix.javasdk.networkedge.model.implementation.AgreementStatus} object.
     */
    AgreementStatus getAgreementStatus(String accountNumber);

    /**
     * <p>createAgreement.</p>
     *
     * @param accountNumber a {@link java.lang.String} object.
     * @param termsVersionId a {@link java.lang.String} object.
     * @return a {@link api.equinix.javasdk.networkedge.model.implementation.AgreementStatus} object.
     */
    AgreementStatus createAgreement(String accountNumber, String termsVersionId);

    /**
     * <p>getVendorsTerms.</p>
     *
     * @param vendorPackage a {@link java.lang.String} object.
     * @param licenseType a {@link api.equinix.javasdk.networkedge.enums.LicenseType} object.
     * @return a {@link java.lang.String} object.
     */
    String getVendorsTerms(String vendorPackage, LicenseType licenseType);

    /**
     * <p>getOrderTerms.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    String getOrderTerms();
}
