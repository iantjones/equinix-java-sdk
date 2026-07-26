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
import com.eqixiac.equinix.internetaccess.client.InternetAccessOperationalUnits;
import com.eqixiac.equinix.internetaccess.client.internal.OperationalUnitClient;
import com.eqixiac.equinix.internetaccess.model.OperationalUnit;
import com.eqixiac.equinix.internetaccess.model.json.OperationalUnitJson;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class InternetAccessOperationalUnitsImpl implements InternetAccessOperationalUnits {

    private final OperationalUnitClient serviceClient;

    private final InternetAccess serviceManager;

    public PaginatedList<OperationalUnit> list(String ibx) {
        Page<OperationalUnitJson> responsePage = this.serviceClient.list(ibx);
        return ResponseHandler.toPaginatedList(responsePage, this.serviceClient, (json, client) -> json);
    }
}
