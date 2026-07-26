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

package com.eqixiac.equinix.customerportal.model.wrappers;

import com.eqixiac.equinix.core.http.response.Pageable;
import com.eqixiac.equinix.core.model.ResourceImpl;
import com.eqixiac.equinix.customerportal.model.InvoiceSummary;
import com.eqixiac.equinix.customerportal.model.json.InvoiceSummaryJson;
import lombok.Getter;
import lombok.experimental.Delegate;

public class InvoiceSummaryWrapper extends ResourceImpl<InvoiceSummary> implements InvoiceSummary {

    @Delegate
    private InvoiceSummaryJson json;
    @Getter
    private final Pageable<InvoiceSummary> serviceClient;

    public InvoiceSummaryWrapper(InvoiceSummaryJson invoiceSummaryJson, Pageable<InvoiceSummary> serviceClient) {
        this.json = invoiceSummaryJson;
        this.serviceClient = serviceClient;
    }
}
