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

package api.equinix.javasdk.fabric.model.wrappers;

import api.equinix.javasdk.core.http.response.Pageable;
import api.equinix.javasdk.core.model.ResourceImpl;
import api.equinix.javasdk.fabric.model.Pricing;
import api.equinix.javasdk.fabric.model.json.PricingJson;
import lombok.Getter;
import lombok.experimental.Delegate;

public class PricingWrapper extends ResourceImpl<Pricing> implements Pricing {

    @Delegate
    private PricingJson jsonObject;
    @Getter
    private final Pageable<Pricing> serviceClient;

    public PricingWrapper(PricingJson pricingJson, Pageable<Pricing> serviceClient) {
        this.jsonObject = pricingJson;
        this.serviceClient = serviceClient;
    }

//
//
//    public ConnectionPricing refresh() {
//        this.jsonObject = ((ConnectionsClientImpl)this.serviceClient).refreshStatistics(this.getUuid(),
//                this.getStats().getStartDateTime(), this.getStats().getEndDateTime(), this.getStats().getViewPoint());
//        return this;
//    }
}
