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

package api.equinix.javasdk.internetaccess.client.internal;

import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.Pageable;
import api.equinix.javasdk.internetaccess.enums.PurchaseOrderCategory;
import api.equinix.javasdk.internetaccess.model.PurchaseOrder;
import api.equinix.javasdk.internetaccess.model.json.PurchaseOrderJson;

/**
 * Internal client for the Equinix Internet Access (EIA) v1 purchase-order lookups:
 * {@code GET /internetAccess/v1/accounts/{accountNumber}/purchaseOrders} (list) and
 * {@code GET /internetAccess/v1/accounts/{accountNumber}/purchaseOrders/{number}} (single get).
 */
public interface PurchaseOrderClient extends Pageable<PurchaseOrder> {

    Page<PurchaseOrderJson> list(String accountNumber, String ibx, PurchaseOrderCategory category);

    PurchaseOrderJson getOne(String accountNumber, String number);
}
