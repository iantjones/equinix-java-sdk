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

package api.equinix.javasdk.customerportal.model.json.creators;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.Map;

/**
 * Request body for searching IBX or network notifications
 * ({@code POST /v1/notifications/ibx/search}, {@code POST /v1/notifications/network/search}).
 *
 * <p>The {@code filter} carries {@code ibxs}, {@code types}, {@code statuses}, a {@code dateRange}
 * and (for network) {@code productTypes}; because the filter shape varies it is supplied as a
 * free-form map.</p>
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificationSearchRequest {

    @JsonProperty("filter")
    private final Map<String, Object> filter;

    public NotificationSearchRequest(Map<String, Object> filter) {
        this.filter = filter;
    }
}
