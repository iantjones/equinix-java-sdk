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

package api.equinix.javasdk.networkedge.client.internal.implementation;

import api.equinix.javasdk.core.client.ClientBase;
import api.equinix.javasdk.core.enums.RequestType;
import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.networkedge.client.implementation.NetworkEdgeConfigImpl;
import api.equinix.javasdk.networkedge.client.internal.AgreementClient;
import api.equinix.javasdk.networkedge.enums.LicenseType;
import api.equinix.javasdk.networkedge.model.implementation.AgreementStatus;

import java.util.List;
import java.util.Map;

/**
 * <p>AgreementClientImpl class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class AgreementClientImpl extends ClientBase implements AgreementClient {

    public AgreementClientImpl(NetworkEdgeConfigImpl configClient) {
        super(configClient, "NetworkEdge", "Agreements");
    }

    /** {@inheritDoc} */
    public AgreementStatus getAgreementStatus(String accountNumber) {
        return getAs("GetAgreementStatus", null, Utils.singleParamMap("account_number", accountNumber), AgreementStatus.class);
    }

    /** {@inheritDoc} */
    public AgreementStatus createAgreement(String accountNumber, String termsVersionId) {
        Map<String, String> requestBody = Utils.concatStringMaps(Utils.singlePropertyBody("accountNumber", accountNumber),
                                                                 Utils.singlePropertyBody("apttusId", termsVersionId));
        postAs("GetVPN", requestBody, AgreementStatus.class);
        return getAgreementStatus(accountNumber);
    }

    /** {@inheritDoc} */
    public String getVendorsTerms(String vendorPackage, LicenseType licenseType) {
        Map<String, List<String>> qParams = Map.of("vendorPackage", Utils.singleParamList(vendorPackage), "licenseType", Utils.singleParamList(licenseType));
        return mapOp("GetVendorTerms", RequestType.SINGLE, null, qParams, null).get("terms");
    }

    /** {@inheritDoc} */
    public String getOrderTerms() {
        return mapOp("GetOrderTerms", RequestType.SINGLE, null, null, null).get("terms");
    }
}
