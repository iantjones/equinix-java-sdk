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
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.customerportal.client.implementation.CustomerPortalConfigImpl;
import api.equinix.javasdk.customerportal.client.internal.BillingCreditClient;
import api.equinix.javasdk.customerportal.model.BillingCredit;
import api.equinix.javasdk.customerportal.model.json.BillingCreditJson;

public class BillingCreditClientImpl extends ResourceClientBase<BillingCredit, BillingCreditJson> implements BillingCreditClient<BillingCredit> {

    public BillingCreditClientImpl(CustomerPortalConfigImpl configClient) {
        super(configClient, "CustomerPortal", "BillingCredits", BillingCreditJson.class);
    }

    @Override
    protected BillingCredit wrap(BillingCreditJson json) {
        return json;
    }

    public Page<BillingCredit, BillingCreditJson> list() {
        return listPage("ListBillingCredits");
    }

    public BillingCreditJson getByUuid(String uuid) {
        return getOne("GetBillingCredit", uuid);
    }
}
