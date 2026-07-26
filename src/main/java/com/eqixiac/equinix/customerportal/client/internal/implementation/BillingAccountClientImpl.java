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
import com.eqixiac.equinix.core.http.response.Page;
import com.eqixiac.equinix.customerportal.client.implementation.CustomerPortalConfigImpl;
import com.eqixiac.equinix.customerportal.client.internal.BillingAccountClient;
import com.eqixiac.equinix.customerportal.model.BillingAccount;
import com.eqixiac.equinix.customerportal.model.json.BillingAccountJson;

import java.util.List;
import java.util.Map;

public class BillingAccountClientImpl extends ResourceClientBase<BillingAccount, BillingAccountJson>
        implements BillingAccountClient<BillingAccount> {

    public BillingAccountClientImpl(CustomerPortalConfigImpl configClient) {
        super(configClient, "CustomerPortal", "BillingAccounts", BillingAccountJson.class);
    }

    @Override
    protected BillingAccount wrap(BillingAccountJson json) {
        return json;
    }

    public Page<BillingAccountJson> summaries() {
        return listPage("ListBillingAccounts");
    }

    public Page<BillingAccountJson> summaries(String sorts) {
        if (sorts == null) {
            return listPage("ListBillingAccounts");
        }
        return listPage("ListBillingAccounts", Map.of("sorts", List.of(sorts)));
    }

    public BillingAccountJson getByAccountNumber(String accountNumber) {
        return getOne("GetBillingAccount", Map.of("accountNumber", accountNumber));
    }

    public BillingAccountJson getByAccountNumber(String accountNumber, String months) {
        if (months == null) {
            return getByAccountNumber(accountNumber);
        }
        return getAs("GetBillingAccount", Map.of("accountNumber", accountNumber),
                Map.of("months", List.of(months)), BillingAccountJson.class);
    }

    public byte[] downloadInvoiceDocument(String accountNumber, String invoiceId, String documentId) {
        return bytesOp("DownloadBillingDocument",
                Map.of("accountNumber", accountNumber, "invoiceId", invoiceId),
                Map.of("documentId", List.of(documentId)));
    }
}
