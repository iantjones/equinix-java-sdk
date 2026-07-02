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

package api.equinix.javasdk.fabric.model.json;

import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.fabric.enums.MarketplaceSubscriptionState;
import api.equinix.javasdk.fabric.model.MarketplaceSubscription;
import api.equinix.javasdk.fabric.model.implementation.ChangeLog;
import api.equinix.javasdk.fabric.model.implementation.SubscriptionEntitlement;
import api.equinix.javasdk.fabric.model.implementation.SubscriptionTrial;
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

    @JsonProperty("type")
    private String type;

    @JsonProperty("state")
    private MarketplaceSubscriptionState state;

    @JsonProperty("marketplace")
    private String marketplace;

    @JsonProperty("offerType")
    private String offerType;

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
