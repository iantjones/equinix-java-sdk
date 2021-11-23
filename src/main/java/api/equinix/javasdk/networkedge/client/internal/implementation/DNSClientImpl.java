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

import api.equinix.javasdk.core.client.PageableBase;
import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.request.EquinixRequest;
import api.equinix.javasdk.core.http.response.EquinixResponse;
import api.equinix.javasdk.core.enums.RequestType;
import api.equinix.javasdk.networkedge.client.RequestBuilder;
import api.equinix.javasdk.networkedge.client.implementation.NetworkEdgeConfigImpl;
import api.equinix.javasdk.networkedge.client.internal.DNSClient;
import api.equinix.javasdk.networkedge.model.implementation.DNSLookup;

import java.util.Map;

/**
 * <p>DNSClientImpl class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class DNSClientImpl extends PageableBase implements DNSClient<DNSLookup> {

    /**
     * <p>Constructor for DNSClientImpl.</p>
     *
     * @param configClient a {@link api.equinix.javasdk.networkedge.client.implementation.NetworkEdgeConfigImpl} object.
     */
    public DNSClientImpl(NetworkEdgeConfigImpl configClient) {
        super(configClient, "NetworkEdge", "DNS");
    }

    /**
     * {@inheritDoc}
     *
     * <p>dnsLookup.</p>
     */
    public Map<String, DNSLookup> dnsLookup(RequestBuilder.DNSLookup requestBuilder) {
        EquinixRequest<DNSLookup> equinixRequest = this.buildRequest("DNSLookup", RequestType.SINGLE, DNSLookup.getSingleTypeRef());
        Utils.serializeJson(equinixRequest, requestBuilder.build().getDnsLookupRequest());
        EquinixResponse<DNSLookup> equinixResponse = this.invoke(equinixRequest);
        return Utils.handleSingletonResponse(equinixResponse, equinixRequest);
    }
}
