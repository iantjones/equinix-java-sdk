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

package api.equinix.javasdk.customerportal.client.implementation;

import api.equinix.javasdk.core.client.Config;
import api.equinix.javasdk.core.client.EquinixClient;
import api.equinix.javasdk.customerportal.client.CustomerPortalConfig;
import api.equinix.javasdk.customerportal.client.internal.implementation.InvoiceDetailClientImpl;
import api.equinix.javasdk.customerportal.client.internal.implementation.InvoiceSummaryClientImpl;
import api.equinix.javasdk.customerportal.client.internal.implementation.ResellerClientImpl;
import api.equinix.javasdk.customerportal.client.internal.implementation.ResellerCustomerClientImpl;
import lombok.Getter;

@Getter
public class CustomerPortalConfigImpl extends Config implements CustomerPortalConfig {

    private final InvoiceSummaryClientImpl invoiceSummaryClient;

    private final InvoiceDetailClientImpl invoiceDetailClient;

    private final ResellerClientImpl resellerClient;

    private final ResellerCustomerClientImpl resellerCustomerClient;

    public CustomerPortalConfigImpl(EquinixClient equinixClient) {
        super(equinixClient);
        this.invoiceSummaryClient = new InvoiceSummaryClientImpl(this);
        this.invoiceDetailClient = new InvoiceDetailClientImpl(this);
        this.resellerClient = new ResellerClientImpl(this);
        this.resellerCustomerClient = new ResellerCustomerClientImpl(this);
    }
}
