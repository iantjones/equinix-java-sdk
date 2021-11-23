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
import api.equinix.javasdk.core.http.request.EquinixRequest;
import api.equinix.javasdk.core.http.response.EquinixResponse;
import api.equinix.javasdk.core.enums.RequestType;
import api.equinix.javasdk.networkedge.client.RequestBuilder;
import api.equinix.javasdk.networkedge.client.implementation.NetworkEdgeConfigImpl;
import api.equinix.javasdk.networkedge.client.internal.PricingClient;
import api.equinix.javasdk.networkedge.model.json.Pricing;
import api.equinix.javasdk.networkedge.model.json.VPNJson;

import java.util.List;
import java.util.Map;

/**
 * <p>PricingClientImpl class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class PricingClientImpl extends ClientBase implements PricingClient {

    /**
     * <p>Constructor for PricingClientImpl.</p>
     *
     * @param configClient a {@link api.equinix.javasdk.networkedge.client.implementation.NetworkEdgeConfigImpl} object.
     */
    public PricingClientImpl(NetworkEdgeConfigImpl configClient) {
        super(configClient, "NetworkEdge", "Pricing");
    }

    /**
     * {@inheritDoc}
     *
     * <p>getPricing.</p>
     */
    public Pricing getPricing(RequestBuilder.Pricing requestBuilder) {
        Map<String, List<String>> qParams = Utils.newMap(requestBuilder);

        EquinixRequest<Pricing> equinixRequest = this.buildRequestWithQueryParams("GetPricing", RequestType.SINGLE, qParams, Pricing.getSingleTypeRef());
        EquinixResponse<Pricing> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }

    /**
     * <p>getPricing.</p>
     *
     * @param deviceUuid a {@link java.lang.String} object.
     * @return a {@link api.equinix.javasdk.networkedge.model.json.Pricing} object.
     */
    public Pricing getPricing(String deviceUuid) {
        Map<String, List<String>> qParams = Utils.singleParamMap("virtualDeviceUuid", deviceUuid);

        EquinixRequest<VPNJson> equinixRequest = this.buildRequestWithQueryParams("GetPricing", RequestType.SINGLE, qParams, Pricing.getSingleTypeRef());
        EquinixResponse<VPNJson> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }
}
