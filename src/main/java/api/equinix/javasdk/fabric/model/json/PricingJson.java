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
import api.equinix.javasdk.fabric.enums.PriceType;
import api.equinix.javasdk.fabric.model.Pricing;
import api.equinix.javasdk.fabric.model.implementation.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Getter;

import java.util.ArrayList;

@Getter
public class PricingJson implements Serializable {

    @Getter static TypeReference<PricingJson> singleTypeRef = new TypeReference<>() {};
    @Getter static TypeReference<Page<Pricing, PricingJson>> pagedTypeRef = new TypeReference<>() {};

    @JsonProperty("type")
    private PriceType type;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("code")
    private String code;

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("account")
    private Account account;

    @JsonProperty("charges")
    private ArrayList<Charge> charges;

    @JsonProperty("connection")
    private PricingConnection connection;

    @JsonProperty("port")
    private PricingPort port;

    @JsonProperty("gateway")
    private PricingGateway gateway;

    @JsonProperty("ipBlock")
    private PricingIPBlock ipBlock;
}
