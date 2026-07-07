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

package api.equinix.javasdk.networkedge.client.internal.implementation;

import api.equinix.javasdk.core.client.ClientBase;
import api.equinix.javasdk.core.http.ParameterMapper;
import api.equinix.javasdk.networkedge.client.RequestBuilder;
import api.equinix.javasdk.networkedge.client.implementation.NetworkEdgeConfigImpl;
import api.equinix.javasdk.networkedge.client.internal.PricingClient;
import api.equinix.javasdk.networkedge.model.json.Pricing;

import java.util.List;
import java.util.Map;

/**
 *
 * @author ianjones
 */
public class PricingClientImpl extends ClientBase implements PricingClient {

    public PricingClientImpl(NetworkEdgeConfigImpl configClient) {
        super(configClient, "NetworkEdge", "Pricing");
    }

    public Pricing getPricing(RequestBuilder.Pricing requestBuilder) {
        Map<String, List<String>> qParams = ParameterMapper.newMap(requestBuilder);
        return getAs("GetPricing", null, qParams, Pricing.class);
    }

    public Pricing getPricing(String deviceUuid) {
        return getAs("GetPricing", null, ParameterMapper.singleParamMap("virtualDeviceUuid", deviceUuid), Pricing.class);
    }
}
