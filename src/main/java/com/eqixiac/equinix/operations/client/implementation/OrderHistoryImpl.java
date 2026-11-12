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

package com.eqixiac.equinix.operations.client.implementation;

import com.eqixiac.equinix.Operations;
import com.eqixiac.equinix.operations.client.OrderHistory;
import com.eqixiac.equinix.operations.client.internal.OrderHistoryClient;
import com.eqixiac.equinix.operations.model.OrderHistoryItem;
import com.eqixiac.equinix.operations.model.OrderHistoryPage;
import com.eqixiac.equinix.operations.model.PermissibleLocation;
import com.eqixiac.equinix.operations.model.json.creators.OrderHistorySearchRequest;

import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class OrderHistoryImpl implements OrderHistory {

    private final OrderHistoryClient serviceClient;

    private final Operations serviceManager;

    public List<? extends OrderHistoryItem> search(OrderHistorySearchRequest request) {
        return this.serviceClient.searchPage(request).getContent();
    }

    public OrderHistoryPage searchPage(OrderHistorySearchRequest request) {
        return this.serviceClient.searchPage(request);
    }

    public List<? extends PermissibleLocation> listLocations() {
        return this.serviceClient.listLocations();
    }
}
