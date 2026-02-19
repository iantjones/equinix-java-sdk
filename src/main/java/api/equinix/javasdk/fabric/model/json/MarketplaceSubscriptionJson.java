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
import api.equinix.javasdk.core.model.Serializable;
import api.equinix.javasdk.fabric.enums.MarketplaceSubscriptionState;
import api.equinix.javasdk.fabric.model.MarketplaceSubscription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Getter;

import java.util.List;

@Getter
public final class MarketplaceSubscriptionJson implements Serializable, MarketplaceSubscription {

    @Getter static TypeReference<Page<MarketplaceSubscription, MarketplaceSubscriptionJson>> pagedTypeRef = new TypeReference<>() {};
    @Getter static TypeReference<List<MarketplaceSubscriptionJson>> listTypeRef = new TypeReference<>() {};
    @Getter static TypeReference<MarketplaceSubscriptionJson> singleTypeRef = new TypeReference<>() {};

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

    @JsonProperty("offerId")
    private String offerId;

    @JsonProperty("isAutoRenew")
    private Boolean isAutoRenew;
}
