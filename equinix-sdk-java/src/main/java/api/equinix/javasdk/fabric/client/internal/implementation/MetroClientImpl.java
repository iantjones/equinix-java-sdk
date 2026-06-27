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

package api.equinix.javasdk.fabric.client.internal.implementation;

import api.equinix.javasdk.core.client.ResourceClientBase;
import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.fabric.client.implementation.FabricConfigImpl;
import api.equinix.javasdk.fabric.client.internal.MetroClient;
import api.equinix.javasdk.fabric.enums.MetroPresence;
import api.equinix.javasdk.fabric.model.Metro;
import api.equinix.javasdk.fabric.model.json.MetroJson;
import api.equinix.javasdk.fabric.model.wrappers.MetroWrapper;

import java.util.List;
import java.util.Map;

/**
 * Internal client for Fabric Metros (read-only). Plumbing/paging provided by {@link ResourceClientBase}.
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class MetroClientImpl extends ResourceClientBase<Metro, MetroJson> implements MetroClient<Metro> {

    public MetroClientImpl(FabricConfigImpl configClient) {
        super(configClient, "Fabric", "Metros", MetroJson.class);
    }

    @Override
    protected Metro wrap(MetroJson json) {
        return new MetroWrapper(json, this);
    }

    public Page<Metro, MetroJson> list() {
        return list(null);
    }

    public Page<Metro, MetroJson> list(MetroPresence metroPresence) {
        Map<String, List<String>> queryParams = metroPresence != null
                ? Map.of("presence", Utils.singleParamList(metroPresence)) : null;
        return listPage("GetMetros", queryParams);
    }

    public MetroJson getByMetroCode(MetroCode metroCode) {
        return getOne("GetMetro", Map.of("metroCode", metroCode.toString()));
    }

    public MetroJson refresh(MetroCode metroCode) {
        return getByMetroCode(metroCode);
    }
}
