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

package api.equinix.javasdk;

import api.equinix.javasdk.core.auth.EquinixCredentials;
import api.equinix.javasdk.core.model.Service;
import api.equinix.javasdk.customerportal.client.CustomerPortalConfig;
import api.equinix.javasdk.customerportal.client.Invoices;
import api.equinix.javasdk.customerportal.client.Resellers;
import api.equinix.javasdk.customerportal.client.implementation.CustomerPortalConfigImpl;
import api.equinix.javasdk.customerportal.client.implementation.InvoicesImpl;
import api.equinix.javasdk.customerportal.client.implementation.ResellersImpl;

public final class CustomerPortal extends EquinixClient implements Service {

    private Invoices invoices;

    private Resellers resellers;

    final private CustomerPortalConfig customerPortalConfig;

    public CustomerPortal(EquinixCredentials equinixCredentials) {
        this(equinixCredentials, false);
    }

    public CustomerPortal(EquinixCredentials equinixCredentials, boolean isSandBoxed) {
        super(equinixCredentials, isSandBoxed);

        String paramFile = "json/apiParams_CustomerPortal.json";
        equinixClient.appendApiParams(paramFile);

        this.customerPortalConfig = new CustomerPortalConfigImpl(equinixClient);
    }

    public Invoices invoices() {
        if (this.invoices == null) {
            this.invoices = new InvoicesImpl(this.customerPortalConfig.getInvoiceSummaryClient(),
                    this.customerPortalConfig.getInvoiceDetailClient(), this);
        }
        return invoices;
    }

    public Resellers resellers() {
        if (this.resellers == null) {
            this.resellers = new ResellersImpl(this.customerPortalConfig.getResellerClient(),
                    this.customerPortalConfig.getResellerCustomerClient(), this);
        }
        return resellers;
    }
}
