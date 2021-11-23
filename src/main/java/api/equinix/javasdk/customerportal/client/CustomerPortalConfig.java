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

package api.equinix.javasdk.customerportal.client;

import api.equinix.javasdk.customerportal.client.internal.InvoiceDetailClient;
import api.equinix.javasdk.customerportal.client.internal.InvoiceSummaryClient;
import api.equinix.javasdk.customerportal.client.internal.ResellerClient;
import api.equinix.javasdk.customerportal.client.internal.ResellerCustomerClient;
import api.equinix.javasdk.customerportal.model.InvoiceDetail;
import api.equinix.javasdk.customerportal.model.InvoiceSummary;
import api.equinix.javasdk.customerportal.model.Reseller;
import api.equinix.javasdk.customerportal.model.ResellerCustomer;

public interface CustomerPortalConfig {

    InvoiceSummaryClient<InvoiceSummary> getInvoiceSummaryClient();

    InvoiceDetailClient<InvoiceDetail> getInvoiceDetailClient();

    ResellerClient<Reseller> getResellerClient();

    ResellerCustomerClient<ResellerCustomer> getResellerCustomerClient();
}
