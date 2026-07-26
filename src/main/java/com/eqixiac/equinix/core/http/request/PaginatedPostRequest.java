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

package com.eqixiac.equinix.core.http.request;

/**
 * Request carrier for POST-based search operations whose paging state lives in the request
 * <em>body</em> (a {@link com.eqixiac.equinix.core.model.PaginatedPostBody}) rather than in
 * {@code offset}/{@code limit} query parameters. The paging pipeline advances the body's
 * pagination between pages; because the wire entity is rebuilt from the {@link RequestBody}
 * at every dispatch, the advanced offset is re-serialized automatically.
 *
 * @param <T> the operation's model type
 * @author ianjones
 */
public final class PaginatedPostRequest<T> extends EquinixRequest<T> {

    /**
     * Convenience accessor for the live search body: the payload of this request's JSON
     * {@link RequestBody}.
     *
     * @return the search body payload, or {@code null} when no JSON body is attached
     */
    public Object getSearchBody() {
        RequestBody requestBody = getBody();
        return requestBody != null ? requestBody.getPayload() : null;
    }
}
