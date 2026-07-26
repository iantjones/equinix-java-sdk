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

package com.eqixiac.equinix.networkedge.client.internal.implementation;

import com.eqixiac.equinix.core.client.ClientBase;
import com.eqixiac.equinix.core.enums.RequestType;
import com.eqixiac.equinix.core.http.ParameterMapper;
import com.eqixiac.equinix.networkedge.client.implementation.NetworkEdgeConfigImpl;
import com.eqixiac.equinix.networkedge.client.internal.AgreementClient;
import com.eqixiac.equinix.networkedge.enums.LicenseType;
import com.eqixiac.equinix.networkedge.model.implementation.AgreementStatus;

import java.util.List;
import java.util.Map;

/**
 *
 * @author ianjones
 */
public class AgreementClientImpl extends ClientBase implements AgreementClient {

    public AgreementClientImpl(NetworkEdgeConfigImpl configClient) {
        super(configClient, "NetworkEdge", "Agreements");
    }

    public AgreementStatus getAgreementStatus(String accountNumber) {
        return getAs("GetAgreementStatus", null, ParameterMapper.singleParamMap("accountNumber", accountNumber), AgreementStatus.class);
    }

    public AgreementStatus createAgreement(String accountNumber, String termsVersionId) {
        Map<String, String> requestBody = ParameterMapper.concatStringMaps(ParameterMapper.singlePropertyBody("accountNumber", accountNumber),
                                                                 ParameterMapper.singlePropertyBody("apttusId", termsVersionId));
        postAs("CreateAgreement", requestBody, AgreementStatus.class);
        return getAgreementStatus(accountNumber);
    }

    public String getVendorsTerms(String vendorPackage, LicenseType licenseType) {
        // The licenseType query parameter expects Subscription / BYOL (see LicenseType.getQueryValue),
        // not the SUB / BYOL body form.
        Map<String, List<String>> qParams = Map.of("vendorPackage", ParameterMapper.singleParamList(vendorPackage),
                "licenseType", ParameterMapper.singleParamList(licenseType != null ? licenseType.getQueryValue() : null));
        return mapOp("GetVendorTerms", RequestType.SINGLE, null, qParams, null).get("terms");
    }

    public String getOrderTerms() {
        return mapOp("GetOrderTerms", RequestType.SINGLE, null, null, null).get("terms");
    }
}
