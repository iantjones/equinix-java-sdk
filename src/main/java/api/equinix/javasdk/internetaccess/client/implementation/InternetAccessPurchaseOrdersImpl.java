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

package api.equinix.javasdk.internetaccess.client.implementation;

import api.equinix.javasdk.InternetAccess;
import api.equinix.javasdk.core.http.ResponseHandler;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.internetaccess.client.InternetAccessPurchaseOrders;
import api.equinix.javasdk.internetaccess.client.internal.PurchaseOrderClient;
import api.equinix.javasdk.internetaccess.enums.PurchaseOrderCategory;
import api.equinix.javasdk.internetaccess.model.PurchaseOrder;
import api.equinix.javasdk.internetaccess.model.json.PurchaseOrderJson;

public class InternetAccessPurchaseOrdersImpl implements InternetAccessPurchaseOrders {

    private final InternetAccess serviceManager;

    private final PurchaseOrderClient serviceClient;

    public InternetAccessPurchaseOrdersImpl(PurchaseOrderClient serviceClient, InternetAccess serviceManager) {
        this.serviceManager = serviceManager;
        this.serviceClient = serviceClient;
    }

    public PaginatedList<PurchaseOrder> list(String accountNumber) {
        return list(accountNumber, null, null);
    }

    public PaginatedList<PurchaseOrder> list(String accountNumber, String ibx, PurchaseOrderCategory category) {
        Page<PurchaseOrderJson> responsePage = this.serviceClient.list(accountNumber, ibx, category);
        return ResponseHandler.toPaginatedList(responsePage, this.serviceClient, (json, client) -> json);
    }

    public PurchaseOrder get(String accountNumber, String number) {
        return this.serviceClient.getOne(accountNumber, number);
    }
}
