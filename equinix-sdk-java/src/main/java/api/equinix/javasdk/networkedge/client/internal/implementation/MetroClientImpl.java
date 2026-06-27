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

import api.equinix.javasdk.core.client.ResourceClientBase;
import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.enums.Region;
import api.equinix.javasdk.networkedge.client.implementation.NetworkEdgeConfigImpl;
import api.equinix.javasdk.networkedge.client.internal.MetroClient;
import api.equinix.javasdk.networkedge.model.Metro;
import api.equinix.javasdk.networkedge.model.json.MetroJson;
import api.equinix.javasdk.networkedge.model.wrappers.MetroWrapper;

import java.util.List;
import java.util.Map;

/**
 * <p>MetroClientImpl class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class MetroClientImpl extends ResourceClientBase<Metro, MetroJson> implements MetroClient<Metro> {

    /**
     * <p>Constructor for MetroClientImpl.</p>
     *
     * @param configClient a {@link api.equinix.javasdk.networkedge.client.implementation.NetworkEdgeConfigImpl} object.
     */
    public MetroClientImpl(NetworkEdgeConfigImpl configClient) {
        super(configClient, "NetworkEdge", "Metros", MetroJson.class);
    }

    /** {@inheritDoc} */
    @Override
    protected Metro wrap(MetroJson json) {
        return new MetroWrapper(json, this);
    }

    /** {@inheritDoc} */
    public Page<Metro, MetroJson> list(Region region) {
        Map<String, List<String>> qParams = Utils.singleParamMap("region" , region);
        return listPage("ListMetros", qParams);
    }
}
