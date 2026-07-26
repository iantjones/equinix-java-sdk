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

package com.eqixiac.equinix.internetaccess.client.internal.implementation;

import com.eqixiac.equinix.core.client.ResourceClientBase;
import com.eqixiac.equinix.core.enums.RequestType;
import com.eqixiac.equinix.core.http.ResponseHandler;
import com.eqixiac.equinix.core.http.request.EquinixRequest;
import com.eqixiac.equinix.core.http.response.Page;
import com.eqixiac.equinix.internetaccess.client.implementation.InternetAccessConfigImpl;
import com.eqixiac.equinix.internetaccess.client.internal.PurchaseOrderClient;
import com.eqixiac.equinix.internetaccess.enums.PurchaseOrderCategory;
import com.eqixiac.equinix.internetaccess.model.PurchaseOrder;
import com.eqixiac.equinix.internetaccess.model.json.PurchaseOrderJson;

import java.util.Map;

/**
 * Internal client implementation for the Equinix Internet Access (EIA) v1 purchase-order lookups
 * {@code GET /internetAccess/v1/accounts/{accountNumber}/purchaseOrders} (list) and
 * {@code GET /internetAccess/v1/accounts/{accountNumber}/purchaseOrders/{number}} (single get). The
 * {@code PurchaseOrder} response is read-only, so the deserialized {@link PurchaseOrderJson} (which
 * implements {@link PurchaseOrder} directly) is returned without a wrapper.
 */
public class PurchaseOrderClientImpl extends ResourceClientBase<PurchaseOrder, PurchaseOrderJson> implements PurchaseOrderClient {

    public PurchaseOrderClientImpl(InternetAccessConfigImpl configClient) {
        super(configClient, "InternetAccess", "PurchaseOrdersV1", PurchaseOrderJson.class);
    }

    @Override
    protected PurchaseOrder wrap(PurchaseOrderJson json) {
        return json;
    }

    public Page<PurchaseOrderJson> list(String accountNumber, String ibx, PurchaseOrderCategory category) {
        EquinixRequest<PurchaseOrder> request = buildRequestWithPathParams("ListPurchaseOrders", RequestType.PAGINATED,
                Map.of("accountNumber", accountNumber), PurchaseOrderJson.class);
        if (ibx != null) {
            request.addSingleQueryParameter("locations.ibx", ibx);
        }
        if (category != null) {
            request.addSingleQueryParameter("category", category.toString());
        }
        return ResponseHandler.handlePaginatedListResponse(invoke(request), request);
    }

    public PurchaseOrderJson getOne(String accountNumber, String number) {
        return getOne("GetPurchaseOrder", Map.of("accountNumber", accountNumber, "number", number));
    }
}
