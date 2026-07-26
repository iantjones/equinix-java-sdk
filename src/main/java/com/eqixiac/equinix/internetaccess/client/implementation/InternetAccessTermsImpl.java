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

package com.eqixiac.equinix.internetaccess.client.implementation;

import com.eqixiac.equinix.InternetAccess;
import com.eqixiac.equinix.core.http.ResponseHandler;
import com.eqixiac.equinix.core.http.response.Page;
import com.eqixiac.equinix.core.http.response.PaginatedList;
import com.eqixiac.equinix.internetaccess.client.InternetAccessTerms;
import com.eqixiac.equinix.internetaccess.client.internal.TermsClient;
import com.eqixiac.equinix.internetaccess.enums.TermsProduct;
import com.eqixiac.equinix.internetaccess.enums.TermsType;
import com.eqixiac.equinix.internetaccess.model.TermsAndConditions;
import com.eqixiac.equinix.internetaccess.model.json.TermsAndConditionsJson;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class InternetAccessTermsImpl implements InternetAccessTerms {

    private final TermsClient serviceClient;

    private final InternetAccess serviceManager;

    public PaginatedList<TermsAndConditions> list(String accountNumber, String ibx, TermsProduct product) {
        return list(accountNumber, ibx, product, null, null);
    }

    public PaginatedList<TermsAndConditions> list(String accountNumber, String ibx, TermsProduct product, TermsType type, String language) {
        Page<TermsAndConditionsJson> responsePage = this.serviceClient.list(accountNumber, ibx, product, type, language);
        return ResponseHandler.toPaginatedList(responsePage, this.serviceClient, (json, client) -> json);
    }
}
