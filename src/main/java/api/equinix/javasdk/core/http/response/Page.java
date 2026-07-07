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

package api.equinix.javasdk.core.http.response;

import api.equinix.javasdk.core.http.request.EquinixRequest;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * A deserialized page of a list/search response: the raw JSON items plus the response's
 * pagination metadata and the request/response pair used to fetch it (for lazy paging).
 *
 * @param <J> the item type the page's {@code items}/{@code data} array deserializes into
 *            (usually the resource's JSON model class)
 * @author ianjones
 */

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class Page<J> {

    // The request/response pair is carried for lazy paging only; its element type is
    // deliberately unbounded (the request may be typed over the public model while the
    // page is typed over the JSON model).
    @JsonIgnore
    private EquinixRequest<?> associatedRequest;

    @JsonIgnore
    private EquinixResponse<?> associatedResponse;

    /**
     * The page's items. Initialized so a response that omits the {@code items}/{@code data}
     * array (some endpoints do this on empty results) reads as an empty page rather than
     * {@code null} — a {@code null} here used to NPE deep inside the list-mapping helpers.
     */
    @JsonAlias("data")
    private List<J> items = new ArrayList<>();

    private Pagination pagination;
}
