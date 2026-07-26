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
import com.eqixiac.equinix.core.http.response.Page;
import com.eqixiac.equinix.fabric.client.implementation.FabricConfigImpl;
import com.eqixiac.equinix.fabric.client.internal.TagClient;
import com.eqixiac.equinix.fabric.model.Tag;
import com.eqixiac.equinix.fabric.model.json.TagJson;
import com.eqixiac.equinix.fabric.model.json.creators.TagCreatorJson;

/**
 * Internal client for Fabric resource tags. The JSON model implements the public interface
 * directly, so {@link #wrap(TagJson)} is the identity.
 *
 * @author ianjones
 */
public class TagClientImpl extends ResourceClientBase<Tag, TagJson> implements TagClient<Tag> {

    public TagClientImpl(FabricConfigImpl configClient) {
        super(configClient, "Fabric", "Tags", TagJson.class);
    }

    @Override
    protected Tag wrap(TagJson json) {
        return json;
    }

    public Page<TagJson> list() {
        return listPage("GetTags");
    }

    public TagJson create(String type, String name, String displayName) {
        return postOne("PostTag", TagCreatorJson.builder()
                .type(type)
                .name(name)
                .displayName(displayName)
                .build());
    }
}
