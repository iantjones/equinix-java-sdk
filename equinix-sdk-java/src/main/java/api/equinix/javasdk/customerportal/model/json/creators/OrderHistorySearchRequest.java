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

import java.util.List;
import java.util.Map;

/**
 * Request body for searching order history
 * ({@code POST /v1/retrieve-orders}, {@code OrderHistoryAPIRequest}).
 *
 * <p>{@code filters} carries the optional {@code ibxs}, {@code productTypes}, {@code orderStatus},
 * {@code dateRange}, {@code fromDate} and {@code toDate}; {@code source} restricts the fields a
 * free-text {@code q} query matches; {@code page} carries paging ({@code number}/{@code size}).
 * The nested objects are highly structured, so {@code filters} and {@code page} are supplied as
 * free-form maps.</p>
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderHistorySearchRequest {

    @JsonProperty("filters")
    private final Map<String, Object> filters;

    @JsonProperty("source")
    private final List<String> source;

    @JsonProperty("q")
    private final String q;

    @JsonProperty("sorts")
    private final List<Map<String, Object>> sorts;

    @JsonProperty("page")
    private final Map<String, Object> page;

    private OrderHistorySearchRequest(Builder builder) {
        this.filters = builder.filters;
        this.source = builder.source;
        this.q = builder.q;
        this.sorts = builder.sorts;
        this.page = builder.page;
    }

    /**
     * Returns a new builder for an order history search request.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Map<String, Object> filters;
        private List<String> source;
        private String q;
        private List<Map<String, Object>> sorts;
        private Map<String, Object> page;

        private Builder() {
        }

        public Builder filters(Map<String, Object> filters) {
            this.filters = filters;
            return this;
        }

        public Builder source(List<String> source) {
            this.source = source;
            return this;
        }

        public Builder q(String q) {
            this.q = q;
            return this;
        }

        public Builder sorts(List<Map<String, Object>> sorts) {
            this.sorts = sorts;
            return this;
        }

        public Builder page(Map<String, Object> page) {
            this.page = page;
            return this;
        }

        public OrderHistorySearchRequest build() {
            return new OrderHistorySearchRequest(this);
        }
    }
}
