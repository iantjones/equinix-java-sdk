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

package api.equinix.javasdk.fabric.client.implementation;

import api.equinix.javasdk.core.http.ResponseHandler;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.fabric.client.Tags;
import api.equinix.javasdk.fabric.client.internal.TagClient;
import api.equinix.javasdk.fabric.model.Tag;
import api.equinix.javasdk.fabric.model.json.TagJson;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TagsImpl implements Tags {

    private final TagClient<Tag> serviceClient;

    public PaginatedList<Tag> list() {
        Page<TagJson> responsePage = this.serviceClient.list();
        PaginatedList<Tag> tagList = ResponseHandler.mapPaginatedList(responsePage.getItems(), this.serviceClient, (json, client) -> json);
        return new PaginatedList<>(tagList, this.serviceClient, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    public Tag create(String type, String name, String displayName) {
        return this.serviceClient.create(type, name, displayName);
    }
}
