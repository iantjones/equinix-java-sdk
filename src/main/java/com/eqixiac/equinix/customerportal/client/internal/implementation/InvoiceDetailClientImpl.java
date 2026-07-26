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

package com.eqixiac.equinix.customerportal.client.internal.implementation;

import com.eqixiac.equinix.core.client.ResourceClientBase;
import com.eqixiac.equinix.core.http.ParameterMapper;
import com.eqixiac.equinix.core.http.response.Page;
import com.eqixiac.equinix.customerportal.client.RequestBuilder;
import com.eqixiac.equinix.customerportal.client.implementation.CustomerPortalConfigImpl;
import com.eqixiac.equinix.customerportal.client.internal.InvoiceDetailClient;
import com.eqixiac.equinix.customerportal.model.InvoiceDetail;
import com.eqixiac.equinix.customerportal.model.json.InvoiceDetailJson;
import com.eqixiac.equinix.customerportal.model.wrappers.InvoiceDetailWrapper;

import java.util.List;
import java.util.Map;

public class InvoiceDetailClientImpl extends ResourceClientBase<InvoiceDetail, InvoiceDetailJson> implements InvoiceDetailClient<InvoiceDetail> {

    public InvoiceDetailClientImpl(CustomerPortalConfigImpl configClient) {
        super(configClient, "CustomerPortal", "Invoices", InvoiceDetailJson.class);
    }

    @Override
    protected InvoiceDetail wrap(InvoiceDetailJson json) {
        return new InvoiceDetailWrapper(json, this);
    }

    public Page<InvoiceDetailJson> list(RequestBuilder.Invoice requestBuilder) {
        Map<String, List<String>> qParams = ParameterMapper.processRequestBuilder(requestBuilder);
        return listPage("ListInvoiceDetails", qParams);
    }
}
