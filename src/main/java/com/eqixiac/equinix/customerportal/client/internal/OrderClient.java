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

import com.eqixiac.equinix.core.http.response.Pageable;
import com.eqixiac.equinix.customerportal.model.OrderNegotiation;
import com.eqixiac.equinix.customerportal.model.json.OrderJson;
import com.eqixiac.equinix.customerportal.model.json.creators.CancelRequestJson;
import com.eqixiac.equinix.customerportal.model.json.creators.NegotiationsRequestJson;
import com.eqixiac.equinix.customerportal.model.json.creators.NoteRequestJson;

import java.util.List;

public interface OrderClient<T> extends Pageable<T> {

    OrderJson getByUuid(String orderId);

    OrderJson getByUuid(String orderId, List<String> ibxs);

    OrderJson refresh(String orderId);

    List<? extends OrderNegotiation> getNegotiations(String orderId);

    Boolean replyNegotiation(String orderId, NegotiationsRequestJson negotiationsRequestJson);

    Boolean addNote(String orderId, NoteRequestJson noteRequestJson);

    Boolean cancel(String orderId, CancelRequestJson cancelRequestJson);
}
