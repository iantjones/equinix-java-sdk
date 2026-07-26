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

package com.eqixiac.equinix.fabric.client.internal.implementation;

import com.eqixiac.equinix.core.client.ResourceClientBase;
import com.eqixiac.equinix.core.enums.MetroCode;
import com.eqixiac.equinix.core.http.ParameterMapper;
import com.eqixiac.equinix.core.http.response.Page;
import com.eqixiac.equinix.fabric.client.implementation.FabricConfigImpl;
import com.eqixiac.equinix.fabric.client.internal.MetroClient;
import com.eqixiac.equinix.fabric.enums.MetroPresence;
import com.eqixiac.equinix.fabric.model.Metro;
import com.eqixiac.equinix.fabric.model.json.MetroJson;
import com.eqixiac.equinix.fabric.model.wrappers.MetroWrapper;

import java.util.List;
import java.util.Map;

/**
 * Internal client for Fabric Metros (read-only). Plumbing/paging provided by {@link ResourceClientBase}.
 *
 * @author ianjones
 */
public class MetroClientImpl extends ResourceClientBase<Metro, MetroJson> implements MetroClient<Metro> {

    public MetroClientImpl(FabricConfigImpl configClient) {
        super(configClient, "Fabric", "Metros", MetroJson.class);
    }

    @Override
    protected Metro wrap(MetroJson json) {
        return new MetroWrapper(json, this);
    }

    public Page<MetroJson> list() {
        return list(null);
    }

    public Page<MetroJson> list(MetroPresence metroPresence) {
        Map<String, List<String>> queryParams = metroPresence != null
                ? Map.of("presence", ParameterMapper.singleParamList(metroPresence)) : null;
        return listPage("GetMetros", queryParams);
    }

    public MetroJson getByMetroCode(MetroCode metroCode) {
        return getByMetroCode(metroCode.toString());
    }

    public MetroJson getByMetroCode(String metroCode) {
        return getOne("GetMetro", Map.of("metroCode", metroCode));
    }

    public MetroJson refresh(MetroCode metroCode) {
        return getByMetroCode(metroCode);
    }
}
