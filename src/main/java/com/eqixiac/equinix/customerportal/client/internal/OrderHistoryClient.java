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

package com.eqixiac.equinix.customerportal.client.internal;

import com.eqixiac.equinix.customerportal.model.OrderHistoryItem;
import com.eqixiac.equinix.customerportal.model.PermissibleLocation;
import com.eqixiac.equinix.customerportal.model.json.creators.OrderHistorySearchRequest;

import java.util.List;

public interface OrderHistoryClient {

    List<? extends OrderHistoryItem> search(OrderHistorySearchRequest request);

    List<? extends PermissibleLocation> listLocations();
}
