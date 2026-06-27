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

package api.equinix.javasdk.networkedge.client.internal;

import api.equinix.javasdk.networkedge.client.RequestBuilder;
import api.equinix.javasdk.networkedge.model.json.Pricing;

/**
 * <p>PricingClient interface.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public interface PricingClient {

    /**
     * <p>getPricing.</p>
     *
     * @param requestBuilder a {@link api.equinix.javasdk.networkedge.client.RequestBuilder.Pricing} object.
     * @return a {@link api.equinix.javasdk.networkedge.model.json.Pricing} object.
     */
    Pricing getPricing(RequestBuilder.Pricing requestBuilder);

    /**
     * <p>getPricing.</p>
     *
     * @param deviceUuid a {@link java.lang.String} object.
     * @return a {@link api.equinix.javasdk.networkedge.model.json.Pricing} object.
     */
    Pricing getPricing(String deviceUuid);
}
