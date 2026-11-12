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

package com.eqixiac.equinix.fabric.client.internal;

import com.eqixiac.equinix.core.http.response.Page;
import com.eqixiac.equinix.core.http.response.PageablePost;
import com.eqixiac.equinix.fabric.model.implementation.filter.FilterPropertyList;
import com.eqixiac.equinix.fabric.model.implementation.sort.SortPropertyList;
import com.eqixiac.equinix.fabric.model.Stream;
import com.eqixiac.equinix.fabric.model.json.StreamJson;
import com.eqixiac.equinix.fabric.model.json.creators.StreamCreatorJson;

public interface StreamClient<T> extends PageablePost<T> {

    Page<StreamJson> list();

    StreamJson getByUuid(String uuid);

    StreamJson create(StreamCreatorJson streamCreatorJson);

    StreamJson update(String uuid, StreamCreatorJson streamCreatorJson);

    StreamJson delete(String uuid);

    StreamJson refresh(String uuid);

    /**
     * {@code POST /fabric/v4/streams/search}.
     *
     * @param filter the filter, or {@code null}
     * @param sort the sort, or {@code null}
     * @return the first page
     */
    Page<StreamJson> search(FilterPropertyList filter, SortPropertyList sort);
}
