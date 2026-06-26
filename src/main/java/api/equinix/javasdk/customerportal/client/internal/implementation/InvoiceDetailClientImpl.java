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

package api.equinix.javasdk.customerportal.client.internal.implementation;

import api.equinix.javasdk.core.client.ResourceClientBase;
import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.customerportal.client.RequestBuilder;
import api.equinix.javasdk.customerportal.client.implementation.CustomerPortalConfigImpl;
import api.equinix.javasdk.customerportal.client.internal.InvoiceDetailClient;
import api.equinix.javasdk.customerportal.model.InvoiceDetail;
import api.equinix.javasdk.customerportal.model.json.InvoiceDetailJson;
import api.equinix.javasdk.customerportal.model.wrappers.InvoiceDetailWrapper;

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

    public Page<InvoiceDetail, InvoiceDetailJson> list(RequestBuilder.Invoice requestBuilder) {
        Map<String, List<String>> qParams = Utils.processRequestBuilder(requestBuilder);
        return listPage("ListInvoiceDetails", qParams);
    }
}
