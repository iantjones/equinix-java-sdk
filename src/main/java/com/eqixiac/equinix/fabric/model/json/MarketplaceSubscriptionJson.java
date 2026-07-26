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

package com.eqixiac.equinix.fabric.model.json;
import com.eqixiac.equinix.fabric.enums.Marketplace;
import com.eqixiac.equinix.fabric.enums.MarketplaceOfferType;

import com.eqixiac.equinix.core.http.response.Page;
import com.eqixiac.equinix.fabric.enums.MarketplaceSubscriptionState;
import com.eqixiac.equinix.fabric.model.MarketplaceSubscription;
import com.eqixiac.equinix.fabric.model.implementation.ChangeLog;
import com.eqixiac.equinix.fabric.model.implementation.SubscriptionEntitlement;
import com.eqixiac.equinix.fabric.model.implementation.SubscriptionTrial;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Getter;

import java.util.List;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public final class MarketplaceSubscriptionJson implements MarketplaceSubscription {

    @Getter static TypeReference<List<MarketplaceSubscriptionJson>> listTypeRef = new TypeReference<>() {};

    @JsonProperty("uuid")
    private String uuid;

    @JsonProperty("href")
    private String href;

    @JsonProperty("state")
    private MarketplaceSubscriptionState state;

    @JsonProperty("marketplace")
    private Marketplace marketplace;

    @JsonProperty("offerType")
    private MarketplaceOfferType offerType;

    @JsonProperty("offerId")
    private String offerId;

    @JsonProperty("isAutoRenew")
    private Boolean isAutoRenew;

    @JsonProperty("trial")
    private SubscriptionTrial trial;

    @JsonProperty("metroCodes")
    private List<String> metroCodes;

    @JsonProperty("entitlements")
    private List<SubscriptionEntitlement> entitlements;

    @JsonProperty("changelog")
    private ChangeLog changeLog;
}
