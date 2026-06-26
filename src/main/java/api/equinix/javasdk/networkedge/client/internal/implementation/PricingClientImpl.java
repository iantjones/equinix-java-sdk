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
import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.networkedge.client.RequestBuilder;
import api.equinix.javasdk.networkedge.client.implementation.NetworkEdgeConfigImpl;
import api.equinix.javasdk.networkedge.client.internal.PricingClient;
import api.equinix.javasdk.networkedge.model.json.Pricing;

import java.util.List;
import java.util.Map;

/**
 * <p>PricingClientImpl class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class PricingClientImpl extends ClientBase implements PricingClient {

    public PricingClientImpl(NetworkEdgeConfigImpl configClient) {
        super(configClient, "NetworkEdge", "Pricing");
    }

    /** {@inheritDoc} */
    public Pricing getPricing(RequestBuilder.Pricing requestBuilder) {
        Map<String, List<String>> qParams = Utils.newMap(requestBuilder);
        return getAs("GetPricing", null, qParams, Pricing.class);
    }

    /**
     * <p>getPricing.</p>
     *
     * @param deviceUuid a {@link java.lang.String} object.
     * @return a {@link api.equinix.javasdk.networkedge.model.json.Pricing} object.
     */
    public Pricing getPricing(String deviceUuid) {
        return getAs("GetPricing", null, Utils.singleParamMap("virtualDeviceUuid", deviceUuid), Pricing.class);
    }
}
