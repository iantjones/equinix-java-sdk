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

/**
 * <p>Page class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class Page<T, S> {

    @JsonIgnore
    private EquinixRequest<T> associatedRequest;

    @JsonIgnore
    private EquinixResponse<T> associatedResponse;

    @JsonAlias("data")
    private ArrayList<S> items;

    private Pagination pagination;
}
